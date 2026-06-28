package com.dataplatform.common.dto;

/**
 * 筛选条件
 */
public class FilterCondition {
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
