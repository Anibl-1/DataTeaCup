package com.dataplatform.data.service.cache;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * L2 缓存（Redis）统计信息
 * 
 * 包含分布式缓存的详细统计数据
 * 
 * 需求引用：
 * - 需求 10.1: 分别统计 L2 缓存的命中率
 * - 需求 10.2: 实时监控缓存内存占用大小
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class L2CacheStats {
    
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
     * 当前缓存键数量
     */
    private long keyCount;
    
    /**
     * 内存使用量（字节）
     */
    private long memoryUsage;
    
    /**
     * 内存使用量（人类可读格式）
     */
    private String memoryUsageHuman;
    
    /**
     * 最大内存限制（字节）
     */
    private long maxMemory;
    
    /**
     * 连接客户端数
     */
    private int connectedClients;
    
    /**
     * Redis 服务器是否可用
     */
    private boolean available;
    
    /**
     * 过期键数量
     */
    private long expiredKeys;
    
    /**
     * 淘汰键数量
     */
    private long evictedKeys;
    
    /**
     * 获取总请求次数
     */
    public long getTotalRequests() {
        return hitCount + missCount;
    }
    
    /**
     * 获取内存使用率
     */
    public double getMemoryUsageRate() {
        if (maxMemory <= 0) {
            return 0.0;
        }
        return (double) memoryUsage / maxMemory;
    }
}
