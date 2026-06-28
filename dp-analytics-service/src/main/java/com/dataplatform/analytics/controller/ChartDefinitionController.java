package com.dataplatform.analytics.controller;

import com.dataplatform.common.annotation.OperationLog;
import com.dataplatform.common.annotation.RequirePermission;
import com.dataplatform.common.PageResult;
import com.dataplatform.common.Result;
import com.dataplatform.data.entity.ChartDefinition;
import com.dataplatform.data.service.ChartDefinitionService;
import com.dataplatform.data.service.ChartCacheService;
import com.dataplatform.data.service.ChartDataExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 图表定义控制器
 * 处理图表定义的增删改查和SQL执行
 * 
 * @author dataplatform
 */
@RestController
@RequestMapping("/chart-definition")
@RequirePermission("chart:read")
public class ChartDefinitionController {
    @Autowired
    private ChartDefinitionService chartDefinitionService;

    @Autowired
    private ChartCacheService chartCacheService;

    @Autowired
    private ChartDataExportService chartDataExportService;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    
    /**
     * 获取图表定义列表（分页）
     */
    @GetMapping("/list")
    public Result<PageResult<ChartDefinition>> getChartDefinitionList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String chartType,
            @RequestParam(required = false) Integer status) {
        page = Math.max(1, page);
        pageSize = Math.max(1, Math.min(200, pageSize));
        List<ChartDefinition> list = chartDefinitionService.getChartDefinitionList(page, pageSize, keyword, chartType, status);
        long total = chartDefinitionService.getChartDefinitionCount(keyword, chartType, status);
        
        PageResult<ChartDefinition> pageResult = new PageResult<>(list, total);
        return Result.success(pageResult);
    }
    
    /**
     * 根据ID获取图表定义
     */
    @GetMapping("/{id}")
    public Result<ChartDefinition> getChartDefinitionById(@PathVariable Long id) {
        ChartDefinition chart = chartDefinitionService.getChartDefinitionById(id);
        return Result.success(chart);
    }
    
    /**
     * 根据编码获取图表定义
     */
    @GetMapping("/code/{code}")
    public Result<ChartDefinition> getChartDefinitionByCode(@PathVariable String code) {
        ChartDefinition chart = chartDefinitionService.getChartDefinitionByCode(code);
        return Result.success(chart);
    }
    
    /**
     * 创建图表定义
     */
    @RequirePermission("chart:manage")
    @OperationLog(module = "图表管理", type = OperationLog.OperationType.CREATE, description = "创建图表")
    @PostMapping
    public Result<ChartDefinition> createChartDefinition(@RequestBody ChartDefinition chart) {
        ChartDefinition created = chartDefinitionService.createChartDefinition(chart);
        return Result.success(created);
    }
    
    /**
     * 更新图表定义
     */
    @RequirePermission("chart:design")
    @OperationLog(module = "图表管理", type = OperationLog.OperationType.UPDATE, description = "更新图表")
    @PutMapping("/{id}")
    public Result<ChartDefinition> updateChartDefinition(@PathVariable Long id, @RequestBody ChartDefinition chart) {
        chart.setId(id);
        ChartDefinition updated = chartDefinitionService.updateChartDefinition(chart);
        return Result.success(updated);
    }
    
    /**
     * 删除图表定义
     */
    @RequirePermission("chart:manage")
    @OperationLog(module = "图表管理", type = OperationLog.OperationType.DELETE, description = "删除图表")
    @DeleteMapping("/{id}")
    public Result<Void> deleteChartDefinition(@PathVariable Long id) {
        chartDefinitionService.deleteChartDefinition(id);
        return Result.success(null);
    }
    
    /**
     * 更新图表移动端配置
     */
    @PutMapping("/{id}/mobile")
    public Result<Void> updateChartMobileEnabled(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        Integer mobileEnabled = params.get("mobileEnabled") != null ? ((Number) params.get("mobileEnabled")).intValue() : 0;
        jdbcTemplate.update("UPDATE chart_definition SET mobile_enabled = ? WHERE id = ?", mobileEnabled, id);
        return Result.success(null);
    }
    
    /**
     * 执行图表SQL查询，返回图表数据（带缓存）
     * @param id 图表ID
     * @param filters 筛选条件（JSON格式，可选）
     * @param limit 数据量限制（可选，默认10000）
     * @param useCache 是否使用缓存（可选，默认true）
     * @param parameters 查询参数（JSON格式，可选）
     */
    @GetMapping("/{id}/data")
    public Result<List<Map<String, Object>>> getChartData(
            @PathVariable Long id,
            @RequestParam(required = false) String filters,
            @RequestParam(required = false, defaultValue = "10000") Integer limit,
            @RequestParam(required = false, defaultValue = "true") Boolean useCache,
            @RequestParam(required = false) String parameters) {
        limit = Math.min(limit, 50000);
        
        // 尝试从缓存获取（缓存key需要包含parameters）
        if (Boolean.TRUE.equals(useCache)) {
            String cacheKey = chartCacheService.generateKey(id, filters, limit, parameters);
            List<Map<String, Object>> cachedData = chartCacheService.get(cacheKey);
            if (cachedData != null) {
                return Result.success(cachedData);
            }
        }
        
        // 从数据库查询
        List<Map<String, Object>> data = chartDefinitionService.executeChartQuery(id, filters, limit, parameters);
        
        // 存入缓存
        if (Boolean.TRUE.equals(useCache)) {
            String cacheKey = chartCacheService.generateKey(id, filters, limit, parameters);
            chartCacheService.set(cacheKey, data);
        }
        
        return Result.success(data);
    }
    
    /**
     * 清除图表缓存
     */
    @DeleteMapping("/{id}/cache")
    public Result<Void> clearChartCache(@PathVariable Long id) {
        chartCacheService.clearChart(id);
        return Result.success(null);
    }
    
    /**
     * 获取缓存统计信息
     */
    @GetMapping("/cache/stats")
    public Result<Map<String, Object>> getCacheStats() {
        return Result.success(chartCacheService.getStats());
    }
    
    /**
     * 导出图表数据为Excel
     */
    @OperationLog(module = "图表管理", type = OperationLog.OperationType.EXPORT, description = "导出图表Excel")
    @GetMapping("/{id}/export/excel")
    public ResponseEntity<byte[]> exportToExcel(
            @PathVariable Long id,
            @RequestParam(required = false) String filters,
            @RequestParam(required = false, defaultValue = "10000") Integer limit) throws IOException {
        
        ChartDefinition chart = chartDefinitionService.getChartDefinitionById(id);
        List<Map<String, Object>> data = chartDefinitionService.executeChartQuery(id, filters, limit);
        
        byte[] excelData = chartDataExportService.exportToExcel(data, chart.getChartName());
        
        String filename = URLEncoder.encode(chart.getChartName() + ".xlsx", StandardCharsets.UTF_8);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelData);
    }
    
    /**
     * 导出图表数据为CSV
     */
    @OperationLog(module = "图表管理", type = OperationLog.OperationType.EXPORT, description = "导出图表CSV")
    @GetMapping("/{id}/export/csv")
    public ResponseEntity<byte[]> exportToCsv(
            @PathVariable Long id,
            @RequestParam(required = false) String filters,
            @RequestParam(required = false, defaultValue = "10000") Integer limit) throws IOException {
        
        ChartDefinition chart = chartDefinitionService.getChartDefinitionById(id);
        List<Map<String, Object>> data = chartDefinitionService.executeChartQuery(id, filters, limit);
        
        byte[] csvData = chartDataExportService.exportToCsv(data, ",");
        
        String filename = URLEncoder.encode(chart.getChartName() + ".csv", StandardCharsets.UTF_8);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }
    
    /**
     * 导出图表数据为JSON
     */
    @OperationLog(module = "图表管理", type = OperationLog.OperationType.EXPORT, description = "导出图表JSON")
    @GetMapping("/{id}/export/json")
    public ResponseEntity<String> exportToJson(
            @PathVariable Long id,
            @RequestParam(required = false) String filters,
            @RequestParam(required = false, defaultValue = "10000") Integer limit) {
        
        ChartDefinition chart = chartDefinitionService.getChartDefinitionById(id);
        List<Map<String, Object>> data = chartDefinitionService.executeChartQuery(id, filters, limit);
        
        String jsonData = chartDataExportService.exportToJson(data);
        
        String filename = URLEncoder.encode(chart.getChartName() + ".json", StandardCharsets.UTF_8);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(jsonData);
    }
    
    /**
     * 测试SQL并获取数据（用于预览）
     */
    @PostMapping("/test-sql")
    public Result<List<Map<String, Object>>> testSql(@RequestBody Map<String, Object> params) {
        Long dataSourceId = Long.valueOf(params.get("dataSourceId").toString());
        String sql = params.get("sqlContent") != null ? params.get("sqlContent").toString() : params.get("sql").toString();
        Integer limit = params.get("limit") != null ? Math.min(Integer.valueOf(params.get("limit").toString()), 10000) : 10000;
        List<Map<String, Object>> data = chartDefinitionService.testSql(dataSourceId, sql, limit);
        return Result.success(data);
    }
    
    /**
     * 复制图表
     */
    @OperationLog(module = "图表管理", type = OperationLog.OperationType.CREATE, description = "复制图表")
    @PostMapping("/{id}/copy")
    public Result<ChartDefinition> copyChartDefinition(@PathVariable Long id) {
        ChartDefinition copied = chartDefinitionService.copyChartDefinition(id);
        return Result.success(copied);
    }
    
    /**
     * 更新图表状态
     */
    @PutMapping("/{id}/status")
    public Result<ChartDefinition> updateChartStatus(
            @PathVariable Long id,
            @RequestParam Integer status) {
        ChartDefinition updated = chartDefinitionService.updateChartStatus(id, status);
        return Result.success(updated);
    }
    
    /**
     * 批量更新图表状态
     */
    @OperationLog(module = "图表管理", type = OperationLog.OperationType.UPDATE, description = "批量更新图表状态")
    @PutMapping("/batch/status")
    public Result<Integer> batchUpdateChartStatus(
            @RequestBody Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        List<Number> ids = (List<Number>) params.get("ids");
        if (ids == null || ids.isEmpty()) {
            return Result.error("请选择要更新的图表");
        }
        if (ids.size() > 100) {
            return Result.error("批量操作最多支持100个");
        }
        Integer status = Integer.valueOf(params.get("status").toString());
        
        List<Long> chartIds = ids.stream()
                .map(Number::longValue)
                .collect(java.util.stream.Collectors.toList());
        
        int count = chartDefinitionService.batchUpdateChartStatus(chartIds, status);
        return Result.success(count);
    }
    
    /**
     * 批量删除图表
     */
    @OperationLog(module = "图表管理", type = OperationLog.OperationType.DELETE, description = "批量删除图表")
    @DeleteMapping("/batch")
    public Result<Integer> batchDeleteChartDefinitions(@RequestBody Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        List<Number> ids = (List<Number>) params.get("ids");
        if (ids == null || ids.isEmpty()) {
            return Result.error("请选择要删除的图表");
        }
        if (ids.size() > 100) {
            return Result.error("批量删除最多支持100个");
        }
        List<Long> chartIds = ids.stream()
                .map(Number::longValue)
                .collect(java.util.stream.Collectors.toList());
        int count = 0;
        for (Long id : chartIds) {
            try {
                chartDefinitionService.deleteChartDefinition(id);
                count++;
            } catch (Exception e) {
                // 跳过删除失败的图表，继续删除其他图表
            }
        }
        return Result.success(count);
    }

    /**
     * 获取图表数据统计信息
     */
    @GetMapping("/{id}/statistics")
    public Result<Map<String, Object>> getChartStatistics(
            @PathVariable Long id,
            @RequestParam String field,
            @RequestParam(required = false) String filters) {
        List<Map<String, Object>> data = chartDefinitionService.executeChartQuery(id, filters, 10000);
        Map<String, Object> stats = chartDataAggregateService.calculateStatistics(data, field);
        return Result.success(stats);
    }
    
    @Autowired
    private com.dataplatform.data.service.ChartDataAggregateService chartDataAggregateService;
    
    @Autowired
    private com.dataplatform.data.service.ReportShareService reportShareService;

    /**
     * 创建图表分享/嵌入链接
     */
    @PostMapping("/{id}/share")
    public Result<com.dataplatform.data.entity.ReportShare> createShare(
            @PathVariable Long id,
            @RequestBody Map<String, Object> params) {
        String password = (String) params.get("password");
        int expireHours = params.get("expireHours") != null ? ((Number) params.get("expireHours")).intValue() : 0;
        int maxAccessCount = params.get("maxAccessCount") != null ? ((Number) params.get("maxAccessCount")).intValue() : 0;
        com.dataplatform.data.entity.ReportShare share = reportShareService.createShare(
                id, "chart", password, expireHours, maxAccessCount, null);
        return Result.success(share);
    }

    /**
     * 获取图表分享列表
     */
    @GetMapping("/{id}/shares")
    public Result<List<com.dataplatform.data.entity.ReportShare>> getShares(@PathVariable Long id) {
        return Result.success(reportShareService.getShareList(id, "chart"));
    }

    /**
     * 图表嵌入端点（公开，无需认证）
     * 返回自包含HTML页面，可直接作为 iframe src 使用
     */
    @GetMapping("/{id}/embed")
    public ResponseEntity<String> embedChart(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "10000") Integer limit) {
        ChartDefinition chart = chartDefinitionService.getChartDefinitionById(id);
        if (chart == null || chart.getStatus() == null || chart.getStatus() != 1) {
            return ResponseEntity.notFound().build();
        }
        List<Map<String, Object>> data = chartDefinitionService.executeChartQuery(id, null, limit);
        String chartConfig = chart.getChartConfig() != null ? chart.getChartConfig() : "{}";
        // 将数据序列化为JSON
        String dataJson;
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            dataJson = om.writeValueAsString(data);
        } catch (Exception e) {
            dataJson = "[]";
        }
        String html = "<!DOCTYPE html><html><head><meta charset='UTF-8'>"
                + "<title>" + chart.getChartName().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;") + "</title>"
                + "<script src='https://cdn.jsdelivr.net/npm/echarts@5/dist/echarts.min.js'></script>"
                + "<style>html,body{margin:0;padding:0;width:100%;height:100%;}#chart{width:100%;height:100%;}</style>"
                + "</head><body><div id='chart'></div><script>"
                + "var chartDom=document.getElementById('chart');"
                + "var myChart=echarts.init(chartDom);"
                + "var rawConfig=" + chartConfig + ";"
                + "var rawData=" + dataJson + ";"
                + "if(rawConfig.series){myChart.setOption(rawConfig);}"
                + "else{myChart.setOption({tooltip:{},dataset:{source:rawData},xAxis:{type:'category'},yAxis:{},series:[{type:'bar'}]});}"
                + "window.addEventListener('resize',function(){myChart.resize();});"
                + "</script></body></html>";
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }
}

