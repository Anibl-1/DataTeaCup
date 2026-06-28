package com.dataplatform;

import com.dataplatform.system.dto.OperationLogQueryDTO;
import com.dataplatform.system.entity.OperationLog;
import com.dataplatform.system.mapper.OperationLogMapper;
import com.dataplatform.system.service.impl.OperationLogServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 操作日志查询增强单元测试
 * Validates: Requirements 12.2, 12.3
 */
class OperationLogQueryTest {

    private OperationLogServiceImpl service;
    private OperationLogMapper mockMapper;

    @BeforeEach
    void setUp() throws Exception {
        service = new OperationLogServiceImpl();
        mockMapper = mock(OperationLogMapper.class);
        Field mapperField = OperationLogServiceImpl.class.getDeclaredField("operationLogMapper");
        mapperField.setAccessible(true);
        mapperField.set(service, mockMapper);
    }

    @Test
    void queryByCondition_withOperationType_filtersCorrectly() {
        OperationLogQueryDTO query = new OperationLogQueryDTO();
        query.setOperationType("CREATE");
        query.setPage(1);
        query.setPageSize(10);

        OperationLog log = createLog("CREATE", "admin", "system");
        when(mockMapper.selectByQuery(any())).thenReturn(List.of(log));
        when(mockMapper.countByQuery(any())).thenReturn(1);

        Map<String, Object> result = service.queryByCondition(query);

        assertThat(result.get("total")).isEqualTo(1);
        assertThat((List<?>) result.get("list")).hasSize(1);
        verify(mockMapper).selectByQuery(any(OperationLogQueryDTO.class));
    }

    @Test
    void queryByCondition_withUsername_filtersCorrectly() {
        OperationLogQueryDTO query = new OperationLogQueryDTO();
        query.setUsername("admin");
        query.setPage(1);
        query.setPageSize(10);

        when(mockMapper.selectByQuery(any())).thenReturn(List.of(createLog("UPDATE", "admin", "system")));
        when(mockMapper.countByQuery(any())).thenReturn(1);

        Map<String, Object> result = service.queryByCondition(query);

        assertThat(result.get("total")).isEqualTo(1);
        ArgumentCaptor<OperationLogQueryDTO> captor = ArgumentCaptor.forClass(OperationLogQueryDTO.class);
        verify(mockMapper).selectByQuery(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("admin");
    }

    @Test
    void queryByCondition_withTimeRange_filtersCorrectly() {
        OperationLogQueryDTO query = new OperationLogQueryDTO();
        Date startTime = new Date(System.currentTimeMillis() - 86400000L);
        Date endTime = new Date();
        query.setStartTime(startTime);
        query.setEndTime(endTime);
        query.setPage(1);
        query.setPageSize(10);

        when(mockMapper.selectByQuery(any())).thenReturn(Collections.emptyList());
        when(mockMapper.countByQuery(any())).thenReturn(0);

        Map<String, Object> result = service.queryByCondition(query);

        assertThat(result.get("total")).isEqualTo(0);
        ArgumentCaptor<OperationLogQueryDTO> captor = ArgumentCaptor.forClass(OperationLogQueryDTO.class);
        verify(mockMapper).selectByQuery(captor.capture());
        assertThat(captor.getValue().getStartTime()).isEqualTo(startTime);
        assertThat(captor.getValue().getEndTime()).isEqualTo(endTime);
    }

    @Test
    void queryByCondition_withModuleName_filtersCorrectly() {
        OperationLogQueryDTO query = new OperationLogQueryDTO();
        query.setModuleName("system");
        query.setPage(1);
        query.setPageSize(10);

        when(mockMapper.selectByQuery(any())).thenReturn(List.of(createLog("DELETE", "admin", "system")));
        when(mockMapper.countByQuery(any())).thenReturn(1);

        Map<String, Object> result = service.queryByCondition(query);

        ArgumentCaptor<OperationLogQueryDTO> captor = ArgumentCaptor.forClass(OperationLogQueryDTO.class);
        verify(mockMapper).selectByQuery(captor.capture());
        assertThat(captor.getValue().getModuleName()).isEqualTo("system");
    }

    @Test
    void queryByCondition_withCombinedFilters() {
        OperationLogQueryDTO query = new OperationLogQueryDTO();
        query.setOperationType("UPDATE");
        query.setUsername("admin");
        query.setModuleName("org");
        query.setStartTime(new Date(System.currentTimeMillis() - 86400000L));
        query.setEndTime(new Date());
        query.setPage(1);
        query.setPageSize(10);

        when(mockMapper.selectByQuery(any())).thenReturn(List.of(createLog("UPDATE", "admin", "org")));
        when(mockMapper.countByQuery(any())).thenReturn(1);

        Map<String, Object> result = service.queryByCondition(query);

        assertThat(result.get("total")).isEqualTo(1);
        ArgumentCaptor<OperationLogQueryDTO> captor = ArgumentCaptor.forClass(OperationLogQueryDTO.class);
        verify(mockMapper).selectByQuery(captor.capture());
        OperationLogQueryDTO captured = captor.getValue();
        assertThat(captured.getOperationType()).isEqualTo("UPDATE");
        assertThat(captured.getUsername()).isEqualTo("admin");
        assertThat(captured.getModuleName()).isEqualTo("org");
        assertThat(captured.getStartTime()).isNotNull();
        assertThat(captured.getEndTime()).isNotNull();
    }

    @Test
    void queryByCondition_defaultsPagination() {
        OperationLogQueryDTO query = new OperationLogQueryDTO();
        // page and pageSize are null

        when(mockMapper.selectByQuery(any())).thenReturn(Collections.emptyList());
        when(mockMapper.countByQuery(any())).thenReturn(0);

        Map<String, Object> result = service.queryByCondition(query);

        assertThat(result.get("page")).isEqualTo(1);
        assertThat(result.get("pageSize")).isEqualTo(10);
    }

    @Test
    void queryDTO_offsetCalculation() {
        OperationLogQueryDTO query = new OperationLogQueryDTO();
        query.setPage(3);
        query.setPageSize(20);
        assertThat(query.getOffset()).isEqualTo(40);

        query.setPage(1);
        query.setPageSize(10);
        assertThat(query.getOffset()).isEqualTo(0);

        // null defaults
        OperationLogQueryDTO nullQuery = new OperationLogQueryDTO();
        assertThat(nullQuery.getOffset()).isEqualTo(0);
    }

    private OperationLog createLog(String operationType, String username, String moduleName) {
        OperationLog log = new OperationLog();
        log.setId(1L);
        log.setOperationType(operationType);
        log.setUsername(username);
        log.setModuleName(moduleName);
        log.setOperationDesc("test operation");
        log.setStatus("success");
        log.setCreateTime(new Date());
        return log;
    }
}
