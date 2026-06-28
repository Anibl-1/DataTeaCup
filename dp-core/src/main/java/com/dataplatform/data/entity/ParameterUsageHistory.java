package com.dataplatform.data.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 参数使用历史记录实体
 * 用于记录用户对报表参数的使用历史，支持智能推荐默认值
 * 
 * @validates 需求 13.3 - 基于用户历史使用记录智能推荐参数默认值
 */
@Data
@TableName("parameter_usage_history")
public class ParameterUsageHistory {
    
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 报表ID（可选，用于报表级别的推荐）
     */
    private Long reportId;
    
    /**
     * 图表ID（可选，用于图表级别的推荐）
     */
    private Long chartId;
    
    /**
     * 参数名称
     */
    private String paramName;
    
    /**
     * 参数值（JSON格式存储，支持复杂类型）
     */
    private String paramValue;
    
    /**
     * 参数值类型（string, number, date, array, object）
     */
    private String valueType;
    
    /**
     * 使用次数
     */
    private Integer usageCount;
    
    /**
     * 最后使用时间
     */
    private LocalDateTime lastUsedAt;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
