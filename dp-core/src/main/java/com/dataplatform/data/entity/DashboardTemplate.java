package com.dataplatform.data.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 仪表盘模板实体
 * 
 * @author dataplatform
 */
@Data
@TableName("dashboard_template")
public class DashboardTemplate {
    
    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 模板名称 */
    private String name;
    
    /** 分类（销售分析/运营监控/财务概览） */
    private String category;
    
    /** 模板布局JSON */
    private String layoutJson;
    
    /** 缩略图URL */
    private String thumbnail;
    
    /** 描述 */
    private String description;
}
