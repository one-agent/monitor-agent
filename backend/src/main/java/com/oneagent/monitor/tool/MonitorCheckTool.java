package com.oneagent.monitor.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.core.tool.Tool;
import com.oneagent.monitor.model.dto.MonitorLog;
import com.oneagent.monitor.service.MonitorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 检查系统监控状态的工具类
 * 只提供查询功能，不触发告警
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MonitorCheckTool {

    private final MonitorService monitorService;
    private final ObjectMapper objectMapper = new ObjectMapper();

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
     * 检查 API 是否健康（基于最近的监控日志）
     */
    @Tool(name = "is_api_healthy", description = "检查 API 是否健康。基于最近的监控日志判断系统状态，如果没有错误日志则返回 true，否则返回 false。")
    public String isApiHealthy() {
        List<MonitorLog> logs = monitorService.checkRecentLogs();
        boolean isHealthy = logs == null || logs.isEmpty();
        try {
            return objectMapper.writeValueAsString(isHealthy);
        } catch (Exception e) {
            log.error("Failed to serialize isApiHealthy result", e);
            return "false";
        }
    }
}
