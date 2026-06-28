package com.dataplatform.data.service.masking;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dataplatform.common.exception.BusinessException;
import com.dataplatform.common.exception.ErrorCode;
import com.dataplatform.data.entity.MaskingRuleEntity;
import com.dataplatform.data.entity.MaskingRuleRole;
import com.dataplatform.data.mapper.MaskingRuleMapper;
import com.dataplatform.data.mapper.MaskingRuleRoleMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 脱敏规则服务实现
 * 支持字段级脱敏规则配置（需求 5.7）和角色级脱敏配置（需求 5.8）
 * 
 * @author dataplatform
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MaskingRuleServiceImpl implements MaskingRuleService {
    
    private final MaskingRuleMapper maskingRuleMapper;
    private final MaskingRuleRoleMapper maskingRuleRoleMapper;
    private final ObjectMapper objectMapper;
    
    // ==================== CRUD 操作 ====================
    
    @Override
    @Transactional
    public MaskingRuleEntity createRule(MaskingRuleEntity rule) {
        validateRule(rule);
        
        // 检查规则名称是否重复
        if (StringUtils.hasText(rule.getName())) {
            MaskingRuleEntity existing = maskingRuleMapper.findByName(rule.getName());
            if (existing != null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "规则名称已存在");
            }
        }
        
        rule.setCreateTime(LocalDateTime.now());
        rule.setUpdateTime(LocalDateTime.now());
        if (rule.getEnabled() == null) {
            rule.setEnabled(true);
        }
        if (rule.getPriority() == null) {
            rule.setPriority(0);
        }
        
        maskingRuleMapper.insert(rule);
        log.info("Created masking rule: id={}, name={}", rule.getId(), rule.getName());
        return rule;
    }
    
    @Override
    @Transactional
    public MaskingRuleEntity updateRule(MaskingRuleEntity rule) {
        if (rule.getId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "规则ID不能为空");
        }
        
        MaskingRuleEntity existing = maskingRuleMapper.selectById(rule.getId());
        if (existing == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "脱敏规则不存在");
        }
        
        validateRule(rule);
        
        // 检查规则名称是否重复（排除自身）
        if (StringUtils.hasText(rule.getName())) {
            MaskingRuleEntity byName = maskingRuleMapper.findByName(rule.getName());
            if (byName != null && !byName.getId().equals(rule.getId())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "规则名称已存在");
            }
        }
        
        rule.setUpdateTime(LocalDateTime.now());
        maskingRuleMapper.updateById(rule);
        log.info("Updated masking rule: id={}", rule.getId());
        return maskingRuleMapper.selectById(rule.getId());
    }
    
    @Override
    @Transactional
    public void deleteRule(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "规则ID不能为空");
        }
        
        MaskingRuleEntity existing = maskingRuleMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "脱敏规则不存在");
        }
        
        // 删除角色关联
        maskingRuleRoleMapper.deleteByRuleId(id);
        
        // 删除规则
        maskingRuleMapper.deleteById(id);
        log.info("Deleted masking rule: id={}", id);
    }
    
    @Override
    public MaskingRuleEntity getRuleById(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "规则ID不能为空");
        }
        
        MaskingRuleEntity rule = maskingRuleMapper.selectById(id);
        if (rule == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "脱敏规则不存在");
        }
        return rule;
    }
    
    @Override
    public List<MaskingRuleEntity> getAllRules() {
        return maskingRuleMapper.selectList(
                new LambdaQueryWrapper<MaskingRuleEntity>()
                        .orderByAsc(MaskingRuleEntity::getPriority)
                        .orderByDesc(MaskingRuleEntity::getCreateTime)
        );
    }
    
    @Override
    public List<MaskingRuleEntity> getRulesByPage(int page, int pageSize) {
        if (page < 1) page = 1;
        if (pageSize < 1) pageSize = 10;
        
        Page<MaskingRuleEntity> pageObj = new Page<>(page, pageSize);
        Page<MaskingRuleEntity> result = maskingRuleMapper.selectPage(pageObj,
                new LambdaQueryWrapper<MaskingRuleEntity>()
                        .orderByAsc(MaskingRuleEntity::getPriority)
                        .orderByDesc(MaskingRuleEntity::getCreateTime)
        );
        return result.getRecords();
    }
    
    @Override
    public long getRuleCount() {
        return maskingRuleMapper.selectCount(null);
    }
    
    @Override
    @Transactional
    public void toggleRuleEnabled(Long id, boolean enabled) {
        MaskingRuleEntity rule = getRuleById(id);
        rule.setEnabled(enabled);
        rule.setUpdateTime(LocalDateTime.now());
        maskingRuleMapper.updateById(rule);
        log.info("Toggled masking rule enabled: id={}, enabled={}", id, enabled);
    }
    
    // ==================== 查询方法 ====================
    
    @Override
    public List<MaskingRuleEntity> findByFieldName(String fieldName) {
        if (!StringUtils.hasText(fieldName)) {
            return Collections.emptyList();
        }
        return maskingRuleMapper.findByFieldName(fieldName);
    }
    
    @Override
    public List<MaskingRuleEntity> findByDataSource(Long dataSourceId) {
        if (dataSourceId == null) {
            return maskingRuleMapper.findAllEnabled();
        }
        return maskingRuleMapper.findByDataSource(dataSourceId);
    }
    
    @Override
    public List<MaskingRuleEntity> findBySensitiveType(String sensitiveType) {
        if (!StringUtils.hasText(sensitiveType)) {
            return Collections.emptyList();
        }
        return maskingRuleMapper.findBySensitiveType(sensitiveType);
    }
    
    @Override
    public List<MaskingRuleEntity> getApplicableRules(Long dataSourceId, String tableName, 
                                                       String fieldName, Long roleId) {
        return getApplicableRules(dataSourceId, tableName, fieldName, 
                roleId != null ? Collections.singletonList(roleId) : Collections.emptyList());
    }
    
    @Override
    public List<MaskingRuleEntity> getApplicableRules(Long dataSourceId, String tableName, 
                                                       String fieldName, List<Long> roleIds) {
        if (!StringUtils.hasText(fieldName)) {
            return Collections.emptyList();
        }
        
        // 获取字段匹配的规则
        List<MaskingRuleEntity> rules = maskingRuleMapper.findApplicableRules(dataSourceId, tableName, fieldName);
        
        if (rules.isEmpty()) {
            return rules;
        }
        
        // 如果没有指定角色，返回所有匹配的规则
        if (roleIds == null || roleIds.isEmpty()) {
            return rules;
        }
        
        // 过滤角色适用的规则
        return rules.stream()
                .filter(rule -> isRuleApplicableToRoles(rule.getId(), roleIds))
                .collect(Collectors.toList());
    }
    
    /**
     * 检查规则是否适用于指定角色
     */
    private boolean isRuleApplicableToRoles(Long ruleId, List<Long> roleIds) {
        List<MaskingRuleRole> ruleRoles = maskingRuleRoleMapper.findByRuleId(ruleId);
        
        // 如果规则没有配置角色关联，则适用于所有角色
        if (ruleRoles.isEmpty()) {
            return true;
        }
        
        // 检查是否有任一角色启用了该规则
        return ruleRoles.stream()
                .anyMatch(rr -> roleIds.contains(rr.getRoleId()) && Boolean.TRUE.equals(rr.getEnabled()));
    }
    
    // ==================== 角色级脱敏配置（需求 5.8）====================
    
    @Override
    @Transactional
    public void assignRoleToRule(Long ruleId, Long roleId, Integer maskingLevel) {
        validateRuleExists(ruleId);
        
        if (roleId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "角色ID不能为空");
        }
        
        if (maskingLevel == null || maskingLevel < 1 || maskingLevel > 3) {
            maskingLevel = 1; // 默认完全脱敏
        }
        
        // 检查是否已存在关联
        MaskingRuleRole existing = maskingRuleRoleMapper.findByRuleIdAndRoleId(ruleId, roleId);
        if (existing != null) {
            // 更新现有关联
            existing.setMaskingLevel(maskingLevel);
            existing.setEnabled(true);
            existing.setUpdateTime(LocalDateTime.now());
            maskingRuleRoleMapper.updateById(existing);
        } else {
            // 创建新关联
            MaskingRuleRole ruleRole = MaskingRuleRole.builder()
                    .ruleId(ruleId)
                    .roleId(roleId)
                    .maskingLevel(maskingLevel)
                    .enabled(true)
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();
            maskingRuleRoleMapper.insert(ruleRole);
        }
        
        log.info("Assigned role to masking rule: ruleId={}, roleId={}, maskingLevel={}", 
                ruleId, roleId, maskingLevel);
    }
    
    @Override
    @Transactional
    public void removeRoleFromRule(Long ruleId, Long roleId) {
        validateRuleExists(ruleId);
        
        if (roleId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "角色ID不能为空");
        }
        
        MaskingRuleRole existing = maskingRuleRoleMapper.findByRuleIdAndRoleId(ruleId, roleId);
        if (existing != null) {
            maskingRuleRoleMapper.deleteById(existing.getId());
            log.info("Removed role from masking rule: ruleId={}, roleId={}", ruleId, roleId);
        }
    }
    
    @Override
    @Transactional
    public void assignRolesToRule(Long ruleId, List<Long> roleIds, Integer maskingLevel) {
        validateRuleExists(ruleId);
        
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        
        for (Long roleId : roleIds) {
            assignRoleToRule(ruleId, roleId, maskingLevel);
        }
    }
    
    @Override
    @Transactional
    public void updateRoleMaskingLevel(Long ruleId, Long roleId, Integer maskingLevel) {
        validateRuleExists(ruleId);
        
        if (roleId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "角色ID不能为空");
        }
        
        if (maskingLevel == null || maskingLevel < 1 || maskingLevel > 3) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "脱敏级别必须为1-3");
        }
        
        MaskingRuleRole existing = maskingRuleRoleMapper.findByRuleIdAndRoleId(ruleId, roleId);
        if (existing == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "角色规则关联不存在");
        }
        
        existing.setMaskingLevel(maskingLevel);
        existing.setUpdateTime(LocalDateTime.now());
        maskingRuleRoleMapper.updateById(existing);
        
        log.info("Updated role masking level: ruleId={}, roleId={}, maskingLevel={}", 
                ruleId, roleId, maskingLevel);
    }
    
    @Override
    public List<MaskingRuleEntity> getRulesByRole(Long roleId) {
        if (roleId == null) {
            return Collections.emptyList();
        }
        
        List<Long> ruleIds = maskingRuleRoleMapper.findEnabledRuleIdsByRoleId(roleId);
        if (ruleIds.isEmpty()) {
            return Collections.emptyList();
        }
        
        return maskingRuleMapper.selectBatchIds(ruleIds);
    }
    
    @Override
    public List<MaskingRuleRole> getRuleRoles(Long ruleId) {
        if (ruleId == null) {
            return Collections.emptyList();
        }
        return maskingRuleRoleMapper.findByRuleId(ruleId);
    }
    
    @Override
    public Integer getRoleMaskingLevel(Long ruleId, Long roleId) {
        if (ruleId == null || roleId == null) {
            return null;
        }
        
        MaskingRuleRole ruleRole = maskingRuleRoleMapper.findByRuleIdAndRoleId(ruleId, roleId);
        return ruleRole != null ? ruleRole.getMaskingLevel() : null;
    }
    
    // ==================== 转换方法 ====================
    
    @Override
    public MaskingRule toMaskingRule(MaskingRuleEntity entity) {
        if (entity == null) {
            return null;
        }
        
        Map<String, Object> strategyConfig = parseStrategyConfig(entity.getStrategyConfig());
        List<Long> roleIds = maskingRuleRoleMapper.findEnabledRoleIdsByRuleId(entity.getId());
        
        return MaskingRule.builder()
                .id(entity.getId())
                .fieldName(entity.getFieldName())
                .fieldPattern(entity.getFieldPattern())
                .sensitiveType(parseSensitiveType(entity.getSensitiveType()))
                .strategyType(entity.getStrategyType())
                .strategyConfig(strategyConfig)
                .roleIds(roleIds)
                .priority(entity.getPriority())
                .enabled(entity.getEnabled())
                .description(entity.getDescription())
                .build();
    }
    
    @Override
    public List<MaskingRule> toMaskingRules(List<MaskingRuleEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream()
                .map(this::toMaskingRule)
                .collect(Collectors.toList());
    }
    
    // ==================== 私有方法 ====================
    
    private void validateRule(MaskingRuleEntity rule) {
        if (rule == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "规则不能为空");
        }
        
        if (!StringUtils.hasText(rule.getName())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "规则名称不能为空");
        }
        
        if (!StringUtils.hasText(rule.getFieldName()) && !StringUtils.hasText(rule.getFieldPattern())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "字段名或字段模式至少需要配置一个");
        }
        
        if (!StringUtils.hasText(rule.getStrategyType())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "脱敏策略类型不能为空");
        }
        
        // 验证策略类型是否有效
        try {
            MaskingStrategyType.valueOf(rule.getStrategyType());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "无效的脱敏策略类型: " + rule.getStrategyType());
        }
    }
    
    private void validateRuleExists(Long ruleId) {
        if (ruleId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "规则ID不能为空");
        }
        
        MaskingRuleEntity rule = maskingRuleMapper.selectById(ruleId);
        if (rule == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "脱敏规则不存在");
        }
    }
    
    private Map<String, Object> parseStrategyConfig(String configJson) {
        if (!StringUtils.hasText(configJson)) {
            return Collections.emptyMap();
        }
        
        try {
            return objectMapper.readValue(configJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse strategy config: {}", configJson, e);
            return Collections.emptyMap();
        }
    }
    
    private SensitiveFieldType parseSensitiveType(String type) {
        if (!StringUtils.hasText(type)) {
            return SensitiveFieldType.CUSTOM;
        }
        
        try {
            return SensitiveFieldType.valueOf(type);
        } catch (IllegalArgumentException e) {
            return SensitiveFieldType.CUSTOM;
        }
    }
}
