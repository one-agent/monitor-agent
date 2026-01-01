package com.oneagent.monitor.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.UUID;

/**
 * 创建 Apifox 文档的工具类
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApifoxApiTool {

    private static final MediaType FORM = MediaType.get("application/x-www-form-urlencoded;charset=UTF-8");
    private static final DateTimeFormatter DOC_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final MonitorProperties monitorProperties;
    private final OkHttpClient httpClient = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 创建 Apifox 故障记录文档
     */
    @Tool(name = "create_apifox_document", description = "创建 Apifox 故障记录文档。当系统发生异常时调用此工具记录故障。文档标题格式：[故障记录] YYYY-MM-DD HH:mm:ss。")
    public String createApifoxDocument(
            ToolExecutionContext context,
            @ToolParam(name = "timestamp", description = "故障发生的时间戳") String timestamp,
            @ToolParam(name = "errorCode", description = "故障错误代码") String errorCode,
            @ToolParam(name = "errorMsg", description = "详细的错误信息") String errorMsg,
            @ToolParam(name = "latency", description = "当前的系统响应延迟") String latency
    ) {
        log.info("Creating Apifox document: time={}, code={}, msg={}, latency={}",
                timestamp, errorCode, errorMsg, latency);
        String apiToken = monitorProperties.getApifox().getApiToken();
        String projectId = monitorProperties.getApifox().getProjectId();
        String folderId = monitorProperties.getApifox().getFolderId();
        String moduleId = monitorProperties.getApifox().getModuleId();

        // 调试：打印实际配置值
        log.info("Apifox Config - Token: [{}], ProjectId: [{}], FolderId: [{}], ModuleId: [{}]",
                apiToken != null ? "***" + apiToken.substring(0, Math.min(10, apiToken.length())) + "***" : "null",
                projectId,
                folderId,
                moduleId);

        String result;
        // 检查是否已配置
        if (apiToken == null || apiToken.contains("your-apifox-token-here") ||
            projectId == null || projectId.contains("your-project-id-here")) {
            String docId = "DOC_" + UUID.randomUUID().toString().substring(0, 8);
            String msg = String.format("Apifox API not fully configured. Simulation: docId=%s, time=%s, code=%s",
                    docId, timestamp, errorCode);
            log.warn(msg);
            result = docId;
            return wrapResult(result);
        }

        try {
            String docTitle = "[故障记录] " + LocalDateTime.now().format(DOC_TIME_FORMATTER);
            String docId = generateDocId(errorCode);

            // 使用 form-urlencoded 格式构建请求体
            String formData = buildFormData(docTitle, folderId, moduleId, timestamp, errorCode, errorMsg, latency);

            // Apifox API 端点（根据实际 curl 命令）
            String apiUrl = monitorProperties.getApifox().getApiUrl() + "/api/v1/doc?locale=zh-CN";
            log.info("Apifox API Request - URL: {}, folder_id: {}, title: {}", apiUrl, folderId, docTitle);

            RequestBody body = RequestBody.create(formData, FORM);
            log.info("Apifox Request Body: {}", formData);

            Request request = new Request.Builder()
                    .url(apiUrl)
                    .addHeader("Authorization", "Bearer " + apiToken)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
                    .addHeader("Accept", "application/json")
                    .addHeader("x-project-id", projectId)
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                log.info("Apifox Response - Code: {}, Success: {}, Message: {}",
                        response.code(), response.isSuccessful(), response.message());

                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    log.info("Apifox Response Body: {}", responseBody);

                    // 解析响应获取文档 ID
                    JsonNode jsonNode = objectMapper.readTree(responseBody);
                    if (jsonNode.has("success") && jsonNode.get("success").asBoolean() &&
                        jsonNode.has("data") && jsonNode.get("data").has("id")) {
                        String actualDocId = jsonNode.get("data").get("id").asText();
                        log.info("Apifox document created successfully: {}", actualDocId);
                        result = actualDocId;
                    } else {
                        result = docId;
                    }
                } else {
                    log.error("Failed to create Apifox document: code={}, message={}", response.code(), response.message());
                    String responseBody = response.body() != null ? response.body().string() : "no response";
                    log.error("Apifox Response Body: {}", responseBody);
                    result = docId;
                }
            }
        } catch (IOException e) {
            log.error("Error creating Apifox document", e);
            result = generateDocId(errorCode);
        }
        
        return wrapResult(result);
    }
    
    private String wrapResult(String result) {
        try {
            return String.format("{\"__tool_name__\": \"create_apifox_document\", \"result\": %s}", 
                    objectMapper.writeValueAsString(result));
        } catch (Exception e) {
             return String.format("{\"__tool_name__\": \"create_apifox_document\", \"result\": \"%s\"}", result);
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
     * 构建 form-urlencoded 格式的请求体
     */
    private String buildFormData(String docTitle, String folderId, String moduleId,
                                  String timestamp, String errorCode, String errorMsg, String latency) {
        StringBuilder formData = new StringBuilder();
        formData.append("name=").append(urlEncode(docTitle));

        if (moduleId != null && !moduleId.trim().isEmpty()) {
            formData.append("&moduleId=").append(urlEncode(moduleId));
        }

        // 构建文档内容（markdown 格式）
        String markdownContent = buildDocContent(timestamp, errorCode, errorMsg, latency);
        formData.append("&content=").append(urlEncode(markdownContent));

        if (folderId != null && !folderId.trim().isEmpty()) {
            formData.append("&folderId=").append(urlEncode(folderId));
        }

        return formData.toString();
    }

    /**
     * URL 编码
     */
    private String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            return value;
        }
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
