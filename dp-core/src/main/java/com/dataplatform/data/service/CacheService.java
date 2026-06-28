package com.dataplatform.data.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 统一缓存服务
 * 支持 Redis 和本地内存缓存自动切换
 * 当 Redis 启用时使用 Redis，否则使用本地缓存
 * 
 * @author dataplatform
 */
@Slf4j
@Service
public class CacheService {
    
    @Value("${spring.data.redis.enabled:false}")
    private boolean redisEnabled;
    
    @Value("${cache.default.ttl:1800}")
    private long defaultTtlSeconds;
    
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    // 本地缓存（Redis 不可用时使用）
    private final ConcurrentHashMap<String, LocalCacheEntry> localCache = new ConcurrentHashMap<>();
    
    // 定时清理过期缓存的调度器
    private ScheduledExecutorService cleanupScheduler;
    
    @PostConstruct
    public void init() {
        // 每分钟清理一次过期的本地缓存
        cleanupScheduler = Executors.newSingleThreadScheduledExecutor();
        cleanupScheduler.scheduleAtFixedRate(this::cleanExpiredLocalCache, 1, 1, TimeUnit.MINUTES);
        log.info("缓存服务已启动，Redis启用状态: {}", redisEnabled);
    }
    
    @PreDestroy
    public void destroy() {
        if (cleanupScheduler != null) {
            cleanupScheduler.shutdown();
        }
    }
    
    /**
     * 清理过期的本地缓存
     */
    private void cleanExpiredLocalCache() {
        int removed = 0;
        for (var entry : localCache.entrySet()) {
            if (entry.getValue().isExpired()) {
                localCache.remove(entry.getKey());
                removed++;
            }
        }
        if (removed > 0) {
            log.debug("清理过期本地缓存: {} 条", removed);
        }
    }
    
    /**
     * 本地缓存条目
     */
    private static class LocalCacheEntry {
        final Object data;
        final long expireAt;
        
        LocalCacheEntry(Object data, long ttlSeconds) {
            this.data = data;
            this.expireAt = System.currentTimeMillis() + ttlSeconds * 1000;
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() > expireAt;
        }
    }
    
    /**
     * 检查 Redis 是否可用
     */
    public boolean isRedisAvailable() {
        if (!redisEnabled || redisTemplate == null) {
            return false;
        }
        try {
            var connectionFactory = redisTemplate.getConnectionFactory();
            if (connectionFactory == null) {
                return false;
            }
            connectionFactory.getConnection().ping();
            return true;
        } catch (Exception e) {
            log.debug("Redis连接不可用，将使用本地缓存: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 设置缓存（使用默认TTL）
     */
    public void set(String key, Object value) {
        set(key, value, defaultTtlSeconds, TimeUnit.SECONDS);
    }
    
    /**
     * 设置缓存（指定TTL）
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        long ttlSeconds = unit.toSeconds(timeout);
        
        if (isRedisAvailable()) {
            try {
                redisTemplate.opsForValue().set(key, value, timeout, unit);
                log.debug("Redis缓存设置成功: key={}", key);
            } catch (Exception e) {
                log.warn("Redis缓存设置失败，使用本地缓存: {}", e.getMessage());
                setLocal(key, value, ttlSeconds);
            }
        } else {
            setLocal(key, value, ttlSeconds);
        }
    }
    
    /**
     * 获取缓存
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        if (isRedisAvailable()) {
            try {
                Object value = redisTemplate.opsForValue().get(key);
                if (value != null) {
                    log.debug("Redis缓存命中: key={}", key);
                    return convertValue(value, type);
                }
            } catch (Exception e) {
                log.warn("Redis缓存获取失败，尝试本地缓存: {}", e.getMessage());
            }
        }
        
        // 从本地缓存获取
        return getLocal(key, type);
    }
    
    /**
     * 获取缓存（带类型引用，用于泛型）
     */
    public <T> T get(String key, TypeReference<T> typeRef) {
        if (isRedisAvailable()) {
            try {
                Object value = redisTemplate.opsForValue().get(key);
                if (value != null) {
                    log.debug("Redis缓存命中: key={}", key);
                    return convertValue(value, typeRef);
                }
            } catch (Exception e) {
                log.warn("Redis缓存获取失败，尝试本地缓存: {}", e.getMessage());
            }
        }
        
        return getLocal(key, typeRef);
    }
    
    /**
     * 删除缓存
     */
    public void delete(String key) {
        if (isRedisAvailable()) {
            try {
                redisTemplate.delete(key);
                log.debug("Redis缓存删除: key={}", key);
            } catch (Exception e) {
                log.warn("Redis缓存删除失败: {}", e.getMessage());
            }
        }
        localCache.remove(key);
    }
    
    /**
     * 删除匹配模式的缓存
     */
    public void deleteByPattern(String pattern) {
        if (isRedisAvailable()) {
            try {
                var keys = redisTemplate.keys(pattern);
                if (keys != null && !keys.isEmpty()) {
                    redisTemplate.delete(keys);
                    log.debug("Redis缓存批量删除: pattern={}, count={}", pattern, keys.size());
                }
            } catch (Exception e) {
                log.warn("Redis缓存批量删除失败: {}", e.getMessage());
            }
        }
        
        // 本地缓存模式匹配删除
        String regexPattern = pattern.replace("*", ".*");
        localCache.keySet().removeIf(key -> key.matches(regexPattern));
    }
    
    /**
     * 检查缓存是否存在
     */
    public boolean exists(String key) {
        if (isRedisAvailable()) {
            try {
                Boolean exists = redisTemplate.hasKey(key);
                return Boolean.TRUE.equals(exists);
            } catch (Exception e) {
                log.warn("Redis检查key失败: {}", e.getMessage());
            }
        }
        
        LocalCacheEntry entry = localCache.get(key);
        return entry != null && !entry.isExpired();
    }
    
    /**
     * 设置过期时间
     */
    public void expire(String key, long timeout, TimeUnit unit) {
        if (isRedisAvailable()) {
            try {
                redisTemplate.expire(key, timeout, unit);
            } catch (Exception e) {
                log.warn("Redis设置过期时间失败: {}", e.getMessage());
            }
        }
    }
    
    /**
     * 清空所有缓存
     */
    public void clear() {
        localCache.clear();
        log.info("本地缓存已清空");
    }
    
    /**
     * 获取缓存统计信息
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("redisEnabled", redisEnabled);
        stats.put("redisAvailable", isRedisAvailable());
        stats.put("localCacheSize", localCache.size());
        
        // 统计本地缓存过期数量
        long expiredCount = localCache.values().stream()
                .filter(LocalCacheEntry::isExpired)
                .count();
        stats.put("localCacheExpired", expiredCount);
        stats.put("localCacheActive", localCache.size() - expiredCount);
        
        return stats;
    }
    
    // ========== 私有方法 ==========
    
    private void setLocal(String key, Object value, long ttlSeconds) {
        localCache.put(key, new LocalCacheEntry(value, ttlSeconds));
        log.debug("本地缓存设置: key={}", key);
    }
    
    @SuppressWarnings("unchecked")
    private <T> T getLocal(String key, Class<T> type) {
        LocalCacheEntry entry = localCache.get(key);
        if (entry != null && !entry.isExpired()) {
            log.debug("本地缓存命中: key={}", key);
            return convertValue(entry.data, type);
        }
        if (entry != null) {
            localCache.remove(key);
        }
        return null;
    }
    
    private <T> T getLocal(String key, TypeReference<T> typeRef) {
        LocalCacheEntry entry = localCache.get(key);
        if (entry != null && !entry.isExpired()) {
            log.debug("本地缓存命中: key={}", key);
            return convertValue(entry.data, typeRef);
        }
        if (entry != null) {
            localCache.remove(key);
        }
        return null;
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
            String json = objectMapper.writeValueAsString(value);
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            log.warn("缓存值类型转换失败: {}", e.getMessage());
            return null;
        }
    }
    
    private <T> T convertValue(Object value, TypeReference<T> typeRef) {
        if (value == null) {
            return null;
        }
        try {
            String json = objectMapper.writeValueAsString(value);
            return objectMapper.readValue(json, typeRef);
        } catch (JsonProcessingException e) {
            log.warn("缓存值类型转换失败: {}", e.getMessage());
            return null;
        }
    }
}
