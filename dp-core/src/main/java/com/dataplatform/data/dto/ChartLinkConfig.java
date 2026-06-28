package com.dataplatform.data.dto;

import lombok.Data;

/**
 * 图表联动配置DTO
 * 
 * @author dataplatform
 */
@Data
public class ChartLinkConfig {
    
    /** 联动配置ID */
    private String id;
    
    /** 源图表ID */
    private String sourceChartId;
    
    /** 目标图表ID */
    private String targetChartId;
    
    /** 源字段 */
    private String sourceField;
    
    /** 目标字段 */
    private String targetField;
    
    /** 联动类型: filter(筛选), drillDown(下钻), highlight(高亮) */
    private String linkType;
}
