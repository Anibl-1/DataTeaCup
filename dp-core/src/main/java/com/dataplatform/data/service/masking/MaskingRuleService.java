package com.dataplatform.data.service.masking;

import com.dataplatform.data.entity.MaskingRuleEntity;
import com.dataplatform.data.entity.MaskingRuleRole;

import java.util.List;

/**
 * 脱敏规则服务接口
 * 支持字段级脱敏规则配置（需求 5.7）和角色级脱敏配置（需求 5.8）
 * 
 * @author dataplatform
 */
public interface MaskingRuleService {
    
    // ==================== CRUD 操作 ====================
    
    /**
     * 创建脱敏规则
     * 
     * @param rule 脱敏规则实体
     * @return 创建后的规则（包含ID）
     */
    MaskingRuleEntity createRule(MaskingRuleEntity rule);
    
    /**
     * 更新脱敏规则
     * 
     * @param rule 脱敏规则实体
     * @return 更新后的规则
     */
    MaskingRuleEntity updateRule(MaskingRuleEntity rule);
    
    /**
     * 删除脱敏规则
     * 
     * @param id 规则ID
     */
    void deleteRule(Long id);
    
    /**
     * 根据ID查询脱敏规则
     * 
     * @param id 规则ID
     * @return 脱敏规则实体
     */
    MaskingRuleEntity getRuleById(Long id);
    
    /**
     * 查询所有脱敏规则
     * 
     * @return 脱敏规则列表
     */
    List<MaskingRuleEntity> getAllRules();
    
    /**
     * 分页查询脱敏规则
     * 
     * @param page 页码（从1开始）
     * @param pageSize 每页大小
     * @return 脱敏规则列表
     */
    List<MaskingRuleEntity> getRulesByPage(int page, int pageSize);
    
    /**
     * 查询脱敏规则总数
     * 
     * @return 规则总数
     */
    long getRuleCount();
    
    /**
     * 启用/禁用脱敏规则
     * 
     * @param id 规则ID
     * @param enabled 是否启用
     */
    void toggleRuleEnabled(Long id, boolean enabled);
    
    // ==================== 查询方法 ====================
    
    /**
     * 根据字段名查询脱敏规则
     * 
     * @param fieldName 字段名
     * @return 脱敏规则列表
     */
    List<MaskingRuleEntity> findByFieldName(String fieldName);
    
    /**
     * 根据数据源ID查询脱敏规则
     * 
     * @param dataSourceId 数据源ID
     * @return 脱敏规则列表
     */
    List<MaskingRuleEntity> findByDataSource(Long dataSourceId);
    
    /**
     * 根据敏感类型查询脱敏规则
     * 
     * @param sensitiveType 敏感类型
     * @return 脱敏规则列表
     */
    List<MaskingRuleEntity> findBySensitiveType(String sensitiveType);
    
    /**
     * 获取适用的脱敏规则
     * 根据数据源、表名、字段名和角色ID查询适用的脱敏规则
     * 
     * @param dataSourceId 数据源ID
     * @param tableName 表名（可选）
     * @param fieldName 字段名
     * @param roleId 角色ID
     * @return 适用的脱敏规则列表（按优先级排序）
     */
    List<MaskingRuleEntity> getApplicableRules(Long dataSourceId, String tableName, String fieldName, Long roleId);
    
    /**
     * 获取适用的脱敏规则（多角色）
     * 
     * @param dataSourceId 数据源ID
     * @param tableName 表名（可选）
     * @param fieldName 字段名
     * @param roleIds 角色ID列表
     * @return 适用的脱敏规则列表（按优先级排序）
     */
    List<MaskingRuleEntity> getApplicableRules(Long dataSourceId, String tableName, String fieldName, List<Long> roleIds);
    
    // ==================== 角色级脱敏配置（需求 5.8）====================
    
    /**
     * 为规则分配角色
     * 
     * @param ruleId 规则ID
     * @param roleId 角色ID
     * @param maskingLevel 脱敏级别（1-完全脱敏，2-部分脱敏，3-不脱敏）
     */
    void assignRoleToRule(Long ruleId, Long roleId, Integer maskingLevel);
    
    /**
     * 从规则移除角色
     * 
     * @param ruleId 规则ID
     * @param roleId 角色ID
     */
    void removeRoleFromRule(Long ruleId, Long roleId);
    
    /**
     * 批量为规则分配角色
     * 
     * @param ruleId 规则ID
     * @param roleIds 角色ID列表
     * @param maskingLevel 脱敏级别
     */
    void assignRolesToRule(Long ruleId, List<Long> roleIds, Integer maskingLevel);
    
    /**
     * 更新角色的脱敏级别
     * 
     * @param ruleId 规则ID
     * @param roleId 角色ID
     * @param maskingLevel 脱敏级别
     */
    void updateRoleMaskingLevel(Long ruleId, Long roleId, Integer maskingLevel);
    
    /**
     * 根据角色查询脱敏规则
     * 
     * @param roleId 角色ID
     * @return 脱敏规则列表
     */
    List<MaskingRuleEntity> getRulesByRole(Long roleId);
    
    /**
     * 查询规则关联的角色配置
     * 
     * @param ruleId 规则ID
     * @return 角色关联列表
     */
    List<MaskingRuleRole> getRuleRoles(Long ruleId);
    
    /**
     * 获取角色对特定规则的脱敏级别
     * 
     * @param ruleId 规则ID
     * @param roleId 角色ID
     * @return 脱敏级别（1-完全脱敏，2-部分脱敏，3-不脱敏），如果未配置返回null
     */
    Integer getRoleMaskingLevel(Long ruleId, Long roleId);
    
    // ==================== 转换方法 ====================
    
    /**
     * 将实体转换为DTO
     * 
     * @param entity 脱敏规则实体
     * @return 脱敏规则DTO
     */
    MaskingRule toMaskingRule(MaskingRuleEntity entity);
    
    /**
     * 将实体列表转换为DTO列表
     * 
     * @param entities 脱敏规则实体列表
     * @return 脱敏规则DTO列表
     */
    List<MaskingRule> toMaskingRules(List<MaskingRuleEntity> entities);
}
