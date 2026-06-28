package com.dataplatform.data.mapper;

import com.dataplatform.data.entity.DataSource;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 数据源Mapper接口
 * 
 * @author dataplatform
 */
public interface DataSourceMapper {
    /**
     * 根据ID查询数据源
     * 
     * @param id 数据源ID
     * @return 数据源信息
     */
    DataSource selectById(Long id);
    
    /**
     * 分页查询数据源列表
     * 
     * @param offset 偏移量
     * @param pageSize 每页大小
     * @param filters 筛选条件列表
     * @return 数据源列表
     */
    List<DataSource> selectList(@Param("offset") Integer offset, @Param("pageSize") Integer pageSize, 
                                @Param("filters") java.util.List<com.dataplatform.common.dto.FilterCondition> filters);
    
    /**
     * 查询数据源总数
     * 
     * @param filters 筛选条件列表
     * @return 数据源总数
     */
    long count(@Param("filters") java.util.List<com.dataplatform.common.dto.FilterCondition> filters);
    
    /**
     * 插入数据源
     * 
     * @param dataSource 数据源信息
     * @return 影响行数
     */
    int insert(DataSource dataSource);
    
    /**
     * 更新数据源
     * 
     * @param dataSource 数据源信息
     * @return 影响行数
     */
    int update(DataSource dataSource);
    
    /**
     * 删除数据源
     * 
     * @param id 数据源ID
     * @return 影响行数
     */
    int delete(Long id);

    /**
     * 按数据库类型统计数据源数量
     * 
     * @return 各类型数据源数量列表
     */
    java.util.List<java.util.Map<String, Object>> countByDbType();

    /**
     * 查询所有分组名称
     */
    java.util.List<String> selectGroups();
}

