package com.oneagent.monitor.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 输出结果用例 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResultCase {

    /**
     * 唯一的用例标识符
     */
    @JsonProperty("case_id")
    private String caseId;

    /**
     * Agent 对用户查询的回复
     */
    @JsonProperty("reply")
    private String reply;

    /**
     * 处理期间触发的动作
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
