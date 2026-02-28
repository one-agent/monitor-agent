package com.oneagent.monitor.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 监控日志条目 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonitorLog {

    /**
     * 日志条目的时间戳
     */
    @JsonProperty("timestamp")
    private String timestamp;

    /**
     * 状态（例如："Error"、"OK"）
     */
    @JsonProperty("status")
    private String status;

    /**
     * 错误或状态消息
     */
    @JsonProperty("msg")
    private String msg;

    /**
     * 监控项 ID
     */
    @JsonProperty("monitorId")
    private String monitorId;

    /**
     * 监控项名称
     */
    @JsonProperty("monitorName")
    private String monitorName;

    /**
     * 日志级别（ERROR, WARN, INFO, DEBUG）
     * 用于日志监控功能
     */
    @JsonProperty("logLevel")
    private String logLevel;

    /**
     * 服务名称
     * 用于日志监控功能
     */
    @JsonProperty("service")
    private String service;

    /**
     * 堆栈跟踪信息
     * 用于日志监控功能
     */
    @JsonProperty("stackTrace")
    private String stackTrace;

    /**
     * 日志来源（uptime-kuma, alertmanager 等）
     * 用于区分不同的监控源
     */
    @JsonProperty("source")
    private String source;

    /**
     * 错误指纹（用于去重和相似故障检索）
     */
    @JsonProperty("fingerprint")
    private String fingerprint;

    /**
     * 异常类型（从消息中提取）
     */
    @JsonProperty("exceptionType")
    private String exceptionType;

    /**
     * 判断是否为错误日志
     */
    public boolean isError() {
        return "Error".equalsIgnoreCase(status) ||
               "0".equals(status) ||
               "ERROR".equalsIgnoreCase(logLevel);
    }

    /**
     * 判断是否为正常状态
     */
    public boolean isHealthy() {
        return "OK".equalsIgnoreCase(status) ||
               "1".equals(status) ||
               "INFO".equalsIgnoreCase(logLevel);
    }
}
