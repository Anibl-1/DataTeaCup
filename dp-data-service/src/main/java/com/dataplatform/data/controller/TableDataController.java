package com.dataplatform.data.controller;

import com.dataplatform.common.annotation.OperationLog;
import com.dataplatform.common.annotation.RequirePermission;
import com.dataplatform.common.Result;
import com.dataplatform.data.service.TableDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * 数据表管理控制器
 */
@RestController
@RequestMapping("/tabledata")
@RequirePermission("tabledata:read")
public class TableDataController {

    @Autowired
    private TableDataService tableDataService;

    @Autowired
    private com.dataplatform.data.service.DataImportService dataImportService;

    @Autowired
    @Qualifier("taskExecutor")
    private Executor taskExecutor;

    /**
     * 获取数据源下的表列表
     */
    @RequirePermission("tabledata:read")
    @GetMapping("/tables/{dataSourceId}")
    public Result<List<Map<String, Object>>> getTables(@PathVariable Long dataSourceId) {
        return Result.success(tableDataService.getTables(dataSourceId));
    }

    /**
     * 获取表结构
     */
    @RequirePermission("tabledata:read")
    @GetMapping("/structure/{dataSourceId}/{tableName}")
    public Result<List<Map<String, Object>>> getTableStructure(
            @PathVariable Long dataSourceId,
            @PathVariable String tableName) {
        if (tableName == null || !tableName.matches("^[a-zA-Z0-9_.]+$")) {
            return Result.error("无效的表名");
        }
        return Result.success(tableDataService.getTableStructure(dataSourceId, tableName));
    }

    /**
     * 查询表数据
     */
    @SuppressWarnings("unchecked")
    @RequirePermission("tabledata:read")
    @PostMapping("/query")
    public Result<Map<String, Object>> queryTableData(@RequestBody Map<String, Object> params) {
        Long dataSourceId = Long.valueOf(params.get("dataSourceId").toString());
        String tableName = (String) params.get("tableName");
        
        // 参数校验
        if (tableName == null || tableName.trim().isEmpty()) {
            return Result.error("表名不能为空");
        }
        
        int page = params.get("page") != null ? Integer.parseInt(params.get("page").toString()) : 1;
        int pageSize = params.get("pageSize") != null ? Math.min(Integer.parseInt(params.get("pageSize").toString()), 500) : 50;
        String where = (String) params.get("where");
        String orderBy = (String) params.get("orderBy");
        
        // 安全的参数化搜索（优先于where字符串）
        String searchKeyword = (String) params.get("searchKeyword");
        List<String> searchColumns = params.get("searchColumns") instanceof List
                ? (List<String>) params.get("searchColumns") : null;
        
        return Result.success(tableDataService.queryTableData(
                dataSourceId, tableName, page, pageSize, where, orderBy, searchKeyword, searchColumns));
    }

    /**
     * 新增数据行
     */
    @RequirePermission("tabledata:manage")
    @OperationLog(module = "数据管理", type = OperationLog.OperationType.CREATE, description = "新增数据行")
    @PostMapping("/insert")
    public Result<Integer> insertRow(@RequestBody Map<String, Object> params) {
        Long dataSourceId = Long.valueOf(params.get("dataSourceId").toString());
        String tableName = (String) params.get("tableName");
        
        // 参数校验
        if (tableName == null || tableName.trim().isEmpty()) {
            return Result.error("表名不能为空");
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) params.get("data");
        if (data == null || data.isEmpty()) {
            return Result.error("新增数据不能为空");
        }
        
        return Result.success(tableDataService.insertRow(dataSourceId, tableName, data));
    }

    /**
     * 更新数据行（支持复合主键）
     */
    @RequirePermission("tabledata:manage")
    @OperationLog(module = "数据管理", type = OperationLog.OperationType.UPDATE, description = "更新数据行")
    @PostMapping("/update")
    public Result<Integer> updateRow(@RequestBody Map<String, Object> params) {
        Long dataSourceId = Long.valueOf(params.get("dataSourceId").toString());
        String tableName = (String) params.get("tableName");
        
        // 参数校验
        if (tableName == null || tableName.trim().isEmpty()) {
            return Result.error("表名不能为空");
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) params.get("data");
        if (data == null || data.isEmpty()) {
            return Result.error("更新数据不能为空");
        }
        
        // 支持复合主键
        @SuppressWarnings("unchecked")
        List<String> primaryKeys = (List<String>) params.get("primaryKeys");
        @SuppressWarnings("unchecked")
        Map<String, Object> primaryValues = (Map<String, Object>) params.get("primaryValues");
        
        if (primaryKeys != null && !primaryKeys.isEmpty() && primaryValues != null && !primaryValues.isEmpty()) {
            // 复合主键模式
            return Result.success(tableDataService.updateRowWithCompositeKey(dataSourceId, tableName, data, primaryKeys, primaryValues));
        }
        
        // 向后兼容：单主键模式
        String primaryKey = (String) params.get("primaryKey");
        if (primaryKey == null || primaryKey.trim().isEmpty()) {
            return Result.error("主键字段不能为空");
        }
        
        Object primaryValue = params.get("primaryValue");
        if (primaryValue == null) {
            return Result.error("主键值不能为空");
        }
        
        return Result.success(tableDataService.updateRow(dataSourceId, tableName, data, primaryKey, primaryValue));
    }

    /**
     * 删除数据行（支持复合主键）
     */
    @RequirePermission("tabledata:manage")
    @OperationLog(module = "数据管理", type = OperationLog.OperationType.DELETE, description = "删除数据行")
    @PostMapping("/delete")
    public Result<Integer> deleteRow(@RequestBody Map<String, Object> params) {
        Long dataSourceId = Long.valueOf(params.get("dataSourceId").toString());
        String tableName = (String) params.get("tableName");
        
        // 参数校验
        if (tableName == null || tableName.trim().isEmpty()) {
            return Result.error("表名不能为空");
        }
        
        // 支持复合主键
        @SuppressWarnings("unchecked")
        List<String> primaryKeys = (List<String>) params.get("primaryKeys");
        @SuppressWarnings("unchecked")
        Map<String, Object> primaryValues = (Map<String, Object>) params.get("primaryValues");
        
        if (primaryKeys != null && !primaryKeys.isEmpty() && primaryValues != null && !primaryValues.isEmpty()) {
            // 复合主键模式
            return Result.success(tableDataService.deleteRowWithCompositeKey(dataSourceId, tableName, primaryKeys, primaryValues));
        }
        
        // 向后兼容：单主键模式
        String primaryKey = (String) params.get("primaryKey");
        if (primaryKey == null || primaryKey.trim().isEmpty()) {
            return Result.error("主键字段不能为空");
        }
        
        Object primaryValue = params.get("primaryValue");
        if (primaryValue == null) {
            return Result.error("主键值不能为空");
        }
        
        return Result.success(tableDataService.deleteRow(dataSourceId, tableName, primaryKey, primaryValue));
    }

    /**
     * 批量删除数据行（支持复合主键）
     */
    @RequirePermission("tabledata:manage")
    @OperationLog(module = "数据管理", type = OperationLog.OperationType.DELETE, description = "批量删除数据行")
    @PostMapping("/batch-delete")
    public Result<Integer> batchDeleteRows(@RequestBody Map<String, Object> params) {
        Long dataSourceId = Long.valueOf(params.get("dataSourceId").toString());
        String tableName = (String) params.get("tableName");
        
        // 参数校验
        if (tableName == null || tableName.trim().isEmpty()) {
            return Result.error("表名不能为空");
        }
        
        // 支持复合主键
        @SuppressWarnings("unchecked")
        List<String> primaryKeys = (List<String>) params.get("primaryKeys");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> primaryValuesArray = (List<Map<String, Object>>) params.get("primaryValuesArray");
        
        if (primaryKeys != null && !primaryKeys.isEmpty() && primaryValuesArray != null && !primaryValuesArray.isEmpty()) {
            // 复合主键模式
            return Result.success(tableDataService.batchDeleteRowsWithCompositeKey(dataSourceId, tableName, primaryKeys, primaryValuesArray));
        }
        
        // 向后兼容：单主键模式
        String primaryKey = (String) params.get("primaryKey");
        if (primaryKey == null || primaryKey.trim().isEmpty()) {
            return Result.error("主键字段不能为空");
        }
        
        @SuppressWarnings("unchecked")
        List<Object> primaryValues = (List<Object>) params.get("primaryValues");
        if (primaryValues == null || primaryValues.isEmpty()) {
            return Result.error("请选择要删除的数据");
        }
        
        return Result.success(tableDataService.batchDeleteRows(dataSourceId, tableName, primaryKey, primaryValues));
    }

    /**
     * 导入数据
     */
    @RequirePermission("tabledata:manage")
    @OperationLog(module = "数据管理", type = OperationLog.OperationType.IMPORT, description = "导入数据", saveParams = false)
    @PostMapping("/import")
    public Result<Map<String, Object>> importData(
            @RequestParam("file") MultipartFile file,
            @RequestParam("dataSourceId") Long dataSourceId,
            @RequestParam("tableName") String tableName,
            @RequestParam("format") String format,
            @RequestParam("mode") String mode,
            @RequestParam(value = "uniqueFields", required = false) String uniqueFields,
            @RequestParam(value = "updateFieldMode", required = false) String updateFieldMode,
            @RequestParam(value = "updateFields", required = false) String updateFields) {
        
        // 参数校验
        if (file == null || file.isEmpty()) {
            return Result.error("请选择要导入的文件");
        }
        if (tableName == null || tableName.trim().isEmpty()) {
            return Result.error("表名不能为空");
        }
        if (format == null || (!format.equals("excel") && !format.equals("csv"))) {
            return Result.error("不支持的文件格式");
        }
        if (mode == null || (!mode.equals("append") && !mode.equals("increment") && !mode.equals("replace"))) {
            return Result.error("不支持的导入模式");
        }
        // 增量模式必须指定唯一字段
        if ("increment".equals(mode) && (uniqueFields == null || uniqueFields.trim().isEmpty())) {
            return Result.error("增量导入模式必须指定唯一标识字段");
        }
        
        return Result.success(tableDataService.importData(file, dataSourceId, tableName, format, mode, 
                uniqueFields, updateFieldMode, updateFields));
    }

    /**
     * 带字段映射的数据导入
     */
    @RequirePermission("tabledata:manage")
    @OperationLog(module = "数据管理", type = OperationLog.OperationType.IMPORT, description = "字段映射导入", saveParams = false)
    @PostMapping("/import-mapped")
    public Result<Map<String, Object>> importDataWithMapping(
            @RequestParam("file") MultipartFile file,
            @RequestParam("dataSourceId") Long dataSourceId,
            @RequestParam("tableName") String tableName,
            @RequestParam("mapping") String mappingJson) {
        if (file == null || file.isEmpty()) return Result.error("请选择文件");
        if (tableName == null || tableName.trim().isEmpty()) return Result.error("表名不能为空");
        
        try {
            // 解析字段映射 JSON: {"sourceCol": "targetCol", ...}
            @SuppressWarnings("unchecked")
            Map<String, String> mapping = new com.fasterxml.jackson.databind.ObjectMapper()
                .readValue(mappingJson, Map.class);
            
            String taskId = "import_" + System.currentTimeMillis();
            importProgress.put(taskId, new int[]{0, 0}); // [current, total]
            
            // 异步执行导入
            taskExecutor.execute(() -> {
                try {
                    Map<String, Object> result = tableDataService.importDataWithMapping(
                        file, dataSourceId, tableName, mapping, (current, total) -> {
                            importProgress.put(taskId, new int[]{current, total});
                        });
                    importProgress.put(taskId, new int[]{-1, -1}); // -1 表示完成
                } catch (Exception e) {
                    importProgress.put(taskId, new int[]{-2, -2}); // -2 表示失败
                }
            });
            
            return Result.success(Map.of("taskId", taskId, "message", "导入任务已启动"));
        } catch (Exception e) {
            return Result.error("导入失败: " + e.getMessage());
        }
    }
    
    /** 导入进度存储 */
    private final java.util.concurrent.ConcurrentHashMap<String, int[]> importProgress = new java.util.concurrent.ConcurrentHashMap<>();
    
    /**
     * 查询导入进度
     */
    @GetMapping("/import-progress/{taskId}")
    public Result<Map<String, Object>> getImportProgress(@PathVariable String taskId) {
        int[] progress = importProgress.get(taskId);
        if (progress == null) return Result.error("任务不存在");
        
        Map<String, Object> result = new java.util.HashMap<>();
        if (progress[0] == -1) {
            result.put("status", "completed");
            result.put("percent", 100);
            importProgress.remove(taskId);
        } else if (progress[0] == -2) {
            result.put("status", "failed");
            result.put("percent", 0);
            importProgress.remove(taskId);
        } else {
            result.put("status", "running");
            result.put("current", progress[0]);
            result.put("total", progress[1]);
            result.put("percent", progress[1] > 0 ? Math.round(progress[0] * 100.0 / progress[1]) : 0);
        }
        return Result.success(result);
    }
    
    /**
     * 建表（DDL）
     */
    @RequirePermission("tabledata:manage")
    @OperationLog(module = "数据管理", type = OperationLog.OperationType.CREATE, description = "创建数据表")
    @PostMapping("/create-table")
    public Result<Map<String, Object>> createTable(@RequestBody Map<String, Object> params) {
        Long dataSourceId = Long.valueOf(params.get("dataSourceId").toString());
        String tableName = (String) params.get("tableName");
        if (tableName == null || tableName.trim().isEmpty()) return Result.error("表名不能为空");
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> columns = (List<Map<String, Object>>) params.get("columns");
        if (columns == null || columns.isEmpty()) return Result.error("字段列表不能为空");
        
        String primaryKey = (String) params.get("primaryKey");
        String tableComment = (String) params.get("tableComment");
        
        return Result.success(tableDataService.createTable(dataSourceId, tableName, columns, primaryKey, tableComment));
    }
    
    /**
     * 改表（DDL）— 加字段/删字段/改字段
     */
    @RequirePermission("tabledata:manage")
    @OperationLog(module = "数据管理", type = OperationLog.OperationType.UPDATE, description = "修改表结构")
    @PostMapping("/alter-table")
    public Result<Map<String, Object>> alterTable(@RequestBody Map<String, Object> params) {
        Long dataSourceId = Long.valueOf(params.get("dataSourceId").toString());
        String tableName = (String) params.get("tableName");
        if (tableName == null || tableName.trim().isEmpty()) return Result.error("表名不能为空");
        
        String action = (String) params.get("action"); // ADD, DROP, MODIFY
        if (action == null) return Result.error("操作类型不能为空");
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> columns = (List<Map<String, Object>>) params.get("columns");
        
        return Result.success(tableDataService.alterTable(dataSourceId, tableName, action, columns));
    }
    
    /**
     * 导出数据
     */
    @RequirePermission("tabledata:read")
    @OperationLog(module = "数据管理", type = OperationLog.OperationType.EXPORT, description = "导出数据")
    @PostMapping("/export")
    public void exportData(@RequestBody Map<String, Object> params, HttpServletResponse response) {
        Long dataSourceId = Long.valueOf(params.get("dataSourceId").toString());
        String tableName = (String) params.get("tableName");
        if (tableName == null || !tableName.matches("^[a-zA-Z0-9_.]+$")) {
            response.setStatus(400);
            return;
        }
        String format = (String) params.get("format");
        String where = (String) params.get("where");
        
        tableDataService.exportData(dataSourceId, tableName, format, where, response);
    }

    /**
     * 执行SQL
     */
    @RequirePermission("tabledata:manage")
    @OperationLog(module = "数据管理", type = OperationLog.OperationType.UPDATE, description = "执行SQL")
    @PostMapping("/execute-sql")
    public Result<Map<String, Object>> executeSql(@RequestBody Map<String, Object> params) {
        Long dataSourceId = Long.valueOf(params.get("dataSourceId").toString());
        String sql = (String) params.get("sql");
        
        return Result.success(tableDataService.executeSql(dataSourceId, sql));
    }

    /**
     * 下载导入模板（含表头的空Excel文件）
     */
    @GetMapping("/import/template")
    public org.springframework.http.ResponseEntity<byte[]> downloadImportTemplate(
            @RequestParam Long dataSourceId,
            @RequestParam String tableName) {
        if (tableName == null || !tableName.matches("^[a-zA-Z0-9_.]+$")) {
            return org.springframework.http.ResponseEntity.badRequest().body(new byte[0]);
        }
        byte[] data = dataImportService.generateImportTemplate(dataSourceId, tableName);
        String filename;
        try {
            filename = java.net.URLEncoder.encode(tableName + "_导入模板.xlsx", java.nio.charset.StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        } catch (Exception e) {
            filename = "import_template.xlsx";
        }
        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + filename)
                .header(org.springframework.http.HttpHeaders.CONTENT_TYPE,
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .body(data);
    }
}
