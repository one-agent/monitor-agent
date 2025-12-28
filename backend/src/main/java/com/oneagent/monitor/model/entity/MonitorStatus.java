package com.oneagent.monitor.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 监控状态实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonitorStatus {

    /**
     * API 状态码（例如："200 OK"、"500 Internal Server Error"）
     */
    private String status;

    /**
     * 响应时间（例如："120ms"、"Timeout"）
     */
    private String responseTime;

    /**
     * 系统是否健康
     */
    private boolean healthy;

    /**
     * 最近的错误数量
     */
    private int errorCount;

    /**
     * 最后一次检查的时间戳
     */
    private String lastCheckTime;
}
