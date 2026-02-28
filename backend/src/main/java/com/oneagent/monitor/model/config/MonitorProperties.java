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
     * 日志告警配置
     */
    private LogAlertConfig logAlert = new LogAlertConfig();

    /**
     * 历史故障记录配置
     */
    private FaultHistoryConfig faultHistory = new FaultHistoryConfig();

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
     * Uptime Kuma 配置
     */
    @Data
    public static class UptimeKumaConfig {
        /**
         * 是否启用 Uptime Kuma 集成
         */
        private boolean enabled = false;

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
     * 日志告警配置
     */
    @Data
    public static class LogAlertConfig {
        /**
         * 是否启用日志告警
         */
        private boolean enabled = false;

        /**
         * Webhook 验证密钥（可选）
         * 如果配置，则验证 Alertmanager Webhook 请求头中的密钥
         */
        private String webhookSecret;

        /**
         * 告警去重时间窗口（秒）
         * 基于错误指纹的去重，同一错误在此时间内只发送一次告警
         */
        private int dedupeWindow = 300;

        /**
         * 最小告警日志级别
         * ERROR, WARNING, INFO, DEBUG
         */
        private String minLevel = "ERROR";

        /**
         * 告警关键字（逗号分隔）
         * 日志消息包含这些关键字时才会触发告警
         */
        private String keywords = "Exception,Failed,Timeout,Error,Refused,Denied";

        /**
         * 频率阈值（可选）
         * 同一错误在指定时间内出现多少次才触发告警
         * 0 表示不启用频率检查
         */
        private int frequencyThreshold = 0;
    }

    /**
     * 历史故障记录配置
     */
    @Data
    public static class FaultHistoryConfig {
        /**
         * 存储方式（apifox, memory）
         */
        private String storage = "apifox";

        /**
         * 最大保留天数
         * 超过此天数的故障记录将被清理
         */
        private int maxDays = 30;

        /**
         * 最大保留记录数
         * 超过此数量的旧记录将被清理
         */
        private int maxRecords = 1000;
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