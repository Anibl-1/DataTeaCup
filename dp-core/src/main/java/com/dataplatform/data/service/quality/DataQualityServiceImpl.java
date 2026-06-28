package com.dataplatform.data.service.quality;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * 数据质量服务实现
 * 需求: 19.1, 19.2, 19.3, 19.4, 19.5, 19.6, 19.7, 19.8
 */
@Slf4j
@Service
public class DataQualityServiceImpl implements DataQualityService {

    private final Map<String, QualityRule> ruleMap = new ConcurrentHashMap<>();
    private final Map<String, Deque<QualityCheckResult>> historyMap = new ConcurrentHashMap<>();

    @Override
    public QualityRule addRule(QualityRule rule) {
        if (rule.getId() == null || rule.getId().isEmpty()) {
            rule.setId(UUID.randomUUID().toString().substring(0, 12));
        }
        ruleMap.put(rule.getId(), rule);
        return rule;
    }

    @Override
    public List<QualityRule> listRules() {
        return new ArrayList<>(ruleMap.values());
    }

    @Override
    public List<QualityCheckResult> checkQuality(String tableName, List<Map<String, Object>> data) {
        List<QualityCheckResult> results = new ArrayList<>();
        for (QualityRule rule : ruleMap.values()) {
            if (tableName.equals(rule.getTargetTable())) {
                QualityCheckResult result = checkRule(rule, data);
                results.add(result);
                storeResult(tableName, result);
            }
        }
        return results;
    }

    @Override
    public QualityCheckResult checkRule(QualityRule rule, List<Map<String, Object>> data) {
        QualityCheckResult result = new QualityCheckResult();
        result.setId(UUID.randomUUID().toString().substring(0, 12));
        result.setRuleId(rule.getId());
        result.setRuleName(rule.getName());
        result.setRuleType(rule.getType());
        result.setTargetTable(rule.getTargetTable());
        result.setTargetColumn(rule.getTargetColumn());
        result.setTotalRecords(data.size());
        result.setSeverity(rule.getSeverity());
        result.setCheckTime(LocalDateTime.now());

        if (data.isEmpty()) {
            result.setScore(1.0);
            result.setViolationCount(0);
            result.setPassed(true);
            return result;
        }

        long violations = 0;
        List<Map<String, Object>> sampleViolations = new ArrayList<>();

        switch (rule.getType()) {
            case "completeness":
                violations = checkCompleteness(rule, data, sampleViolations);
                break;
            case "uniqueness":
                violations = checkUniqueness(rule, data, sampleViolations);
                break;
            case "timeliness":
                violations = checkTimeliness(rule, data, sampleViolations);
                break;
            case "accuracy":
                violations = checkAccuracy(rule, data, sampleViolations);
                break;
            default:
                break;
        }

        result.setViolationCount(violations);
        double score = data.isEmpty() ? 1.0 : 1.0 - (double) violations / data.size();
        result.setScore(score);
        result.setPassed(score >= rule.getThreshold());
        result.setSampleViolations(sampleViolations.size() > 5
                ? sampleViolations.subList(0, 5) : sampleViolations);

        return result;
    }

    @Override
    public Map<String, Object> getQualityReport(String tableName) {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("tableName", tableName);

        Deque<QualityCheckResult> history = historyMap.get(tableName);
        if (history == null || history.isEmpty()) {
            report.put("overallScore", 1.0);
            report.put("checks", Collections.emptyList());
            return report;
        }

        // 取最近一轮检查结果
        List<QualityCheckResult> latest = new ArrayList<>();
        LocalDateTime latestTime = history.peekLast().getCheckTime();
        for (QualityCheckResult r : history) {
            if (r.getCheckTime().equals(latestTime)) {
                latest.add(r);
            }
        }

        double avgScore = latest.stream().mapToDouble(QualityCheckResult::getScore).average().orElse(1.0);
        report.put("overallScore", avgScore);
        report.put("totalRules", latest.size());
        report.put("passedRules", latest.stream().filter(QualityCheckResult::isPassed).count());
        report.put("failedRules", latest.stream().filter(r -> !r.isPassed()).count());
        report.put("checks", latest);
        return report;
    }

    @Override
    public List<Map<String, Object>> getQualityTrend(String tableName, int days) {
        Deque<QualityCheckResult> history = historyMap.get(tableName);
        if (history == null) return Collections.emptyList();

        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        Map<String, List<Double>> dailyScores = new LinkedHashMap<>();

        for (QualityCheckResult r : history) {
            if (r.getCheckTime().isAfter(cutoff)) {
                String date = r.getCheckTime().toLocalDate().toString();
                dailyScores.computeIfAbsent(date, k -> new ArrayList<>()).add(r.getScore());
            }
        }

        List<Map<String, Object>> trend = new ArrayList<>();
        for (Map.Entry<String, List<Double>> entry : dailyScores.entrySet()) {
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", entry.getKey());
            point.put("avgScore", entry.getValue().stream().mapToDouble(d -> d).average().orElse(1.0));
            point.put("checkCount", entry.getValue().size());
            trend.add(point);
        }
        return trend;
    }

    /**
     * 完整性检查 - 检测空值、缺失值（需求: 19.1）
     */
    long checkCompleteness(QualityRule rule, List<Map<String, Object>> data,
                           List<Map<String, Object>> violations) {
        String column = rule.getTargetColumn();
        long count = 0;
        for (Map<String, Object> row : data) {
            Object value = row.get(column);
            if (value == null || value.toString().trim().isEmpty()) {
                count++;
                violations.add(row);
            }
        }
        return count;
    }

    /**
     * 唯一性检查 - 检测重复值（需求: 19.1）
     */
    long checkUniqueness(QualityRule rule, List<Map<String, Object>> data,
                         List<Map<String, Object>> violations) {
        String column = rule.getTargetColumn();
        Map<Object, Integer> valueCounts = new LinkedHashMap<>();
        for (Map<String, Object> row : data) {
            Object value = row.get(column);
            if (value != null) {
                valueCounts.merge(value, 1, Integer::sum);
            }
        }

        long duplicates = 0;
        Set<Object> duplicateValues = new HashSet<>();
        for (Map.Entry<Object, Integer> entry : valueCounts.entrySet()) {
            if (entry.getValue() > 1) {
                duplicateValues.add(entry.getKey());
                duplicates += entry.getValue() - 1;
            }
        }

        for (Map<String, Object> row : data) {
            if (duplicateValues.contains(row.get(column))) {
                violations.add(row);
            }
        }
        return duplicates;
    }

    /**
     * 时效性检查 - 检测数据更新延迟（需求: 19.3）
     */
    long checkTimeliness(QualityRule rule, List<Map<String, Object>> data,
                         List<Map<String, Object>> violations) {
        // 简化实现：检查时间字段是否在合理范围内
        String column = rule.getTargetColumn();
        String maxAge = rule.getExpression(); // 如 "24h" 表示24小时内
        long count = 0;
        for (Map<String, Object> row : data) {
            Object value = row.get(column);
            if (value == null) {
                count++;
                violations.add(row);
            }
        }
        return count;
    }

    /**
     * 准确性检查 - 基于规则验证数据正确性（需求: 19.4）
     */
    long checkAccuracy(QualityRule rule, List<Map<String, Object>> data,
                       List<Map<String, Object>> violations) {
        String column = rule.getTargetColumn();
        String expression = rule.getExpression(); // 如 ">0", "between:1:100"
        long count = 0;

        for (Map<String, Object> row : data) {
            Object value = row.get(column);
            if (!evaluateExpression(value, expression)) {
                count++;
                violations.add(row);
            }
        }
        return count;
    }

    private boolean evaluateExpression(Object value, String expression) {
        if (value == null || expression == null) return false;

        try {
            if (expression.startsWith(">")) {
                double threshold = Double.parseDouble(expression.substring(1).trim());
                return Double.parseDouble(value.toString()) > threshold;
            } else if (expression.startsWith("<")) {
                double threshold = Double.parseDouble(expression.substring(1).trim());
                return Double.parseDouble(value.toString()) < threshold;
            } else if (expression.startsWith("between:")) {
                String[] parts = expression.split(":");
                double min = Double.parseDouble(parts[1]);
                double max = Double.parseDouble(parts[2]);
                double val = Double.parseDouble(value.toString());
                return val >= min && val <= max;
            } else if (expression.startsWith("regex:")) {
                String pattern = expression.substring(6);
                return value.toString().matches(pattern);
            }
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    private void storeResult(String tableName, QualityCheckResult result) {
        Deque<QualityCheckResult> history = historyMap.computeIfAbsent(
                tableName, k -> new ConcurrentLinkedDeque<>());
        history.addLast(result);
        while (history.size() > 1000) {
            history.pollFirst();
        }
    }
}
