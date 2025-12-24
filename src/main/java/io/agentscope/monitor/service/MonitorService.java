package io.agentscope.monitor.service;

import io.agentscope.monitor.model.dto.MonitorLog;
import io.agentscope.monitor.model.entity.MonitorStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for monitoring system status
 */
@Slf4j
@Service
public class MonitorService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Thread-safe in-memory storage for current status
    private volatile MonitorStatus currentStatus;
    // Thread-safe in-memory storage for logs
    private final List<MonitorLog> monitorLogs = new ArrayList<>();

    /**
     * Check API status and determine if alert is needed
     */
    public boolean needsAlert(String apiStatus) {
        return apiStatus != null && !"200 OK".equalsIgnoreCase(apiStatus);
    }

    /**
     * Update current monitor status
     */
    public void updateStatus(String apiStatus, String responseTime, List<MonitorLog> logs) {
        this.currentStatus = MonitorStatus.builder()
                .status(apiStatus)
                .responseTime(responseTime)
                .healthy("200 OK".equalsIgnoreCase(apiStatus))
                .errorCount(logs != null ? logs.size() : 0)
                .lastCheckTime(LocalDateTime.now().format(TIME_FORMATTER))
                .build();

        // Update logs if provided
        if (logs != null && !logs.isEmpty()) {
            synchronized (monitorLogs) {
                monitorLogs.clear();
                monitorLogs.addAll(logs);
            }
        }

        log.debug("Monitor status updated: {}", this.currentStatus);
    }

    /**
     * Get current monitor status
     */
    public MonitorStatus getCurrentStatus() {
        if (currentStatus == null) {
            return MonitorStatus.builder()
                    .status("Unknown")
                    .responseTime("N/A")
                    .healthy(true)
                    .errorCount(0)
                    .lastCheckTime(LocalDateTime.now().format(TIME_FORMATTER))
                    .build();
        }
        return currentStatus;
    }

    /**
     * Get recent monitor logs
     */
    public List<MonitorLog> getRecentLogs() {
        synchronized (monitorLogs) {
            return new ArrayList<>(monitorLogs);
        }
    }

    /**
     * Add a monitor log entry
     */
    public void addLog(MonitorLog log) {
        synchronized (monitorLogs) {
            monitorLogs.add(log);
        }
        log.debug("Added monitor log: {}", log);
    }

    /**
     * Clear all monitor logs
     */
    public void clearLogs() {
        synchronized (monitorLogs) {
            monitorLogs.clear();
        }
        log.info("Monitor logs cleared");
    }
}
