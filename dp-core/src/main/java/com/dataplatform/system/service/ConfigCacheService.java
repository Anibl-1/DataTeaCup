package com.dataplatform.system.service;

import com.dataplatform.system.entity.SystemConfig;
import com.dataplatform.system.mapper.SystemConfigMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;

/**
 * 配置缓存服务 - 基于 Redis 的配置缓存层
 * <p>
 * 提供配置项的 Redis 缓存读写能力，应用启动时预加载全部配置到 Redis。
 * 当 Redis 不可用时优雅降级，不影响主流程。
 */
@Slf4j
@Service
public class ConfigCacheService {

    private static final String KEY_PREFIX = "dp:config:";

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private SystemConfigMapper systemConfigMapper;

    /**
     * 应用启动时预加载全部配置到 Redis
     */
    @PostConstruct
    public void preloadAll() {
        if (!isRedisAvailable()) {
            log.warn("Redis 不可用，跳过配置预加载");
            return;
        }
        try {
            List<SystemConfig> configs = systemConfigMapper.selectList(null);
            for (SystemConfig config : configs) {
                if (config.getConfigKey() != null && config.getConfigValue() != null) {
                    redisTemplate.opsForValue().set(KEY_PREFIX + config.getConfigKey(), config.getConfigValue());
                }
            }
            log.info("配置预加载完成，共加载 {} 条配置到 Redis", configs.size());
        } catch (Exception e) {
            log.warn("配置预加载到 Redis 失败，将使用数据库直接读取: {}", e.getMessage());
        }
    }

    /**
     * 检测 Redis 是否真正可用（连接可达）
     */
    private boolean isRedisAvailable() {
        if (redisTemplate == null) {
            return false;
        }
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            return true;
        } catch (Exception e) {
            log.info("Redis 连接不可用，降级为数据库直读模式: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 将配置项写入 Redis 缓存
     */
    public void put(String configKey, String configValue) {
        if (!isRedisAvailable()) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(KEY_PREFIX + configKey, configValue);
        } catch (Exception e) {
            log.warn("写入配置缓存失败: key={}, error={}", configKey, e.getMessage());
        }
    }

    /**
     * 从 Redis 缓存读取配置项
     */
    public String get(String configKey) {
        if (!isRedisAvailable()) {
            return null;
        }
        try {
            Object val = redisTemplate.opsForValue().get(KEY_PREFIX + configKey);
            return val != null ? val.toString() : null;
        } catch (Exception e) {
            log.warn("读取配置缓存失败: key={}, error={}", configKey, e.getMessage());
            return null;
        }
    }

    /**
     * 从 Redis 缓存中移除配置项
     */
    public void evict(String configKey) {
        if (!isRedisAvailable()) {
            return;
        }
        try {
            redisTemplate.delete(KEY_PREFIX + configKey);
        } catch (Exception e) {
            log.warn("移除配置缓存失败: key={}, error={}", configKey, e.getMessage());
        }
    }
}
