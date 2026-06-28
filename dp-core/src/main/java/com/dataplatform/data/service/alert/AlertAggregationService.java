package com.dataplatform.data.service.alert;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 告警聚合服务
 * 相同告警在时间窗口内只发送一次，避免告警风暴
 * 需求: 15.4
 */
@Slf4j
@Service
public class AlertAggregationService {

    /** 聚合窗口 key=fingerprint */
    private final Map<String, AggregationEntry> aggregationMap = new ConcurrentHashMap<>();

    /** 默认聚合窗口（秒） */
    private static final int DEFAULT_WINDOW_SECONDS = 300;

    /**
     * 检查告警是否应该发送（聚合判断）
     *
     * @param fingerprint 告警指纹（相同指纹的告警会被聚合）
     * @return true=应该发送, false=被聚合抑制
     */
    public boolean shouldSend(String fingerprint) {
        return shouldSend(fingerprint, DEFAULT_WINDOW_SECONDS);
    }

    /**
     * 检查告警是否应该发送
     *
     * @param fingerprint   告警指纹
     * @param windowSeconds 聚合窗口（秒）
     * @return true=应该发送
     */
    public boolean shouldSend(String fingerprint, int windowSeconds) {
        AggregationEntry entry = aggregationMap.get(fingerprint);
        long now = System.currentTimeMillis();

        if (entry == null) {
            aggregationMap.put(fingerprint, new AggregationEntry(now, 1));
            return true;
        }

        if (now - entry.getFirstOccurrence() > windowSeconds * 1000L) {
            // 窗口过期，重置
            entry.setFirstOccurrence(now);
            entry.setCount(1);
            entry.setLastSentTime(now);
            return true;
        }

        // 在窗口内，增加计数但不发送
        entry.setCount(entry.getCount() + 1);
        return false;
    }

    /**
     * 获取聚合统计
     */
    public Map<String, Object> getAggregationStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("activeFingerprints", aggregationMap.size());

        List<Map<String, Object>> entries = new ArrayList<>();
        aggregationMap.forEach((fp, entry) -> {
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("fingerprint", fp);
            e.put("count", entry.getCount());
            e.put("firstOccurrence", entry.getFirstOccurrence());
            e.put("lastSentTime", entry.getLastSentTime());
            entries.add(e);
        });
        stats.put("entries", entries);
        return stats;
    }

    /**
     * 清理过期的聚合条目
     */
    public void cleanup(int maxAgeSeconds) {
        long now = System.currentTimeMillis();
        aggregationMap.entrySet().removeIf(e ->
                now - e.getValue().getFirstOccurrence() > maxAgeSeconds * 1000L);
    }

    /**
     * 生成告警指纹
     */
    public static String generateFingerprint(String ruleName, String metricType, String level) {
        return ruleName + "|" + metricType + "|" + level;
    }

    @Data
    private static class AggregationEntry {
        private long firstOccurrence;
        private long lastSentTime;
        private int count;

        AggregationEntry(long time, int count) {
            this.firstOccurrence = time;
            this.lastSentTime = time;
            this.count = count;
        }
    }
}
