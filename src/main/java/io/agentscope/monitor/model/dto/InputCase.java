package io.agentscope.monitor.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Input case DTO for processing user queries
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InputCase {

    /**
     * Unique case identifier
     */
    @JsonProperty("case_id")
    private String caseId;

    /**
     * User query/question
     */
    @JsonProperty("user_query")
    private String userQuery;

    /**
     * Current API status
     */
    @JsonProperty("api_status")
    private String apiStatus;

    /**
     * API response time
     */
    @JsonProperty("api_response_time")
    private String apiResponseTime;

    /**
     * Monitor log entries
     */
    @JsonProperty("monitor_log")
    private List<MonitorLog> monitorLog;
}
