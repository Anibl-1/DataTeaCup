package com.dataplatform.analytics.controller;

import com.dataplatform.common.annotation.OperationLog;
import com.dataplatform.common.annotation.RequirePermission;
import com.dataplatform.common.PageResult;
import com.dataplatform.common.Result;
import com.dataplatform.data.entity.ReportDefinition;
import com.dataplatform.data.entity.ReportTemplate;
import com.dataplatform.data.service.ReportTemplateService;
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
import java.util.Map;

/**
 * 报表模板控制器
 * 支持报表模板的 CRUD 操作（需求 11.2）
 * 支持模板参数化配置（需求 11.3）
 * 支持模板应用生成报表定义（需求 11.4, 2.5）
 * 支持模板导入导出（需求 11.5）
 * 
 * @author dataplatform
 */
@Slf4j
@RestController
@RequestMapping("/report-template")
@RequirePermission("report:read")
@RequiredArgsConstructor
public class ReportTemplateController {
    
    private final ReportTemplateService reportTemplateService;
    
    // ==================== 查询接口 ====================
    
    /**
     * 获取所有模板列表
     */
    @GetMapping("/list")
    public Result<List<ReportTemplate>> getAllTemplates() {
        List<ReportTemplate> templates = reportTemplateService.getAllTemplates();
        return Result.success(templates);
    }
    
    /**
     * 分页获取模板列表
     */
    @GetMapping("/page")
    public Result<PageResult<ReportTemplate>> getTemplatesByPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        List<ReportTemplate> templates = reportTemplateService.getTemplatesByPage(page, pageSize);
        long total = reportTemplateService.getTemplateCount();
        return Result.success(new PageResult<>(templates, total));
    }
    
    /**
     * 根据分类获取模板列表
     */
    @GetMapping("/category/{category}")
    public Result<List<ReportTemplate>> getTemplatesByCategory(@PathVariable String category) {
        List<ReportTemplate> templates = reportTemplateService.listByCategory(category);
        return Result.success(templates);
    }
    
    /**
     * 获取系统预设模板
     */
    @GetMapping("/system")
    public Result<List<ReportTemplate>> getSystemTemplates() {
        List<ReportTemplate> templates = reportTemplateService.listSystemTemplates();
        return Result.success(templates);
    }
    
    /**
     * 根据ID获取模板详情
     */
    @GetMapping("/{id}")
    public Result<ReportTemplate> getTemplateById(@PathVariable Long id) {
        ReportTemplate template = reportTemplateService.getById(id);
        return Result.success(template);
    }
    
    /**
     * 搜索模板
     */
    @GetMapping("/search")
    public Result<List<ReportTemplate>> searchTemplates(@RequestParam String keyword) {
        List<ReportTemplate> templates = reportTemplateService.searchByKeyword(keyword);
        return Result.success(templates);
    }
    
    // ==================== CRUD 接口（需求 11.2）====================
    
    /**
     * 创建模板
     */
    @RequirePermission("report:manage")
    @OperationLog(module = "报表模板", type = OperationLog.OperationType.CREATE, description = "创建报表模板")
    @PostMapping("/create")
    public Result<ReportTemplate> createTemplate(
            @RequestBody ReportTemplate template) {
        Long creatorId = SecurityContext.getCurrentUserId();
        template.setCreatorId(creatorId);
        ReportTemplate created = reportTemplateService.createTemplate(template);
        return Result.success(created);
    }
    
    /**
     * 更新模板
     */
    @RequirePermission("report:manage")
    @OperationLog(module = "报表模板", type = OperationLog.OperationType.UPDATE, description = "更新报表模板")
    @PutMapping("/{id}")
    public Result<ReportTemplate> updateTemplate(
            @PathVariable Long id,
            @RequestBody ReportTemplate template) {
        template.setId(id);
        ReportTemplate updated = reportTemplateService.updateTemplate(template);
        return Result.success(updated);
    }
    
    /**
     * 删除模板
     */
    @RequirePermission("report:manage")
    @OperationLog(module = "报表模板", type = OperationLog.OperationType.DELETE, description = "删除报表模板")
    @DeleteMapping("/{id}")
    public Result<Void> deleteTemplate(@PathVariable Long id) {
        reportTemplateService.deleteTemplate(id);
        return Result.success();
    }
    
    // ==================== 模板应用接口（需求 11.4, 2.5）====================
    
    /**
     * 应用模板生成报表定义
     * 一键生成完整的报表定义，包含 SQL、字段配置和参数配置
     * 
     * **Validates: Requirements 11.4, 2.5**
     * - WHEN 用户选择模板时，THE Template_System SHALL 一键生成完整的报表定义 (11.4)
     * - WHEN 用户选择报表模板时，THE Report_Designer SHALL 自动填充模板定义的 SQL 和字段配置 (2.5)
     * 
     * @param id 模板ID
     * @param params 参数值映射，用于替换模板中的参数占位符
     * @return 生成的报表定义
     */
    @PostMapping("/{id}/apply")
    public Result<ReportDefinition> applyTemplate(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> params) {
        ReportDefinition report = reportTemplateService.applyTemplate(id, params != null ? params : Map.of());
        return Result.success(report);
    }
    
    /**
     * 将现有报表保存为模板
     */
    @RequirePermission("report:manage")
    @OperationLog(module = "报表模板", type = OperationLog.OperationType.CREATE, description = "保存报表为模板")
    @PostMapping("/save-as-template")
    public Result<Long> saveAsTemplate(@RequestBody SaveAsTemplateRequest request) {
        Long templateId = reportTemplateService.saveAsTemplate(
                request.getReport(),
                request.getName(),
                request.getCategory(),
                request.getDescription()
        );
        return Result.success(templateId);
    }
    
    // ==================== 模板参数接口（需求 11.3）====================
    
    /**
     * 解析模板中的参数占位符
     */
    @GetMapping("/{id}/params")
    public Result<List<String>> parseTemplateParams(@PathVariable Long id) {
        List<String> params = reportTemplateService.parseTemplateParams(id);
        return Result.success(params);
    }
    
    /**
     * 验证参数值
     */
    @PostMapping("/{id}/validate-params")
    public Result<List<String>> validateParams(
            @PathVariable Long id,
            @RequestBody Map<String, Object> params) {
        List<String> errors = reportTemplateService.validateParams(id, params);
        return Result.success(errors);
    }
    
    // ==================== 模板导入导出接口（需求 11.5）====================
    
    /**
     * 导出单个模板
     */
    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> exportTemplate(@PathVariable Long id) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            reportTemplateService.exportTemplate(id, out);
            
            ReportTemplate template = reportTemplateService.getById(id);
            String filename = template.getName().replaceAll("[^a-zA-Z0-9_\\-\\u4e00-\\u9fa5]", "_") + ".json";
            String encodedFileName = java.net.URLEncoder.encode(filename, "UTF-8").replaceAll("\\+", "%20");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Content-Disposition", 
                    "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encodedFileName);
            headers.set("Access-Control-Expose-Headers", "Content-Disposition");
            
            return ResponseEntity.ok().headers(headers).body(out.toByteArray());
        } catch (Exception e) {
            log.error("Failed to export template: id={}", id, e);
            throw new com.dataplatform.common.exception.BusinessException(
                    com.dataplatform.common.exception.ErrorCode.ERROR, "导出模板失败: " + e.getMessage());
        }
    }
    
    /**
     * 导入单个模板
     */
    @RequirePermission("report:manage")
    @OperationLog(module = "报表模板", type = OperationLog.OperationType.CREATE, description = "导入报表模板")
    @PostMapping("/import")
    public Result<Long> importTemplate(@RequestParam("file") MultipartFile file) {
        try {
            Long templateId = reportTemplateService.importTemplate(file.getInputStream());
            return Result.success(templateId);
        } catch (Exception e) {
            log.error("Failed to import template", e);
            throw new com.dataplatform.common.exception.BusinessException(
                    com.dataplatform.common.exception.ErrorCode.PARAM_ERROR, "导入模板失败: " + e.getMessage());
        }
    }
    
    /**
     * 批量导出模板
     */
    @PostMapping("/export-batch")
    public ResponseEntity<byte[]> exportTemplates(@RequestBody List<Long> ids) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            reportTemplateService.exportTemplates(ids, out);
            
            String filename = "templates_" + System.currentTimeMillis() + ".zip";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.set("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            headers.set("Access-Control-Expose-Headers", "Content-Disposition");
            
            return ResponseEntity.ok().headers(headers).body(out.toByteArray());
        } catch (Exception e) {
            log.error("Failed to export templates", e);
            throw new com.dataplatform.common.exception.BusinessException(
                    com.dataplatform.common.exception.ErrorCode.ERROR, "批量导出模板失败: " + e.getMessage());
        }
    }
    
    /**
     * 批量导入模板
     */
    @RequirePermission("report:manage")
    @OperationLog(module = "报表模板", type = OperationLog.OperationType.CREATE, description = "批量导入报表模板")
    @PostMapping("/import-batch")
    public Result<List<Long>> importTemplates(@RequestParam("file") MultipartFile file) {
        try {
            List<Long> templateIds = reportTemplateService.importTemplates(file.getInputStream());
            return Result.success(templateIds);
        } catch (Exception e) {
            log.error("Failed to import templates", e);
            throw new com.dataplatform.common.exception.BusinessException(
                    com.dataplatform.common.exception.ErrorCode.PARAM_ERROR, "批量导入模板失败: " + e.getMessage());
        }
    }
    
    
    /**
     * 保存为模板请求DTO
     */
    @lombok.Data
    public static class SaveAsTemplateRequest {
        private ReportDefinition report;
        private String name;
        private String category;
        private String description;
    }
}
