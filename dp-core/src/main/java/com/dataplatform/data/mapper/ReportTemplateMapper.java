package com.dataplatform.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dataplatform.data.entity.ReportTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 报表模板 Mapper 接口
 * 支持报表模板的 CRUD 操作（需求 11.1, 11.2）
 * 
 * @author dataplatform
 */
@Mapper
public interface ReportTemplateMapper extends BaseMapper<ReportTemplate> {
    
    /**
     * 根据分类查询模板列表
     * 
     * @param category 分类
     * @return 模板列表
     */
    @Select("SELECT * FROM report_template WHERE category = #{category} AND status = 1 ORDER BY is_system DESC, use_count DESC")
    List<ReportTemplate> findByCategory(@Param("category") String category);
    
    /**
     * 查询所有系统预设模板
     * 
     * @return 系统模板列表
     */
    @Select("SELECT * FROM report_template WHERE is_system = 1 AND status = 1 ORDER BY category, name")
    List<ReportTemplate> findSystemTemplates();
    
    /**
     * 查询用户创建的模板
     * 
     * @param creatorId 创建者ID
     * @return 用户模板列表
     */
    @Select("SELECT * FROM report_template WHERE creator_id = #{creatorId} AND status = 1 ORDER BY update_time DESC")
    List<ReportTemplate> findByCreator(@Param("creatorId") Long creatorId);
    
    /**
     * 根据名称查询模板
     * 
     * @param name 模板名称
     * @return 模板
     */
    @Select("SELECT * FROM report_template WHERE name = #{name} AND status = 1")
    ReportTemplate findByName(@Param("name") String name);
    
    /**
     * 增加使用次数
     * 
     * @param id 模板ID
     * @return 影响行数
     */
    @Update("UPDATE report_template SET use_count = use_count + 1 WHERE id = #{id}")
    int incrementUseCount(@Param("id") Long id);
    
    /**
     * 查询所有启用的模板
     * 
     * @return 模板列表
     */
    @Select("SELECT * FROM report_template WHERE status = 1 ORDER BY is_system DESC, category, use_count DESC")
    List<ReportTemplate> findAllEnabled();
    
    /**
     * 根据关键词搜索模板
     * 
     * @param keyword 关键词
     * @return 模板列表
     */
    @Select("SELECT * FROM report_template WHERE status = 1 AND (name LIKE CONCAT('%', #{keyword}, '%') OR description LIKE CONCAT('%', #{keyword}, '%')) ORDER BY is_system DESC, use_count DESC")
    List<ReportTemplate> searchByKeyword(@Param("keyword") String keyword);
}
