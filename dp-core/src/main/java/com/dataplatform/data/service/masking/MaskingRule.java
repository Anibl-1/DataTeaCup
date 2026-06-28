package com.dataplatform.data.service.masking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 脱敏规则
 * 定义字段脱敏的完整配置
 * 
 * @author dataplatform
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaskingRule {
    
    /**
     * 规则ID
     */
    private Long id;
    
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
     */
    private SensitiveFieldType sensitiveType;
    
    /**
     * 脱敏策略类型
     */
    private String strategyType;
    
    /**
     * 策略配置参数
     */
    private Map<String, Object> strategyConfig;
    
    /**
     * 适用角色ID列表（为空表示适用所有角色）
     */
    private List<Long> roleIds;
    
    /**
     * 规则优先级（数字越小优先级越高）
     */
    private Integer priority;
    
    /**
     * 是否启用
     */
    private Boolean enabled;
    
    /**
     * 规则描述
     */
    private String description;
    
    /**
     * 判断规则是否匹配指定字段
     * 
     * @param targetFieldName 目标字段名
     * @return 是否匹配
     */
    public boolean matchesField(String targetFieldName) {
        if (targetFieldName == null) {
            return false;
        }
        
        // 精确匹配
        if (fieldName != null && fieldName.equalsIgnoreCase(targetFieldName)) {
            return true;
        }
        
        // 模式匹配
        if (fieldPattern != null && !fieldPattern.isEmpty()) {
            return targetFieldName.matches(fieldPattern);
        }
        
        return false;
    }
    
    /**
     * 判断规则是否适用于指定角色
     * 
     * @param targetRoleIds 目标角色ID列表
     * @return 是否适用
     */
    public boolean appliesToRoles(List<Long> targetRoleIds) {
        // 未配置角色限制，适用所有角色
        if (roleIds == null || roleIds.isEmpty()) {
            return true;
        }
        
        // 检查是否有交集
        if (targetRoleIds == null || targetRoleIds.isEmpty()) {
            return false;
        }
        
        return targetRoleIds.stream().anyMatch(roleIds::contains);
    }
    
    /**
     * 判断规则是否有效
     * 
     * @return 是否有效
     */
    public boolean isValid() {
        return enabled != null && enabled 
            && strategyType != null && !strategyType.isEmpty()
            && (fieldName != null || fieldPattern != null);
    }
}
