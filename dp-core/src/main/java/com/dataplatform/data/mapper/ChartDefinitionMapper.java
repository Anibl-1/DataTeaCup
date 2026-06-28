package com.dataplatform.data.mapper;

import com.dataplatform.data.entity.ChartDefinition;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 图表定义Mapper接口
 * 
 * @author dataplatform
 */
public interface ChartDefinitionMapper {
    /**
     * 根据ID查询图表定义
     * 
     * @param id 图表ID
     * @return 图表定义信息
     */
    ChartDefinition selectById(Long id);
    
    /**
     * 根据编码查询图表定义
     * 
     * @param chartCode 图表编码
     * @return 图表定义信息
     */
    ChartDefinition selectByCode(String chartCode);
    
    /**
     * 分页查询图表定义列表
     * 
     * @param offset 偏移量
     * @param pageSize 每页大小
     * @param keyword 搜索关键词（可选）
     * @param chartType 图表类型（可选）
     * @param status 状态（可选）
     * @return 图表定义列表
     */
    List<ChartDefinition> selectList(@Param("offset") Integer offset, 
                                     @Param("pageSize") Integer pageSize,
                                     @Param("keyword") String keyword,
                                     @Param("chartType") String chartType,
                                     @Param("status") Integer status);
    
    /**
     * 查询图表定义总数
     * 
     * @param keyword 搜索关键词（可选）
     * @param chartType 图表类型（可选）
     * @param status 状态（可选）
     * @return 图表定义总数
     */
    long count(@Param("keyword") String keyword,
               @Param("chartType") String chartType,
               @Param("status") Integer status);
    
    /**
     * 插入图表定义
     * 
     * @param chart 图表定义信息
     * @return 影响行数
     */
    int insert(ChartDefinition chart);
    
    /**
     * 更新图表定义
     * 
     * @param chart 图表定义信息
     * @return 影响行数
     */
    int update(ChartDefinition chart);
    
    /**
     * 删除图表定义
     * 
     * @param id 图表ID
     * @return 影响行数
     */
    int delete(Long id);
}

