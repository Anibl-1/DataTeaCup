package com.dataplatform.data.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 仪表盘配置实体
 * 
 * @author dataplatform
 */
@Data
@TableName("dashboard_config")
public class DashboardConfig {
    
    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 仪表盘名称 */
    private String name;
    
    /** 描述 */
    private String description;
    
    /** 布局配置JSON */
    private String layoutJson;
    
    /** 全局筛选器配置JSON */
    private String globalFiltersJson;
    
    /** 图表联动配置JSON */
    private String linkConfigJson;
    
    /** 模板ID */
    private Long templateId;
    
    /** 创建人 */
    private Long createBy;
    
    /** 创建时间 */
    private LocalDateTime createTime;
    
    /** 更新时间 */
    private LocalDateTime updateTime;
    
    /** 状态: 1-启用 0-禁用 */
    private Integer status;
}
