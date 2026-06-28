package com.dataplatform.data.controller;

import com.dataplatform.common.Result;
import com.dataplatform.common.annotation.RequirePermission;
import com.dataplatform.data.service.sync.DataSyncService;
import com.dataplatform.data.service.sync.SyncExecutionLog;
import com.dataplatform.data.service.sync.SyncTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 鏁版嵁鍚屾鎺у埗鍣?
 * 鎻愪緵鏁版嵁鍚屾浠诲姟绠＄悊鍜屾墽琛岀殑REST API
 * 
 * @author dataplatform
 */
@Slf4j
@RestController
@RequestMapping("/data-sync")
@RequiredArgsConstructor
@RequirePermission("data:sync")
public class DataSyncController {

    private final DataSyncService dataSyncService;

    /**
     * 鑾峰彇鍚屾浠诲姟鍒楄〃
     */
    @GetMapping("/tasks")
    public Result<List<SyncTask>> getTasks() {
        try {
            List<SyncTask> tasks = dataSyncService.listTasks();
            return Result.success(tasks);
        } catch (Exception e) {
            log.error("鑾峰彇鍚屾浠诲姟鍒楄〃澶辫触", e);
            return Result.error("鑾峰彇鍚屾浠诲姟鍒楄〃澶辫触: " + e.getMessage());
        }
    }

    /**
     * 鑾峰彇鍚屾浠诲姟璇︽儏
     */
    @GetMapping("/tasks/{taskId}")
    public Result<SyncTask> getTask(@PathVariable String taskId) {
        try {
            SyncTask task = dataSyncService.getTask(taskId);
            if (task == null) {
                return Result.error("浠诲姟涓嶅瓨鍦? " + taskId);
            }
            return Result.success(task);
        } catch (Exception e) {
            log.error("鑾峰彇鍚屾浠诲姟璇︽儏澶辫触: taskId={}", taskId, e);
            return Result.error("鑾峰彇鍚屾浠诲姟璇︽儏澶辫触: " + e.getMessage());
        }
    }

    /**
     * 鍒涘缓鍚屾浠诲姟
     */
    @PostMapping("/tasks")
    public Result<SyncTask> createTask(@RequestBody SyncTask task) {
        try {
            SyncTask created = dataSyncService.createTask(task);
            return Result.success(created);
        } catch (Exception e) {
            log.error("鍒涘缓鍚屾浠诲姟澶辫触", e);
            return Result.error("鍒涘缓鍚屾浠诲姟澶辫触: " + e.getMessage());
        }
    }

    /**
     * 鍚姩鍚屾浠诲姟锛堟仮澶嶏級
     */
    @PostMapping("/tasks/{taskId}/start")
    public Result<Void> startTask(@PathVariable String taskId) {
        try {
            dataSyncService.resumeTask(taskId);
            return Result.success();
        } catch (Exception e) {
            log.error("鍚姩鍚屾浠诲姟澶辫触: taskId={}", taskId, e);
            return Result.error("鍚姩鍚屾浠诲姟澶辫触: " + e.getMessage());
        }
    }

    /**
     * 鍋滄鍚屾浠诲姟锛堟殏鍋滐級
     */
    @PostMapping("/tasks/{taskId}/stop")
    public Result<Void> stopTask(@PathVariable String taskId) {
        try {
            dataSyncService.pauseTask(taskId);
            return Result.success();
        } catch (Exception e) {
            log.error("鍋滄鍚屾浠诲姟澶辫触: taskId={}", taskId, e);
            return Result.error("鍋滄鍚屾浠诲姟澶辫触: " + e.getMessage());
        }
    }

    /**
     * 鑾峰彇鍚屾鎵ц鏃ュ織
     */
    @GetMapping("/tasks/{taskId}/logs")
    public Result<List<SyncExecutionLog>> getLogs(
            @PathVariable String taskId,
            @RequestParam(defaultValue = "50") int limit) {
        try {
            List<SyncExecutionLog> logs = dataSyncService.getExecutionLogs(taskId, Math.min(limit, 500));
            return Result.success(logs);
        } catch (Exception e) {
            log.error("鑾峰彇鍚屾鎵ц鏃ュ織澶辫触: taskId={}", taskId, e);
            return Result.error("鑾峰彇鍚屾鎵ц鏃ュ織澶辫触: " + e.getMessage());
        }
    }

    /**
     * 鎵ц鍚屾浠诲姟
     */
    @PostMapping("/tasks/{taskId}/execute")
    public Result<SyncExecutionLog> executeTask(@PathVariable String taskId) {
        try {
            SyncExecutionLog log = dataSyncService.executeTask(taskId);
            return Result.success(log);
        } catch (Exception e) {
            log.error("鎵ц鍚屾浠诲姟澶辫触: taskId={}", taskId, e);
            return Result.error("鎵ц鍚屾浠诲姟澶辫触: " + e.getMessage());
        }
    }
}
