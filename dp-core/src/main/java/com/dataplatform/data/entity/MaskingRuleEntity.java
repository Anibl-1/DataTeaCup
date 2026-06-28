package com.dataplatform.data.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 数据脱敏规则实体
 * 支持字段级脱敏规则配置
 * 
 * @author dataplatform
 * @see com.dataplatform.service.masking.MaskingRule 脱敏规则DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("masking_rule")
public class MaskingRuleEntity {
    
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 规则名称
     */
    private String name;
    
    /**
     * 数据源ID（可选，为空表示适用所有数据源）
     */
    private Long dataSourceId;
    
    /**
     * 表名（可选，为空表示适用所有表）
     */
    private String tableName;
    
    /**
     * 字段名（精确匹配）
     */
    private String fieldName;
    
    /**
     * 字段名匹配模式（正则表达式）
     */
    private String fieldPattern;
    
    /**
     * 敏感字段类型
     * @see com.dataplatform.service.masking.SensitiveFieldType
     */
    private String sensitiveType;
    
    /**
     * 脱敏策略类型
     * @see com.dataplatform.service.masking.MaskingStrategyType
     */
    private String strategyType;
    
    /**
     * 策略配置JSON
     * 不同策略类型有不同的配置参数：
     * - MASK: {"maskStart": 3, "maskEnd": 4, "maskChar": "*"}
     * - TRUNCATE: {"keepLength": 1, "suffix": "*"}
     * - HASH: {"algorithm": "SHA-256", "displayLength": 8}
     * - REPLACE: {"replacement": "***"}
     * - RANGE: {"ranges": [[0, 10000], [10000, 50000], [50000, 100000]]}
     * - REGEX: {"pattern": "...", "replacement": "..."}
     */
    private String strategyConfig;
    
    /**
     * 规则优先级（数字越小优先级越高）
     */
    private Integer priority;
    
    /**
     * 是否启用
     */
    @TableField("enabled")
    private Boolean enabled;
    
    /**
     * 规则描述
     */
    private String description;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    
    /**
     * 创建人ID
     */
    private Long createBy;
    
    /**
     * 更新人ID
     */
    private Long updateBy;
}
