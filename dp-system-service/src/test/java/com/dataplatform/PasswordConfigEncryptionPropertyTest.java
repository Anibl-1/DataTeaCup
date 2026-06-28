package com.dataplatform;

import com.dataplatform.common.util.AesEncryptUtil;
import com.dataplatform.system.entity.SystemConfig;
import com.dataplatform.system.mapper.SystemConfigMapper;
import com.dataplatform.system.service.ConfigCacheService;
import com.dataplatform.system.service.SystemConfigService;
import net.jqwik.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 属性测试：Password 类型配置加密与脱敏
 *
 * Feature: mars-integration-optimization, Property 5: Password 类型配置加密与脱敏
 * Validates: Requirements 6.3
 */
class PasswordConfigEncryptionPropertyTest {

    // ========== Helper methods ==========

    private SystemConfigService createServiceWithMockMapper() throws Exception {
        SystemConfigService service = new SystemConfigService();
        SystemConfigMapper mockMapper = Mockito.mock(SystemConfigMapper.class);
        when(mockMapper.selectByKey(any())).thenReturn(null);
        when(mockMapper.insert(any(SystemConfig.class))).thenReturn(1);
        when(mockMapper.update(any(SystemConfig.class))).thenReturn(1);
        Field mapperField = SystemConfigService.class.getDeclaredField("systemConfigMapper");
        mapperField.setAccessible(true);
        mapperField.set(service, mockMapper);

        ConfigCacheService mockCacheService = Mockito.mock(ConfigCacheService.class);
        Field cacheField = SystemConfigService.class.getDeclaredField("configCacheService");
        cacheField.setAccessible(true);
        cacheField.set(service, mockCacheService);

        return service;
    }

    private SystemConfigMapper extractMockMapper(SystemConfigService service) throws Exception {
        Field mapperField = SystemConfigService.class.getDeclaredField("systemConfigMapper");
        mapperField.setAccessible(true);
        return (SystemConfigMapper) mapperField.get(service);
    }

    private SystemConfig buildPasswordConfig(String plaintext) {
        SystemConfig config = new SystemConfig();
        config.setConfigKey("test.password." + System.nanoTime());
        config.setConfigValue(plaintext);
        config.setConfigType("password");
        config.setConfigDesc("test password config");
        config.setConfigGroup("默认");
        config.setIsSystem(false);
        return config;
    }

    // ========== Property 5a: encrypt produces ciphertext different from plaintext ==========

    /**
     * For any non-null plaintext string, AesEncryptUtil.encrypt(plaintext) produces
     * a ciphertext that is different from the original plaintext.
     *
     * Feature: mars-integration-optimization, Property 5: Password 类型配置加密与脱敏
     * Validates: Requirements 6.3
     */
    @Property(tries = 100)
    @Tag("Feature_mars-integration-optimization")
    @Tag("Property_5_Password类型配置加密与脱敏")
    void encrypt_producesCiphertextDifferentFromPlaintext(@ForAll("nonNullPlaintexts") String plaintext) {
        String ciphertext = AesEncryptUtil.encrypt(plaintext);

        assertThat(ciphertext).isNotNull();
        assertThat(ciphertext).isNotEqualTo(plaintext);
    }

    // ========== Property 5b: encrypt/decrypt roundtrip ==========

    /**
     * For any non-null plaintext string, AesEncryptUtil.decrypt(AesEncryptUtil.encrypt(plaintext))
     * equals the original plaintext (roundtrip consistency).
     *
     * Feature: mars-integration-optimization, Property 5: Password 类型配置加密与脱敏
     * Validates: Requirements 6.3
     */
    @Property(tries = 100)
    @Tag("Feature_mars-integration-optimization")
    @Tag("Property_5_Password类型配置加密与脱敏")
    void encryptDecrypt_roundtrip_returnsOriginalPlaintext(@ForAll("nonNullPlaintexts") String plaintext) {
        String ciphertext = AesEncryptUtil.encrypt(plaintext);
        String decrypted = AesEncryptUtil.decrypt(ciphertext);

        assertThat(decrypted).isEqualTo(plaintext);
    }

    // ========== Property 5c: password config stored as encrypted value ==========

    /**
     * When a password-type config is created, the value stored (passed to mapper.insert)
     * is the AES-encrypted form, not the original plaintext.
     *
     * Feature: mars-integration-optimization, Property 5: Password 类型配置加密与脱敏
     * Validates: Requirements 6.3
     */
    @Property(tries = 100)
    @Tag("Feature_mars-integration-optimization")
    @Tag("Property_5_Password类型配置加密与脱敏")
    void create_passwordConfig_storesEncryptedValue(@ForAll("nonNullPlaintexts") String plaintext) throws Exception {
        SystemConfigService service = createServiceWithMockMapper();
        SystemConfigMapper mockMapper = extractMockMapper(service);
        ArgumentCaptor<SystemConfig> captor = ArgumentCaptor.forClass(SystemConfig.class);

        SystemConfig config = buildPasswordConfig(plaintext);
        service.create(config);

        Mockito.verify(mockMapper).insert(captor.capture());
        SystemConfig stored = captor.getValue();

        // The stored value should be the encrypted form, not the plaintext
        assertThat(stored.getConfigValue()).isNotEqualTo(plaintext);
        // Decrypting the stored value should yield the original plaintext
        assertThat(AesEncryptUtil.decrypt(stored.getConfigValue())).isEqualTo(plaintext);
    }

    // ========== Property 5d: password configs masked in list queries ==========

    /**
     * When password-type configs are queried via list methods, configValue is "******".
     *
     * Feature: mars-integration-optimization, Property 5: Password 类型配置加密与脱敏
     * Validates: Requirements 6.3
     */
    @Property(tries = 100)
    @Tag("Feature_mars-integration-optimization")
    @Tag("Property_5_Password类型配置加密与脱敏")
    void getList_passwordConfig_returnsMaskedValue(@ForAll("nonNullPlaintexts") String plaintext) throws Exception {
        SystemConfigService service = createServiceWithMockMapper();
        SystemConfigMapper mockMapper = extractMockMapper(service);

        // Simulate a password config stored in DB with encrypted value
        String encryptedValue = AesEncryptUtil.encrypt(plaintext);
        SystemConfig dbConfig = new SystemConfig();
        dbConfig.setId(1L);
        dbConfig.setConfigKey("test.password.key");
        dbConfig.setConfigValue(encryptedValue);
        dbConfig.setConfigType("password");
        dbConfig.setConfigDesc("test");
        dbConfig.setConfigGroup("默认");

        when(mockMapper.selectList(any())).thenReturn(Arrays.asList(dbConfig));

        List<SystemConfig> result = service.getList(null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getConfigValue()).isEqualTo("******");
    }

    // ========== Providers ==========

    @Provide
    Arbitrary<String> nonNullPlaintexts() {
        return Arbitraries.oneOf(
                // Random alphanumeric strings of various lengths
                Arbitraries.strings().ofMinLength(1).ofMaxLength(100)
                        .filter(s -> !s.isEmpty()),
                // Typical password-like strings
                Arbitraries.of(
                        "password123", "P@ssw0rd!", "admin", "secret",
                        "MyS3cur3P@ss", "a", "12345678",
                        "特殊密码!@#$%", "hello world", "   spaces   ",
                        "very-long-password-that-goes-on-and-on-and-on-1234567890"
                )
        );
    }
}
