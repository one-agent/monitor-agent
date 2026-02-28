package com.oneagent.monitor.service;

import com.oneagent.monitor.model.config.MonitorProperties;
import com.oneagent.monitor.model.dto.ErrorLog;
import com.oneagent.monitor.model.dto.LogAlertDTO;
import com.oneagent.monitor.model.dto.LogAlertLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 日志分析服务
 * 负责解析日志数据、生成错误指纹、判断告警条件
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogAnalysisService {

    private final MonitorProperties monitorProperties;

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;
    private static final Pattern EXCEPTION_PATTERN = Pattern.compile("(?:[a-zA-Z0-9.]+\\.)?([A-Z][a-zA-Z0-9]*Exception)");
    private static final Pattern ERROR_PATTERN = Pattern.compile("(?i)(error|exception|failed|timeout|refused|denied)");

    /**
     * 解析 LogAlertDTO 为 ErrorLog 列表
     *
     * @param alertDTO Alertmanager 发送的告警数据
     * @return ErrorLog 列表
     */
    public List<ErrorLog> parseLogAlert(LogAlertDTO alertDTO) {
        if (alertDTO == null || alertDTO.getAlerts() == null || alertDTO.getAlerts().isEmpty()) {
            return List.of();
        }

        return alertDTO.getAlerts().stream()
                .map(this::convertToErrorLog)
                .toList();
    }

    /**
     * 将单个 Alert 转换为 ErrorLog
     *
     * @param alert 单个告警
     * @return ErrorLog
     */
    private ErrorLog convertToErrorLog(LogAlertDTO.Alert alert) {
        String logLevelStr = alert.getLogLevel();
        LogAlertLevel logLevel = LogAlertLevel.fromString(logLevelStr);

        String message = alert.getLogMessage();
        if (message == null || message.isEmpty()) {
            message = alert.getDescription();
        }
        if (message == null || message.isEmpty()) {
            message = alert.getSummary();
        }

        String exceptionType = extractExceptionType(message);

        return ErrorLog.builder()
                .timestamp(alert.getStartTimeFormatted())
                .localDateTime(parseDateTime(alert.getStartsAt()))
                .logLevel(logLevel)
                .service(alert.getService())
                .source("alertmanager")
                .message(message)
                .stackTrace(alert.getStackTrace())
                .exceptionType(exceptionType)
                .monitorId(alert.getLabels() != null ? alert.getLabels().get("monitor_id") : null)
                .monitorName(alert.getLabels() != null ? alert.getLabels().get("monitor_name") : null)
                .alertUrl(alert.getGeneratorURL())
                .build();
    }

    /**
     * 生成错误指纹（用于去重和相似故障检索）
     *
     * @param errorLog 错误日志
     * @return 错误指纹（MD5）
     */
    public String generateErrorFingerprint(ErrorLog errorLog) {
        String key = String.format("%s|%s|%s",
                errorLog.getService(),
                errorLog.getLogLevel().name(),
                errorLog.getExceptionType() != null ? errorLog.getExceptionType() : "unknown"
        );
        return DigestUtils.md5Hex(key);
    }

    /**
     * 判断是否需要触发告警（组合条件）
     *
     * @param errorLog 错误日志
     * @return 如果需要告警返回 true
     */
    public boolean shouldTriggerAlert(ErrorLog errorLog) {
        // 条件1：日志级别检查
        if (!isLogLevelAboveThreshold(errorLog.getLogLevel())) {
            log.debug("日志级别 {} 未达到告警阈值", errorLog.getLogLevel());
            return false;
        }

        // 条件2：关键字检查
        if (!containsAlertKeywords(errorLog.getMessage())) {
            log.debug("日志消息不包含告警关键字: {}", errorLog.getMessage());
            return false;
        }

        // 条件3：堆栈信息检查（如果有堆栈，优先告警）
        if (errorLog.getStackTrace() != null && !errorLog.getStackTrace().isEmpty()) {
            return true;
        }

        // 条件4：异常类型检查（如果有异常类型，优先告警）
        if (errorLog.getExceptionType() != null && !errorLog.getExceptionType().isEmpty()) {
            return true;
        }

        // 条件5：严重程度检查（CRITICAL 和 ERROR 级别必须告警）
        if (errorLog.getLogLevel() == LogAlertLevel.CRITICAL || errorLog.getLogLevel() == LogAlertLevel.ERROR) {
            return true;
        }

        return false;
    }

    /**
     * 判断日志级别是否达到告警阈值
     *
     * @param logLevel 日志级别
     * @return 如果达到阈值返回 true
     */
    private boolean isLogLevelAboveThreshold(LogAlertLevel logLevel) {
        LogAlertLevel minLevel = LogAlertLevel.fromString(
                monitorProperties.getLogAlert().getMinLevel()
        );
        return logLevel.getLevel() >= minLevel.getLevel();
    }

    /**
     * 检查消息是否包含告警关键字
     *
     * @param message 日志消息
     * @return 如果包含告警关键字返回 true
     */
    private boolean containsAlertKeywords(String message) {
        if (message == null || message.isEmpty()) {
            return false;
        }

        String keywords = monitorProperties.getLogAlert().getKeywords();
        if (keywords == null || keywords.isEmpty()) {
            // 默认检查常见错误关键字
            Matcher matcher = ERROR_PATTERN.matcher(message);
            return matcher.find();
        }

        // 检查配置的关键字
        String[] keywordArray = keywords.split(",");
        String lowerMessage = message.toLowerCase();

        return Arrays.stream(keywordArray)
                .map(String::trim)
                .anyMatch(keyword -> lowerMessage.contains(keyword.toLowerCase()));
    }

    /**
     * 从日志消息中提取异常类型
     *
     * @param message 日志消息
     * @return 异常类型，如果未找到返回 null
     */
    private String extractExceptionType(String message) {
        if (message == null || message.isEmpty()) {
            return null;
        }

        Matcher matcher = EXCEPTION_PATTERN.matcher(message);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    /**
     * 解析日期时间字符串
     *
     * @param dateTimeStr 日期时间字符串
     * @return LocalDateTime
     */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return LocalDateTime.now();
        }

        try {
            return LocalDateTime.parse(dateTimeStr, ISO_FORMATTER);
        } catch (Exception e) {
            log.warn("Failed to parse datetime: {}", dateTimeStr, e);
            return LocalDateTime.now();
        }
    }

    /**
     * 生成日志分析摘要
     *
     * @param errorLog 错误日志
     * @return 分析摘要
     */
    public String generateAnalysisSummary(ErrorLog errorLog) {
        StringBuilder summary = new StringBuilder();

        summary.append("【服务】").append(errorLog.getService()).append("\n");
        summary.append("【级别】").append(errorLog.getLogLevel().getDescription()).append("\n");
        summary.append("【时间】").append(errorLog.getTimestamp()).append("\n");

        if (errorLog.getExceptionType() != null) {
            summary.append("【异常类型】").append(errorLog.getExceptionType()).append("\n");
        }

        summary.append("【错误消息】").append(errorLog.getSummary()).append("\n");

        return summary.toString();
    }
}
