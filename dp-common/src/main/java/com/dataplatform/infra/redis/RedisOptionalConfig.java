package com.dataplatform.infra.redis;

import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoDefaultImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

/**
 * Redis可选配置
 * 当 spring.data.redis.enabled=false 时：
 * 1. Sa-Token使用内存存储替代Redis
 * 2. 提供懒连接的RedisConnectionFactory/RedisTemplate，启动时不连Redis
 */
@Slf4j
@Configuration
@EnableCaching
public class RedisOptionalConfig {

    @Bean
    @Primary
    @ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "false")
    public SaTokenDao saTokenDaoInit() {
        log.info("Redis未启用，Sa-Token使用内存存储");
        return new SaTokenDaoDefaultImpl();
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "false")
    public LettuceConnectionFactory redisConnectionFactory() {
        log.info("Redis未启用，创建懒初始化的Redis连接工厂");
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration("localhost", 6379);
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofMillis(100))
                .build();
        LettuceConnectionFactory factory = new LettuceConnectionFactory(config, clientConfig);
        factory.setEagerInitialization(false);
        factory.setValidateConnection(false);
        factory.setShareNativeConnection(false);
        return factory;
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "false")
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "false")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        return template;
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "false")
    public CacheManager cacheManager() {
        log.info("Redis未启用，使用本地内存缓存管理器");
        return new ConcurrentMapCacheManager();
    }
}
