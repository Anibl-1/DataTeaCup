package com.dataplatform.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dataplatform.data.entity.ReportVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 报表版本Mapper接口
 * 
 * @author dataplatform
 */
@Mapper
public interface ReportVersionMapper extends BaseMapper<ReportVersion> {
    
    /**
     * 根据报表ID查询版本历史列表（按版本号降序）
     * 
     * @param reportId 报表ID
     * @return 版本历史列表
     */
    List<ReportVersion> selectByReportId(@Param("reportId") Long reportId);
    
    /**
     * 获取报表的最新版本号
     * 
     * @param reportId 报表ID
     * @return 最新版本号，如果没有版本则返回null
     */
    Integer selectMaxVersionNo(@Param("reportId") Long reportId);
    
    /**
     * 根据报表ID和版本号查询版本
     * 
     * @param reportId 报表ID
     * @param versionNo 版本号
     * @return 版本信息
     */
    ReportVersion selectByReportIdAndVersionNo(@Param("reportId") Long reportId, @Param("versionNo") Integer versionNo);
}
