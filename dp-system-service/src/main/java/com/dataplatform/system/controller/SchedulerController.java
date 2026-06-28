package com.dataplatform.system.controller;

import com.dataplatform.common.Result;
import com.dataplatform.common.annotation.RequireRole;
import com.dataplatform.data.scheduler.UnifiedScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 调度器Web管理控制器
 */
@RestController
@RequestMapping("/scheduler")
@RequireRole("admin")
public class SchedulerController {

    @Autowired
    private UnifiedScheduler unifiedScheduler;
    
    @Value("${scheduler.enabled:true}")
    private boolean schedulerEnabled;
    
    @Value("${scheduler.collect-task.enabled:true}")
    private boolean collectTaskEnabled;
    
    @Value("${scheduler.datax-job.enabled:true}")
    private boolean dataxJobEnabled;
    
    @Value("${scheduler.pipeline.enabled:true}")
    private boolean pipelineEnabled;
    
    @Value("${scheduler.max-concurrent-tasks:10}")
    private int maxConcurrentTasks;

    /**
     * 获取调度器状态
     */
    @GetMapping("/status")
    public Result<Map<String, Object>> getStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("enabled", schedulerEnabled);
        status.put("collectTaskEnabled", collectTaskEnabled);
        status.put("dataxJobEnabled", dataxJobEnabled);
        status.put("pipelineEnabled", pipelineEnabled);
        status.put("maxConcurrentTasks", maxConcurrentTasks);
        status.put("statusText", unifiedScheduler.getStatus());
        return Result.success(status);
    }

    /**
     * 手动触发一次调度
     */
    @PostMapping("/trigger")
    public Result<Map<String, Object>> triggerOnce() {
        unifiedScheduler.triggerOnce();
        return Result.success(Map.of("message", "已触发一次调度", "time", new Date().toString()));
    }

    /**
     * 获取最近执行记录
     */
    @GetMapping("/history")
    public Result<List<Map<String, Object>>> getHistory() {
        List<Map<String, Object>> history = new ArrayList<>();
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("time", new Date().toString());
        entry.put("status", unifiedScheduler.getStatus());
        entry.put("type", "status_check");
        history.add(entry);
        return Result.success(history);
    }
}
