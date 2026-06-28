package com.dataplatform.data.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 图表数据聚合服务
 * 提供数据聚合、统计、转换等功能
 * 
 * @author dataplatform
 */
@Service
public class ChartDataAggregateService {
    
    /**
     * 聚合函数枚举
     */
    public enum AggregateFunction {
        SUM, AVG, COUNT, MAX, MIN, NONE
    }
    
    /**
     * 按字段分组聚合数据
     * 
     * @param data 原始数据
     * @param groupByField 分组字段
     * @param aggregates 聚合配置 {字段名: 聚合函数}
     * @return 聚合后的数据
     */
    public List<Map<String, Object>> aggregateData(
            List<Map<String, Object>> data,
            String groupByField,
            Map<String, AggregateFunction> aggregates) {
        
        if (data == null || data.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 按分组字段分组
        Map<Object, List<Map<String, Object>>> groups = data.stream()
                .collect(Collectors.groupingBy(
                        row -> row.get(groupByField) != null ? row.get(groupByField) : "null"
                ));
        
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (Map.Entry<Object, List<Map<String, Object>>> entry : groups.entrySet()) {
            Map<String, Object> aggregatedRow = new LinkedHashMap<>();
            aggregatedRow.put(groupByField, "null".equals(entry.getKey()) ? null : entry.getKey());
            
            List<Map<String, Object>> groupRows = entry.getValue();
            
            for (Map.Entry<String, AggregateFunction> aggEntry : aggregates.entrySet()) {
                String field = aggEntry.getKey();
                AggregateFunction func = aggEntry.getValue();
                Object aggregatedValue = calculateAggregate(groupRows, field, func);
                aggregatedRow.put(field, aggregatedValue);
            }
            
            result.add(aggregatedRow);
        }
        
        return result;
    }
    
    /**
     * 计算聚合值
     */
    private Object calculateAggregate(List<Map<String, Object>> rows, String field, AggregateFunction func) {
        List<BigDecimal> values = rows.stream()
                .map(row -> row.get(field))
                .filter(Objects::nonNull)
                .map(this::toBigDecimal)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        
        if (values.isEmpty()) {
            return null;
        }
        
        switch (func) {
            case SUM:
                return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            case AVG:
                BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                return sum.divide(BigDecimal.valueOf(values.size()), 4, RoundingMode.HALF_UP);
            case COUNT:
                return values.size();
            case MAX:
                return values.stream().max(BigDecimal::compareTo).orElse(null);
            case MIN:
                return values.stream().min(BigDecimal::compareTo).orElse(null);
            case NONE:
            default:
                return values.get(0);
        }
    }
    
    /**
     * 转换为BigDecimal
     */
    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * 数据排序
     */
    public List<Map<String, Object>> sortData(
            List<Map<String, Object>> data,
            String sortField,
            boolean ascending) {
        
        if (data == null || data.isEmpty() || sortField == null) {
            return data;
        }
        
        return data.stream()
                .sorted((a, b) -> {
                    Object aVal = a.get(sortField);
                    Object bVal = b.get(sortField);
                    
                    if (aVal == null && bVal == null) return 0;
                    if (aVal == null) return ascending ? 1 : -1;
                    if (bVal == null) return ascending ? -1 : 1;
                    
                    int result;
                    if (aVal instanceof Number && bVal instanceof Number) {
                        result = Double.compare(
                                ((Number) aVal).doubleValue(),
                                ((Number) bVal).doubleValue()
                        );
                    } else {
                        result = aVal.toString().compareTo(bVal.toString());
                    }
                    
                    return ascending ? result : -result;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 数据过滤
     */
    public List<Map<String, Object>> filterData(
            List<Map<String, Object>> data,
            List<FilterCondition> conditions) {
        
        if (data == null || data.isEmpty() || conditions == null || conditions.isEmpty()) {
            return data;
        }
        
        return data.stream()
                .filter(row -> conditions.stream().allMatch(cond -> matchCondition(row, cond)))
                .collect(Collectors.toList());
    }
    
    /**
     * 检查行是否匹配条件
     */
    private boolean matchCondition(Map<String, Object> row, FilterCondition condition) {
        Object fieldValue = row.get(condition.getField());
        Object filterValue = condition.getValue();
        
        switch (condition.getOperator().toUpperCase()) {
            case "=":
            case "==":
                return Objects.equals(fieldValue, filterValue);
            case "!=":
            case "<>":
                return !Objects.equals(fieldValue, filterValue);
            case ">":
                return compareValues(fieldValue, filterValue) > 0;
            case ">=":
                return compareValues(fieldValue, filterValue) >= 0;
            case "<":
                return compareValues(fieldValue, filterValue) < 0;
            case "<=":
                return compareValues(fieldValue, filterValue) <= 0;
            case "LIKE":
                return fieldValue != null && filterValue != null &&
                        fieldValue.toString().contains(filterValue.toString());
            case "IS NULL":
                return fieldValue == null;
            case "IS NOT NULL":
                return fieldValue != null;
            default:
                return true;
        }
    }
    
    /**
     * 比较两个值
     */
    private int compareValues(Object a, Object b) {
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return 1;
        
        if (a instanceof Number && b instanceof Number) {
            return Double.compare(((Number) a).doubleValue(), ((Number) b).doubleValue());
        }
        
        return a.toString().compareTo(b.toString());
    }
    
    /**
     * 计算统计信息
     */
    public Map<String, Object> calculateStatistics(List<Map<String, Object>> data, String field) {
        Map<String, Object> stats = new LinkedHashMap<>();
        
        if (data == null || data.isEmpty()) {
            stats.put("count", 0);
            stats.put("sum", 0);
            stats.put("avg", 0);
            stats.put("min", 0);
            stats.put("max", 0);
            return stats;
        }
        
        List<BigDecimal> values = data.stream()
                .map(row -> row.get(field))
                .filter(Objects::nonNull)
                .map(this::toBigDecimal)
                .filter(Objects::nonNull)
                .sorted()
                .collect(Collectors.toList());
        
        if (values.isEmpty()) {
            stats.put("count", 0);
            stats.put("sum", BigDecimal.ZERO);
            stats.put("avg", BigDecimal.ZERO);
            stats.put("min", null);
            stats.put("max", null);
            return stats;
        }
        
        int count = values.size();
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avg = sum.divide(BigDecimal.valueOf(count), 4, RoundingMode.HALF_UP);
        BigDecimal min = values.get(0);
        BigDecimal max = values.get(count - 1);
        
        // 中位数
        BigDecimal median;
        if (count % 2 == 0) {
            median = values.get(count / 2 - 1).add(values.get(count / 2))
                    .divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
        } else {
            median = values.get(count / 2);
        }
        
        stats.put("count", count);
        stats.put("sum", sum);
        stats.put("avg", avg);
        stats.put("min", min);
        stats.put("max", max);
        stats.put("median", median);
        
        return stats;
    }
    
    /**
     * 数据透视
     */
    public Map<String, Object> pivotData(
            List<Map<String, Object>> data,
            String rowField,
            String colField,
            String valueField,
            AggregateFunction aggregateFunc) {
        
        if (data == null || data.isEmpty()) {
            return Collections.emptyMap();
        }
        
        // 获取所有列值
        Set<Object> colValues = data.stream()
                .map(row -> row.get(colField))
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        
        // 按行分组
        Map<Object, Map<Object, List<Map<String, Object>>>> rowGroups = new LinkedHashMap<>();
        
        for (Map<String, Object> row : data) {
            Object rowKey = row.get(rowField);
            Object colKey = row.get(colField);
            
            rowGroups.computeIfAbsent(rowKey, k -> new LinkedHashMap<>())
                    .computeIfAbsent(colKey, k -> new ArrayList<>())
                    .add(row);
        }
        
        // 构建结果
        List<String> headers = new ArrayList<>();
        headers.add(rowField);
        colValues.forEach(col -> headers.add(col.toString()));
        
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map.Entry<Object, Map<Object, List<Map<String, Object>>>> rowEntry : rowGroups.entrySet()) {
            Map<String, Object> pivotRow = new LinkedHashMap<>();
            pivotRow.put(rowField, rowEntry.getKey());
            
            for (Object colKey : colValues) {
                List<Map<String, Object>> cellData = rowEntry.getValue().getOrDefault(colKey, Collections.emptyList());
                Object aggregatedValue = cellData.isEmpty() ? null :
                        calculateAggregate(cellData, valueField, aggregateFunc);
                pivotRow.put(colKey.toString(), aggregatedValue);
            }
            
            rows.add(pivotRow);
        }
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("headers", headers);
        result.put("rows", rows);
        
        return result;
    }
    
    /**
     * 计算同比/环比增长率
     */
    public List<Map<String, Object>> calculateGrowthRate(
            List<Map<String, Object>> data,
            String timeField,
            String valueField,
            boolean isYearOverYear) {
        
        if (data == null || data.size() < 2) {
            return data;
        }
        
        // 按时间排序
        List<Map<String, Object>> sorted = sortData(new ArrayList<>(data), timeField, true);
        
        List<Map<String, Object>> result = new ArrayList<>();
        int compareOffset = isYearOverYear ? 12 : 1; // 同比12期，环比1期
        
        for (int i = 0; i < sorted.size(); i++) {
            Map<String, Object> row = new LinkedHashMap<>(sorted.get(i));
            
            BigDecimal currentValue = toBigDecimal(row.get(valueField));
            BigDecimal previousValue = null;
            BigDecimal growthRate = null;
            
            if (i >= compareOffset) {
                previousValue = toBigDecimal(sorted.get(i - compareOffset).get(valueField));
            }
            
            if (currentValue != null && previousValue != null && previousValue.compareTo(BigDecimal.ZERO) != 0) {
                growthRate = currentValue.subtract(previousValue)
                        .divide(previousValue, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            }
            
            row.put("previousValue", previousValue);
            row.put("growthRate", growthRate);
            result.add(row);
        }
        
        return result;
    }
    
    /**
     * 过滤条件类
     */
    public static class FilterCondition {
        private String field;
        private String operator;
        private Object value;
        
        public FilterCondition() {}
        
        public FilterCondition(String field, String operator, Object value) {
            this.field = field;
            this.operator = operator;
            this.value = value;
        }
        
        public String getField() { return field; }
        public void setField(String field) { this.field = field; }
        public String getOperator() { return operator; }
        public void setOperator(String operator) { this.operator = operator; }
        public Object getValue() { return value; }
        public void setValue(Object value) { this.value = value; }
    }
}
