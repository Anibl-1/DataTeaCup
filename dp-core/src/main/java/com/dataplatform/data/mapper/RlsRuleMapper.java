package com.dataplatform.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dataplatform.data.entity.RlsRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 行级权限规则Mapper接口
 * 
 * @author dataplatform
 */
@Mapper
public interface RlsRuleMapper extends BaseMapper<RlsRule> {
    
    /**
     * 根据角色ID查询RLS规则列表
     * 
     * @param roleId 角色ID
     * @return RLS规则列表
     */
    List<RlsRule> selectByRoleId(@Param("roleId") Long roleId);
    
    /**
     * 根据角色ID和数据源ID查询RLS规则列表
     * 
     * @param roleId 角色ID
     * @param dataSourceId 数据源ID
     * @return RLS规则列表
     */
    List<RlsRule> selectByRoleAndDataSource(@Param("roleId") Long roleId, @Param("dataSourceId") Long dataSourceId);
    
    /**
     * 根据数据源ID和表名查询RLS规则列表
     * 
     * @param dataSourceId 数据源ID
     * @param tableName 表名
     * @return RLS规则列表
     */
    List<RlsRule> selectByDataSourceAndTable(@Param("dataSourceId") Long dataSourceId, @Param("tableName") String tableName);
}
