package com.dataplatform.analytics.controller;

import com.dataplatform.common.annotation.OperationLog;
import com.dataplatform.common.annotation.RequirePermission;
import com.dataplatform.common.PageResult;
import com.dataplatform.common.Result;
import com.dataplatform.data.dto.ReportDefinitionCreateDTO;
import com.dataplatform.data.dto.ReportDefinitionUpdateDTO;
import com.dataplatform.data.dto.ReportFieldDTO;
import com.dataplatform.data.entity.ReportDefinition;
import com.dataplatform.data.service.ReportDefinitionService;
import com.dataplatform.common.security.SecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 报表定义控制器
 * 处理报表定义的增删改查和SQL执行
 * 
 * @author dataplatform
 */
@RestController
@RequestMapping("/report-definition")
@RequirePermission("report:read")
public class ReportDefinitionController {
    @Autowired
    private ReportDefinitionService reportDefinitionService;
    
    @Autowired
    private com.dataplatform.data.service.ReportShareService reportShareService;
    
    /**
     * 获取所有可用的报表列表（用于图表设计器选择）
     * 不分页，只返回启用状态的报表
     * 
     * @return 报表定义列表
     */
    @GetMapping("/available")
    public Result<List<ReportDefinition>> getAvailableReports() {
        List<ReportDefinition> list = reportDefinitionService.getAvailableReports();
        return Result.success(list);
    }
    
    /**
     * 获取报表定义列表（分页）
     * 
     * @param page 页码
     * @param pageSize 每页大小
     * @param keyword 搜索关键词（可选）
     * @return 报表定义列表
     */
    @GetMapping("/list")
    public Result<PageResult<ReportDefinition>> getReportDefinitionList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword) {
        page = Math.max(1, page);
        pageSize = Math.max(1, Math.min(200, pageSize));
        List<ReportDefinition> list = reportDefinitionService.getReportDefinitionList(page, pageSize, keyword);
        long total = reportDefinitionService.getReportDefinitionCount(keyword);
        
        PageResult<ReportDefinition> pageResult = new PageResult<>(list, total);
        return Result.success(pageResult);
    }
    
    /**
     * 根据ID获取报表定义
     * 
     * @param id 报表ID
     * @return 报表定义信息
     */
    @GetMapping("/{id}")
    public Result<ReportDefinition> getReportDefinitionById(@PathVariable Long id) {
        ReportDefinition report = reportDefinitionService.getReportDefinitionById(id);
        return Result.success(report);
    }
    
    /**
     * 根据编码获取报表定义
     * 
     * @param code 报表编码
     * @return 报表定义信息
     */
    @GetMapping("/code/{code}")
    public Result<ReportDefinition> getReportDefinitionByCode(@PathVariable String code) {
        ReportDefinition report = reportDefinitionService.getReportDefinitionByCode(code);
        return Result.success(report);
    }
    
    /**
     * 创建报表定义
     * 
     * @param dto 报表定义创建DTO
     * @return 报表定义ID
     */
    @OperationLog(module = "报表管理", type = OperationLog.OperationType.CREATE, description = "创建报表", saveResult = true)
    @PostMapping("/create")
    public Result<Long> createReportDefinition(
            @Valid @RequestBody ReportDefinitionCreateDTO dto) {
        Long createBy = SecurityContext.requireCurrentUserId();
        Long id = reportDefinitionService.createReportDefinition(dto, createBy);
        return Result.success(id);
    }
    
    
    /**
     * 更新报表定义
     * 
     * @param dto 报表定义更新DTO
     * @return 操作结果
     */
    @OperationLog(module = "报表管理", type = OperationLog.OperationType.UPDATE, description = "更新报表", saveResult = true)
    @PostMapping("/update")
    public Result<Void> updateReportDefinition(@Valid @RequestBody ReportDefinitionUpdateDTO dto) {
        reportDefinitionService.updateReportDefinition(dto);
        return Result.success(null);
    }
    
    /**
     * 删除报表定义
     * 
     * @param id 报表ID
     * @return 操作结果
     */
    @RequirePermission("report:manage")
    @OperationLog(module = "报表管理", type = OperationLog.OperationType.DELETE, description = "删除报表")
    @DeleteMapping("/{id}")
    public Result<Void> deleteReportDefinition(@PathVariable Long id) {
        reportDefinitionService.deleteReportDefinition(id);
        return Result.success(null);
    }
    
    /**
     * 更新报表移动端配置
     */
    @PutMapping("/{id}/mobile")
    public Result<Void> updateReportMobileEnabled(@PathVariable Long id, @RequestBody java.util.Map<String, Object> params) {
        Integer mobileEnabled = params.get("mobileEnabled") != null ? ((Number) params.get("mobileEnabled")).intValue() : 0;
        reportJdbcTemplate.update("UPDATE report_definition SET mobile_enabled = ? WHERE id = ?", mobileEnabled, id);
        return Result.success(null);
    }
    
    /**
     * 复制报表
     */
    @OperationLog(module = "报表管理", type = OperationLog.OperationType.CREATE, description = "复制报表", saveResult = true)
    @PostMapping("/{id}/copy")
    public Result<ReportDefinition> copyReportDefinition(@PathVariable Long id) {
        ReportDefinition copied = reportDefinitionService.copyReportDefinition(id);
        return Result.success(copied);
    }

    /**
     * 执行报表查询
     * 
     * @param id 报表ID
     * @param page 页码
     * @param pageSize 每页大小
     * @param filters 筛选条件（JSON字符串，可选）
     * @return 分页查询结果
     */
    @GetMapping("/{id}/execute")
    public Result<com.dataplatform.common.PageResult<Map<String, Object>>> executeReportQuery(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String filters,
            @RequestParam(required = false) String params) {
        com.dataplatform.common.PageResult<Map<String, Object>> result = 
            reportDefinitionService.executeReportQueryWithPagination(id, page, Math.min(pageSize, 500), filters, params);
        return Result.success(result);
    }
    
    /**
     * 执行报表查询（根据编码）
     * 
     * @param code 报表编码
     * @param page 页码
     * @param pageSize 每页大小
     * @param filters 筛选条件（JSON字符串，可选）
     * @return 分页查询结果
     */
    @GetMapping("/code/{code}/execute")
    public Result<com.dataplatform.common.PageResult<Map<String, Object>>> executeReportQueryByCode(
            @PathVariable String code,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String filters,
            @RequestParam(required = false) String params) {
        com.dataplatform.common.PageResult<Map<String, Object>> result = 
            reportDefinitionService.executeReportQueryByCodeWithPagination(code, page, Math.min(pageSize, 500), filters, params);
        return Result.success(result);
    }
    
    /**
     * 测试SQL并获取字段信息
     * 
     * @param dto 测试SQL DTO
     * @return 字段列表
     */
    @PostMapping("/test-sql")
    public Result<List<ReportFieldDTO>> testSql(@RequestBody com.dataplatform.data.dto.TestSqlDTO dto) {
        List<ReportFieldDTO> fields = reportDefinitionService.testSqlAndGetFields(dto.getDataSourceId(), dto.getSql());
        return Result.success(fields);
    }
    
    /**
     * 导出报表数据为Excel（根据ID）
     * 
     * @param id 报表ID
     * @param filters 筛选条件（JSON字符串，可选）
     * @return Excel文件响应
     */
    @OperationLog(module = "报表管理", type = OperationLog.OperationType.EXPORT, description = "导出报表")
    @GetMapping("/{id}/export")
    public ResponseEntity<?> exportReport(
            @PathVariable Long id,
            @RequestParam(required = false) String filters,
            @RequestParam(required = false) String params) {
        try {
            byte[] data = reportDefinitionService.exportReport(id, filters, params);
            ReportDefinition report = reportDefinitionService.getReportDefinitionById(id);
            String filename = (report != null && report.getReportName() != null) 
                ? report.getReportName() : "报表数据";
            
            // 对文件名进行URL编码，支持中文文件名
            String encodedFileName = java.net.URLEncoder.encode(filename + ".xlsx", "UTF-8")
                .replaceAll("\\+", "%20");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            // 使用RFC 5987标准的filename*参数支持UTF-8编码的文件名
            headers.set("Content-Disposition", 
                "attachment; filename=\"" + filename + ".xlsx\"; filename*=UTF-8''" + encodedFileName);
            headers.set("Access-Control-Expose-Headers", "Content-Disposition");
            return ResponseEntity.ok().headers(headers).body(data);
        } catch (com.dataplatform.common.exception.BusinessException e) {
            com.dataplatform.common.Result<Void> errorResult = 
                com.dataplatform.common.Result.error(e.getCode(), e.getMessage());
            HttpHeaders errorHeaders = new HttpHeaders();
            errorHeaders.setContentType(MediaType.APPLICATION_JSON);
            return ResponseEntity.status(500).headers(errorHeaders).body(errorResult);
        } catch (Exception e) {
            com.dataplatform.common.Result<Void> errorResult = 
                com.dataplatform.common.Result.error(500, "导出失败: " + e.getMessage());
            HttpHeaders errorHeaders = new HttpHeaders();
            errorHeaders.setContentType(MediaType.APPLICATION_JSON);
            return ResponseEntity.status(500).headers(errorHeaders).body(errorResult);
        }
    }
    
    /**
     * 创建报表分享链接
     */
    @PostMapping("/{id}/share")
    public Result<com.dataplatform.data.entity.ReportShare> createShare(
            @PathVariable Long id,
            @RequestBody Map<String, Object> params) {
        Long userId = SecurityContext.requireCurrentUserId();
        String password = (String) params.get("password");
        int expireHours = params.get("expireHours") != null ? ((Number) params.get("expireHours")).intValue() : 0;
        int maxAccessCount = params.get("maxAccessCount") != null ? ((Number) params.get("maxAccessCount")).intValue() : 0;
        com.dataplatform.data.entity.ReportShare share = reportShareService.createShare(
                id, "report", password, expireHours, maxAccessCount, userId);
        return Result.success(share);
    }

    /**
     * 获取报表分享列表
     */
    @GetMapping("/{id}/shares")
    public Result<java.util.List<com.dataplatform.data.entity.ReportShare>> getShares(@PathVariable Long id) {
        return Result.success(reportShareService.getShareList(id, "report"));
    }

    /**
     * 导出报表数据为Excel（根据编码）
     * 
     * @param code 报表编码
     * @param filters 筛选条件（JSON字符串，可选）
     * @return Excel文件响应
     */
    /**
     * 导出报表数据为PDF（根据ID）
     */
    @OperationLog(module = "报表管理", type = OperationLog.OperationType.EXPORT, description = "导出PDF报表")
    @GetMapping("/{id}/export-pdf")
    public ResponseEntity<?> exportReportAsPdf(
            @PathVariable Long id,
            @RequestParam(required = false) String filters,
            @RequestParam(required = false) String params) {
        try {
            byte[] data = reportDefinitionService.exportReportAsPdf(id, filters, params);
            ReportDefinition report = reportDefinitionService.getReportDefinitionById(id);
            String filename = (report != null && report.getReportName() != null) 
                ? report.getReportName() : "报表数据";
            String encodedFileName = java.net.URLEncoder.encode(filename + ".pdf", "UTF-8")
                .replaceAll("\\+", "%20");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.set("Content-Disposition", 
                "attachment; filename=\"" + filename + ".pdf\"; filename*=UTF-8''" + encodedFileName);
            headers.set("Access-Control-Expose-Headers", "Content-Disposition");
            return ResponseEntity.ok().headers(headers).body(data);
        } catch (com.dataplatform.common.exception.BusinessException e) {
            HttpHeaders errorHeaders = new HttpHeaders();
            errorHeaders.setContentType(MediaType.APPLICATION_JSON);
            return ResponseEntity.status(500).headers(errorHeaders).body(com.dataplatform.common.Result.error(e.getCode(), e.getMessage()));
        } catch (Exception e) {
            HttpHeaders errorHeaders = new HttpHeaders();
            errorHeaders.setContentType(MediaType.APPLICATION_JSON);
            return ResponseEntity.status(500).headers(errorHeaders).body(com.dataplatform.common.Result.error(500, "PDF导出失败: " + e.getMessage()));
        }
    }

    // ==================== 报表订阅 ====================
    
    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate reportJdbcTemplate;
    
    /**
     * 订阅报表
     */
    @PostMapping("/{id}/subscribe")
    public Result<Map<String, Object>> subscribeReport(@PathVariable Long id, @RequestBody Map<String, String> params) {
        String email = params.get("email");
        String cronExpr = params.getOrDefault("cron", "0 0 8 * * ?");
        if (email == null || email.isEmpty()) return Result.error("邮箱不能为空");
        
        try {
            reportJdbcTemplate.execute("CREATE TABLE IF NOT EXISTS sys_report_subscription (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "report_id BIGINT NOT NULL, " +
                "email VARCHAR(200) NOT NULL, " +
                "cron_expr VARCHAR(100) DEFAULT '0 0 8 * * ?', " +
                "status INT DEFAULT 1, " +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP)");
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).debug("初始化sys_report_subscription表: {}", e.getMessage());
        }
        
        reportJdbcTemplate.update("INSERT INTO sys_report_subscription (report_id, email, cron_expr) VALUES (?, ?, ?)",
            id, email, cronExpr);
        return Result.success(Map.of("message", "订阅成功"));
    }
    
    /**
     * 获取报表订阅列表
     */
    @GetMapping("/{id}/subscriptions")
    public Result<List<Map<String, Object>>> getSubscriptions(@PathVariable Long id) {
        try {
            List<Map<String, Object>> subs = reportJdbcTemplate.queryForList(
                "SELECT * FROM sys_report_subscription WHERE report_id = ? ORDER BY create_time DESC", id);
            return Result.success(subs);
        } catch (Exception e) {
            return Result.success(new java.util.ArrayList<>());
        }
    }
    
    /**
     * 取消订阅
     */
    @DeleteMapping("/{id}/subscriptions/{subId}")
    public Result<Void> unsubscribeReport(@PathVariable Long id, @PathVariable Long subId) {
        reportJdbcTemplate.update("DELETE FROM sys_report_subscription WHERE id = ? AND report_id = ?", subId, id);
        return Result.success();
    }
    
    @OperationLog(module = "报表管理", type = OperationLog.OperationType.EXPORT, description = "按编码导出报表")
    @GetMapping("/code/{code}/export")
    public ResponseEntity<?> exportReportByCode(
            @PathVariable String code,
            @RequestParam(required = false) String filters,
            @RequestParam(required = false) String params) {
        try {
            byte[] data = reportDefinitionService.exportReportByCode(code, filters, params);
            ReportDefinition report = reportDefinitionService.getReportDefinitionByCode(code);
            String filename = (report != null && report.getReportName() != null) 
                ? report.getReportName() : "报表数据";
            
            // 对文件名进行URL编码，支持中文文件名
            String encodedFileName = java.net.URLEncoder.encode(filename + ".xlsx", "UTF-8")
                .replaceAll("\\+", "%20");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            // 使用RFC 5987标准的filename*参数支持UTF-8编码的文件名
            headers.set("Content-Disposition", 
                "attachment; filename=\"" + filename + ".xlsx\"; filename*=UTF-8''" + encodedFileName);
            headers.set("Access-Control-Expose-Headers", "Content-Disposition");
            return ResponseEntity.ok().headers(headers).body(data);
        } catch (com.dataplatform.common.exception.BusinessException e) {
            com.dataplatform.common.Result<Void> errorResult = 
                com.dataplatform.common.Result.error(e.getCode(), e.getMessage());
            HttpHeaders errorHeaders = new HttpHeaders();
            errorHeaders.setContentType(MediaType.APPLICATION_JSON);
            return ResponseEntity.status(500).headers(errorHeaders).body(errorResult);
        } catch (Exception e) {
            com.dataplatform.common.Result<Void> errorResult = 
                com.dataplatform.common.Result.error(500, "导出失败: " + e.getMessage());
            HttpHeaders errorHeaders = new HttpHeaders();
            errorHeaders.setContentType(MediaType.APPLICATION_JSON);
            return ResponseEntity.status(500).headers(errorHeaders).body(errorResult);
        }
    }
}

