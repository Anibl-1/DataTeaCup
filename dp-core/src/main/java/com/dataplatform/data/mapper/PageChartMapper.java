package com.dataplatform.data.mapper;

import com.dataplatform.data.entity.PageChart;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 页面图表关联Mapper接口
 * 
 * @author dataplatform
 */
public interface PageChartMapper {
    /**
     * 根据页面ID查询图表列表
     */
    List<PageChart> selectByPageId(Long pageId);
    
    /**
     * 插入页面图表关联
     */
    int insert(PageChart pageChart);
    
    /**
     * 更新页面图表关联
     */
    int update(PageChart pageChart);
    
    /**
     * 删除页面图表关联
     */
    int delete(Long id);
    
    /**
     * 根据页面ID删除所有图表关联
     */
    int deleteByPageId(Long pageId);
    
    /**
     * 批量插入页面图表关联
     */
    int batchInsert(@Param("list") List<PageChart> list);
}

