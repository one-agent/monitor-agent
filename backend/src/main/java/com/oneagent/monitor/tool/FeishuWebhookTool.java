package com.oneagent.monitor.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolExecutionContext;
import io.agentscope.core.tool.ToolParam;
import com.oneagent.monitor.model.config.MonitorProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 发送飞书 Webhook 告警的工具类
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeishuWebhookTool {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final MonitorProperties monitorProperties;
    private final OkHttpClient httpClient = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 检测到 API 错误时发送飞书告警
     */
    @Tool(name = "send_feishu_alert", description = "发送飞书告警通知。当系统检测到 API 异常时调用此工具。消息包含报错时间、错误代码和当前延迟。")
    public String sendFeishuAlert(
            ToolExecutionContext context,
            @ToolParam(name = "timestamp", description = "告警发生的时间戳") String timestamp,
            @ToolParam(name = "errorCode", description = "异常的错误代码") String errorCode,
            @ToolParam(name = "latency", description = "当前的系统响应延迟") String latency
    ) {
        log.info("Sending Feishu alert: time={}, code={}, latency={}", timestamp, errorCode, latency);

        String webhookUrl = monitorProperties.getFeishu().getWebhookUrl();
        if (webhookUrl == null || webhookUrl.contains("placeholder")) {
            String msg = String.format("Feishu webhook URL not configured. Alert details: time=%s, code=%s, latency=%s",
                    timestamp, errorCode, latency);
            log.warn(msg);
            return "Simulation: " + msg;
        }

        try {
            ObjectNode card = objectMapper.createObjectNode();
            card.put("msg_type", "interactive");

            ObjectNode cardContent = card.putObject("card");
            ObjectNode header = cardContent.putObject("header");
            ObjectNode title = header.putObject("title");
            title.put("tag", "plain_text");
            title.put("content", "🚨 系统异常告警");
            header.put("template", "red");

            ObjectNode element = objectMapper.createObjectNode();
            ObjectNode text = element.putObject("text");
            text.put("tag", "lark_md");
            text.put("content", String.format(
                    "**发生时间**: %s\n**错误代码**: %s\n**当前延迟**: %s",
                    timestamp, errorCode, latency
            ));
            element.put("tag", "div");

            cardContent.set("elements", objectMapper.createArrayNode().add(element));

            RequestBody body = RequestBody.create(card.toString(), JSON);
            Request request = new Request.Builder()
                    .url(webhookUrl)
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String result;
                if (response.isSuccessful()) {
                    log.info("Feishu alert sent successfully");
                    result = "Sent success";
                } else {
                    log.error("Failed to send Feishu alert: {}", response.code());
                    result = "Failed: " + response.code();
                }
                return objectMapper.writeValueAsString(result);
            }
        } catch (IOException e) {
            log.error("Error sending Feishu alert", e);
            return "Error: " + e.getMessage();
        }
    }

    /**
     * 使用指定时间戳发送告警
     */
    public String sendFeishuAlert(String timestamp, String errorCode, String latency) {
        return sendFeishuAlert(null, timestamp, errorCode, latency);
    }

    /**
     * 使用当前时间戳发送告警
     */
    public String sendFeishuAlert(String errorCode, String latency) {
        return sendFeishuAlert(null, LocalDateTime.now().format(TIME_FORMATTER), errorCode, latency);
    }

    /**
     * 发送带告警级别的飞书告警（用于 Uptime Kuma）
     *
     * @param timestamp 时间戳
     * @param errorCode 错误代码
     * @param latency 响应延迟
     * @param alertLevel 告警级别
     * @param monitorName 监控项名称
     * @param monitorUrl 监控项 URL
     * @param monitorType 监控项类型
     * @param errorMsg 错误消息
     * @return 发送结果
     */
    public String sendFeishuAlertWithLevel(
            String timestamp,
            String errorCode,
            String latency,
            com.oneagent.monitor.model.dto.AlertLevel alertLevel,
            String monitorName,
            String monitorUrl,
            String monitorType,
            String errorMsg
    ) {
        log.info("Sending Feishu alert with level: monitorName={}, level={}, code={}",
                monitorName, alertLevel, errorCode);

        String webhookUrl = monitorProperties.getFeishu().getWebhookUrl();
        if (webhookUrl == null || webhookUrl.contains("placeholder")) {
            String msg = String.format(
                    "Feishu webhook URL not configured. Alert: monitorName=%s, level=%s, code=%s, latency=%s",
                    monitorName, alertLevel, errorCode, latency);
            log.warn(msg);
            return "Simulation: " + msg;
        }

        try {
            ObjectNode card = objectMapper.createObjectNode();
            card.put("msg_type", "interactive");

            ObjectNode cardContent = card.putObject("card");
            ObjectNode header = cardContent.putObject("header");
            ObjectNode title = header.putObject("title");
            title.put("tag", "plain_text");

            // 根据告警级别设置标题和颜色
            String titleText = String.format("%s %s - %s", alertLevel.getEmoji(), alertLevel.getDescription(), monitorName);
            title.put("content", titleText);
            header.put("template", alertLevel.getColor());

            // 构建告警内容
            StringBuilder contentBuilder = new StringBuilder();
            contentBuilder.append("**监控项名称**: ").append(monitorName).append("\n");

            if (monitorUrl != null && !monitorUrl.isEmpty()) {
                contentBuilder.append("**监控地址**: ").append(monitorUrl).append("\n");
            }

            if (monitorType != null && !monitorType.isEmpty()) {
                contentBuilder.append("**监控类型**: ").append(monitorType).append("\n");
            }

            contentBuilder.append("**发生时间**: ").append(timestamp).append("\n");
            contentBuilder.append("**错误代码**: ").append(errorCode).append("\n");
            contentBuilder.append("**响应延迟**: ").append(latency).append("\n");

            if (errorMsg != null && !errorMsg.isEmpty()) {
                contentBuilder.append("**错误详情**: ").append(errorMsg).append("\n");
            }

            ObjectNode element = objectMapper.createObjectNode();
            ObjectNode text = element.putObject("text");
            text.put("tag", "lark_md");
            text.put("content", contentBuilder.toString());
            element.put("tag", "div");

            cardContent.set("elements", objectMapper.createArrayNode().add(element));

            RequestBody body = RequestBody.create(card.toString(), JSON);
            Request request = new Request.Builder()
                    .url(webhookUrl)
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String result;
                if (response.isSuccessful()) {
                    log.info("Feishu alert sent successfully with level {}", alertLevel);
                    result = "Sent success";
                } else {
                    log.error("Failed to send Feishu alert: {}", response.code());
                    result = "Failed: " + response.code();
                }
                return objectMapper.writeValueAsString(result);
            }
        } catch (IOException e) {
            log.error("Error sending Feishu alert with level", e);
            return "Error: " + e.getMessage();
        }
    }

    /**
     * 发送飞书恢复通知（用于 Uptime Kuma）
     *
     * @param timestamp 恢复时间
     * @param latency 响应延迟
     * @param monitorName 监控项名称
     * @param monitorUrl 监控项 URL
     * @param monitorType 监控项类型
     * @return 发送结果
     */
    public String sendFeishuRecovery(
            String timestamp,
            String latency,
            String monitorName,
            String monitorUrl,
            String monitorType
    ) {
        log.info("Sending Feishu recovery: monitorName={}", monitorName);

        String webhookUrl = monitorProperties.getFeishu().getWebhookUrl();
        if (webhookUrl == null || webhookUrl.contains("placeholder")) {
            String msg = String.format(
                    "Feishu webhook URL not configured. Recovery: monitorName=%s, latency=%s",
                    monitorName, latency);
            log.warn(msg);
            return "Simulation: " + msg;
        }

        try {
            ObjectNode card = objectMapper.createObjectNode();
            card.put("msg_type", "interactive");

            ObjectNode cardContent = card.putObject("card");
            ObjectNode header = cardContent.putObject("header");
            ObjectNode title = header.putObject("title");
            title.put("tag", "plain_text");
            title.put("content", "🟢 服务恢复通知 - " + monitorName);
            header.put("template", "green");

            // 构建恢复通知内容
            StringBuilder contentBuilder = new StringBuilder();
            contentBuilder.append("**监控项名称**: ").append(monitorName).append("\n");

            if (monitorUrl != null && !monitorUrl.isEmpty()) {
                contentBuilder.append("**监控地址**: ").append(monitorUrl).append("\n");
            }

            if (monitorType != null && !monitorType.isEmpty()) {
                contentBuilder.append("**监控类型**: ").append(monitorType).append("\n");
            }

            contentBuilder.append("**恢复时间**: ").append(timestamp).append("\n");
            contentBuilder.append("**响应延迟**: ").append(latency).append("\n");
            contentBuilder.append("**状态**: 服务已恢复正常\n");

            ObjectNode element = objectMapper.createObjectNode();
            ObjectNode text = element.putObject("text");
            text.put("tag", "lark_md");
            text.put("content", contentBuilder.toString());
            element.put("tag", "div");

            cardContent.set("elements", objectMapper.createArrayNode().add(element));

            RequestBody body = RequestBody.create(card.toString(), JSON);
            Request request = new Request.Builder()
                    .url(webhookUrl)
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String result;
                if (response.isSuccessful()) {
                    log.info("Feishu recovery sent successfully");
                    result = "Sent success";
                } else {
                    log.error("Failed to send Feishu recovery: {}", response.code());
                    result = "Failed: " + response.code();
                }
                return objectMapper.writeValueAsString(result);
            }
        } catch (IOException e) {
            log.error("Error sending Feishu recovery", e);
            return "Error: " + e.getMessage();
        }
    }

    /**
     * 发送日志告警（包含分析和解决方案）
     *
     * @param errorLog 错误日志
     * @param analysis AI 分析结果
     * @param solution 解决方案
     * @param historicalCount 历史类似故障数量
     * @return 发送结果
     */
    public String sendLogAlertWithSolution(
            com.oneagent.monitor.model.dto.ErrorLog errorLog,
            String analysis,
            String solution,
            int historicalCount
    ) {
        log.info("Sending log alert with solution: service={}, level={}, exceptionType={}",
                errorLog.getService(), errorLog.getLogLevel(), errorLog.getExceptionType());

        String webhookUrl = monitorProperties.getFeishu().getWebhookUrl();
        if (webhookUrl == null || webhookUrl.contains("placeholder")) {
            String msg = String.format(
                    "Feishu webhook URL not configured. Log alert: service=%s, level=%s, message=%s",
                    errorLog.getService(), errorLog.getLogLevel(), errorLog.getSummary());
            log.warn(msg);
            return "Simulation: " + msg;
        }

        try {
            ObjectNode card = objectMapper.createObjectNode();
            card.put("msg_type", "interactive");

            ObjectNode cardContent = card.putObject("card");
            ObjectNode header = cardContent.putObject("header");
            ObjectNode title = header.putObject("title");
            title.put("tag", "plain_text");

            // 根据日志级别设置标题和颜色
            String titleText = String.format("%s %s - %s",
                    errorLog.getLogLevel().getEmoji(),
                    errorLog.getLogLevel().getDescription(),
                    errorLog.getService());
            title.put("content", titleText);
            header.put("template", errorLog.getLogLevel().getColor());

            // 构建告警内容
            StringBuilder contentBuilder = new StringBuilder();
            contentBuilder.append("**服务名称**: ").append(errorLog.getService()).append("\n");
            contentBuilder.append("**日志级别**: ").append(errorLog.getLogLevel().getDescription()).append("\n");
            contentBuilder.append("**发生时间**: ").append(errorLog.getTimestamp()).append("\n");

            if (errorLog.getExceptionType() != null && !errorLog.getExceptionType().isEmpty()) {
                contentBuilder.append("**异常类型**: ").append(errorLog.getExceptionType()).append("\n");
            }

            contentBuilder.append("**错误消息**: ").append(errorLog.getSummary()).append("\n");

            // 如果有历史类似故障，显示数量
            if (historicalCount > 0) {
                contentBuilder.append("**历史类似故障**: ").append(historicalCount).append(" 条\n");
            }

            // 添加 AI 分析结果
            if (analysis != null && !analysis.isEmpty()) {
                contentBuilder.append("\n---\n");
                contentBuilder.append("**📊 AI 分析**\n");
                contentBuilder.append(analysis).append("\n");
            }

            // 添加解决方案
            if (solution != null && !solution.isEmpty()) {
                contentBuilder.append("\n---\n");
                contentBuilder.append("**💡 解决方案**\n");
                contentBuilder.append(solution).append("\n");
            }

            ObjectNode element = objectMapper.createObjectNode();
            ObjectNode text = element.putObject("text");
            text.put("tag", "lark_md");
            text.put("content", contentBuilder.toString());
            element.put("tag", "div");

            cardContent.set("elements", objectMapper.createArrayNode().add(element));

            RequestBody body = RequestBody.create(card.toString(), JSON);
            Request request = new Request.Builder()
                    .url(webhookUrl)
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String result;
                if (response.isSuccessful()) {
                    log.info("Log alert with solution sent successfully");
                    result = "Sent success";
                } else {
                    log.error("Failed to send log alert: {}", response.code());
                    result = "Failed: " + response.code();
                }
                return objectMapper.writeValueAsString(result);
            }
        } catch (IOException e) {
            log.error("Error sending log alert with solution", e);
            return "Error: " + e.getMessage();
        }
    }
}
