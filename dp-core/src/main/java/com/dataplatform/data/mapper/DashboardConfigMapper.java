package com.dataplatform.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dataplatform.data.entity.DashboardConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 仪表盘配置Mapper接口
 * 
 * @author dataplatform
 */
@Mapper
public interface DashboardConfigMapper extends BaseMapper<DashboardConfig> {
    
    /**
     * 根据创建人查询仪表盘列表
     * 
     * @param createBy 创建人ID
     * @param keyword 搜索关键词（可选）
     * @return 仪表盘列表
     */
    List<DashboardConfig> selectByCreateBy(@Param("createBy") Long createBy, @Param("keyword") String keyword);
    
    /**
     * 根据模板ID查询仪表盘列表
     * 
     * @param templateId 模板ID
     * @return 仪表盘列表
     */
    List<DashboardConfig> selectByTemplateId(@Param("templateId") Long templateId);
}
