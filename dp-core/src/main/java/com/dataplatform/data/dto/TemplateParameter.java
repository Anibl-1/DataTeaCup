package com.dataplatform.data.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 模板参数配置DTO
 * 用于定义报表模板中的参数配置，支持参数化配置（需求 11.3）
 * 
 * 支持的参数类型：
 * - string: 字符串类型
 * - number: 数字类型
 * - integer: 整数类型
 * - date: 日期类型 (YYYY-MM-DD)
 * - datetime: 日期时间类型 (YYYY-MM-DD HH:mm:ss)
 * - boolean: 布尔类型
 * - select: 下拉选择类型
 * - multiselect: 多选类型
 * 
 * @author dataplatform
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateParameter {
    
    /** 参数名称，用于SQL模板中的占位符 ${paramName} */
    private String name;
    
    /** 参数显示标签 */
    private String label;
    
    /** 参数类型：string, number, integer, date, datetime, boolean, select, multiselect */
    private String type;
    
    /** 是否必填 */
    private Boolean required;
    
    /** 默认值 */
    private Object defaultValue;
    
    /** 参数描述/提示信息 */
    private String description;
    
    /** 占位符文本 */
    private String placeholder;
    
    /** 选项列表（用于select/multiselect类型） */
    private List<ParameterOption> options;
    
    /** 最小值（用于number/integer类型） */
    private Number minValue;
    
    /** 最大值（用于number/integer类型） */
    private Number maxValue;
    
    /** 最小长度（用于string类型） */
    private Integer minLength;
    
    /** 最大长度（用于string类型） */
    private Integer maxLength;
    
    /** 正则表达式验证模式（用于string类型） */
    private String pattern;
    
    /** 日期格式（用于date/datetime类型） */
    private String dateFormat;
    
    /** 排序顺序 */
    private Integer sortOrder;
    
    /** 是否在SQL中需要引号包裹（用于字符串类型参数） */
    private Boolean quoteInSql;
    
    /** 级联依赖的参数名（用于参数联动） */
    private String dependsOn;
    
    /** 级联查询SQL（当依赖参数变化时执行） */
    private String cascadeSql;
    
    /**
     * 参数选项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParameterOption {
        /** 选项值 */
        private Object value;
        
        /** 选项显示标签 */
        private String label;
        
        /** 是否禁用 */
        private Boolean disabled;
    }
    
    /**
     * 获取有效的默认值
     * 如果没有设置默认值，根据类型返回合理的默认值
     */
    @JsonIgnore
    public Object getEffectiveDefaultValue() {
        if (defaultValue != null) {
            return defaultValue;
        }
        
        // 根据类型返回默认值
        if (type == null) {
            return null;
        }
        
        switch (type.toLowerCase()) {
            case "boolean":
                return false;
            case "number":
            case "integer":
                return 0;
            case "string":
                return "";
            default:
                return null;
        }
    }
    
    /**
     * 检查参数是否为必填
     */
    public boolean isRequired() {
        return Boolean.TRUE.equals(required);
    }
    
    /**
     * 检查参数是否需要在SQL中加引号
     */
    public boolean shouldQuoteInSql() {
        if (quoteInSql != null) {
            return quoteInSql;
        }
        // 默认情况下，字符串、日期类型需要引号
        if (type == null) {
            return true;
        }
        switch (type.toLowerCase()) {
            case "string":
            case "date":
            case "datetime":
            case "select":
                return true;
            case "number":
            case "integer":
            case "boolean":
                return false;
            default:
                return true;
        }
    }
}
