package com.dataplatform.data.service.cache;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 查询缓存状态记录
 * 
 * 记录每次查询的缓存命中状态，用于关联分析。
 * 
 * 需求引用：
 * - 需求 10.3: 关联分析缓存未命中的慢查询
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryCacheStatusRecord {
    
    /**
     * 记录 ID
     */
    private Long id;
    
    /**
     * 缓存键
     */
    private String cacheKey;
    
    /**
     * SQL 语句
     */
    private String sql;
    
    /**
     * SQL 哈希值
     */
    private String sqlHash;
    
    /**
     * 缓存命中状态
     */
    private SlowQueryCacheAnalyzer.CacheHitStatus cacheStatus;
    
    /**
     * 执行时间（毫秒）
     */
    private long executionTime;
    
    /**
     * 数据源 ID
     */
    private Long dataSourceId;
    
    /**
     * 是否为慢查询
     */
    private boolean slowQuery;
    
    /**
     * 记录时间
     */
    private LocalDateTime recordTime;
    
    /**
     * 获取缓存状态代码
     */
    public String getCacheStatusCode() {
        return cacheStatus != null ? cacheStatus.getCode() : "UNKNOWN";
    }
    
    /**
     * 判断是否缓存未命中
     */
    public boolean isCacheMiss() {
        return cacheStatus == SlowQueryCacheAnalyzer.CacheHitStatus.MISS;
    }
}
