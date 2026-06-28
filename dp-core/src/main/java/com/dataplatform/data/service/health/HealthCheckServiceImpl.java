package com.dataplatform.data.service.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

/**
 * 健康检查服务实现
 */
@Slf4j
@Service
public class HealthCheckServiceImpl implements HealthCheckService {

    private final Map<String, HealthChecker> checkers = new ConcurrentHashMap<>();
    private final Map<String, List<HealthRecord>> healthHistory = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(5);
    
    private static final int MAX_HISTORY_SIZE = 1000;

    @Override
    public HealthStatus checkHealth() {
        long startTime = System.currentTimeMillis();
        HealthStatus status = new HealthStatus();
        Map<String, ComponentHealth> components = new ConcurrentHashMap<>();
        
        List<Future<ComponentHealth>> futures = new ArrayList<>();
        
        // 并行执行所有健康检查
        for (Map.Entry<String, HealthChecker> entry : checkers.entrySet()) {
            futures.add(executor.submit(() -> {
                try {
                    return entry.getValue().check();
                } catch (Exception e) {
                    log.error("健康检查失败: {}", entry.getKey(), e);
                    return ComponentHealth.down(entry.getKey(), "检查异常: " + e.getMessage());
                }
            }));
        }
        
        // 收集结果
        int healthyCount = 0;
        int unhealthyCount = 0;
        boolean hasCriticalFailure = false;
        
        for (Future<ComponentHealth> future : futures) {
            try {
                ComponentHealth health = future.get(10, TimeUnit.SECONDS);
                components.put(health.getName(), health);
                
                // 记录历史
                recordHealth(health);
                
                if (health.isUp()) {
                    healthyCount++;
                } else {
                    unhealthyCount++;
                    if (health.isCritical()) {
                        hasCriticalFailure = true;
                    }
                }
            } catch (TimeoutException e) {
                log.warn("健康检查超时");
                unhealthyCount++;
            } catch (Exception e) {
                log.error("获取健康检查结果失败", e);
                unhealthyCount++;
            }
        }
        
        // 确定整体状态
        if (hasCriticalFailure) {
            status.setStatus(HealthStatus.STATUS_DOWN);
        } else if (unhealthyCount > 0) {
            status.setStatus(HealthStatus.STATUS_DEGRADED);
        } else {
            status.setStatus(HealthStatus.STATUS_UP);
        }
        
        status.setComponents(components);
        status.setCheckTime(LocalDateTime.now());
        status.setHealthyCount(healthyCount);
        status.setUnhealthyCount(unhealthyCount);
        status.setTotalCheckTime(System.currentTimeMillis() - startTime);
        
        return status;
    }

    @Override
    public ComponentHealth checkComponent(String componentName) {
        HealthChecker checker = checkers.get(componentName);
        if (checker == null) {
            return ComponentHealth.down(componentName, "组件不存在");
        }
        
        try {
            ComponentHealth health = checker.check();
            recordHealth(health);
            return health;
        } catch (Exception e) {
            log.error("检查组件失败: {}", componentName, e);
            return ComponentHealth.down(componentName, "检查异常: " + e.getMessage());
        }
    }

    @Override
    public List<HealthRecord> getHealthHistory(String componentName, int hours) {
        List<HealthRecord> records = healthHistory.getOrDefault(componentName, new ArrayList<>());
        LocalDateTime cutoff = LocalDateTime.now().minusHours(hours);
        
        return records.stream()
            .filter(r -> r.getCheckTime().isAfter(cutoff))
            .toList();
    }

    @Override
    public void registerChecker(String name, HealthChecker checker) {
        checkers.put(name, checker);
        log.info("注册健康检查器: {}", name);
    }

    @Override
    public Map<String, HealthChecker> getCheckers() {
        return Collections.unmodifiableMap(checkers);
    }

    @Override
    public boolean isHealthy() {
        HealthStatus status = checkHealth();
        return HealthStatus.STATUS_UP.equals(status.getStatus());
    }

    private void recordHealth(ComponentHealth health) {
        HealthRecord record = new HealthRecord();
        record.setComponentName(health.getName());
        record.setStatus(health.getStatus());
        record.setMessage(health.getMessage());
        record.setResponseTime(health.getResponseTime());
        record.setCheckTime(LocalDateTime.now());
        
        healthHistory.computeIfAbsent(health.getName(), k -> new CopyOnWriteArrayList<>())
            .add(record);
        
        // 限制历史记录大小
        List<HealthRecord> records = healthHistory.get(health.getName());
        if (records.size() > MAX_HISTORY_SIZE) {
            records.subList(0, records.size() - MAX_HISTORY_SIZE).clear();
        }
    }
}
