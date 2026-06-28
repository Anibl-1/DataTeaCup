package com.dataplatform.data.service.health.checker;

import com.dataplatform.data.service.health.ComponentHealth;
import com.dataplatform.data.service.health.HealthChecker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * 磁盘空间健康检查器
 */
@Slf4j
@Component
public class DiskSpaceHealthChecker implements HealthChecker {

    @Value("${health.disk.threshold:0.9}")
    private double threshold;

    @Override
    public ComponentHealth check() {
        long start = System.currentTimeMillis();
        ComponentHealth health = new ComponentHealth();
        health.setName(getName());
        health.setCritical(false);

        try {
            File root = new File("/");
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                root = new File("C:");
            }

            long totalSpace = root.getTotalSpace();
            long freeSpace = root.getFreeSpace();
            long usedSpace = totalSpace - freeSpace;
            double usageRatio = (double) usedSpace / totalSpace;

            health.setResponseTime(System.currentTimeMillis() - start);
            health.withDetail("total", formatBytes(totalSpace))
                  .withDetail("free", formatBytes(freeSpace))
                  .withDetail("used", formatBytes(usedSpace))
                  .withDetail("usageRatio", String.format("%.2f%%", usageRatio * 100));

            if (usageRatio < threshold) {
                health.setStatus(ComponentHealth.STATUS_UP);
                health.setMessage("磁盘空间充足");
            } else {
                health.setStatus(ComponentHealth.STATUS_DOWN);
                health.setMessage(String.format("磁盘空间不足，使用率: %.2f%%", usageRatio * 100));
            }
        } catch (Exception e) {
            health.setResponseTime(System.currentTimeMillis() - start);
            health.setStatus(ComponentHealth.STATUS_DOWN);
            health.setMessage("磁盘检查失败: " + e.getMessage());
            log.error("磁盘健康检查失败", e);
        }

        return health;
    }

    @Override
    public String getName() {
        return "diskSpace";
    }

    @Override
    public int getCheckInterval() {
        return 60;
    }

    @Override
    public boolean isCritical() {
        return false;
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
