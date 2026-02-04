package com.oneagent.monitor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oneagent.monitor.model.config.MonitorProperties;
import com.oneagent.monitor.model.dto.AlertLevel;
import com.oneagent.monitor.model.dto.InputCase;
import com.oneagent.monitor.model.dto.MonitorLog;
import com.oneagent.monitor.model.dto.UptimeKumaHeartbeat;
import com.oneagent.monitor.model.dto.UptimeKumaMonitor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Uptime Kuma 轮询服务
 * 定时轮询 Uptime Kuma API 获取监控状态，作为 Webhook 的备份机制
 */
@Slf4j
@Service
public class UptimeKumaPollingService {

    private final MonitorProperties monitorProperties;
    private final MonitorService monitorService;
    private final ChatService chatService;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    // 记录每个监控项的上一次状态，用于检测状态变化
    private final Map<Integer, Integer> lastStatusMap = new ConcurrentHashMap<>();

    public UptimeKumaPollingService(
            MonitorProperties monitorProperties,
            MonitorService monitorService,
            ChatService chatService) {
        this.monitorProperties = monitorProperties;
        this.monitorService = monitorService;
        this.chatService = chatService;
        this.httpClient = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 定时轮询 Uptime Kuma 监控状态
     * 执行间隔由 monitor.uptime-kuma.polling-interval 配置
     */
    @Scheduled(fixedRateString = "${monitor.uptime-kuma.polling-interval:60}000")
    public void pollUptimeKumaStatus() {
        if (!monitorProperties.getUptimeKuma().isEnabled()
                || !monitorProperties.getUptimeKuma().isPollingEnabled()) {
            return;
        }

        log.debug("开始轮询 Uptime Kuma 监控状态");

        try {
            // 获取所有监控项
            List<UptimeKumaMonitor> monitors = fetchMonitors();
            if (monitors == null || monitors.isEmpty()) {
                log.warn("未获取到任何监控项");
                return;
            }

            // 获取最新的心跳数据
            Map<Integer, UptimeKumaHeartbeat> heartbeatMap = fetchHeartbeats(monitors);

            // 检查每个监控项的状态变化
            for (UptimeKumaMonitor monitor : monitors) {
                if (!monitor.getActive().equals(1)) {
                    continue; // 跳过未启用的监控项
                }

                Integer monitorId = monitor.getId();
                UptimeKumaHeartbeat heartbeat = heartbeatMap.get(monitorId);

                if (heartbeat == null) {
                    log.warn("监控项 {} 没有心跳数据", monitorId);
                    continue;
                }

                Integer currentStatus = heartbeat.getStatus();
                Integer lastStatus = lastStatusMap.get(monitorId);

                // 检测状态变化
                if (lastStatus == null || !lastStatus.equals(currentStatus)) {
                    handleStatusChange(monitor, heartbeat, lastStatus, currentStatus);
                    lastStatusMap.put(monitorId, currentStatus);
                }
            }

        } catch (Exception e) {
            log.error("轮询 Uptime Kuma 时发生错误", e);
        }
    }

    /**
     * 处理监控项状态变化
     */
    private void handleStatusChange(
            UptimeKumaMonitor monitor,
            UptimeKumaHeartbeat heartbeat,
            Integer lastStatus,
            Integer currentStatus) {

        AlertLevel alertLevel = AlertLevel.fromStatus(currentStatus);
        String monitorIdStr = String.valueOf(monitor.getId());

        log.info("检测到监控项状态变化: monitorId={}, monitorName={}, status: {} -> {}, alertLevel={}",
                monitor.getId(), monitor.getName(), lastStatus, currentStatus, alertLevel);

        // 构建监控日志
        MonitorLog monitorLog = MonitorLog.builder()
                .timestamp(heartbeat.getTime())
                .status(String.valueOf(currentStatus))
                .msg(heartbeat.getMsg())
                .build();

        List<MonitorLog> logs = new ArrayList<>();
        logs.add(monitorLog);

        // 构建状态字符串
        String apiStatus;
        switch (alertLevel) {
            case CRITICAL:
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
        String responseTime = heartbeat.getPing() != null && heartbeat.getPing() > 0
                ? heartbeat.getPing() + "ms"
                : "Unknown";

        // 更新监控服务
        monitorService.updateStatus(apiStatus, responseTime, logs);

        // 根据告警级别处理
        if (alertLevel.needsAlert()) {
            // 需要告警
            if (monitorService.shouldSendAlert(monitorIdStr)) {
                // 发送告警
                InputCase alertCase = InputCase.builder()
                        .caseId("UK-POLL-" + monitorIdStr)
                        .userQuery("Uptime Kuma Alert: " + monitor.getName())
                        .apiStatus(apiStatus)
                        .apiResponseTime(responseTime)
                        .monitorLog(logs)
                        .images(new ArrayList<>())
                        .build();

                // 构建简化的 Webhook DTO
                com.oneagent.monitor.model.dto.UptimeKumaWebhookDTO webhookData =
                        com.oneagent.monitor.model.dto.UptimeKumaWebhookDTO.builder()
                                .heartbeat(heartbeat)
                                .monitor(monitor)
                                .msg("[" + monitor.getName() + "] [" + alertLevel.getEmoji() + " " + alertLevel.getDescription() + "] " + heartbeat.getMsg())
                                .build();

                chatService.handleUptimeKumaAlert(alertCase, webhookData);
                log.info("已通过轮询发送 Uptime Kuma 告警: monitorId={}, level={}", monitorIdStr, alertLevel);
            } else {
                log.info("告警被去重机制过滤: monitorId={}", monitorIdStr);
            }
        } else if (alertLevel.isRecovery()) {
            // 恢复通知
            InputCase recoveryCase = InputCase.builder()
                    .caseId("UK-POLL-" + monitorIdStr)
                    .userQuery("Uptime Kuma Recovery: " + monitor.getName())
                    .apiStatus(apiStatus)
                    .apiResponseTime(responseTime)
                    .monitorLog(logs)
                    .images(new ArrayList<>())
                    .build();

            // 构建简化的 Webhook DTO
            com.oneagent.monitor.model.dto.UptimeKumaWebhookDTO webhookData =
                    com.oneagent.monitor.model.dto.UptimeKumaWebhookDTO.builder()
                            .heartbeat(heartbeat)
                            .monitor(monitor)
                            .msg("[" + monitor.getName() + "] [" + alertLevel.getEmoji() + " " + alertLevel.getDescription() + "] " + heartbeat.getMsg())
                            .build();

            chatService.handleUptimeKumaRecovery(recoveryCase, webhookData);
            log.info("已通过轮询发送 Uptime Kuma 恢复通知: monitorId={}", monitorIdStr);
        }
    }

    /**
     * 获取所有监控项
     */
    private List<UptimeKumaMonitor> fetchMonitors() throws Exception {
        String baseUrl = monitorProperties.getUptimeKuma().getBaseUrl();
        String apiToken = monitorProperties.getUptimeKuma().getApiToken();

        if (apiToken == null || apiToken.isEmpty()) {
            log.warn("未配置 Uptime Kuma API Token，无法轮询");
            return new ArrayList<>();
        }

        String url = baseUrl + "/api/monitors";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + apiToken)
                .addHeader("Content-Type", "application/json")
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("获取监控项失败: {}", response.code());
                return new ArrayList<>();
            }

            String responseBody = response.body().string();
            JsonNode jsonNode = objectMapper.readTree(responseBody);

            List<UptimeKumaMonitor> monitors = new ArrayList<>();
            if (jsonNode.isArray()) {
                for (JsonNode node : jsonNode) {
                    UptimeKumaMonitor monitor = objectMapper.treeToValue(node, UptimeKumaMonitor.class);
                    monitors.add(monitor);
                }
            }

            return monitors;
        }
    }

    /**
     * 获取所有监控项的最新心跳数据
     */
    private Map<Integer, UptimeKumaHeartbeat> fetchHeartbeats(List<UptimeKumaMonitor> monitors) throws Exception {
        String baseUrl = monitorProperties.getUptimeKuma().getBaseUrl();
        String apiToken = monitorProperties.getUptimeKuma().getApiToken();

        Map<Integer, UptimeKumaHeartbeat> heartbeatMap = new HashMap<>();

        for (UptimeKumaMonitor monitor : monitors) {
            String url = baseUrl + "/api/monitor/" + monitor.getId() + "/heartbeat";

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + apiToken)
                    .addHeader("Content-Type", "application/json")
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.warn("获取监控项 {} 的心跳数据失败: {}", monitor.getId(), response.code());
                    continue;
                }

                String responseBody = response.body().string();
                JsonNode jsonNode = objectMapper.readTree(responseBody);

                if (jsonNode.isArray() && jsonNode.size() > 0) {
                    // 获取最新的心跳（数组第一个元素）
                    JsonNode latestHeartbeatNode = jsonNode.get(0);
                    UptimeKumaHeartbeat heartbeat = objectMapper.treeToValue(latestHeartbeatNode, UptimeKumaHeartbeat.class);
                    heartbeatMap.put(monitor.getId(), heartbeat);
                }
            }
        }

        return heartbeatMap;
    }
}