package io.agentscope.monitor.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Action triggered DTO for tracking system actions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionTriggered {

    /**
     * Feishu webhook status
     */
    @JsonProperty("feishu_webhook")
    private String feishuWebhook;

    /**
     * Apifox document ID
     */
    @JsonProperty("apifox_doc_id")
    private String apifoxDocId;
}
