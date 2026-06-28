package com.dataplatform.data.entity;

import lombok.Data;
import java.util.Date;
import java.util.List;

/**
 * 页面定义实体类
 * 
 * @author dataplatform
 */
@Data
public class PageDefinition {
    /** 页面ID */
    private Long id;
    
    /** 页面名称 */
    private String pageName;
    
    /** 页面编码（唯一标识） */
    private String pageCode;
    
    /** 布局配置（JSON格式） */
    private String layoutConfig;
    
    /** 页面描述 */
    private String description;
    
    /** 页面主题 */
    private String theme;
    
    /** 主题配置（JSON格式，自定义主题时使用） */
    private String themeConfig;
    
    /** 参数面板配置（JSON格式） */
    private String parameterPanel;
    
    /** 状态：1-启用，0-禁用 */
    private Integer status;
    
    /** 移动端启用：1-启用，0-禁用 */
    private Integer mobileEnabled;
    
    /** 布局模式: desktop-桌面端 mobile-移动端 bigscreen-大屏 */
    private String layoutMode;
    
    /** 大屏配置（JSON格式，layoutMode='bigscreen'时使用） */
    private String bigscreenConfig;
    
    /** 移动端布局配置（JSON格式，layoutMode='mobile'时使用） */
    private String mobileLayoutConfig;
    
    /** 所属大屏项目ID */
    private Long projectId;
    
    /** 创建人ID */
    private Long createBy;
    
    /** 创建时间 */
    private Date createTime;
    
    /** 更新时间 */
    private Date updateTime;
    
    /** 页面图表列表（关联查询） */
    private List<PageChart> charts;
}

