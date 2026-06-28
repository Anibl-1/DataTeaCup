package com.dataplatform.data.service.health.checker;

import com.dataplatform.data.service.health.ComponentHealth;
import com.dataplatform.data.service.health.HealthChecker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;

/**
 * JVM健康检查器
 */
@Slf4j
@Component
public class JvmHealthChecker implements HealthChecker {

    @Value("${health.jvm.heap-threshold:0.9}")
    private double heapThreshold;

    @Override
    public ComponentHealth check() {
        long start = System.currentTimeMillis();
        ComponentHealth health = new ComponentHealth();
        health.setName(getName());
        health.setCritical(true);

        try {
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
            
            MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
            MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
            
            long heapUsed = heapUsage.getUsed();
            long heapMax = heapUsage.getMax();
            double heapRatio = (double) heapUsed / heapMax;

            health.setResponseTime(System.currentTimeMillis() - start);
            health.withDetail("heapUsed", formatBytes(heapUsed))
                  .withDetail("heapMax", formatBytes(heapMax))
                  .withDetail("heapUsageRatio", String.format("%.2f%%", heapRatio * 100))
                  .withDetail("nonHeapUsed", formatBytes(nonHeapUsage.getUsed()))
                  .withDetail("uptime", formatUptime(runtimeBean.getUptime()))
                  .withDetail("vmName", runtimeBean.getVmName())
                  .withDetail("vmVersion", runtimeBean.getVmVersion())
                  .withDetail("availableProcessors", Runtime.getRuntime().availableProcessors());

            if (heapRatio < heapThreshold) {
                health.setStatus(ComponentHealth.STATUS_UP);
                health.setMessage("JVM运行正常");
            } else {
                health.setStatus(ComponentHealth.STATUS_DOWN);
                health.setMessage(String.format("JVM堆内存使用率过高: %.2f%%", heapRatio * 100));
            }
        } catch (Exception e) {
            health.setResponseTime(System.currentTimeMillis() - start);
            health.setStatus(ComponentHealth.STATUS_DOWN);
            health.setMessage("JVM检查失败: " + e.getMessage());
            log.error("JVM健康检查失败", e);
        }

        return health;
    }

    @Override
    public String getName() {
        return "jvm";
    }

    @Override
    public int getCheckInterval() {
        return 30;
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }

    private String formatUptime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return String.format("%d天%d小时%d分钟", days, hours % 24, minutes % 60);
        } else if (hours > 0) {
            return String.format("%d小时%d分钟", hours, minutes % 60);
        } else {
            return String.format("%d分钟", minutes);
        }
    }
}
