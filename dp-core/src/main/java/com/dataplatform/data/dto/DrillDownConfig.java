package com.dataplatform.data.dto;

import lombok.Data;

import java.util.List;

/**
 * 下钻配置DTO
 * 
 * @author dataplatform
 */
@Data
public class DrillDownConfig {
    
    /** 图表ID */
    private Long chartId;
    
    /** 下钻层级配置 */
    private List<DrillLevel> levels;
    
    /** 当前层级（从0开始） */
    private Integer currentLevel;
    
    /**
     * 下钻层级定义
     */
    @Data
    public static class DrillLevel {
        /** 字段名 */
        private String field;
        
        /** 粒度: year, quarter, month, day */
        private String granularity;
    }
}
