package com.dataplatform.data.service.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 仅本地缓存管理器实现
 * 
 * 当 Redis 不可用时的备用实现，仅使用 L1（Caffeine）缓存
 * 
 * 特性：
 * - 仅使用本地 Caffeine 缓存
 * - 适用于单机部署或 Redis 不可用的场景
 * - L2 相关操作为空实现
 * 
 * 需求引用：
 * - 需求 7.7: 缓存键生成（包含 RLS 信息）
 */
@Slf4j
@Service
@ConditionalOnMissingBean(RedisTemplate.class)
public class LocalOnlyCacheManager implements MultiLevelCacheManager {
    
    /**
     * 缓存键前缀
     */
    private static final String CACHE_KEY_PREFIX = "dp:cache:";
    
    /**
     * L1 本地缓存（Caffeine）
     */
    private final Cache<String, Object> l1Cache;
    
    /**
     * JSON 序列化器
     */
    private final ObjectMapper objectMapper;
    
    /**
     * 缓存键生成器
     */
    private final CacheKeyGenerator cacheKeyGenerator;
    
    @Autowired
    public LocalOnlyCacheManager(
            @Qualifier("l1Cache") Cache<String, Object> l1Cache,
            ObjectMapper objectMapper,
            CacheKeyGenerator cacheKeyGenerator) {
        this.l1Cache = l1Cache;
        this.objectMapper = objectMapper;
        this.cacheKeyGenerator = cacheKeyGenerator;
        log.info("本地缓存管理器初始化完成 - 仅使用 L1 Caffeine 缓存（Redis 不可用）");
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        String cacheKey = buildCacheKey(key);
        
        Object value = l1Cache.getIfPresent(cacheKey);
        if (value != null) {
            log.debug("L1 缓存命中 - key: {}", key);
            return convertValue(value, type);
        }
        
        log.debug("缓存未命中 - key: {}", key);
        return null;
    }
    
    @Override
    public void put(String key, Object value, long ttl, TimeUnit unit) {
        if (value == null) {
            log.debug("跳过空值缓存 - key: {}", key);
            return;
        }
        
        String cacheKey = buildCacheKey(key);
        l1Cache.put(cacheKey, value);
        log.debug("写入 L1 缓存 - key: {}", key);
    }
    
    @Override
    public void put(String key, Object value) {
        if (value == null) {
            log.debug("跳过空值缓存 - key: {}", key);
            return;
        }
        
        String cacheKey = buildCacheKey(key);
        l1Cache.put(cacheKey, value);
        log.debug("写入 L1 缓存 - key: {}", key);
    }
    
    @Override
    public void evict(String key) {
        String cacheKey = buildCacheKey(key);
        l1Cache.invalidate(cacheKey);
        log.debug("从 L1 缓存删除 - key: {}", key);
    }
    
    @Override
    public void evictByPattern(String pattern) {
        String cacheKeyPattern = buildCacheKey(pattern);
        
        l1Cache.asMap().keySet().stream()
                .filter(k -> matchPattern(k, cacheKeyPattern))
                .forEach(l1Cache::invalidate);
        log.debug("从 L1 缓存按模式删除 - pattern: {}", pattern);
    }
    
    @Override
    public void clear() {
        l1Cache.invalidateAll();
        log.info("L1 缓存已清空");
    }
    
    @Override
    public CacheStats getStats() {
        com.github.benmanes.caffeine.cache.stats.CacheStats caffeineStats = l1Cache.stats();
        
        return CacheStats.builder()
                .l1HitCount(caffeineStats.hitCount())
                .l1MissCount(caffeineStats.missCount())
                .l1HitRate(caffeineStats.hitRate())
                .l1Size(l1Cache.estimatedSize())
                .l2HitCount(0)
                .l2MissCount(0)
                .l2HitRate(0.0)
                .l2MemoryUsage(0)
                .build();
    }
    
    @Override
    public String generateCacheKey(String sql, Map<String, Object> params, Long dataSourceId, Long userId, List<Long> roleIds) {
        // 委托给 CacheKeyGenerator 生成缓存键
        // 确保 RLS（行级权限）正确应用
        return cacheKeyGenerator.generate(sql, params, dataSourceId, userId, roleIds);
    }
    
    @Override
    public boolean exists(String key) {
        String cacheKey = buildCacheKey(key);
        return l1Cache.getIfPresent(cacheKey) != null;
    }
    
    @Override
    public long getL1Size() {
        return l1Cache.estimatedSize();
    }
    
    @Override
    public long getL2Size() {
        return 0; // Redis 不可用
    }
    
    // ==================== 私有辅助方法 ====================
    
    private String buildCacheKey(String key) {
        return CACHE_KEY_PREFIX + key;
    }
    
    @SuppressWarnings("unchecked")
    private <T> T convertValue(Object value, Class<T> type) {
        if (value == null) {
            return null;
        }
        
        if (type.isInstance(value)) {
            return (T) value;
        }
        
        try {
            return objectMapper.convertValue(value, type);
        } catch (Exception e) {
            log.warn("缓存值类型转换失败 - targetType: {}, error: {}", type.getName(), e.getMessage());
            return null;
        }
    }
    
    private boolean matchPattern(String key, String pattern) {
        String regex = pattern
                .replace(".", "\\.")
                .replace("*", ".*");
        return key.matches(regex);
    }
}
