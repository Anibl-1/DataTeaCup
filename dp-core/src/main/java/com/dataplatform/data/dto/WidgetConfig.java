package com.dataplatform.data.dto;

import lombok.Data;

/**
 * 仪表盘组件配置DTO
 * 
 * @author dataplatform
 */
@Data
public class WidgetConfig {
    
    /** 组件唯一标识 */
    private String i;
    
    /** 网格X坐标 */
    private Integer x;
    
    /** 网格Y坐标 */
    private Integer y;
    
    /** 宽度（网格单位） */
    private Integer w;
    
    /** 高度（网格单位） */
    private Integer h;
    
    /** 组件类型: chart, kpi, text, filter */
    private String type;
    
    /** 组件配置JSON（根据type不同，配置内容不同） */
    private Object config;
}
