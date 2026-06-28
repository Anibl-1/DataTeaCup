package com.dataplatform;

import com.dataplatform.common.constants.Constants;
import com.dataplatform.common.exception.BusinessException;
import com.dataplatform.data.entity.CollectTask;
import com.dataplatform.data.entity.DataSource;
import com.dataplatform.data.mapper.CollectLogMapper;
import com.dataplatform.data.mapper.CollectTaskMapper;
import com.dataplatform.data.mapper.DataSourceMapper;
import com.dataplatform.data.mapper.DatabaseViewMapper;
import com.dataplatform.data.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * DataCollectService 单元测试
 * 覆盖: CRUD操作、参数校验、任务启停、采集模式验证、默认值设置
 */
class DataCollectServiceTest {

    private DataCollectService service;
    private CollectTaskMapper collectTaskMapper;
    private CollectLogMapper collectLogMapper;
    private DataSourceMapper dataSourceMapper;
    private DataSourceService dataSourceService;
    private DbConnectionUtil dbConnectionUtil;
    private DatabaseViewMapper viewMapper;
    private DataLineageService dataLineageService;
    private DataSourceConnectionPoolManager connectionPoolManager;

    @BeforeEach
    void setUp() {
        collectTaskMapper = mock(CollectTaskMapper.class);
        collectLogMapper = mock(CollectLogMapper.class);
        dataSourceMapper = mock(DataSourceMapper.class);
        dataSourceService = mock(DataSourceService.class);
        dbConnectionUtil = mock(DbConnectionUtil.class);
        viewMapper = mock(DatabaseViewMapper.class);
        dataLineageService = mock(DataLineageService.class);
        connectionPoolManager = mock(DataSourceConnectionPoolManager.class);
        Executor taskExecutor = mock(Executor.class);

        service = new DataCollectService(
            collectTaskMapper, collectLogMapper, dataSourceMapper,
            dataSourceService, dbConnectionUtil, viewMapper,
            dataLineageService, null, taskExecutor, connectionPoolManager
        );
    }

    // ==================== createCollectTask ====================

    @Test
    void createCollectTask_throwsWhenNull() {
        assertThatThrownBy(() -> service.createCollectTask(null))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("不能为空");
    }

    @Test
    void createCollectTask_throwsWhenDataSourceIdNull() {
        CollectTask task = new CollectTask();
        assertThatThrownBy(() -> service.createCollectTask(task))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("数据源ID");
    }

    @Test
    void createCollectTask_throwsWhenSourceNotFound() {
        CollectTask task = new CollectTask();
        task.setDataSourceId(999L);
        when(dataSourceMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> service.createCollectTask(task))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("源数据源不存在");
    }

    @Test
    void createCollectTask_throwsWhenTargetNotFound() {
        CollectTask task = buildValidTask();
        task.setTargetDataSourceId(999L);
        when(dataSourceMapper.selectById(1L)).thenReturn(buildDataSource("src"));
        when(dataSourceMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> service.createCollectTask(task))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("目标数据源不存在");
    }

    @Test
    void createCollectTask_throwsWhenTaskNameEmpty() {
        CollectTask task = buildValidTask();
        task.setTaskName("");
        when(dataSourceMapper.selectById(1L)).thenReturn(buildDataSource("src"));

        assertThatThrownBy(() -> service.createCollectTask(task))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("任务名称");
    }

    @Test
    void createCollectTask_throwsWhenCustomModeWithoutSql() {
        CollectTask task = buildValidTask();
        task.setCollectMode("custom");
        task.setCustomSql(null);
        when(dataSourceMapper.selectById(1L)).thenReturn(buildDataSource("src"));

        assertThatThrownBy(() -> service.createCollectTask(task))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("SQL语句");
    }

    @Test
    void createCollectTask_throwsWhenIncrementalModeWithoutField() {
        CollectTask task = buildValidTask();
        task.setCollectMode("incremental");
        task.setIncrementalField(null);
        when(dataSourceMapper.selectById(1L)).thenReturn(buildDataSource("src"));

        assertThatThrownBy(() -> service.createCollectTask(task))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("增量字段");
    }

    @Test
    void createCollectTask_throwsWhenInvalidCollectMode() {
        CollectTask task = buildValidTask();
        task.setCollectMode("invalid_mode");
        when(dataSourceMapper.selectById(1L)).thenReturn(buildDataSource("src"));

        assertThatThrownBy(() -> service.createCollectTask(task))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("full、incremental或custom");
    }

    @Test
    void createCollectTask_throwsWhenTableNameEmpty_nonCustomMode() {
        CollectTask task = buildValidTask();
        task.setCollectMode("full");
        task.setTableName("");
        when(dataSourceMapper.selectById(1L)).thenReturn(buildDataSource("src"));

        assertThatThrownBy(() -> service.createCollectTask(task))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("表名");
    }

    @Test
    void createCollectTask_success_setsDefaults() {
        CollectTask task = buildValidTask();
        task.setBatchSize(null);
        task.setAutoCreateTable(null);
        task.setTargetTableName(null);
        task.setMaxRetryCount(null);
        task.setRetryInterval(null);
        task.setCollectMode(null);
        when(dataSourceMapper.selectById(1L)).thenReturn(buildDataSource("src"));

        service.createCollectTask(task);

        assertThat(task.getBatchSize()).isEqualTo(1000);
        assertThat(task.getAutoCreateTable()).isTrue();
        assertThat(task.getTargetTableName()).isEqualTo(task.getTableName());
        assertThat(task.getMaxRetryCount()).isEqualTo(3);
        assertThat(task.getRetryInterval()).isEqualTo(30);
        assertThat(task.getCollectMode()).isEqualTo("full");
        assertThat(task.getDataSourceName()).isEqualTo("src");
        verify(collectTaskMapper).insert(task);
    }

    @Test
    void createCollectTask_incrementalMode_defaultsTimestampType() {
        CollectTask task = buildValidTask();
        task.setCollectMode("incremental");
        task.setIncrementalField("update_time");
        task.setIncrementalType(null);
        when(dataSourceMapper.selectById(1L)).thenReturn(buildDataSource("src"));

        service.createCollectTask(task);

        assertThat(task.getIncrementalType()).isEqualTo("timestamp");
    }

    @Test
    void createCollectTask_setsTargetDataSourceName() {
        CollectTask task = buildValidTask();
        task.setTargetDataSourceId(2L);
        when(dataSourceMapper.selectById(1L)).thenReturn(buildDataSource("src"));
        when(dataSourceMapper.selectById(2L)).thenReturn(buildDataSource("target"));

        service.createCollectTask(task);

        assertThat(task.getTargetDataSourceName()).isEqualTo("target");
    }

    // ==================== updateCollectTask ====================

    @Test
    void updateCollectTask_throwsWhenNull() {
        assertThatThrownBy(() -> service.updateCollectTask(null))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void updateCollectTask_throwsWhenIdNull() {
        CollectTask task = new CollectTask();
        assertThatThrownBy(() -> service.updateCollectTask(task))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("任务ID");
    }

    @Test
    void updateCollectTask_throwsWhenNotFound() {
        CollectTask task = new CollectTask();
        task.setId(999L);
        when(collectTaskMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> service.updateCollectTask(task))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("不存在");
    }

    @Test
    void updateCollectTask_success() {
        CollectTask task = new CollectTask();
        task.setId(1L);
        when(collectTaskMapper.selectById(1L)).thenReturn(task);

        service.updateCollectTask(task);

        verify(collectTaskMapper).update(task);
    }

    // ==================== deleteCollectTask ====================

    @Test
    void deleteCollectTask_throwsWhenIdNull() {
        assertThatThrownBy(() -> service.deleteCollectTask(null))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("任务ID");
    }

    @Test
    void deleteCollectTask_throwsWhenNotFound() {
        when(collectTaskMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> service.deleteCollectTask(999L))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("不存在");
    }

    @Test
    void deleteCollectTask_stopsAndDeletes() {
        CollectTask task = new CollectTask();
        task.setId(1L);
        task.setStatus(Constants.TASK_STATUS_RUNNING);
        when(collectTaskMapper.selectById(1L)).thenReturn(task);

        service.deleteCollectTask(1L);

        verify(collectTaskMapper).delete(1L);
    }

    // ==================== startCollectTask ====================

    @Test
    void startCollectTask_throwsWhenIdNull() {
        assertThatThrownBy(() -> service.startCollectTask(null))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void startCollectTask_throwsWhenNotFound() {
        when(collectTaskMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> service.startCollectTask(999L))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("不存在");
    }

    @Test
    void startCollectTask_throwsWhenAlreadyRunning() {
        CollectTask task = new CollectTask();
        task.setId(1L);
        task.setStatus(Constants.TASK_STATUS_RUNNING);
        when(collectTaskMapper.selectById(1L)).thenReturn(task);

        assertThatThrownBy(() -> service.startCollectTask(1L))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("已在运行中");
    }

    @Test
    void startCollectTask_scheduledTask_updatesStatusOnly() {
        CollectTask task = new CollectTask();
        task.setId(1L);
        task.setStatus(Constants.TASK_STATUS_STOPPED);
        task.setScheduleEnabled(true);
        task.setCronExpression("0 0 * * * ?");
        when(collectTaskMapper.selectById(1L)).thenReturn(task);

        service.startCollectTask(1L);

        assertThat(task.getStatus()).isEqualTo(Constants.TASK_STATUS_RUNNING);
        verify(collectTaskMapper).update(task);
    }

    // ==================== stopCollectTask ====================

    @Test
    void stopCollectTask_throwsWhenIdNull() {
        assertThatThrownBy(() -> service.stopCollectTask(null))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void stopCollectTask_setsStatusToStopped() {
        CollectTask task = new CollectTask();
        task.setId(1L);
        task.setStatus(Constants.TASK_STATUS_RUNNING);
        when(collectTaskMapper.selectById(1L)).thenReturn(task);

        service.stopCollectTask(1L);

        assertThat(task.getStatus()).isEqualTo(Constants.TASK_STATUS_STOPPED);
        verify(collectTaskMapper).update(task);
    }

    // ==================== executeTaskOnce ====================

    @Test
    void executeTaskOnce_throwsWhenIdNull() {
        assertThatThrownBy(() -> service.executeTaskOnce(null))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void executeTaskOnce_throwsWhenNotFound() {
        when(collectTaskMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> service.executeTaskOnce(999L))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("不存在");
    }

    // ==================== getCollectTaskList ====================

    @Test
    void getCollectTaskList_defaultsPagination() {
        when(collectTaskMapper.selectList(anyInt(), anyInt(), any())).thenReturn(List.of());

        service.getCollectTaskList(null, null, null);

        verify(collectTaskMapper).selectList(eq(0), anyInt(), any());
    }

    @Test
    void getCollectTaskList_correctOffset() {
        when(collectTaskMapper.selectList(anyInt(), anyInt(), any())).thenReturn(List.of());

        service.getCollectTaskList(3, 20, null);

        verify(collectTaskMapper).selectList(eq(40), eq(20), any());
    }

    // ==================== view-based methods ====================

    @Test
    void getTaskDetailById_delegatesToViewMapper() {
        Map<String, Object> expected = Map.of("id", 1L, "taskName", "test");
        when(viewMapper.getTaskDetailById(1L)).thenReturn(expected);

        Map<String, Object> result = service.getTaskDetailById(1L);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void getIncrementalTasks_delegatesToViewMapper() {
        List<Map<String, Object>> expected = List.of(Map.of("id", 1L));
        when(viewMapper.getIncrementalTasks()).thenReturn(expected);

        List<Map<String, Object>> result = service.getIncrementalTasks();
        assertThat(result).isEqualTo(expected);
    }

    // ==================== helpers ====================

    private CollectTask buildValidTask() {
        CollectTask task = new CollectTask();
        task.setDataSourceId(1L);
        task.setTaskName("test_collect");
        task.setTableName("users");
        task.setCollectMode("full");
        return task;
    }

    private DataSource buildDataSource(String name) {
        DataSource ds = new DataSource();
        ds.setName(name);
        ds.setDbType("mysql");
        return ds;
    }
}
