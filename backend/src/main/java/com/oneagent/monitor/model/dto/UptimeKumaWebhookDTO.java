package com.oneagent.monitor.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Uptime Kuma Webhook 数据传输对象
 * 接收 Uptime Kuma 发送的 Webhook 通知
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UptimeKumaWebhookDTO {
    
    /**
     * 心跳数据
     */
    @JsonProperty("heartbeat")
    private UptimeKumaHeartbeat heartbeat;
    
    /**
     * 监控项数据
     */
    @JsonProperty("monitor")
    private UptimeKumaMonitor monitor;
    
    /**
     * 格式化的消息
     * 例如："[Testing] [🔴 Down] PING failed..."
     */
    @JsonProperty("msg")
    private String msg;
    
    /**
     * 获取监控项的唯一标识
     * 
     * @return 监控项 ID 字符串
     */
    public String getMonitorIdStr() {
        return heartbeat != null ? String.valueOf(heartbeat.getMonitorId()) : "unknown";
    }
    
    /**
     * 获取告警级别
     * 
     * @return 根据 heartbeat.status 判断的告警级别
     */
    public AlertLevel getAlertLevel() {
        return heartbeat != null 
                ? AlertLevel.fromStatus(heartbeat.getStatus()) 
                : AlertLevel.WARNING;
    }
    
    /**
     * 获取监控项名称
     * 
     * @return 监控项名称，如果不存在则返回 "Unknown"
     */
    public String getMonitorName() {
        return monitor != null ? monitor.getName() : "Unknown Monitor";
    }
    
    /**
     * 获取监控 URL
     * 
     * @return 监控 URL，如果不存在则返回 ""
     */
    public String getMonitorUrl() {
        if (monitor != null) {
            return monitor.getUrl() != null ? monitor.getUrl() : "";
        }
        return "";
    }
    
    /**
     * 获取监控类型
     * 
     * @return 监控类型，如果不存在则返回 "unknown"
     */
    public String getMonitorType() {
        return monitor != null ? monitor.getType() : "unknown";
    }
    
    /**
     * 获取响应时间（毫秒）
     * 
     * @return 响应时间，如果不存在则返回 0
     */
    public Integer getResponseTime() {
        if (heartbeat != null) {
            return heartbeat.getPing() != null ? heartbeat.getPing() : 0;
        }
        return 0;
    }
    
    /**
     * 获取错误消息
     * 
     * @return 错误消息，如果不存在则返回 "N/A"
     */
    public String getErrorMessage() {
        if (heartbeat != null && heartbeat.getMsg() != null) {
            return heartbeat.getMsg();
        }
        return "N/A";
    }
    
    /**
     * 获取心跳时间
     * 
     * @return 心跳时间，如果不存在则返回 ""
     */
    public String getHeartbeatTime() {
        return heartbeat != null ? heartbeat.getTime() : "";
    }
    
    /**
     * 判断是否为恢复通知
     * 
     * @return 如果状态为 UP (status = 2) 返回 true
     */
    public boolean isRecovery() {
        return getAlertLevel().isRecovery();
    }
    
    /**
     * 判断是否需要发送告警
     * 
     * @return 如果不是恢复状态返回 true
     */
    public boolean needsAlert() {
        return getAlertLevel().needsAlert();
    }
}