package com.dataplatform.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dataplatform.data.entity.DashboardShare;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 仪表盘分享Mapper接口
 * 
 * @author dataplatform
 */
@Mapper
public interface DashboardShareMapper extends BaseMapper<DashboardShare> {
    
    /**
     * 根据分享token查询分享记录
     * 
     * @param shareToken 分享token
     * @return 分享记录
     */
    DashboardShare selectByShareToken(@Param("shareToken") String shareToken);
    
    /**
     * 根据仪表盘ID查询有效的分享记录
     * 
     * @param dashboardId 仪表盘ID
     * @return 分享记录
     */
    DashboardShare selectActiveByDashboardId(@Param("dashboardId") Long dashboardId);
    
    /**
     * 撤销仪表盘的所有分享
     * 
     * @param dashboardId 仪表盘ID
     * @return 影响行数
     */
    int revokeByDashboardId(@Param("dashboardId") Long dashboardId);
}
