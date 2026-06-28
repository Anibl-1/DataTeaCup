package com.dataplatform.data.service;

import com.dataplatform.data.entity.HealthCheck;
import com.dataplatform.data.mapper.HealthCheckMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 健康检查服务
 * 执行组件健康检查、记录结果、查询状态和历史
 */
@Slf4j
@Service
public class HealthCheckService {

    @Autowired
    private HealthCheckMapper healthCheckMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 执行所有组件健康检查
     */
    public List<HealthCheck> runAllChecks() {
        List<HealthCheck> results = new ArrayList<>();
        results.add(checkDatabase());
        results.add(checkJvm());
        results.add(checkDiskSpace());
        return results;
    }

    /**
     * 检查数据库连接
     */
    public HealthCheck checkDatabase() {
        HealthCheck check = new HealthCheck();
        check.setCheckType("database");
        check.setCheckName("主数据库");
        check.setCheckTime(new Date());

        long start = System.currentTimeMillis();
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            long elapsed = System.currentTimeMillis() - start;
            check.setStatus("healthy");
            check.setResponseTime(elapsed);

            // 获取连接池信息
            try {
                Map<String, Object> poolInfo = new LinkedHashMap<>();
                List<Map<String, Object>> processInfo = jdbcTemplate.queryForList(
                        "SHOW STATUS LIKE 'Threads_connected'");
                if (!processInfo.isEmpty()) {
                    poolInfo.put("activeConnections", processInfo.get(0).get("Value"));
                }
                List<Map<String, Object>> maxConn = jdbcTemplate.queryForList(
                        "SHOW VARIABLES LIKE 'max_connections'");
                if (!maxConn.isEmpty()) {
                    poolInfo.put("maxConnections", maxConn.get(0).get("Value"));
                }
                check.setDetails(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(poolInfo));
            } catch (Exception e) {
                log.debug("获取数据库连接池信息失败", e);
            }
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            check.setStatus("unhealthy");
            check.setResponseTime(elapsed);
            check.setErrorMessage(e.getMessage());
            log.error("数据库健康检查失败", e);
        }

        healthCheckMapper.insert(check);
        return check;
    }

    /**
     * 检查JVM状态
     */
    public HealthCheck checkJvm() {
        HealthCheck check = new HealthCheck();
        check.setCheckType("jvm");
        check.setCheckName("JVM运行时");
        check.setCheckTime(new Date());

        long start = System.currentTimeMillis();
        try {
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            double usagePercent = (double) usedMemory / maxMemory * 100;

            check.setResponseTime(System.currentTimeMillis() - start);

            if (usagePercent > 90) {
                check.setStatus("unhealthy");
                check.setErrorMessage("JVM堆内存使用率超过90%: " + String.format("%.1f%%", usagePercent));
            } else if (usagePercent > 75) {
                check.setStatus("degraded");
            } else {
                check.setStatus("healthy");
            }

            Map<String, Object> details = new LinkedHashMap<>();
            details.put("maxMemoryMB", maxMemory / 1024 / 1024);
            details.put("usedMemoryMB", usedMemory / 1024 / 1024);
            details.put("usagePercent", Math.round(usagePercent * 10.0) / 10.0);
            details.put("threadCount", Thread.activeCount());
            check.setDetails(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(details));
        } catch (Exception e) {
            check.setStatus("unhealthy");
            check.setResponseTime(System.currentTimeMillis() - start);
            check.setErrorMessage(e.getMessage());
        }

        healthCheckMapper.insert(check);
        return check;
    }

    /**
     * 检查磁盘空间
     */
    public HealthCheck checkDiskSpace() {
        HealthCheck check = new HealthCheck();
        check.setCheckType("disk");
        check.setCheckName("磁盘空间");
        check.setCheckTime(new Date());

        long start = System.currentTimeMillis();
        try {
            java.io.File root = new java.io.File(".");
            long totalSpace = root.getTotalSpace();
            long freeSpace = root.getFreeSpace();
            double usagePercent = totalSpace > 0 ? (double) (totalSpace - freeSpace) / totalSpace * 100 : 0;

            check.setResponseTime(System.currentTimeMillis() - start);

            if (usagePercent > 90) {
                check.setStatus("unhealthy");
                check.setErrorMessage("磁盘使用率超过90%: " + String.format("%.1f%%", usagePercent));
            } else if (usagePercent > 80) {
                check.setStatus("degraded");
            } else {
                check.setStatus("healthy");
            }

            Map<String, Object> details = new LinkedHashMap<>();
            details.put("totalGB", Math.round(totalSpace / 1024.0 / 1024 / 1024 * 100.0) / 100.0);
            details.put("freeGB", Math.round(freeSpace / 1024.0 / 1024 / 1024 * 100.0) / 100.0);
            details.put("usagePercent", Math.round(usagePercent * 10.0) / 10.0);
            check.setDetails(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(details));
        } catch (Exception e) {
            check.setStatus("unhealthy");
            check.setResponseTime(System.currentTimeMillis() - start);
            check.setErrorMessage(e.getMessage());
        }

        healthCheckMapper.insert(check);
        return check;
    }

    // ==================== 查询 ====================

    /**
     * 获取所有组件最新健康状态
     */
    public List<HealthCheck> getLatestStatus() {
        return healthCheckMapper.findLatestAll();
    }

    /**
     * 获取健康检查历史
     */
    public List<HealthCheck> getHistory(int hours, int limit) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR_OF_DAY, -hours);
        return healthCheckMapper.findByTimeRange(cal.getTime(), limit);
    }

    /**
     * 清理过期记录
     */
    public int cleanOldRecords(int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -days);
        return healthCheckMapper.deleteOlderThan(cal.getTime());
    }
}
