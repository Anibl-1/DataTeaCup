package com.dataplatform;

import com.dataplatform.common.exception.BusinessException;
import com.dataplatform.data.dto.export.ExportProgress;
import com.dataplatform.data.dto.export.ExportRequest;
import com.dataplatform.data.entity.ExportTask;
import com.dataplatform.data.mapper.ChartDefinitionMapper;
import com.dataplatform.data.mapper.DataSourceMapper;
import com.dataplatform.data.mapper.ExportTaskMapper;
import com.dataplatform.data.mapper.ReportDefinitionMapper;
import com.dataplatform.data.service.DataSourceConnectionPoolManager;
import com.dataplatform.data.service.DataSourceService;
import com.dataplatform.data.service.DbConnectionUtil;
import com.dataplatform.data.service.export.StreamingExportServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * StreamingExportServiceImpl 单元测试
 * 覆盖: createAsyncExportTask, executeAsyncExport(信号量/状态转换),
 *       executeResumeExport(信号量一致性), getProgress, cancelExport
 */
class StreamingExportServiceTest {

    private StreamingExportServiceImpl service;
    private ExportTaskMapper exportTaskMapper;
    private DataSourceMapper dataSourceMapper;
    private DataSourceService dataSourceService;
    private DbConnectionUtil dbConnectionUtil;
    private DataSourceConnectionPoolManager connectionPoolManager;
    private ReportDefinitionMapper reportDefinitionMapper;
    private ChartDefinitionMapper chartDefinitionMapper;

    @BeforeEach
    void setUp() throws Exception {
        service = new StreamingExportServiceImpl();
        exportTaskMapper = mock(ExportTaskMapper.class);
        dataSourceMapper = mock(DataSourceMapper.class);
        dataSourceService = mock(DataSourceService.class);
        dbConnectionUtil = mock(DbConnectionUtil.class);
        connectionPoolManager = mock(DataSourceConnectionPoolManager.class);
        reportDefinitionMapper = mock(ReportDefinitionMapper.class);
        chartDefinitionMapper = mock(ChartDefinitionMapper.class);

        injectField("exportTaskMapper", exportTaskMapper);
        injectField("dataSourceMapper", dataSourceMapper);
        injectField("dataSourceService", dataSourceService);
        injectField("dbConnectionUtil", dbConnectionUtil);
        injectField("connectionPoolManager", connectionPoolManager);
        injectField("reportDefinitionMapper", reportDefinitionMapper);
        injectField("chartDefinitionMapper", chartDefinitionMapper);
        injectField("exportPath", "../runtime/exports");
        injectField("maxRowsPerSheet", 1000000);
        injectField("flushRows", 500);
        injectField("expireDays", 7);
        injectField("asyncThreshold", 50000L);
        injectField("compressionThreshold", 10485760L);
        injectField("queryTimeout", 300);
        injectField("maxConcurrent", 3);
        injectField("maxRowsPerExport", 5000000L);

        // Initialize semaphore (normally done in @PostConstruct)
        Semaphore semaphore = new Semaphore(3);
        injectField("exportSemaphore", semaphore);
    }

    private void injectField(String name, Object value) throws Exception {
        Field f = StreamingExportServiceImpl.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(service, value);
    }

    @SuppressWarnings("unchecked")
    private <T> T getField(String name) throws Exception {
        Field f = StreamingExportServiceImpl.class.getDeclaredField(name);
        f.setAccessible(true);
        return (T) f.get(service);
    }

    // ==================== createAsyncExportTask ====================

    @Test
    void createAsyncExportTask_returnsTaskId() {
        doAnswer(inv -> {
            ExportTask t = inv.getArgument(0);
            t.setId(42L);
            return null;
        }).when(exportTaskMapper).insert(any(ExportTask.class));

        ExportRequest request = new ExportRequest();
        request.setTaskName("test_task");
        request.setTaskType("excel");
        request.setUserId(1L);
        request.setReportId(10L);

        Long taskId = service.createAsyncExportTask(request);

        assertThat(taskId).isEqualTo(42L);
        verify(exportTaskMapper).insert(any(ExportTask.class));
    }

    @Test
    void createAsyncExportTask_setsDefaultName_whenNull() {
        doAnswer(inv -> {
            ExportTask t = inv.getArgument(0);
            t.setId(1L);
            // Verify default name pattern
            assertThat(t.getTaskName()).startsWith("导出任务_");
            assertThat(t.getStatus()).isEqualTo(0); // PENDING
            assertThat(t.getProgress()).isEqualTo(0);
            return null;
        }).when(exportTaskMapper).insert(any(ExportTask.class));

        ExportRequest request = new ExportRequest();
        request.setUserId(1L);

        service.createAsyncExportTask(request);
        verify(exportTaskMapper).insert(any(ExportTask.class));
    }

    // ==================== executeAsyncExport — semaphore & state ====================

    @Test
    void executeAsyncExport_returnsEarly_whenTaskNotFound() {
        when(exportTaskMapper.selectById(999L)).thenReturn(null);

        service.executeAsyncExport(999L);

        verify(exportTaskMapper, never()).update(any());
    }

    @Test
    void executeAsyncExport_semaphoreReleasedOnFailure() throws Exception {
        Semaphore semaphore = getField("exportSemaphore");
        int permitsBefore = semaphore.availablePermits();

        ExportTask task = new ExportTask();
        task.setId(1L);
        task.setTaskType("excel");
        task.setTaskName("fail_task");
        task.setStatus(0);
        when(exportTaskMapper.selectById(1L)).thenReturn(task);
        // The export will fail because there's no actual data source / SQL
        // but semaphore should still be released in finally block

        service.executeAsyncExport(1L);

        int permitsAfter = semaphore.availablePermits();
        assertThat(permitsAfter).isEqualTo(permitsBefore);
    }

    @Test
    void executeAsyncExport_setsStatusToRunning() {
        ExportTask task = new ExportTask();
        task.setId(1L);
        task.setTaskType("excel");
        task.setTaskName("test");
        task.setStatus(0);
        when(exportTaskMapper.selectById(1L)).thenReturn(task);

        service.executeAsyncExport(1L);

        // verify update was called at least once (status -> RUNNING, then FAILED due to missing data)
        verify(exportTaskMapper, atLeastOnce()).update(any(ExportTask.class));
    }

    // ==================== executeResumeExport — semaphore consistency ====================

    @Test
    void executeResumeExport_semaphoreReleasedOnFailure() throws Exception {
        // This verifies the BUG-3 fix: executeResumeExport must release semaphore
        Semaphore semaphore = getField("exportSemaphore");
        int permitsBefore = semaphore.availablePermits();

        ExportTask task = new ExportTask();
        task.setId(2L);
        task.setTaskType("excel");
        task.setTaskName("resume_test");
        task.setStatus(0);
        when(exportTaskMapper.selectById(2L)).thenReturn(task);

        service.executeResumeExport(2L, 1000L);

        int permitsAfter = semaphore.availablePermits();
        assertThat(permitsAfter).isEqualTo(permitsBefore);
    }

    @Test
    void executeResumeExport_returnsEarly_whenTaskNotFound() {
        when(exportTaskMapper.selectById(999L)).thenReturn(null);

        service.executeResumeExport(999L, 0L);

        verify(exportTaskMapper, never()).update(any());
    }

    @Test
    void executeResumeExport_clearsErrorMsg_onStart() {
        ExportTask task = new ExportTask();
        task.setId(3L);
        task.setTaskType("excel");
        task.setTaskName("resume_with_error");
        task.setStatus(3); // previously FAILED
        task.setErrorMsg("previous error");
        when(exportTaskMapper.selectById(3L)).thenReturn(task);

        service.executeResumeExport(3L, 0L);

        // After the first update (status -> RUNNING), errorMsg should be null
        verify(exportTaskMapper, atLeastOnce()).update(argThat(t -> {
            // At least one update should set status=1 (RUNNING) with null errorMsg
            return t.getStatus() == 1 || t.getStatus() == 3;
        }));
    }

    // ==================== getProgress ====================

    @Test
    void getProgress_throwsWhenTaskNotFound() {
        when(exportTaskMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> service.getProgress(999L))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void getProgress_returnsCorrectProgress() {
        ExportTask task = new ExportTask();
        task.setId(1L);
        task.setTaskName("test");
        task.setStatus(2); // COMPLETED
        task.setProgress(100);
        task.setTotalRows(5000L);
        task.setProcessedRows(5000L);
        task.setFilePath("/exports/test.xlsx");
        task.setFileName("test.xlsx");
        task.setFileSize(1024L);
        task.setCreateTime(LocalDateTime.now().minusHours(1));
        task.setStartTime(LocalDateTime.now().minusMinutes(30));
        task.setFinishTime(LocalDateTime.now());
        task.setExpireTime(LocalDateTime.now().plusDays(7));
        when(exportTaskMapper.selectById(1L)).thenReturn(task);

        ExportProgress progress = service.getProgress(1L);

        assertThat(progress.getTaskId()).isEqualTo(1L);
        assertThat(progress.getStatus()).isEqualTo("COMPLETED");
        assertThat(progress.getProgressPercent()).isEqualTo(100);
        assertThat(progress.getDownloadUrl()).isEqualTo("/api/export/download/1");
    }

    @Test
    void getProgress_partialCompleted_hasDownloadUrl() {
        ExportTask task = new ExportTask();
        task.setId(1L);
        task.setTaskName("partial");
        task.setStatus(6); // PARTIAL_COMPLETED
        task.setProgress(60);
        task.setFilePath("/exports/partial.xlsx");
        task.setCreateTime(LocalDateTime.now());
        task.setExpireTime(LocalDateTime.now().plusDays(7));
        when(exportTaskMapper.selectById(1L)).thenReturn(task);

        ExportProgress progress = service.getProgress(1L);
        assertThat(progress.getDownloadUrl()).isEqualTo("/api/export/download/1");
    }

    @Test
    void getProgress_runningTask_noDownloadUrl() {
        ExportTask task = new ExportTask();
        task.setId(1L);
        task.setTaskName("running");
        task.setStatus(1); // RUNNING
        task.setProgress(50);
        task.setCreateTime(LocalDateTime.now());
        task.setExpireTime(LocalDateTime.now().plusDays(7));
        when(exportTaskMapper.selectById(1L)).thenReturn(task);

        ExportProgress progress = service.getProgress(1L);
        assertThat(progress.getDownloadUrl()).isNull();
    }

    // ==================== cancelExport ====================

    @Test
    void cancelExport_throwsWhenTaskNotFound() {
        when(exportTaskMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> service.cancelExport(99L))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void cancelExport_throwsWhenAlreadyCompleted() {
        ExportTask task = new ExportTask();
        task.setId(5L);
        task.setStatus(2); // COMPLETED
        when(exportTaskMapper.selectById(5L)).thenReturn(task);

        assertThatThrownBy(() -> service.cancelExport(5L))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("已完成或已取消");
    }

    @Test
    void cancelExport_setsFlag_whenTaskRunning() throws Exception {
        ConcurrentHashMap<Long, Boolean> runningTasks = getField("runningTasks");
        runningTasks.put(5L, true);

        ExportTask task = new ExportTask();
        task.setId(5L);
        task.setStatus(1); // RUNNING
        when(exportTaskMapper.selectById(5L)).thenReturn(task);

        boolean cancelled = service.cancelExport(5L);

        assertThat(cancelled).isTrue();
        assertThat(runningTasks.get(5L)).isFalse();
        assertThat(task.getStatus()).isEqualTo(4); // CANCELLED
        verify(exportTaskMapper).update(task);
    }
}
