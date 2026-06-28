package com.dataplatform.data.service.health.checker;

import com.dataplatform.data.service.health.ComponentHealth;
import com.dataplatform.data.service.health.HealthChecker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Properties;

/**
 * Redis健康检查器
 */
@Slf4j
@Component
public class RedisHealthChecker implements HealthChecker {

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Value("${spring.data.redis.enabled:false}")
    private boolean redisEnabled;

    @Override
    public ComponentHealth check() {
        long start = System.currentTimeMillis();
        ComponentHealth health = new ComponentHealth();
        health.setName(getName());
        health.setCritical(false);

        if (!redisEnabled || redisTemplate == null) {
            health.setResponseTime(System.currentTimeMillis() - start);
            health.setStatus(ComponentHealth.STATUS_UP);
            health.setMessage("Redis未启用");
            health.withDetail("enabled", false);
            return health;
        }

        try {
            RedisConnectionFactory factory = redisTemplate.getConnectionFactory();
            if (factory == null) {
                health.setStatus(ComponentHealth.STATUS_DOWN);
                health.setMessage("Redis连接工厂未配置");
                return health;
            }

            RedisConnection connection = factory.getConnection();
            String pong = connection.ping();
            health.setResponseTime(System.currentTimeMillis() - start);

            if ("PONG".equals(pong)) {
                health.setStatus(ComponentHealth.STATUS_UP);
                health.setMessage("Redis连接正常");
                
                // 获取Redis信息
                Properties info = connection.info();
                if (info != null) {
                    health.withDetail("redis_version", info.getProperty("redis_version"))
                          .withDetail("connected_clients", info.getProperty("connected_clients"))
                          .withDetail("used_memory_human", info.getProperty("used_memory_human"))
                          .withDetail("uptime_in_days", info.getProperty("uptime_in_days"));
                }
            } else {
                health.setStatus(ComponentHealth.STATUS_DOWN);
                health.setMessage("Redis响应异常");
            }
            
            connection.close();
        } catch (Exception e) {
            health.setResponseTime(System.currentTimeMillis() - start);
            health.setStatus(ComponentHealth.STATUS_DOWN);
            health.setMessage("Redis连接失败: " + e.getMessage());
            log.error("Redis健康检查失败", e);
        }

        return health;
    }

    @Override
    public String getName() {
        return "redis";
    }

    @Override
    public int getCheckInterval() {
        return 30;
    }

    @Override
    public boolean isCritical() {
        return false;
    }
}
