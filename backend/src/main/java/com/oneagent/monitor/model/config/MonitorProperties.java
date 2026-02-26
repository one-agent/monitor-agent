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
     * Uptime Kuma 配置
     */
    private UptimeKumaConfig uptimeKuma = new UptimeKumaConfig();

    /**
     * Studio 配置
     */
    private StudioConfig studio = new StudioConfig();

    /**
     * CORS 配置
     */
    private CorsConfig cors = new CorsConfig();

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

    /**
     * Uptime Kuma 配置
     */
    @Data
    public static class UptimeKumaConfig {
        /**
         * 是否启用 Uptime Kuma 集成
         */
        private boolean enabled = false;

        /**
         * Uptime Kuma 基础 URL
         */
        private String baseUrl = "http://localhost:3001";

        /**
         * Uptime Kuma API Token（用于轮询）
         */
        private String apiToken;

        /**
         * 是否启用轮询模式
         */
        private boolean pollingEnabled = false;

        /**
         * 轮询间隔（秒）
         */
        private int pollingInterval = 60;

        /**
         * 告警去重时间窗口（秒）
         * 同一监控项在此时间内只发送一次告警
         */
        private int alertDedupeWindow = 300;

        /**
         * Webhook 验证密钥（可选）
         * 如果配置，则验证 Uptime Kuma Webhook 请求头中的密钥
         */
        private String webhookSecret;
    }

    /**
     * CORS 配置
     */
    @Data
    public static class CorsConfig {
        /**
         * 允许的来源列表，逗号分隔
         * 例如: http://localhost:5173,http://example.com
         * 空值或 * 表示允许所有来源
         */
        private String allowedOrigins = "*";
    }
}