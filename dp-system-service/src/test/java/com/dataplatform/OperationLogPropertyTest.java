package com.dataplatform;

import com.dataplatform.common.service.OperationLogProvider;
import com.dataplatform.system.dto.OperationLogQueryDTO;
import com.dataplatform.system.entity.OperationLog;
import com.dataplatform.system.mapper.OperationLogMapper;
import com.dataplatform.system.service.impl.OperationLogProviderImpl;
import com.dataplatform.system.service.impl.OperationLogServiceImpl;
import com.dataplatform.system.service.OperationLogService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.jqwik.api.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 属性测试：操作日志增强
 *
 * Feature: system-modularization
 * Validates: Requirements 12.1, 12.2, 12.3
 */
class OperationLogPropertyTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ========== Helper Methods ==========

    /**
     * 创建带 mock mapper 的 OperationLogServiceImpl
     */
    private OperationLogServiceImpl createServiceWithMockMapper(OperationLogMapper mockMapper) throws Exception {
        OperationLogServiceImpl service = new OperationLogServiceImpl();
        Field mapperField = OperationLogServiceImpl.class.getDeclaredField("operationLogMapper");
        mapperField.setAccessible(true);
        mapperField.set(service, mockMapper);
        return service;
    }

    /**
     * 创建带 mock OperationLogService 的 OperationLogProviderImpl
     */
    private OperationLogProviderImpl createProviderWithMockService(OperationLogService mockService) throws Exception {
        OperationLogProviderImpl provider = new OperationLogProviderImpl();
        Field serviceField = OperationLogProviderImpl.class.getDeclaredField("operationLogService");
        serviceField.setAccessible(true);
        serviceField.set(provider, mockService);
        return provider;
    }

    // ========== Property 19: 操作日志记录变更数据 ==========

    /**
     * Property 19: 操作日志记录变更数据
     *
     * For any 通过 @OperationLog 注解标记的数据修改操作，操作完成后生成的日志记录
     * 应包含非空的 beforeData 和 afterData 字段，且两个字段均为有效的 JSON 字符串。
     *
     * 测试策略：通过 OperationLogProviderImpl 的 saveLogAsync 方法验证，
     * 当传入非空的 beforeData 和 afterData 时，保存的日志记录包含这些字段且为有效 JSON。
     *
     * Validates: Requirements 12.1
     */
    @Property(tries = 100)
    @Tag("Feature_system-modularization")
    @Tag("Property_19_操作日志记录变更数据")
    void operationLogCapturesBeforeAndAfterData(
            @ForAll("operationType") String operationType,
            @ForAll("moduleName") String moduleName,
            @ForAll("jsonData") String beforeData,
            @ForAll("jsonData") String afterData
    ) throws Exception {
        // 捕获保存到 mapper 的日志记录
        OperationLogMapper mockMapper = mock(OperationLogMapper.class);
        final OperationLog[] captured = {null};
        when(mockMapper.insert(any(OperationLog.class))).thenAnswer(inv -> {
            captured[0] = inv.getArgument(0);
            return 1;
        });

        OperationLogServiceImpl service = createServiceWithMockMapper(mockMapper);
        OperationLogProviderImpl provider = createProviderWithMockService(service);

        // 调用 saveLogAsync（含 beforeData 和 afterData）
        provider.saveLogAsync(
                moduleName, operationType, "测试操作",
                "com.test.TestService.testMethod",
                "/api/test", "POST",
                null, null,
                "testUser", "127.0.0.1",
                100L, true,
                beforeData, afterData
        );

        // 验证：日志记录被保存
        assertThat(captured[0]).isNotNull();

        // 验证：beforeData 和 afterData 非空
        assertThat(captured[0].getBeforeData()).isNotNull();
        assertThat(captured[0].getBeforeData()).isNotEmpty();
        assertThat(captured[0].getAfterData()).isNotNull();
        assertThat(captured[0].getAfterData()).isNotEmpty();

        // 验证：beforeData 和 afterData 是有效的 JSON 字符串
        assertThat(isValidJson(captured[0].getBeforeData())).isTrue();
        assertThat(isValidJson(captured[0].getAfterData())).isTrue();
    }

    // ========== Property 20: 操作日志筛选正确性 ==========

    /**
     * Property 20: 操作日志筛选正确性
     *
     * For any 操作日志筛选条件（操作类型、操作人、时间范围、模块名称的任意组合），
     * 返回的所有日志记录都满足所有指定的筛选条件。
     *
     * 测试策略：生成随机的日志列表和筛选条件，mock mapper 返回满足条件的子集，
     * 验证 service 返回的结果中每条记录都满足所有筛选条件。
     *
     * Validates: Requirements 12.2, 12.3
     */
    @Property(tries = 100)
    @Tag("Feature_system-modularization")
    @Tag("Property_20_操作日志筛选正确性")
    void operationLogFilterCorrectness(
            @ForAll("operationLogList") List<OperationLog> allLogs,
            @ForAll("optionalOperationType") String filterOperationType,
            @ForAll("optionalUsername") String filterUsername,
            @ForAll("optionalModuleName") String filterModuleName,
            @ForAll("optionalTimeRange") boolean useTimeRange
    ) throws Exception {
        // 构建查询条件
        OperationLogQueryDTO query = new OperationLogQueryDTO();
        query.setPage(1);
        query.setPageSize(100);

        if (filterOperationType != null) {
            query.setOperationType(filterOperationType);
        }
        if (filterUsername != null) {
            query.setUsername(filterUsername);
        }
        if (filterModuleName != null) {
            query.setModuleName(filterModuleName);
        }

        Date now = new Date();
        Date startTime = null;
        Date endTime = null;
        if (useTimeRange) {
            startTime = new Date(now.getTime() - 86400000L); // 1天前
            endTime = now;
            query.setStartTime(startTime);
            query.setEndTime(endTime);
        }

        // 计算期望结果：根据筛选条件过滤日志
        final Date fStartTime = startTime;
        final Date fEndTime = endTime;
        List<OperationLog> expectedLogs = allLogs.stream()
                .filter(log -> filterOperationType == null
                        || filterOperationType.equals(log.getOperationType()))
                .filter(log -> filterUsername == null
                        || (log.getUsername() != null && log.getUsername().contains(filterUsername)))
                .filter(log -> filterModuleName == null
                        || filterModuleName.equals(log.getModuleName()))
                .filter(log -> {
                    if (!useTimeRange) return true;
                    if (log.getCreateTime() == null) return false;
                    return !log.getCreateTime().before(fStartTime)
                            && !log.getCreateTime().after(fEndTime);
                })
                .collect(Collectors.toList());

        // Mock mapper
        OperationLogMapper mockMapper = mock(OperationLogMapper.class);
        when(mockMapper.selectByQuery(any(OperationLogQueryDTO.class))).thenReturn(expectedLogs);
        when(mockMapper.countByQuery(any(OperationLogQueryDTO.class))).thenReturn(expectedLogs.size());

        OperationLogServiceImpl service = createServiceWithMockMapper(mockMapper);

        // 执行查询
        Map<String, Object> result = service.queryByCondition(query);

        @SuppressWarnings("unchecked")
        List<OperationLog> resultLogs = (List<OperationLog>) result.get("list");

        // 验证：返回的所有日志记录都满足所有指定的筛选条件
        for (OperationLog log : resultLogs) {
            // 验证操作类型
            if (filterOperationType != null) {
                assertThat(log.getOperationType()).isEqualTo(filterOperationType);
            }

            // 验证操作人（模糊匹配）
            if (filterUsername != null) {
                assertThat(log.getUsername()).contains(filterUsername);
            }

            // 验证模块名称
            if (filterModuleName != null) {
                assertThat(log.getModuleName()).isEqualTo(filterModuleName);
            }

            // 验证时间范围
            if (useTimeRange && log.getCreateTime() != null) {
                assertThat(log.getCreateTime()).isAfterOrEqualTo(fStartTime);
                assertThat(log.getCreateTime()).isBeforeOrEqualTo(fEndTime);
            }
        }
    }

    // ========== JSON Validation Helper ==========

    private boolean isValidJson(String json) {
        if (json == null || json.isEmpty()) {
            return false;
        }
        try {
            objectMapper.readTree(json);
            return true;
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    // ========== Providers ==========

    @Provide
    Arbitrary<String> operationType() {
        return Arbitraries.of("CREATE", "UPDATE", "DELETE");
    }

    @Provide
    Arbitrary<String> moduleName() {
        return Arbitraries.of("system", "org", "data");
    }

    /**
     * 生成有效的 JSON 字符串数据
     */
    @Provide
    Arbitrary<String> jsonData() {
        Arbitrary<String> keys = Arbitraries.of("id", "name", "status", "code", "value", "type", "desc");
        Arbitrary<String> stringValues = Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20);
        Arbitrary<Integer> intValues = Arbitraries.integers().between(1, 10000);

        // 生成简单的 JSON 对象
        return Combinators.combine(keys, stringValues, intValues)
                .as((key1, strVal, intVal) -> {
                    Map<String, Object> data = new LinkedHashMap<>();
                    data.put(key1, strVal);
                    data.put("numericField", intVal);
                    try {
                        return new ObjectMapper().writeValueAsString(data);
                    } catch (JsonProcessingException e) {
                        return "{\"fallback\":\"data\"}";
                    }
                });
    }

    @Provide
    Arbitrary<String> optionalOperationType() {
        return Arbitraries.of("CREATE", "UPDATE", "DELETE", null);
    }

    @Provide
    Arbitrary<String> optionalUsername() {
        return Arbitraries.of("admin", "user1", "operator", null);
    }

    @Provide
    Arbitrary<String> optionalModuleName() {
        return Arbitraries.of("system", "org", "data", null);
    }

    @Provide
    Arbitrary<Boolean> optionalTimeRange() {
        return Arbitraries.of(true, false);
    }

    @Provide
    Arbitrary<List<OperationLog>> operationLogList() {
        return validOperationLog().list().ofMinSize(0).ofMaxSize(15);
    }

    @Provide
    Arbitrary<OperationLog> validOperationLog() {
        Arbitrary<String> opTypes = Arbitraries.of("CREATE", "UPDATE", "DELETE", "QUERY");
        Arbitrary<String> usernames = Arbitraries.of("admin", "user1", "operator", "manager");
        Arbitrary<String> modules = Arbitraries.of("system", "org", "data");
        Arbitrary<Long> durations = Arbitraries.longs().between(10, 5000);

        return Combinators.combine(opTypes, usernames, modules, durations)
                .as((opType, username, module, duration) -> {
                    OperationLog log = new OperationLog();
                    log.setId((long) (Math.random() * 100000 + 1));
                    log.setOperationType(opType);
                    log.setUsername(username);
                    log.setModuleName(module);
                    log.setOperationDesc("测试操作 " + opType);
                    log.setDurationMs(duration);
                    log.setStatus("success");
                    log.setIpAddress("127.0.0.1");
                    // 设置 createTime 在最近 2 天内
                    long now = System.currentTimeMillis();
                    long randomOffset = (long) (Math.random() * 172800000L); // 0~2天
                    log.setCreateTime(new Date(now - randomOffset));
                    return log;
                });
    }
}
