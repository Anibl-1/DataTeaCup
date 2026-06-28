package com.dataplatform.analytics.controller;

import com.dataplatform.common.annotation.OperationLog;
import com.dataplatform.common.annotation.RequirePermission;
import com.dataplatform.common.PageResult;
import com.dataplatform.common.Result;
import com.dataplatform.data.entity.FieldStyleConfig;
import com.dataplatform.data.entity.StyleTemplate;
import com.dataplatform.data.service.FieldStyleConfigService;
import com.dataplatform.data.service.StyleTemplateService;
import com.dataplatform.common.security.SecurityContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * 样式配置控制器
 * 支持字段样式配置的 CRUD 操作（需求 21.1.3）
 * 支持从数据库加载样式配置（需求 21.1.4）
 * 支持样式模板的管理（需求 21.3）
 * 
 * @author dataplatform
 */
@Slf4j
@RestController
@RequestMapping("/style-config")
@RequiredArgsConstructor
@RequirePermission("style:read")
public class StyleConfigController {
    
    private final FieldStyleConfigService fieldStyleConfigService;
    private final StyleTemplateService styleTemplateService;
    
    // ==================== 字段样式配置接口 ====================
    
    /**
     * 获取报表的所有字段样式配置
     * 
     * **Validates: Requirements 21.1.4**
     * - THE Style_Engine SHALL 支持样式配置加载（从数据库加载）
     */
    @GetMapping("/field/{reportId}")
    public Result<List<FieldStyleConfig>> getFieldStyleConfigs(@PathVariable Long reportId) {
        List<FieldStyleConfig> configs = fieldStyleConfigService.getByReportId(reportId);
        return Result.success(configs);
    }
    
    /**
     * 获取单个字段的样式配置
     */
    @GetMapping("/field/{reportId}/{fieldName}")
    public Result<FieldStyleConfig> getFieldStyleConfig(
            @PathVariable Long reportId,
            @PathVariable String fieldName) {
        FieldStyleConfig config = fieldStyleConfigService.getByReportIdAndFieldName(reportId, fieldName);
        return Result.success(config);
    }
    
    /**
     * 创建字段样式配置
     * 
     * **Validates: Requirements 21.1.3**
     * - THE Style_Engine SHALL 支持样式配置持久化（保存到数据库）
     */
    @RequirePermission("report:manage")
    @OperationLog(module = "样式配置", type = OperationLog.OperationType.CREATE, description = "创建字段样式配置")
    @PostMapping("/field")
    public Result<FieldStyleConfig> createFieldStyleConfig(
            @RequestBody FieldStyleConfig config) {
        Long userId = SecurityContext.getCurrentUserId();
        config.setCreatedBy(userId);
        FieldStyleConfig created = fieldStyleConfigService.create(config);
        return Result.success(created);
    }
    
    /**
     * 更新字段样式配置
     */
    @RequirePermission("report:manage")
    @OperationLog(module = "样式配置", type = OperationLog.OperationType.UPDATE, description = "更新字段样式配置")
    @PutMapping("/field/{id}")
    public Result<FieldStyleConfig> updateFieldStyleConfig(
            @PathVariable Long id,
            @RequestBody FieldStyleConfig config) {
        config.setId(id);
        FieldStyleConfig updated = fieldStyleConfigService.update(config);
        return Result.success(updated);
    }
    
    /**
     * 删除字段样式配置
     */
    @RequirePermission("report:manage")
    @OperationLog(module = "样式配置", type = OperationLog.OperationType.DELETE, description = "删除字段样式配置")
    @DeleteMapping("/field/{id}")
    public Result<Void> deleteFieldStyleConfig(@PathVariable Long id) {
        fieldStyleConfigService.delete(id);
        return Result.success();
    }
    
    /**
     * 批量保存字段样式配置
     */
    @RequirePermission("report:manage")
    @OperationLog(module = "样式配置", type = OperationLog.OperationType.UPDATE, description = "批量保存字段样式配置")
    @PostMapping("/field/batch/{reportId}")
    public Result<List<FieldStyleConfig>> batchSaveFieldStyleConfigs(
            @PathVariable Long reportId,
            @RequestBody List<FieldStyleConfig> configs) {
        Long userId = SecurityContext.getCurrentUserId();
        configs.forEach(c -> c.setCreatedBy(userId));
        List<FieldStyleConfig> saved = fieldStyleConfigService.batchSave(reportId, configs);
        return Result.success(saved);
    }
    
    /**
     * 删除报表的所有字段样式配置
     */
    @RequirePermission("report:manage")
    @OperationLog(module = "样式配置", type = OperationLog.OperationType.DELETE, description = "删除报表所有字段样式配置")
    @DeleteMapping("/field/report/{reportId}")
    public Result<Void> deleteFieldStyleConfigsByReportId(@PathVariable Long reportId) {
        fieldStyleConfigService.deleteByReportId(reportId);
        return Result.success();
    }
    
    /**
     * 复制报表样式配置
     */
    @RequirePermission("report:manage")
    @OperationLog(module = "样式配置", type = OperationLog.OperationType.CREATE, description = "复制报表样式配置")
    @PostMapping("/field/copy")
    public Result<List<FieldStyleConfig>> copyFieldStyleConfigs(@RequestBody CopyStyleRequest request) {
        List<FieldStyleConfig> copied = fieldStyleConfigService.copyStyles(
                request.getSourceReportId(), request.getTargetReportId());
        return Result.success(copied);
    }
    
    // ==================== 样式模板接口 ====================
    
    /**
     * 获取所有样式模板
     */
    @GetMapping("/template/list")
    public Result<List<StyleTemplate>> getAllTemplates() {
        List<StyleTemplate> templates = styleTemplateService.getAllTemplates();
        return Result.success(templates);
    }
    
    /**
     * 分页获取样式模板
     */
    @GetMapping("/template/page")
    public Result<PageResult<StyleTemplate>> getTemplatesByPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        List<StyleTemplate> templates = styleTemplateService.getTemplatesByPage(page, pageSize);
        long total = styleTemplateService.getTemplateCount();
        return Result.success(new PageResult<>(templates, total));
    }
    
    /**
     * 根据分类获取样式模板
     */
    @GetMapping("/template/category/{category}")
    public Result<List<StyleTemplate>> getTemplatesByCategory(@PathVariable String category) {
        List<StyleTemplate> templates = styleTemplateService.listByCategory(category);
        return Result.success(templates);
    }
    
    /**
     * 获取系统预设样式模板
     */
    @GetMapping("/template/system")
    public Result<List<StyleTemplate>> getSystemTemplates() {
        List<StyleTemplate> templates = styleTemplateService.listSystemTemplates();
        return Result.success(templates);
    }
    
    /**
     * 根据ID获取样式模板
     */
    @GetMapping("/template/{id}")
    public Result<StyleTemplate> getTemplateById(@PathVariable Long id) {
        StyleTemplate template = styleTemplateService.getById(id);
        return Result.success(template);
    }
    
    /**
     * 搜索样式模板
     */
    @GetMapping("/template/search")
    public Result<List<StyleTemplate>> searchTemplates(@RequestParam String keyword) {
        List<StyleTemplate> templates = styleTemplateService.searchByKeyword(keyword);
        return Result.success(templates);
    }
    
    /**
     * 创建样式模板
     */
    @RequirePermission("report:manage")
    @OperationLog(module = "样式模板", type = OperationLog.OperationType.CREATE, description = "创建样式模板")
    @PostMapping("/template")
    public Result<StyleTemplate> createTemplate(
            @RequestBody StyleTemplate template) {
        Long userId = SecurityContext.getCurrentUserId();
        template.setCreatedBy(userId);
        StyleTemplate created = styleTemplateService.create(template);
        return Result.success(created);
    }
    
    /**
     * 更新样式模板
     */
    @RequirePermission("report:manage")
    @OperationLog(module = "样式模板", type = OperationLog.OperationType.UPDATE, description = "更新样式模板")
    @PutMapping("/template/{id}")
    public Result<StyleTemplate> updateTemplate(
            @PathVariable Long id,
            @RequestBody StyleTemplate template) {
        template.setId(id);
        StyleTemplate updated = styleTemplateService.update(template);
        return Result.success(updated);
    }
    
    /**
     * 删除样式模板
     */
    @RequirePermission("report:manage")
    @OperationLog(module = "样式模板", type = OperationLog.OperationType.DELETE, description = "删除样式模板")
    @DeleteMapping("/template/{id}")
    public Result<Void> deleteTemplate(@PathVariable Long id) {
        styleTemplateService.delete(id);
        return Result.success();
    }
    
    /**
     * 应用样式模板（增加使用次数）
     */
    @PostMapping("/template/{id}/apply")
    public Result<StyleTemplate> applyTemplate(@PathVariable Long id) {
        styleTemplateService.incrementUseCount(id);
        StyleTemplate template = styleTemplateService.getById(id);
        return Result.success(template);
    }
    
    // ==================== 样式模板导入导出接口 ====================
    
    /**
     * 导出样式模板
     */
    @GetMapping("/template/{id}/export")
    public ResponseEntity<byte[]> exportTemplate(@PathVariable Long id) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            styleTemplateService.exportTemplate(id, out);
            
            StyleTemplate template = styleTemplateService.getById(id);
            String filename = template.getName().replaceAll("[^a-zA-Z0-9_\\-\\u4e00-\\u9fa5]", "_") + ".json";
            String encodedFileName = java.net.URLEncoder.encode(filename, "UTF-8").replaceAll("\\+", "%20");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Content-Disposition", 
                    "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encodedFileName);
            headers.set("Access-Control-Expose-Headers", "Content-Disposition");
            
            return ResponseEntity.ok().headers(headers).body(out.toByteArray());
        } catch (Exception e) {
            log.error("Failed to export style template: id={}", id, e);
            throw new com.dataplatform.common.exception.BusinessException(
                    com.dataplatform.common.exception.ErrorCode.ERROR, "导出样式模板失败: " + e.getMessage());
        }
    }
    
    /**
     * 导入样式模板
     */
    @RequirePermission("report:manage")
    @OperationLog(module = "样式模板", type = OperationLog.OperationType.CREATE, description = "导入样式模板")
    @PostMapping("/template/import")
    public Result<Long> importTemplate(@RequestParam("file") MultipartFile file) {
        try {
            Long templateId = styleTemplateService.importTemplate(file.getInputStream());
            return Result.success(templateId);
        } catch (Exception e) {
            log.error("Failed to import style template", e);
            throw new com.dataplatform.common.exception.BusinessException(
                    com.dataplatform.common.exception.ErrorCode.PARAM_ERROR, "导入样式模板失败: " + e.getMessage());
        }
    }
    
    /**
     * 批量导出样式模板
     */
    @PostMapping("/template/export-batch")
    public ResponseEntity<byte[]> exportTemplates(@RequestBody List<Long> ids) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            styleTemplateService.exportTemplates(ids, out);
            
            String filename = "style_templates_" + System.currentTimeMillis() + ".zip";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.set("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            headers.set("Access-Control-Expose-Headers", "Content-Disposition");
            
            return ResponseEntity.ok().headers(headers).body(out.toByteArray());
        } catch (Exception e) {
            log.error("Failed to export style templates", e);
            throw new com.dataplatform.common.exception.BusinessException(
                    com.dataplatform.common.exception.ErrorCode.ERROR, "批量导出样式模板失败: " + e.getMessage());
        }
    }
    
    /**
     * 批量导入样式模板
     */
    @RequirePermission("report:manage")
    @OperationLog(module = "样式模板", type = OperationLog.OperationType.CREATE, description = "批量导入样式模板")
    @PostMapping("/template/import-batch")
    public Result<List<Long>> importTemplates(@RequestParam("file") MultipartFile file) {
        try {
            List<Long> templateIds = styleTemplateService.importTemplates(file.getInputStream());
            return Result.success(templateIds);
        } catch (Exception e) {
            log.error("Failed to import style templates", e);
            throw new com.dataplatform.common.exception.BusinessException(
                    com.dataplatform.common.exception.ErrorCode.PARAM_ERROR, "批量导入样式模板失败: " + e.getMessage());
        }
    }
    
    
    /**
     * 复制样式请求DTO
     */
    @lombok.Data
    public static class CopyStyleRequest {
        private Long sourceReportId;
        private Long targetReportId;
    }
}
