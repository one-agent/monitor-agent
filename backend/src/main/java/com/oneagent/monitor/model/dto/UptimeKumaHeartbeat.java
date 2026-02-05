package com.oneagent.monitor.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Uptime Kuma Heartbeat 数据模型
 * 对应 Uptime Kuma Webhook 中的 heartbeat 对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UptimeKumaHeartbeat {
    
    /**
     * 监控项 ID
     */
    @JsonProperty("monitorID")
    private Integer monitorId;
    
    /**
     * 状态码
     * 0 = DOWN (不可用)
     * 1 = UP (正常)
     * 2 = DEGRADED (降级)
     */
    private Integer status;
    
    /**
     * 心跳时间（ISO 8601 格式）
     * 例如: "2026-02-05T12:00:00.000Z"
     */
    private String time;
    
    /**
     * 消息内容
     */
    private String msg;
    
    /**
     * 响应时间（毫秒）
     */
    private Integer ping;
    
    /**
     * 是否为重要心跳
     */
    private Boolean important;

    /**
     * duration
     *
     */
    private Integer duration;

    /**
     * 响应内容
     */
    private String response;

    /**
     * 时区
     * 例如: "Asia/Shanghai"
     */
     private String timezone;
    /**
     * 时区偏移
     * 例如: "+08:00"
     */
    private String timezoneOffset;

    /**
     * 本地日期时间
     * 例如: "2026-02-05 20:00:00"
     */
    private String localDateTime;
}