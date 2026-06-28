package com.dataplatform.data.service.cache;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 慢查询缓存关联分析器接口
 * 
 * 关联分析缓存未命中的慢查询，生成包含缓存关联信息的分析报告。
 * 
 * 需求引用：
 * - 需求 10.3: 关联分析缓存未命中的慢查询
 * 
 * 属性 32: 慢查询缓存关联
 * 对于任意缓存未命中的慢查询，应在分析报告中关联显示缓存未命中信息。
 * 
 * @see CacheStatsService 缓存统计服务
 * @see com.dataplatform.service.SlowQueryService 慢查询服务
 */
public interface SlowQueryCacheAnalyzer {
    
    /**
     * 记录慢查询的缓存状态
     * 
     * 当执行查询时，记录该查询的缓存命中状态信息
     * 
     * @param cacheKey    缓存键
     * @param sql         SQL 语句
     * @param cacheStatus 缓存命中状态
     * @param executionTime 执行时间（毫秒）
     * @param dataSourceId 数据源 ID
     */
    void recordQueryCacheStatus(String cacheKey, String sql, CacheHitStatus cacheStatus, 
                                 long executionTime, Long dataSourceId);
    
    /**
     * 获取慢查询缓存关联分析报告
     * 
     * 返回指定时间范围内的慢查询及其缓存关联信息
     * 
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param slowQueryThresholdMs 慢查询阈值（毫秒），默认 3000ms
     * @return 慢查询缓存关联报告列表
     */
    List<SlowQueryCacheReport> getSlowQueryCacheReports(LocalDateTime startTime, 
                                                         LocalDateTime endTime,
                                                         long slowQueryThresholdMs);
    
    /**
     * 获取慢查询缓存关联分析报告（使用默认阈值 3000ms）
     * 
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 慢查询缓存关联报告列表
     */
    default List<SlowQueryCacheReport> getSlowQueryCacheReports(LocalDateTime startTime, 
                                                                  LocalDateTime endTime) {
        return getSlowQueryCacheReports(startTime, endTime, 3000L);
    }
    
    /**
     * 获取缓存未命中的慢查询列表
     * 
     * 只返回缓存未命中的慢查询，用于重点分析
     * 
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param slowQueryThresholdMs 慢查询阈值（毫秒）
     * @return 缓存未命中的慢查询报告列表
     */
    List<SlowQueryCacheReport> getCacheMissSlowQueries(LocalDateTime startTime, 
                                                        LocalDateTime endTime,
                                                        long slowQueryThresholdMs);
    
    /**
     * 获取慢查询缓存统计摘要
     * 
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 统计摘要
     */
    SlowQueryCacheSummary getSummary(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 根据缓存键获取查询的缓存状态历史
     * 
     * @param cacheKey 缓存键
     * @param limit    返回记录数限制
     * @return 缓存状态历史列表
     */
    List<QueryCacheStatusRecord> getCacheStatusHistory(String cacheKey, int limit);
    
    /**
     * 清理过期的缓存状态记录
     * 
     * @param retentionDays 保留天数
     * @return 清理的记录数
     */
    int cleanExpiredRecords(int retentionDays);
    
    /**
     * 缓存命中状态枚举
     */
    enum CacheHitStatus {
        /**
         * L1 缓存命中
         */
        L1_HIT("L1_HIT", "L1 命中"),
        
        /**
         * L2 缓存命中
         */
        L2_HIT("L2_HIT", "L2 命中"),
        
        /**
         * 缓存未命中（需要查询数据库）
         */
        MISS("MISS", "未命中"),
        
        /**
         * 缓存被跳过（如布隆过滤器拦截）
         */
        SKIPPED("SKIPPED", "已跳过");
        
        private final String code;
        private final String description;
        
        CacheHitStatus(String code, String description) {
            this.code = code;
            this.description = description;
        }
        
        public String getCode() {
            return code;
        }
        
        public String getDescription() {
            return description;
        }
        
        public static CacheHitStatus fromCode(String code) {
            for (CacheHitStatus status : values()) {
                if (status.code.equals(code)) {
                    return status;
                }
            }
            return MISS;
        }
    }
}
