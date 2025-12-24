package com.oneagent.monitor.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result case DTO for output
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResultCase {

    /**
     * Unique case identifier
     */
    @JsonProperty("case_id")
    private String caseId;

    /**
     * Agent's reply to user query
     */
    @JsonProperty("reply")
    private String reply;

    /**
     * Actions triggered during processing
     */
    @JsonProperty("action_triggered")
    private ActionTriggered actionTriggered;

    public static ResultCase withReply(String caseId, String reply) {
        return new ResultCase(caseId, reply, null);
    }

    public static ResultCase withActions(String caseId, String reply, ActionTriggered actions) {
        return new ResultCase(caseId, reply, actions);
    }
}
