package com.dataplatform.data.service;

import com.dataplatform.common.exception.BusinessException;
import com.dataplatform.common.exception.ErrorCode;
import com.dataplatform.data.entity.MonitorMetric;
import com.dataplatform.data.mapper.DatabaseViewMapper;
import com.dataplatform.data.mapper.MonitorMetricMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 系统监控服务
 * 使用OSHI库和JdbcTemplate实现系统监控功能
 */
@Slf4j
@Service
public class SystemMonitorService {
    
    private final DatabaseViewMapper viewMapper;
    private final JdbcTemplate jdbcTemplate;
    private final MonitorMetricMapper monitorMetricMapper;
    private final AlertNotificationService alertNotificationService;

    public SystemMonitorService(
            DatabaseViewMapper viewMapper,
            JdbcTemplate jdbcTemplate,
            MonitorMetricMapper monitorMetricMapper,
            @Nullable AlertNotificationService alertNotificationService) {
        this.viewMapper = viewMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.monitorMetricMapper = monitorMetricMapper;
        this.alertNotificationService = alertNotificationService;
    }
    
    // OSHI系统信息对象（单例，避免重复创建）
    private final SystemInfo systemInfo = new SystemInfo();
    private final HardwareAbstractionLayer hardware = systemInfo.getHardware();
    private final OperatingSystem os = systemInfo.getOperatingSystem();

    /** 缓存的CPU使用率，由后台线程定时采样更新，避免getSystemHealth()阻塞500ms */
    private volatile double cachedCpuUsage = 0.0;
    private volatile long[] prevCpuTicks = null;

    @jakarta.annotation.PostConstruct
    public void initCpuSampler() {
        // 初始化CPU ticks
        prevCpuTicks = hardware.getProcessor().getSystemCpuLoadTicks();
        // 后台线程每5秒采样CPU使用率
        java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "cpu-sampler");
            t.setDaemon(true);
            return t;
        }).scheduleAtFixedRate(() -> {
            try {
                CentralProcessor processor = hardware.getProcessor();
                long[] currentTicks = processor.getSystemCpuLoadTicks();
                if (prevCpuTicks != null) {
                    cachedCpuUsage = processor.getSystemCpuLoadBetweenTicks(prevCpuTicks) * 100;
                }
                prevCpuTicks = currentTicks;
            } catch (Exception e) {
                log.debug("CPU采样异常", e);
            }
        }, 2, 5, java.util.concurrent.TimeUnit.SECONDS);
    }
    
    /**
     * 获取系统健康状态
     * 使用OSHI库获取服务器系统信息，使用JdbcTemplate获取数据库信息
     */
    public Map<String, Object> getSystemHealth() {
        try {
            Map<String, Object> result = new LinkedHashMap<>();
            
            // ========== 服务器系统信息（使用OSHI） ==========
            
            // 1. CPU使用率（使用后台缓存值，避免阻塞）
            result.put("CPU Usage", Math.round(cachedCpuUsage * 100.0) / 100.0);
            
            // 2. 内存信息
            GlobalMemory memory = hardware.getMemory();
            long totalMemory = memory.getTotal();
            long availableMemory = memory.getAvailable();
            long usedMemory = totalMemory - availableMemory;
            result.put("Total Memory (GB)", Math.round(totalMemory / 1024.0 / 1024.0 / 1024.0 * 100.0) / 100.0);
            result.put("Used Memory (GB)", Math.round(usedMemory / 1024.0 / 1024.0 / 1024.0 * 100.0) / 100.0);
            result.put("Memory Usage (%)", Math.round((double) usedMemory / totalMemory * 100.0 * 100.0) / 100.0);
            
            // 3. 磁盘空间信息
            FileSystem fileSystem = os.getFileSystem();
            List<OSFileStore> fileStores = fileSystem.getFileStores();
            
            long totalDiskSpace = 0;
            long usableDiskSpace = 0;
            
            for (OSFileStore store : fileStores) {
                totalDiskSpace += store.getTotalSpace();
                usableDiskSpace += store.getUsableSpace();
            }
            
            long usedDiskSpace = totalDiskSpace - usableDiskSpace;
            result.put("Total Disk Space (GB)", Math.round(totalDiskSpace / 1024.0 / 1024.0 / 1024.0 * 100.0) / 100.0);
            result.put("Used Disk Space (GB)", Math.round(usedDiskSpace / 1024.0 / 1024.0 / 1024.0 * 100.0) / 100.0);
            result.put("Available Disk Space (GB)", Math.round(usableDiskSpace / 1024.0 / 1024.0 / 1024.0 * 100.0) / 100.0);
            result.put("Disk Usage (%)", totalDiskSpace > 0 ? Math.round((double) usedDiskSpace / totalDiskSpace * 100.0 * 100.0) / 100.0 : 0);
            
            // ========== 数据库信息 ==========
            
            // 4. 数据库详细大小信息
            String dbDetailQuery = "SELECT " +
                    "ROUND(SUM(data_length) / 1024 / 1024, 2) AS data_size, " +
                    "ROUND(SUM(index_length) / 1024 / 1024, 2) AS index_size, " +
                    "ROUND(SUM(data_free) / 1024 / 1024, 2) AS free_size, " +
                    "ROUND(SUM(data_length + index_length) / 1024 / 1024, 2) AS total_size " +
                    "FROM information_schema.TABLES WHERE table_schema = 'data_platform'";
            Map<String, Object> dbDetail = jdbcTemplate.queryForMap(dbDetailQuery);
            result.put("Database Size (MB)", dbDetail.get("total_size") != null ? dbDetail.get("total_size") : 0.0);
            result.put("Data Size (MB)", dbDetail.get("data_size") != null ? dbDetail.get("data_size") : 0.0);
            result.put("Index Size (MB)", dbDetail.get("index_size") != null ? dbDetail.get("index_size") : 0.0);
            result.put("Free Size (MB)", dbDetail.get("free_size") != null ? dbDetail.get("free_size") : 0.0);
            
            // 5. 表数量
            String tableCountQuery = "SELECT COUNT(*) FROM information_schema.TABLES " +
                    "WHERE table_schema = 'data_platform' AND table_type = 'BASE TABLE'";
            Long tableCount = jdbcTemplate.queryForObject(tableCountQuery, Long.class);
            result.put("Total Tables", tableCount != null ? tableCount : 0L);
            
            // 6. 活跃任务数
            String runningTasksQuery = "SELECT COUNT(*) FROM collect_task WHERE status = 'running'";
            Long runningTasks = jdbcTemplate.queryForObject(runningTasksQuery, Long.class);
            result.put("Running Tasks", runningTasks != null ? runningTasks : 0L);
            
            // 7. 今日操作日志数
            String todayOpsQuery = "SELECT COUNT(*) FROM sys_operation_log WHERE DATE(create_time) = CURDATE()";
            Long todayOps = jdbcTemplate.queryForObject(todayOpsQuery, Long.class);
            result.put("Today Operations", todayOps != null ? todayOps : 0L);
            
            // 8. 未读通知数
            String unreadQuery = "SELECT COUNT(*) FROM sys_notification WHERE is_read = 0";
            Long unreadCount = jdbcTemplate.queryForObject(unreadQuery, Long.class);
            result.put("Unread Notifications", unreadCount != null ? unreadCount : 0L);
            
            // 9. 活跃数据库连接数
            String activeConnQuery = "SELECT COUNT(*) FROM information_schema.PROCESSLIST WHERE command != 'Sleep'";
            Long activeConns = jdbcTemplate.queryForObject(activeConnQuery, Long.class);
            result.put("Active DB Connections", activeConns != null ? activeConns : 0L);
            
            // 10. 慢查询数
            String slowQueryCountQuery = "SHOW GLOBAL STATUS LIKE 'Slow_queries'";
            try {
                Map<String, Object> slowResult = jdbcTemplate.queryForMap(slowQueryCountQuery);
                result.put("Slow Queries", Long.parseLong(slowResult.get("Value").toString()));
            } catch (Exception e) {
                result.put("Slow Queries", 0L);
            }
            
            log.info("系统健康检查完成");
            return result;
            
        } catch (Exception e) {
            log.error("系统健康检查失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_CONFIG_ERROR, "系统健康检查失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取活跃任务列表
     * 使用视图查询，性能优于传统JOIN查询
     */
    public List<Map<String, Object>> getActiveTasks() {
        try {
            List<Map<String, Object>> activeTasks = viewMapper.getActiveTasks();
            log.info("查询到{}个活跃任务", activeTasks.size());
            return activeTasks;
            
        } catch (Exception e) {
            log.error("查询活跃任务失败", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 获取数据源使用统计
     * 使用视图自动聚合统计信息
     */
    public List<Map<String, Object>> getDataSourceUsageStats() {
        try {
            return viewMapper.getDataSourceUsage();
        } catch (Exception e) {
            log.error("查询数据源使用统计失败", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 获取操作日志汇总
     * 使用视图查询最近7天的日志统计
     */
    public List<Map<String, Object>> getOperationLogSummary() {
        try {
            return viewMapper.getOperationLogSummary();
        } catch (Exception e) {
            log.error("查询操作日志汇总失败", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 清理过期日志
     * 用Java实现原存储过程的功能
     * 
     * @param retentionDays 日志保留天数
     * @return 清理结果
     */
    public Map<String, Object> cleanExpiredLogs(int retentionDays) {
        try {
            String deleteQuery = "DELETE FROM sys_operation_log " +
                    "WHERE create_time < DATE_SUB(NOW(), INTERVAL ? DAY)";
            int deletedCount = jdbcTemplate.update(deleteQuery, retentionDays);
            
            Map<String, Object> result = new HashMap<>();
            result.put("deleted_records", deletedCount);
            result.put("cleanup_time", new Date());
            
            log.info("清理过期日志完成，删除{}条记录", deletedCount);
            return result;
            
        } catch (Exception e) {
            log.error("清理过期日志失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_CONFIG_ERROR, "清理过期日志失败: " + e.getMessage());
        }
    }
    
    /**
     * 重置超时任务
     * 用Java实现原存储过程的功能
     * 
     * @param timeoutHours 超时小时数
     * @return 重置结果
     */
    public Map<String, Object> resetStuckTasks(int timeoutHours) {
        try {
            String updateQuery = "UPDATE collect_task " +
                    "SET status = 'error', " +
                    "last_execute_result = CONCAT('任务超时（运行超过', ?, '小时），已自动停止'), " +
                    "update_time = NOW() " +
                    "WHERE status = 'running' " +
                    "AND last_execute_time < DATE_SUB(NOW(), INTERVAL ? HOUR)";
            int resetCount = jdbcTemplate.update(updateQuery, timeoutHours, timeoutHours);
            
            Map<String, Object> result = new HashMap<>();
            result.put("reset_tasks", resetCount);
            result.put("reset_time", new Date());
            
            log.info("重置超时任务完成，重置{}个任务", resetCount);
            return result;
            
        } catch (Exception e) {
            log.error("重置超时任务失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_CONFIG_ERROR, "重置超时任务失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取任务执行报告
     * 用Java实现原存储过程的功能
     */
    public List<Map<String, Object>> getTaskReport(Date startDate, Date endDate) {
        try {
            String query = "SELECT " +
                    "DATE(last_execute_time) AS execute_date, " +
                    "COUNT(*) AS total_executions, " +
                    "COUNT(CASE WHEN status = 'running' THEN 1 END) AS running_count, " +
                    "COUNT(CASE WHEN status = 'stopped' THEN 1 END) AS stopped_count, " +
                    "COUNT(CASE WHEN status = 'error' THEN 1 END) AS error_count, " +
                    "COUNT(CASE WHEN last_execute_result LIKE '%成功%' THEN 1 END) AS success_count " +
                    "FROM collect_task " +
                    "WHERE last_execute_time BETWEEN ? AND ? " +
                    "GROUP BY DATE(last_execute_time) " +
                    "ORDER BY execute_date DESC";
            return jdbcTemplate.queryForList(query, startDate, endDate);
        } catch (Exception e) {
            log.error("获取任务报告失败", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 获取JVM监控指标
     * 包含堆内存、非堆内存、GC、线程等信息
     */
    public Map<String, Object> getJvmMetrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();
        try {
            // 堆内存
            MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
            MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
            metrics.put("heapUsed", heapUsage.getUsed() / 1024 / 1024);
            metrics.put("heapMax", heapUsage.getMax() / 1024 / 1024);
            metrics.put("heapCommitted", heapUsage.getCommitted() / 1024 / 1024);
            metrics.put("heapUsagePercent", heapUsage.getMax() > 0
                    ? Math.round(heapUsage.getUsed() * 100.0 / heapUsage.getMax() * 100.0) / 100.0 : 0);

            // 非堆内存
            MemoryUsage nonHeapUsage = memoryMXBean.getNonHeapMemoryUsage();
            metrics.put("nonHeapUsed", nonHeapUsage.getUsed() / 1024 / 1024);
            metrics.put("nonHeapCommitted", nonHeapUsage.getCommitted() / 1024 / 1024);

            // 线程
            ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
            metrics.put("threadCount", threadMXBean.getThreadCount());
            metrics.put("peakThreadCount", threadMXBean.getPeakThreadCount());
            metrics.put("daemonThreadCount", threadMXBean.getDaemonThreadCount());
            metrics.put("totalStartedThreadCount", threadMXBean.getTotalStartedThreadCount());

            // GC
            List<Map<String, Object>> gcInfos = new ArrayList<>();
            long totalGcCount = 0;
            long totalGcTime = 0;
            for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
                Map<String, Object> gcInfo = new LinkedHashMap<>();
                gcInfo.put("name", gc.getName());
                gcInfo.put("collectionCount", gc.getCollectionCount());
                gcInfo.put("collectionTime", gc.getCollectionTime());
                gcInfos.add(gcInfo);
                if (gc.getCollectionCount() >= 0) totalGcCount += gc.getCollectionCount();
                if (gc.getCollectionTime() >= 0) totalGcTime += gc.getCollectionTime();
            }
            metrics.put("gcDetails", gcInfos);
            metrics.put("totalGcCount", totalGcCount);
            metrics.put("totalGcTime", totalGcTime);

            // JVM启动时间
            long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
            metrics.put("uptimeSeconds", uptimeMs / 1000);
            metrics.put("uptimeFormatted", formatUptime(uptimeMs));

            // 类加载
            metrics.put("loadedClassCount", ManagementFactory.getClassLoadingMXBean().getLoadedClassCount());

        } catch (Exception e) {
            log.error("获取JVM指标失败", e);
        }
        return metrics;
    }

    @Value("${alert.threshold.cpu:90}")
    private double cpuThreshold;

    @Value("${alert.threshold.memory:85}")
    private double memoryThreshold;

    @Value("${alert.threshold.disk:90}")
    private double diskThreshold;

    @Value("${alert.cooldown.minutes:10}")
    private int alertCooldownMinutes;

    /** 告警冷却记录：告警类型 -> 上次发送时间 */
    private final Map<String, Long> alertCooldownMap = new HashMap<>();

    /**
     * 定时采集监控指标（每5分钟一次）
     */
    @Scheduled(fixedRate = 300000, initialDelay = 60000)
    public void collectMetrics() {
        try {
            Map<String, Object> health = getSystemHealth();
            Map<String, Object> jvm = getJvmMetrics();

            double cpuUsage = toDouble(health.get("CPU Usage"));
            double memoryUsage = toDouble(health.get("Memory Usage (%)"));
            double diskUsage = toDouble(health.get("Disk Usage (%)"));

            MonitorMetric metric = new MonitorMetric();
            metric.setCpuUsage(cpuUsage);
            metric.setMemoryUsage(memoryUsage);
            metric.setDiskUsage(diskUsage);
            metric.setHeapUsed(toLong(jvm.get("heapUsed")));
            metric.setHeapMax(toLong(jvm.get("heapMax")));
            metric.setThreadCount(toInt(jvm.get("threadCount")));
            metric.setGcCount(toLong(jvm.get("totalGcCount")));
            metric.setActiveConnections(toInt(health.get("Active DB Connections")));
            metric.setRunningTasks(toInt(health.get("Running Tasks")));
            metric.setCollectTime(new Date());

            monitorMetricMapper.insert(metric);

            // 阈值告警检查
            checkThresholdAlert("CPU", cpuUsage, cpuThreshold);
            checkThresholdAlert("内存", memoryUsage, memoryThreshold);
            checkThresholdAlert("磁盘", diskUsage, diskThreshold);

            // 清理7天前的旧数据
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, -7);
            monitorMetricMapper.deleteOlderThan(cal.getTime());

            log.debug("监控指标采集完成");
        } catch (Exception e) {
            log.error("监控指标采集失败", e);
        }
    }

    /**
     * 检查阈值并发送告警（带冷却机制）
     */
    private void checkThresholdAlert(String resourceType, double currentValue, double threshold) {
        if (currentValue <= threshold || alertNotificationService == null) {
            return;
        }
        long now = System.currentTimeMillis();
        Long lastAlertTime = alertCooldownMap.get(resourceType);
        if (lastAlertTime != null && (now - lastAlertTime) < alertCooldownMinutes * 60_000L) {
            return; // 冷却期内，不重复发送
        }
        alertNotificationService.sendResourceAlert(resourceType, currentValue, threshold);
        alertCooldownMap.put(resourceType, now);
        log.warn("监控告警: {}使用率 {}% 超过阈值 {}%", resourceType, String.format("%.1f", currentValue), String.format("%.1f", threshold));
    }

    /**
     * 获取监控指标历史趋势
     */
    public List<Map<String, Object>> getMetricsHistory(int hours) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR_OF_DAY, -hours);
        List<MonitorMetric> metrics = monitorMetricMapper.findSince(cal.getTime());

        List<Map<String, Object>> result = new ArrayList<>();
        for (MonitorMetric m : metrics) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("time", new java.text.SimpleDateFormat("HH:mm").format(m.getCollectTime()));
            item.put("cpuUsage", m.getCpuUsage());
            item.put("memoryUsage", m.getMemoryUsage());
            item.put("diskUsage", m.getDiskUsage());
            item.put("heapUsed", m.getHeapUsed());
            item.put("heapMax", m.getHeapMax());
            item.put("threadCount", m.getThreadCount());
            item.put("gcCount", m.getGcCount());
            item.put("activeConnections", m.getActiveConnections());
            item.put("runningTasks", m.getRunningTasks());
            result.add(item);
        }
        return result;
    }

    private double toDouble(Object val) {
        return val instanceof Number ? ((Number) val).doubleValue() : 0.0;
    }

    private long toLong(Object val) {
        return val instanceof Number ? ((Number) val).longValue() : 0L;
    }

    private int toInt(Object val) {
        return val instanceof Number ? ((Number) val).intValue() : 0;
    }

    private String formatUptime(long uptimeMs) {
        long seconds = uptimeMs / 1000;
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        if (days > 0) return days + "天" + hours + "小时" + minutes + "分钟";
        if (hours > 0) return hours + "小时" + minutes + "分钟";
        return minutes + "分钟";
    }

    /**
     * 分析数据源使用情况
     * 用Java实现原存储过程的功能
     */
    public List<Map<String, Object>> analyzeDataSourceUsage() {
        try {
            String query = "SELECT " +
                    "ds.id, " +
                    "ds.name, " +
                    "ds.db_type, " +
                    "COUNT(DISTINCT ct.id) AS task_count, " +
                    "COUNT(DISTINCT r.id) AS report_count, " +
                    "COUNT(DISTINCT c.id) AS chart_count, " +
                    "ds.create_time " +
                    "FROM data_source ds " +
                    "LEFT JOIN collect_task ct ON ds.id = ct.data_source_id OR ds.id = ct.target_data_source_id " +
                    "LEFT JOIN report_definition r ON ds.id = r.data_source_id " +
                    "LEFT JOIN chart_definition c ON ds.id = c.data_source_id " +
                    "GROUP BY ds.id, ds.name, ds.db_type, ds.create_time " +
                    "ORDER BY task_count DESC, report_count DESC";
            return jdbcTemplate.queryForList(query);
        } catch (Exception e) {
            log.error("分析数据源使用失败", e);
            return Collections.emptyList();
        }
    }
}
