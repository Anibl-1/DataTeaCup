package com.dataplatform.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dataplatform.data.entity.MaskingRuleRole;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 脱敏规则角色关联 Mapper 接口
 * 支持角色级脱敏配置（需求 5.8）
 * 
 * @author dataplatform
 */
@Mapper
public interface MaskingRuleRoleMapper extends BaseMapper<MaskingRuleRole> {
    
    /**
     * 根据规则ID查询角色关联
     * 
     * @param ruleId 规则ID
     * @return 角色关联列表
     */
    @Select("SELECT * FROM masking_rule_role WHERE rule_id = #{ruleId}")
    List<MaskingRuleRole> findByRuleId(@Param("ruleId") Long ruleId);
    
    /**
     * 根据角色ID查询规则关联
     * 
     * @param roleId 角色ID
     * @return 角色关联列表
     */
    @Select("SELECT * FROM masking_rule_role WHERE role_id = #{roleId}")
    List<MaskingRuleRole> findByRoleId(@Param("roleId") Long roleId);
    
    /**
     * 根据规则ID和角色ID查询关联
     * 
     * @param ruleId 规则ID
     * @param roleId 角色ID
     * @return 角色关联
     */
    @Select("SELECT * FROM masking_rule_role WHERE rule_id = #{ruleId} AND role_id = #{roleId}")
    MaskingRuleRole findByRuleIdAndRoleId(@Param("ruleId") Long ruleId, @Param("roleId") Long roleId);
    
    /**
     * 删除规则的所有角色关联
     * 
     * @param ruleId 规则ID
     * @return 删除的行数
     */
    @Delete("DELETE FROM masking_rule_role WHERE rule_id = #{ruleId}")
    int deleteByRuleId(@Param("ruleId") Long ruleId);
    
    /**
     * 删除角色的所有规则关联
     * 
     * @param roleId 角色ID
     * @return 删除的行数
     */
    @Delete("DELETE FROM masking_rule_role WHERE role_id = #{roleId}")
    int deleteByRoleId(@Param("roleId") Long roleId);
    
    /**
     * 查询角色关联的启用规则ID列表
     * 
     * @param roleId 角色ID
     * @return 规则ID列表
     */
    @Select("SELECT mrr.rule_id FROM masking_rule_role mrr " +
            "INNER JOIN masking_rule mr ON mrr.rule_id = mr.id " +
            "WHERE mrr.role_id = #{roleId} AND mrr.enabled = 1 AND mr.enabled = 1")
    List<Long> findEnabledRuleIdsByRoleId(@Param("roleId") Long roleId);
    
    /**
     * 查询规则关联的角色ID列表
     * 
     * @param ruleId 规则ID
     * @return 角色ID列表
     */
    @Select("SELECT role_id FROM masking_rule_role WHERE rule_id = #{ruleId} AND enabled = 1")
    List<Long> findEnabledRoleIdsByRuleId(@Param("ruleId") Long ruleId);
}
