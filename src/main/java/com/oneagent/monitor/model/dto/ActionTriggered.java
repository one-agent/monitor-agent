package com.oneagent.monitor.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 跟踪系统动作的触发动作 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionTriggered {

    /**
     * 飞书 Webhook 状态
     */
    @JsonProperty("feishu_webhook")
    private String feishuWebhook;

    /**
     * Apifox 文档 ID
     */
    @JsonProperty("apifox_doc_id")
    private String apifoxDocId;
}
