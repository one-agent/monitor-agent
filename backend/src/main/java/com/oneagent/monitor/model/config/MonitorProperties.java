package com.oneagent.monitor.model.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Monitor Agent 的配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "monitor")
public class MonitorProperties {

    /**
     * LLM API 配置
     */
    private LlmConfig llm = new LlmConfig();

    /**
     * 飞书 Webhook 配置
     */
    private FeishuConfig feishu = new FeishuConfig();

    /**
     * Apifox API 配置
     */
    private ApifoxConfig apifox = new ApifoxConfig();

    /**
     * 知识库配置
     */
    private KnowledgeConfig knowledge = new KnowledgeConfig();
    /**
     * Studio 配置
     */
    private StudioConfig studio = new StudioConfig();

    /**
     * 输入/输出路径配置
     */
    private PathConfig path = new PathConfig();

    /**
     * LLM 配置
     */
    @Data
    public static class LlmConfig {
        private String apiKey;
        private String baseUrl = "http://localhost:11434/v1";
        private String modelName = "gpt-3.5-turbo";
        private Double temperature = 0.7;
        private Integer maxTokens = 2000;
        private Boolean stream = false;
    }

    /**
     * 飞书配置
     */
    @Data
    public static class FeishuConfig {
        private String webhookUrl;
    }

    /**
     * Apifox 配置
     */
    @Data
    public static class ApifoxConfig {
        private String apiUrl = "https://api.apifox.com";
        private String apiToken;
        private String projectId;
        private String folderId;
    }

    /**
     * 知识库配置
     */
    @Data
    public static class KnowledgeConfig {
        private String path = "src/main/resources/knowledge";
    }

    /**
     * 路径配置
     */
    @Data
    public static class PathConfig {
        private String inputPath = "inputs/inputs.json";
        private String outputPath = "outputs/results.json";
    }

    /**
     * Studio 配置
     */
    @Data
    public static class StudioConfig {
        /**
         * 是否启用 Studio 支持
         */
        private boolean enabled = false;

        /**
         * Studio 服务端 URL
         */
        private String studioUrl = "http://localhost:3000";

        /**
         * 项目名称（用于 Studio 中的项目标识）
         */
        private String projectName = "MonitorAgent";

        /**
         * 运行名称（用于区分不同的 Agent）
         */
        private String runName = "demo_";
    }
}
