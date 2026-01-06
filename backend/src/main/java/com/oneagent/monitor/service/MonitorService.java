package com.oneagent.monitor.service;

import com.oneagent.monitor.model.dto.MonitorLog;
import com.oneagent.monitor.model.entity.MonitorStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 监控系统状态的服务类
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
     * 检查 API 状态并判断是否需要告警
     */
    public boolean needsAlert(String apiStatus) {
        return apiStatus != null && !"200 OK".equalsIgnoreCase(apiStatus);
    }

    /**
     * 更新当前监控状态
     */
    public void updateStatus(String apiStatus, String responseTime, List<MonitorLog> logs) {
        this.currentStatus = MonitorStatus.builder()
                .status(apiStatus)
                .responseTime(responseTime)
                .healthy("200 OK".equalsIgnoreCase(apiStatus))
                .errorCount(logs != null ? logs.size() : 0)
                .lastCheckTime(LocalDateTime.now().format(TIME_FORMATTER))
                .build();

        // 如果提供了日志则更新
        synchronized (monitorLogs) {
            if (logs != null && !logs.isEmpty()) {
                monitorLogs.addAll(logs);
            }else{
                if("200 OK".equalsIgnoreCase(apiStatus)){
                    monitorLogs.clear();
                }
            }
        }


        log.debug("监控状态已更新: {}", this.currentStatus);
    }

    /**
     * 获取当前监控状态
     */
    public MonitorStatus getCurrentStatus() {
        if (currentStatus == null) {
            return MonitorStatus.builder()
                    .status("Up")
                    .responseTime("N/A")
                    .healthy(true)
                    .errorCount(0)
                    .lastCheckTime(LocalDateTime.now().format(TIME_FORMATTER))
                    .build();
        }
        return currentStatus;
    }

    /**
     * 获取最近的监控日志
     */
    public List<MonitorLog> getRecentLogs() {
        synchronized (monitorLogs) {
            return new ArrayList<>(monitorLogs);
        }
    }

    /**
     * 添加一条监控日志记录
     */
    public void addLog(MonitorLog monitorLog) {
        synchronized (monitorLogs) {
            monitorLogs.add(monitorLog);
        }
        log.debug("已添加监控日志: {}", log);
    }

    /**
     * 清除所有监控日志
     */
    public void clearLogs() {
        synchronized (monitorLogs) {
            monitorLogs.clear();
        }
        log.info("监控日志已清除");
    }
}
