package com.dataplatform.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dataplatform.data.entity.DataQualityRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 数据质量规则Mapper接口
 * 
 * @author dataplatform
 */
@Mapper
public interface DataQualityRuleMapper extends BaseMapper<DataQualityRule> {
    
    /**
     * 根据数据源ID查询规则列表
     * 
     * @param dataSourceId 数据源ID
     * @return 规则列表
     */
    List<DataQualityRule> selectByDataSourceId(@Param("dataSourceId") Long dataSourceId);
    
    /**
     * 根据数据源ID和表名查询规则
     * 
     * @param dataSourceId 数据源ID
     * @param tableName 表名
     * @return 规则
     */
    DataQualityRule selectByDataSourceAndTable(@Param("dataSourceId") Long dataSourceId, @Param("tableName") String tableName);
    
    /**
     * 查询所有启用状态的规则
     * 
     * @return 启用的规则列表
     */
    List<DataQualityRule> selectActiveRules();
}
