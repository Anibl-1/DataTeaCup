package com.dataplatform.data.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dataplatform.common.exception.BusinessException;
import com.dataplatform.common.exception.ErrorCode;
import com.dataplatform.data.entity.StyleTemplate;
import com.dataplatform.data.mapper.StyleTemplateMapper;
import com.dataplatform.data.service.StyleTemplateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * 样式模板服务实现类
 * 支持样式模板的 CRUD 操作（需求 21.1.3）
 * 支持样式模板的保存和复用（需求 21.3）
 * 
 * @author dataplatform
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StyleTemplateServiceImpl implements StyleTemplateService {
    
    private final StyleTemplateMapper styleTemplateMapper;
    private final ObjectMapper objectMapper;
    
    // ==================== CRUD 操作 ====================
    
    @Override
    @Transactional
    public StyleTemplate create(StyleTemplate template) {
        log.info("Creating style template: {}", template.getName());
        
        // 检查名称是否已存在
        StyleTemplate existing = styleTemplateMapper.findByName(template.getName());
        if (existing != null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "样式模板名称已存在: " + template.getName());
        }
        
        template.setIsSystem(false);
        template.setUseCount(0);
        template.setStatus(1);
        template.setCreateTime(LocalDateTime.now());
        template.setUpdateTime(LocalDateTime.now());
        styleTemplateMapper.insert(template);
        
        log.info("Created style template with id: {}", template.getId());
        return template;
    }
    
    @Override
    @Transactional
    public StyleTemplate update(StyleTemplate template) {
        log.info("Updating style template: {}", template.getId());
        
        StyleTemplate existing = styleTemplateMapper.selectById(template.getId());
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "样式模板不存在: " + template.getId());
        }
        
        // 系统模板不允许修改
        if (Boolean.TRUE.equals(existing.getIsSystem())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "系统预设模板不允许修改");
        }
        
        // 检查名称是否与其他模板冲突
        StyleTemplate byName = styleTemplateMapper.findByName(template.getName());
        if (byName != null && !byName.getId().equals(template.getId())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "样式模板名称已存在: " + template.getName());
        }
        
        template.setUpdateTime(LocalDateTime.now());
        styleTemplateMapper.updateById(template);
        
        log.info("Updated style template: {}", template.getId());
        return styleTemplateMapper.selectById(template.getId());
    }
    
    @Override
    @Transactional
    public void delete(Long id) {
        log.info("Deleting style template: {}", id);
        
        StyleTemplate existing = styleTemplateMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "样式模板不存在: " + id);
        }
        
        // 系统模板不允许删除
        if (Boolean.TRUE.equals(existing.getIsSystem())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "系统预设模板不允许删除");
        }
        
        styleTemplateMapper.deleteById(id);
        log.info("Deleted style template: {}", id);
    }
    
    @Override
    public StyleTemplate getById(Long id) {
        return styleTemplateMapper.selectById(id);
    }
    
    @Override
    public List<StyleTemplate> getAllTemplates() {
        return styleTemplateMapper.findAllEnabled();
    }
    
    @Override
    public List<StyleTemplate> getTemplatesByPage(int page, int pageSize) {
        Page<StyleTemplate> pageObj = new Page<>(page, pageSize);
        LambdaQueryWrapper<StyleTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StyleTemplate::getStatus, 1)
               .orderByDesc(StyleTemplate::getIsSystem)
               .orderByAsc(StyleTemplate::getCategory)
               .orderByDesc(StyleTemplate::getUseCount);
        return styleTemplateMapper.selectPage(pageObj, wrapper).getRecords();
    }
    
    @Override
    public long getTemplateCount() {
        LambdaQueryWrapper<StyleTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StyleTemplate::getStatus, 1);
        return styleTemplateMapper.selectCount(wrapper);
    }
    
    // ==================== 查询方法 ====================
    
    @Override
    public List<StyleTemplate> listByCategory(String category) {
        return styleTemplateMapper.findByCategory(category);
    }
    
    @Override
    public List<StyleTemplate> listSystemTemplates() {
        return styleTemplateMapper.findSystemTemplates();
    }
    
    @Override
    public List<StyleTemplate> listByCreatedBy(Long createdBy) {
        return styleTemplateMapper.findByCreatedBy(createdBy);
    }
    
    @Override
    public StyleTemplate getByName(String name) {
        return styleTemplateMapper.findByName(name);
    }
    
    @Override
    public List<StyleTemplate> searchByKeyword(String keyword) {
        return styleTemplateMapper.searchByKeyword(keyword);
    }
    
    // ==================== 模板应用 ====================
    
    @Override
    @Transactional
    public void incrementUseCount(Long id) {
        styleTemplateMapper.incrementUseCount(id);
    }
    
    // ==================== 模板导入导出 ====================
    
    @Override
    public void exportTemplate(Long id, OutputStream out) {
        log.info("Exporting style template: {}", id);
        
        StyleTemplate template = styleTemplateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "样式模板不存在: " + id);
        }
        
        try {
            // 创建导出对象，排除ID和系统相关字段
            StyleTemplateExport export = new StyleTemplateExport();
            export.setName(template.getName());
            export.setCategory(template.getCategory());
            export.setDescription(template.getDescription());
            export.setColumnStyles(template.getColumnStyles());
            export.setConditionalRules(template.getConditionalRules());
            export.setTableStyle(template.getTableStyle());
            export.setPreviewImage(template.getPreviewImage());
            
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(export);
            out.write(json.getBytes(StandardCharsets.UTF_8));
            out.flush();
            
            log.info("Exported style template: {}", id);
        } catch (IOException e) {
            log.error("Failed to export style template: {}", id, e);
            throw new BusinessException(ErrorCode.ERROR, "导出样式模板失败: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional
    public Long importTemplate(InputStream in) {
        log.info("Importing style template");
        
        try {
            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            StyleTemplateExport export = objectMapper.readValue(json, StyleTemplateExport.class);
            
            // 检查名称是否已存在，如果存在则添加后缀
            String name = export.getName();
            StyleTemplate existing = styleTemplateMapper.findByName(name);
            if (existing != null) {
                name = name + "_" + System.currentTimeMillis();
            }
            
            StyleTemplate template = StyleTemplate.builder()
                    .name(name)
                    .category(export.getCategory())
                    .description(export.getDescription())
                    .isSystem(false)
                    .columnStyles(export.getColumnStyles())
                    .conditionalRules(export.getConditionalRules())
                    .tableStyle(export.getTableStyle())
                    .previewImage(export.getPreviewImage())
                    .useCount(0)
                    .status(1)
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();
            
            styleTemplateMapper.insert(template);
            
            log.info("Imported style template with id: {}", template.getId());
            return template.getId();
        } catch (IOException e) {
            log.error("Failed to import style template", e);
            throw new BusinessException(ErrorCode.PARAM_ERROR, "导入样式模板失败: " + e.getMessage());
        }
    }
    
    @Override
    public void exportTemplates(List<Long> ids, OutputStream out) {
        log.info("Exporting {} style templates", ids.size());
        
        try (ZipOutputStream zos = new ZipOutputStream(out)) {
            for (Long id : ids) {
                StyleTemplate template = styleTemplateMapper.selectById(id);
                if (template == null) {
                    log.warn("Style template not found: {}", id);
                    continue;
                }
                
                StyleTemplateExport export = new StyleTemplateExport();
                export.setName(template.getName());
                export.setCategory(template.getCategory());
                export.setDescription(template.getDescription());
                export.setColumnStyles(template.getColumnStyles());
                export.setConditionalRules(template.getConditionalRules());
                export.setTableStyle(template.getTableStyle());
                export.setPreviewImage(template.getPreviewImage());
                
                String filename = template.getName().replaceAll("[^a-zA-Z0-9_\\-\\u4e00-\\u9fa5]", "_") + ".json";
                ZipEntry entry = new ZipEntry(filename);
                zos.putNextEntry(entry);
                
                String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(export);
                zos.write(json.getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
            
            log.info("Exported {} style templates", ids.size());
        } catch (IOException e) {
            log.error("Failed to export style templates", e);
            throw new BusinessException(ErrorCode.ERROR, "批量导出样式模板失败: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional
    public List<Long> importTemplates(InputStream in) {
        log.info("Importing style templates from zip");
        
        List<Long> importedIds = new ArrayList<>();
        
        try (ZipInputStream zis = new ZipInputStream(in)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory() || !entry.getName().endsWith(".json")) {
                    continue;
                }
                
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    baos.write(buffer, 0, len);
                }
                
                String json = baos.toString(StandardCharsets.UTF_8);
                StyleTemplateExport export = objectMapper.readValue(json, StyleTemplateExport.class);
                
                // 检查名称是否已存在
                String name = export.getName();
                StyleTemplate existing = styleTemplateMapper.findByName(name);
                if (existing != null) {
                    name = name + "_" + System.currentTimeMillis();
                }
                
                StyleTemplate template = StyleTemplate.builder()
                        .name(name)
                        .category(export.getCategory())
                        .description(export.getDescription())
                        .isSystem(false)
                        .columnStyles(export.getColumnStyles())
                        .conditionalRules(export.getConditionalRules())
                        .tableStyle(export.getTableStyle())
                        .previewImage(export.getPreviewImage())
                        .useCount(0)
                        .status(1)
                        .createTime(LocalDateTime.now())
                        .updateTime(LocalDateTime.now())
                        .build();
                
                styleTemplateMapper.insert(template);
                importedIds.add(template.getId());
                
                zis.closeEntry();
            }
            
            log.info("Imported {} style templates", importedIds.size());
            return importedIds;
        } catch (IOException e) {
            log.error("Failed to import style templates", e);
            throw new BusinessException(ErrorCode.PARAM_ERROR, "批量导入样式模板失败: " + e.getMessage());
        }
    }
    
    /**
     * 样式模板导出DTO
     */
    @lombok.Data
    private static class StyleTemplateExport {
        private String name;
        private String category;
        private String description;
        private String columnStyles;
        private String conditionalRules;
        private String tableStyle;
        private String previewImage;
    }
}
