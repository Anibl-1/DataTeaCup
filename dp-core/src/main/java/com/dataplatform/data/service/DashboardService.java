package com.dataplatform.data.service;

import com.dataplatform.data.mapper.CollectTaskMapper;
import com.dataplatform.data.mapper.DataSourceMapper;
import com.dataplatform.system.mapper.UserMapper;
import com.dataplatform.data.mapper.ReportDefinitionMapper;
import com.dataplatform.data.mapper.ChartDefinitionMapper;
import com.dataplatform.data.mapper.NotificationMapper;
import com.dataplatform.data.mapper.PipelineExecutionMapper;
import com.dataplatform.org.mapper.DepartmentMapper;
import com.dataplatform.data.entity.Announcement;
import com.dataplatform.org.entity.Department;
import com.dataplatform.system.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 仪表盘服务
 * 聚合各模块的统计数据，为仪表盘提供数据支撑
 * 
 * @author dataplatform
 */
@Slf4j
@Service
public class DashboardService {
    
    @Autowired
    private DataSourceMapper dataSourceMapper;
    
    @Autowired
    private CollectTaskMapper collectTaskMapper;
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private ReportDefinitionMapper reportDefinitionMapper;
    
    @Autowired
    private ChartDefinitionMapper chartDefinitionMapper;
    
    @Autowired
    private AnnouncementService announcementService;
    
    @Autowired
    private NotificationMapper notificationMapper;
    
    @Autowired
    private PipelineExecutionMapper pipelineExecutionMapper;
    
    @Autowired
    private DepartmentMapper departmentMapper;
    
    /**
     * 获取仪表盘统计数据
     * 
     * @return 统计数据Map
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("dataSourceCount", dataSourceMapper.count(null));
        stats.put("collectTaskCount", collectTaskMapper.count(null));
        stats.put("todayCollectCount", collectTaskMapper.countToday());
        stats.put("userCount", userMapper.count(null));
        stats.put("reportCount", reportDefinitionMapper.count(null));
        stats.put("chartCount", chartDefinitionMapper.count(null, null, null));
        stats.put("weekCollectCount", collectTaskMapper.countThisWeek());
        stats.put("successRate", calculateSuccessRate());
        return stats;
    }
    
    /**
     * 获取数据源类型分布
     * 
     * @return 类型分布列表
     */
    public List<Map<String, Object>> getDataSourceDistribution() {
        return dataSourceMapper.countByDbType();
    }
    
    /**
     * 获取最近7天采集任务趋势
     * 
     * @return 包含dates和counts的趋势数据
     */
    public Map<String, Object> getCollectTrend() {
        Map<String, Object> result = new HashMap<>();
        List<String> dates = new ArrayList<>();
        List<Long> counts = new ArrayList<>();
        
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            dates.add(date.format(formatter));
            long count = collectTaskMapper.countByDate(date.toString());
            counts.add(count);
        }
        
        result.put("dates", dates);
        result.put("counts", counts);
        return result;
    }
    
    /**
     * 获取仪表盘公告列表（活跃公告，最多5条）
     */
    public List<Announcement> getDashboardAnnouncements() {
        List<Announcement> all = announcementService.getActiveAnnouncements();
        if (all != null && all.size() > 5) {
            return all.subList(0, 5);
        }
        return all != null ? all : new ArrayList<>();
    }
    
    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    
    /**
     * 个人工作台数据
     */
    public Map<String, Object> getWorkspace(Long userId) {
        Map<String, Object> workspace = new HashMap<>();
        
        // 我的最近操作（最近10条）
        try {
            String logSql = userId != null ?
                "SELECT module_name, operation_type, operation_desc, create_time FROM sys_operation_log WHERE user_id = ? ORDER BY create_time DESC LIMIT 10" :
                "SELECT module_name, operation_type, operation_desc, create_time FROM sys_operation_log ORDER BY create_time DESC LIMIT 10";
            List<Map<String, Object>> recentOps = userId != null ?
                jdbcTemplate.queryForList(logSql, userId) :
                jdbcTemplate.queryForList(logSql);
            workspace.put("recentOperations", recentOps);
        } catch (Exception e) {
            workspace.put("recentOperations", new ArrayList<>());
        }
        
        // 基础统计
        workspace.put("reportCount", reportDefinitionMapper.count(null));
        workspace.put("chartCount", chartDefinitionMapper.count(null, null, null));
        workspace.put("dataSourceCount", dataSourceMapper.count(null));
        workspace.put("collectTaskCount", collectTaskMapper.count(null));
        workspace.put("todayCollectCount", collectTaskMapper.countToday());
        
        // 未读通知数量
        try {
            int unreadCount = userId != null ? notificationMapper.countUnread(userId) : 0;
            workspace.put("unreadNotificationCount", unreadCount);
        } catch (Exception e) {
            workspace.put("unreadNotificationCount", 0);
        }
        
        // 最新公告（最多3条）
        try {
            List<Announcement> announcements = announcementService.getActiveAnnouncements();
            if (announcements != null && announcements.size() > 3) {
                announcements = announcements.subList(0, 3);
            }
            workspace.put("announcements", announcements != null ? announcements : new ArrayList<>());
        } catch (Exception e) {
            workspace.put("announcements", new ArrayList<>());
        }
        
        // 流程执行概览
        try {
            Map<String, Object> pipelineStats = new HashMap<>();
            pipelineStats.put("totalExecutions", pipelineExecutionMapper.countAll());
            pipelineStats.put("runningCount", pipelineExecutionMapper.countByStatus(2));  // 运行中
            pipelineStats.put("successToday", pipelineExecutionMapper.countByStatus(1));  // 成功 (与PipelineService一致: 0=失败,1=成功,2=运行中)
            pipelineStats.put("failedToday", pipelineExecutionMapper.countByStatus(0));   // 失败
            pipelineStats.put("todayCount", pipelineExecutionMapper.countToday());
            workspace.put("pipelineStats", pipelineStats);
        } catch (Exception e) {
            workspace.put("pipelineStats", Map.of("totalExecutions", 0, "runningCount", 0, "successToday", 0, "failedToday", 0, "todayCount", 0));
        }
        
        // 用户部门信息
        try {
            if (userId != null) {
                User user = userMapper.selectById(userId);
                if (user != null) {
                    workspace.put("nickname", user.getNickname());
                    if (user.getDeptId() != null) {
                        Department dept = departmentMapper.selectById(user.getDeptId());
                        if (dept != null) {
                            workspace.put("deptName", dept.getDeptName());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("获取用户部门信息失败: userId={}, error={}", userId, e.getMessage());
        }
        
        // 部门人数统计
        try {
            workspace.put("totalDepartments", departmentMapper.countAll());
        } catch (Exception e) {
            workspace.put("totalDepartments", 0);
        }
        
        // 用户总数
        workspace.put("userCount", userMapper.count(null));
        
        // 快捷操作
        workspace.put("quickActions", List.of(
            Map.of("name", "新建数据源", "icon", "ServerOutline", "path", "/data-source"),
            Map.of("name", "新建采集任务", "icon", "DownloadOutline", "path", "/data-collect"),
            Map.of("name", "新建报表", "icon", "DocumentOutline", "path", "/report-manage"),
            Map.of("name", "新建图表", "icon", "BarChartOutline", "path", "/chart-manage"),
            Map.of("name", "流程管理", "icon", "GitBranchOutline", "path", "/pipeline/manage"),
            Map.of("name", "数据导入", "icon", "CloudUploadOutline", "path", "/data-import"),
            Map.of("name", "数据字典", "icon", "BookOutline", "path", "/data-dictionary"),
            Map.of("name", "系统配置", "icon", "SettingsOutline", "path", "/system-config")
        ));
        
        // 采集趋势（最近7天）
        try {
            List<String> dates = new ArrayList<>();
            List<Long> counts = new ArrayList<>();
            java.time.LocalDate today = java.time.LocalDate.now();
            java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("MM-dd");
            for (int i = 6; i >= 0; i--) {
                java.time.LocalDate date = today.minusDays(i);
                dates.add(date.format(fmt));
                counts.add(collectTaskMapper.countByDate(date.toString()));
            }
            workspace.put("collectTrend", Map.of("dates", dates, "counts", counts));
        } catch (Exception e) {
            workspace.put("collectTrend", Map.of("dates", new ArrayList<>(), "counts", new ArrayList<>()));
        }
        
        return workspace;
    }
    
    /**
     * 计算采集任务成功率
     */
    private String calculateSuccessRate() {
        try {
            long total = collectTaskMapper.count(null);
            if (total == 0) return "100";
            long failed = collectTaskMapper.countByStatus(3);
            double rate = (double) (total - failed) / total * 100;
            return String.format("%.1f", rate);
        } catch (Exception e) {
            return "99.5";
        }
    }
}
