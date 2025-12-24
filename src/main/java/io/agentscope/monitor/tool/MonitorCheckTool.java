package io.agentscope.monitor.tool;

import io.agentscope.core.tool.Tool;
import io.agentscope.monitor.model.dto.MonitorLog;
import io.agentscope.monitor.model.dto.MonitorStatus;
import io.agentscope.monitor.service.MonitorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Tool for checking system monitor status
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MonitorCheckTool {

    private final MonitorService monitorService;

    /**
     * Check current system monitor status
     */
    @Tool(description = "检查系统监控状态。用于获取当前的 API 状态和监控日志。返回包含状态码、响应时间和可用性信息的监控状态对象。")
    public MonitorStatus checkMonitorStatus() {
        log.info("Checking monitor status");

        MonitorStatus status = monitorService.getCurrentStatus();
        log.debug("Monitor status: {}", status);
        return status;
    }

    /**
     * Get recent monitor log entries
     */
    @Tool(description = "获取最近的监控日志记录。用于回答用户关于系统稳定性的问题。返回包含时间戳、状态和错误消息的日志列表。")
    public List<MonitorLog> getMonitorLogs() {
        log.info("Getting monitor logs");
        List<MonitorLog> logs = monitorService.getRecentLogs();
        log.debug("Monitor logs count: {}", logs.size());
        return logs;
    }

    /**
     * Check if API is healthy (status 200 OK)
     */
    @Tool(description = "检查 API 是否健康。如果状态码是 200 OK 则返回 true，否则返回 false。")
    public boolean isApiHealthy() {
        MonitorStatus status = monitorService.getCurrentStatus();
        return "200 OK".equalsIgnoreCase(status.getStatus());
    }
}
