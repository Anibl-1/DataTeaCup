package com.dataplatform.data.service.cache;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 缓存统计记录
 * 
 * 用于持久化和查询历史统计数据
 * 
 * 对应数据库表：cache_stats
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheStatsRecord {
    
    /**
     * 记录 ID
     */
    private Long id;
    
    /**
     * 统计时间
     */
    private LocalDateTime statTime;
    
    /**
     * 缓存级别：L1/L2
     */
    private String cacheLevel;
    
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
     * 内存使用量（字节）
     */
    private long memoryUsage;
    
    /**
     * 缓存条目数
     */
    private int entryCount;
    
    /**
     * 获取总请求次数
     */
    public long getTotalRequests() {
        return hitCount + missCount;
    }
}
