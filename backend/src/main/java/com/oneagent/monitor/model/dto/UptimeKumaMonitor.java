package com.oneagent.monitor.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Uptime Kuma Monitor 数据模型
 * 对应 Uptime Kuma Webhook 中的 monitor 对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UptimeKumaMonitor {
    
    /**
     * 监控项 ID
     */
    private Integer id;
    
    /**
     * 监控项名称
     */
    private String name;
    
    /**
     * 监控类型
     * http, ping, tcp, dns, push, steam, gamedig, docker, mqtt
     */
    private String type;

    /**
     * 监控 URL
     */
    private String url;
    
    /**
     * 是否启用
     */
    private Boolean active;

    /**
     * 重试间隔（秒）
     */
    private Integer retryInterval;

    /**
     * 检查间隔（秒）
     */
    private Integer resendInterval;

    /**
     * 端口
     */
    private Integer port;

    /**
     * hostname
     */
    private String hostname;

    /**
     * 最大重试数
     */
    private Integer maxretries;

}