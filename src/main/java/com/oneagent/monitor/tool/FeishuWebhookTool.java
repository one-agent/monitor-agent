package com.oneagent.monitor.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolExecutionContext;
import com.oneagent.monitor.model.config.MonitorProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Tool for sending Feishu webhook alerts
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
     * Send Feishu alert when API error is detected
     */
    @Tool(description = "发送飞书告警通知。当系统检测到 API 异常时调用此工具。消息包含报错时间、错误代码和当前延迟。")
    public String sendFeishuAlert(
            ToolExecutionContext context,
            String timestamp,
            String errorCode,
            String latency
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
                if (response.isSuccessful()) {
                    log.info("Feishu alert sent successfully");
                    return "Sent success";
                } else {
                    log.error("Failed to send Feishu alert: {}", response.code());
                    return "Failed: " + response.code();
                }
            }
        } catch (IOException e) {
            log.error("Error sending Feishu alert", e);
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Send alert with current timestamp
     */
    public String sendFeishuAlert(String errorCode, String latency) {
        return sendFeishuAlert(null, LocalDateTime.now().format(TIME_FORMATTER), errorCode, latency);
    }
}
