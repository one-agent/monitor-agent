package io.agentscope.monitor.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Monitor log entry DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonitorLog {

    /**
     * Timestamp of the log entry
     */
    @JsonProperty("timestamp")
    private String timestamp;

    /**
     * Status (e.g., "Error", "OK")
     */
    @JsonProperty("status")
    private String status;

    /**
     * Error or status message
     */
    @JsonProperty("msg")
    private String msg;
}
