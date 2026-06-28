package com.dataplatform.data.mapper;

import com.dataplatform.data.entity.PageDefinition;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 页面定义Mapper接口
 * 
 * @author dataplatform
 */
public interface PageDefinitionMapper {
    /**
     * 根据ID查询页面定义
     */
    PageDefinition selectById(Long id);
    
    /**
     * 根据编码查询页面定义
     */
    PageDefinition selectByCode(String pageCode);
    
    /**
     * 分页查询页面定义列表（按布局模式过滤）
     */
    List<PageDefinition> selectList(@Param("offset") Integer offset, 
                                    @Param("pageSize") Integer pageSize,
                                    @Param("keyword") String keyword,
                                    @Param("layoutMode") String layoutMode);
    
    /**
     * 查询页面定义总数（按布局模式过滤）
     */
    long count(@Param("keyword") String keyword,
               @Param("layoutMode") String layoutMode);
    
    /**
     * 按布局模式统计页面数量（用于Tab计数）
     */
    long countByLayoutMode(@Param("layoutMode") String layoutMode);

    /**
     * 查询所有页面定义数量
     */
    long countAll();
    
    /**
     * 插入页面定义
     */
    int insert(PageDefinition page);
    
    /**
     * 更新页面定义
     */
    int update(PageDefinition page);
    
    /**
     * 删除页面定义
     */
    int delete(Long id);
}

