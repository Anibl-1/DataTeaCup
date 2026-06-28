package com.dataplatform.data.mapper;

import com.dataplatform.data.entity.ReportField;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 报表字段Mapper接口
 * 
 * @author dataplatform
 */
public interface ReportFieldMapper {
    /**
     * 根据报表ID查询字段列表
     * 
     * @param reportId 报表ID
     * @return 字段列表
     */
    List<ReportField> selectByReportId(Long reportId);
    
    /**
     * 根据ID查询字段
     * 
     * @param id 字段ID
     * @return 字段信息
     */
    ReportField selectById(Long id);
    
    /**
     * 插入字段
     * 
     * @param field 字段信息
     * @return 影响行数
     */
    int insert(ReportField field);
    
    /**
     * 批量插入字段
     * 
     * @param fields 字段列表
     * @return 影响行数
     */
    int batchInsert(@Param("fields") List<ReportField> fields);
    
    /**
     * 更新字段
     * 
     * @param field 字段信息
     * @return 影响行数
     */
    int update(ReportField field);
    
    /**
     * 删除报表的所有字段
     * 
     * @param reportId 报表ID
     * @return 影响行数
     */
    int deleteByReportId(Long reportId);
    
    /**
     * 根据ID删除字段
     * 
     * @param id 字段ID
     * @return 影响行数
     */
    int delete(Long id);
}

