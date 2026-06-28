package com.dataplatform.data.service;

import com.dataplatform.data.entity.StyleTemplate;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * 样式模板服务接口
 * 支持样式模板的 CRUD 操作（需求 21.1.3）
 * 支持样式模板的保存和复用（需求 21.3）
 * 
 * @author dataplatform
 */
public interface StyleTemplateService {
    
    // ==================== CRUD 操作 ====================
    
    /**
     * 创建样式模板
     * 
     * @param template 模板实体
     * @return 创建后的模板（包含ID）
     */
    StyleTemplate create(StyleTemplate template);
    
    /**
     * 更新样式模板
     * 
     * @param template 模板实体
     * @return 更新后的模板
     */
    StyleTemplate update(StyleTemplate template);
    
    /**
     * 删除样式模板
     * 
     * @param id 模板ID
     */
    void delete(Long id);
    
    /**
     * 根据ID查询模板
     * 
     * @param id 模板ID
     * @return 模板实体
     */
    StyleTemplate getById(Long id);
    
    /**
     * 查询所有启用的模板
     * 
     * @return 模板列表
     */
    List<StyleTemplate> getAllTemplates();
    
    /**
     * 分页查询模板
     * 
     * @param page 页码（从1开始）
     * @param pageSize 每页大小
     * @return 模板列表
     */
    List<StyleTemplate> getTemplatesByPage(int page, int pageSize);
    
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
    List<StyleTemplate> listByCategory(String category);
    
    /**
     * 查询系统预设模板
     * 
     * @return 系统模板列表
     */
    List<StyleTemplate> listSystemTemplates();
    
    /**
     * 查询用户创建的模板
     * 
     * @param createdBy 创建者ID
     * @return 用户模板列表
     */
    List<StyleTemplate> listByCreatedBy(Long createdBy);
    
    /**
     * 根据名称查询模板
     * 
     * @param name 模板名称
     * @return 模板实体
     */
    StyleTemplate getByName(String name);
    
    /**
     * 根据关键词搜索模板
     * 
     * @param keyword 关键词
     * @return 模板列表
     */
    List<StyleTemplate> searchByKeyword(String keyword);
    
    // ==================== 模板应用 ====================
    
    /**
     * 增加模板使用次数
     * 
     * @param id 模板ID
     */
    void incrementUseCount(Long id);
    
    // ==================== 模板导入导出 ====================
    
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
}
