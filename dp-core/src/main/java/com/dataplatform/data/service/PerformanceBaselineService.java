package com.dataplatform.data.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * 性能基线检测服务
 * 自动检测性能异常，基于滑动窗口计算基线并检测偏差
 * 需求: 14.8
 */
@Slf4j
@Service
public class PerformanceBaselineService {

    /** 指标基线数据 key=metricName */
    private final Map<String, MetricBaseline> baselines = new ConcurrentHashMap<>();

    /** 异常记录 */
    private final Deque<AnomalyRecord> anomalies = new ConcurrentLinkedDeque<>();

    /** 默认窗口大小 */
    private static final int DEFAULT_WINDOW_SIZE = 100;

    /** 默认偏差倍数（超过均值+N倍标准差视为异常） */
    private static final double DEFAULT_DEVIATION_FACTOR = 3.0;

    /** 最大异常记录数 */
    private static final int MAX_ANOMALY_RECORDS = 5000;

    /**
     * 记录指标值并检测异常
     *
     * @param metricName 指标名称
     * @param value      指标值
     * @return 是否检测到异常
     */
    public boolean recordAndCheck(String metricName, double value) {
        MetricBaseline baseline = baselines.computeIfAbsent(metricName,
                k -> new MetricBaseline(DEFAULT_WINDOW_SIZE));

        boolean isAnomaly = baseline.isAnomaly(value, DEFAULT_DEVIATION_FACTOR);

        if (isAnomaly && baseline.getCount() >= DEFAULT_WINDOW_SIZE / 2) {
            AnomalyRecord record = new AnomalyRecord();
            record.setMetricName(metricName);
            record.setValue(value);
            record.setMean(baseline.getMean());
            record.setStdDev(baseline.getStdDev());
            record.setThreshold(baseline.getMean() + DEFAULT_DEVIATION_FACTOR * baseline.getStdDev());
            record.setTimestamp(LocalDateTime.now());

            addAnomaly(record);
            log.warn("[性能异常] metric={}, value={}, mean={}, stdDev={}, threshold={}",
                    metricName, value, baseline.getMean(), baseline.getStdDev(), record.getThreshold());
        }

        baseline.addValue(value);
        return isAnomaly;
    }

    /**
     * 获取指标基线信息
     */
    public Map<String, Object> getBaseline(String metricName) {
        MetricBaseline baseline = baselines.get(metricName);
        if (baseline == null) {
            return Collections.emptyMap();
        }
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("metricName", metricName);
        info.put("mean", baseline.getMean());
        info.put("stdDev", baseline.getStdDev());
        info.put("min", baseline.getMin());
        info.put("max", baseline.getMax());
        info.put("count", baseline.getCount());
        return info;
    }

    /**
     * 获取所有基线概览
     */
    public List<Map<String, Object>> getAllBaselines() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (String name : baselines.keySet()) {
            result.add(getBaseline(name));
        }
        return result;
    }

    /**
     * 获取异常记录
     */
    public List<AnomalyRecord> getAnomalies(int limit) {
        List<AnomalyRecord> result = new ArrayList<>(anomalies);
        if (result.size() > limit) {
            return result.subList(result.size() - limit, result.size());
        }
        return result;
    }

    /**
     * 重置指标基线
     */
    public void resetBaseline(String metricName) {
        baselines.remove(metricName);
    }

    private void addAnomaly(AnomalyRecord record) {
        anomalies.addLast(record);
        while (anomalies.size() > MAX_ANOMALY_RECORDS) {
            anomalies.pollFirst();
        }
    }

    /**
     * 指标基线（滑动窗口统计）
     */
    static class MetricBaseline {
        private final int windowSize;
        private final Deque<Double> values;
        private double sum = 0;
        private double sumOfSquares = 0;
        private double min = Double.MAX_VALUE;
        private double max = Double.MIN_VALUE;

        MetricBaseline(int windowSize) {
            this.windowSize = windowSize;
            this.values = new ArrayDeque<>(windowSize);
        }

        synchronized void addValue(double value) {
            if (values.size() >= windowSize) {
                double removed = values.pollFirst();
                sum -= removed;
                sumOfSquares -= removed * removed;
            }
            values.addLast(value);
            sum += value;
            sumOfSquares += value * value;
            min = Math.min(min, value);
            max = Math.max(max, value);
        }

        synchronized boolean isAnomaly(double value, double deviationFactor) {
            if (values.size() < 10) {
                return false; // 数据不足，不判断
            }
            double mean = getMean();
            double stdDev = getStdDev();
            if (stdDev < 0.001) {
                return Math.abs(value - mean) > 0.001;
            }
            return value > mean + deviationFactor * stdDev;
        }

        synchronized double getMean() {
            return values.isEmpty() ? 0 : sum / values.size();
        }

        synchronized double getStdDev() {
            if (values.size() < 2) return 0;
            double mean = getMean();
            double variance = (sumOfSquares / values.size()) - (mean * mean);
            return variance > 0 ? Math.sqrt(variance) : 0;
        }

        synchronized double getMin() { return min == Double.MAX_VALUE ? 0 : min; }
        synchronized double getMax() { return max == Double.MIN_VALUE ? 0 : max; }
        synchronized int getCount() { return values.size(); }
    }

    @Data
    public static class AnomalyRecord {
        private String metricName;
        private double value;
        private double mean;
        private double stdDev;
        private double threshold;
        private LocalDateTime timestamp;
    }
}
