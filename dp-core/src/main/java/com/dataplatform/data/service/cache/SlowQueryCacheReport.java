package com.dataplatform.data.service.cache;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 慢查询缓存关联报告
 * 
 * 包含慢查询的详细信息及其缓存命中状态，用于分析缓存未命中的慢查询。
 * 
 * 需求引用：
 * - 需求 10.3: 关联分析缓存未命中的慢查询
 * 
 * 分析报告应包含：
 * - 慢查询 SQL
 * - 执行时间
 * - 缓存命中状态（L1 命中、L2 命中、未命中）
 * - 缓存键
 * - 建议（如：建议预热此查询）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlowQueryCacheReport {
    
    /**
     * 记录 ID
     */
    private Long id;
    
    /**
     * SQL 语句
     */
    private String sql;
    
    /**
     * SQL 哈希值（用于去重和关联）
     */
    private String sqlHash;
    
    /**
     * 执行时间（毫秒）
     */
    private long executionTime;
    
    /**
     * 缓存命中状态
     */
    private SlowQueryCacheAnalyzer.CacheHitStatus cacheStatus;
    
    /**
     * 缓存键
     */
    private String cacheKey;
    
    /**
     * 数据源 ID
     */
    private Long dataSourceId;
    
    /**
     * 数据源名称
     */
    private String dataSourceName;
    
    /**
     * 查询时间
     */
    private LocalDateTime queryTime;
    
    /**
     * 用户名
     */
    private String userName;
    
    /**
     * 出现次数（相同 SQL 的聚合统计）
     */
    private int occurrenceCount;
    
    /**
     * 平均执行时间（毫秒）
     */
    private double avgExecutionTime;
    
    /**
     * 最大执行时间（毫秒）
     */
    private long maxExecutionTime;
    
    /**
     * 缓存未命中次数
     */
    private int cacheMissCount;
    
    /**
     * 缓存命中次数
     */
    private int cacheHitCount;
    
    /**
     * 优化建议列表
     */
    @Builder.Default
    private List<String> suggestions = new ArrayList<>();
    
    /**
     * 是否为慢查询
     */
    private boolean isSlowQuery;
    
    /**
     * 是否缓存未命中
     */
    public boolean isCacheMiss() {
        return cacheStatus == SlowQueryCacheAnalyzer.CacheHitStatus.MISS;
    }
    
    /**
     * 是否 L1 命中
     */
    public boolean isL1Hit() {
        return cacheStatus == SlowQueryCacheAnalyzer.CacheHitStatus.L1_HIT;
    }
    
    /**
     * 是否 L2 命中
     */
    public boolean isL2Hit() {
        return cacheStatus == SlowQueryCacheAnalyzer.CacheHitStatus.L2_HIT;
    }
    
    /**
     * 获取缓存状态描述
     */
    public String getCacheStatusDescription() {
        return cacheStatus != null ? cacheStatus.getDescription() : "未知";
    }
    
    /**
     * 添加优化建议
     */
    public void addSuggestion(String suggestion) {
        if (suggestions == null) {
            suggestions = new ArrayList<>();
        }
        if (!suggestions.contains(suggestion)) {
            suggestions.add(suggestion);
        }
    }
    
    /**
     * 获取截断的 SQL（用于显示）
     */
    public String getTruncatedSql(int maxLength) {
        if (sql == null) {
            return "";
        }
        if (sql.length() <= maxLength) {
            return sql;
        }
        return sql.substring(0, maxLength) + "...";
    }
}
