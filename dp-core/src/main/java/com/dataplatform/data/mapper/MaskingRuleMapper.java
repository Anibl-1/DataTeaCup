package com.dataplatform.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dataplatform.data.entity.MaskingRuleEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 脱敏规则 Mapper 接口
 * 支持字段级脱敏规则配置（需求 5.7）
 * 
 * @author dataplatform
 */
@Mapper
public interface MaskingRuleMapper extends BaseMapper<MaskingRuleEntity> {
    
    /**
     * 根据字段名查询脱敏规则
     * 
     * @param fieldName 字段名
     * @return 脱敏规则列表
     */
    @Select("SELECT * FROM masking_rule WHERE field_name = #{fieldName} AND enabled = 1 ORDER BY priority ASC")
    List<MaskingRuleEntity> findByFieldName(@Param("fieldName") String fieldName);
    
    /**
     * 根据数据源ID查询脱敏规则
     * 
     * @param dataSourceId 数据源ID
     * @return 脱敏规则列表
     */
    @Select("SELECT * FROM masking_rule WHERE (data_source_id = #{dataSourceId} OR data_source_id IS NULL) AND enabled = 1 ORDER BY priority ASC")
    List<MaskingRuleEntity> findByDataSource(@Param("dataSourceId") Long dataSourceId);
    
    /**
     * 根据敏感类型查询脱敏规则
     * 
     * @param sensitiveType 敏感类型
     * @return 脱敏规则列表
     */
    @Select("SELECT * FROM masking_rule WHERE sensitive_type = #{sensitiveType} AND enabled = 1 ORDER BY priority ASC")
    List<MaskingRuleEntity> findBySensitiveType(@Param("sensitiveType") String sensitiveType);
    
    /**
     * 查询所有启用的脱敏规则
     * 
     * @return 脱敏规则列表
     */
    @Select("SELECT * FROM masking_rule WHERE enabled = 1 ORDER BY priority ASC")
    List<MaskingRuleEntity> findAllEnabled();
    
    /**
     * 根据数据源、表名和字段名查询适用的脱敏规则
     * 
     * @param dataSourceId 数据源ID
     * @param tableName 表名
     * @param fieldName 字段名
     * @return 脱敏规则列表
     */
    @Select("<script>" +
            "SELECT * FROM masking_rule WHERE enabled = 1 " +
            "AND (data_source_id = #{dataSourceId} OR data_source_id IS NULL) " +
            "<if test='tableName != null'>" +
            "AND (table_name = #{tableName} OR table_name IS NULL) " +
            "</if>" +
            "AND (field_name = #{fieldName} OR #{fieldName} REGEXP field_pattern) " +
            "ORDER BY priority ASC" +
            "</script>")
    List<MaskingRuleEntity> findApplicableRules(
            @Param("dataSourceId") Long dataSourceId,
            @Param("tableName") String tableName,
            @Param("fieldName") String fieldName);
    
    /**
     * 根据规则名称查询
     * 
     * @param name 规则名称
     * @return 脱敏规则
     */
    @Select("SELECT * FROM masking_rule WHERE name = #{name}")
    MaskingRuleEntity findByName(@Param("name") String name);
}
