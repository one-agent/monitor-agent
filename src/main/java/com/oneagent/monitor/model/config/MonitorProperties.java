package com.oneagent.monitor.model.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for Monitor Agent
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "monitor")
public class MonitorProperties {

    /**
     * LLM API configuration
     */
    private LlmConfig llm = new LlmConfig();

    /**
     * Feishu webhook configuration
     */
    private FeishuConfig feishu = new FeishuConfig();

    /**
     * Apifox API configuration
     */
    private ApifoxConfig apifox = new ApifoxConfig();

    /**
     * Knowledge base configuration
     */
    private KnowledgeConfig knowledge = new KnowledgeConfig();

    /**
     * Input/Output path configuration
     */
    private PathConfig path = new PathConfig();

    /**
     * LLM Configuration
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
     * Feishu Configuration
     */
    @Data
    public static class FeishuConfig {
        private String webhookUrl;
    }

    /**
     * Apifox Configuration
     */
    @Data
    public static class ApifoxConfig {
        private String apiUrl = "https://api.apifox.com";
        private String apiToken;
        private String projectId;
        private String folderId;
    }

    /**
     * Knowledge Configuration
     */
    @Data
    public static class KnowledgeConfig {
        private String path = "src/main/resources/knowledge";
    }

    /**
     * Path Configuration
     */
    @Data
    public static class PathConfig {
        private String inputPath = "inputs/inputs.json";
        private String outputPath = "outputs/results.json";
    }
}
