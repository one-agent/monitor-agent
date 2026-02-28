package com.oneagent.monitor.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 错误日志数据模型
 * 用于存储和传输结构化的错误日志信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorLog {
    /**
     * 错误指纹（用于去重和相似故障检索）
     */
    private String fingerprint;

    /**
     * 时间戳
     */
    private String timestamp;

    /**
     * 本地日期时间
     */
    private LocalDateTime localDateTime;

    /**
     * 日志级别
     */
    private LogAlertLevel logLevel;

    /**
     * 服务名称
     */
    private String service;

    /**
     * 日志来源（alertmanager, uptime-kuma 等）
     */
    private String source;

    /**
     * 错误消息
     */
    private String message;

    /**
     * 堆栈跟踪信息
     */
    private String stackTrace;

    /**
     * 异常类型（从消息中提取）
     */
    private String exceptionType;

    /**
     * 上下文信息（key-value 格式）
     */
    private Map<String, String> context;

    /**
     * 请求 ID（用于追踪）
     */
    private String requestId;

    /**
     * 主机名
     */
    private String hostname;

    /**
     * 端口
     */
    private Integer port;

    /**
     * 监控项 ID（如果是通过监控触发）
     */
    private String monitorId;

    /**
     * 监控项名称
     */
    private String monitorName;

    /**
     * 告警 URL（如果是通过监控触发）
     */
    private String alertUrl;

    /**
     * 发生频率（相同错误在指定时间内的出现次数）
     */
    private Integer frequency;

    /**
     * 判断是否需要发送告警
     * 基于日志级别和消息内容
     *
     * @return 如果需要告警返回 true
     */
    public boolean needsAlert() {
        return logLevel != null && logLevel.needsAlert();
    }

    /**
     * 获取简短的错误摘要（用于飞书告警）
     *
     * @return 错误摘要文本
     */
    public String getSummary() {
        if (message == null || message.isEmpty()) {
            return "未知错误";
        }

        // 截取前 100 个字符
        String summary = message.length() > 100 ? message.substring(0, 100) + "..." : message;

        // 如果有异常类型，添加到摘要中
        if (exceptionType != null && !exceptionType.isEmpty()) {
            summary = "[" + exceptionType + "] " + summary;
        }

        return summary;
    }
}