package com.oneagent.monitor.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Monitor status entity
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonitorStatus {

    /**
     * API status code (e.g., "200 OK", "500 Internal Server Error")
     */
    private String status;

    /**
     * Response time (e.g., "120ms", "Timeout")
     */
    private String responseTime;

    /**
     * Whether system is healthy
     */
    private boolean healthy;

    /**
     * Number of recent errors
     */
    private int errorCount;

    /**
     * Timestamp of last check
     */
    private String lastCheckTime;
}
