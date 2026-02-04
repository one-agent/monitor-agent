package com.oneagent.monitor.service;

import com.oneagent.monitor.model.config.MonitorProperties;
import com.oneagent.monitor.model.dto.MonitorLog;
import com.oneagent.monitor.model.entity.MonitorStatus;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 监控系统状态的服务类
 */
@Slf4j
@Service
public class MonitorService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final MonitorProperties monitorProperties;

    // Thread-safe in-memory storage for current status
    private volatile MonitorStatus currentStatus;
    // Thread-safe in-memory storage for logs
    private final List<MonitorLog> monitorLogs = new ArrayList<>();
    // 告警去重：记录每个监控项的最后告警时间
    private final ConcurrentMap<String, LocalDateTime> lastAlertTimes = new ConcurrentHashMap<>();

    public MonitorService(MonitorProperties monitorProperties) {
        this.monitorProperties = monitorProperties;
    }

    /**
     * 检查 API 状态并判断是否需要告警
     */
    public boolean needsAlert(String apiStatus) {
        return apiStatus != null && !"200 OK".equalsIgnoreCase(apiStatus);
    }

    /**
     * 判断是否应该发送告警（去重检查）
     * 
     * @param monitorId 监控项 ID
     * @return 如果应该发送告警返回 true，否则返回 false
     */
    public boolean shouldSendAlert(String monitorId) {
        int dedupWindowSeconds = monitorProperties.getUptimeKuma().getAlertDedupWindow();
        
        if (dedupWindowSeconds <= 0) {
            // 未配置去重窗口，始终发送告警
            return true;
        }

        LocalDateTime lastAlertTime = lastAlertTimes.get(monitorId);
        LocalDateTime now = LocalDateTime.now();

        if (lastAlertTime == null) {
            // 第一次告警，可以发送
            lastAlertTimes.put(monitorId, now);
            return true;
        }

        // 检查是否超过去重窗口
        long secondsSinceLastAlert = ChronoUnit.SECONDS.between(lastAlertTime, now);
        if (secondsSinceLastAlert >= dedupWindowSeconds) {
            // 超过去重窗口，可以发送告警
            lastAlertTimes.put(monitorId, now);
            return true;
        }

        // 在去重窗口内，不发送告警
        log.debug("监控项 {} 在去重窗口内（{}秒内），跳过告警", monitorId, secondsSinceLastAlert);
        return false;
    }

    /**
     * 清理过期的告警记录
     * 定期清理超过去重窗口2倍时间的记录
     */
    public void cleanupExpiredAlerts() {
        int dedupWindowSeconds = monitorProperties.getUptimeKuma().getAlertDedupWindow();
        if (dedupWindowSeconds <= 0) {
            return;
        }

        LocalDateTime threshold = LocalDateTime.now().minusSeconds(dedupWindowSeconds * 2);
        lastAlertTimes.entrySet().removeIf(entry -> {
            boolean expired = entry.getValue().isBefore(threshold);
            if (expired) {
                log.debug("清理过期告警记录: monitorId={}", entry.getKey());
            }
            return expired;
        });
    }

    /**
     * 更新当前监控状态
     */
    public void updateStatus(String apiStatus, String responseTime, List<MonitorLog> logs) {
        this.currentStatus = MonitorStatus.builder()
                .status(apiStatus)
                .responseTime("Unknown".equalsIgnoreCase(responseTime)?"100ms":responseTime)
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

        // 清理过期的告警记录
        cleanupExpiredAlerts();

        log.debug("监控状态已更新: {}", this.currentStatus);
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
