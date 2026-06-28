package com.dataplatform.system.controller;

import com.dataplatform.common.annotation.RequirePermission;
import com.dataplatform.common.Result;
import com.dataplatform.system.dto.OperationLogQueryDTO;
import com.dataplatform.system.entity.OperationLog;
import com.dataplatform.system.service.OperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/operation-log")
@RequirePermission("log:operation")
public class OperationLogController {
    
    @Autowired
    private OperationLogService operationLogService;
    
    /**
     * 查询操作日志列表（增强版）
     * 支持按操作类型、操作人、时间范围、模块名称组合筛选
     */
    @GetMapping("/list")
    public Result<Map<String, Object>> list(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String moduleName,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date endTime,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        OperationLogQueryDTO query = new OperationLogQueryDTO();
        query.setUserId(userId);
        query.setUsername(username);
        query.setModuleName(moduleName);
        query.setOperationType(operationType);
        query.setStatus(status);
        query.setStartTime(startTime);
        query.setEndTime(endTime);
        query.setKeyword(keyword);
        query.setPage(page);
        query.setPageSize(pageSize);
        Map<String, Object> result = operationLogService.queryByCondition(query);
        return Result.success(result);
    }
    
    @GetMapping("/detail/{id}")
    public Result<OperationLog> detail(@PathVariable Long id) {
        OperationLog log = operationLogService.getById(id);
        return Result.success(log);
    }
    
    @GetMapping("/stats/overview")
    public Result<Map<String, Object>> statsOverview(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date endTime) {
        if (startTime == null || endTime == null) {
            endTime = new Date();
            startTime = new Date(endTime.getTime() - 7 * 24 * 60 * 60 * 1000L);
        }
        Map<String, Object> stats = operationLogService.getStatsOverview(startTime, endTime);
        return Result.success(stats);
    }
    
    @GetMapping("/stats/by-user")
    public Result<List<Map<String, Object>>> statsByUser(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date endTime) {
        if (startTime == null || endTime == null) {
            endTime = new Date();
            startTime = new Date(endTime.getTime() - 7 * 24 * 60 * 60 * 1000L);
        }
        List<Map<String, Object>> stats = operationLogService.getStatsByUser(startTime, endTime);
        return Result.success(stats);
    }
    
    @GetMapping("/stats/by-module")
    public Result<List<Map<String, Object>>> statsByModule(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date endTime) {
        if (startTime == null || endTime == null) {
            endTime = new Date();
            startTime = new Date(endTime.getTime() - 7 * 24 * 60 * 60 * 1000L);
        }
        List<Map<String, Object>> stats = operationLogService.getStatsByModule(startTime, endTime);
        return Result.success(stats);
    }
    
    @GetMapping("/stats/by-operation")
    public Result<List<Map<String, Object>>> statsByOperation(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date endTime) {
        if (startTime == null || endTime == null) {
            endTime = new Date();
            startTime = new Date(endTime.getTime() - 7 * 24 * 60 * 60 * 1000L);
        }
        List<Map<String, Object>> stats = operationLogService.getStatsByOperation(startTime, endTime);
        return Result.success(stats);
    }
    
    @GetMapping("/stats/trend")
    public Result<List<Map<String, Object>>> trend(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date endTime,
            @RequestParam(defaultValue = "day") String groupBy) {
        if (startTime == null || endTime == null) {
            endTime = new Date();
            startTime = new Date(endTime.getTime() - 7 * 24 * 60 * 60 * 1000L);
        }
        List<Map<String, Object>> trend = operationLogService.getTrend(startTime, endTime, groupBy);
        return Result.success(trend);
    }
    
    @RequirePermission("log:operation")
    @DeleteMapping("/clean")
    public Result<Integer> clean(@RequestParam(defaultValue = "90") int days) {
        int count = operationLogService.cleanHistory(days);
        return Result.success(count);
    }
}
