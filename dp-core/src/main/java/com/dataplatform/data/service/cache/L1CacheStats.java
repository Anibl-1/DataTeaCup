package com.dataplatform.data.service.cache;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * L1 缓存（Caffeine）统计信息
 * 
 * 包含本地缓存的详细统计数据
 * 
 * 需求引用：
 * - 需求 10.1: 分别统计 L1 缓存的命中率
 * - 需求 10.2: 实时监控缓存内存占用大小
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class L1CacheStats {
    
    /**
     * 命中次数
     */
    private long hitCount;
    
    /**
     * 未命中次数
     */
    private long missCount;
    
    /**
     * 命中率 (0.0 - 1.0)
     */
    private double hitRate;
    
    /**
     * 当前缓存条目数
     */
    private long entryCount;
    
    /**
     * 最大缓存条目数
     */
    private long maxSize;
    
    /**
     * 淘汰次数
     */
    private long evictionCount;
    
    /**
     * 加载成功次数
     */
    private long loadSuccessCount;
    
    /**
     * 加载失败次数
     */
    private long loadFailureCount;
    
    /**
     * 平均加载时间（纳秒）
     */
    private double averageLoadPenalty;
    
    /**
     * 估算内存使用量（字节）
     */
    private long estimatedMemoryUsage;
    
    /**
     * 获取总请求次数
     */
    public long getTotalRequests() {
        return hitCount + missCount;
    }
    
    /**
     * 获取缓存使用率
     */
    public double getUsageRate() {
        if (maxSize <= 0) {
            return 0.0;
        }
        return (double) entryCount / maxSize;
    }
}
