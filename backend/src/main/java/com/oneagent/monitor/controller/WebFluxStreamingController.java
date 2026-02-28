package com.oneagent.monitor.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oneagent.monitor.model.config.MonitorProperties;
import com.oneagent.monitor.model.dto.*;
import com.oneagent.monitor.model.entity.MonitorStatus;
import com.oneagent.monitor.service.ChatService;
import com.oneagent.monitor.service.MonitorService;
import com.oneagent.monitor.session.SessionManager;
import com.oneagent.monitor.util.MsgUtils;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Event;
import io.agentscope.core.agent.EventType;
import io.agentscope.core.agent.StreamOptions;
import io.agentscope.core.message.*;
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
    private final SessionManager sessionManager;
    private final MonitorProperties monitorProperties;

    public WebFluxStreamingController(
            ChatService chatService,
            MonitorService monitorService,
            ObjectProvider<ReActAgent> customerServiceAgentProvider,
            SessionManager sessionManager,
            MonitorProperties monitorProperties) {
        this.chatService = chatService;
        this.monitorService = monitorService;
        this.customerServiceAgentProvider = customerServiceAgentProvider;
        this.sessionManager = sessionManager;
        this.monitorProperties = monitorProperties;
    }

    /**
     * 处理流式请求 - 使用 WebFlux Flux<ServerSentEvent> - 接收JSON请求体（包含图片数据）
     */
    @PostMapping(value = "/process", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> processRequest(@RequestBody InputCase inputCase) {
        log.info("处理流式请求: caseId={}", inputCase.getCaseId());
        log.info("用户查询: {}", inputCase.getUserQuery());
        log.info("图片数量: {}", inputCase.getImages() != null ? inputCase.getImages().size() : 0);

        // 打印图片信息（仅打印前 50 个字符）
        if (inputCase.getImages() != null && !inputCase.getImages().isEmpty()) {
            for (int i = 0; i < inputCase.getImages().size(); i++) {
                String image = inputCase.getImages().get(i);
                String preview = image.length() > 50 ? image.substring(0, 50) + "..." : image;
                log.info("图片[{}]: {}", i, preview);
            }
        }

        try {
            // 使用SessionManager获取或创建Agent实例，并自动加载会话
            ReActAgent customerServiceAgent = sessionManager.getOrCreateAgent(
                    inputCase.getCaseId(),
                    customerServiceAgentProvider.getObject()
            );

            // 构建用户查询 - 包含监控状态信息供 Agent 参考
            String userQuery = buildUserQueryWithMonitorContext(inputCase);

            // 构建消息 - 支持多模态
            Msg message = buildMultimodalMessage(userQuery, inputCase.getImages());

        // Configure streaming options - INCREMENTAL mode for SSE
        StreamOptions streamOptions =
                StreamOptions.builder()
                        .eventTypes(EventType.REASONING, EventType.TOOL_RESULT)
                        .incremental(true)
                        .includeReasoningResult(false)
                        .build();

        // 使用 AgentScope 的 stream API
        Flux<Event> eventFlux = customerServiceAgent.stream(message, streamOptions);

        return eventFlux
                .subscribeOn(Schedulers.boundedElastic())
                .doFinally(
                        signalType -> {
                            // 使用SessionManager保存会话
                            sessionManager.saveSession(inputCase.getCaseId());
                        })
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
                            if (!thinking.isEmpty()) {
                                events.add(
                                        ServerSentEvent.<String>builder()
                                                .event("reasoning")
                                                .data(toJson(thinking))
                                                .build()
                                );
                            }

                            // Only add content if it's not empty (allow whitespace)
                            if (!text.isEmpty()) {
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
        } catch (Exception e) {
            log.error("处理请求时发生错误: {}", e.getMessage(), e);
            String errorMsg = "Error: " + e.getMessage();
            return Flux.just(
                ServerSentEvent.<String>builder()
                    .event("content")
                    .data(toJson(errorMsg))
                    .build()
            );
        }
    }

    /**
     * 构建带有监控上下文的用户查询
     * Agent 可以根据上下文自主决定是否需要调用监控工具
     */
    private String buildUserQueryWithMonitorContext(InputCase inputCase) {
        StringBuilder queryBuilder = new StringBuilder();
        
        // 添加用户查询
        queryBuilder.append(inputCase.getUserQuery());
        
        // 如果输入中包含监控状态信息，添加到上下文中
        if (inputCase.getApiStatus() != null && !"200 OK".equalsIgnoreCase(inputCase.getApiStatus())) {
            queryBuilder.append("\n\n[当前API状态: ").append(inputCase.getApiStatus());
            if (inputCase.getApiResponseTime() != null) {
                queryBuilder.append(", 响应时间: ").append(inputCase.getApiResponseTime());
            }
            queryBuilder.append("]");
            
            // 如果有监控日志，也添加到上下文中
            if (inputCase.getMonitorLog() != null && !inputCase.getMonitorLog().isEmpty()) {
                queryBuilder.append("\n[最近监控日志:");
                for (MonitorLog log : inputCase.getMonitorLog()) {
                    queryBuilder.append("\n  - ").append(log.getTimestamp())
                               .append(": ").append(log.getStatus())
                               .append(" - ").append(log.getMsg());
                }
                queryBuilder.append("]");
            }
            
            // 提示 Agent 可以使用监控工具
            queryBuilder.append("\n\n提示：如果用户询问关于系统状态或错误的问题，可以使用 check_monitor_status、get_monitor_logs 或 handle_uptime_kuma_webhook 工具获取更多信息。");
        }
        
        return queryBuilder.toString();
    }

    /**
     * 构建支持多模态的消息
     */
    private Msg buildMultimodalMessage(String textContent, List<String> base64Images) {
        log.info("开始构建多模态消息，文本长度: {}, 图片数量: {}", textContent.length(), base64Images != null ? base64Images.size() : 0);
        List<ContentBlock> contentBlocks = new ArrayList<>();

        // 添加文本内容
        if (textContent != null && !textContent.isEmpty()) {
            contentBlocks.add(TextBlock.builder().text(textContent).build());
        }

        // 添加图片内容
        if (base64Images != null && !base64Images.isEmpty()) {
            log.info("开始处理 {} 张图片", base64Images.size());
            for (int i = 0; i < base64Images.size(); i++) {
                String base64Image = base64Images.get(i);
                try {
                    log.info("处理图片 [{}], 长度: {}", i, base64Image.length());

                    // 自动检测 MIME 类型
                    String mediaType = "image/png";
                    if (base64Image.startsWith("data:image/")) {
                        int endTypeIndex = base64Image.indexOf(";");
                        if (endTypeIndex > 11) { // "data:image/".length() = 11
                            mediaType = base64Image.substring(5, endTypeIndex); // "data:image/".length() = 5
                        }
                    }
                    log.info("图片 [{}] MIME 类型: {}", i, mediaType);

                    // 提取 base64 数据部分（去掉 "data:image/png;base64," 这样的前缀）
                    String base64Data = base64Image;
                    if (base64Image.contains(",")) {
                        base64Data = base64Image.split(",", 2)[1];
                    }
                    log.info("图片 [{}] Base64 数据长度: {}", i, base64Data.length());

                    ContentBlock imageBlock = ImageBlock.builder()
                        .source(Base64Source.builder()
                            .data(base64Data)
                            .mediaType(mediaType)
                            .build())
                        .build();

                    contentBlocks.add(imageBlock);
                    log.info("图片 [{}] 处理成功", i);
                } catch (Exception e) {
                    log.error("处理图片 [{}] 时出错: {}", i, e.getMessage(), e);
                }
            }
            log.info("所有图片处理完成，共 {} 个图片块", contentBlocks.size());
        }

        return Msg.builder()
                .name("user")
                .role(MsgRole.USER)
                .content(contentBlocks)
                .build();
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
     * 重置指定会话
     */
    @PostMapping("/session/reset/{caseId}")
    public Mono<Map<String, String>> resetSession(@PathVariable String caseId) {
        sessionManager.deleteSession(caseId);
        log.info("已重置会话: {}", caseId);
        return Mono.just(Map.of(
                "status", "success",
                "message", "Session " + caseId + " has been reset"
        ));
    }

    /**
     * Uptime Kuma Webhook 接收端点
     * 接收 Uptime Kuma 发送的监控状态变更通知
     * 在数据采集层直接触发告警
     */
    @PostMapping("/webhook/uptime-kuma")
    public Mono<Map<String, String>> handleUptimeKumaWebhook(
            @RequestBody UptimeKumaWebhookDTO webhookData,
            @RequestHeader(value = "X-Uptime-Kuma-Secret", required = false) String webhookSecret) {
        log.info("收到 Uptime Kuma Webhook: {}", webhookData);

        // 检查 Uptime Kuma 集成是否启用
        if (!monitorProperties.getUptimeKuma().isEnabled()) {
            log.warn("Uptime Kuma 集成未启用，忽略 Webhook 请求");
            return Mono.just(Map.of(
                    "status", "ignored",
                    "message", "Uptime Kuma integration is not enabled"
            ));
        }

        // 验证 Webhook 密钥（如果配置）
        if (monitorProperties.getUptimeKuma().getWebhookSecret() != null
                && !monitorProperties.getUptimeKuma().getWebhookSecret().isEmpty()) {
            if (!monitorProperties.getUptimeKuma().getWebhookSecret().equals(webhookSecret)) {
                log.warn("Webhook 密钥验证失败");
                return Mono.just(Map.of(
                        "status", "error",
                        "message", "Invalid webhook secret"
                ));
            }
        }

        try {
            // 构建监控日志
            MonitorLog monitorLog = MonitorLog.builder()
                    .timestamp(webhookData.getHeartbeatTime())
                    .status(String.valueOf(webhookData.getHeartbeat() != null ? webhookData.getHeartbeat().getStatus() : -1))
                    .msg(webhookData.getErrorMessage())
                    .monitorId(webhookData.getMonitorIdStr())
                    .monitorName(webhookData.getMonitorName())
                    .build();

            List<MonitorLog> logs = new ArrayList<>();
            logs.add(monitorLog);

            // 构建状态字符串
            String apiStatus;
            AlertLevel alertLevel = webhookData.getAlertLevel();
            switch (alertLevel) {
                case DOWN:
                    apiStatus = "503 Service Unavailable";
                    break;
                case WARNING:
                    apiStatus = "502 Bad Gateway";
                    break;
                case INFO:
                    apiStatus = "200 OK";
                    break;
                default:
                    apiStatus = "500 Internal Server Error";
            }
            // 构建响应时间字符串
            String responseTime = webhookData.getResponseTime() > 0
                    ? webhookData.getResponseTime() + "ms"
                    : "Unknown";

            // 更新监控服务（保存到内存）
            monitorService.updateStatus(logs);

            // 根据告警级别触发告警
            ActionTriggered actions = null;
            if (alertLevel.needsAlert()) {
                // 需要告警
                if (monitorService.shouldSendAlert(webhookData.getMonitorIdStr())) {
                    // 发送告警
                    InputCase alertCase = InputCase.builder()
                            .caseId("UK-" + webhookData.getMonitorIdStr())
                            .userQuery("Uptime Kuma Alert: " + webhookData.getMonitorName())
                            .apiStatus(apiStatus)
                            .apiResponseTime(responseTime)
                            .monitorLog(logs)
                            .images(new ArrayList<>())
                            .build();

                    actions = chatService.handleUptimeKumaAlert(alertCase, webhookData);
                    log.info("已发送 Uptime Kuma 告警: monitorId={}, level={}",
                            webhookData.getMonitorIdStr(), alertLevel);
                } else {
                    log.info("告警被去重机制过滤: monitorId={}", webhookData.getMonitorIdStr());
                }
            } else if (alertLevel.isRecovery()) {
                // 恢复通知
                InputCase recoveryCase = InputCase.builder()
                        .caseId("UK-" + webhookData.getMonitorIdStr())
                        .userQuery("Uptime Kuma Recovery: " + webhookData.getMonitorName())
                        .apiStatus(apiStatus)
                        .apiResponseTime(responseTime)
                        .monitorLog(logs)
                        .images(new ArrayList<>())
                        .build();

                actions = chatService.handleUptimeKumaRecovery(recoveryCase, webhookData);
                log.info("已发送 Uptime Kuma 恢复通知: monitorId={}", webhookData.getMonitorIdStr());
            }

            // 构建响应
            Map<String, String> response = new java.util.HashMap<>();
            response.put("status", "success");
            response.put("message", "Uptime Kuma webhook processed successfully");
            response.put("monitorId", webhookData.getMonitorIdStr());
            response.put("alertLevel", alertLevel.name());
            response.put("monitorName", webhookData.getMonitorName());

            if (actions != null) {
                response.put("feishuWebhook", actions.getFeishuWebhook());
                response.put("apifoxDocId", actions.getApifoxDocId());
            }

            return Mono.just(response);

        } catch (Exception e) {
            log.error("处理 Uptime Kuma Webhook 时发生错误", e);
            return Mono.just(Map.of(
                    "status", "error",
                    "message", "Failed to process webhook: " + e.getMessage()
            ));
        }
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

    /**
     * 接收 Alertmanager 发送的日志告警通知
     * 支持组合条件告警：日志级别 + 关键字 + 频率
     */
    @PostMapping("/webhook/log-alert")
    public Mono<Map<String, String>> handleLogAlert(
            @RequestBody com.oneagent.monitor.model.dto.LogAlertDTO logAlertData,
            @RequestHeader(value = "X-Log-Alert-Secret", required = false) String webhookSecret) {
        log.info("收到 Log Alert Webhook: status={}, alerts={}",
                logAlertData.getStatus(), logAlertData.getAlerts() != null ? logAlertData.getAlerts().size() : 0);

        // 检查日志告警集成是否启用
        if (!monitorProperties.getLogAlert().isEnabled()) {
            log.warn("日志告警集成未启用，忽略 Webhook 请求");
            return Mono.just(Map.of(
                    "status", "ignored",
                    "message", "Log alert integration is not enabled"
            ));
        }

        // 验证 Webhook 密钥（如果配置）
        if (monitorProperties.getLogAlert().getWebhookSecret() != null
                && !monitorProperties.getLogAlert().getWebhookSecret().isEmpty()) {
            if (!monitorProperties.getLogAlert().getWebhookSecret().equals(webhookSecret)) {
                log.warn("Log Alert Webhook 密钥验证失败");
                return Mono.just(Map.of(
                        "status", "error",
                        "message", "Invalid webhook secret"
                ));
            }
        }

        try {
            // 检查是否需要处理告警
            if (!logAlertData.needsAlert()) {
                log.info("Log Alert 状态为 resolved，无需处理告警");
                return Mono.just(Map.of(
                        "status", "success",
                        "message", "Log alert is resolved, no action needed"
                ));
            }

            // 解析日志告警数据
            var logAnalysisService = new com.oneagent.monitor.service.LogAnalysisService(monitorProperties);
            List<com.oneagent.monitor.model.dto.ErrorLog> errorLogs = logAnalysisService.parseLogAlert(logAlertData);

            if (errorLogs.isEmpty()) {
                log.warn("解析 Log Alert 失败，没有有效的错误日志");
                return Mono.just(Map.of(
                        "status", "error",
                        "message", "No valid error logs found in alert data"
                ));
            }

            // 处理每个错误日志
            List<Map<String, String>> results = new ArrayList<>();
            for (com.oneagent.monitor.model.dto.ErrorLog errorLog : errorLogs) {
                try {
                    // 生成错误指纹
                    String fingerprint = logAnalysisService.generateErrorFingerprint(errorLog);
                    errorLog.setFingerprint(fingerprint);

                    // 判断是否需要触发告警
                    if (!logAnalysisService.shouldTriggerAlert(errorLog)) {
                        log.debug("错误日志未满足告警条件: service={}, level={}",
                                errorLog.getService(), errorLog.getLogLevel());
                        results.add(Map.of(
                                "status", "skipped",
                                "message", "Alert conditions not met",
                                "fingerprint", fingerprint
                        ));
                        continue;
                    }

                    // 去重检查
                    if (!monitorService.shouldSendLogAlert(fingerprint)) {
                        log.info("日志告警被去重机制过滤: fingerprint={}", fingerprint);
                        results.add(Map.of(
                                "status", "deduplicated",
                                "message", "Alert deduplicated",
                                "fingerprint", fingerprint
                        ));
                        continue;
                    }

                    // 增加错误频率
                    monitorService.incrementErrorFrequency(fingerprint);
                    errorLog.setFrequency(monitorService.getErrorFrequency(fingerprint));

                    // 构建监控日志
                    MonitorLog monitorLog = MonitorLog.builder()
                            .timestamp(errorLog.getTimestamp())
                            .status("ERROR")
                            .msg(errorLog.getSummary())
                            .monitorId(errorLog.getMonitorId())
                            .monitorName(errorLog.getMonitorName())
                            .logLevel(errorLog.getLogLevel().name())
                            .service(errorLog.getService())
                            .stackTrace(errorLog.getStackTrace())
                            .source("alertmanager")
                            .fingerprint(fingerprint)
                            .exceptionType(errorLog.getExceptionType())
                            .build();

                    // 更新监控服务
                    monitorService.updateStatus(List.of(monitorLog));

                    // 触发告警
                    var actions = chatService.handleLogAlert(errorLog);
                    log.info("已发送日志告警: service={}, level={}, fingerprint={}",
                            errorLog.getService(), errorLog.getLogLevel(), fingerprint);

                    results.add(Map.of(
                            "status", "success",
                            "message", "Log alert sent successfully",
                            "fingerprint", fingerprint,
                            "feishuWebhook", actions.getFeishuWebhook(),
                            "apifoxDocId", actions.getApifoxDocId()
                    ));

                } catch (Exception e) {
                    log.error("处理错误日志时出错: {}", errorLog.getSummary(), e);
                    results.add(Map.of(
                            "status", "error",
                            "message", "Failed to process error log: " + e.getMessage(),
                            "summary", errorLog.getSummary()
                    ));
                }
            }

            // 构建响应
            Map<String, String> response = new java.util.HashMap<>();
            response.put("status", "success");
            response.put("message", "Processed " + results.size() + " error logs");

            // 添加第一个成功的结果作为主要结果
            for (Map<String, String> result : results) {
                if ("success".equals(result.get("status"))) {
                    response.putAll(result);
                    break;
                }
            }

            return Mono.just(response);

        } catch (Exception e) {
            log.error("处理 Log Alert Webhook 时发生错误", e);
            return Mono.just(Map.of(
                    "status", "error",
                    "message", "Failed to process webhook: " + e.getMessage()
            ));
        }
    }
}
