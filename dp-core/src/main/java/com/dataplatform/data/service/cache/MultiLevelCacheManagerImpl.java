package com.dataplatform.data.service.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 多级缓存管理器实现
 * 
 * 实现 L1（Caffeine）-> L2（Redis）-> DB 的查询链
 * 
 * 特性：
 * - L1 缓存：本地 Caffeine 缓存，最大 10000 条，TTL 5 分钟
 * - L2 缓存：分布式 Redis 缓存，TTL 30 分钟
 * - 查询时先查 L1，未命中查 L2，L2 命中后回填 L1
 * - 写入时同时写入 L1 和 L2
 * - 删除时同时从 L1 和 L2 删除
 * 
 * 需求引用：
 * - 需求 7.1: 多级缓存架构
 * - 需求 7.7: 缓存键生成（包含 RLS 信息）
 * - 需求 7.8: L1 -> L2 -> DB 查询链
 * - 需求 7.9: 同时写入 L1 和 L2
 */
@Slf4j
@Service
@ConditionalOnBean(RedisTemplate.class)
public class MultiLevelCacheManagerImpl implements MultiLevelCacheManager {
    
    /**
     * 缓存键前缀
     */
    private static final String CACHE_KEY_PREFIX = "dp:cache:";
    
    /**
     * L1 默认 TTL（分钟）
     */
    private static final long L1_DEFAULT_TTL_MINUTES = 5;
    
    /**
     * L2 默认 TTL（分钟）
     */
    private static final long L2_DEFAULT_TTL_MINUTES = 30;
    
    /**
     * L1 本地缓存（Caffeine）
     */
    private final Cache<String, Object> l1Cache;
    
    /**
     * L2 分布式缓存（Redis）
     */
    private final RedisTemplate<String, Object> redisTemplate;
    
    /**
     * JSON 序列化器
     */
    private final ObjectMapper objectMapper;
    
    /**
     * 缓存键生成器
     */
    private final CacheKeyGenerator cacheKeyGenerator;
    
    /**
     * L2 命中计数器
     */
    private final AtomicLong l2HitCount = new AtomicLong(0);
    
    /**
     * L2 未命中计数器
     */
    private final AtomicLong l2MissCount = new AtomicLong(0);
    
    @Autowired
    public MultiLevelCacheManagerImpl(
            @Qualifier("l1Cache") Cache<String, Object> l1Cache,
            RedisTemplate<String, Object> redisTemplate,
            ObjectMapper objectMapper,
            CacheKeyGenerator cacheKeyGenerator) {
        this.l1Cache = l1Cache;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.cacheKeyGenerator = cacheKeyGenerator;
        log.info("多级缓存管理器初始化完成 - L1: Caffeine, L2: Redis");
    }

    
    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        String cacheKey = buildCacheKey(key);
        
        // 1. 先查 L1 缓存
        Object l1Value = l1Cache.getIfPresent(cacheKey);
        if (l1Value != null) {
            log.debug("L1 缓存命中 - key: {}", key);
            return convertValue(l1Value, type);
        }
        
        // 2. L1 未命中，查 L2 缓存
        try {
            Object l2Value = redisTemplate.opsForValue().get(cacheKey);
            if (l2Value != null) {
                log.debug("L2 缓存命中 - key: {}", key);
                l2HitCount.incrementAndGet();
                
                // 回填到 L1 缓存
                l1Cache.put(cacheKey, l2Value);
                log.debug("L2 命中后回填 L1 - key: {}", key);
                
                return convertValue(l2Value, type);
            }
        } catch (Exception e) {
            log.warn("L2 缓存查询失败 - key: {}, error: {}", key, e.getMessage());
        }
        
        // 3. L1 和 L2 都未命中
        log.debug("缓存未命中 - key: {}", key);
        l2MissCount.incrementAndGet();
        return null;
    }
    
    @Override
    public void put(String key, Object value, long ttl, TimeUnit unit) {
        if (value == null) {
            log.debug("跳过空值缓存 - key: {}", key);
            return;
        }
        
        String cacheKey = buildCacheKey(key);
        
        // 1. 写入 L1 缓存
        l1Cache.put(cacheKey, value);
        log.debug("写入 L1 缓存 - key: {}", key);
        
        // 2. 写入 L2 缓存
        try {
            redisTemplate.opsForValue().set(cacheKey, value, ttl, unit);
            log.debug("写入 L2 缓存 - key: {}, ttl: {} {}", key, ttl, unit);
        } catch (Exception e) {
            log.warn("L2 缓存写入失败 - key: {}, error: {}", key, e.getMessage());
        }
    }
    
    @Override
    public void put(String key, Object value) {
        if (value == null) {
            log.debug("跳过空值缓存 - key: {}", key);
            return;
        }
        
        String cacheKey = buildCacheKey(key);
        
        // 1. 写入 L1 缓存（使用默认 TTL，由 Caffeine 配置控制）
        l1Cache.put(cacheKey, value);
        log.debug("写入 L1 缓存 - key: {}, ttl: {}分钟", key, L1_DEFAULT_TTL_MINUTES);
        
        // 2. 写入 L2 缓存（使用默认 30 分钟 TTL）
        try {
            redisTemplate.opsForValue().set(cacheKey, value, L2_DEFAULT_TTL_MINUTES, TimeUnit.MINUTES);
            log.debug("写入 L2 缓存 - key: {}, ttl: {}分钟", key, L2_DEFAULT_TTL_MINUTES);
        } catch (Exception e) {
            log.warn("L2 缓存写入失败 - key: {}, error: {}", key, e.getMessage());
        }
    }
    
    @Override
    public void evict(String key) {
        String cacheKey = buildCacheKey(key);
        
        // 1. 从 L1 删除
        l1Cache.invalidate(cacheKey);
        log.debug("从 L1 缓存删除 - key: {}", key);
        
        // 2. 从 L2 删除
        try {
            redisTemplate.delete(cacheKey);
            log.debug("从 L2 缓存删除 - key: {}", key);
        } catch (Exception e) {
            log.warn("L2 缓存删除失败 - key: {}, error: {}", key, e.getMessage());
        }
    }
    
    @Override
    public void evictByPattern(String pattern) {
        String cacheKeyPattern = buildCacheKey(pattern);
        
        // 1. 从 L1 删除匹配的键
        // Caffeine 不支持模式匹配，需要遍历所有键
        l1Cache.asMap().keySet().stream()
                .filter(k -> matchPattern(k, cacheKeyPattern))
                .forEach(l1Cache::invalidate);
        log.debug("从 L1 缓存按模式删除 - pattern: {}", pattern);
        
        // 2. 从 L2 删除匹配的键
        try {
            Set<String> keys = redisTemplate.keys(cacheKeyPattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("从 L2 缓存按模式删除 - pattern: {}, count: {}", pattern, keys.size());
            }
        } catch (Exception e) {
            log.warn("L2 缓存按模式删除失败 - pattern: {}, error: {}", pattern, e.getMessage());
        }
    }
    
    @Override
    public void clear() {
        // 1. 清空 L1 缓存
        l1Cache.invalidateAll();
        log.info("L1 缓存已清空");
        
        // 2. 清空 L2 缓存（只清空本应用的缓存键）
        try {
            Set<String> keys = redisTemplate.keys(CACHE_KEY_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("L2 缓存已清空 - 删除 {} 个键", keys.size());
            }
        } catch (Exception e) {
            log.warn("L2 缓存清空失败 - error: {}", e.getMessage());
        }
        
        // 重置统计计数器
        l2HitCount.set(0);
        l2MissCount.set(0);
    }

    
    @Override
    public CacheStats getStats() {
        // 获取 L1 统计信息
        com.github.benmanes.caffeine.cache.stats.CacheStats caffeineStats = l1Cache.stats();
        long l1Hit = caffeineStats.hitCount();
        long l1Miss = caffeineStats.missCount();
        double l1HitRate = caffeineStats.hitRate();
        long l1Size = l1Cache.estimatedSize();
        
        // 获取 L2 统计信息
        long l2Hit = l2HitCount.get();
        long l2Miss = l2MissCount.get();
        double l2HitRate = (l2Hit + l2Miss) > 0 ? (double) l2Hit / (l2Hit + l2Miss) : 0.0;
        long l2MemoryUsage = estimateL2MemoryUsage();
        
        return CacheStats.builder()
                .l1HitCount(l1Hit)
                .l1MissCount(l1Miss)
                .l1HitRate(l1HitRate)
                .l1Size(l1Size)
                .l2HitCount(l2Hit)
                .l2MissCount(l2Miss)
                .l2HitRate(l2HitRate)
                .l2MemoryUsage(l2MemoryUsage)
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
        
        // 先检查 L1
        if (l1Cache.getIfPresent(cacheKey) != null) {
            return true;
        }
        
        // 再检查 L2
        try {
            Boolean exists = redisTemplate.hasKey(cacheKey);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.warn("检查 L2 缓存存在性失败 - key: {}, error: {}", key, e.getMessage());
            return false;
        }
    }
    
    @Override
    public long getL1Size() {
        return l1Cache.estimatedSize();
    }
    
    @Override
    public long getL2Size() {
        try {
            Set<String> keys = redisTemplate.keys(CACHE_KEY_PREFIX + "*");
            return keys != null ? keys.size() : 0;
        } catch (Exception e) {
            log.warn("获取 L2 缓存大小失败 - error: {}", e.getMessage());
            return 0;
        }
    }
    
    // ==================== 私有辅助方法 ====================
    
    /**
     * 构建完整的缓存键
     */
    private String buildCacheKey(String key) {
        return CACHE_KEY_PREFIX + key;
    }
    
    /**
     * 转换缓存值类型
     */
    @SuppressWarnings("unchecked")
    private <T> T convertValue(Object value, Class<T> type) {
        if (value == null) {
            return null;
        }
        
        if (type.isInstance(value)) {
            return (T) value;
        }
        
        // 尝试使用 ObjectMapper 转换
        try {
            return objectMapper.convertValue(value, type);
        } catch (Exception e) {
            log.warn("缓存值类型转换失败 - targetType: {}, error: {}", type.getName(), e.getMessage());
            return null;
        }
    }
    
    /**
     * 简单的通配符模式匹配
     * 支持 * 作为通配符
     */
    private boolean matchPattern(String key, String pattern) {
        String regex = pattern
                .replace(".", "\\.")
                .replace("*", ".*");
        return key.matches(regex);
    }
    
    /**
     * 估算 L2 缓存内存使用量
     * 注意：这是一个粗略估算，实际内存使用可能不同
     */
    private long estimateL2MemoryUsage() {
        try {
            // 获取 Redis 内存信息
            Object info = redisTemplate.execute((RedisCallback<Object>) connection -> {
                Properties props = connection.serverCommands().info("memory");
                return props != null ? props.getProperty("used_memory") : null;
            });
            
            if (info != null) {
                return Long.parseLong(info.toString());
            }
        } catch (Exception e) {
            log.debug("获取 Redis 内存信息失败 - error: {}", e.getMessage());
        }
        return 0;
    }
}
