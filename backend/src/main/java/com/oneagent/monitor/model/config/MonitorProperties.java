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
     * 飞书 Webhook 配置
     */
    private FeishuConfig feishu = new FeishuConfig();

    /**
     * Apifox API 配置
     */
    private ApifoxConfig apifox = new ApifoxConfig();

    /**
     * Studio 配置
     */
    private StudioConfig studio = new StudioConfig();

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
        private String moduleId;
        private String branchId;
        private String deviceId;
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
