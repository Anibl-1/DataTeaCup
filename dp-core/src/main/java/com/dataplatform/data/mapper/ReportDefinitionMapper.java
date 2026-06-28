package com.dataplatform.data.mapper;

import com.dataplatform.data.entity.ReportDefinition;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 报表定义Mapper接口
 * 
 * @author dataplatform
 */
public interface ReportDefinitionMapper {
    /**
     * 根据ID查询报表定义
     * 
     * @param id 报表ID
     * @return 报表定义信息
     */
    ReportDefinition selectById(Long id);
    
    /**
     * 根据编码查询报表定义
     * 
     * @param reportCode 报表编码
     * @return 报表定义信息
     */
    ReportDefinition selectByCode(String reportCode);
    
    /**
     * 分页查询报表定义列表
     * 
     * @param offset 偏移量
     * @param pageSize 每页大小
     * @param keyword 搜索关键词（可选）
     * @return 报表定义列表
     */
    List<ReportDefinition> selectList(@Param("offset") Integer offset, 
                                       @Param("pageSize") Integer pageSize,
                                       @Param("keyword") String keyword);
    
    /**
     * 查询报表定义总数
     * 
     * @param keyword 搜索关键词（可选）
     * @return 报表定义总数
     */
    long count(@Param("keyword") String keyword);

    /**
     * 查询所有报表定义数量
     */
    long countAll();
    
    /**
     * 查询所有可用的报表（用于图表设计器选择）
     * 只返回启用状态的报表
     * 
     * @return 可用报表列表
     */
    List<ReportDefinition> selectAvailableReports();
    
    /**
     * 插入报表定义
     * 
     * @param report 报表定义信息
     * @return 影响行数
     */
    int insert(ReportDefinition report);
    
    /**
     * 更新报表定义
     * 
     * @param report 报表定义信息
     * @return 影响行数
     */
    int update(ReportDefinition report);
    
    /**
     * 删除报表定义
     * 
     * @param id 报表ID
     * @return 影响行数
     */
    int delete(Long id);
}

