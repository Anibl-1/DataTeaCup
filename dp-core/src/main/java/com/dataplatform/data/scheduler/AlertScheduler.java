package com.dataplatform.data.scheduler;

import com.dataplatform.data.service.AlertService;
import com.dataplatform.data.service.SystemMonitorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 告警定时任务
 * 每分钟检查系统指标是否触发告警规则
 */
@Slf4j
@Component
public class AlertScheduler {

    @Autowired
    private AlertService alertService;

    @Autowired
    private SystemMonitorService monitorService;

    @Value("${alert.enabled:false}")
    private boolean alertEnabled;

    /**
     * 每分钟检查告警规则
     */
    @Scheduled(cron = "0 * * * * ?")
    public void checkAlertRules() {
        if (!alertEnabled) {
            return;
        }

        try {
            Map<String, Object> health = monitorService.getSystemHealth();
            Map<String, Double> metrics = new HashMap<>();

            // 提取CPU使用率
            Object cpuUsage = health.get("CPU Usage");
            if (cpuUsage instanceof Number) {
                metrics.put("cpu", ((Number) cpuUsage).doubleValue());
            }

            // 提取内存使用率
            Object memUsage = health.get("Memory Usage (%)");
            if (memUsage instanceof Number) {
                metrics.put("memory", ((Number) memUsage).doubleValue());
            }

            // 提取磁盘使用率
            Object diskUsage = health.get("Disk Usage (%)");
            if (diskUsage instanceof Number) {
                metrics.put("disk", ((Number) diskUsage).doubleValue());
            }

            // 提取JVM堆使用率
            Map<String, Object> jvmMetrics = monitorService.getJvmMetrics();
            Object heapUsage = jvmMetrics.get("heapUsagePercent");
            if (heapUsage instanceof Number) {
                metrics.put("jvm", ((Number) heapUsage).doubleValue());
            }

            // 提取活跃连接数
            Object activeConn = health.get("Active Connections");
            if (activeConn instanceof Number) {
                metrics.put("db", ((Number) activeConn).doubleValue());
            }

            alertService.checkAllMetrics(metrics);

        } catch (Exception e) {
            log.error("[AlertScheduler] 告警检查异常: {}", e.getMessage());
        }
    }
}
