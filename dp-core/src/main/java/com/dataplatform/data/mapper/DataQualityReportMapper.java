package com.dataplatform.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dataplatform.data.entity.DataQualityReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 数据质量报告Mapper接口
 * 
 * @author dataplatform
 */
@Mapper
public interface DataQualityReportMapper extends BaseMapper<DataQualityReport> {
    
    /**
     * 根据规则ID查询报告列表（按时间降序）
     * 
     * @param ruleId 规则ID
     * @return 报告列表
     */
    List<DataQualityReport> selectByRuleId(@Param("ruleId") Long ruleId);
    
    /**
     * 根据数据源ID和表名查询最新报告
     * 
     * @param dataSourceId 数据源ID
     * @param tableName 表名
     * @return 最新报告
     */
    DataQualityReport selectLatestByDataSourceAndTable(@Param("dataSourceId") Long dataSourceId, @Param("tableName") String tableName);
    
    /**
     * 根据数据源ID查询报告列表
     * 
     * @param dataSourceId 数据源ID
     * @return 报告列表
     */
    List<DataQualityReport> selectByDataSourceId(@Param("dataSourceId") Long dataSourceId);
}
