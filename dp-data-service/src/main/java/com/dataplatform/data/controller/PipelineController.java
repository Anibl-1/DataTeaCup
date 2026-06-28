package com.dataplatform.data.controller;

import com.dataplatform.common.annotation.OperationLog;
import com.dataplatform.common.annotation.RequirePermission;
import com.dataplatform.common.Result;
import com.dataplatform.data.entity.Pipeline;
import com.dataplatform.data.entity.PipelineExecution;
import com.dataplatform.data.entity.PipelineNode;
import com.dataplatform.data.service.PipelineService;
import com.dataplatform.data.service.WecomNotifyService;
import com.dataplatform.data.service.EmailNotifyService;
import com.dataplatform.data.service.SmsNotifyService;
import com.dataplatform.data.service.DingtalkNotifyService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/pipeline")
@RequirePermission("pipeline:read")
public class PipelineController {
    
    @Autowired
    private PipelineService pipelineService;

    @Autowired @Nullable
    private WecomNotifyService wecomNotifyService;
    @Autowired @Nullable
    private EmailNotifyService emailNotifyService;
    @Autowired @Nullable
    private SmsNotifyService smsNotifyService;
    @Autowired @Nullable
    private DingtalkNotifyService dingtalkNotifyService;
    
    // ==================== 流程管理 ====================
    
    @GetMapping("/list")
    public Result<List<Pipeline>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer pipelineType,
            @RequestParam(required = false) Integer pipelineStatus,
            @RequestParam(required = false) Integer scheduleType) {
        List<Pipeline> pipelines = pipelineService.search(keyword, pipelineType, pipelineStatus, scheduleType);
        return Result.success(pipelines);
    }
    
    @GetMapping("/{id}")
    public Result<Pipeline> getById(@PathVariable Long id) {
        Pipeline pipeline = pipelineService.findById(id);
        if (pipeline == null) {
            return Result.error("流程不存在");
        }
        return Result.success(pipeline);
    }
    
    @RequirePermission("pipeline:manage")
    @OperationLog(module = "流程管理", type = OperationLog.OperationType.CREATE, description = "创建流程")
    @PostMapping
    public Result<Pipeline> create(@RequestBody Pipeline pipeline, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        pipeline.setCreateBy(userId);
        Pipeline created = pipelineService.create(pipeline);
        return Result.success(created);
    }

    @RequirePermission("pipeline:manage")
    @OperationLog(module = "流程管理", type = OperationLog.OperationType.UPDATE, description = "更新流程")
    @PutMapping("/{id}")
    public Result<Pipeline> update(@PathVariable Long id, @RequestBody Pipeline pipeline) {
        pipeline.setId(id);
        Pipeline updated = pipelineService.update(pipeline);
        return Result.success(updated);
    }
    
    @RequirePermission("pipeline:manage")
    @OperationLog(module = "流程管理", type = OperationLog.OperationType.DELETE, description = "删除流程")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        pipelineService.delete(id);
        return Result.success();
    }
    
    @RequirePermission("pipeline:manage")
    @OperationLog(module = "流程管理", type = OperationLog.OperationType.UPDATE, description = "更新流程状态")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Integer status = body.get("status");
        pipelineService.updateStatus(id, status);
        return Result.success();
    }
    
    @RequirePermission("pipeline:manage")
    @OperationLog(module = "流程管理", type = OperationLog.OperationType.CREATE, description = "复制流程")
    @PostMapping("/{id}/copy")
    public Result<Pipeline> copy(@PathVariable Long id) {
        Pipeline copied = pipelineService.copy(id);
        return Result.success(copied);
    }
    
    // ==================== 流程设计 ====================
    
    @GetMapping("/{id}/design")
    public Result<Map<String, Object>> getDesign(@PathVariable Long id) {
        Map<String, Object> design = pipelineService.getDesign(id);
        return Result.success(design);
    }
    
    @RequirePermission("pipeline:design")
    @PostMapping("/{id}/design")
    public Result<Void> saveDesign(@PathVariable Long id, @RequestBody DesignRequest request) {
        pipelineService.saveDesign(id, request.getFlowJson(), request.getNodes());
        return Result.success();
    }
    
    // ==================== 流程执行 ====================
    
    @RequirePermission("pipeline:execute")
    @OperationLog(module = "流程管理", type = OperationLog.OperationType.UPDATE, description = "执行流程")
    @PostMapping("/{id}/execute")
    public Result<PipelineExecution> execute(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        PipelineExecution execution = pipelineService.execute(id, userId);
        return Result.success(execution);
    }
    
    @RequirePermission("pipeline:execute")
    @OperationLog(module = "流程管理", type = OperationLog.OperationType.UPDATE, description = "停止流程执行")
    @PostMapping("/execution/{id}/stop")
    public Result<Void> stopExecution(@PathVariable Long id) {
        pipelineService.stopExecution(id);
        return Result.success();
    }
    
    // ==================== 执行日志 ====================
    
    @GetMapping("/executions")
    public Result<List<PipelineExecution>> getExecutions(
            @RequestParam(required = false) Long pipelineId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer triggerType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        List<PipelineExecution> executions = pipelineService.getExecutions(
            pipelineId, status, triggerType, startDate, endDate);
        return Result.success(executions);
    }
    
    @GetMapping("/execution/{id}")
    public Result<PipelineExecution> getExecution(@PathVariable Long id) {
        PipelineExecution execution = pipelineService.getExecution(id);
        return Result.success(execution);
    }
    
    @GetMapping("/executions/running")
    public Result<List<PipelineExecution>> getRunningExecutions() {
        List<PipelineExecution> executions = pipelineService.getRunningExecutions();
        return Result.success(executions);
    }
    
    /**
     * 暂停定时调度
     */
    @RequirePermission("pipeline:manage")
    @OperationLog(module = "流程管理", type = OperationLog.OperationType.UPDATE, description = "暂停定时调度")
    @PostMapping("/{id}/pause")
    public Result<Void> pauseSchedule(@PathVariable Long id) {
        Pipeline pipeline = pipelineService.findById(id);
        if (pipeline == null) return Result.error("流程不存在");
        pipeline.setScheduleStatus("paused");
        pipelineService.update(pipeline);
        return Result.success();
    }

    /**
     * 恢复定时调度
     */
    @RequirePermission("pipeline:manage")
    @OperationLog(module = "流程管理", type = OperationLog.OperationType.UPDATE, description = "恢复定时调度")
    @PostMapping("/{id}/resume")
    public Result<Void> resumeSchedule(@PathVariable Long id) {
        Pipeline pipeline = pipelineService.findById(id);
        if (pipeline == null) return Result.error("流程不存在");
        pipeline.setScheduleStatus("running");
        pipelineService.update(pipeline);
        return Result.success();
    }

    // ==================== 统计 ====================
    
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = pipelineService.getStatistics();
        return Result.success(stats);
    }

    @GetMapping("/statistics/trend")
    public Result<List<Map<String, Object>>> getExecutionTrend(
            @RequestParam(defaultValue = "7") int days) {
        return Result.success(pipelineService.getExecutionTrend(days));
    }
    
    // ==================== 节点类型 ====================
    
    @GetMapping("/node-types")
    public Result<List<Map<String, Object>>> getNodeTypes() {
        List<Map<String, Object>> nodeTypes = new ArrayList<>();
        
        Map<String, Object> source = new HashMap<>();
        source.put("type", "source");
        source.put("name", "数据源");
        source.put("icon", "ServerOutline");
        source.put("color", "#18a058");
        source.put("description", "从数据库、文件或API读取数据");
        nodeTypes.add(source);
        
        Map<String, Object> transform = new HashMap<>();
        transform.put("type", "transform");
        transform.put("name", "数据转换");
        transform.put("icon", "SwapHorizontalOutline");
        transform.put("color", "#2080f0");
        transform.put("description", "字段映射、类型转换、数据清洗");
        nodeTypes.add(transform);
        
        Map<String, Object> filter = new HashMap<>();
        filter.put("type", "filter");
        filter.put("name", "数据过滤");
        filter.put("icon", "FilterOutline");
        filter.put("color", "#f0a020");
        filter.put("description", "按条件过滤数据行");
        nodeTypes.add(filter);
        
        Map<String, Object> aggregate = new HashMap<>();
        aggregate.put("type", "aggregate");
        aggregate.put("name", "数据聚合");
        aggregate.put("icon", "AnalyticsOutline");
        aggregate.put("color", "#8a2be2");
        aggregate.put("description", "分组、求和、计数等聚合操作");
        nodeTypes.add(aggregate);
        
        Map<String, Object> script = new HashMap<>();
        script.put("type", "script");
        script.put("name", "脚本处理");
        script.put("icon", "CodeOutline");
        script.put("color", "#d03050");
        script.put("description", "自定义SQL或脚本处理");
        nodeTypes.add(script);
        
        Map<String, Object> sink = new HashMap<>();
        sink.put("type", "sink");
        sink.put("name", "数据输出");
        sink.put("icon", "DownloadOutline");
        sink.put("color", "#36ad6a");
        sink.put("description", "写入数据库、文件或调用API");
        nodeTypes.add(sink);
        
        return Result.success(nodeTypes);
    }
    
    // ==================== 通知测试发送 ====================

    @PostMapping("/notify/test")
    public Result<String> testNotify(@RequestBody Map<String, Object> request) {
        String channel = (String) request.getOrDefault("channel", "");
        @SuppressWarnings("unchecked")
        Map<String, Object> config = (Map<String, Object>) request.getOrDefault("config", new HashMap<>());

        Map<String, String> testContext = new HashMap<>();
        testContext.put("timestamp", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        testContext.put("pipeline.name", "测试流程");
        testContext.put("pipeline.code", "test_pipeline");
        testContext.put("node.name", "测试节点");
        testContext.put("node.type", channel);
        testContext.put("node.status", "success");

        try {
            switch (channel) {
                case "wecom":
                    if (wecomNotifyService == null) return Result.error("企业微信服务未配置");
                    wecomNotifyService.send(config, testContext);
                    return Result.success("企业微信测试消息发送成功");
                case "email":
                    if (emailNotifyService == null) return Result.error("邮件服务未配置");
                    emailNotifyService.send(config, testContext);
                    return Result.success("邮件测试消息发送成功");
                case "sms":
                    if (smsNotifyService == null) return Result.error("短信服务未配置");
                    smsNotifyService.send(config, testContext);
                    return Result.success("短信测试消息发送成功");
                case "dingtalk":
                    if (dingtalkNotifyService == null) return Result.error("钉钉服务未配置");
                    dingtalkNotifyService.send(config, testContext);
                    return Result.success("钉钉测试消息发送成功");
                default:
                    return Result.error("不支持的通知渠道: " + channel);
            }
        } catch (Exception e) {
            log.error("通知测试发送失败: channel={}, error={}", channel, e.getMessage());
            return Result.error("发送失败: " + e.getMessage());
        }
    }

    @Data
    static class DesignRequest {
        private String flowJson;
        private List<PipelineNode> nodes;
    }
}
