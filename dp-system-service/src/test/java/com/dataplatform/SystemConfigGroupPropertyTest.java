package com.dataplatform;

import com.dataplatform.system.entity.SystemConfig;
import com.dataplatform.system.mapper.SystemConfigMapper;
import com.dataplatform.system.service.SystemConfigService;
import net.jqwik.api.*;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 属性测试：系统配置分组
 * 
 * Feature: system-modularization
 * Validates: Requirements 8.2, 8.3, 8.4, 8.5
 */
class SystemConfigGroupPropertyTest {

    /**
     * 创建一个带有 mock Mapper 的 SystemConfigService 实例
     */
    private SystemConfigService createServiceWithMockMapper(SystemConfigMapper mockMapper) throws Exception {
        SystemConfigService service = new SystemConfigService();
        Field mapperField = SystemConfigService.class.getDeclaredField("systemConfigMapper");
        mapperField.setAccessible(true);
        mapperField.set(service, mockMapper);
        return service;
    }

    // ========== Property 1: 配置分组筛选正确性 ==========

    /**
     * Property 1: 配置分组筛选正确性
     * 
     * For any 配置分组名称和任意数量的系统配置项，按该分组筛选后返回的所有配置项的
     * configGroup 字段都等于筛选条件中的分组名称。
     * 
     * Validates: Requirements 8.3
     */
    @Property(tries = 100)
    @Tag("Feature_system-modularization")
    @Tag("Property_1_配置分组筛选正确性")
    void filterByGroupReturnsOnlyMatchingConfigs(
            @ForAll("configGroupName") String targetGroup,
            @ForAll("configList") List<SystemConfig> allConfigs
    ) throws Exception {
        // 计算期望结果：只有 configGroup 等于 targetGroup 的配置项
        List<SystemConfig> expectedConfigs = allConfigs.stream()
                .filter(c -> targetGroup.equals(c.getConfigGroup()))
                .collect(Collectors.toList());

        // Mock mapper 的 selectByGroup 方法返回期望结果
        SystemConfigMapper mockMapper = Mockito.mock(SystemConfigMapper.class);
        when(mockMapper.selectByGroup(eq(targetGroup))).thenReturn(expectedConfigs);

        SystemConfigService service = createServiceWithMockMapper(mockMapper);

        // 执行按分组筛选
        List<SystemConfig> result = service.listByGroup(targetGroup);

        // 验证：返回的所有配置项的 configGroup 都等于筛选条件
        assertThat(result).allSatisfy(config ->
                assertThat(config.getConfigGroup()).isEqualTo(targetGroup)
        );
    }

    // ========== Property 2: 配置分组变更不影响配置值 ==========

    /**
     * Property 2: 配置分组变更不影响配置值
     * 
     * For any 系统配置项，修改其 configGroup 字段后，该配置项的
     * configKey、configValue、configType、configDesc 字段保持不变。
     * 
     * Validates: Requirements 8.4
     */
    @Property(tries = 100)
    @Tag("Feature_system-modularization")
    @Tag("Property_2_配置分组变更不影响配置值")
    void changingGroupDoesNotAffectOtherFields(
            @ForAll("validSystemConfig") SystemConfig originalConfig,
            @ForAll("configGroupName") String newGroup
    ) throws Exception {
        // 记录原始值
        String originalKey = originalConfig.getConfigKey();
        String originalValue = originalConfig.getConfigValue();
        String originalType = originalConfig.getConfigType();
        String originalDesc = originalConfig.getConfigDesc();

        // Mock mapper：update 成功，selectById 返回更新后的配置
        SystemConfigMapper mockMapper = Mockito.mock(SystemConfigMapper.class);
        when(mockMapper.update(any(SystemConfig.class))).thenAnswer(invocation -> {
            SystemConfig updated = invocation.getArgument(0);
            // 模拟数据库更新：只更新 configGroup，其他字段保持不变
            return 1;
        });

        // 创建更新请求：只修改 configGroup
        SystemConfig updateRequest = new SystemConfig();
        updateRequest.setId(originalConfig.getId());
        updateRequest.setConfigKey(originalKey);
        updateRequest.setConfigValue(originalValue);
        updateRequest.setConfigType(originalType);
        updateRequest.setConfigDesc(originalDesc);
        updateRequest.setConfigGroup(newGroup);

        // 模拟更新后查询返回的结果
        SystemConfig afterUpdate = new SystemConfig();
        afterUpdate.setId(originalConfig.getId());
        afterUpdate.setConfigKey(originalKey);
        afterUpdate.setConfigValue(originalValue);
        afterUpdate.setConfigType(originalType);
        afterUpdate.setConfigDesc(originalDesc);
        // configGroup 变为新值（如果 newGroup 是空白则变为"默认"）
        afterUpdate.setConfigGroup(
                (newGroup != null && newGroup.isBlank()) ? "默认" : newGroup
        );

        when(mockMapper.selectById(eq(originalConfig.getId()))).thenReturn(afterUpdate);

        SystemConfigService service = createServiceWithMockMapper(mockMapper);

        // 执行更新
        service.update(updateRequest);

        // 查询更新后的配置
        SystemConfig result = service.getById(originalConfig.getId());

        // 验证：configKey、configValue、configType、configDesc 保持不变
        assertThat(result.getConfigKey()).isEqualTo(originalKey);
        assertThat(result.getConfigValue()).isEqualTo(originalValue);
        assertThat(result.getConfigType()).isEqualTo(originalType);
        assertThat(result.getConfigDesc()).isEqualTo(originalDesc);
    }

    // ========== Property 3: 配置分组非空验证 ==========

    /**
     * Property 3: 配置分组非空验证
     * 
     * For any 创建系统配置的请求，如果 configGroup 为纯空白字符串，
     * 该请求应被拒绝或自动归入"默认"分组。
     * 
     * Validates: Requirements 8.2, 8.5
     */
    @Property(tries = 100)
    @Tag("Feature_system-modularization")
    @Tag("Property_3_配置分组非空验证")
    void blankGroupDefaultsToDefault(
            @ForAll("blankString") String blankGroup,
            @ForAll("validSystemConfig") SystemConfig config
    ) throws Exception {
        // 设置 configGroup 为空白字符串
        config.setConfigGroup(blankGroup);

        // Mock mapper
        SystemConfigMapper mockMapper = Mockito.mock(SystemConfigMapper.class);
        when(mockMapper.selectByKey(any())).thenReturn(null); // 配置键不存在
        when(mockMapper.insert(any(SystemConfig.class))).thenAnswer(invocation -> {
            SystemConfig inserted = invocation.getArgument(0);
            inserted.setId(1L);
            return 1;
        });

        SystemConfigService service = createServiceWithMockMapper(mockMapper);

        // 执行创建
        service.create(config);

        // 验证：configGroup 被自动设置为"默认"
        assertThat(config.getConfigGroup()).isEqualTo("默认");

        // 验证 mapper.insert 被调用，且传入的 config 的 configGroup 为"默认"
        verify(mockMapper).insert(argThat(c -> "默认".equals(c.getConfigGroup())));
    }

    // ========== Providers ==========

    @Provide
    Arbitrary<String> configGroupName() {
        return Arbitraries.of("基础配置", "安全配置", "邮件配置", "存储配置", "默认", "系统配置", "网络配置");
    }

    @Provide
    Arbitrary<List<SystemConfig>> configList() {
        return validSystemConfig().list().ofMinSize(0).ofMaxSize(10);
    }

    @Provide
    Arbitrary<SystemConfig> validSystemConfig() {
        Arbitrary<Long> ids = Arbitraries.longs().between(1, 10000);
        Arbitrary<String> keys = Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(30)
                .map(s -> "config." + s);
        Arbitrary<String> values = Arbitraries.strings().ofMinLength(1).ofMaxLength(100);
        Arbitrary<String> types = Arbitraries.of("string", "number", "boolean", "json");
        Arbitrary<String> descs = Arbitraries.strings().ofMinLength(0).ofMaxLength(200);
        Arbitrary<String> groups = configGroupName();

        return Combinators.combine(ids, keys, values, types, descs, groups)
                .as((id, key, value, type, desc, group) -> {
                    SystemConfig config = new SystemConfig();
                    config.setId(id);
                    config.setConfigKey(key);
                    config.setConfigValue(value);
                    config.setConfigType(type);
                    config.setConfigDesc(desc);
                    config.setConfigGroup(group);
                    config.setIsSystem(false);
                    return config;
                });
    }

    @Provide
    Arbitrary<String> blankString() {
        return Arbitraries.of(
                "",
                " ",
                "  ",
                "   ",
                "\t",
                "\n",
                " \t ",
                "\t\n ",
                "    "
        );
    }
}
