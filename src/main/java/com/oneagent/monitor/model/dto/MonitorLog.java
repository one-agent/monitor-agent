package com.oneagent.monitor.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 监控日志条目 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonitorLog {

    /**
     * 日志条目的时间戳
     */
    @JsonProperty("timestamp")
    private String timestamp;

    /**
     * 状态（例如："Error"、"OK"）
     */
    @JsonProperty("status")
    private String status;

    /**
     * 错误或状态消息
     */
    @JsonProperty("msg")
    private String msg;
}
