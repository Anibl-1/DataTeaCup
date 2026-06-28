package com.dataplatform.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dataplatform.data.entity.FieldStyleConfig;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 字段样式配置 Mapper 接口
 * 支持字段样式配置的 CRUD 操作（需求 21.1.3, 21.1.4）
 * 
 * @author dataplatform
 */
@Mapper
public interface FieldStyleConfigMapper extends BaseMapper<FieldStyleConfig> {
    
    /**
     * 根据报表ID查询所有字段样式配置
     * 
     * @param reportId 报表ID
     * @return 字段样式配置列表
     */
    @Select("SELECT * FROM field_style_config WHERE report_id = #{reportId} ORDER BY sort_order")
    List<FieldStyleConfig> findByReportId(@Param("reportId") Long reportId);
    
    /**
     * 根据报表ID和字段名查询样式配置
     * 
     * @param reportId 报表ID
     * @param fieldName 字段名
     * @return 字段样式配置
     */
    @Select("SELECT * FROM field_style_config WHERE report_id = #{reportId} AND field_name = #{fieldName}")
    FieldStyleConfig findByReportIdAndFieldName(@Param("reportId") Long reportId, @Param("fieldName") String fieldName);
    
    /**
     * 删除报表的所有字段样式配置
     * 
     * @param reportId 报表ID
     * @return 删除的记录数
     */
    @Delete("DELETE FROM field_style_config WHERE report_id = #{reportId}")
    int deleteByReportId(@Param("reportId") Long reportId);
    
    /**
     * 根据创建人ID查询样式配置
     * 
     * @param createdBy 创建人ID
     * @return 字段样式配置列表
     */
    @Select("SELECT * FROM field_style_config WHERE created_by = #{createdBy} ORDER BY create_time DESC")
    List<FieldStyleConfig> findByCreatedBy(@Param("createdBy") Long createdBy);
}
