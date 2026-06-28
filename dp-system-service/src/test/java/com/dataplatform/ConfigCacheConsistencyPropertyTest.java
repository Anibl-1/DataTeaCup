package com.dataplatform;

import com.dataplatform.system.entity.SystemConfig;
import com.dataplatform.system.mapper.SystemConfigMapper;
import com.dataplatform.system.service.ConfigCacheService;
import com.dataplatform.system.service.SystemConfigService;
import net.jqwik.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 属性测试：配置缓存一致性
 *
 * Feature: mars-integration-optimization, Property 6: 配置缓存一致性
 * Validates: Requirements 6.4
 */
class ConfigCacheConsistencyPropertyTest {

    // ========== Helper methods ==========

    @SuppressWarnings("unchecked")
    private ConfigCacheService createCacheServiceWithMockRedis() throws Exception {
        ConfigCacheService cacheService = new ConfigCacheService();
        RedisTemplate<String, Object> mockRedis = Mockito.mock(RedisTemplate.class);
        ValueOperations<String, Object> mockValueOps = Mockito.mock(ValueOperations.class);
        when(mockRedis.opsForValue()).thenReturn(mockValueOps);
        when(mockRedis.delete(any(String.class))).thenReturn(true);

        Field redisField = ConfigCacheService.class.getDeclaredField("redisTemplate");
        redisField.setAccessible(true);
        redisField.set(cacheService, mockRedis);
        return cacheService;
    }

    @SuppressWarnings("unchecked")
    private RedisTemplate<String, Object> extractMockRedis(ConfigCacheService cacheService) throws Exception {
        Field redisField = ConfigCacheService.class.getDeclaredField("redisTemplate");
        redisField.setAccessible(true);
        return (RedisTemplate<String, Object>) redisField.get(cacheService);
    }

    private SystemConfigService createServiceWithMocks(ConfigCacheService cacheService) throws Exception {
        SystemConfigService service = new SystemConfigService();
        SystemConfigMapper mockMapper = Mockito.mock(SystemConfigMapper.class);
        when(mockMapper.selectByKey(any())).thenReturn(null);
        when(mockMapper.insert(any(SystemConfig.class))).thenReturn(1);
        when(mockMapper.update(any(SystemConfig.class))).thenReturn(1);
        when(mockMapper.deleteById(any())).thenReturn(1);

        Field mapperField = SystemConfigService.class.getDeclaredField("systemConfigMapper");
        mapperField.setAccessible(true);
        mapperField.set(service, mockMapper);

        Field cacheField = SystemConfigService.class.getDeclaredField("configCacheService");
        cacheField.setAccessible(true);
        cacheField.set(service, cacheService);

        return service;
    }

    private SystemConfigMapper extractMockMapper(SystemConfigService service) throws Exception {
        Field mapperField = SystemConfigService.class.getDeclaredField("systemConfigMapper");
        mapperField.setAccessible(true);
        return (SystemConfigMapper) mapperField.get(service);
    }

    private SystemConfig buildStringConfig(String key, String value) {
        SystemConfig config = new SystemConfig();
        config.setConfigKey(key);
        config.setConfigValue(value);
        config.setConfigType("string");
        config.setConfigDesc("test config");
        config.setConfigGroup("默认");
        config.setIsSystem(false);
        return config;
    }

    // ========== Property 6a: After create, cache receives correct key and value ==========

    /**
     * For any config creation, the cache put is called with key "dp:config:{configKey}"
     * and the stored config value, ensuring cache consistency after create.
     *
     * Feature: mars-integration-optimization, Property 6: 配置缓存一致性
     * Validates: Requirements 6.4
     */
    @Property(tries = 100)
    @Tag("Feature_mars-integration-optimization")
    @Tag("Property_6_配置缓存一致性")
    void create_config_cacheReceivesCorrectKeyAndValue(
            @ForAll("configKeys") String configKey,
            @ForAll("configValues") String configValue
    ) throws Exception {
        ConfigCacheService cacheService = createCacheServiceWithMockRedis();
        RedisTemplate<String, Object> mockRedis = extractMockRedis(cacheService);
        ValueOperations<String, Object> mockValueOps = mockRedis.opsForValue();
        SystemConfigService service = createServiceWithMocks(cacheService);

        SystemConfig config = buildStringConfig(configKey, configValue);
        service.create(config);

        // Verify Redis received the correct key with prefix and the correct value
        verify(mockValueOps).set(eq("dp:config:" + configKey), eq(configValue));
    }

    // ========== Property 6b: After update, cache is updated to new value ==========

    /**
     * For any config update, the cache put is called with the updated value,
     * ensuring cache consistency after update.
     *
     * Feature: mars-integration-optimization, Property 6: 配置缓存一致性
     * Validates: Requirements 6.4
     */
    @Property(tries = 100)
    @Tag("Feature_mars-integration-optimization")
    @Tag("Property_6_配置缓存一致性")
    void update_config_cacheReceivesUpdatedValue(
            @ForAll("configKeys") String configKey,
            @ForAll("configValues") String originalValue,
            @ForAll("configValues") String newValue
    ) throws Exception {
        ConfigCacheService cacheService = createCacheServiceWithMockRedis();
        RedisTemplate<String, Object> mockRedis = extractMockRedis(cacheService);
        ValueOperations<String, Object> mockValueOps = mockRedis.opsForValue();
        SystemConfigService service = createServiceWithMocks(cacheService);

        // Set up existing config in DB for the update path
        SystemConfig existing = buildStringConfig(configKey, originalValue);
        existing.setId(1L);
        SystemConfigMapper mockMapper = extractMockMapper(service);
        when(mockMapper.selectById(1L)).thenReturn(existing);

        // Perform update with new value
        SystemConfig updateConfig = new SystemConfig();
        updateConfig.setId(1L);
        updateConfig.setConfigKey(configKey);
        updateConfig.setConfigValue(newValue);
        updateConfig.setConfigType("string");
        updateConfig.setConfigDesc("updated");
        service.update(updateConfig);

        // Verify cache received the new value
        verify(mockValueOps).set(eq("dp:config:" + configKey), eq(newValue));
    }

    // ========== Property 6c: After delete, cache evicts the key ==========

    /**
     * For any config deletion, the cache evict is called with the correct key,
     * ensuring the deleted config is removed from cache.
     *
     * Feature: mars-integration-optimization, Property 6: 配置缓存一致性
     * Validates: Requirements 6.4
     */
    @Property(tries = 100)
    @Tag("Feature_mars-integration-optimization")
    @Tag("Property_6_配置缓存一致性")
    void delete_config_cacheEvictsCorrectKey(
            @ForAll("configKeys") String configKey,
            @ForAll("configValues") String configValue
    ) throws Exception {
        ConfigCacheService cacheService = createCacheServiceWithMockRedis();
        RedisTemplate<String, Object> mockRedis = extractMockRedis(cacheService);
        SystemConfigService service = createServiceWithMocks(cacheService);

        // Set up existing non-system config in DB
        SystemConfig existing = buildStringConfig(configKey, configValue);
        existing.setId(1L);
        existing.setIsSystem(false);
        SystemConfigMapper mockMapper = extractMockMapper(service);
        when(mockMapper.selectById(1L)).thenReturn(existing);

        service.delete(1L);

        // Verify Redis delete was called with the correct prefixed key
        verify(mockRedis).delete(eq("dp:config:" + configKey));
    }

    // ========== Property 6d: ConfigCacheService put/get roundtrip via RedisTemplate ==========

    /**
     * For any config key and value, after calling put(), a subsequent get() returns
     * the same value (verifying the RedisTemplate key format is consistent).
     *
     * Feature: mars-integration-optimization, Property 6: 配置缓存一致性
     * Validates: Requirements 6.4
     */
    @SuppressWarnings("unchecked")
    @Property(tries = 100)
    @Tag("Feature_mars-integration-optimization")
    @Tag("Property_6_配置缓存一致性")
    void cacheService_putGet_roundtrip(
            @ForAll("configKeys") String configKey,
            @ForAll("configValues") String configValue
    ) throws Exception {
        ConfigCacheService cacheService = createCacheServiceWithMockRedis();
        RedisTemplate<String, Object> mockRedis = extractMockRedis(cacheService);
        ValueOperations<String, Object> mockValueOps = mockRedis.opsForValue();

        // Simulate that after put, get returns the same value
        when(mockValueOps.get("dp:config:" + configKey)).thenReturn(configValue);

        cacheService.put(configKey, configValue);
        String retrieved = cacheService.get(configKey);

        // The put and get use the same key format, so the value should match
        assertThat(retrieved).isEqualTo(configValue);
        // Verify put was called with the correct prefixed key
        verify(mockValueOps).set(eq("dp:config:" + configKey), eq(configValue));
    }

    // ========== Property 6e: ConfigCacheService evict removes key from Redis ==========

    /**
     * For any config key, after calling evict(), the Redis delete is called with
     * the correctly prefixed key "dp:config:{configKey}".
     *
     * Feature: mars-integration-optimization, Property 6: 配置缓存一致性
     * Validates: Requirements 6.4
     */
    @Property(tries = 100)
    @Tag("Feature_mars-integration-optimization")
    @Tag("Property_6_配置缓存一致性")
    void cacheService_evict_deletesCorrectKey(
            @ForAll("configKeys") String configKey
    ) throws Exception {
        ConfigCacheService cacheService = createCacheServiceWithMockRedis();
        RedisTemplate<String, Object> mockRedis = extractMockRedis(cacheService);

        cacheService.evict(configKey);

        verify(mockRedis).delete(eq("dp:config:" + configKey));
    }

    // ========== Providers ==========

    @Provide
    Arbitrary<String> configKeys() {
        return Arbitraries.oneOf(
                // Dot-separated config keys (typical format)
                Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(10)
                        .flatMap(prefix -> Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(10)
                                .map(suffix -> prefix.toLowerCase() + "." + suffix.toLowerCase())),
                // Typical config key examples
                Arbitraries.of(
                        "system.version", "system.copyright", "app.name",
                        "mail.host", "upload.maxSize", "cache.ttl",
                        "db.url", "redis.host", "log.level",
                        "security.key", "api.timeout", "feature.enabled"
                )
        );
    }

    @Provide
    Arbitrary<String> configValues() {
        return Arbitraries.oneOf(
                Arbitraries.strings().ofMinLength(1).ofMaxLength(200)
                        .filter(s -> !s.isEmpty()),
                Arbitraries.of(
                        "hello", "1.0.0", "true", "42", "localhost:6379",
                        "https://example.com", "{\"key\":\"value\"}",
                        "DataTeaCup © 2026", "some config value",
                        "/path/to/file", "utf-8", "3600"
                )
        );
    }
}
