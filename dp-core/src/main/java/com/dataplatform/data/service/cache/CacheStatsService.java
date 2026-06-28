package com.dataplatform.data.service.cache;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 缓存统计服务接口
 * 
 * 提供 L1（Caffeine）和 L2（Redis）缓存的统计功能：
 * - 实时统计：获取当前缓存命中率、大小等信息
 * - 历史统计：查询历史统计记录
 * - 统计持久化：定期保存统计数据到数据库
 * 
 * 需求引用：
 * - 需求 10.1: 分别统计 L1 和 L2 缓存的命中率
 * - 需求 10.2: 实时监控缓存内存占用大小
 * 
 * @see CacheStats 缓存统计数据结构
 * @see MultiLevelCacheManager 多级缓存管理器
 */
public interface CacheStatsService {
    
    /**
     * 获取实时缓存统计信息
     * 
     * 返回当前 L1 和 L2 缓存的命中率、大小等统计信息
     * 
     * @return 当前缓存统计信息
     */
    CacheStats getRealTimeStats();
    
    /**
     * 获取 L1 缓存统计信息
     * 
     * @return L1 缓存统计信息
     */
    L1CacheStats getL1Stats();
    
    /**
     * 获取 L2 缓存统计信息
     * 
     * @return L2 缓存统计信息
     */
    L2CacheStats getL2Stats();
    
    /**
     * 记录缓存命中
     * 
     * @param cacheLevel 缓存级别（L1 或 L2）
     */
    void recordHit(CacheLevel cacheLevel);
    
    /**
     * 记录缓存未命中
     * 
     * @param cacheLevel 缓存级别（L1 或 L2）
     */
    void recordMiss(CacheLevel cacheLevel);
    
    /**
     * 保存当前统计数据到数据库
     * 
     * 用于定期持久化统计数据，支持历史查询
     */
    void persistStats();
    
    /**
     * 查询历史统计记录
     * 
     * @param startTime  开始时间
     * @param endTime    结束时间
     * @param cacheLevel 缓存级别（可选，null 表示查询所有级别）
     * @return 历史统计记录列表
     */
    List<CacheStatsRecord> getHistoryStats(LocalDateTime startTime, LocalDateTime endTime, CacheLevel cacheLevel);
    
    /**
     * 获取指定时间段的平均命中率
     * 
     * @param startTime  开始时间
     * @param endTime    结束时间
     * @param cacheLevel 缓存级别
     * @return 平均命中率（0.0 - 1.0）
     */
    double getAverageHitRate(LocalDateTime startTime, LocalDateTime endTime, CacheLevel cacheLevel);
    
    /**
     * 重置统计计数器
     * 
     * 清空当前的命中/未命中计数，重新开始统计
     */
    void resetStats();
    
    /**
     * 获取缓存健康状态
     * 
     * 基于命中率和内存使用情况评估缓存健康状态
     * 
     * @return 缓存健康状态
     */
    CacheHealthStatus getHealthStatus();
    
    /**
     * 缓存级别枚举
     */
    enum CacheLevel {
        L1("L1", "本地缓存"),
        L2("L2", "分布式缓存");
        
        private final String code;
        private final String description;
        
        CacheLevel(String code, String description) {
            this.code = code;
            this.description = description;
        }
        
        public String getCode() {
            return code;
        }
        
        public String getDescription() {
            return description;
        }
    }
}
