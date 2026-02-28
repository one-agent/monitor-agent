package com.oneagent.monitor.service;

import com.oneagent.monitor.model.dto.ErrorLog;
import com.oneagent.monitor.model.config.MonitorProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * 历史故障记录服务
 * 负责存储、检索历史故障记录
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HistoricalFaultService {

    private final MonitorProperties monitorProperties;

    /**
     * 内存存储：错误指纹 -> 故障记录列表
     * 使用 ConcurrentHashMap 保证线程安全
     */
    private final ConcurrentMap<String, List<FaultRecord>> faultRecords = new ConcurrentHashMap<>();

    /**
     * 故障记录
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class FaultRecord {
        /**
         * 错误指纹
         */
        private String fingerprint;

        /**
         * 错误日志
         */
        private ErrorLog errorLog;

        /**
         * 解决方案
         */
        private String solution;

        /**
         * 记录时间
         */
        private LocalDateTime recordTime;

        /**
         * 是否已解决
         */
        private boolean resolved;

        /**
         * 解决时间
         */
        private LocalDateTime resolvedTime;

        /**
         * 故障文档 ID（Apifox）
         */
        private String documentId;

        /**
         * 发生次数
         */
        private int occurrenceCount;
    }

    /**
     * 保存故障记录
     *
     * @param errorLog 错误日志
     * @param solution 解决方案（可以为空）
     * @param documentId 文档 ID（可以为空）
     */
    public void saveFaultRecord(ErrorLog errorLog, String solution, String documentId) {
        String fingerprint = errorLog.getFingerprint();
        if (fingerprint == null || fingerprint.isEmpty()) {
            log.warn("Error log has no fingerprint, cannot save fault record");
            return;
        }

        FaultRecord record = FaultRecord.builder()
                .fingerprint(fingerprint)
                .errorLog(errorLog)
                .solution(solution)
                .recordTime(LocalDateTime.now())
                .resolved(false)
                .documentId(documentId)
                .occurrenceCount(1)
                .build();

        faultRecords.computeIfAbsent(fingerprint, k -> new ArrayList<>()).add(record);

        log.info("Saved fault record for fingerprint: {}", fingerprint);
    }

    /**
     * 根据错误指纹检索历史故障记录
     *
     * @param fingerprint 错误指纹
     * @param maxDays 最大天数（只检索最近 N 天的记录）
     * @return 故障记录列表
     */
    public List<FaultRecord> retrieveFaultRecords(String fingerprint, int maxDays) {
        List<FaultRecord> records = faultRecords.get(fingerprint);
        if (records == null || records.isEmpty()) {
            return List.of();
        }

        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(maxDays);

        return records.stream()
                .filter(record -> record.getRecordTime().isAfter(cutoffTime))
                .collect(Collectors.toList());
    }

    /**
     * 根据错误指纹检索历史故障记录（使用配置的最大天数）
     *
     * @param fingerprint 错误指纹
     * @return 故障记录列表
     */
    public List<FaultRecord> retrieveFaultRecords(String fingerprint) {
        int maxDays = monitorProperties.getFaultHistory().getMaxDays();
        return retrieveFaultRecords(fingerprint, maxDays);
    }

    /**
     * 根据服务名称检索历史故障记录
     *
     * @param serviceName 服务名称
     * @param maxDays 最大天数
     * @return 故障记录列表
     */
    public List<FaultRecord> retrieveFaultRecordsByService(String serviceName, int maxDays) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(maxDays);

        return faultRecords.values().stream()
                .flatMap(List::stream)
                .filter(record -> record.getErrorLog().getService().equals(serviceName))
                .filter(record -> record.getRecordTime().isAfter(cutoffTime))
                .collect(Collectors.toList());
    }

    /**
     * 获取最近的故障记录（按时间排序）
     *
     * @param limit 最大数量
     * @return 故障记录列表
     */
    public List<FaultRecord> getRecentFaults(int limit) {
        return faultRecords.values().stream()
                .flatMap(List::stream)
                .sorted((a, b) -> b.getRecordTime().compareTo(a.getRecordTime()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 标记故障为已解决
     *
     * @param fingerprint 错误指纹
     * @param documentId 文档 ID
     */
    public void markAsResolved(String fingerprint, String documentId) {
        List<FaultRecord> records = faultRecords.get(fingerprint);
        if (records != null) {
            LocalDateTime now = LocalDateTime.now();
            records.forEach(record -> {
                record.setResolved(true);
                record.setResolvedTime(now);
                record.setDocumentId(documentId);
            });
            log.info("Marked fault as resolved: {}", fingerprint);
        }
    }

    /**
     * 增加错误发生次数
     *
     * @param fingerprint 错误指纹
     */
    public void incrementOccurrence(String fingerprint) {
        List<FaultRecord> records = faultRecords.get(fingerprint);
        if (records != null && !records.isEmpty()) {
            FaultRecord latestRecord = records.get(records.size() - 1);
            latestRecord.setOccurrenceCount(latestRecord.getOccurrenceCount() + 1);
            log.debug("Incremented occurrence count for fingerprint: {}", fingerprint);
        }
    }

    /**
     * 清理过期的故障记录
     */
    public void cleanupExpiredRecords() {
        int maxDays = monitorProperties.getFaultHistory().getMaxDays();
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(maxDays);

        int removedCount = 0;

        for (String fingerprint : faultRecords.keySet()) {
            List<FaultRecord> records = faultRecords.get(fingerprint);

            List<FaultRecord> filtered = records.stream()
                    .filter(record -> record.getRecordTime().isAfter(cutoffTime))
                    .collect(Collectors.toList());

            if (filtered.size() < records.size()) {
                removedCount += (records.size() - filtered.size());
                if (filtered.isEmpty()) {
                    faultRecords.remove(fingerprint);
                } else {
                    faultRecords.put(fingerprint, filtered);
                }
            }
        }

        if (removedCount > 0) {
            log.info("Cleaned up {} expired fault records", removedCount);
        }
    }

    /**
     * 格式化历史故障记录为文本（供 AI 参考）
     *
     * @param records 故障记录列表
     * @return 格式化的文本
     */
    public String formatFaultRecordsForAI(List<FaultRecord> records) {
        if (records == null || records.isEmpty()) {
            return "无历史故障记录。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("【历史故障记录】共 ").append(records.size()).append(" 条\n\n");

        for (int i = 0; i < records.size(); i++) {
            FaultRecord record = records.get(i);
            ErrorLog errorLog = record.getErrorLog();

            sb.append("记录 ").append(i + 1).append(":\n");
            sb.append("  时间: ").append(errorLog.getTimestamp()).append("\n");
            sb.append("  服务: ").append(errorLog.getService()).append("\n");
            sb.append("  级别: ").append(errorLog.getLogLevel().getDescription()).append("\n");

            if (errorLog.getExceptionType() != null) {
                sb.append("  异常类型: ").append(errorLog.getExceptionType()).append("\n");
            }

            sb.append("  错误消息: ").append(errorLog.getSummary()).append("\n");

            if (record.getSolution() != null && !record.getSolution().isEmpty()) {
                sb.append("  解决方案: ").append(record.getSolution()).append("\n");
            }

            if (record.isResolved()) {
                sb.append("  状态: 已解决\n");
            } else {
                sb.append("  状态: 未解决\n");
            }

            sb.append("  发生次数: ").append(record.getOccurrenceCount()).append("\n");
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * 获取统计信息
     *
     * @return 统计信息字符串
     */
    public String getStatistics() {
        int totalRecords = faultRecords.values().stream()
                .mapToInt(List::size)
                .sum();

        int uniqueFingerprints = faultRecords.size();

        int resolvedCount = faultRecords.values().stream()
                .flatMap(List::stream)
                .mapToInt(record -> record.isResolved() ? 1 : 0)
                .sum();

        return String.format(
                "历史故障统计: 唯一错误数=%d, 总记录数=%d, 已解决=%d, 未解决=%d",
                uniqueFingerprints, totalRecords, resolvedCount, totalRecords - resolvedCount
        );
    }
}