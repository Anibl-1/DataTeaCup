package com.dataplatform.data.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dataplatform.common.exception.BusinessException;
import com.dataplatform.common.exception.ErrorCode;
import com.dataplatform.data.dto.TemplateParameter;
import com.dataplatform.data.entity.ReportDefinition;
import com.dataplatform.data.entity.ReportTemplate;
import com.dataplatform.data.mapper.ReportTemplateMapper;
import com.dataplatform.data.service.ReportTemplateService;
import com.dataplatform.data.service.template.TemplateParameterService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * 报表模板服务实现
 * 支持报表模板的 CRUD 操作（需求 11.2）
 * 支持模板参数化配置（需求 11.3）
 * 支持模板应用生成报表定义（需求 11.4）
 * 支持模板导入导出（需求 11.5）
 * 
 * @author dataplatform
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportTemplateServiceImpl implements ReportTemplateService {
    
    private final ReportTemplateMapper reportTemplateMapper;
    private final ObjectMapper objectMapper;
    private final TemplateParameterService templateParameterService;
    
    /** 参数占位符正则表达式：${paramName} */
    private static final Pattern PARAM_PATTERN = Pattern.compile("\\$\\{([a-zA-Z_][a-zA-Z0-9_]*)\\}");
    
    /** 模板导出文件扩展名 */
    private static final String TEMPLATE_FILE_EXTENSION = ".json";
    
    // ==================== CRUD 操作（需求 11.2）====================
    
    @Override
    @Transactional
    public ReportTemplate createTemplate(ReportTemplate template) {
        validateTemplate(template);
        
        // 检查模板名称是否重复
        if (StringUtils.hasText(template.getName())) {
            ReportTemplate existing = reportTemplateMapper.findByName(template.getName());
            if (existing != null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "模板名称已存在");
            }
        }
        
        template.setCreateTime(LocalDateTime.now());
        template.setUpdateTime(LocalDateTime.now());
        if (template.getStatus() == null) {
            template.setStatus(1);
        }
        if (template.getUseCount() == null) {
            template.setUseCount(0);
        }
        if (template.getIsSystem() == null) {
            template.setIsSystem(false);
        }
        
        reportTemplateMapper.insert(template);
        log.info("Created report template: id={}, name={}", template.getId(), template.getName());
        return template;
    }
    
    @Override
    @Transactional
    public ReportTemplate updateTemplate(ReportTemplate template) {
        if (template.getId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "模板ID不能为空");
        }
        
        ReportTemplate existing = reportTemplateMapper.selectById(template.getId());
        if (existing == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "报表模板不存在");
        }
        
        // 系统预设模板不允许修改
        if (Boolean.TRUE.equals(existing.getIsSystem())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "系统预设模板不允许修改");
        }
        
        validateTemplate(template);
        
        // 检查模板名称是否重复（排除自身）
        if (StringUtils.hasText(template.getName())) {
            ReportTemplate byName = reportTemplateMapper.findByName(template.getName());
            if (byName != null && !byName.getId().equals(template.getId())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "模板名称已存在");
            }
        }
        
        template.setUpdateTime(LocalDateTime.now());
        // 保留原有的系统标识和创建者
        template.setIsSystem(existing.getIsSystem());
        template.setCreatorId(existing.getCreatorId());
        template.setCreateTime(existing.getCreateTime());
        
        reportTemplateMapper.updateById(template);
        log.info("Updated report template: id={}", template.getId());
        return reportTemplateMapper.selectById(template.getId());
    }
    
    @Override
    @Transactional
    public void deleteTemplate(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "模板ID不能为空");
        }
        
        ReportTemplate existing = reportTemplateMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "报表模板不存在");
        }
        
        // 系统预设模板不允许删除
        if (Boolean.TRUE.equals(existing.getIsSystem())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "系统预设模板不允许删除");
        }
        
        reportTemplateMapper.deleteById(id);
        log.info("Deleted report template: id={}", id);
    }
    
    @Override
    public ReportTemplate getById(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "模板ID不能为空");
        }
        
        ReportTemplate template = reportTemplateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "报表模板不存在");
        }
        return template;
    }
    
    @Override
    public List<ReportTemplate> getAllTemplates() {
        return reportTemplateMapper.findAllEnabled();
    }
    
    @Override
    public List<ReportTemplate> getTemplatesByPage(int page, int pageSize) {
        if (page < 1) page = 1;
        if (pageSize < 1) pageSize = 10;
        
        Page<ReportTemplate> pageObj = new Page<>(page, pageSize);
        Page<ReportTemplate> result = reportTemplateMapper.selectPage(pageObj,
                new LambdaQueryWrapper<ReportTemplate>()
                        .eq(ReportTemplate::getStatus, 1)
                        .orderByDesc(ReportTemplate::getIsSystem)
                        .orderByDesc(ReportTemplate::getUseCount)
                        .orderByDesc(ReportTemplate::getUpdateTime)
        );
        return result.getRecords();
    }
    
    @Override
    public long getTemplateCount() {
        return reportTemplateMapper.selectCount(
                new LambdaQueryWrapper<ReportTemplate>()
                        .eq(ReportTemplate::getStatus, 1)
        );
    }
    
    // ==================== 查询方法 ====================
    
    @Override
    public List<ReportTemplate> listByCategory(String category) {
        if (!StringUtils.hasText(category)) {
            return getAllTemplates();
        }
        return reportTemplateMapper.findByCategory(category);
    }
    
    @Override
    public List<ReportTemplate> listSystemTemplates() {
        return reportTemplateMapper.findSystemTemplates();
    }
    
    @Override
    public List<ReportTemplate> listByCreator(Long creatorId) {
        if (creatorId == null) {
            return Collections.emptyList();
        }
        return reportTemplateMapper.findByCreator(creatorId);
    }
    
    @Override
    public ReportTemplate getByName(String name) {
        if (!StringUtils.hasText(name)) {
            return null;
        }
        return reportTemplateMapper.findByName(name);
    }
    
    @Override
    public List<ReportTemplate> searchByKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return getAllTemplates();
        }
        return reportTemplateMapper.searchByKeyword(keyword);
    }
    
    // ==================== 模板应用（需求 11.4）====================
    
    @Override
    @Transactional
    public ReportDefinition applyTemplate(Long templateId, Map<String, Object> params) {
        ReportTemplate template = getById(templateId);
        
        // 解析参数配置
        List<TemplateParameter> parameterConfigs = templateParameterService
                .parseParameterConfig(template.getParamsConfig());
        
        // 应用默认值
        Map<String, Object> effectiveParams = templateParameterService
                .applyDefaultValues(params, parameterConfigs);
        
        // 验证参数
        List<String> errors = templateParameterService
                .validateParameters(effectiveParams, parameterConfigs);
        if (!errors.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "参数验证失败: " + String.join(", ", errors));
        }
        
        // 替换SQL模板中的参数（使用增强的参数替换）
        String sql = templateParameterService
                .replaceParameters(template.getSqlTemplate(), effectiveParams, parameterConfigs);
        
        // 创建报表定义
        ReportDefinition report = new ReportDefinition();
        report.setReportName(template.getName() + "_" + System.currentTimeMillis());
        report.setSqlContent(sql);
        report.setParams(template.getParamsConfig());
        report.setConfigJson(template.getFieldsConfig());
        report.setDescription(template.getDescription());
        report.setStatus(1);
        report.setReportType("sql");
        report.setAllowExportExcel(1);
        report.setAllowExportPdf(1);
        report.setCreateTime(new Date());
        report.setUpdateTime(new Date());
        
        // 增加模板使用次数
        incrementUseCount(templateId);
        
        log.info("Applied template: templateId={}, reportName={}", templateId, report.getReportName());
        return report;
    }
    
    @Override
    @Transactional
    public Long saveAsTemplate(ReportDefinition report, String name, String category, String description) {
        if (report == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "报表定义不能为空");
        }
        
        ReportTemplate template = new ReportTemplate();
        template.setName(name);
        template.setCategory(StringUtils.hasText(category) ? category : "custom");
        template.setDescription(description);
        template.setSqlTemplate(report.getSqlContent());
        template.setFieldsConfig(report.getConfigJson());
        template.setParamsConfig(report.getParams());
        template.setIsSystem(false);
        
        ReportTemplate created = createTemplate(template);
        log.info("Saved report as template: reportId={}, templateId={}", report.getId(), created.getId());
        return created.getId();
    }
    
    @Override
    @Transactional
    public void incrementUseCount(Long id) {
        if (id != null) {
            reportTemplateMapper.incrementUseCount(id);
        }
    }
    
    // ==================== 模板导入导出（需求 11.5）====================
    
    @Override
    public void exportTemplate(Long id, OutputStream out) {
        ReportTemplate template = getById(id);
        
        try {
            // 创建导出数据对象
            Map<String, Object> exportData = createExportData(template);
            
            // 写入JSON
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(out, exportData);
            log.info("Exported template: id={}, name={}", id, template.getName());
        } catch (IOException e) {
            log.error("Failed to export template: id={}", id, e);
            throw new BusinessException(ErrorCode.ERROR, "导出模板失败: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional
    public Long importTemplate(InputStream in) {
        try {
            // 读取JSON
            Map<String, Object> importData = objectMapper.readValue(in, 
                    new TypeReference<Map<String, Object>>() {});
            
            // 创建模板
            ReportTemplate template = createTemplateFromImportData(importData);
            
            // 检查名称是否重复，如果重复则添加后缀
            String originalName = template.getName();
            int suffix = 1;
            while (reportTemplateMapper.findByName(template.getName()) != null) {
                template.setName(originalName + "_" + suffix++);
            }
            
            // 导入的模板不是系统模板
            template.setIsSystem(false);
            template.setId(null);
            
            ReportTemplate created = createTemplate(template);
            log.info("Imported template: id={}, name={}", created.getId(), created.getName());
            return created.getId();
        } catch (IOException e) {
            log.error("Failed to import template", e);
            throw new BusinessException(ErrorCode.PARAM_ERROR, "导入模板失败: " + e.getMessage());
        }
    }
    
    @Override
    public void exportTemplates(List<Long> ids, OutputStream out) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "模板ID列表不能为空");
        }
        
        try (ZipOutputStream zos = new ZipOutputStream(out)) {
            for (Long id : ids) {
                ReportTemplate template = reportTemplateMapper.selectById(id);
                if (template == null) {
                    continue;
                }
                
                Map<String, Object> exportData = createExportData(template);
                String fileName = template.getName().replaceAll("[^a-zA-Z0-9_\\-]", "_") + TEMPLATE_FILE_EXTENSION;
                
                ZipEntry entry = new ZipEntry(fileName);
                zos.putNextEntry(entry);
                zos.write(objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsBytes(exportData));
                zos.closeEntry();
            }
            log.info("Exported {} templates", ids.size());
        } catch (IOException e) {
            log.error("Failed to export templates", e);
            throw new BusinessException(ErrorCode.ERROR, "批量导出模板失败: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional
    public List<Long> importTemplates(InputStream in) {
        List<Long> importedIds = new ArrayList<>();
        
        try (ZipInputStream zis = new ZipInputStream(in)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory() || !entry.getName().endsWith(TEMPLATE_FILE_EXTENSION)) {
                    continue;
                }
                
                // 读取JSON内容
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    baos.write(buffer, 0, len);
                }
                
                try {
                    Map<String, Object> importData = objectMapper.readValue(
                            baos.toByteArray(), new TypeReference<Map<String, Object>>() {});
                    
                    ReportTemplate template = createTemplateFromImportData(importData);
                    
                    // 检查名称是否重复
                    String originalName = template.getName();
                    int suffix = 1;
                    while (reportTemplateMapper.findByName(template.getName()) != null) {
                        template.setName(originalName + "_" + suffix++);
                    }
                    
                    template.setIsSystem(false);
                    template.setId(null);
                    
                    ReportTemplate created = createTemplate(template);
                    importedIds.add(created.getId());
                } catch (Exception e) {
                    log.warn("Failed to import template from entry: {}", entry.getName(), e);
                }
                
                zis.closeEntry();
            }
            log.info("Imported {} templates", importedIds.size());
        } catch (IOException e) {
            log.error("Failed to import templates", e);
            throw new BusinessException(ErrorCode.PARAM_ERROR, "批量导入模板失败: " + e.getMessage());
        }
        
        return importedIds;
    }
    
    // ==================== 模板参数化（需求 11.3）====================
    
    @Override
    public List<String> parseTemplateParams(Long templateId) {
        ReportTemplate template = getById(templateId);
        return templateParameterService.parseParameterPlaceholders(template.getSqlTemplate());
    }
    
    /**
     * 从SQL模板中解析参数名
     */
    private List<String> parseParamsFromSql(String sqlTemplate) {
        return templateParameterService.parseParameterPlaceholders(sqlTemplate);
    }
    
    @Override
    public String replaceTemplateParams(String sqlTemplate, Map<String, Object> params) {
        return templateParameterService.replaceParameters(sqlTemplate, params);
    }
    
    /**
     * 格式化参数值为SQL安全的字符串
     * @deprecated Use TemplateParameterService.formatParameterValue instead
     */
    @Deprecated
    private String formatParamValue(Object value) {
        return templateParameterService.formatParameterValue(value, null);
    }
    
    @Override
    public List<String> validateParams(Long templateId, Map<String, Object> params) {
        ReportTemplate template = getById(templateId);
        
        // 解析参数配置
        List<TemplateParameter> parameterConfigs = templateParameterService
                .parseParameterConfig(template.getParamsConfig());
        
        // 如果没有参数配置，使用SQL中的参数进行基本验证
        if (parameterConfigs.isEmpty()) {
            List<String> sqlParams = parseParamsFromSql(template.getSqlTemplate());
            parameterConfigs = sqlParams.stream()
                    .map(templateParameterService::createDefaultParameterConfig)
                    .collect(Collectors.toList());
        }
        
        return templateParameterService.validateParameters(params, parameterConfigs);
    }
    
    /**
     * 验证参数类型
     * @deprecated Use TemplateParameterService.validateParameter instead
     */
    @Deprecated
    private List<String> validateParamType(String paramName, Object value, String type) {
        TemplateParameter config = new TemplateParameter();
        config.setName(paramName);
        config.setType(type);
        return templateParameterService.validateParameter(paramName, value, config);
    }
    
    /**
     * 解析参数配置JSON
     * @deprecated Use TemplateParameterService.parseParameterConfig instead
     */
    @Deprecated
    private Map<String, Map<String, Object>> parseParamsConfig(String paramsConfigJson) {
        if (!StringUtils.hasText(paramsConfigJson)) {
            return Collections.emptyMap();
        }
        
        try {
            List<Map<String, Object>> paramsList = objectMapper.readValue(paramsConfigJson,
                    new TypeReference<List<Map<String, Object>>>() {});
            
            Map<String, Map<String, Object>> result = new HashMap<>();
            for (Map<String, Object> param : paramsList) {
                String name = (String) param.get("name");
                if (StringUtils.hasText(name)) {
                    result.put(name, param);
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to parse params config: {}", paramsConfigJson, e);
            return Collections.emptyMap();
        }
    }
    
    // ==================== 私有方法 ====================
    
    private void validateTemplate(ReportTemplate template) {
        if (template == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "模板不能为空");
        }
        
        if (!StringUtils.hasText(template.getName())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "模板名称不能为空");
        }
        
        if (!StringUtils.hasText(template.getCategory())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "模板分类不能为空");
        }
        
        // 验证分类是否有效
        List<String> validCategories = Arrays.asList("sales", "finance", "operation", "inventory", "hr", "custom");
        if (!validCategories.contains(template.getCategory())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, 
                    "无效的模板分类，有效值: " + String.join(", ", validCategories));
        }
    }
    
    /**
     * 创建导出数据对象
     */
    private Map<String, Object> createExportData(ReportTemplate template) {
        Map<String, Object> exportData = new LinkedHashMap<>();
        exportData.put("version", "1.0");
        exportData.put("exportTime", LocalDateTime.now().toString());
        exportData.put("name", template.getName());
        exportData.put("category", template.getCategory());
        exportData.put("description", template.getDescription());
        exportData.put("sqlTemplate", template.getSqlTemplate());
        exportData.put("fieldsConfig", template.getFieldsConfig());
        exportData.put("paramsConfig", template.getParamsConfig());
        exportData.put("previewImage", template.getPreviewImage());
        return exportData;
    }
    
    /**
     * 从导入数据创建模板
     */
    private ReportTemplate createTemplateFromImportData(Map<String, Object> importData) {
        ReportTemplate template = new ReportTemplate();
        template.setName((String) importData.get("name"));
        template.setCategory((String) importData.get("category"));
        template.setDescription((String) importData.get("description"));
        template.setSqlTemplate((String) importData.get("sqlTemplate"));
        template.setFieldsConfig((String) importData.get("fieldsConfig"));
        template.setParamsConfig((String) importData.get("paramsConfig"));
        template.setPreviewImage((String) importData.get("previewImage"));
        
        // 设置默认值
        if (!StringUtils.hasText(template.getCategory())) {
            template.setCategory("custom");
        }
        
        return template;
    }
}
