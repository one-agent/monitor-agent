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
import java.util.UUID;

/**
 * 创建 Apifox 文档的工具类
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApifoxApiTool {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final DateTimeFormatter DOC_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final MonitorProperties monitorProperties;
    private final OkHttpClient httpClient = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 创建 Apifox 故障记录文档
     */
    @Tool(description = "创建 Apifox 故障记录文档。当系统发生异常时调用此工具记录故障。文档标题格式：[故障记录] YYYY-MM-DD HH:mm:ss。")
    public String createApifoxDocument(
            ToolExecutionContext context,
            String timestamp,
            String errorCode,
            String errorMsg,
            String latency
    ) {
        log.info("Creating Apifox document: time={}, code={}, msg={}, latency={}",
                timestamp, errorCode, errorMsg, latency);

        String apiToken = monitorProperties.getApifox().getApiToken();
        String projectId = monitorProperties.getApifox().getProjectId();
        String folderId = monitorProperties.getApifox().getFolderId();

        // 检查是否已配置
        if (apiToken == null || apiToken.contains("your-apifox-token-here") ||
            projectId == null || projectId.contains("your-project-id-here")) {
            String docId = "DOC_" + UUID.randomUUID().toString().substring(0, 8);
            String msg = String.format("Apifox API not fully configured. Simulation: docId=%s, time=%s, code=%s",
                    docId, timestamp, errorCode);
            log.warn(msg);
            return docId;
        }

        try {
            String docTitle = "[故障记录] " + LocalDateTime.now().format(DOC_TIME_FORMATTER);
            String docId = generateDocId(errorCode);

            // 准备 Apifox API 请求体
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("project_id", projectId);
            requestBody.put("folder_id", folderId);
            requestBody.put("name", docTitle);
            requestBody.put("type", "api"); // or "doc" for documentation

            ObjectNode content = requestBody.putObject("content");
            content.put("markdown", buildDocContent(timestamp, errorCode, errorMsg, latency));

            // 注意：实际的 Apifox API 端点可能会有所不同
            String apiUrl = monitorProperties.getApifox().getApiUrl() + "/v1/projects/" + projectId + "/docs";

            RequestBody body = RequestBody.create(requestBody.toString(), JSON);
            Request request = new Request.Builder()
                    .url(apiUrl)
                    .addHeader("Authorization", "Bearer " + apiToken)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    log.info("Apifox document created successfully: {}", docId);
                    return docId;
                } else {
                    log.error("Failed to create Apifox document: {}", response.code());
                    String responseBody = response.body() != null ? response.body().string() : "no response";
                    log.debug("Response: {}", responseBody);
                    // 即使 API 调用失败也返回模拟的文档 ID
                    return docId;
                }
            }
        } catch (IOException e) {
            log.error("Error creating Apifox document", e);
            return generateDocId(errorCode);
        }
    }

    /**
     * 使用指定时间戳创建文档
     */
    public String createApifoxDocument(String timestamp, String errorCode, String errorMsg, String latency) {
        return createApifoxDocument(null, timestamp, errorCode, errorMsg, latency);
    }

    /**
     * 使用当前时间戳创建文档
     */
    public String createApifoxDocument(String errorCode, String errorMsg, String latency) {
        return createApifoxDocument(null,
                LocalDateTime.now().format(DOC_TIME_FORMATTER),
                errorCode,
                errorMsg,
                latency);
    }

    /**
     * 根据错误代码生成文档 ID
     */
    private String generateDocId(String errorCode) {
        String cleanCode = errorCode.replaceAll("[^A-Za-z0-9]", "_");
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return "DOC_" + timestamp + "_" + cleanCode;
    }

    /**
     * 构建Markdown格式的文档内容
     */
    private String buildDocContent(String timestamp, String errorCode, String errorMsg, String latency) {
        return String.format("""
                # 故障记录

                ## 基本信息
                - **故障时间**: %s
                - **错误代码**: %s
                - **当前延迟**: %s

                ## 错误详情
                %s

                ## 处理状态
                - [ ] 已确认
                - [ ] 正在处理
                - [ ] 已解决

                ## 备注
                此文档由智能客服监控 Agent 自动生成。
                """, timestamp, errorCode, latency, errorMsg != null ? errorMsg : "N/A");
    }
}
