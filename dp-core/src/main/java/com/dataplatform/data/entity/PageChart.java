package com.dataplatform.data.entity;

import lombok.Data;
import java.util.Date;

/**
 * 页面图表关联实体类
 * 
 * @author dataplatform
 */
@Data
public class PageChart {
    /** 关联ID */
    private Long id;
    
    /** 页面ID */
    private Long pageId;
    
    /** 图表ID（引用模式使用） */
    private Long chartId;
    
    /** 图表模式：inline-内联(页面私有), referenced-引用(公共图表) */
    private String mode;
    
    /** 内联图表配置（JSON格式，mode=inline时使用） */
    private String inlineConfig;
    
    /** X坐标（网格位置，兼容旧格式） */
    private Integer x;
    
    /** Y坐标（网格位置，兼容旧格式） */
    private Integer y;
    
    /** 宽度（网格单位，12列网格，兼容旧格式） */
    private Integer w;
    
    /** 高度（网格单位，兼容旧格式） */
    private Integer h;
    
    /** 左坐标（像素，新格式） */
    private Integer left;
    
    /** 上坐标（像素，新格式） */
    private Integer top;
    
    /** 宽度（像素，新格式） */
    private Integer width;
    
    /** 高度（像素，新格式） */
    private Integer height;
    
    /** 排序顺序 */
    private Integer sortOrder;
    
    /** 创建时间 */
    private Date createTime;
    
    /** 更新时间 */
    private Date updateTime;
    
    /** 图表信息（关联查询） */
    private ChartDefinition chart;
}

