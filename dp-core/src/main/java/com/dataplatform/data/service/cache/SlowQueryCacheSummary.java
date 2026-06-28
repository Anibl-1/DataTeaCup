package com.dataplatform.data.service.cache;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 慢查询缓存统计摘要
 * 
 * 提供慢查询与缓存关联的统计概览信息。
 * 
 * 需求引用：
 * - 需求 10.3: 关联分析缓存未命中的慢查询
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlowQueryCacheSummary {
    
    /**
     * 统计开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 统计结束时间
     */
    private LocalDateTime endTime;
    
    /**
     * 慢查询总数
     */
    private long totalSlowQueries;
    
    /**
     * 缓存未命中的慢查询数
     */
    private long cacheMissSlowQueries;
    
    /**
     * L1 命中的慢查询数
     */
    private long l1HitSlowQueries;
    
    /**
     * L2 命中的慢查询数
     */
    private long l2HitSlowQueries;
    
    /**
     * 缓存未命中的慢查询占比
     */
    private double cacheMissRate;
    
    /**
     * 平均执行时间（毫秒）
     */
    private double avgExecutionTime;
    
    /**
     * 缓存未命中慢查询的平均执行时间（毫秒）
     */
    private double avgCacheMissExecutionTime;
    
    /**
     * 缓存命中慢查询的平均执行时间（毫秒）
     */
    private double avgCacheHitExecutionTime;
    
    /**
     * 最慢查询执行时间（毫秒）
     */
    private long maxExecutionTime;
    
    /**
     * 唯一 SQL 数量
     */
    private long uniqueSqlCount;
    
    /**
     * 建议预热的查询数量
     */
    private long suggestedWarmUpCount;
    
    /**
     * 计算缓存未命中率
     */
    public double calculateCacheMissRate() {
        if (totalSlowQueries == 0) {
            return 0.0;
        }
        return (double) cacheMissSlowQueries / totalSlowQueries;
    }
    
    /**
     * 计算缓存命中带来的性能提升
     * 
     * @return 性能提升百分比（如果缓存命中比未命中快）
     */
    public double calculateCachePerformanceGain() {
        if (avgCacheMissExecutionTime == 0) {
            return 0.0;
        }
        return (avgCacheMissExecutionTime - avgCacheHitExecutionTime) / avgCacheMissExecutionTime * 100;
    }
    
    /**
     * 获取健康状态描述
     */
    public String getHealthDescription() {
        if (cacheMissRate > 0.5) {
            return "警告：超过50%的慢查询缓存未命中，建议优化缓存策略";
        } else if (cacheMissRate > 0.3) {
            return "注意：约30%的慢查询缓存未命中，可考虑增加缓存预热";
        } else {
            return "良好：大部分慢查询已被缓存覆盖";
        }
    }
}
