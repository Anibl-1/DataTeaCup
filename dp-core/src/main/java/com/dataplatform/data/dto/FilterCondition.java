package com.dataplatform.data.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 筛选条件
 */
public class FilterCondition {
    /**
     * 字段名
     */
    private String field;
    
    /**
     * 操作符：eq, ne, contains, notContains, startsWith, endsWith, gt, gte, lt, lte, isNull, isNotNull
     */
    private String operator;
    
    /**
     * 筛选值
     */
    private Object value;
    
    public FilterCondition() {
    }
    
    public FilterCondition(String field, String operator, Object value) {
        this.field = field;
        this.operator = operator;
        this.value = value;
    }
    
    public String getField() {
        return field;
    }
    
    public void setField(String field) {
        this.field = field;
    }
    
    public String getOperator() {
        return operator;
    }
    
    public void setOperator(String operator) {
        this.operator = operator;
    }
    
    public Object getValue() {
        return value;
    }
    
    public void setValue(Object value) {
        this.value = value;
    }
}

