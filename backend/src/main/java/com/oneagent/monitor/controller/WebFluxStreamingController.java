package com.oneagent.monitor.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oneagent.monitor.model.dto.ActionTriggered;
import com.oneagent.monitor.model.dto.InputCase;
import com.oneagent.monitor.model.entity.MonitorStatus;
import com.oneagent.monitor.service.ChatService;
import com.oneagent.monitor.service.MonitorService;
import com.oneagent.monitor.util.MsgUtils;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Event;
import io.agentscope.core.agent.EventType;
import io.agentscope.core.agent.StreamOptions;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ThinkingBlock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * WebFlux 流式响应控制器
 * 使用 Flux<ServerSentEvent> 实现 AgentScope 流式响应
 */
@Slf4j
@RestController
@RequestMapping("/api")
public class WebFluxStreamingController {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ChatService chatService;
    private final MonitorService monitorService;
    private final ObjectProvider<ReActAgent> customerServiceAgentProvider;

    // 会话管理：每个 caseId 对应一个 Agent 实例
    private final ConcurrentHashMap<String, ReActAgent> agentSessions = new ConcurrentHashMap<>();

    public WebFluxStreamingController(
            ChatService chatService,
            MonitorService monitorService,
            ObjectProvider<ReActAgent> customerServiceAgentProvider) {
        this.chatService = chatService;
        this.monitorService = monitorService;
        this.customerServiceAgentProvider = customerServiceAgentProvider;
    }

    /**
     * 处理流式请求 - 使用 WebFlux Flux<ServerSentEvent>
     */
    @PostMapping(value = "/process", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> processRequest(@RequestBody InputCase inputCase) {
        log.info("处理流式请求: caseId={}", inputCase.getCaseId());

        // 根据 caseId 获取或创建 Agent 实例（同一个聊天框使用同一个 Agent）
        ReActAgent customerServiceAgent = agentSessions.computeIfAbsent(
                inputCase.getCaseId(),
                id -> {
                    log.info("为会话 {} 创建新的 Agent 实例", id);
                    return customerServiceAgentProvider.getObject();
                }
        );

        // 更新监控服务
        monitorService.updateStatus(
                inputCase.getApiStatus(),
                inputCase.getApiResponseTime(),
                inputCase.getMonitorLog()
        );

        // 检查是否需要告警
        ActionTriggered actions = null;
        if (monitorService.needsAlert(inputCase.getApiStatus())) {
            actions = chatService.handleApiAlert(inputCase);
        }

        // 构建带有上下文的查询
        String contextualQuery = chatService.buildContextualQuery(inputCase);

        // 构建消息
        Msg message = Msg.builder()
                .name("user")
                .role(MsgRole.USER)
                .textContent(contextualQuery)
                .build();

        // Configure streaming options - INCREMENTAL mode for SSE
        StreamOptions streamOptions =
                StreamOptions.builder()
                        .eventTypes(EventType.REASONING, EventType.TOOL_RESULT)
                        .incremental(true)
                        .includeReasoningResult(false)
                        .build();

        // 使用 AgentScope 的 stream API
        Flux<Event> eventFlux = customerServiceAgent.stream(message, streamOptions);

        return eventFlux.subscribeOn(Schedulers.boundedElastic())
            .flatMap(
                    event -> {
                        // Determine event type
                        if (event.getType() == EventType.TOOL_RESULT) {
                            // Tool result event
                            String toolsContent = MsgUtils.getToolsContent(event.getMessage());
                            return Flux.just(
                                    ServerSentEvent.<String>builder()
                                            .event("tool_result")
                                            .data(toolsContent)
                                            .build()
                            );
                        } else if (event.getType() == EventType.REASONING) {
                            // Reasoning event - may contain both ThinkingBlock and TextBlock
                            String thinking = event.getMessage().getContent().stream()
                                    .filter(block -> block instanceof ThinkingBlock)
                                    .map(block -> ((ThinkingBlock) block).getThinking())
                                    .collect(Collectors.joining("\n"));

                            String text = event.getMessage().getContent().stream()
                                    .filter(block -> block instanceof TextBlock)
                                    .map(block -> ((TextBlock) block).getText())
                                    .collect(Collectors.joining("\n"));

                            // Create a flux of SSE events
                            List<ServerSentEvent<String>> events = new ArrayList<>();

                            // Only add reasoning if it's not empty (allow whitespace)
                            if (thinking != null && !thinking.isEmpty()) {
                                events.add(
                                        ServerSentEvent.<String>builder()
                                                .event("reasoning")
                                                .data(toJson(thinking))
                                                .build()
                                );
                            }

                            // Only add content if it's not empty (allow whitespace)
                            if (text != null && !text.isEmpty()) {
                                events.add(
                                        ServerSentEvent.<String>builder()
                                                .event("content")
                                                .data(toJson(text))
                                                .build()
                                );
                            }                                    
                            return Flux.fromIterable(events);
                        } else {                                        
                            // Other event types - treat as content
                            String textContent = MsgUtils.getTextContent(event.getMessage());
                            return Flux.just(
                                    ServerSentEvent.<String>builder()
                                            .event("content")
                                            .data(toJson(textContent))
                                            .build()
                            );
                        }
                    })            .filter(sseEvent -> sseEvent.data() != null && !sseEvent.data().isEmpty());
    }

    /**
     * 健康检查端点
     */
    @GetMapping("/health")
    public Mono<Map<String, String>> health() {
        return Mono.just(Map.of(
                "status", "UP",
                "service", "Monitor Agent WebFlux Streaming"
        ));
    }

    /**
     * 获取当前监控状态
     */
    @GetMapping("/monitor/status")
    public Mono<MonitorStatus> getMonitorStatus() {
        return Mono.fromCallable(monitorService::getCurrentStatus);
    }

    /**
     * 重置指定会话
     */
    @PostMapping("/session/reset/{caseId}")
    public Mono<Map<String, String>> resetSession(@PathVariable String caseId) {
        agentSessions.remove(caseId);
        log.info("已重置会话: {}", caseId);
        return Mono.just(Map.of(
                "status", "success",
                "message", "Session " + caseId + " has been reset"
        ));
    }

    private String toJson(String content) {
        try {
            return objectMapper.writeValueAsString(content);
        } catch (Exception e) {
            log.error("Failed to serialize content to JSON", e);
            // Fallback: simple escaping (incomplete but better than crashing)
            return "\"" + content.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\"";
        }
    }
}
