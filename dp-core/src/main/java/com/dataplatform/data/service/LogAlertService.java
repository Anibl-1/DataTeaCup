package com.dataplatform.data.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 日志告警服务
 * 监控ERROR级别日志并触发告警，支持告警聚合和静默
 * 需求: 13.6
 */
@Slf4j
@Service
public class LogAlertService {

    /** 告警记录（内存存储，生产环境可替换为持久化） */
    private final Deque<LogAlertRecord> alertHistory = new ConcurrentLinkedDeque<>();

    /** 告警聚合窗口内的计数器 key=alertKey, value=count */
    private final Map<String, AggregationWindow> aggregationWindows = new ConcurrentHashMap<>();

    /** 静默规则 */
    private final Map<String, LocalDateTime> silenceRules = new ConcurrentHashMap<>();

    /** 告警ID生成器 */
    private final AtomicLong alertIdGenerator = new AtomicLong(0);

    /** 最大历史记录数 */
    private static final int MAX_HISTORY_SIZE = 10000;

    /** 默认聚合窗口（秒） */
    private static final int DEFAULT_AGGREGATION_WINDOW_SECONDS = 300;

    /**
     * 处理ERROR级别日志，触发告警
     *
     * @param loggerName 日志来源
     * @param message    错误消息
     * @param throwable  异常信息（可选）
     */
    public void onErrorLog(String loggerName, String message, Throwable throwable) {
        String alertKey = buildAlertKey(loggerName, message);

        // 检查是否在静默期
        if (isSilenced(alertKey)) {
            return;
        }

        // 检查聚合窗口
        AggregationWindow window = aggregationWindows.computeIfAbsent(alertKey,
                k -> new AggregationWindow());

        if (!window.shouldAlert(DEFAULT_AGGREGATION_WINDOW_SECONDS)) {
            window.incrementCount();
            return;
        }

        window.reset();

        // 创建告警记录
        LogAlertRecord record = new LogAlertRecord();
        record.setId(alertIdGenerator.incrementAndGet());
        record.setAlertKey(alertKey);
        record.setLoggerName(loggerName);
        record.setMessage(message);
        record.setLevel("ERROR");
        record.setTimestamp(LocalDateTime.now());
        record.setStackTrace(throwable != null ? getStackTraceString(throwable) : null);
        record.setStatus("OPEN");

        addToHistory(record);

        log.warn("[日志告警] 检测到ERROR日志: logger={}, message={}", loggerName, truncate(message, 200));
    }

    /**
     * 获取告警历史
     */
    public List<LogAlertRecord> getAlertHistory(int limit) {
        List<LogAlertRecord> result = new ArrayList<>(alertHistory);
        if (result.size() > limit) {
            return result.subList(result.size() - limit, result.size());
        }
        return result;
    }

    /**
     * 获取未处理的告警
     */
    public List<LogAlertRecord> getOpenAlerts() {
        List<LogAlertRecord> result = new ArrayList<>();
        for (LogAlertRecord record : alertHistory) {
            if ("OPEN".equals(record.getStatus())) {
                result.add(record);
            }
        }
        return result;
    }

    /**
     * 确认告警
     */
    public boolean acknowledgeAlert(long alertId) {
        for (LogAlertRecord record : alertHistory) {
            if (record.getId() == alertId) {
                record.setStatus("ACKNOWLEDGED");
                record.setAcknowledgedAt(LocalDateTime.now());
                return true;
            }
        }
        return false;
    }

    /**
     * 关闭告警
     */
    public boolean closeAlert(long alertId) {
        for (LogAlertRecord record : alertHistory) {
            if (record.getId() == alertId) {
                record.setStatus("CLOSED");
                record.setClosedAt(LocalDateTime.now());
                return true;
            }
        }
        return false;
    }

    /**
     * 添加静默规则
     *
     * @param alertKey 告警键
     * @param until    静默截止时间
     */
    public void addSilenceRule(String alertKey, LocalDateTime until) {
        silenceRules.put(alertKey, until);
    }

    /**
     * 移除静默规则
     */
    public void removeSilenceRule(String alertKey) {
        silenceRules.remove(alertKey);
    }

    /**
     * 获取告警统计
     */
    public Map<String, Object> getAlertStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        long total = alertHistory.size();
        long open = alertHistory.stream().filter(r -> "OPEN".equals(r.getStatus())).count();
        long acknowledged = alertHistory.stream().filter(r -> "ACKNOWLEDGED".equals(r.getStatus())).count();
        long closed = alertHistory.stream().filter(r -> "CLOSED".equals(r.getStatus())).count();

        stats.put("total", total);
        stats.put("open", open);
        stats.put("acknowledged", acknowledged);
        stats.put("closed", closed);
        stats.put("silenceRules", silenceRules.size());
        return stats;
    }

    private String buildAlertKey(String loggerName, String message) {
        // 使用logger名称 + 消息前100字符作为聚合键
        String msgKey = message != null && message.length() > 100 ? message.substring(0, 100) : message;
        return loggerName + ":" + msgKey;
    }

    private boolean isSilenced(String alertKey) {
        LocalDateTime until = silenceRules.get(alertKey);
        if (until == null) {
            return false;
        }
        if (LocalDateTime.now().isAfter(until)) {
            silenceRules.remove(alertKey);
            return false;
        }
        return true;
    }

    private void addToHistory(LogAlertRecord record) {
        alertHistory.addLast(record);
        while (alertHistory.size() > MAX_HISTORY_SIZE) {
            alertHistory.pollFirst();
        }
    }

    private String getStackTraceString(Throwable t) {
        StringBuilder sb = new StringBuilder();
        sb.append(t.getClass().getName()).append(": ").append(t.getMessage()).append("\n");
        StackTraceElement[] elements = t.getStackTrace();
        int limit = Math.min(elements.length, 10);
        for (int i = 0; i < limit; i++) {
            sb.append("\tat ").append(elements[i]).append("\n");
        }
        if (elements.length > limit) {
            sb.append("\t... ").append(elements.length - limit).append(" more\n");
        }
        return sb.toString();
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() > maxLen ? s.substring(0, maxLen) + "..." : s;
    }

    /**
     * 告警记录
     */
    @Data
    public static class LogAlertRecord {
        private long id;
        private String alertKey;
        private String loggerName;
        private String message;
        private String level;
        private LocalDateTime timestamp;
        private String stackTrace;
        private String status; // OPEN, ACKNOWLEDGED, CLOSED
        private LocalDateTime acknowledgedAt;
        private LocalDateTime closedAt;
    }

    /**
     * 聚合窗口
     */
    private static class AggregationWindow {
        private long lastAlertTime = 0;
        private int count = 0;

        synchronized boolean shouldAlert(int windowSeconds) {
            long now = System.currentTimeMillis();
            if (now - lastAlertTime > windowSeconds * 1000L) {
                return true;
            }
            return false;
        }

        synchronized void reset() {
            lastAlertTime = System.currentTimeMillis();
            count = 1;
        }

        synchronized void incrementCount() {
            count++;
        }
    }
}
