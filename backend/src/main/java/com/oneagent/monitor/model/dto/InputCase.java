package com.oneagent.monitor.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 处理用户查询的输入用例 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InputCase {

    /**
     * 唯一的用例标识符
     */
    @JsonProperty("case_id")
    private String caseId;

    /**
     * 用户查询/问题
     */
    @JsonProperty("user_query")
    private String userQuery;

    /**
     * 当前的 API 状态
     */
    @JsonProperty("api_status")
    private String apiStatus;

    /**
     * API 响应时间
     */
    @JsonProperty("api_response_time")
    private String apiResponseTime;

    /**
     * 监控日志条目
     */
    @JsonProperty("monitor_log")
    private List<MonitorLog> monitorLog;


}
