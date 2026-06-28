package com.dataplatform.data.service.cache;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 缓存统计信息
 * 
 * 用于记录 L1（Caffeine）和 L2（Redis）缓存的命中率和使用情况
 * 
 * @see MultiLevelCacheManager#getStats()
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheStats {
    
    /**
     * L1 缓存命中次数
     */
    private long l1HitCount;
    
    /**
     * L1 缓存未命中次数
     */
    private long l1MissCount;
    
    /**
     * L1 缓存命中率 (0.0 - 1.0)
     */
    private double l1HitRate;
    
    /**
     * L1 缓存当前条目数
     */
    private long l1Size;
    
    /**
     * L2 缓存命中次数
     */
    private long l2HitCount;
    
    /**
     * L2 缓存未命中次数
     */
    private long l2MissCount;
    
    /**
     * L2 缓存命中率 (0.0 - 1.0)
     */
    private double l2HitRate;
    
    /**
     * L2 缓存内存占用（字节）
     */
    private long l2MemoryUsage;
    
    /**
     * 总命中次数（L1 + L2）
     */
    public long getTotalHitCount() {
        return l1HitCount + l2HitCount;
    }
    
    /**
     * 总未命中次数
     */
    public long getTotalMissCount() {
        return l1MissCount + l2MissCount;
    }
    
    /**
     * 总体命中率
     */
    public double getTotalHitRate() {
        long totalRequests = getTotalHitCount() + l2MissCount; // L1 miss 会查 L2，所以只算 L2 miss
        if (totalRequests == 0) {
            return 0.0;
        }
        return (double) getTotalHitCount() / totalRequests;
    }
}
