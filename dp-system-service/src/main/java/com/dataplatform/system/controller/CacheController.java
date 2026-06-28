package com.dataplatform.system.controller;

import com.dataplatform.common.Result;
import com.dataplatform.common.annotation.RequirePermission;
import com.dataplatform.common.annotation.RequireRole;
import com.dataplatform.data.service.CacheService;
import com.dataplatform.data.service.cache.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 缓存管理接口
 * 
 * 提供缓存状态查看、统计信息、健康检查和管理功能。
 * 
 * 需求引用：
 * - 需求 10.1: 分别统计 L1 和 L2 缓存的命中率
 * - 需求 10.2: 实时监控缓存内存占用大小
 * - 需求 10.3: 关联分析缓存未命中的慢查询
 * - 需求 10.4: 缓存命中率低于70%时触发告警
 * - 需求 10.5: 提供缓存统计信息的 API 接口
 * 
 * @author dataplatform
 */
@Slf4j
@RestController
@RequestMapping("/cache")
@Tag(name = "缓存管理", description = "缓存状态查看、统计和管理")
@RequireRole("admin")
public class CacheController {
    
    @Autowired
    private CacheService cacheService;
    
    @Autowired
    private CacheStatsService cacheStatsService;
    
    @Autowired
    private SlowQueryCacheAnalyzer slowQueryCacheAnalyzer;
    
    @Autowired(required = false)
    private CacheAlertService cacheAlertService;
    
    // ==================== 基础缓存操作 ====================
    
    @RequirePermission("cache:monitor")
    @GetMapping("/status")
    @Operation(summary = "获取缓存状态", description = "查看Redis和本地缓存的运行状态")
    public Result<Map<String, Object>> getStatus() {
        Map<String, Object> stats = cacheService.getStats();
        return Result.success(stats);
    }
    
    @GetMapping("/redis/available")
    @Operation(summary = "检查Redis可用性", description = "测试Redis连接是否正常")
    public Result<Boolean> checkRedisAvailable() {
        boolean available = cacheService.isRedisAvailable();
        return Result.success(available);
    }
    
    @RequirePermission("cache:manage")
    @DeleteMapping("/clear")
    @Operation(summary = "清空本地缓存", description = "清空所有本地内存缓存")
    public Result<Void> clearCache() {
        cacheService.clear();
        log.info("缓存已手动清空");
        return Result.success();
    }
    
    @RequirePermission("cache:manage")
    @DeleteMapping("/key/{key}")
    @Operation(summary = "删除指定缓存", description = "根据key删除缓存")
    public Result<Void> deleteCache(@PathVariable String key) {
        cacheService.delete(key);
        return Result.success();
    }
    
    // ==================== 缓存统计 API（需求 10.5）====================
    
    @RequirePermission("cache:monitor")
    @GetMapping("/stats")
    @Operation(summary = "获取实时缓存统计", description = "获取 L1 和 L2 缓存的实时统计信息，包括命中率、大小等")
    public Result<CacheStats> getRealTimeStats() {
        CacheStats stats = cacheStatsService.getRealTimeStats();
        return Result.success(stats);
    }
    
    @GetMapping("/stats/l1")
    @Operation(summary = "获取 L1 缓存统计", description = "获取本地缓存（Caffeine）的详细统计信息")
    public Result<L1CacheStats> getL1Stats() {
        L1CacheStats stats = cacheStatsService.getL1Stats();
        return Result.success(stats);
    }
    
    @GetMapping("/stats/l2")
    @Operation(summary = "获取 L2 缓存统计", description = "获取分布式缓存（Redis）的详细统计信息")
    public Result<L2CacheStats> getL2Stats() {
        L2CacheStats stats = cacheStatsService.getL2Stats();
        return Result.success(stats);
    }
    
    @GetMapping("/stats/history")
    @Operation(summary = "获取历史统计记录", description = "查询指定时间范围内的缓存统计历史记录")
    public Result<List<CacheStatsRecord>> getHistoryStats(
            @Parameter(description = "开始时间") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @Parameter(description = "缓存级别（L1/L2，可选）") 
            @RequestParam(required = false) String cacheLevel) {
        
        CacheStatsService.CacheLevel level = null;
        if (cacheLevel != null && !cacheLevel.isEmpty()) {
            try {
                level = CacheStatsService.CacheLevel.valueOf(cacheLevel.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("无效的缓存级别: {}", cacheLevel);
            }
        }
        
        List<CacheStatsRecord> records = cacheStatsService.getHistoryStats(startTime, endTime, level);
        return Result.success(records);
    }
    
    @GetMapping("/stats/hit-rate")
    @Operation(summary = "获取平均命中率", description = "获取指定时间范围内的平均缓存命中率")
    public Result<Double> getAverageHitRate(
            @Parameter(description = "开始时间") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @Parameter(description = "缓存级别（L1/L2）") 
            @RequestParam String cacheLevel) {
        
        CacheStatsService.CacheLevel level;
        try {
            level = CacheStatsService.CacheLevel.valueOf(cacheLevel.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Result.error("无效的缓存级别: " + cacheLevel);
        }
        
        double hitRate = cacheStatsService.getAverageHitRate(startTime, endTime, level);
        return Result.success(hitRate);
    }
    
    @GetMapping("/health")
    @Operation(summary = "获取缓存健康状态", description = "基于命中率和内存使用情况评估缓存系统健康状态")
    public Result<CacheHealthStatus> getHealthStatus() {
        CacheHealthStatus status = cacheStatsService.getHealthStatus();
        return Result.success(status);
    }
    
    @PostMapping("/stats/reset")
    @Operation(summary = "重置统计计数器", description = "清空当前的命中/未命中计数，重新开始统计")
    public Result<Void> resetStats() {
        cacheStatsService.resetStats();
        log.info("缓存统计计数器已重置");
        return Result.success();
    }
    
    // ==================== 慢查询缓存关联分析（需求 10.3）====================
    
    @GetMapping("/slow-query/reports")
    @Operation(summary = "获取慢查询缓存关联报告", description = "获取指定时间范围内的慢查询及其缓存关联信息")
    public Result<List<SlowQueryCacheReport>> getSlowQueryCacheReports(
            @Parameter(description = "开始时间") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @Parameter(description = "慢查询阈值（毫秒），默认3000") 
            @RequestParam(defaultValue = "3000") long thresholdMs) {
        
        List<SlowQueryCacheReport> reports = slowQueryCacheAnalyzer.getSlowQueryCacheReports(
                startTime, endTime, thresholdMs);
        return Result.success(reports);
    }
    
    @GetMapping("/slow-query/cache-miss")
    @Operation(summary = "获取缓存未命中的慢查询", description = "只返回缓存未命中的慢查询，用于重点分析")
    public Result<List<SlowQueryCacheReport>> getCacheMissSlowQueries(
            @Parameter(description = "开始时间") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @Parameter(description = "慢查询阈值（毫秒），默认3000") 
            @RequestParam(defaultValue = "3000") long thresholdMs) {
        
        List<SlowQueryCacheReport> reports = slowQueryCacheAnalyzer.getCacheMissSlowQueries(
                startTime, endTime, thresholdMs);
        return Result.success(reports);
    }
    
    @GetMapping("/slow-query/summary")
    @Operation(summary = "获取慢查询缓存统计摘要", description = "获取慢查询与缓存关联的统计概览信息")
    public Result<SlowQueryCacheSummary> getSlowQueryCacheSummary(
            @Parameter(description = "开始时间") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        
        SlowQueryCacheSummary summary = slowQueryCacheAnalyzer.getSummary(startTime, endTime);
        return Result.success(summary);
    }
    
    @GetMapping("/slow-query/history/{cacheKey}")
    @Operation(summary = "获取缓存键的查询历史", description = "根据缓存键获取查询的缓存状态历史")
    public Result<List<QueryCacheStatusRecord>> getCacheStatusHistory(
            @Parameter(description = "缓存键") 
            @PathVariable String cacheKey,
            @Parameter(description = "返回记录数限制，默认20") 
            @RequestParam(defaultValue = "20") int limit) {
        
        List<QueryCacheStatusRecord> records = slowQueryCacheAnalyzer.getCacheStatusHistory(cacheKey, limit);
        return Result.success(records);
    }
    
    @DeleteMapping("/slow-query/cleanup")
    @Operation(summary = "清理过期的缓存状态记录", description = "清理指定天数前的缓存状态记录")
    public Result<Integer> cleanExpiredRecords(
            @Parameter(description = "保留天数，默认7天") 
            @RequestParam(defaultValue = "7") int retentionDays) {
        
        int deleted = slowQueryCacheAnalyzer.cleanExpiredRecords(retentionDays);
        log.info("清理过期缓存状态记录: {} 条", deleted);
        return Result.success(deleted);
    }
    
    // ==================== 缓存告警（需求 10.4）====================
    
    @GetMapping("/alerts")
    @Operation(summary = "获取缓存告警列表", description = "获取最近的缓存告警记录")
    public Result<List<CacheAlert>> getAlerts(
            @Parameter(description = "返回记录数限制，默认50") 
            @RequestParam(defaultValue = "50") int limit) {
        
        if (cacheAlertService == null) {
            return Result.error("告警服务不可用");
        }
        
        List<CacheAlert> alerts = cacheAlertService.getRecentAlerts(limit);
        return Result.success(alerts);
    }
    
    @PostMapping("/alerts/check")
    @Operation(summary = "手动触发告警检查", description = "立即检查缓存命中率并触发告警（如果需要）")
    public Result<List<CacheAlert>> triggerAlertCheck() {
        if (cacheAlertService == null) {
            return Result.error("告警服务不可用");
        }
        
        List<CacheAlert> alerts = cacheAlertService.checkAndAlert();
        return Result.success(alerts);
    }
    
    @DeleteMapping("/alerts/clear")
    @Operation(summary = "清除告警历史", description = "清除所有告警历史记录")
    public Result<Void> clearAlertHistory() {
        if (cacheAlertService == null) {
            return Result.error("告警服务不可用");
        }
        
        cacheAlertService.clearAlertHistory();
        return Result.success();
    }
    
    @GetMapping("/alerts/enabled")
    @Operation(summary = "检查告警是否启用", description = "检查缓存告警功能是否启用")
    public Result<Boolean> isAlertEnabled() {
        if (cacheAlertService == null) {
            return Result.error("告警服务不可用");
        }
        
        return Result.success(cacheAlertService.isEnabled());
    }
    
    @PostMapping("/alerts/enabled")
    @Operation(summary = "启用或禁用告警", description = "启用或禁用缓存告警功能")
    public Result<Void> setAlertEnabled(
            @Parameter(description = "是否启用") 
            @RequestParam boolean enabled) {
        
        if (cacheAlertService == null) {
            return Result.error("告警服务不可用");
        }
        
        cacheAlertService.setEnabled(enabled);
        return Result.success();
    }
}
