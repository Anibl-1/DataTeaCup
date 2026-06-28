package com.dataplatform.data.service;

import com.dataplatform.data.entity.FieldStyleConfig;

import java.util.List;

/**
 * 字段样式配置服务接口
 * 支持字段样式配置的 CRUD 操作（需求 21.1.3）
 * 支持从数据库加载样式配置（需求 21.1.4）
 * 
 * @author dataplatform
 */
public interface FieldStyleConfigService {
    
    // ==================== CRUD 操作 ====================
    
    /**
     * 创建字段样式配置
     * 
     * @param config 样式配置实体
     * @return 创建后的配置（包含ID）
     */
    FieldStyleConfig create(FieldStyleConfig config);
    
    /**
     * 更新字段样式配置
     * 
     * @param config 样式配置实体
     * @return 更新后的配置
     */
    FieldStyleConfig update(FieldStyleConfig config);
    
    /**
     * 删除字段样式配置
     * 
     * @param id 配置ID
     */
    void delete(Long id);
    
    /**
     * 根据ID查询配置
     * 
     * @param id 配置ID
     * @return 样式配置实体
     */
    FieldStyleConfig getById(Long id);
    
    // ==================== 查询方法 ====================
    
    /**
     * 根据报表ID查询所有字段样式配置
     * 
     * @param reportId 报表ID
     * @return 字段样式配置列表
     */
    List<FieldStyleConfig> getByReportId(Long reportId);
    
    /**
     * 根据报表ID和字段名查询样式配置
     * 
     * @param reportId 报表ID
     * @param fieldName 字段名
     * @return 字段样式配置
     */
    FieldStyleConfig getByReportIdAndFieldName(Long reportId, String fieldName);
    
    /**
     * 根据创建人ID查询样式配置
     * 
     * @param createdBy 创建人ID
     * @return 字段样式配置列表
     */
    List<FieldStyleConfig> getByCreatedBy(Long createdBy);
    
    // ==================== 批量操作 ====================
    
    /**
     * 批量保存字段样式配置
     * 如果配置已存在则更新，不存在则创建
     * 
     * @param reportId 报表ID
     * @param configs 样式配置列表
     * @return 保存后的配置列表
     */
    List<FieldStyleConfig> batchSave(Long reportId, List<FieldStyleConfig> configs);
    
    /**
     * 删除报表的所有字段样式配置
     * 
     * @param reportId 报表ID
     */
    void deleteByReportId(Long reportId);
    
    // ==================== 样式复制 ====================
    
    /**
     * 复制报表的样式配置到另一个报表
     * 
     * @param sourceReportId 源报表ID
     * @param targetReportId 目标报表ID
     * @return 复制后的配置列表
     */
    List<FieldStyleConfig> copyStyles(Long sourceReportId, Long targetReportId);
}
