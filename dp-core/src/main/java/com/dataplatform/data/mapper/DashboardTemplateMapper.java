package com.dataplatform.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dataplatform.data.entity.DashboardTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 仪表盘模板Mapper接口
 * 
 * @author dataplatform
 */
@Mapper
public interface DashboardTemplateMapper extends BaseMapper<DashboardTemplate> {
    
    /**
     * 根据分类查询模板列表
     * 
     * @param category 分类
     * @return 模板列表
     */
    List<DashboardTemplate> selectByCategory(@Param("category") String category);
}
