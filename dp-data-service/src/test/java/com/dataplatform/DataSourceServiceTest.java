package com.dataplatform;

import com.dataplatform.common.exception.BusinessException;
import com.dataplatform.data.entity.DataSource;
import com.dataplatform.data.mapper.DataSourceMapper;
import com.dataplatform.data.service.DataSourceConnectionPoolManager;
import com.dataplatform.data.service.DataSourceService;
import com.dataplatform.data.service.DbConnectionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * DataSourceService 单元测试
 * 覆盖: CRUD操作、参数校验、testConnection、deleteDataSource关联检查、batchTestConnection
 */
class DataSourceServiceTest {

    private DataSourceService service;
    private DataSourceMapper dataSourceMapper;
    private DbConnectionUtil dbConnectionUtil;
    private JdbcTemplate jdbcTemplate;
    private DataSourceConnectionPoolManager connectionPoolManager;

    @BeforeEach
    void setUp() {
        dataSourceMapper = mock(DataSourceMapper.class);
        dbConnectionUtil = mock(DbConnectionUtil.class);
        jdbcTemplate = mock(JdbcTemplate.class);
        connectionPoolManager = mock(DataSourceConnectionPoolManager.class);

        service = new DataSourceService(dataSourceMapper, dbConnectionUtil, jdbcTemplate, connectionPoolManager);
        // Initialize caches (normally done in @PostConstruct)
        service.initCache();
    }

    // ==================== createDataSource ====================

    @Test
    void createDataSource_success() {
        DataSource ds = buildValidDataSource();
        doNothing().when(dataSourceMapper).insert(any());

        service.createDataSource(ds);

        verify(dataSourceMapper).insert(ds);
    }

    @Test
    void createDataSource_throwsWhenNull() {
        assertThatThrownBy(() -> service.createDataSource(null))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("不能为空");
    }

    @Test
    void createDataSource_throwsWhenNameEmpty() {
        DataSource ds = buildValidDataSource();
        ds.setName("");

        assertThatThrownBy(() -> service.createDataSource(ds))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("名称");
    }

    @Test
    void createDataSource_throwsWhenDbTypeEmpty() {
        DataSource ds = buildValidDataSource();
        ds.setDbType("");

        assertThatThrownBy(() -> service.createDataSource(ds))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("数据库类型");
    }

    @Test
    void createDataSource_throwsWhenHostEmpty() {
        DataSource ds = buildValidDataSource();
        ds.setHost("");

        assertThatThrownBy(() -> service.createDataSource(ds))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("主机地址");
    }

    @Test
    void createDataSource_throwsWhenPortInvalid() {
        DataSource ds = buildValidDataSource();
        ds.setPort(0);

        assertThatThrownBy(() -> service.createDataSource(ds))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("端口号");

        ds.setPort(70000);
        assertThatThrownBy(() -> service.createDataSource(ds))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("端口号");
    }

    @Test
    void createDataSource_throwsWhenDatabaseEmpty() {
        DataSource ds = buildValidDataSource();
        ds.setDatabase("");

        assertThatThrownBy(() -> service.createDataSource(ds))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("数据库名");
    }

    @Test
    void createDataSource_throwsWhenUsernameEmpty() {
        DataSource ds = buildValidDataSource();
        ds.setUsername("");

        assertThatThrownBy(() -> service.createDataSource(ds))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("用户名");
    }

    @Test
    void createDataSource_throwsWhenPasswordEmpty() {
        DataSource ds = buildValidDataSource();
        ds.setPassword("");

        assertThatThrownBy(() -> service.createDataSource(ds))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("密码");
    }

    @Test
    void createDataSource_trimsDatabaseField() {
        DataSource ds = buildValidDataSource();
        ds.setDatabase("  testdb  ");
        doNothing().when(dataSourceMapper).insert(any());

        service.createDataSource(ds);

        assertThat(ds.getDatabase()).isEqualTo("testdb");
    }

    // ==================== updateDataSource ====================

    @Test
    void updateDataSource_throwsWhenNull() {
        assertThatThrownBy(() -> service.updateDataSource(null))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void updateDataSource_throwsWhenIdNull() {
        DataSource ds = new DataSource();
        assertThatThrownBy(() -> service.updateDataSource(ds))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("ID");
    }

    @Test
    void updateDataSource_throwsWhenNotFound() {
        DataSource ds = new DataSource();
        ds.setId(999L);
        when(dataSourceMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> service.updateDataSource(ds))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("不存在");
    }

    @Test
    void updateDataSource_fallsBackToExistingValues() {
        DataSource existing = buildValidDataSource();
        existing.setId(1L);
        existing.setName("original");
        existing.setDbType("mysql");
        when(dataSourceMapper.selectById(1L)).thenReturn(existing);

        DataSource update = new DataSource();
        update.setId(1L);
        // name, dbType etc are null/empty — should fall back to existing

        service.updateDataSource(update);

        assertThat(update.getName()).isEqualTo("original");
        assertThat(update.getDbType()).isEqualTo("mysql");
        verify(dataSourceMapper).update(update);
    }

    // ==================== deleteDataSource ====================

    @Test
    void deleteDataSource_throwsWhenIdNull() {
        assertThatThrownBy(() -> service.deleteDataSource(null))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("ID");
    }

    @Test
    void deleteDataSource_throwsWhenNotFound() {
        when(dataSourceMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> service.deleteDataSource(999L))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("不存在");
    }

    @Test
    void deleteDataSource_throwsWhenInUse() {
        DataSource ds = buildValidDataSource();
        ds.setId(1L);
        when(dataSourceMapper.selectById(1L)).thenReturn(ds);
        when(jdbcTemplate.queryForObject(contains("collect_task"), eq(Long.class), eq(1L), eq(1L))).thenReturn(3L);
        when(jdbcTemplate.queryForObject(contains("report_definition"), eq(Long.class), eq(1L))).thenReturn(0L);
        when(jdbcTemplate.queryForObject(contains("chart_definition"), eq(Long.class), eq(1L))).thenReturn(0L);

        assertThatThrownBy(() -> service.deleteDataSource(1L))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("正在被使用");
    }

    @Test
    void deleteDataSource_success_whenNoReferences() {
        DataSource ds = buildValidDataSource();
        ds.setId(1L);
        when(dataSourceMapper.selectById(1L)).thenReturn(ds);
        when(jdbcTemplate.queryForObject(contains("collect_task"), eq(Long.class), eq(1L), eq(1L))).thenReturn(0L);
        when(jdbcTemplate.queryForObject(contains("report_definition"), eq(Long.class), eq(1L))).thenReturn(0L);
        when(jdbcTemplate.queryForObject(contains("chart_definition"), eq(Long.class), eq(1L))).thenReturn(0L);

        service.deleteDataSource(1L);

        verify(dataSourceMapper).delete(1L);
    }

    // ==================== testConnection ====================

    @Test
    void testConnection_throwsWhenNull() {
        assertThatThrownBy(() -> service.testConnection(null))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void testConnection_delegatesToDbConnectionUtil() {
        DataSource ds = buildValidDataSource();
        doNothing().when(dbConnectionUtil).testConnection(ds);

        service.testConnection(ds);

        verify(dbConnectionUtil).testConnection(ds);
    }

    // ==================== batchTestConnection ====================

    @Test
    void batchTestConnection_handlesNotFound() {
        when(dataSourceMapper.selectById(999L)).thenReturn(null);

        List<Map<String, Object>> results = service.batchTestConnection(List.of(999L));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).get("success")).isEqualTo(false);
        assertThat(results.get(0).get("message")).isEqualTo("数据源不存在");
    }

    @Test
    void batchTestConnection_recordsSuccessAndResponseTime() {
        DataSource ds = buildValidDataSource();
        ds.setId(1L);
        ds.setName("test-mysql");
        when(dataSourceMapper.selectById(1L)).thenReturn(ds);
        doNothing().when(dbConnectionUtil).testConnection(ds);

        List<Map<String, Object>> results = service.batchTestConnection(List.of(1L));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).get("success")).isEqualTo(true);
        assertThat(results.get(0).get("name")).isEqualTo("test-mysql");
        assertThat(results.get(0)).containsKey("responseTime");
        verify(dataSourceMapper).update(argThat(d -> d.getLastTestResult() == 1));
    }

    @Test
    void batchTestConnection_recordsFailure() {
        DataSource ds = buildValidDataSource();
        ds.setId(2L);
        ds.setName("bad-ds");
        when(dataSourceMapper.selectById(2L)).thenReturn(ds);
        doThrow(new RuntimeException("Connection refused")).when(dbConnectionUtil).testConnection(ds);

        List<Map<String, Object>> results = service.batchTestConnection(List.of(2L));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).get("success")).isEqualTo(false);
        assertThat(results.get(0).get("message")).isEqualTo("Connection refused");
        verify(dataSourceMapper).update(argThat(d -> d.getLastTestResult() == 0));
    }

    // ==================== getDataSourceList ====================

    @Test
    void getDataSourceList_defaultsPagination_whenNull() {
        when(dataSourceMapper.selectList(anyInt(), anyInt(), any())).thenReturn(List.of());

        service.getDataSourceList(null, null, null);

        // Should use default page=1, pageSize=10 -> offset=0
        verify(dataSourceMapper).selectList(eq(0), anyInt(), any());
    }

    @Test
    void getDataSourceList_correctOffset() {
        when(dataSourceMapper.selectList(anyInt(), anyInt(), any())).thenReturn(List.of());

        service.getDataSourceList(3, 20, null);

        verify(dataSourceMapper).selectList(eq(40), eq(20), any()); // (3-1)*20 = 40
    }

    // ==================== getDataSource (wrapper) ====================

    @Test
    void getDataSource_throwsWhenNotFound() {
        when(dataSourceMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> service.getDataSource(999L))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("不存在");
    }

    // ==================== getDataSourceDetail ====================

    @Test
    void getDataSourceDetail_throwsWhenNotFound() {
        when(dataSourceMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> service.getDataSourceDetail(999L))
            .isInstanceOf(BusinessException.class);
    }

    // ==================== helpers ====================

    private DataSource buildValidDataSource() {
        DataSource ds = new DataSource();
        ds.setName("test-mysql");
        ds.setDbType("mysql");
        ds.setHost("localhost");
        ds.setPort(3306);
        ds.setDatabase("testdb");
        ds.setUsername("root");
        ds.setPassword("password");
        return ds;
    }
}
