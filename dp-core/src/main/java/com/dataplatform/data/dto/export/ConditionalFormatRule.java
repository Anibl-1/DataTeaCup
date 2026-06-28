package com.dataplatform.data.dto.export;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 条件格式化规则
 * Conditional formatting rule for export
 * 
 * **Validates: Requirements 23.1**
 * - 导出时保留所有条件格式化样式
 * 
 * @author dataplatform
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConditionalFormatRule {
    
    /**
     * 规则ID
     */
    private String id;
    
    /**
     * 规则名称
     */
    private String name;
    
    /**
     * 优先级（数字越小优先级越高）
     */
    private Integer priority;
    
    /**
     * 是否启用
     */
    @Builder.Default
    private boolean enabled = true;
    
    /**
     * 应用的字段名列表（逗号分隔，为空表示应用到所有字段）
     */
    private String applyToFields;
    
    // ==================== 条件配置 ====================
    
    /**
     * 条件类型 (value, text, date, formula, null, crossField)
     */
    private String conditionType;
    
    /**
     * 比较运算符 (eq, ne, gt, gte, lt, lte, between, notBetween, contains, startsWith, endsWith, regex, isEmpty, isNotEmpty)
     */
    private String operator;
    
    /**
     * 比较值1
     */
    private String value1;
    
    /**
     * 比较值2（用于between等需要两个值的运算符）
     */
    private String value2;
    
    /**
     * 跨字段比较时的目标字段名
     */
    private String compareField;
    
    /**
     * 自定义表达式（用于formula类型）
     */
    private String expression;
    
    // ==================== 样式配置 ====================
    
    /**
     * 字体颜色
     */
    private String fontColor;
    
    /**
     * 字体粗细
     */
    private String fontWeight;
    
    /**
     * 字体样式
     */
    private String fontStyle;
    
    /**
     * 背景颜色
     */
    private String backgroundColor;
    
    /**
     * 边框颜色
     */
    private String borderColor;
    
    /**
     * 边框样式
     */
    private String borderStyle;
    
    /**
     * 评估条件是否满足
     * 
     * @param cellValue 单元格值
     * @param rowData 行数据（用于跨字段比较）
     * @return 是否满足条件
     */
    public boolean evaluate(Object cellValue, java.util.Map<String, Object> rowData) {
        if (!enabled) {
            return false;
        }
        
        if (cellValue == null && !"isEmpty".equals(operator) && !"isNotEmpty".equals(operator)) {
            return false;
        }
        
        switch (conditionType) {
            case "value":
                return evaluateValueCondition(cellValue);
            case "text":
                return evaluateTextCondition(cellValue);
            case "null":
                return evaluateNullCondition(cellValue);
            case "crossField":
                return evaluateCrossFieldCondition(cellValue, rowData);
            default:
                return false;
        }
    }
    
    private boolean evaluateValueCondition(Object cellValue) {
        if (!(cellValue instanceof Number)) {
            return false;
        }
        
        double numValue = ((Number) cellValue).doubleValue();
        double compareValue1 = parseDouble(value1);
        
        switch (operator) {
            case "eq":
                return numValue == compareValue1;
            case "ne":
                return numValue != compareValue1;
            case "gt":
                return numValue > compareValue1;
            case "gte":
                return numValue >= compareValue1;
            case "lt":
                return numValue < compareValue1;
            case "lte":
                return numValue <= compareValue1;
            case "between":
                double compareValue2 = parseDouble(value2);
                return numValue >= compareValue1 && numValue <= compareValue2;
            case "notBetween":
                double cv2 = parseDouble(value2);
                return numValue < compareValue1 || numValue > cv2;
            default:
                return false;
        }
    }
    
    private boolean evaluateTextCondition(Object cellValue) {
        String strValue = cellValue != null ? cellValue.toString() : "";
        String compareStr = value1 != null ? value1 : "";
        
        switch (operator) {
            case "contains":
                return strValue.contains(compareStr);
            case "notContains":
                return !strValue.contains(compareStr);
            case "startsWith":
                return strValue.startsWith(compareStr);
            case "endsWith":
                return strValue.endsWith(compareStr);
            case "equals":
                return strValue.equals(compareStr);
            case "notEquals":
                return !strValue.equals(compareStr);
            case "regex":
                try {
                    return strValue.matches(compareStr);
                } catch (Exception e) {
                    return false;
                }
            default:
                return false;
        }
    }
    
    private boolean evaluateNullCondition(Object cellValue) {
        boolean isEmpty = cellValue == null || 
                         (cellValue instanceof String && ((String) cellValue).isEmpty());
        
        switch (operator) {
            case "isEmpty":
                return isEmpty;
            case "isNotEmpty":
                return !isEmpty;
            default:
                return false;
        }
    }
    
    private boolean evaluateCrossFieldCondition(Object cellValue, java.util.Map<String, Object> rowData) {
        if (rowData == null || compareField == null) {
            return false;
        }
        
        Object compareValue = rowData.get(compareField);
        if (cellValue == null || compareValue == null) {
            return false;
        }
        
        // 尝试数值比较
        if (cellValue instanceof Number && compareValue instanceof Number) {
            double v1 = ((Number) cellValue).doubleValue();
            double v2 = ((Number) compareValue).doubleValue();
            
            switch (operator) {
                case "eq":
                    return v1 == v2;
                case "ne":
                    return v1 != v2;
                case "gt":
                    return v1 > v2;
                case "gte":
                    return v1 >= v2;
                case "lt":
                    return v1 < v2;
                case "lte":
                    return v1 <= v2;
                default:
                    return false;
            }
        }
        
        // 字符串比较
        String s1 = cellValue.toString();
        String s2 = compareValue.toString();
        int cmp = s1.compareTo(s2);
        
        switch (operator) {
            case "eq":
                return cmp == 0;
            case "ne":
                return cmp != 0;
            case "gt":
                return cmp > 0;
            case "gte":
                return cmp >= 0;
            case "lt":
                return cmp < 0;
            case "lte":
                return cmp <= 0;
            default:
                return false;
        }
    }
    
    private double parseDouble(String value) {
        try {
            return value != null ? Double.parseDouble(value) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
