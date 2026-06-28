package com.dataplatform.data.controller;

import com.dataplatform.common.annotation.OperationLog;
import com.dataplatform.common.annotation.RequirePermission;
import com.dataplatform.common.PageResult;
import com.dataplatform.common.Result;
import com.dataplatform.data.dto.CollectTaskCreateDTO;
import com.dataplatform.data.dto.CollectTaskUpdateDTO;
import com.dataplatform.data.entity.CollectTask;
import com.dataplatform.data.entity.CollectLog;
import com.dataplatform.data.service.DataCollectService;
import com.dataplatform.data.mapper.CollectLogMapper;
import com.dataplatform.data.mapper.CollectTaskMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据采集控制器
 * 处理采集任务管理和数据查询相关接口
 * 
 * @author dataplatform
 */
@Slf4j
@RestController
@RequestMapping("/collect")
@RequirePermission("data:collect:read")
public class CollectController {
    @Autowired
    private DataCollectService dataCollectService;
    
    @Autowired
    private com.dataplatform.data.service.DataImportService dataImportService;
    
    @Autowired
    private CollectLogMapper collectLogMapper;
    
    @Autowired
    private CollectTaskMapper collectTaskMapper;

    /**
     * 调试接口：检查定时任务状态
     */
    @GetMapping("/task/scheduler/debug")
    public Result<Map<String, Object>> debugScheduler() {
        Map<String, Object> debug = new HashMap<>();
        List<CollectTask> scheduledTasks = collectTaskMapper.selectScheduledRunningTasks();
        debug.put("scheduledRunningTasksCount", scheduledTasks.size());
        debug.put("scheduledRunningTasks", scheduledTasks);
        debug.put("currentTime", new java.util.Date());
        return Result.success(debug);
    }

    /**
     * 获取采集任务列表（分页）
     * 
     * @param page 页码，默认1
     * @param pageSize 每页大小，默认10
     * @param filters 筛选条件（JSON字符串）
     * @return 分页结果
     */
    @GetMapping("/task/list")
    public Result<PageResult<CollectTask>> getCollectTaskList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String filters) {
        List<CollectTask> list = dataCollectService.getCollectTaskList(page, Math.min(pageSize, 200), filters);
        long total = dataCollectService.getCollectTaskCount(filters);
        return Result.success(new PageResult<>(list, total));
    }

    /**
     * 创建采集任务
     * 
     * @param taskDTO 采集任务创建信息
     * @return 操作结果
     */
    @RequirePermission("data:collect")
    @OperationLog(module = "数据采集", type = OperationLog.OperationType.CREATE, description = "创建采集任务")
    @PostMapping("/task/create")
    public Result<Void> createCollectTask(
            @Validated @RequestBody CollectTaskCreateDTO taskDTO) {
        CollectTask task = new CollectTask();
        BeanUtils.copyProperties(taskDTO, task);
        dataCollectService.createCollectTask(task);
        return Result.success();
    }

    /**
     * 更新采集任务
     * 
     * @param taskDTO 采集任务更新信息
     * @return 操作结果
     */
    @RequirePermission("data:collect")
    @OperationLog(module = "数据采集", type = OperationLog.OperationType.UPDATE, description = "更新采集任务")
    @PutMapping("/task/update")
    public Result<Void> updateCollectTask(
            @Validated @RequestBody CollectTaskUpdateDTO taskDTO) {
        CollectTask task = new CollectTask();
        BeanUtils.copyProperties(taskDTO, task);
        dataCollectService.updateCollectTask(task);
        return Result.success();
    }

    /**
     * 删除采集任务
     * 
     * @param id 任务ID
     * @return 操作结果
     */
    @RequirePermission("data:collect")
    @OperationLog(module = "数据采集", type = OperationLog.OperationType.DELETE, description = "删除采集任务")
    @DeleteMapping("/task/delete/{id}")
    public Result<Void> deleteCollectTask(
            @PathVariable Long id) {
        dataCollectService.deleteCollectTask(id);
        return Result.success();
    }

    /**
     * 启动采集任务
     * 定时任务只启用调度，非定时任务立即执行
     * 
     * @param id 任务ID
     * @return 操作结果
     */
    @RequirePermission("data:collect")
    @OperationLog(module = "数据采集", type = OperationLog.OperationType.UPDATE, description = "启动采集任务")
    @PostMapping("/task/start/{id}")
    public Result<Void> startCollectTask(
            @PathVariable Long id) {
        dataCollectService.startCollectTask(id);
        return Result.success();
    }
    
    /**
     * 立即执行一次任务（无论是否启用定时）
     * 
     * @param id 任务ID
     * @return 操作结果
     */
    @RequirePermission("data:collect")
    @OperationLog(module = "数据采集", type = OperationLog.OperationType.UPDATE, description = "执行采集任务")
    @PostMapping("/task/execute/{id}")
    public Result<Void> executeTaskOnce(
            @PathVariable Long id) {
        dataCollectService.executeTaskOnce(id);
        return Result.success();
    }

    /**
     * 停止采集任务
     * 
     * @param id 任务ID
     * @return 操作结果
     */
    @RequirePermission("data:collect")
    @OperationLog(module = "数据采集", type = OperationLog.OperationType.UPDATE, description = "停止采集任务")
    @PostMapping("/task/stop/{id}")
    public Result<Void> stopCollectTask(
            @PathVariable Long id) {
        dataCollectService.stopCollectTask(id);
        return Result.success();
    }

    /**
     * 查询采集数据
     * 
     * @param dataSourceId 数据源ID
     * @param tableName 表名
     * @param page 页码，默认1
     * @param pageSize 每页大小，默认10
     * @return 分页结果
     */
    @GetMapping("/data")
    public Result<PageResult<Map<String, Object>>> getCollectData(
            @RequestParam Long dataSourceId,
            @RequestParam String tableName,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        List<Map<String, Object>> list = dataCollectService.getCollectData(dataSourceId, tableName, page, pageSize);
        return Result.success(new PageResult<>(list, list.size()));
    }
    
    /**
     * 下载导入模板（含表头的空Excel文件）
     */
    @GetMapping("/import/template")
    public org.springframework.http.ResponseEntity<byte[]> downloadImportTemplate(
            @RequestParam Long dataSourceId,
            @RequestParam String tableName) {
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
                .contentType(org.springframework.http.MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    /**
     * 预览导入文件（不实际导入，仅返回预览数据）
     * 
     * @param file 上传的文件
     * @param firstRowAsHeader 第一行是否作为表头
     * @param previewRows 预览行数（默认10行）
     * @return 预览数据
     */
    @PostMapping("/import/preview")
    public Result<Map<String, Object>> previewImportFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "true") boolean firstRowAsHeader,
            @RequestParam(defaultValue = "10") int previewRows) {
        Map<String, Object> preview = dataImportService.previewFile(file, firstRowAsHeader, previewRows);
        return Result.success(preview);
    }
    
    /**
     * 导入数据文件（Excel/TXT）
     * 
     * @param file 上传的文件
     * @param dataSourceId 目标数据源ID
     * @param tableName 目标表名（可选，为空则自动生成）
     * @param autoCreateTable 是否自动创建表
     * @param firstRowAsHeader 第一行是否作为表头
     * @param truncateFirst 导入前是否清空表
     * @return 导入结果
     */
    @OperationLog(module = "数据采集", type = OperationLog.OperationType.IMPORT, description = "导入数据文件")
    @PostMapping("/import")
    public Result<Map<String, Object>> importData(
            @RequestParam("file") MultipartFile file,
            @RequestParam Long dataSourceId,
            @RequestParam(required = false) String tableName,
            @RequestParam(defaultValue = "true") boolean autoCreateTable,
            @RequestParam(defaultValue = "true") boolean firstRowAsHeader,
            @RequestParam(defaultValue = "false") boolean truncateFirst) {
        log.debug("[CollectController] 导入参数: dataSourceId={}, tableName={}, autoCreateTable={}, firstRowAsHeader={}, truncateFirst={}", 
            dataSourceId, tableName, autoCreateTable, firstRowAsHeader, truncateFirst);
        Map<String, Object> result = dataImportService.importData(
            file, dataSourceId, tableName, autoCreateTable, firstRowAsHeader, truncateFirst);
        return Result.success(result);
    }
    
    /**
     * 数据库到数据库导入
     * 从源数据源的表导入数据到目标数据源的表
     * 
     * @param sourceDataSourceId 源数据源ID
     * @param sourceTable 源表名
     * @param targetDataSourceId 目标数据源ID
     * @param targetTable 目标表名（可选，为空则使用源表名）
     * @param autoCreateTable 是否自动创建表
     * @param truncateFirst 导入前是否清空表
     * @param whereClause 筛选条件（可选）
     * @return 导入结果
     */
    @OperationLog(module = "数据采集", type = OperationLog.OperationType.IMPORT, description = "数据库导入")
    @PostMapping("/import/database")
    public Result<Map<String, Object>> importFromDatabase(
            @RequestParam Long sourceDataSourceId,
            @RequestParam String sourceTable,
            @RequestParam Long targetDataSourceId,
            @RequestParam(required = false) String targetTable,
            @RequestParam(defaultValue = "true") boolean autoCreateTable,
            @RequestParam(defaultValue = "false") boolean truncateFirst,
            @RequestParam(required = false) String whereClause) {
        log.debug("[CollectController] 数据库导入参数: sourceDataSourceId={}, sourceTable={}, targetDataSourceId={}, targetTable={}, autoCreateTable={}, truncateFirst={}, whereClause={}", 
            sourceDataSourceId, sourceTable, targetDataSourceId, targetTable, autoCreateTable, truncateFirst, whereClause);
        Map<String, Object> result = dataImportService.importFromDatabase(
            sourceDataSourceId, sourceTable, targetDataSourceId, targetTable, 
            autoCreateTable, truncateFirst, whereClause);
        return Result.success(result);
    }
    
    /**
     * 校验导入数据（不实际导入，仅返回校验结果）
     */
    @PostMapping("/import/validate")
    public Result<Map<String, Object>> validateImportData(
            @RequestParam("file") MultipartFile file,
            @RequestParam Long dataSourceId,
            @RequestParam String tableName,
            @RequestParam(defaultValue = "true") boolean firstRowAsHeader) {
        Map<String, Object> result = dataImportService.validateImportData(file, dataSourceId, tableName, firstRowAsHeader);
        return Result.success(result);
    }
    
    /**
     * 获取某字段的最大值（用于增量导入起始值）
     */
    @GetMapping("/field-max-value")
    public Result<Map<String, Object>> getFieldMaxValue(
            @RequestParam Long dataSourceId,
            @RequestParam String tableName,
            @RequestParam String fieldName) {
        Map<String, Object> result = dataImportService.getFieldMaxValue(dataSourceId, tableName, fieldName);
        return Result.success(result);
    }

    /**
     * 获取采集任务详情（使用视图，性能更优）
     * GET /collect/task/{id}/detail
     * 
     * 使用视图v_collect_task_detail，一次查询获取完整信息
     * 包含源数据源和目标数据源的详细信息
     * 
     * @param id 任务ID
     * @return 任务详情
     */
    @GetMapping("/task/{id}/detail")
    public Result<Map<String, Object>> getTaskDetail(@PathVariable Long id) {
        Map<String, Object> detail = dataCollectService.getTaskDetailById(id);
        return Result.success(detail);
    }
    
    /**
     * 获取增量采集任务列表
     * GET /collect/task/incremental
     * 
     * 使用视图v_incremental_tasks
     * 显示所有增量任务及其采集进度
     * 
     * @return 增量任务列表
     */
    @GetMapping("/task/incremental")
    public Result<List<Map<String, Object>>> getIncrementalTasks() {
        List<Map<String, Object>> tasks = dataCollectService.getIncrementalTasks();
        return Result.success(tasks);
    }
    
    /**
     * 获取采集日志列表（分页）
     */
    @GetMapping("/log/list")
    public Result<PageResult<CollectLog>> getLogList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Long taskId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        int offset = (page - 1) * pageSize;
        List<CollectLog> list = collectLogMapper.selectList(taskId, status, startDate, endDate, offset, pageSize);
        long total = collectLogMapper.count(taskId, status, startDate, endDate);
        return Result.success(new PageResult<>(list, total));
    }
    
    /**
     * 获取采集日志详情
     */
    @GetMapping("/log/{id}")
    public Result<CollectLog> getLogDetail(@PathVariable Long id) {
        CollectLog log = collectLogMapper.selectById(id);
        return Result.success(log);
    }
    
    /**
     * 获取任务最近的日志
     */
    @GetMapping("/log/task/{taskId}")
    public Result<List<CollectLog>> getTaskLogs(
            @PathVariable Long taskId,
            @RequestParam(defaultValue = "10") Integer limit) {
        List<CollectLog> logs = collectLogMapper.selectByTaskId(taskId, limit);
        return Result.success(logs);
    }
}
