package com.dataplatform.data.service.usage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 使用数据采集器
 * 自动采集功能使用数据
 * 需求: 30.1
 */
@Component
public class UsageTracker {

    private static final Logger log = LoggerFactory.getLogger(UsageTracker.class);

    private final UsageStatsService usageStatsService;

    public UsageTracker(UsageStatsService usageStatsService) {
        this.usageStatsService = usageStatsService;
    }

    /**
     * 记录功能访问
     */
    public void trackFeatureAccess(String userId, String featureCode, String action,
                                    String resourceType, String resourceId, String clientType) {
        UsageStats stats = new UsageStats();
        stats.setUserId(userId);
        stats.setFeatureCode(featureCode);
        stats.setAction(action);
        stats.setResourceType(resourceType);
        stats.setResourceId(resourceId);
        stats.setClientType(clientType);
        stats.setCreateTime(LocalDateTime.now());
        usageStatsService.recordUsage(stats);
    }

    /**
     * 记录查询操作（含耗时和结果数）
     */
    public void trackQuery(String userId, String resourceType, String resourceId,
                           long durationMs, int resultCount, String errorCode) {
        UsageStats stats = new UsageStats();
        stats.setUserId(userId);
        stats.setFeatureCode(resourceType);
        stats.setAction("query");
        stats.setResourceType(resourceType);
        stats.setResourceId(resourceId);
        stats.setDuration(durationMs);
        stats.setResultCount(resultCount);
        stats.setErrorCode(errorCode);
        stats.setCreateTime(LocalDateTime.now());
        usageStatsService.recordUsage(stats);
    }

    /**
     * 记录错误事件
     */
    public void trackError(String userId, String featureCode, String errorCode) {
        UsageStats stats = new UsageStats();
        stats.setUserId(userId);
        stats.setFeatureCode(featureCode);
        stats.setAction("error");
        stats.setErrorCode(errorCode);
        stats.setCreateTime(LocalDateTime.now());
        usageStatsService.recordUsage(stats);
    }
}
