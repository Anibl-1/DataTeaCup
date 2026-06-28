package com.dataplatform.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dataplatform.data.entity.ParameterUsageHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 参数使用历史Mapper
 * 
 * @validates 需求 13.3 - 基于用户历史使用记录智能推荐参数默认值
 */
@Mapper
public interface ParameterUsageHistoryMapper extends BaseMapper<ParameterUsageHistory> {
    
    /**
     * 查询用户对特定参数的使用历史（按使用次数降序）
     */
    @Select("SELECT * FROM parameter_usage_history " +
            "WHERE user_id = #{userId} AND param_name = #{paramName} " +
            "ORDER BY usage_count DESC, last_used_at DESC " +
            "LIMIT #{limit}")
    List<ParameterUsageHistory> findByUserAndParamOrderByUsage(
            @Param("userId") Long userId,
            @Param("paramName") String paramName,
            @Param("limit") int limit);
    
    /**
     * 查询用户对特定参数的使用历史（按最近使用时间降序）
     */
    @Select("SELECT * FROM parameter_usage_history " +
            "WHERE user_id = #{userId} AND param_name = #{paramName} " +
            "ORDER BY last_used_at DESC " +
            "LIMIT #{limit}")
    List<ParameterUsageHistory> findByUserAndParamOrderByRecent(
            @Param("userId") Long userId,
            @Param("paramName") String paramName,
            @Param("limit") int limit);
    
    /**
     * 查询用户对特定报表参数的使用历史
     */
    @Select("SELECT * FROM parameter_usage_history " +
            "WHERE user_id = #{userId} AND report_id = #{reportId} AND param_name = #{paramName} " +
            "ORDER BY usage_count DESC, last_used_at DESC " +
            "LIMIT #{limit}")
    List<ParameterUsageHistory> findByUserReportAndParam(
            @Param("userId") Long userId,
            @Param("reportId") Long reportId,
            @Param("paramName") String paramName,
            @Param("limit") int limit);
    
    /**
     * 查询用户对特定图表参数的使用历史
     */
    @Select("SELECT * FROM parameter_usage_history " +
            "WHERE user_id = #{userId} AND chart_id = #{chartId} AND param_name = #{paramName} " +
            "ORDER BY usage_count DESC, last_used_at DESC " +
            "LIMIT #{limit}")
    List<ParameterUsageHistory> findByUserChartAndParam(
            @Param("userId") Long userId,
            @Param("chartId") Long chartId,
            @Param("paramName") String paramName,
            @Param("limit") int limit);
    
    /**
     * 查询全局最常用的参数值（跨用户统计）
     */
    @Select("SELECT param_value, SUM(usage_count) as total_usage " +
            "FROM parameter_usage_history " +
            "WHERE param_name = #{paramName} " +
            "GROUP BY param_value " +
            "ORDER BY total_usage DESC " +
            "LIMIT #{limit}")
    List<ParameterUsageHistory> findGlobalMostUsed(
            @Param("paramName") String paramName,
            @Param("limit") int limit);
    
    /**
     * 查找已存在的使用记录
     */
    @Select("SELECT * FROM parameter_usage_history " +
            "WHERE user_id = #{userId} AND param_name = #{paramName} AND param_value = #{paramValue} " +
            "AND (report_id = #{reportId} OR (report_id IS NULL AND #{reportId} IS NULL)) " +
            "AND (chart_id = #{chartId} OR (chart_id IS NULL AND #{chartId} IS NULL)) " +
            "LIMIT 1")
    ParameterUsageHistory findExisting(
            @Param("userId") Long userId,
            @Param("paramName") String paramName,
            @Param("paramValue") String paramValue,
            @Param("reportId") Long reportId,
            @Param("chartId") Long chartId);
}
