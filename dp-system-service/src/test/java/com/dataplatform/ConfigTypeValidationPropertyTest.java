package com.dataplatform;

import com.dataplatform.common.exception.BusinessException;
import com.dataplatform.common.exception.ErrorCode;
import com.dataplatform.system.entity.SystemConfig;
import com.dataplatform.system.mapper.SystemConfigMapper;
import com.dataplatform.system.service.ConfigCacheService;
import com.dataplatform.system.service.SystemConfigService;
import net.jqwik.api.*;
import org.mockito.Mockito;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 属性测试：配置类型值校验正确性
 *
 * Feature: mars-integration-optimization, Property 4: 配置类型值校验正确性
 * Validates: Requirements 6.1, 6.2
 */
class ConfigTypeValidationPropertyTest {

    private SystemConfigService createServiceWithMockMapper() throws Exception {
        SystemConfigService service = new SystemConfigService();
        SystemConfigMapper mockMapper = Mockito.mock(SystemConfigMapper.class);
        when(mockMapper.selectByKey(any())).thenReturn(null);
        when(mockMapper.insert(any(SystemConfig.class))).thenReturn(1);
        Field mapperField = SystemConfigService.class.getDeclaredField("systemConfigMapper");
        mapperField.setAccessible(true);
        mapperField.set(service, mockMapper);

        ConfigCacheService mockCacheService = Mockito.mock(ConfigCacheService.class);
        Field cacheField = SystemConfigService.class.getDeclaredField("configCacheService");
        cacheField.setAccessible(true);
        cacheField.set(service, mockCacheService);

        return service;
    }

    private SystemConfig buildConfig(String configType, String configValue) {
        SystemConfig config = new SystemConfig();
        config.setConfigKey("test.key." + System.nanoTime());
        config.setConfigValue(configValue);
        config.setConfigType(configType);
        config.setConfigDesc("test");
        config.setConfigGroup("默认");
        config.setIsSystem(false);
        return config;
    }

    // ========== number 类型：有效数字字符串应被接受 ==========

    /**
     * Property 4a: number 类型仅接受可解析为 Double 的字符串
     *
     * Feature: mars-integration-optimization, Property 4: 配置类型值校验正确性
     * Validates: Requirements 6.1, 6.2
     */
    @Property(tries = 100)
    @Tag("Feature_mars-integration-optimization")
    @Tag("Property_4_配置类型值校验正确性")
    void numberType_validDoubles_accepted(@ForAll("validNumberStrings") String numberStr) throws Exception {
        SystemConfigService service = createServiceWithMockMapper();
        SystemConfig config = buildConfig("number", numberStr);

        assertThatCode(() -> service.create(config)).doesNotThrowAnyException();
    }

    // ========== number 类型：非数字字符串应被拒绝 ==========

    /**
     * Property 4b: number 类型拒绝不可解析为 Double 的字符串
     *
     * Feature: mars-integration-optimization, Property 4: 配置类型值校验正确性
     * Validates: Requirements 6.1, 6.2
     */
    @Property(tries = 100)
    @Tag("Feature_mars-integration-optimization")
    @Tag("Property_4_配置类型值校验正确性")
    void numberType_invalidStrings_rejected(@ForAll("invalidNumberStrings") String invalidStr) throws Exception {
        SystemConfigService service = createServiceWithMockMapper();
        SystemConfig config = buildConfig("number", invalidStr);

        assertThatThrownBy(() -> service.create(config))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException bex = (BusinessException) ex;
                    assertThat(bex.getCode()).isEqualTo(ErrorCode.PARAM_ERROR);
                });
    }

    // ========== boolean 类型：true/false（不区分大小写）应被接受 ==========

    /**
     * Property 4c: boolean 类型仅接受 "true"/"false"（不区分大小写）
     *
     * Feature: mars-integration-optimization, Property 4: 配置类型值校验正确性
     * Validates: Requirements 6.1, 6.2
     */
    @Property(tries = 100)
    @Tag("Feature_mars-integration-optimization")
    @Tag("Property_4_配置类型值校验正确性")
    void booleanType_validValues_accepted(@ForAll("validBooleanStrings") String boolStr) throws Exception {
        SystemConfigService service = createServiceWithMockMapper();
        SystemConfig config = buildConfig("boolean", boolStr);

        assertThatCode(() -> service.create(config)).doesNotThrowAnyException();
    }

    // ========== boolean 类型：非 true/false 字符串应被拒绝 ==========

    /**
     * Property 4d: boolean 类型拒绝非 true/false 字符串
     *
     * Feature: mars-integration-optimization, Property 4: 配置类型值校验正确性
     * Validates: Requirements 6.1, 6.2
     */
    @Property(tries = 100)
    @Tag("Feature_mars-integration-optimization")
    @Tag("Property_4_配置类型值校验正确性")
    void booleanType_invalidValues_rejected(@ForAll("invalidBooleanStrings") String invalidStr) throws Exception {
        SystemConfigService service = createServiceWithMockMapper();
        SystemConfig config = buildConfig("boolean", invalidStr);

        assertThatThrownBy(() -> service.create(config))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException bex = (BusinessException) ex;
                    assertThat(bex.getCode()).isEqualTo(ErrorCode.PARAM_ERROR);
                });
    }

    // ========== json 类型：有效 JSON 应被接受 ==========

    /**
     * Property 4e: json 类型仅接受有效 JSON 字符串
     *
     * Feature: mars-integration-optimization, Property 4: 配置类型值校验正确性
     * Validates: Requirements 6.1, 6.2
     */
    @Property(tries = 100)
    @Tag("Feature_mars-integration-optimization")
    @Tag("Property_4_配置类型值校验正确性")
    void jsonType_validJson_accepted(@ForAll("validJsonStrings") String jsonStr) throws Exception {
        SystemConfigService service = createServiceWithMockMapper();
        SystemConfig config = buildConfig("json", jsonStr);

        assertThatCode(() -> service.create(config)).doesNotThrowAnyException();
    }

    // ========== json 类型：无效 JSON 应被拒绝 ==========

    /**
     * Property 4f: json 类型拒绝无效 JSON 字符串
     *
     * Feature: mars-integration-optimization, Property 4: 配置类型值校验正确性
     * Validates: Requirements 6.1, 6.2
     */
    @Property(tries = 100)
    @Tag("Feature_mars-integration-optimization")
    @Tag("Property_4_配置类型值校验正确性")
    void jsonType_invalidJson_rejected(@ForAll("invalidJsonStrings") String invalidStr) throws Exception {
        SystemConfigService service = createServiceWithMockMapper();
        SystemConfig config = buildConfig("json", invalidStr);

        assertThatThrownBy(() -> service.create(config))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException bex = (BusinessException) ex;
                    assertThat(bex.getCode()).isEqualTo(ErrorCode.PARAM_ERROR);
                });
    }

    // ========== string/password 类型：接受任意字符串 ==========

    /**
     * Property 4g: string 和 password 类型接受任意字符串
     *
     * Feature: mars-integration-optimization, Property 4: 配置类型值校验正确性
     * Validates: Requirements 6.1, 6.2
     */
    @Property(tries = 100)
    @Tag("Feature_mars-integration-optimization")
    @Tag("Property_4_配置类型值校验正确性")
    void stringAndPasswordType_anyValue_accepted(
            @ForAll("stringOrPasswordType") String configType,
            @ForAll("arbitraryStrings") String value
    ) throws Exception {
        SystemConfigService service = createServiceWithMockMapper();
        SystemConfig config = buildConfig(configType, value);

        assertThatCode(() -> service.create(config)).doesNotThrowAnyException();
    }

    // ========== Providers ==========

    @Provide
    Arbitrary<String> validNumberStrings() {
        return Arbitraries.oneOf(
                // integers
                Arbitraries.integers().between(-999999, 999999).map(String::valueOf),
                // doubles
                Arbitraries.doubles().between(-1e6, 1e6)
                        .filter(d -> !d.isNaN() && !d.isInfinite())
                        .map(String::valueOf),
                // edge cases
                Arbitraries.of("0", "-0", "0.0", "1", "-1", "3.14", "-3.14",
                        "1e10", "1E10", "1.5e3", "999999999", "-999999999",
                        "0.001", "1234567890")
        );
    }

    @Provide
    Arbitrary<String> invalidNumberStrings() {
        return Arbitraries.oneOf(
                Arbitraries.of("abc", "12.34.56", "1,000", "one", "true",
                        "null", "", " ", "1.2.3", "+-5", "12abc", "NaN_str",
                        "hello123", "3.14.159", "12 34"),
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(10)
        );
    }

    @Provide
    Arbitrary<String> validBooleanStrings() {
        return Arbitraries.of(
                "true", "false", "TRUE", "FALSE", "True", "False",
                "tRuE", "fAlSe", "TrUe", "FaLsE"
        );
    }

    @Provide
    Arbitrary<String> invalidBooleanStrings() {
        return Arbitraries.of(
                "yes", "no", "1", "0", "on", "off", "t", "f",
                "truee", "fals", "tru", "falsee", "", " ", "null",
                "TRUE1", "abc", "truefalse"
        );
    }

    @Provide
    Arbitrary<String> validJsonStrings() {
        return Arbitraries.oneOf(
                // JSON objects
                Arbitraries.of(
                        "{}", "{\"key\":\"value\"}", "{\"a\":1,\"b\":2}",
                        "{\"nested\":{\"inner\":true}}",
                        "{\"arr\":[1,2,3]}"
                ),
                // JSON arrays
                Arbitraries.of(
                        "[]", "[1,2,3]", "[\"a\",\"b\"]",
                        "[{\"id\":1},{\"id\":2}]"
                ),
                // JSON primitives (valid JSON per Jackson)
                Arbitraries.of("\"hello\"", "123", "true", "false", "null")
        );
    }

    @Provide
    Arbitrary<String> invalidJsonStrings() {
        return Arbitraries.of(
                "{key:value}", "{\"key\":}", "{'key':'value'}",
                "{\"unclosed\":\"str", "[1,2,", "}{",
                "not json at all", "undefined",
                "abc def", "<<>>", "{[}]"
        );
    }

    @Provide
    Arbitrary<String> stringOrPasswordType() {
        return Arbitraries.of("string", "password");
    }

    @Provide
    Arbitrary<String> arbitraryStrings() {
        return Arbitraries.oneOf(
                Arbitraries.strings().ofMinLength(0).ofMaxLength(100),
                Arbitraries.of("", " ", "hello", "123", "true", "{}", "[]",
                        "any random value", "特殊字符!@#$%")
        );
    }
}
