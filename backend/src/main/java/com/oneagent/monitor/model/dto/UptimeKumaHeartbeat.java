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
     * 1 = DEGRADED (降级)
     * 2 = UP (正常)
     */
    @JsonProperty("status")
    private Integer status;
    
    /**
     * 心跳时间
     */
    @JsonProperty("time")
    private String time;
    
    /**
     * 消息内容
     */
    @JsonProperty("msg")
    private String msg;
    
    /**
     * 是否为重要心跳
     */
    @JsonProperty("important")
    private Boolean important;
    
    /**
     * 持续时间（毫秒）
     */
    @JsonProperty("duration")
    private Integer duration;
    
    /**
     * 响应时间（毫秒）
     */
    @JsonProperty("ping")
    private Integer ping;
}