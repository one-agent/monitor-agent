package com.oneagent.monitor.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 告警级别枚举
 * 根据 Uptime Kuma 的 heartbeat.status 值定义
 */
@Getter
@AllArgsConstructor
public enum AlertLevel {
    /**
     * 严重告警 - 服务完全不可用 (status = 0)
     */
    DOWN(0, "严重告警", "red", "🔴"),

    /**
     * 警告告警 - 服务降级或部分可用 (status = 1)
     */
    WARNING(1, "警告告警", "orange", "🟠"),

    /**
     * 信息通知 - 服务恢复正常 (status = 2)
     */
    INFO(2, "恢复通知", "green", "🟢");

    private final int statusCode;
    private final String description;
    private final String color;
    private final String emoji;

    /**
     * 根据 Uptime Kuma 的 status 值获取告警级别
     *
     * @param status Uptime Kuma heartbeat.status 值
     * @return 对应的告警级别
     */
    public static AlertLevel fromStatus(int status) {
        return switch (status) {
            case 0 -> DOWN;      // 0 = DOWN (服务完全不可用)
            case 1 -> WARNING;   // 1 = UP (服务正常，但可能有问题)
            case 2 -> INFO;      // 2 = DEGRADED (服务降级) 或恢复通知
            default -> WARNING;
        };
    }

    /**
     * 判断是否为恢复状态
     * 
     * @return 如果是 INFO 级别（状态恢复）返回 true
     */
    public boolean isRecovery() {
        return this == INFO;
    }

    /**
     * 判断是否需要发送告警
     * 
     * @return 如果不是 INFO 级别（需要告警）返回 true
     */
    public boolean needsAlert() {
        return this != INFO;
    }
}