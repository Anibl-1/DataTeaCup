package com.dataplatform.infra.redis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * 根据 spring.data.redis.enabled 动态控制 Redis 自动配置
 *
 * <p>当 enabled=false（默认）时，自动排除 Redis AutoConfiguration，
 * 避免启动时尝试连接不存在的 Redis 实例导致失败。</p>
 *
 * <p>当 enabled=true 时，不做任何排除，Spring Boot 正常自动配置 Redis。</p>
 *
 * @author dataplatform
 */
public class RedisAutoConfigExcluder implements EnvironmentPostProcessor {

    private static final String EXCLUDE_CLASSES = String.join(",",
            "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
            "org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration",
            "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
    );

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String enabled = environment.getProperty("spring.data.redis.enabled", "false");

        if (!"true".equalsIgnoreCase(enabled)) {
            Map<String, Object> props = new HashMap<>();
            // 追加到已有的 exclude 列表
            String existing = environment.getProperty("spring.autoconfigure.exclude", "");
            if (existing.isEmpty()) {
                props.put("spring.autoconfigure.exclude", EXCLUDE_CLASSES);
            } else if (!existing.contains("RedisAutoConfiguration")) {
                props.put("spring.autoconfigure.exclude", existing + "," + EXCLUDE_CLASSES);
            }
            environment.getPropertySources().addFirst(
                    new MapPropertySource("redis-auto-excluder", props));
        }
    }
}
