package com.dataplatform.data.service;

import com.dataplatform.data.entity.DataSource;
import com.dataplatform.data.mapper.DataSourceMapper;
import com.dataplatform.data.service.chart.ChartCoreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI 数据洞察服务
 * 
 * 提供自动数据分析、异常检测、趋势识别等功能
 */
@Slf4j
@Service
public class AiInsightService {

    @Autowired
    private AiProviderClient aiProviderClient;
    
    @Autowired
    private AiStreamService aiStreamService;
    
    @Autowired
    private ChartCoreService chartCoreService;
    
    @Autowired
    private DataSourceMapper dataSourceMapper;
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private AlertService alertService;
    
    // 异常检测阈值（标准差倍数）
    private static final double ANOMALY_THRESHOLD = 3.0;

    /**
     * 同步分析数据集（复用 generateInsightReport 逻辑）
     *
     * @param dataSourceId 数据源ID
     * @param tableName 表名
     * @param sql SQL查询
     * @return 洞察报告
     */
    public InsightReport analyzeDatasetSync(Long dataSourceId, String tableName, String sql) {
        return generateInsightReport(dataSourceId, sql);
    }
    
    /**
     * 生成洞察报告
     */
    public InsightReport generateInsightReport(Long dataSourceId, String sql) {
        log.info("Generating insight report: dataSourceId={}", dataSourceId);
        
        try {
            List<Map<String, Object>> data = chartCoreService.executeSql(dataSourceId, sql, null, 1000);
            
            if (data == null || data.isEmpty()) {
                return InsightReport.empty("无数据可分析");
            }
            
            Map<String, Object> summary = generateStatisticsSummary(data);
            List<Map<String, Object>> anomalies = detectAnomalies(data, ANOMALY_THRESHOLD);
            Map<String, Object> trends = analyzeTrends(data);
            
            String insightPrompt = buildInsightPrompt(summary, anomalies, trends);
            String aiInsight = aiProviderClient.chat(buildInsightSystemPrompt(), insightPrompt);
            
            return InsightReport.builder()
                    .summary(summary)
                    .anomalies(anomalies)
                    .trends(trends)
                    .aiInsight(aiInsight)
                    .generatedAt(new Date())
                    .build();
                    
        } catch (Exception e) {
            log.error("Failed to generate insight report", e);
            return InsightReport.error(e.getMessage());
        }
    }

    /**
     * 检测异常值
     * 
     * @param dataSourceId 数据源ID
     * @param sql SQL查询
     * @param threshold 阈值（标准差倍数）
     * @return 异常记录列表
     */
    public List<AnomalyRecord> detectAnomalies(Long dataSourceId, String sql, double threshold) {
        log.info("Detecting anomalies: dataSourceId={}, threshold={}", dataSourceId, threshold);
        
        try {
            List<Map<String, Object>> data = chartCoreService.executeSql(dataSourceId, sql, null, 10000);
            
            if (data == null || data.isEmpty()) {
                return Collections.emptyList();
            }
            
            List<Map<String, Object>> rawAnomalies = detectAnomalies(data, threshold);
            
            return rawAnomalies.stream()
                    .map(a -> AnomalyRecord.builder()
                            .field((String) a.get("field"))
                            .value((Number) a.get("value"))
                            .mean((Double) a.get("mean"))
                            .stdDev((Double) a.get("stdDev"))
                            .deviation((Double) a.get("deviation"))
                            .rowIndex((Integer) a.get("rowIndex"))
                            .build())
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("Anomaly detection failed", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 分析趋势
     */
    public TrendAnalysis analyzeTrend(Long dataSourceId, String sql, String timeField) {
        log.info("Analyzing trend: dataSourceId={}, timeField={}", dataSourceId, timeField);
        
        try {
            List<Map<String, Object>> data = chartCoreService.executeSql(dataSourceId, sql, null, 10000);
            
            if (data == null || data.isEmpty()) {
                return TrendAnalysis.empty();
            }
            
            Map<String, Object> trends = analyzeTrends(data);
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> fieldTrends = (List<Map<String, Object>>) trends.get("fieldTrends");
            
            String overallTrend = "stable";
            double changeRate = 0.0;
            
            if (fieldTrends != null && !fieldTrends.isEmpty()) {
                Map<String, Object> firstTrend = fieldTrends.get(0);
                overallTrend = (String) firstTrend.get("trend");
                changeRate = (Double) firstTrend.getOrDefault("changeRate", 0.0);
            }
            
            return TrendAnalysis.builder()
                    .trend(overallTrend)
                    .changeRate(changeRate)
                    .fieldTrends(fieldTrends)
                    .hasCyclicPattern((Boolean) trends.getOrDefault("hasCyclicPattern", false))
                    .build();
                    
        } catch (Exception e) {
            log.error("Trend analysis failed", e);
            return TrendAnalysis.empty();
        }
    }

    /**
     * 生成统计摘要
     */
    private Map<String, Object> generateStatisticsSummary(List<Map<String, Object>> data) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalRows", data.size());
        
        if (data.isEmpty()) {
            return summary;
        }
        
        Map<String, Object> firstRow = data.get(0);
        List<Map<String, Object>> fieldStats = new ArrayList<>();
        
        for (String field : firstRow.keySet()) {
            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("field", field);
            
            List<Object> values = data.stream()
                    .map(row -> row.get(field))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            
            stats.put("nonNullCount", values.size());
            stats.put("nullCount", data.size() - values.size());
            stats.put("nullRate", (data.size() - values.size()) * 100.0 / data.size());
            
            // 数值字段统计
            List<Double> numericValues = values.stream()
                    .filter(v -> v instanceof Number)
                    .map(v -> ((Number) v).doubleValue())
                    .collect(Collectors.toList());
            
            if (!numericValues.isEmpty()) {
                stats.put("type", "numeric");
                stats.put("min", numericValues.stream().min(Double::compare).orElse(0.0));
                stats.put("max", numericValues.stream().max(Double::compare).orElse(0.0));
                double mean = numericValues.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                stats.put("mean", mean);
                stats.put("sum", numericValues.stream().mapToDouble(Double::doubleValue).sum());
                
                double variance = numericValues.stream()
                        .mapToDouble(v -> Math.pow(v - mean, 2))
                        .average().orElse(0.0);
                stats.put("stdDev", Math.sqrt(variance));
            } else {
                stats.put("type", "categorical");
                Map<Object, Long> valueCounts = values.stream()
                        .collect(Collectors.groupingBy(v -> v, Collectors.counting()));
                stats.put("uniqueCount", valueCounts.size());
                stats.put("topValues", valueCounts.entrySet().stream()
                        .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                        .limit(5)
                        .map(e -> Map.of("value", e.getKey(), "count", e.getValue()))
                        .collect(Collectors.toList()));
            }
            
            fieldStats.add(stats);
        }
        
        summary.put("fields", fieldStats);
        return summary;
    }

    /**
     * 检测异常值（内部方法）
     * 使用 3σ 原则：偏离均值超过 threshold 个标准差的数据点
     */
    private List<Map<String, Object>> detectAnomalies(List<Map<String, Object>> data, double threshold) {
        List<Map<String, Object>> anomalies = new ArrayList<>();
        
        if (data.isEmpty()) {
            return anomalies;
        }
        
        Map<String, Object> firstRow = data.get(0);
        
        for (String field : firstRow.keySet()) {
            List<Double> numericValues = new ArrayList<>();
            List<Integer> indices = new ArrayList<>();
            
            for (int i = 0; i < data.size(); i++) {
                Object value = data.get(i).get(field);
                if (value instanceof Number) {
                    numericValues.add(((Number) value).doubleValue());
                    indices.add(i);
                }
            }
            
            if (numericValues.size() < 3) {
                continue; // 需要至少3个数据点
            }
            
            double mean = numericValues.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double variance = numericValues.stream()
                    .mapToDouble(v -> Math.pow(v - mean, 2))
                    .average().orElse(0.0);
            double stdDev = Math.sqrt(variance);
            
            if (stdDev == 0) {
                continue; // 所有值相同，无异常
            }
            
            for (int i = 0; i < numericValues.size(); i++) {
                double value = numericValues.get(i);
                double deviation = Math.abs(value - mean) / stdDev;
                
                if (deviation > threshold) {
                    Map<String, Object> anomaly = new LinkedHashMap<>();
                    anomaly.put("field", field);
                    anomaly.put("value", value);
                    anomaly.put("mean", mean);
                    anomaly.put("stdDev", stdDev);
                    anomaly.put("deviation", deviation);
                    anomaly.put("rowIndex", indices.get(i));
                    anomalies.add(anomaly);
                }
            }
        }
        
        return anomalies;
    }

    /**
     * 分析趋势
     */
    private Map<String, Object> analyzeTrends(List<Map<String, Object>> data) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> fieldTrends = new ArrayList<>();
        
        if (data.isEmpty()) {
            result.put("fieldTrends", fieldTrends);
            result.put("hasCyclicPattern", false);
            return result;
        }
        
        Map<String, Object> firstRow = data.get(0);
        
        for (String field : firstRow.keySet()) {
            List<Double> values = data.stream()
                    .map(row -> row.get(field))
                    .filter(v -> v instanceof Number)
                    .map(v -> ((Number) v).doubleValue())
                    .collect(Collectors.toList());
            
            if (values.size() < 3) {
                continue;
            }
            
            Map<String, Object> trend = new LinkedHashMap<>();
            trend.put("field", field);
            
            // 简单线性趋势检测
            int n = values.size();
            double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
            
            for (int i = 0; i < n; i++) {
                sumX += i;
                sumY += values.get(i);
                sumXY += i * values.get(i);
                sumX2 += i * i;
            }
            
            double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
            double meanY = sumY / n;
            
            // 计算变化率
            double changeRate = meanY != 0 ? (slope * n / meanY) * 100 : 0;
            trend.put("changeRate", changeRate);
            
            // 判断趋势方向
            if (Math.abs(changeRate) < 5) {
                trend.put("trend", "stable");
            } else if (changeRate > 0) {
                trend.put("trend", "increasing");
            } else {
                trend.put("trend", "decreasing");
            }
            
            fieldTrends.add(trend);
        }
        
        result.put("fieldTrends", fieldTrends);
        result.put("hasCyclicPattern", detectCyclicPattern(data));
        
        return result;
    }
    
    /**
     * 检测周期性模式（简化版）
     */
    private boolean detectCyclicPattern(List<Map<String, Object>> data) {
        // 简化实现：检查数据量是否足够进行周期分析
        return data.size() >= 12; // 假设有足够数据可能存在周期
    }

    /**
     * 构建洞察提示词
     */
    private String buildInsightPrompt(Map<String, Object> summary, 
                                       List<Map<String, Object>> anomalies,
                                       Map<String, Object> trends) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请基于以下数据分析结果，生成简洁的数据洞察报告：\n\n");
        
        prompt.append("## 数据概览\n");
        prompt.append("- 总记录数: ").append(summary.get("totalRows")).append("\n");
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> fields = (List<Map<String, Object>>) summary.get("fields");
        if (fields != null) {
            prompt.append("- 字段数: ").append(fields.size()).append("\n");
            for (Map<String, Object> field : fields) {
                prompt.append("  - ").append(field.get("field")).append(": ");
                if ("numeric".equals(field.get("type"))) {
                    prompt.append(String.format("数值型, 均值=%.2f, 标准差=%.2f", 
                            field.get("mean"), field.get("stdDev")));
                } else {
                    prompt.append("分类型, 唯一值数=").append(field.get("uniqueCount"));
                }
                prompt.append("\n");
            }
        }
        
        prompt.append("\n## 异常检测\n");
        prompt.append("- 发现异常点: ").append(anomalies.size()).append("个\n");
        if (!anomalies.isEmpty()) {
            for (Map<String, Object> anomaly : anomalies.subList(0, Math.min(5, anomalies.size()))) {
                prompt.append(String.format("  - 字段[%s] 值=%.2f (偏离%.1f个标准差)\n",
                        anomaly.get("field"), anomaly.get("value"), anomaly.get("deviation")));
            }
        }
        
        prompt.append("\n## 趋势分析\n");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> fieldTrends = (List<Map<String, Object>>) trends.get("fieldTrends");
        if (fieldTrends != null) {
            for (Map<String, Object> trend : fieldTrends) {
                prompt.append(String.format("  - %s: %s (变化率: %.1f%%)\n",
                        trend.get("field"), trend.get("trend"), trend.get("changeRate")));
            }
        }
        
        prompt.append("\n请提供：\n");
        prompt.append("1. 关键发现（3-5条）\n");
        prompt.append("2. 数据质量评估\n");
        prompt.append("3. 建议的后续分析方向\n");
        
        return prompt.toString();
    }
    
    private String buildInsightSystemPrompt() {
        return "你是一个专业的数据分析师。请基于提供的数据分析结果，生成简洁、专业的洞察报告。" +
               "使用中文回复，重点突出关键发现和可操作的建议。";
    }
    
    /**
     * 发送异常告警
     * 
     * @param dataSourceId 数据源ID
     * @param tableName 表名
     * @param anomalies 异常列表
     */
    public void sendAnomalyAlerts(Long dataSourceId, String tableName, List<AnomalyRecord> anomalies) {
        if (anomalies == null || anomalies.isEmpty()) {
            return;
        }
        
        log.info("Sending anomaly alerts: {} anomalies found in {}", anomalies.size(), tableName);
        
        try {
            // 获取数据源信息
            DataSource dataSource = dataSourceMapper.selectById(dataSourceId);
            String dsName = dataSource != null ? dataSource.getName() : "Unknown";
            
            // 构建告警消息
            StringBuilder message = new StringBuilder();
            message.append(String.format("数据异常告警 - 数据源: %s, 表: %s\n", dsName, tableName));
            message.append(String.format("发现 %d 个异常数据点:\n", anomalies.size()));
            
            for (int i = 0; i < Math.min(5, anomalies.size()); i++) {
                AnomalyRecord anomaly = anomalies.get(i);
                message.append(String.format("  - 字段[%s]: 值=%.2f (偏离%.1f个标准差)\n",
                        anomaly.getField(), 
                        anomaly.getValue().doubleValue(), 
                        anomaly.getDeviation()));
            }
            
            if (anomalies.size() > 5) {
                message.append(String.format("  ... 还有 %d 个异常\n", anomalies.size() - 5));
            }
            
            // 通过告警服务发送
            alertService.checkMetric("data_anomaly", anomalies.size());
            
            // 通过通知服务发送
            notificationService.sendSystemNotification(
                    "数据异常告警",
                    message.toString(),
                    "warning"
            );
            
            log.info("Anomaly alerts sent successfully");
            
        } catch (Exception e) {
            log.error("Failed to send anomaly alerts", e);
        }
    }
    
    /**
     * 检测并告警异常
     */
    public void detectAndAlert(Long dataSourceId, String tableName, String sql, double threshold) {
        List<AnomalyRecord> anomalies = detectAnomalies(dataSourceId, sql, threshold);
        if (!anomalies.isEmpty()) {
            sendAnomalyAlerts(dataSourceId, tableName, anomalies);
        }
    }

    // ==================== 内部数据类 ====================
    
    /**
     * 洞察报告
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class InsightReport {
        private Map<String, Object> summary;
        private List<Map<String, Object>> anomalies;
        private Map<String, Object> trends;
        private String aiInsight;
        private Date generatedAt;
        private String error;
        
        public static InsightReport empty(String message) {
            return InsightReport.builder()
                    .summary(Map.of("message", message))
                    .anomalies(Collections.emptyList())
                    .trends(Collections.emptyMap())
                    .generatedAt(new Date())
                    .build();
        }
        
        public static InsightReport error(String error) {
            return InsightReport.builder()
                    .error(error)
                    .generatedAt(new Date())
                    .build();
        }
    }
    
    /**
     * 异常记录
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AnomalyRecord {
        private String field;
        private Number value;
        private Double mean;
        private Double stdDev;
        private Double deviation;
        private Integer rowIndex;
    }
    
    /**
     * 趋势分析结果
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class TrendAnalysis {
        private String trend; // increasing, decreasing, stable
        private Double changeRate;
        private List<Map<String, Object>> fieldTrends;
        private Boolean hasCyclicPattern;
        
        public static TrendAnalysis empty() {
            return TrendAnalysis.builder()
                    .trend("unknown")
                    .changeRate(0.0)
                    .fieldTrends(Collections.emptyList())
                    .hasCyclicPattern(false)
                    .build();
        }
    }
}
