package com.dataplatform.data.dto;

import lombok.Data;
import java.util.List;

/**
 * 全局筛选器配置DTO
 * 
 * @author dataplatform
 */
@Data
public class FilterConfig {
    
    /** 筛选器唯一标识 */
    private String id;
    
    /** 筛选器名称 */
    private String name;
    
    /** 筛选字段 */
    private String field;
    
    /** 筛选器类型: select, date, dateRange, input */
    private String type;
    
    /** 默认值 */
    private Object defaultValue;
    
    /** 选项列表（用于select类型） */
    private List<FilterOption> options;
    
    /** 关联的图表ID列表 */
    private List<String> linkedChartIds;
    
    /** 是否必填 */
    private Boolean required;
    
    /** 占位提示文本 */
    private String placeholder;
    
    /**
     * 筛选器选项
     */
    @Data
    public static class FilterOption {
        /** 选项值 */
        private Object value;
        /** 选项标签 */
        private String label;
    }
}
