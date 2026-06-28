package com.dataplatform.data.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 数据质量规则实体
 * 
 * @author dataplatform
 */
@Data
@TableName("data_quality_rule")
public class DataQualityRule {
    
    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 规则名称 */
    private String ruleName;
    
    /** 规则编码 */
    private String ruleCode;
    
    /** 规则类型：completeness/accuracy/consistency/timeliness/uniqueness */
    private String ruleType;
    
    /** 数据源ID */
    private Long dataSourceId;
    
    /** 表名 */
    private String tableName;
    
    /** 字段名 */
    private String columnName;
    
    /** 检查SQL */
    private String checkSql;
    
    /** 阈值 */
    private BigDecimal threshold;
    
    /** 严重级别：low/medium/high */
    private String severity;
    
    /** 描述 */
    private String description;
    
    /** 状态: 1-启用 0-禁用 */
    private Integer status;
    
    /** 创建时间 */
    private LocalDateTime createTime;
    
    /** 更新时间 */
    private LocalDateTime updateTime;
}
