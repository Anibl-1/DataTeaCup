package com.dataplatform;

import com.dataplatform.data.entity.Announcement;
import com.dataplatform.data.mapper.*;
import com.dataplatform.data.service.AnnouncementService;
import com.dataplatform.data.service.DashboardService;
import com.dataplatform.org.entity.Department;
import com.dataplatform.org.mapper.DepartmentMapper;
import com.dataplatform.system.entity.User;
import com.dataplatform.system.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.reflect.Field;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * DashboardService 单元测试
 * 覆盖: getStats, getWorkspace, getCollectTrend, getDataSourceDistribution, calculateSuccessRate
 */
class DashboardServiceTest {

    private DashboardService service;
    private DataSourceMapper dataSourceMapper;
    private CollectTaskMapper collectTaskMapper;
    private UserMapper userMapper;
    private ReportDefinitionMapper reportDefinitionMapper;
    private ChartDefinitionMapper chartDefinitionMapper;
    private AnnouncementService announcementService;
    private NotificationMapper notificationMapper;
    private PipelineExecutionMapper pipelineExecutionMapper;
    private DepartmentMapper departmentMapper;
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() throws Exception {
        service = new DashboardService();
        dataSourceMapper = mock(DataSourceMapper.class);
        collectTaskMapper = mock(CollectTaskMapper.class);
        userMapper = mock(UserMapper.class);
        reportDefinitionMapper = mock(ReportDefinitionMapper.class);
        chartDefinitionMapper = mock(ChartDefinitionMapper.class);
        announcementService = mock(AnnouncementService.class);
        notificationMapper = mock(NotificationMapper.class);
        pipelineExecutionMapper = mock(PipelineExecutionMapper.class);
        departmentMapper = mock(DepartmentMapper.class);
        jdbcTemplate = mock(JdbcTemplate.class);

        injectField("dataSourceMapper", dataSourceMapper);
        injectField("collectTaskMapper", collectTaskMapper);
        injectField("userMapper", userMapper);
        injectField("reportDefinitionMapper", reportDefinitionMapper);
        injectField("chartDefinitionMapper", chartDefinitionMapper);
        injectField("announcementService", announcementService);
        injectField("notificationMapper", notificationMapper);
        injectField("pipelineExecutionMapper", pipelineExecutionMapper);
        injectField("departmentMapper", departmentMapper);
        injectField("jdbcTemplate", jdbcTemplate);
    }

    private void injectField(String name, Object value) throws Exception {
        Field f = DashboardService.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(service, value);
    }

    // ==================== getStats ====================

    @Test
    void getStats_returnsAllKeys() {
        when(dataSourceMapper.count(any())).thenReturn(5L);
        when(collectTaskMapper.count(any())).thenReturn(10L);
        when(collectTaskMapper.countToday()).thenReturn(3L);
        when(userMapper.count(any())).thenReturn(20L);
        when(reportDefinitionMapper.count(any())).thenReturn(15L);
        when(chartDefinitionMapper.count(any(), any(), any())).thenReturn(8L);
        when(collectTaskMapper.countThisWeek()).thenReturn(25L);
        when(collectTaskMapper.countByStatus(3)).thenReturn(1L);

        Map<String, Object> stats = service.getStats();

        assertThat(stats).containsKeys(
            "dataSourceCount", "collectTaskCount", "todayCollectCount",
            "userCount", "reportCount", "chartCount", "weekCollectCount", "successRate"
        );
        assertThat(stats.get("dataSourceCount")).isEqualTo(5L);
        assertThat(stats.get("userCount")).isEqualTo(20L);
    }

    @Test
    void getStats_successRate_100_whenNoTasks() {
        when(dataSourceMapper.count(any())).thenReturn(0L);
        when(collectTaskMapper.count(any())).thenReturn(0L);
        when(collectTaskMapper.countToday()).thenReturn(0L);
        when(userMapper.count(any())).thenReturn(0L);
        when(reportDefinitionMapper.count(any())).thenReturn(0L);
        when(chartDefinitionMapper.count(any(), any(), any())).thenReturn(0L);
        when(collectTaskMapper.countThisWeek()).thenReturn(0L);

        Map<String, Object> stats = service.getStats();
        assertThat(stats.get("successRate")).isEqualTo("100");
    }

    @Test
    void getStats_successRate_calculatedCorrectly() {
        when(dataSourceMapper.count(any())).thenReturn(0L);
        when(collectTaskMapper.count(any())).thenReturn(100L);
        when(collectTaskMapper.countToday()).thenReturn(0L);
        when(userMapper.count(any())).thenReturn(0L);
        when(reportDefinitionMapper.count(any())).thenReturn(0L);
        when(chartDefinitionMapper.count(any(), any(), any())).thenReturn(0L);
        when(collectTaskMapper.countThisWeek()).thenReturn(0L);
        when(collectTaskMapper.countByStatus(3)).thenReturn(10L);

        Map<String, Object> stats = service.getStats();
        assertThat(stats.get("successRate")).isEqualTo("90.0");
    }

    // ==================== getWorkspace ====================

    @Test
    void getWorkspace_withUserId_returnsAllSections() {
        Long userId = 1L;
        when(reportDefinitionMapper.count(any())).thenReturn(5L);
        when(chartDefinitionMapper.count(any(), any(), any())).thenReturn(3L);
        when(dataSourceMapper.count(any())).thenReturn(2L);
        when(collectTaskMapper.count(any())).thenReturn(4L);
        when(collectTaskMapper.countToday()).thenReturn(1L);
        when(notificationMapper.countUnread(userId)).thenReturn(7);
        when(announcementService.getActiveAnnouncements()).thenReturn(new ArrayList<>());
        when(pipelineExecutionMapper.countAll()).thenReturn(50);
        when(pipelineExecutionMapper.countByStatus(anyInt())).thenReturn(10);
        when(pipelineExecutionMapper.countToday()).thenReturn(5);
        when(userMapper.count(any())).thenReturn(20L);
        when(departmentMapper.countAll()).thenReturn(3L);
        when(jdbcTemplate.queryForList(anyString(), any(Object.class))).thenReturn(new ArrayList<>());

        User user = new User();
        user.setNickname("测试用户");
        user.setDeptId(1L);
        when(userMapper.selectById(userId)).thenReturn(user);

        Department dept = new Department();
        dept.setDeptName("技术部");
        when(departmentMapper.selectById(1L)).thenReturn(dept);

        when(collectTaskMapper.countByDate(anyString())).thenReturn(0L);

        Map<String, Object> ws = service.getWorkspace(userId);

        assertThat(ws).containsKeys(
            "reportCount", "chartCount", "dataSourceCount",
            "unreadNotificationCount", "pipelineStats", "quickActions",
            "nickname", "deptName", "collectTrend"
        );
        assertThat(ws.get("unreadNotificationCount")).isEqualTo(7);
        assertThat(ws.get("nickname")).isEqualTo("测试用户");
        assertThat(ws.get("deptName")).isEqualTo("技术部");
    }

    @Test
    void getWorkspace_pipelineStats_usesCorrectStatusCodes() {
        // Verify BUG-1 fix: status codes must match PipelineService (0=failed, 1=success, 2=running)
        when(reportDefinitionMapper.count(any())).thenReturn(0L);
        when(chartDefinitionMapper.count(any(), any(), any())).thenReturn(0L);
        when(dataSourceMapper.count(any())).thenReturn(0L);
        when(collectTaskMapper.count(any())).thenReturn(0L);
        when(collectTaskMapper.countToday()).thenReturn(0L);
        when(notificationMapper.countUnread(1L)).thenReturn(0);
        when(announcementService.getActiveAnnouncements()).thenReturn(new ArrayList<>());
        when(userMapper.count(any())).thenReturn(0L);
        when(departmentMapper.countAll()).thenReturn(0L);
        when(collectTaskMapper.countByDate(anyString())).thenReturn(0L);
        when(jdbcTemplate.queryForList(anyString(), any(Object.class))).thenReturn(new ArrayList<>());

        when(pipelineExecutionMapper.countAll()).thenReturn(100);
        when(pipelineExecutionMapper.countByStatus(2)).thenReturn(5);   // running
        when(pipelineExecutionMapper.countByStatus(1)).thenReturn(80);  // success
        when(pipelineExecutionMapper.countByStatus(0)).thenReturn(15);  // failed
        when(pipelineExecutionMapper.countToday()).thenReturn(10);

        Map<String, Object> ws = service.getWorkspace(1L);

        @SuppressWarnings("unchecked")
        Map<String, Object> ps = (Map<String, Object>) ws.get("pipelineStats");
        assertThat(ps.get("runningCount")).isEqualTo(5);
        assertThat(ps.get("successToday")).isEqualTo(80);
        assertThat(ps.get("failedToday")).isEqualTo(15);

        // Verify correct mapper calls: 2=running, 1=success, 0=failed
        verify(pipelineExecutionMapper).countByStatus(2);
        verify(pipelineExecutionMapper).countByStatus(1);
        verify(pipelineExecutionMapper).countByStatus(0);
    }

    @Test
    void getWorkspace_nullUserId_doesNotCrash() {
        when(reportDefinitionMapper.count(any())).thenReturn(0L);
        when(chartDefinitionMapper.count(any(), any(), any())).thenReturn(0L);
        when(dataSourceMapper.count(any())).thenReturn(0L);
        when(collectTaskMapper.count(any())).thenReturn(0L);
        when(collectTaskMapper.countToday()).thenReturn(0L);
        when(announcementService.getActiveAnnouncements()).thenReturn(null);
        when(pipelineExecutionMapper.countAll()).thenReturn(0);
        when(pipelineExecutionMapper.countByStatus(anyInt())).thenReturn(0);
        when(pipelineExecutionMapper.countToday()).thenReturn(0);
        when(userMapper.count(any())).thenReturn(0L);
        when(departmentMapper.countAll()).thenReturn(0L);
        when(collectTaskMapper.countByDate(anyString())).thenReturn(0L);
        when(jdbcTemplate.queryForList(anyString())).thenReturn(new ArrayList<>());

        Map<String, Object> ws = service.getWorkspace(null);

        assertThat(ws).isNotNull();
        assertThat(ws.get("unreadNotificationCount")).isEqualTo(0);
        assertThat((List<?>) ws.get("announcements")).isEmpty();
    }

    @Test
    void getWorkspace_announcements_cappedAt3() {
        when(reportDefinitionMapper.count(any())).thenReturn(0L);
        when(chartDefinitionMapper.count(any(), any(), any())).thenReturn(0L);
        when(dataSourceMapper.count(any())).thenReturn(0L);
        when(collectTaskMapper.count(any())).thenReturn(0L);
        when(collectTaskMapper.countToday()).thenReturn(0L);
        when(notificationMapper.countUnread(1L)).thenReturn(0);
        when(pipelineExecutionMapper.countAll()).thenReturn(0);
        when(pipelineExecutionMapper.countByStatus(anyInt())).thenReturn(0);
        when(pipelineExecutionMapper.countToday()).thenReturn(0);
        when(userMapper.count(any())).thenReturn(0L);
        when(departmentMapper.countAll()).thenReturn(0L);
        when(collectTaskMapper.countByDate(anyString())).thenReturn(0L);
        when(jdbcTemplate.queryForList(anyString(), any(Object.class))).thenReturn(new ArrayList<>());

        List<Announcement> manyAnnouncements = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            manyAnnouncements.add(new Announcement());
        }
        when(announcementService.getActiveAnnouncements()).thenReturn(manyAnnouncements);

        Map<String, Object> ws = service.getWorkspace(1L);
        assertThat((List<?>) ws.get("announcements")).hasSize(3);
    }

    // ==================== getCollectTrend ====================

    @Test
    void getCollectTrend_returns7Days() {
        when(collectTaskMapper.countByDate(anyString())).thenReturn(5L);

        Map<String, Object> trend = service.getCollectTrend();

        assertThat((List<?>) trend.get("dates")).hasSize(7);
        assertThat((List<?>) trend.get("counts")).hasSize(7);
        verify(collectTaskMapper, times(7)).countByDate(anyString());
    }

    // ==================== getDashboardAnnouncements ====================

    @Test
    void getDashboardAnnouncements_cappedAt5() {
        List<Announcement> many = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            many.add(new Announcement());
        }
        when(announcementService.getActiveAnnouncements()).thenReturn(many);

        List<Announcement> result = service.getDashboardAnnouncements();
        assertThat(result).hasSize(5);
    }

    @Test
    void getDashboardAnnouncements_returnsEmpty_whenNull() {
        when(announcementService.getActiveAnnouncements()).thenReturn(null);
        List<Announcement> result = service.getDashboardAnnouncements();
        assertThat(result).isEmpty();
    }

    // ==================== getDataSourceDistribution ====================

    @Test
    void getDataSourceDistribution_delegatesToMapper() {
        List<Map<String, Object>> expected = List.of(Map.of("type", "mysql", "count", 5));
        when(dataSourceMapper.countByDbType()).thenReturn(expected);

        List<Map<String, Object>> result = service.getDataSourceDistribution();
        assertThat(result).isEqualTo(expected);
    }
}
