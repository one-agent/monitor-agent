package com.oneagent.monitor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AgentScope 配置属性
 */
@Data
@ConfigurationProperties(prefix = "agentscope")
public class AgentScopeProperties {

    /**
     * LLM 配置
     */
    private LlmConfig llm = new LlmConfig();

    /**
     * Embedding 配置
     */
    private EmbeddingConfig embedding = new EmbeddingConfig();

    @Data
    public static class LlmConfig {
        private String apiKey;
        private String baseUrl = "http://localhost:11434/v1";
        private String modelName = "gpt-3.5-turbo";
        private Double temperature = 0.7;
        private Integer maxTokens = 2000;
        private Boolean stream = true;
    }

    @Data
    public static class EmbeddingConfig {
        private String apiKey;
        private String baseUrl = "https://api.openai.com/v1";
        private String modelName = "text-embedding-3-small";
        private Boolean enabled = true;
    }
}