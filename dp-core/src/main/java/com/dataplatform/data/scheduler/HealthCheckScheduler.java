package com.dataplatform.data.scheduler;

import com.dataplatform.data.service.HealthCheckService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 健康检查定时任务
 * 每5分钟自动执行所有组件健康检查
 */
@Slf4j
@Component
public class HealthCheckScheduler {

    @Autowired
    private HealthCheckService healthCheckService;

    /**
     * 每5分钟执行一次健康检查
     */
    @Scheduled(cron = "0 0/5 * * * ?")
    public void scheduledHealthCheck() {
        try {
            healthCheckService.runAllChecks();
            log.debug("[HealthCheckScheduler] 健康检查完成");
        } catch (Exception e) {
            log.error("[HealthCheckScheduler] 健康检查异常: {}", e.getMessage());
        }
    }

    /**
     * 每天凌晨3点清理30天前的健康检查记录
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanOldRecords() {
        try {
            int deleted = healthCheckService.cleanOldRecords(30);
            if (deleted > 0) {
                log.info("[HealthCheckScheduler] 清理{}条过期健康检查记录", deleted);
            }
        } catch (Exception e) {
            log.error("[HealthCheckScheduler] 清理过期记录异常: {}", e.getMessage());
        }
    }
}
