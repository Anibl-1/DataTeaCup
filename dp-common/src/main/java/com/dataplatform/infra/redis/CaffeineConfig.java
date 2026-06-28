package com.dataplatform.infra.redis;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine L1 本地缓存配置
 * 
 * 配置说明：
 * - 最大条目数：10000
 * - 淘汰策略：LRU（基于访问时间，expireAfterAccess）
 * - TTL：5分钟
 * 
 * 提供多个缓存实例用于不同用途：
 * - queryCache: 查询结果缓存
 * - metadataCache: 元数据缓存
 * - hotspotCache: 热点数据缓存
 * 
 * @see RedisConfig L2 分布式缓存配置
 */
@Slf4j
@Configuration
public class CaffeineConfig {

    private static final int DEFAULT_MAX_SIZE = 10000;
    private static final int DEFAULT_TTL_MINUTES = 5;

    @Bean(name = "queryCache")
    public Cache<String, Object> queryCache() {
        log.info("初始化 L1 查询缓存 - 最大条目: {}, TTL: {}分钟, 淘汰策略: LRU",
                DEFAULT_MAX_SIZE, DEFAULT_TTL_MINUTES);
        return Caffeine.newBuilder()
                .maximumSize(DEFAULT_MAX_SIZE)
                .expireAfterAccess(DEFAULT_TTL_MINUTES, TimeUnit.MINUTES)
                .recordStats()
                .removalListener((key, value, cause) -> {
                    if (log.isDebugEnabled()) {
                        log.debug("查询缓存条目被移除 - key: {}, 原因: {}", key, cause);
                    }
                })
                .build();
    }

    @Bean(name = "metadataCache")
    public Cache<String, Object> metadataCache() {
        log.info("初始化 L1 元数据缓存 - 最大条目: {}, TTL: {}分钟, 淘汰策略: LRU", 5000, 10);
        return Caffeine.newBuilder()
                .maximumSize(5000)
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .recordStats()
                .removalListener((key, value, cause) -> {
                    if (log.isDebugEnabled()) {
                        log.debug("元数据缓存条目被移除 - key: {}, 原因: {}", key, cause);
                    }
                })
                .build();
    }

    @Bean(name = "hotspotCache")
    public Cache<String, Object> hotspotCache() {
        log.info("初始化 L1 热点数据缓存 - 最大条目: {}, TTL: {}分钟, 淘汰策略: LRU", 2000, 15);
        return Caffeine.newBuilder()
                .maximumSize(2000)
                .expireAfterAccess(15, TimeUnit.MINUTES)
                .recordStats()
                .removalListener((key, value, cause) -> {
                    if (log.isDebugEnabled()) {
                        log.debug("热点缓存条目被移除 - key: {}, 原因: {}", key, cause);
                    }
                })
                .build();
    }

    @Bean(name = "sessionCache")
    public Cache<String, Object> sessionCache() {
        log.info("初始化 L1 会话缓存 - 最大条目: {}, TTL: {}分钟, 淘汰策略: LRU", DEFAULT_MAX_SIZE, 30);
        return Caffeine.newBuilder()
                .maximumSize(DEFAULT_MAX_SIZE)
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .recordStats()
                .removalListener((key, value, cause) -> {
                    if (log.isDebugEnabled()) {
                        log.debug("会话缓存条目被移除 - key: {}, 原因: {}", key, cause);
                    }
                })
                .build();
    }

    @Bean(name = "l1Cache")
    public Cache<String, Object> l1Cache() {
        log.info("初始化通用 L1 缓存 - 最大条目: {}, TTL: {}分钟, 淘汰策略: LRU",
                DEFAULT_MAX_SIZE, DEFAULT_TTL_MINUTES);
        return Caffeine.newBuilder()
                .maximumSize(DEFAULT_MAX_SIZE)
                .expireAfterAccess(DEFAULT_TTL_MINUTES, TimeUnit.MINUTES)
                .recordStats()
                .removalListener((key, value, cause) -> {
                    if (log.isDebugEnabled()) {
                        log.debug("L1 缓存条目被移除 - key: {}, 原因: {}", key, cause);
                    }
                })
                .build();
    }
}
