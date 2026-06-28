package com.dataplatform.data.service.cache;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 多级缓存管理器接口
 * 
 * 实现两级缓存架构：
 * - L1: 本地缓存（Caffeine），最大10000条，TTL 5分钟，LRU淘汰
 * - L2: 分布式缓存（Redis），TTL 30分钟
 * 
 * 查询链：L1 -> L2 -> DB（由调用方处理）
 * 写入策略：同时写入 L1 和 L2
 * 
 * 需求引用：
 * - 需求 7.1: 多级缓存架构
 * - 需求 7.8: L1 -> L2 -> DB 查询链
 * - 需求 7.9: 同时写入 L1 和 L2
 * 
 * @see CacheStats 缓存统计信息
 */
public interface MultiLevelCacheManager {
    
    /**
     * 从缓存获取数据
     * 
     * 查询链：先查 L1 缓存，未命中则查 L2 缓存，仍未命中返回 null
     * 如果 L2 命中，会自动回填到 L1 缓存
     * 
     * @param key  缓存键
     * @param type 返回值类型
     * @param <T>  泛型类型
     * @return 缓存值，未命中返回 null
     */
    <T> T get(String key, Class<T> type);
    
    /**
     * 写入缓存
     * 
     * 同时写入 L1 和 L2 缓存
     * 
     * @param key   缓存键
     * @param value 缓存值
     * @param ttl   过期时间
     * @param unit  时间单位
     */
    void put(String key, Object value, long ttl, TimeUnit unit);
    
    /**
     * 写入缓存（使用默认 TTL）
     * 
     * L1 使用 5 分钟 TTL，L2 使用 30 分钟 TTL
     * 
     * @param key   缓存键
     * @param value 缓存值
     */
    void put(String key, Object value);
    
    /**
     * 删除缓存
     * 
     * 同时从 L1 和 L2 删除
     * 
     * @param key 缓存键
     */
    void evict(String key);
    
    /**
     * 按模式删除缓存
     * 
     * 支持通配符模式匹配，同时从 L1 和 L2 删除匹配的缓存
     * 
     * @param pattern 键模式（支持 * 通配符）
     */
    void evictByPattern(String pattern);
    
    /**
     * 清空所有缓存
     * 
     * 清空 L1 和 L2 中的所有缓存数据
     */
    void clear();
    
    /**
     * 获取缓存统计信息
     * 
     * 返回 L1 和 L2 缓存的命中率、大小等统计信息
     * 
     * @return 缓存统计信息
     */
    CacheStats getStats();
    
    /**
     * 生成缓存键
     * 
     * 基于 SQL、参数、数据源ID、用户ID 和角色列表生成唯一缓存键
     * 确保 RLS（行级权限）正确应用
     * 
     * @param sql          SQL 语句
     * @param params       查询参数
     * @param dataSourceId 数据源 ID
     * @param userId       用户 ID
     * @param roleIds      角色 ID 列表
     * @return 缓存键
     */
    String generateCacheKey(String sql, Map<String, Object> params, Long dataSourceId, Long userId, List<Long> roleIds);
    
    /**
     * 检查缓存是否存在
     * 
     * @param key 缓存键
     * @return 如果缓存存在返回 true
     */
    boolean exists(String key);
    
    /**
     * 获取 L1 缓存大小
     * 
     * @return L1 缓存当前条目数
     */
    long getL1Size();
    
    /**
     * 获取 L2 缓存大小（估算）
     * 
     * @return L2 缓存键数量
     */
    long getL2Size();
}
