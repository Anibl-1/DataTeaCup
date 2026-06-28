package com.dataplatform.data.service;

import com.dataplatform.data.entity.ReportDefinition;
import com.dataplatform.data.entity.ReportTemplate;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * 报表模板服务接口
 * 支持报表模板的 CRUD 操作（需求 11.2）
 * 支持模板参数化配置（需求 11.3）
 * 支持模板应用生成报表定义（需求 11.4）
 * 支持模板导入导出（需求 11.5）
 * 
 * @author dataplatform
 */
public interface ReportTemplateService {
    
    // ==================== CRUD 操作（需求 11.2）====================
    
    /**
     * 创建报表模板
     * 
     * @param template 模板实体
     * @return 创建后的模板（包含ID）
     */
    ReportTemplate createTemplate(ReportTemplate template);
    
    /**
     * 更新报表模板
     * 
     * @param template 模板实体
     * @return 更新后的模板
     */
    ReportTemplate updateTemplate(ReportTemplate template);
    
    /**
     * 删除报表模板
     * 
     * @param id 模板ID
     */
    void deleteTemplate(Long id);
    
    /**
     * 根据ID查询模板
     * 
     * @param id 模板ID
     * @return 模板实体
     */
    ReportTemplate getById(Long id);
    
    /**
     * 查询所有启用的模板
     * 
     * @return 模板列表
     */
    List<ReportTemplate> getAllTemplates();
    
    /**
     * 分页查询模板
     * 
     * @param page 页码（从1开始）
     * @param pageSize 每页大小
     * @return 模板列表
     */
    List<ReportTemplate> getTemplatesByPage(int page, int pageSize);
    
    /**
     * 查询模板总数
     * 
     * @return 模板总数
     */
    long getTemplateCount();
    
    // ==================== 查询方法 ====================
    
    /**
     * 根据分类查询模板
     * 
     * @param category 分类
     * @return 模板列表
     */
    List<ReportTemplate> listByCategory(String category);
    
    /**
     * 查询系统预设模板
     * 
     * @return 系统模板列表
     */
    List<ReportTemplate> listSystemTemplates();
    
    /**
     * 查询用户创建的模板
     * 
     * @param creatorId 创建者ID
     * @return 用户模板列表
     */
    List<ReportTemplate> listByCreator(Long creatorId);
    
    /**
     * 根据名称查询模板
     * 
     * @param name 模板名称
     * @return 模板实体
     */
    ReportTemplate getByName(String name);
    
    /**
     * 根据关键词搜索模板
     * 
     * @param keyword 关键词
     * @return 模板列表
     */
    List<ReportTemplate> searchByKeyword(String keyword);
    
    // ==================== 模板应用（需求 11.4）====================
    
    /**
     * 应用模板生成报表定义
     * 一键生成完整的报表定义，包含 SQL、字段配置和参数配置
     * 
     * @param templateId 模板ID
     * @param params 参数值映射，用于替换模板中的参数占位符
     * @return 生成的报表定义
     */
    ReportDefinition applyTemplate(Long templateId, Map<String, Object> params);
    
    /**
     * 将现有报表保存为模板
     * 
     * @param report 报表定义
     * @param name 模板名称
     * @param category 分类
     * @param description 描述
     * @return 创建的模板ID
     */
    Long saveAsTemplate(ReportDefinition report, String name, String category, String description);
    
    /**
     * 增加模板使用次数
     * 
     * @param id 模板ID
     */
    void incrementUseCount(Long id);
    
    // ==================== 模板导入导出（需求 11.5）====================
    
    /**
     * 导出模板到输出流
     * 
     * @param id 模板ID
     * @param out 输出流
     */
    void exportTemplate(Long id, OutputStream out);
    
    /**
     * 从输入流导入模板
     * 
     * @param in 输入流
     * @return 导入的模板ID
     */
    Long importTemplate(InputStream in);
    
    /**
     * 批量导出模板
     * 
     * @param ids 模板ID列表
     * @param out 输出流
     */
    void exportTemplates(List<Long> ids, OutputStream out);
    
    /**
     * 批量导入模板
     * 
     * @param in 输入流
     * @return 导入的模板ID列表
     */
    List<Long> importTemplates(InputStream in);
    
    // ==================== 模板参数化（需求 11.3）====================
    
    /**
     * 解析模板中的参数占位符
     * 
     * @param templateId 模板ID
     * @return 参数名列表
     */
    List<String> parseTemplateParams(Long templateId);
    
    /**
     * 替换模板中的参数占位符
     * 
     * @param sqlTemplate SQL模板
     * @param params 参数值映射
     * @return 替换后的SQL
     */
    String replaceTemplateParams(String sqlTemplate, Map<String, Object> params);
    
    /**
     * 验证参数值是否满足模板参数配置
     * 
     * @param templateId 模板ID
     * @param params 参数值映射
     * @return 验证结果，如果验证通过返回空列表，否则返回错误信息列表
     */
    List<String> validateParams(Long templateId, Map<String, Object> params);
}
