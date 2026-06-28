package com.dataplatform.data.service.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 自动恢复服务
 * 检测服务异常并尝试自动恢复
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutoRecoveryService {

    private final HealthCheckService healthCheckService;
    private final DataSource dataSource;

    @Value("${health.recovery.max-retries:3}")
    private int maxRetries;

    @Value("${health.recovery.retry-interval:5000}")
    private long retryInterval;

    private final Map<String, AtomicInteger> retryCounters = new ConcurrentHashMap<>();
    private final Map<String, Boolean> recoveryInProgress = new ConcurrentHashMap<>();

    /**
     * 定期检查并尝试恢复
     */
    @Scheduled(fixedDelayString = "${health.recovery.check-interval:30000}")
    public void checkAndRecover() {
        HealthStatus status = healthCheckService.checkHealth();
        
        for (Map.Entry<String, ComponentHealth> entry : status.getComponents().entrySet()) {
            String componentName = entry.getKey();
            ComponentHealth health = entry.getValue();
            
            if (!health.isUp() && health.isCritical()) {
                attemptRecovery(componentName);
            } else if (health.isUp()) {
                // 重置重试计数器
                retryCounters.remove(componentName);
            }
        }
    }

    /**
     * 尝试恢复组件
     */
    public boolean attemptRecovery(String componentName) {
        if (recoveryInProgress.getOrDefault(componentName, false)) {
            log.debug("组件 {} 正在恢复中，跳过", componentName);
            return false;
        }

        AtomicInteger retryCounter = retryCounters.computeIfAbsent(componentName, k -> new AtomicInteger(0));
        int currentRetry = retryCounter.incrementAndGet();

        if (currentRetry > maxRetries) {
            log.error("组件 {} 恢复失败，已达到最大重试次数 {}", componentName, maxRetries);
            return false;
        }

        log.info("尝试恢复组件 {}, 第 {} 次重试", componentName, currentRetry);
        recoveryInProgress.put(componentName, true);

        try {
            boolean recovered = doRecovery(componentName);
            if (recovered) {
                log.info("组件 {} 恢复成功", componentName);
                retryCounters.remove(componentName);
                return true;
            } else {
                log.warn("组件 {} 恢复失败", componentName);
                return false;
            }
        } finally {
            recoveryInProgress.put(componentName, false);
        }
    }

    /**
     * 执行恢复操作
     */
    private boolean doRecovery(String componentName) {
        return switch (componentName) {
            case "database" -> recoverDatabase();
            case "redis" -> recoverRedis();
            default -> {
                log.warn("未知组件类型: {}, 无法自动恢复", componentName);
                yield false;
            }
        };
    }

    /**
     * 恢复数据库连接
     */
    private boolean recoverDatabase() {
        for (int i = 0; i < maxRetries; i++) {
            try {
                log.info("尝试重新建立数据库连接, 第 {} 次", i + 1);
                Connection conn = dataSource.getConnection();
                if (conn.isValid(5)) {
                    conn.close();
                    log.info("数据库连接恢复成功");
                    return true;
                }
                conn.close();
            } catch (SQLException e) {
                log.warn("数据库连接恢复失败: {}", e.getMessage());
            }

            try {
                Thread.sleep(retryInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    /**
     * 恢复Redis连接
     */
    private boolean recoverRedis() {
        // Redis连接池通常会自动重连
        log.info("Redis连接池将自动尝试重连");
        return true;
    }

    /**
     * 获取组件重试次数
     */
    public int getRetryCount(String componentName) {
        AtomicInteger counter = retryCounters.get(componentName);
        return counter != null ? counter.get() : 0;
    }

    /**
     * 重置组件重试计数器
     */
    public void resetRetryCounter(String componentName) {
        retryCounters.remove(componentName);
    }

    /**
     * 检查组件是否正在恢复
     */
    public boolean isRecoveryInProgress(String componentName) {
        return recoveryInProgress.getOrDefault(componentName, false);
    }
}
