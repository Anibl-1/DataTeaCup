package com.dataplatform.system.controller;

import com.dataplatform.common.annotation.RequirePermission;
import com.dataplatform.common.Result;
import com.dataplatform.data.entity.HealthCheck;
import com.dataplatform.data.service.HealthCheckService;
import com.dataplatform.data.service.SlowQueryService;
import com.dataplatform.data.service.SystemMonitorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 绯荤粺鐩戞帶鎺у埗鍣?
 * 鎻愪緵绯荤粺鍋ュ悍妫€鏌ャ€佷换鍔＄洃鎺с€佹棩蹇楃鐞嗙瓑鍔熻兘
 */
@RestController
@RequestMapping("/system/monitor")
@RequirePermission("system:monitor")
public class SystemMonitorController {
    
    @Autowired
    private SystemMonitorService monitorService;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SlowQueryService slowQueryService;

    @Autowired
    private HealthCheckService healthCheckService;
    
    /**
     * 绯荤粺鍋ュ悍妫€鏌?
     * GET /system/monitor/health
     * 
     * 浣跨敤OSHI搴撹幏鍙栨湇鍔″櫒绯荤粺淇℃伅锛圕PU銆佸唴瀛樸€佺鐩橈級
     * 浣跨敤JdbcTemplate鑾峰彇鏁版嵁搴撲俊鎭?
     * 
     * 杩斿洖绀轰緥锛?
     * {
     *   "CPU Usage": 45.23,                    // CPU浣跨敤鐜?%)
     *   "Total Memory (GB)": 16.0,             // 鎬诲唴瀛?GB)
     *   "Used Memory (GB)": 8.5,               // 宸茬敤鍐呭瓨(GB)
     *   "Memory Usage (%)": 53.12,             // 鍐呭瓨浣跨敤鐜?%)
     *   "Total Disk Space (GB)": 500.0,        // 鎬荤鐩樼┖闂?GB)
     *   "Used Disk Space (GB)": 320.5,         // 宸茬敤纾佺洏绌洪棿(GB)
     *   "Available Disk Space (GB)": 179.5,    // 鍙敤纾佺洏绌洪棿(GB)
     *   "Disk Usage (%)": 64.1,                // 纾佺洏浣跨敤鐜?%)
     *   "Database Size (MB)": 256.5,           // 鏁版嵁搴撳ぇ灏?MB)
     *   "Total Tables": 34,                    // 琛ㄦ暟閲?
     *   "Running Tasks": 3,                    // 杩愯涓换鍔℃暟
     *   "Today Operations": 1247,              // 浠婃棩鎿嶄綔鏁?
     *   "Unread Notifications": 15,            // 鏈閫氱煡鏁?
     *   "Active DB Connections": 5             // 娲昏穬鏁版嵁搴撹繛鎺ユ暟
     * }
     */
    @GetMapping("/health")
    public Result<Map<String, Object>> getSystemHealth() {
        Map<String, Object> health = monitorService.getSystemHealth();
        return Result.success(health);
    }
    
    /**
     * 鑾峰彇娲昏穬浠诲姟鍒楄〃
     * GET /system/monitor/active-tasks
     * 
     * 搴旂敤鍦烘櫙锛氫换鍔＄洃鎺ч〉闈㈠疄鏃跺埛鏂?
     */
    @GetMapping("/active-tasks")
    public Result<List<Map<String, Object>>> getActiveTasks() {
        List<Map<String, Object>> tasks = monitorService.getActiveTasks();
        return Result.success(tasks);
    }
    
    /**
     * 鑾峰彇鏁版嵁婧愪娇鐢ㄧ粺璁?
     * GET /system/monitor/datasource-usage
     * 
     * 搴旂敤鍦烘櫙锛氭暟鎹簮绠＄悊椤甸潰銆佷娇鐢ㄥ垎鏋?
     */
    @GetMapping("/datasource-usage")
    public Result<List<Map<String, Object>>> getDataSourceUsage() {
        List<Map<String, Object>> usage = monitorService.getDataSourceUsageStats();
        return Result.success(usage);
    }
    
    /**
     * 鑾峰彇鎿嶄綔鏃ュ織姹囨€?
     * GET /system/monitor/operation-logs
     * 
     * 搴旂敤鍦烘櫙锛氱郴缁熺洃鎺с€佹棩蹇楀垎鏋?
     */
    @GetMapping("/operation-logs")
    public Result<List<Map<String, Object>>> getOperationLogSummary() {
        List<Map<String, Object>> logs = monitorService.getOperationLogSummary();
        return Result.success(logs);
    }
    
    /**
     * 娓呯悊杩囨湡鏃ュ織
     * POST /system/monitor/clean-logs?days=90
     * 
     * @param days 淇濈暀澶╂暟锛堝垹闄澶╁墠鐨勬棩蹇楋級
     * 
     * 搴旂敤鍦烘櫙锛氱郴缁熺淮鎶ゃ€佸畾鏃朵换鍔?
     */
    @RequirePermission("system:monitor")
    @PostMapping("/clean-logs")
    public Result<Map<String, Object>> cleanExpiredLogs(@RequestParam(defaultValue = "90") int days) {
        Map<String, Object> result = monitorService.cleanExpiredLogs(days);
        return Result.success(result);
    }
    
    /**
     * 閲嶇疆瓒呮椂浠诲姟
     * POST /system/monitor/reset-stuck-tasks?hours=24
     * 
     * @param hours 瓒呮椂灏忔椂鏁?
     * 
     * 搴旂敤鍦烘櫙锛氫换鍔＄洃鎺с€佸紓甯告仮澶?
     */
    @RequirePermission("system:monitor")
    @PostMapping("/reset-stuck-tasks")
    public Result<Map<String, Object>> resetStuckTasks(@RequestParam(defaultValue = "24") int hours) {
        Map<String, Object> result = monitorService.resetStuckTasks(hours);
        return Result.success(result);
    }
    
    /**
     * 鑾峰彇浠诲姟鎵ц鎶ュ憡
     * GET /system/monitor/task-report?startDate=2025-01-01&endDate=2025-12-31
     * 
     * @param startDate 寮€濮嬫棩鏈?
     * @param endDate 缁撴潫鏃ユ湡
     * 
     * 搴旂敤鍦烘櫙锛氭姤琛ㄩ〉闈€佹暟鎹垎鏋?
     */
    @GetMapping("/task-report")
    public Result<List<Map<String, Object>>> getTaskReport(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        List<Map<String, Object>> report = monitorService.getTaskReport(startDate, endDate);
        return Result.success(report);
    }
    
    /**
     * 鍒嗘瀽鏁版嵁婧愪娇鐢ㄦ儏鍐?
     * GET /system/monitor/analyze-datasource
     * 
     * 搴旂敤鍦烘櫙锛氭暟鎹簮绠＄悊銆佹€ц兘浼樺寲
     */
    @GetMapping("/analyze-datasource")
    public Result<List<Map<String, Object>>> analyzeDataSourceUsage() {
        List<Map<String, Object>> analysis = monitorService.analyzeDataSourceUsage();
        return Result.success(analysis);
    }

    /**
     * 鑾峰彇JVM鐩戞帶鎸囨爣
     * GET /system/monitor/jvm
     */
    @GetMapping("/jvm")
    public Result<Map<String, Object>> getJvmMetrics() {
        Map<String, Object> metrics = monitorService.getJvmMetrics();
        return Result.success(metrics);
    }

    /**
     * 鑾峰彇鐩戞帶鎸囨爣鍘嗗彶瓒嬪娍
     * GET /system/monitor/metrics/history?hours=24
     */
    @GetMapping("/metrics/history")
    public Result<List<Map<String, Object>>> getMetricsHistory(
            @RequestParam(defaultValue = "24") int hours) {
        List<Map<String, Object>> history = monitorService.getMetricsHistory(hours);
        return Result.success(history);
    }

    /**
     * 鑾峰彇閫氱煡鍒楄〃锛堟寜褰撳墠鐢ㄦ埛杩囨护锛?
     * GET /system/monitor/notifications?limit=20
     */
    @GetMapping("/notifications")
    public Result<List<Map<String, Object>>> getNotifications(
            @RequestParam(defaultValue = "20") int limit,
            HttpServletRequest request) {
        limit = Math.max(1, Math.min(limit, 200));
        Long userId = (Long) request.getAttribute("userId");
        String sql;
        List<Map<String, Object>> list;
        if (userId != null) {
            sql = "SELECT id, title, content, notification_type AS type, is_read AS isRead, create_time AS createTime " +
                  "FROM sys_notification WHERE target_user_id = ? ORDER BY create_time DESC LIMIT ?";
            list = jdbcTemplate.queryForList(sql, userId, limit);
        } else {
            sql = "SELECT id, title, content, notification_type AS type, is_read AS isRead, create_time AS createTime " +
                  "FROM sys_notification ORDER BY create_time DESC LIMIT ?";
            list = jdbcTemplate.queryForList(sql, limit);
        }
        return Result.success(list);
    }

    /**
     * 鑾峰彇鏈閫氱煡鏁伴噺锛堣交閲忕骇鎺ュ彛锛岄伩鍏嶈皟鐢╣etHealth鐨凜PU閲囨牱寮€閿€锛?
     * GET /system/monitor/unread-count
     */
    @GetMapping("/unread-count")
    public Result<Map<String, Object>> getUnreadCount() {
        String sql = "SELECT COUNT(*) FROM sys_notification WHERE is_read = 0";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("unreadCount", count != null ? count : 0L);
        return Result.success(result);
    }

    /**
     * 鏍囪閫氱煡涓哄凡璇?
     * PUT /system/monitor/notifications/{id}/read
     */
    @PutMapping("/notifications/{id}/read")
    public Result<Void> markNotificationRead(@PathVariable Long id) {
        jdbcTemplate.update("UPDATE sys_notification SET is_read = 1, read_time = NOW() WHERE id = ?", id);
        return Result.success(null);
    }

    /**
     * 鏍囪鎵€鏈夐€氱煡涓哄凡璇伙紙鎸夊綋鍓嶇敤鎴疯繃婊わ級
     * PUT /system/monitor/notifications/read-all
     */
    @PutMapping("/notifications/read-all")
    public Result<Void> markAllNotificationsRead(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId != null) {
            jdbcTemplate.update("UPDATE sys_notification SET is_read = 1, read_time = NOW() WHERE is_read = 0 AND target_user_id = ?", userId);
        } else {
            jdbcTemplate.update("UPDATE sys_notification SET is_read = 1, read_time = NOW() WHERE is_read = 0");
        }
        return Result.success(null);
    }

    // ==================== 鎱㈡煡璇㈠垎鏋?====================

    /**
     * 鑾峰彇鎱㈡煡璇㈠垪琛?
     * GET /system/monitor/slow-query/list
     */
    @GetMapping("/slow-query/list")
    public Result<Map<String, Object>> getSlowQueryList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long dataSourceId,
            @RequestParam(required = false) Long minTime,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return Result.success(slowQueryService.listSlowQueries(page, size, dataSourceId, minTime, startDate, endDate));
    }

    /**
     * 鑾峰彇鎱㈡煡璇㈢粺璁?
     * GET /system/monitor/slow-query/stats
     */

    @GetMapping("/slow-query/datasources")
    public Result<java.util.List<Map<String, Object>>> getSlowQueryDataSources() {
        return Result.success(slowQueryService.getDistinctDataSources());
    }

    @GetMapping("/slow-query/threshold")
    public Result<Long> getSlowQueryThreshold() {
        return Result.success(slowQueryService.getThresholdMs());
    }

    @PutMapping("/slow-query/threshold")
    public Result<Long> updateSlowQueryThreshold(@RequestParam long thresholdMs) {
        slowQueryService.setThresholdMs(thresholdMs);
        return Result.success(slowQueryService.getThresholdMs());
    }

    @GetMapping("/slow-query/stats")
    public Result<Map<String, Object>> getSlowQueryStats(
            @RequestParam(defaultValue = "24") int hours) {
        Map<String, Object> stats = new java.util.LinkedHashMap<>();
        stats.put("byDataSource", slowQueryService.getStatsByDataSource(hours));
        stats.put("topSlowQueries", slowQueryService.getTopSlowQueries(hours));
        stats.put("suggestions", slowQueryService.getOptimizationSuggestions(hours));
        return Result.success(stats);
    }

    // ==================== 鍋ュ悍妫€鏌?====================

    /**
     * 鑾峰彇鎵€鏈夌粍浠舵渶鏂板仴搴风姸鎬?
     * GET /system/monitor/health/status
     */
    @GetMapping("/health/status")
    public Result<List<HealthCheck>> getHealthStatus() {
        return Result.success(healthCheckService.getLatestStatus());
    }

    /**
     * 鎵嬪姩瑙﹀彂鍋ュ悍妫€鏌?
     * POST /system/monitor/health/check
     */
    @PostMapping("/health/check")
    public Result<List<HealthCheck>> runHealthCheck() {
        return Result.success(healthCheckService.runAllChecks());
    }

    /**
     * 鑾峰彇鍋ュ悍妫€鏌ュ巻鍙?
     * GET /system/monitor/health/history
     */
    @GetMapping("/health/history")
    public Result<List<HealthCheck>> getHealthHistory(
            @RequestParam(defaultValue = "24") int hours,
            @RequestParam(defaultValue = "100") int limit) {
        return Result.success(healthCheckService.getHistory(hours, limit));
    }
}
