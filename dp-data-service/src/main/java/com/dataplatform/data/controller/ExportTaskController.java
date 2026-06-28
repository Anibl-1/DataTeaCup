package com.dataplatform.data.controller;

import com.dataplatform.common.annotation.OperationLog;
import com.dataplatform.common.annotation.RequirePermission;
import com.dataplatform.common.PageResult;
import com.dataplatform.common.Result;
import com.dataplatform.data.entity.ExportTask;
import com.dataplatform.data.service.ExportTaskService;
import com.dataplatform.common.security.SecurityContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 导出任务控制器
 */
@Slf4j
@RestController
@RequestMapping("/export-task")
@RequirePermission("data:export")
public class ExportTaskController {
    
    @Autowired
    private ExportTaskService exportTaskService;
    
    /**
     * 创建报表导出任务
     */
    @OperationLog(module = "导出中心", type = OperationLog.OperationType.CREATE, description = "创建导出任务")
    @PostMapping("/create")
    public Result<ExportTask> createTask(@RequestBody Map<String, Object> params) {
        Long userId = SecurityContext.requireCurrentUserId();
        
        String taskName = (String) params.get("taskName");
        String taskType = (String) params.getOrDefault("taskType", "report");
        Long refId = params.get("refId") != null ? Long.valueOf(params.get("refId").toString()) : null;
        String refCode = (String) params.get("refCode");
        String filters = (String) params.get("filters");
        String customParams = (String) params.get("params");
        
        if (taskName == null || taskName.isEmpty()) {
            taskName = "导出任务";
        }
        
        ExportTask task = exportTaskService.createExportTask(taskName, taskType, refId, refCode, filters, customParams, userId);
        
        // 异步执行导出
        if ("report".equals(taskType)) {
            try {
                exportTaskService.executeReportExportAsync(task.getId());
            } catch (Exception e) {
                log.error("启动异步导出任务失败: taskId={}", task.getId(), e);
                // 异步调用失败时，标记任务为失败状态
                task.setStatus(3);
                task.setErrorMsg("启动导出任务失败: " + e.getMessage());
            }
        }
        
        return Result.success(task);
    }
    
    /**
     * 获取任务列表
     */
    @GetMapping("/list")
    public Result<PageResult<ExportTask>> getTaskList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String taskName,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Long userId = SecurityContext.requireCurrentUserId();
        PageResult<ExportTask> result = exportTaskService.getTaskList(userId, page, Math.min(pageSize, 200), taskName, startDate, endDate);
        return Result.success(result);
    }
    
    /**
     * 获取进行中的任务
     */
    @GetMapping("/pending")
    public Result<List<ExportTask>> getPendingTasks() {
        Long userId = SecurityContext.requireCurrentUserId();
        List<ExportTask> tasks = exportTaskService.getPendingTasks(userId);
        return Result.success(tasks);
    }
    
    /**
     * 获取任务详情
     */
    @GetMapping("/{id}")
    public Result<ExportTask> getTaskById(@PathVariable Long id) {
        Long userId = SecurityContext.requireCurrentUserId();
        ExportTask task = exportTaskService.getTaskById(id);
        if (task == null || !task.getCreateBy().equals(userId)) {
            return Result.error(404, "任务不存在");
        }
        return Result.success(task);
    }
    
    /**
     * 下载导出文件
     */
    @OperationLog(module = "导出中心", type = OperationLog.OperationType.EXPORT, description = "下载导出文件")
    @GetMapping("/{id}/download")
    public ResponseEntity<?> downloadFile(@PathVariable Long id) {
        try {
            Long userId = SecurityContext.requireCurrentUserId();
            File file = exportTaskService.getExportFile(id, userId);
            ExportTask task = exportTaskService.getTaskById(id);
            
            String fileName = task.getFileName() != null ? task.getFileName() : "export.xlsx";
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");
            
            Resource resource = new FileSystemResource(file);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + fileName + "\"; filename*=UTF-8''" + encodedFileName)
                    .header("Access-Control-Expose-Headers", "Content-Disposition")
                    .contentLength(file.length())
                    .body(resource);
        } catch (Exception e) {
            log.error("下载文件失败", e);
            return ResponseEntity.status(500).body(Result.error(500, e.getMessage()));
        }
    }
    
    /**
     * 删除任务
     */
    @OperationLog(module = "导出中心", type = OperationLog.OperationType.DELETE, description = "删除导出任务")
    @DeleteMapping("/{id}")
    public Result<Void> deleteTask(@PathVariable Long id) {
        Long userId = SecurityContext.requireCurrentUserId();
        boolean success = exportTaskService.deleteTask(id, userId);
        if (success) {
            return Result.success(null);
        } else {
            return Result.error(404, "任务不存在或无权删除");
        }
    }
    
}
