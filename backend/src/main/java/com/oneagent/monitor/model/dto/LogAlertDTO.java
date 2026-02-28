package com.oneagent.monitor.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 日志告警数据传输对象
 * 接收来自 Alertmanager 的日志告警数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogAlertDTO {
    /**
     * 接收器名称
     */
    private String receiver;

    /**
     * 告警状态（firing, resolved）
     */
    private String status;

    /**
     * 告警列表
     */
    private List<Alert> alerts;

    /**
     * 分组标签
     */
    private Map<String, String> groupLabels;

    /**
     * 通用标签
     */
    private Map<String, String> commonLabels;

    /**
     * 通用注解
     */
    private Map<String, String> commonAnnotations;

    /**
     * 外部 URL（Alertmanager 界面链接）
     */
    private String externalURL;

    /**
     * 版本
     */
    private String version;

    /**
     * 分组键
     */
    private String groupKey;

    /**
     * 截止时间
     */
    private String truncatedAlerts;

    /**
     * 单个告警
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Alert {
        /**
         * 告警状态
         */
        private String status;

        /**
         * 标签（service, severity, log_level 等）
         */
        private Map<String, String> labels;

        /**
         * 注解（summary, description, log_message, stack_trace 等）
         */
        private Map<String, String> annotations;

        /**
         * 开始时间
         */
        private String startsAt;

        /**
         * 结束时间
         */
        private String endsAt;

        /**
         * 生成器 URL
         */
        private String generatorURL;

        /**
         * 指纹
         */
        private String fingerprint;

        /**
         * 获取服务名称
         */
        public String getService() {
            return labels != null ? labels.getOrDefault("service", "unknown") : "unknown";
        }

        /**
         * 获取日志级别
         */
        public String getLogLevel() {
            return labels != null ? labels.getOrDefault("log_level", "INFO") : "INFO";
        }

        /**
         * 获取严重程度
         */
        public String getSeverity() {
            return labels != null ? labels.getOrDefault("severity", "warning") : "warning";
        }

        /**
         * 获取摘要
         */
        public String getSummary() {
            return annotations != null ? annotations.getOrDefault("summary", "") : "";
        }

        /**
         * 获取描述
         */
        public String getDescription() {
            return annotations != null ? annotations.getOrDefault("description", "") : "";
        }

        /**
         * 获取日志消息
         */
        public String getLogMessage() {
            return annotations != null ? annotations.getOrDefault("log_message", "") : "";
        }

        /**
         * 获取堆栈跟踪
         */
        public String getStackTrace() {
            return annotations != null ? annotations.getOrDefault("stack_trace", "") : "";
        }

        /**
         * 获取开始时间（格式化）
         */
        public String getStartTimeFormatted() {
            return startsAt != null ? startsAt.replace("T", " ").substring(0, Math.min(19, startsAt.length())) : "";
        }

        /**
         * 判断是否为恢复通知
         */
        public boolean isResolved() {
            return "resolved".equalsIgnoreCase(status);
        }

        /**
         * 判断是否需要告警
         */
        public boolean needsAlert() {
            return "firing".equalsIgnoreCase(status);
        }
    }

    /**
     * 获取第一个告警（简化处理）
     */
    public Alert getFirstAlert() {
        return alerts != null && !alerts.isEmpty() ? alerts.get(0) : null;
    }

    /**
     * 判断是否为恢复通知
     */
    public boolean isResolved() {
        return "resolved".equalsIgnoreCase(status);
    }

    /**
     * 判断是否需要告警
     */
    public boolean needsAlert() {
        return "firing".equalsIgnoreCase(status);
    }

    /**
     * 获取服务名称（从第一个告警）
     */
    public String getService() {
        Alert firstAlert = getFirstAlert();
        return firstAlert != null ? firstAlert.getService() : "unknown";
    }

    /**
     * 获取日志级别（从第一个告警）
     */
    public String getLogLevel() {
        Alert firstAlert = getFirstAlert();
        return firstAlert != null ? firstAlert.getLogLevel() : "INFO";
    }

    /**
     * 获取严重程度（从第一个告警）
     */
    public String getSeverity() {
        Alert firstAlert = getFirstAlert();
        return firstAlert != null ? firstAlert.getSeverity() : "warning";
    }
}