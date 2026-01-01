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
import io.agentscope.core.session.JsonSession;
import io.agentscope.core.session.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * WebFlux 流式响应控制器
 * 使用 Flux<ServerSentEvent> 实现 AgentScope 流式响应
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class WebFluxStreamingController implements InitializingBean {

    private final ChatService chatService;
    private final MonitorService monitorService;
    private final ReActAgent customerServiceAgent;
    private Path sessionPath;
    @Override
    public void afterPropertiesSet(){

        // Set up session path (now using SessionLoader pattern)
        sessionPath =
                Paths.get(
                        System.getProperty("user.home"),
                        ".agentscope",
                        "examples",
                        "web-sessions");
    }

    /**
     * 处理流式请求 - 使用 WebFlux Flux<ServerSentEvent>
     */
    @PostMapping(value = "/process", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> processRequest(@RequestBody InputCase inputCase) {
        Session session = new JsonSession(sessionPath);
        customerServiceAgent.loadIfExists(session, inputCase.getCaseId());
        log.info("处理流式请求: caseId={}", inputCase.getCaseId());

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
            .doFinally(
                    signalType -> {
                        // Save session after completion using SessionLoader
                        customerServiceAgent.saveTo(session, inputCase.getCaseId());
                    })
            .flatMap(
                    event -> {
                        // Determine event type
                        if (event.getType() == EventType.TOOL_RESULT) {
                            // Tool result event
                            String textContent = MsgUtils.getTextContent(event.getMessage());
                            return Flux.just(
                                    ServerSentEvent.<String>builder()
                                            .event("tool_result")
                                            .data(textContent)
                                            .build()
                            );
                        } else if (event.getType() == EventType.REASONING) {
                            // Reasoning event - may contain both ThinkingBlock and TextBlock
                            String thinking = event.getMessage().getContent().stream()
                                    .filter(block -> block instanceof io.agentscope.core.message.ThinkingBlock)
                                    .map(block -> ((io.agentscope.core.message.ThinkingBlock) block).getThinking())
                                    .collect(java.util.stream.Collectors.joining("\n"));

                            String text = event.getMessage().getContent().stream()
                                    .filter(block -> block instanceof io.agentscope.core.message.TextBlock)
                                    .map(block -> ((io.agentscope.core.message.TextBlock) block).getText())
                                    .collect(java.util.stream.Collectors.joining("\n"));

                            // Create a flux of SSE events
                            java.util.List<ServerSentEvent<String>> events = new java.util.ArrayList<>();

                            if (!thinking.isEmpty()) {
                                events.add(
                                        ServerSentEvent.<String>builder()
                                                .event("reasoning")
                                                .data(thinking)
                                                .build()
                                );
                            }

                            if (!text.isEmpty()) {
                                events.add(
                                        ServerSentEvent.<String>builder()
                                                .event("content")
                                                .data(text)
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
                                            .data(textContent)
                                            .build()
                            );
                        }
                    })
            .filter(sseEvent -> sseEvent.data() != null && !sseEvent.data().isEmpty());
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
}
