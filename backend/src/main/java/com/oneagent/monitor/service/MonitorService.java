package com.oneagent.monitor.service;

import com.oneagent.monitor.model.config.MonitorProperties;
import com.oneagent.monitor.model.dto.MonitorLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * 监控系统状态的服务类
 */
@Slf4j
@Service
public class MonitorService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final MonitorProperties monitorProperties;

    // Thread-safe in-memory storage for logs
    private final List<MonitorLog> monitorLogs = new ArrayList<>();
    // 告警去重：记录每个监控项的最后告警时间
    private final ConcurrentMap<String, LocalDateTime> lastAlertTimes = new ConcurrentHashMap<>();
    // 日志告警去重：记录每个错误指纹的最后告警时间
    private final ConcurrentMap<String, LocalDateTime> lastLogAlertTimes = new ConcurrentHashMap<>();
    // 错误聚合：记录每个错误指纹的出现次数
    private final ConcurrentMap<String, Integer> errorFrequencies = new ConcurrentHashMap<>();

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
        int dedupeWindowSeconds = monitorProperties.getUptimeKuma().getAlertDedupeWindow();

        if (dedupeWindowSeconds <= 0) {
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
        if (secondsSinceLastAlert >= dedupeWindowSeconds) {
            // 超过去重窗口，可以发送告警
            lastAlertTimes.put(monitorId, now);
            return true;
        }

        // 在去重窗口内，不发送告警
        log.debug("监控项 {} 在去重窗口内（{}秒内），跳过告警", monitorId, secondsSinceLastAlert);
        return false;
    }

    /**
     * 判断是否应该发送日志告警（基于错误指纹的去重检查）
     *
     * @param fingerprint 错误指纹
     * @return 如果应该发送告警返回 true，否则返回 false
     */
    public boolean shouldSendLogAlert(String fingerprint) {
        int dedupeWindowSeconds = monitorProperties.getLogAlert().getDedupeWindow();

        if (dedupeWindowSeconds <= 0) {
            // 未配置去重窗口，始终发送告警
            return true;
        }

        LocalDateTime lastAlertTime = lastLogAlertTimes.get(fingerprint);
        LocalDateTime now = LocalDateTime.now();

        if (lastAlertTime == null) {
            // 第一次告警，可以发送
            lastLogAlertTimes.put(fingerprint, now);
            return true;
        }

        // 检查是否超过去重窗口
        long secondsSinceLastAlert = ChronoUnit.SECONDS.between(lastAlertTime, now);
        if (secondsSinceLastAlert >= dedupeWindowSeconds) {
            // 超过去重窗口，可以发送告警
            lastLogAlertTimes.put(fingerprint, now);
            return true;
        }

        // 在去重窗口内，不发送告警
        log.debug("错误指纹 {} 在去重窗口内（{}秒内），跳过告警", fingerprint, secondsSinceLastAlert);
        return false;
    }

    /**
     * 增加错误的出现频率
     *
     * @param fingerprint 错误指纹
     */
    public void incrementErrorFrequency(String fingerprint) {
        errorFrequencies.compute(fingerprint, (k, v) -> v == null ? 1 : v + 1);
    }

    /**
     * 获取错误的出现频率
     *
     * @param fingerprint 错误指纹
     * @return 出现次数
     */
    public int getErrorFrequency(String fingerprint) {
        return errorFrequencies.getOrDefault(fingerprint, 0);
    }

    /**
     * 清理过期的告警记录
     * 定期清理超过去重窗口2倍时间的记录
     */
    public void cleanupExpiredAlerts() {
        int dedupeWindowSeconds = monitorProperties.getUptimeKuma().getAlertDedupeWindow();
        if (dedupeWindowSeconds <= 0) {
            return;
        }

        LocalDateTime threshold = LocalDateTime.now().minusSeconds(dedupeWindowSeconds * 2L);
        lastAlertTimes.entrySet().removeIf(entry -> {
            boolean expired = entry.getValue().isBefore(threshold);
            if (expired) {
                log.debug("清理过期告警记录: monitorId={}", entry.getKey());
            }
            return expired;
        });
    }

    /**
     * 清理过期的日志告警记录
     */
    public void cleanupExpiredLogAlerts() {
        int dedupeWindowSeconds = monitorProperties.getLogAlert().getDedupeWindow();
        if (dedupeWindowSeconds <= 0) {
            return;
        }

        LocalDateTime threshold = LocalDateTime.now().minusSeconds(dedupeWindowSeconds * 2L);
        lastLogAlertTimes.entrySet().removeIf(entry -> {
            boolean expired = entry.getValue().isBefore(threshold);
            if (expired) {
                log.debug("清理过期日志告警记录: fingerprint={}", entry.getKey());
            }
            return expired;
        });
    }

    /**
     * 更新当前监控状态
     */
    public void updateStatus(List<MonitorLog> logs) {
        // 如果提供了日志则更新
        synchronized (monitorLogs) {
            if (logs != null && !logs.isEmpty()) {
                monitorLogs.addAll(logs);
            }
        }

        // 清理过期的告警记录
        cleanupExpiredAlerts();
        cleanupExpiredLogAlerts();

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
     * 获取指定来源的监控日志
     *
     * @param source 日志来源（如 "alertmanager", "uptime-kuma"）
     * @return 监控日志列表
     */
    public List<MonitorLog> getLogsBySource(String source) {
        synchronized (monitorLogs) {
            return monitorLogs.stream()
                    .filter(log -> source.equals(log.getSource()))
                    .collect(Collectors.toList());
        }
    }

    /**
     * 检查是否存在未处理的错误日志
     * 去重monitorId取最近时间非1的数据
     */
    public List<MonitorLog> checkRecentLogs() {
        synchronized (monitorLogs) {
            // 用于存储每个monitorId对应的最新日志
            ConcurrentMap<String, MonitorLog> latestLogs = new ConcurrentHashMap<>();

            // 先按monitorId分组，找到每个monitorId对应的最新记录
            for (MonitorLog log : monitorLogs) {
                String monitorId = log.getMonitorId();
                MonitorLog existing = latestLogs.get(monitorId);

                // 如果monitorId不存在，则添加
                if (existing == null) {
                    latestLogs.put(monitorId, log);
                } else if (isNewer(log, existing)) {
                    // 如果当前日志时间更新，则替换
                    latestLogs.put(monitorId, log);
                }
            }

            // 过滤掉status为1的数据
            return latestLogs.values().stream()
                .filter(log -> !"1".equals(log.getStatus()))
                .collect(Collectors.toList());
        }
    }

    /**
     * 获取指定时间范围内的错误日志
     *
     * @param hoursAgo 多少小时前的日志
     * @return 错误日志列表
     */
    public List<MonitorLog> getErrorLogsInHours(int hoursAgo) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(hoursAgo);

        synchronized (monitorLogs) {
            return monitorLogs.stream()
                    .filter(log -> log.isError())
                    .filter(log -> {
                        try {
                            LocalDateTime logTime = LocalDateTime.parse(log.getTimestamp(), TIME_FORMATTER);
                            return logTime.isAfter(cutoffTime);
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .collect(Collectors.toList());
        }
    }

    /**
     * 比较两个MonitorLog的时间戳，判断log1是否比log2更新
     */
    private boolean isNewer(MonitorLog log1, MonitorLog log2) {
        if (log1.getTimestamp() == null || log2.getTimestamp() == null) {
            return false;
        }

        try {
            LocalDateTime time1 = LocalDateTime.parse(log1.getTimestamp(), TIME_FORMATTER);
            LocalDateTime time2 = LocalDateTime.parse(log2.getTimestamp(), TIME_FORMATTER);
            return time1.isAfter(time2);
        } catch (Exception e) {
            log.warn("解析时间戳失败: {} 或 {}", log1.getTimestamp(), log2.getTimestamp());
            return false;
        }
    }

    /**
     * 添加一条监控日志记录
     */
    public void addLog(MonitorLog monitorLog) {
        synchronized (monitorLogs) {
            monitorLogs.add(monitorLog);
        }
        log.debug("已添加监控日志: {}", monitorLog);
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

    /**
     * 获取监控统计信息
     *
     * @return 统计信息字符串
     */
    public String getStatistics() {
        synchronized (monitorLogs) {
            long totalLogs = monitorLogs.size();
            long errorLogs = monitorLogs.stream().filter(MonitorLog::isError).count();
            long uniqueMonitorIds = monitorLogs.stream()
                    .map(MonitorLog::getMonitorId)
                    .distinct()
                    .count();

            return String.format(
                    "监控统计: 总日志=%d, 错误日志=%d, 唯一监控项=%d, 唯一错误=%d",
                    totalLogs, errorLogs, uniqueMonitorIds, errorFrequencies.size()
            );
        }
    }
}
