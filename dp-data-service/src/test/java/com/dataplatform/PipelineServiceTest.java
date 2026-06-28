package com.dataplatform;

import com.dataplatform.data.entity.Pipeline;
import com.dataplatform.data.entity.PipelineExecution;
import com.dataplatform.data.mapper.DataSourceMapper;
import com.dataplatform.data.mapper.PipelineExecutionMapper;
import com.dataplatform.data.mapper.PipelineMapper;
import com.dataplatform.data.mapper.PipelineNodeMapper;
import com.dataplatform.data.engine.TransferEngineSelector;
import com.dataplatform.data.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PipelineService 单元测试
 * 覆盖: CRUD, getStatistics, getExecutionTrend, search
 */
class PipelineServiceTest {

    private PipelineService service;
    private PipelineMapper pipelineMapper;
    private PipelineNodeMapper nodeMapper;
    private PipelineExecutionMapper executionMapper;
    private DataSourceMapper dataSourceMapper;
    private DbConnectionUtil dbConnectionUtil;
    private DataSourceConnectionPoolManager connectionPoolManager;
    private TransferEngineSelector transferEngineSelector;
    private LicenseLimitService licenseLimitService;

    @BeforeEach
    void setUp() {
        pipelineMapper = mock(PipelineMapper.class);
        nodeMapper = mock(PipelineNodeMapper.class);
        executionMapper = mock(PipelineExecutionMapper.class);
        dataSourceMapper = mock(DataSourceMapper.class);
        dbConnectionUtil = mock(DbConnectionUtil.class);
        connectionPoolManager = mock(DataSourceConnectionPoolManager.class);
        transferEngineSelector = mock(TransferEngineSelector.class);
        licenseLimitService = mock(LicenseLimitService.class);
        Executor taskExecutor = mock(Executor.class);

        service = new PipelineService(
            pipelineMapper, nodeMapper, executionMapper,
            new ObjectMapper(), dbConnectionUtil, dataSourceMapper,
            null, // alertNotificationService
            taskExecutor,
            null, // wecomNotifyService
            null, // emailNotifyService
            null, // smsNotifyService
            null, // dingtalkNotifyService
            connectionPoolManager,
            transferEngineSelector,
            licenseLimitService
        );
    }

    // ==================== CRUD ====================

    @Test
    void findAll_delegatesToMapper() {
        List<Pipeline> expected = List.of(new Pipeline());
        when(pipelineMapper.findAll()).thenReturn(expected);

        List<Pipeline> result = service.findAll();
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void findById_returnsCorrectPipeline() {
        Pipeline p = new Pipeline();
        p.setId(1L);
        p.setPipelineName("test-pipeline");
        when(pipelineMapper.findById(1L)).thenReturn(p);

        Pipeline result = service.findById(1L);
        assertThat(result.getPipelineName()).isEqualTo("test-pipeline");
    }

    @Test
    void findById_returnsNull_whenNotFound() {
        when(pipelineMapper.findById(999L)).thenReturn(null);
        assertThat(service.findById(999L)).isNull();
    }

    @Test
    void search_delegatesToMapper() {
        List<Pipeline> expected = List.of(new Pipeline());
        when(pipelineMapper.search("etl", 1, 1, 0)).thenReturn(expected);

        List<Pipeline> result = service.search("etl", 1, 1, 0);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void search_withNullParams() {
        when(pipelineMapper.search(null, null, null, null)).thenReturn(new ArrayList<>());

        List<Pipeline> result = service.search(null, null, null, null);
        assertThat(result).isEmpty();
    }

    // ==================== getStatistics ====================

    @Test
    void getStatistics_returnsAllKeys() {
        when(pipelineMapper.countAll()).thenReturn(50);
        when(pipelineMapper.countByStatus(1)).thenReturn(30);
        when(executionMapper.countAll()).thenReturn(200);
        when(executionMapper.countToday()).thenReturn(10);
        when(executionMapper.countByStatus(1)).thenReturn(170); // success
        when(executionMapper.countByStatus(0)).thenReturn(20);  // failed
        when(executionMapper.countByStatus(2)).thenReturn(10);  // running

        Map<String, Object> stats = service.getStatistics();

        assertThat(stats).containsKeys(
            "totalPipelines", "publishedPipelines", "totalExecutions",
            "todayExecutions", "successExecutions", "failedExecutions", "runningExecutions"
        );
        assertThat(stats.get("totalPipelines")).isEqualTo(50);
        assertThat(stats.get("publishedPipelines")).isEqualTo(30);
        assertThat(stats.get("successExecutions")).isEqualTo(170);
        assertThat(stats.get("failedExecutions")).isEqualTo(20);
        assertThat(stats.get("runningExecutions")).isEqualTo(10);
    }

    @Test
    void getStatistics_statusCodes_consistent() {
        // Verify: 0=failed, 1=success, 2=running (same as DashboardService)
        when(pipelineMapper.countAll()).thenReturn(0);
        when(pipelineMapper.countByStatus(anyInt())).thenReturn(0);
        when(executionMapper.countAll()).thenReturn(0);
        when(executionMapper.countToday()).thenReturn(0);
        when(executionMapper.countByStatus(1)).thenReturn(100); // success
        when(executionMapper.countByStatus(0)).thenReturn(5);   // failed
        when(executionMapper.countByStatus(2)).thenReturn(3);   // running

        Map<String, Object> stats = service.getStatistics();

        assertThat(stats.get("successExecutions")).isEqualTo(100);
        assertThat(stats.get("failedExecutions")).isEqualTo(5);
        assertThat(stats.get("runningExecutions")).isEqualTo(3);

        verify(executionMapper).countByStatus(1); // success
        verify(executionMapper).countByStatus(0); // failed
        verify(executionMapper).countByStatus(2); // running
    }

    @Test
    void getStatistics_zeroCounts() {
        when(pipelineMapper.countAll()).thenReturn(0);
        when(pipelineMapper.countByStatus(1)).thenReturn(0);
        when(executionMapper.countAll()).thenReturn(0);
        when(executionMapper.countToday()).thenReturn(0);
        when(executionMapper.countByStatus(anyInt())).thenReturn(0);

        Map<String, Object> stats = service.getStatistics();

        assertThat(stats.get("totalPipelines")).isEqualTo(0);
        assertThat(stats.get("totalExecutions")).isEqualTo(0);
    }

    // ==================== getExecutionTrend ====================

    @Test
    void getExecutionTrend_returns7Days_fillsMissing() {
        // DB returns data for only 2 days
        List<Map<String, Object>> dbData = new ArrayList<>();
        Map<String, Object> day1 = new HashMap<>();
        day1.put("day", java.time.LocalDate.now().toString());
        day1.put("success_count", 10);
        day1.put("failed_count", 2);
        dbData.add(day1);
        when(executionMapper.countDailyTrend(7)).thenReturn(dbData);

        List<Map<String, Object>> trend = service.getExecutionTrend(7);

        assertThat(trend).hasSize(7);
        // Last entry (today) should have data
        Map<String, Object> today = trend.get(6);
        assertThat(today.get("successCount")).isEqualTo(10);
        assertThat(today.get("failedCount")).isEqualTo(2);

        // Other days should be zero-filled
        Map<String, Object> yesterday = trend.get(5);
        assertThat(yesterday.get("successCount")).isEqualTo(0);
        assertThat(yesterday.get("failedCount")).isEqualTo(0);
    }

    @Test
    void getExecutionTrend_emptyDb_allZeros() {
        when(executionMapper.countDailyTrend(7)).thenReturn(new ArrayList<>());

        List<Map<String, Object>> trend = service.getExecutionTrend(7);

        assertThat(trend).hasSize(7);
        for (Map<String, Object> day : trend) {
            assertThat(day.get("successCount")).isEqualTo(0);
            assertThat(day.get("failedCount")).isEqualTo(0);
            assertThat(day.get("day")).isNotNull();
        }
    }

    @Test
    void getExecutionTrend_customDayCount() {
        when(executionMapper.countDailyTrend(30)).thenReturn(new ArrayList<>());

        List<Map<String, Object>> trend = service.getExecutionTrend(30);
        assertThat(trend).hasSize(30);
    }

    @Test
    void getExecutionTrend_daysInChronologicalOrder() {
        when(executionMapper.countDailyTrend(7)).thenReturn(new ArrayList<>());

        List<Map<String, Object>> trend = service.getExecutionTrend(7);

        // First day should be 6 days ago, last should be today
        String firstDay = (String) trend.get(0).get("day");
        String lastDay = (String) trend.get(6).get("day");
        assertThat(firstDay.compareTo(lastDay)).isLessThan(0);
    }
}
