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
 * 脱敏规则角色关联实体
 * 支持角色级脱敏配置，不同角色可以有不同的脱敏级别
 * 
 * @author dataplatform
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("masking_rule_role")
public class MaskingRuleRole {
    
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 脱敏规则ID
     */
    private Long ruleId;
    
    /**
     * 角色ID
     */
    private Long roleId;
    
    /**
     * 是否对该角色启用此规则
     * true: 该角色查看数据时应用脱敏规则
     * false: 该角色可以查看原始数据
     */
    @TableField("enabled")
    private Boolean enabled;
    
    /**
     * 脱敏级别
     * 1 - 完全脱敏（应用完整的脱敏策略）
     * 2 - 部分脱敏（显示更多原始数据）
     * 3 - 不脱敏（显示原始数据）
     */
    private Integer maskingLevel;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
