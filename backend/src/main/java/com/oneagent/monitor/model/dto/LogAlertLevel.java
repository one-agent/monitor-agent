package com.oneagent.monitor.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 日志告警级别枚举
 * 用于 OpenTelemetry + Alertmanager 日志监控
 */
@Getter
@AllArgsConstructor
public enum LogAlertLevel {
    /**
     * 严重错误 - 系统崩溃或核心功能不可用
     */
    CRITICAL(4, "严重错误", "red", "🔴"),

    /**
     * 错误 - 应用程序错误但不影响核心功能
     */
    ERROR(3, "错误", "orange", "🟠"),

    /**
     * 警告 - 潜在问题或异常情况
     */
    WARNING(2, "警告", "yellow", "🟡"),

    /**
     * 信息 - 一般信息日志
     */
    INFO(1, "信息", "green", "🟢");

    private final int level;
    private final String description;
    private final String color;
    private final String emoji;

    /**
     * 从日志级别字符串转换为枚举
     *
     * @param logLevel 日志级别字符串（不区分大小写）
     * @return 对应的日志告警级别
     */
    public static LogAlertLevel fromString(String logLevel) {
        if (logLevel == null) {
            return INFO;
        }

        return switch (logLevel.toUpperCase()) {
            case "CRITICAL", "FATAL" -> CRITICAL;
            case "ERROR", "ERR" -> ERROR;
            case "WARNING", "WARN" -> WARNING;
            case "INFO" -> INFO;
            default -> INFO;
        };
    }

    /**
     * 判断是否需要发送告警
     *
     * @return 如果是 CRITICAL、ERROR 或 WARNING 级别返回 true
     */
    public boolean needsAlert() {
        return this.level >= WARNING.level;
    }

    /**
     * 判断是否为严重错误
     *
     * @return 如果是 CRITICAL 级别返回 true
     */
    public boolean isCritical() {
        return this == CRITICAL;
    }
}