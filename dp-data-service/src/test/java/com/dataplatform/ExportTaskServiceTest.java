package com.dataplatform;

import com.dataplatform.common.exception.BusinessException;
import com.dataplatform.data.entity.ExportTask;
import com.dataplatform.data.mapper.DataSourceMapper;
import com.dataplatform.data.mapper.ExportTaskMapper;
import com.dataplatform.data.mapper.ReportDefinitionMapper;
import com.dataplatform.data.mapper.ReportFieldMapper;
import com.dataplatform.data.service.DataSourceService;
import com.dataplatform.data.service.DbConnectionUtil;
import com.dataplatform.data.service.ExportTaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * ExportTaskService 单元测试
 * 覆盖: createExportTask, getTaskById, deleteTask, getExportFile(权限校验), getTaskList
 */
class ExportTaskServiceTest {

    private ExportTaskService service;
    private ExportTaskMapper exportTaskMapper;
    private ReportDefinitionMapper reportDefinitionMapper;
    private ReportFieldMapper reportFieldMapper;
    private DataSourceMapper dataSourceMapper;
    private DataSourceService dataSourceService;
    private DbConnectionUtil dbConnectionUtil;

    @BeforeEach
    void setUp() throws Exception {
        service = new ExportTaskService();
        exportTaskMapper = mock(ExportTaskMapper.class);
        reportDefinitionMapper = mock(ReportDefinitionMapper.class);
        reportFieldMapper = mock(ReportFieldMapper.class);
        dataSourceMapper = mock(DataSourceMapper.class);
        dataSourceService = mock(DataSourceService.class);
        dbConnectionUtil = mock(DbConnectionUtil.class);

        injectField("exportTaskMapper", exportTaskMapper);
        injectField("reportDefinitionMapper", reportDefinitionMapper);
        injectField("reportFieldMapper", reportFieldMapper);
        injectField("dataSourceMapper", dataSourceMapper);
        injectField("dataSourceService", dataSourceService);
        injectField("dbConnectionUtil", dbConnectionUtil);
        injectField("exportPath", "../runtime/exports");
        injectField("expireDays", 7);
    }

    private void injectField(String name, Object value) throws Exception {
        Field f = ExportTaskService.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(service, value);
    }

    // ==================== createExportTask ====================

    @Test
    void createExportTask_setsAllFieldsCorrectly() {
        doAnswer(inv -> {
            ExportTask t = inv.getArgument(0);
            t.setId(100L);
            return null;
        }).when(exportTaskMapper).insert(any(ExportTask.class));

        ExportTask task = service.createExportTask(
            "test_export", "excel", 1L, "RPT001", "{}", "{}", 42L
        );

        assertThat(task.getTaskName()).isEqualTo("test_export");
        assertThat(task.getTaskType()).isEqualTo("excel");
        assertThat(task.getRefId()).isEqualTo(1L);
        assertThat(task.getRefCode()).isEqualTo("RPT001");
        assertThat(task.getStatus()).isEqualTo(0); // PENDING
        assertThat(task.getProgress()).isEqualTo(0);
        assertThat(task.getCreateBy()).isEqualTo(42L);
        assertThat(task.getCreateTime()).isNotNull();
        assertThat(task.getExpireTime()).isAfter(task.getCreateTime());
        verify(exportTaskMapper).insert(any(ExportTask.class));
    }

    @Test
    void createExportTask_expireTimeIs7DaysLater() {
        doNothing().when(exportTaskMapper).insert(any());

        ExportTask task = service.createExportTask(
            "test", "csv", null, null, null, null, 1L
        );

        assertThat(task.getExpireTime()).isAfter(LocalDateTime.now().plusDays(6));
        assertThat(task.getExpireTime()).isBefore(LocalDateTime.now().plusDays(8));
    }

    // ==================== getTaskById ====================

    @Test
    void getTaskById_returnsTask() {
        ExportTask expected = new ExportTask();
        expected.setId(1L);
        expected.setTaskName("test");
        when(exportTaskMapper.selectById(1L)).thenReturn(expected);

        ExportTask result = service.getTaskById(1L);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void getTaskById_returnsNull_whenNotFound() {
        when(exportTaskMapper.selectById(999L)).thenReturn(null);
        assertThat(service.getTaskById(999L)).isNull();
    }

    // ==================== deleteTask ====================

    @Test
    void deleteTask_deletesFileAndRecord() {
        ExportTask task = new ExportTask();
        task.setId(1L);
        task.setFilePath("non_existent_path.xlsx"); // file won't exist, but that's fine
        when(exportTaskMapper.selectById(1L)).thenReturn(task);
        when(exportTaskMapper.deleteByIdAndUser(1L, 42L)).thenReturn(1);

        boolean result = service.deleteTask(1L, 42L);

        assertThat(result).isTrue();
        verify(exportTaskMapper).deleteByIdAndUser(1L, 42L);
    }

    @Test
    void deleteTask_returnsFalse_whenNotOwner() {
        ExportTask task = new ExportTask();
        task.setId(1L);
        when(exportTaskMapper.selectById(1L)).thenReturn(task);
        when(exportTaskMapper.deleteByIdAndUser(1L, 99L)).thenReturn(0);

        boolean result = service.deleteTask(1L, 99L);
        assertThat(result).isFalse();
    }

    @Test
    void deleteTask_handlesNullTask() {
        when(exportTaskMapper.selectById(999L)).thenReturn(null);
        when(exportTaskMapper.deleteByIdAndUser(999L, 1L)).thenReturn(0);

        boolean result = service.deleteTask(999L, 1L);
        assertThat(result).isFalse();
    }

    // ==================== getExportFile ====================

    @Test
    void getExportFile_throwsWhenTaskNotFound() {
        when(exportTaskMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> service.getExportFile(999L, 1L))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("任务不存在");
    }

    @Test
    void getExportFile_throwsWhenNotOwner() {
        ExportTask task = new ExportTask();
        task.setId(1L);
        task.setCreateBy(42L);
        when(exportTaskMapper.selectById(1L)).thenReturn(task);

        assertThatThrownBy(() -> service.getExportFile(1L, 99L))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("无权访问");
    }

    @Test
    void getExportFile_throwsWhenTaskNotCompleted() {
        ExportTask task = new ExportTask();
        task.setId(1L);
        task.setCreateBy(42L);
        task.setStatus(1); // RUNNING, not COMPLETED
        when(exportTaskMapper.selectById(1L)).thenReturn(task);

        assertThatThrownBy(() -> service.getExportFile(1L, 42L))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("任务未完成");
    }

    @Test
    void getExportFile_throwsWhenFilePathNull() {
        ExportTask task = new ExportTask();
        task.setId(1L);
        task.setCreateBy(42L);
        task.setStatus(2); // COMPLETED
        task.setFilePath(null);
        when(exportTaskMapper.selectById(1L)).thenReturn(task);

        assertThatThrownBy(() -> service.getExportFile(1L, 42L))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("文件不存在");
    }

    @Test
    void getExportFile_throwsWhenFileDeleted() {
        ExportTask task = new ExportTask();
        task.setId(1L);
        task.setCreateBy(42L);
        task.setStatus(2);
        task.setFilePath("/tmp/definitely_does_not_exist_" + System.nanoTime() + ".xlsx");
        when(exportTaskMapper.selectById(1L)).thenReturn(task);

        assertThatThrownBy(() -> service.getExportFile(1L, 42L))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("已过期或被删除");
    }

    // ==================== getPendingTasks ====================

    @Test
    void getPendingTasks_delegatesToMapper() {
        List<ExportTask> expected = List.of(new ExportTask());
        when(exportTaskMapper.selectPendingByUserId(42L)).thenReturn(expected);

        List<ExportTask> result = service.getPendingTasks(42L);
        assertThat(result).isEqualTo(expected);
        verify(exportTaskMapper).selectPendingByUserId(42L);
    }
}
