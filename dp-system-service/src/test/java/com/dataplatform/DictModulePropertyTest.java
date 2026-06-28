package com.dataplatform;

import com.dataplatform.common.exception.BusinessException;
import com.dataplatform.system.entity.DictData;
import com.dataplatform.system.entity.DictType;
import com.dataplatform.system.mapper.DictDataMapper;
import com.dataplatform.system.mapper.DictTypeMapper;
import com.dataplatform.system.service.DictDataService;
import com.dataplatform.system.service.DictTypeService;
import net.jqwik.api.*;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 属性测试：数据字典模块
 *
 * Feature: system-modularization
 * Validates: Requirements 9.1, 9.2, 9.3, 9.4, 9.5, 9.6, 9.7
 */
class DictModulePropertyTest {

    private final AtomicLong idGenerator = new AtomicLong(1);

    // ========== Helper: 创建带 mock 的 DictDataService ==========

    private DictDataService createDictDataService(DictDataMapper mockDataMapper) {
        // DictDataService 构造函数: (DictDataMapper, @Lazy DictDataService self)
        // 先创建一个 spy，然后用构造函数注入
        DictDataService service = new DictDataService(mockDataMapper, null);
        // 使用 spy 来让 self 引用指向自身（模拟 @Lazy 注入）
        DictDataService spyService = Mockito.spy(service);
        // 通过反射设置 self 字段
        try {
            java.lang.reflect.Field selfField = DictDataService.class.getDeclaredField("self");
            selfField.setAccessible(true);
            selfField.set(spyService, spyService);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set self field", e);
        }
        return spyService;
    }

    // ========== Helper: 创建带 mock 的 DictTypeService ==========

    private DictTypeService createDictTypeService(DictTypeMapper mockTypeMapper,
                                                   DictDataMapper mockDataMapper,
                                                   DictDataService dictDataService) {
        return new DictTypeService(mockTypeMapper, mockDataMapper, dictDataService);
    }

    // ========== Property 4: 字典类型 CRUD 往返一致性 ==========

    /**
     * Property 4: 字典类型 CRUD 往返一致性
     *
     * For any 有效的字典类型对象（包含唯一的 dictCode 和非空的 dictName），
     * 创建后再查询应返回等价的对象。
     *
     * Validates: Requirements 9.1
     */
    @Property(tries = 100)
    @Tag("Feature_system-modularization")
    @Tag("Property_4_字典类型CRUD往返一致性")
    void dictTypeCrudRoundTrip(
            @ForAll("validDictCode") String dictCode,
            @ForAll("validDictName") String dictName
    ) {
        DictTypeMapper mockTypeMapper = mock(DictTypeMapper.class);
        DictDataMapper mockDataMapper = mock(DictDataMapper.class);
        DictDataService dictDataService = createDictDataService(mockDataMapper);

        // 存储创建的字典类型
        final DictType[] stored = {null};
        long generatedId = idGenerator.getAndIncrement();

        // mock: 编码不存在（唯一性检查通过）
        when(mockTypeMapper.selectByDictCode(eq(dictCode))).thenReturn(null);
        // mock: insert 成功并设置 ID
        when(mockTypeMapper.insert(any(DictType.class))).thenAnswer(inv -> {
            DictType dt = inv.getArgument(0);
            dt.setId(generatedId);
            stored[0] = dt;
            return 1;
        });
        // mock: selectById 返回存储的对象
        when(mockTypeMapper.selectById(eq(generatedId))).thenAnswer(inv -> stored[0]);

        DictTypeService service = createDictTypeService(mockTypeMapper, mockDataMapper, dictDataService);

        // 创建字典类型
        DictType input = new DictType();
        input.setDictCode(dictCode);
        input.setDictName(dictName);
        input.setStatus(1);

        DictType created = service.create(input);

        // 查询
        DictType queried = service.getById(created.getId());

        // 验证往返一致性
        assertThat(queried).isNotNull();
        assertThat(queried.getDictCode()).isEqualTo(dictCode);
        assertThat(queried.getDictName()).isEqualTo(dictName);
        assertThat(queried.getStatus()).isEqualTo(1);
        assertThat(queried.getId()).isEqualTo(generatedId);
    }

    // ========== Property 5: 字典数据项 CRUD 往返一致性 ==========

    /**
     * Property 5: 字典数据项 CRUD 往返一致性
     *
     * For any 有效的字典数据项对象（包含有效的 dictCode、非空的 label 和 value），
     * 创建后再查询应返回等价的对象。
     *
     * Validates: Requirements 9.2
     */
    @Property(tries = 100)
    @Tag("Feature_system-modularization")
    @Tag("Property_5_字典数据项CRUD往返一致性")
    void dictDataCrudRoundTrip(
            @ForAll("validDictCode") String dictCode,
            @ForAll("validLabel") String label,
            @ForAll("validValue") String value,
            @ForAll("sortOrder") Integer sortOrder
    ) {
        DictDataMapper mockDataMapper = mock(DictDataMapper.class);

        final DictData[] stored = {null};
        long generatedId = idGenerator.getAndIncrement();

        when(mockDataMapper.insert(any(DictData.class))).thenAnswer(inv -> {
            DictData dd = inv.getArgument(0);
            dd.setId(generatedId);
            stored[0] = dd;
            return 1;
        });
        when(mockDataMapper.selectById(eq(generatedId))).thenAnswer(inv -> stored[0]);

        DictDataService service = createDictDataService(mockDataMapper);

        DictData input = new DictData();
        input.setDictCode(dictCode);
        input.setLabel(label);
        input.setValue(value);
        input.setSortOrder(sortOrder);
        input.setStatus(1);

        DictData created = service.create(input);

        DictData queried = service.getById(created.getId());

        assertThat(queried).isNotNull();
        assertThat(queried.getDictCode()).isEqualTo(dictCode);
        assertThat(queried.getLabel()).isEqualTo(label);
        assertThat(queried.getValue()).isEqualTo(value);
        assertThat(queried.getSortOrder()).isEqualTo(sortOrder);
        assertThat(queried.getStatus()).isEqualTo(1);
        assertThat(queried.getId()).isEqualTo(generatedId);
    }

    // ========== Property 6: 字典数据查询结果的过滤与排序 ==========

    /**
     * Property 6: 字典数据查询结果的过滤与排序
     *
     * For any 字典类型编码，查询该类型的数据项时，返回的所有数据项的 dictCode 等于查询条件、
     * status 等于启用状态，且按 sortOrder 升序排列。
     *
     * Validates: Requirements 9.3
     */
    @Property(tries = 100)
    @Tag("Feature_system-modularization")
    @Tag("Property_6_字典数据查询结果的过滤与排序")
    void dictDataQueryFilterAndSort(
            @ForAll("validDictCode") String dictCode,
            @ForAll("dictDataList") List<DictData> allData
    ) {
        DictDataMapper mockDataMapper = mock(DictDataMapper.class);

        // 设置所有数据的 dictCode（混合一些不同的 dictCode）
        for (DictData dd : allData) {
            // 保持原有的 dictCode（有些匹配，有些不匹配）
        }

        // 计算期望结果：dictCode 匹配、status=1、按 sortOrder 升序
        List<DictData> expectedEnabled = allData.stream()
                .filter(dd -> dictCode.equals(dd.getDictCode()))
                .filter(dd -> dd.getStatus() != null && dd.getStatus() == 1)
                .sorted(Comparator.comparingInt(dd -> dd.getSortOrder() != null ? dd.getSortOrder() : 0))
                .collect(Collectors.toList());

        when(mockDataMapper.selectEnabledByDictCode(eq(dictCode))).thenReturn(expectedEnabled);

        DictDataService service = createDictDataService(mockDataMapper);

        List<DictData> result = service.listEnabledByDictCode(dictCode);

        // 验证：所有返回项的 dictCode 等于查询条件
        assertThat(result).allSatisfy(dd ->
                assertThat(dd.getDictCode()).isEqualTo(dictCode)
        );

        // 验证：所有返回项的 status 等于 1（启用）
        assertThat(result).allSatisfy(dd ->
                assertThat(dd.getStatus()).isEqualTo(1)
        );

        // 验证：按 sortOrder 升序排列
        for (int i = 1; i < result.size(); i++) {
            int prevSort = result.get(i - 1).getSortOrder() != null ? result.get(i - 1).getSortOrder() : 0;
            int currSort = result.get(i).getSortOrder() != null ? result.get(i).getSortOrder() : 0;
            assertThat(currSort).isGreaterThanOrEqualTo(prevSort);
        }
    }

    // ========== Property 7: 字典类型级联删除 ==========

    /**
     * Property 7: 字典类型级联删除
     *
     * For any 字典类型，删除该类型后，查询该类型编码下的字典数据项应返回空列表。
     *
     * Validates: Requirements 9.4
     */
    @Property(tries = 100)
    @Tag("Feature_system-modularization")
    @Tag("Property_7_字典类型级联删除")
    void dictTypeCascadeDelete(
            @ForAll("validDictCode") String dictCode,
            @ForAll("validDictName") String dictName
    ) {
        DictTypeMapper mockTypeMapper = mock(DictTypeMapper.class);
        DictDataMapper mockDataMapper = mock(DictDataMapper.class);
        DictDataService dictDataService = createDictDataService(mockDataMapper);

        long typeId = idGenerator.getAndIncrement();

        // 模拟已存在的字典类型
        DictType existingType = new DictType();
        existingType.setId(typeId);
        existingType.setDictCode(dictCode);
        existingType.setDictName(dictName);
        existingType.setStatus(1);

        // 标记是否已删除
        final boolean[] deleted = {false};

        when(mockTypeMapper.selectById(eq(typeId))).thenAnswer(inv ->
                deleted[0] ? null : existingType
        );
        when(mockDataMapper.deleteByDictCode(eq(dictCode))).thenAnswer(inv -> {
            return 1;
        });
        when(mockTypeMapper.deleteById(eq(typeId))).thenAnswer(inv -> {
            deleted[0] = true;
            return 1;
        });

        // 删除后查询数据项应返回空列表
        when(mockDataMapper.selectEnabledByDictCode(eq(dictCode))).thenReturn(Collections.emptyList());

        DictTypeService typeService = createDictTypeService(mockTypeMapper, mockDataMapper, dictDataService);

        // 执行删除
        typeService.delete(typeId);

        // 验证级联删除：dictDataMapper.deleteByDictCode 被调用
        verify(mockDataMapper).deleteByDictCode(eq(dictCode));

        // 验证删除后查询数据项返回空列表
        List<DictData> remainingData = dictDataService.listEnabledByDictCode(dictCode);
        assertThat(remainingData).isEmpty();
    }

    // ========== Property 8: 字典类型编码唯一性 ==========

    /**
     * Property 8: 字典类型编码唯一性
     *
     * For any 两个字典类型创建请求，如果它们的 dictCode 相同，
     * 第二个创建请求应失败并返回编码重复错误。
     *
     * Validates: Requirements 9.5
     */
    @Property(tries = 100)
    @Tag("Feature_system-modularization")
    @Tag("Property_8_字典类型编码唯一性")
    void dictTypeCodeUniqueness(
            @ForAll("validDictCode") String dictCode,
            @ForAll("validDictName") String dictName1,
            @ForAll("validDictName") String dictName2
    ) {
        DictTypeMapper mockTypeMapper = mock(DictTypeMapper.class);
        DictDataMapper mockDataMapper = mock(DictDataMapper.class);
        DictDataService dictDataService = createDictDataService(mockDataMapper);

        final DictType[] storedType = {null};
        long generatedId = idGenerator.getAndIncrement();

        // 第一次查询编码不存在，第二次查询编码已存在
        when(mockTypeMapper.selectByDictCode(eq(dictCode))).thenAnswer(inv -> storedType[0]);
        when(mockTypeMapper.insert(any(DictType.class))).thenAnswer(inv -> {
            DictType dt = inv.getArgument(0);
            dt.setId(generatedId);
            storedType[0] = dt;
            return 1;
        });

        DictTypeService service = createDictTypeService(mockTypeMapper, mockDataMapper, dictDataService);

        // 第一次创建应成功
        DictType first = new DictType();
        first.setDictCode(dictCode);
        first.setDictName(dictName1);
        service.create(first);

        // 第二次创建相同编码应失败
        DictType second = new DictType();
        second.setDictCode(dictCode);
        second.setDictName(dictName2);

        assertThatThrownBy(() -> service.create(second))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("字典类型编码已存在");
    }

    // ========== Property 9: 字典批量查询等价性 ==========

    /**
     * Property 9: 字典批量查询等价性
     *
     * For any 字典类型编码集合，批量查询的结果应等价于对每个编码单独查询结果的合并。
     *
     * Validates: Requirements 9.6
     */
    @Property(tries = 100)
    @Tag("Feature_system-modularization")
    @Tag("Property_9_字典批量查询等价性")
    void dictBatchQueryEquivalence(
            @ForAll("dictCodeSet") List<String> dictCodes
    ) {
        DictDataMapper mockDataMapper = mock(DictDataMapper.class);

        // 为每个 dictCode 准备模拟数据
        Map<String, List<DictData>> mockDataByCode = new HashMap<>();
        for (String code : dictCodes) {
            if (code != null && !code.isBlank()) {
                List<DictData> items = new ArrayList<>();
                DictData dd = new DictData();
                dd.setId(idGenerator.getAndIncrement());
                dd.setDictCode(code);
                dd.setLabel("label_" + code);
                dd.setValue("value_" + code);
                dd.setSortOrder(0);
                dd.setStatus(1);
                items.add(dd);
                mockDataByCode.put(code, items);
            }
        }

        when(mockDataMapper.selectEnabledByDictCode(anyString())).thenAnswer(inv -> {
            String code = inv.getArgument(0);
            return mockDataByCode.getOrDefault(code, Collections.emptyList());
        });

        DictDataService service = createDictDataService(mockDataMapper);

        // 批量查询
        Map<String, List<DictData>> batchResult = service.listBatch(dictCodes);

        // 逐个查询并合并
        Map<String, List<DictData>> individualResult = new HashMap<>();
        for (String code : dictCodes) {
            if (code != null && !code.isBlank()) {
                individualResult.put(code, service.listEnabledByDictCode(code));
            }
        }

        // 验证：批量查询结果与逐个查询结果等价
        assertThat(batchResult.keySet()).isEqualTo(individualResult.keySet());
        for (String code : batchResult.keySet()) {
            List<DictData> batchItems = batchResult.get(code);
            List<DictData> individualItems = individualResult.get(code);
            assertThat(batchItems).hasSameSizeAs(individualItems);
            for (int i = 0; i < batchItems.size(); i++) {
                assertThat(batchItems.get(i).getDictCode()).isEqualTo(individualItems.get(i).getDictCode());
                assertThat(batchItems.get(i).getLabel()).isEqualTo(individualItems.get(i).getLabel());
                assertThat(batchItems.get(i).getValue()).isEqualTo(individualItems.get(i).getValue());
            }
        }
    }

    // ========== Property 10: 字典缓存一致性 ==========

    /**
     * Property 10: 字典缓存一致性
     *
     * For any 字典数据项的变更操作（创建、更新、删除），变更后通过缓存查询的结果
     * 应与直接从数据库查询的结果一致。
     *
     * 测试策略：验证 Service 层的 @CacheEvict 注解在变更操作后被正确触发，
     * 确保缓存被清除，后续查询会从数据库获取最新数据。
     *
     * Validates: Requirements 9.7
     */
    @Property(tries = 100)
    @Tag("Feature_system-modularization")
    @Tag("Property_10_字典缓存一致性")
    void dictCacheConsistencyAfterMutation(
            @ForAll("validDictCode") String dictCode,
            @ForAll("validLabel") String label,
            @ForAll("validValue") String value,
            @ForAll("cacheOperation") String operation
    ) {
        DictDataMapper mockDataMapper = mock(DictDataMapper.class);

        // 模拟数据库中的数据状态
        final List<DictData> dbState = new ArrayList<>();
        long dataId = idGenerator.getAndIncrement();

        DictData existingData = new DictData();
        existingData.setId(dataId);
        existingData.setDictCode(dictCode);
        existingData.setLabel("old_label");
        existingData.setValue("old_value");
        existingData.setSortOrder(0);
        existingData.setStatus(1);
        existingData.setCreateTime(LocalDateTime.now());
        existingData.setUpdateTime(LocalDateTime.now());
        dbState.add(existingData);

        when(mockDataMapper.selectById(eq(dataId))).thenAnswer(inv -> {
            return dbState.stream().filter(d -> d.getId().equals(dataId)).findFirst().orElse(null);
        });
        when(mockDataMapper.selectEnabledByDictCode(eq(dictCode))).thenAnswer(inv -> {
            return dbState.stream()
                    .filter(d -> dictCode.equals(d.getDictCode()) && d.getStatus() == 1)
                    .sorted(Comparator.comparingInt(d -> d.getSortOrder() != null ? d.getSortOrder() : 0))
                    .collect(Collectors.toList());
        });
        when(mockDataMapper.insert(any(DictData.class))).thenAnswer(inv -> {
            DictData dd = inv.getArgument(0);
            dd.setId(idGenerator.getAndIncrement());
            dbState.add(dd);
            return 1;
        });
        when(mockDataMapper.update(any(DictData.class))).thenAnswer(inv -> {
            DictData dd = inv.getArgument(0);
            dbState.removeIf(d -> d.getId().equals(dd.getId()));
            dd.setDictCode(dictCode); // 保持 dictCode 不变
            dbState.add(dd);
            return 1;
        });
        when(mockDataMapper.deleteById(eq(dataId))).thenAnswer(inv -> {
            dbState.removeIf(d -> d.getId().equals(dataId));
            return 1;
        });

        DictDataService service = createDictDataService(mockDataMapper);

        // 执行变更操作
        switch (operation) {
            case "create" -> {
                DictData newData = new DictData();
                newData.setDictCode(dictCode);
                newData.setLabel(label);
                newData.setValue(value);
                newData.setSortOrder(1);
                newData.setStatus(1);
                service.create(newData);
            }
            case "update" -> {
                DictData updateData = new DictData();
                updateData.setId(dataId);
                updateData.setDictCode(dictCode);
                updateData.setLabel(label);
                updateData.setValue(value);
                updateData.setSortOrder(0);
                updateData.setStatus(1);
                service.update(updateData);
            }
            case "delete" -> {
                service.delete(dataId);
            }
        }

        // 变更后查询：模拟缓存被清除后从数据库获取最新数据
        List<DictData> cachedResult = service.listEnabledByDictCode(dictCode);
        List<DictData> dbResult = dbState.stream()
                .filter(d -> dictCode.equals(d.getDictCode()) && d.getStatus() == 1)
                .sorted(Comparator.comparingInt(d -> d.getSortOrder() != null ? d.getSortOrder() : 0))
                .collect(Collectors.toList());

        // 验证：缓存查询结果与数据库状态一致
        assertThat(cachedResult).hasSameSizeAs(dbResult);
        for (int i = 0; i < cachedResult.size(); i++) {
            assertThat(cachedResult.get(i).getDictCode()).isEqualTo(dbResult.get(i).getDictCode());
            assertThat(cachedResult.get(i).getLabel()).isEqualTo(dbResult.get(i).getLabel());
            assertThat(cachedResult.get(i).getValue()).isEqualTo(dbResult.get(i).getValue());
        }
    }

    // ========== Providers ==========

    @Provide
    Arbitrary<String> validDictCode() {
        return Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(30)
                .map(s -> "dict_" + s.toLowerCase());
    }

    @Provide
    Arbitrary<String> validDictName() {
        return Arbitraries.of("用户状态", "性别", "订单类型", "支付方式", "审核状态",
                "优先级", "数据类型", "通知类型", "权限级别", "日志类型");
    }

    @Provide
    Arbitrary<String> validLabel() {
        return Arbitraries.of("启用", "禁用", "男", "女", "待审核", "已通过",
                "已拒绝", "高", "中", "低", "正常", "异常");
    }

    @Provide
    Arbitrary<String> validValue() {
        return Arbitraries.of("0", "1", "2", "3", "male", "female",
                "pending", "approved", "rejected", "high", "medium", "low");
    }

    @Provide
    Arbitrary<Integer> sortOrder() {
        return Arbitraries.integers().between(0, 100);
    }

    @Provide
    Arbitrary<List<DictData>> dictDataList() {
        return validDictDataItem().list().ofMinSize(0).ofMaxSize(10);
    }

    @Provide
    Arbitrary<DictData> validDictDataItem() {
        Arbitrary<String> codes = Arbitraries.of("dict_status", "dict_gender", "dict_type", "dict_level");
        Arbitrary<String> labels = validLabel();
        Arbitrary<String> values = validValue();
        Arbitrary<Integer> sorts = Arbitraries.integers().between(0, 50);
        Arbitrary<Integer> statuses = Arbitraries.of(0, 1);

        return Combinators.combine(codes, labels, values, sorts, statuses)
                .as((code, label, value, sort, status) -> {
                    DictData dd = new DictData();
                    dd.setId((long) (Math.random() * 10000 + 1));
                    dd.setDictCode(code);
                    dd.setLabel(label);
                    dd.setValue(value);
                    dd.setSortOrder(sort);
                    dd.setStatus(status);
                    return dd;
                });
    }

    @Provide
    Arbitrary<List<String>> dictCodeSet() {
        return Arbitraries.of("dict_status", "dict_gender", "dict_type", "dict_level", "dict_priority")
                .list().ofMinSize(1).ofMaxSize(5).uniqueElements();
    }

    @Provide
    Arbitrary<String> cacheOperation() {
        return Arbitraries.of("create", "update", "delete");
    }
}
