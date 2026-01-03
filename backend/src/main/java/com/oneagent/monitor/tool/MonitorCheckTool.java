package com.oneagent.monitor.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import com.oneagent.monitor.model.dto.MonitorLog;
import com.oneagent.monitor.model.entity.MonitorStatus;
import com.oneagent.monitor.service.MonitorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 检查系统监控状态的工具类
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MonitorCheckTool {

    private final MonitorService monitorService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 检查当前系统监控状态
     */
    @Tool(name = "check_monitor_status", description = "检查系统监控状态。用于获取当前的 API 状态和监控日志。返回包含状态码、响应时间和可用性信息的监控状态对象。")
    public String checkMonitorStatus() {
        log.info("Checking monitor status");

        MonitorStatus status = monitorService.getCurrentStatus();
        log.debug("Monitor status: {}", status);
        try {
            return objectMapper.writeValueAsString(status);
        } catch (Exception e) {
            log.error("Failed to serialize monitor status", e);
            return "{\"error\": \"Failed to serialize monitor status\"}";
        }
    }

    /**
     * 获取最近的监控日志记录
     */
    @Tool(name = "get_monitor_logs", description = "获取最近的监控日志记录。用于回答用户关于系统稳定性的问题。返回包含时间戳、状态和错误消息的日志列表。")
    public String getMonitorLogs() {
        log.info("Getting monitor logs");
        List<MonitorLog> logs = monitorService.getRecentLogs();
        log.debug("Monitor logs count: {}", logs.size());
        try {
            return objectMapper.writeValueAsString(logs);
        } catch (Exception e) {
            log.error("Failed to serialize monitor logs", e);
            return "[]";
        }
    }

    /**
     * 检查 API 是否健康（状态码为 200 OK）
     */
    @Tool(name = "is_api_healthy", description = "检查 API 是否健康。如果状态码是 200 OK 则返回 true，否则返回 false。")
    public String isApiHealthy() {
        MonitorStatus status = monitorService.getCurrentStatus();
        boolean isHealthy = "200 OK".equalsIgnoreCase(status.getStatus());
        try {
            return objectMapper.writeValueAsString(isHealthy);
        } catch (Exception e) {
            log.error("Failed to serialize isApiHealthy result", e);
            return "false";
        }
    }
}
