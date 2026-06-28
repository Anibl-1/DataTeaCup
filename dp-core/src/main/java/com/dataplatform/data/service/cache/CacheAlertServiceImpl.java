package com.dataplatform.data.service.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 缓存告警服务实现
 * 
 * 提供缓存命中率监控和告警功能：
 * - 定期检查 L1 和 L2 缓存命中率
 * - 当命中率低于阈值时触发告警
 * - 支持多种告警通知渠道
 * - 支持告警阈值配置和冷却机制
 * 
 * 需求引用：
 * - 需求 10.4: 当缓存命中率低于70%时，触发告警通知
 * 
 * @see CacheStatsService 缓存统计服务
 * @see AlertNotifier 告警通知器
 */
@Slf4j
@Service
public class CacheAlertServiceImpl implements CacheAlertService {
    
    /**
     * 缓存统计服务
     */
    private final CacheStatsService cacheStatsService;
    
    /**
     * 告警通知器列表
     */
    private final List<AlertNotifier> notifiers = new CopyOnWriteArrayList<>();
    
    /**
     * 告警历史记录
     */
    private final List<CacheAlert> alertHistory = Collections.synchronizedList(new LinkedList<>());
    
    /**
     * 最大告警历史记录数
     */
    private static final int MAX_ALERT_HISTORY = 1000;
    
    /**
     * 告警阈值配置
     */
    private volatile AlertThresholdConfig thresholdConfig;
    
    /**
     * 是否启用告警
     */
    private final AtomicBoolean enabled = new AtomicBoolean(true);
    
    /**
     * 上次告警时间（用于冷却机制）
     */
    private final Map<CacheStatsService.CacheLevel, LocalDateTime> lastAlertTime = new ConcurrentHashMap<>();
    
    @Autowired
    public CacheAlertServiceImpl(CacheStatsService cacheStatsService,
                                  @Autowired(required = false) List<AlertNotifier> notifiers) {
        this.cacheStatsService = cacheStatsService;
        this.thresholdConfig = AlertThresholdConfig.defaultConfig();
        
        // 注册所有可用的通知器
        if (notifiers != null) {
            for (AlertNotifier notifier : notifiers) {
                if (notifier.isAvailable()) {
                    this.notifiers.add(notifier);
                    log.info("注册告警通知器: {}", notifier.getName());
                }
            }
        }
        
        log.info("缓存告警服务初始化完成 - 健康阈值: {}%, 警告阈值: {}%",
                thresholdConfig.getHealthyThreshold() * 100,
                thresholdConfig.getWarningThreshold() * 100);
    }
    
    @Override
    @Scheduled(fixedRate = 60000) // 每分钟检查一次
    public List<CacheAlert> checkAndAlert() {
        if (!enabled.get()) {
            return Collections.emptyList();
        }
        
        List<CacheAlert> alerts = new ArrayList<>();
        
        // 检查 L1 缓存
        if (thresholdConfig.isL1AlertEnabled()) {
            CacheAlert l1Alert = checkHitRate(CacheStatsService.CacheLevel.L1);
            if (l1Alert != null) {
                alerts.add(l1Alert);
            }
        }
        
        // 检查 L2 缓存
        if (thresholdConfig.isL2AlertEnabled()) {
            CacheAlert l2Alert = checkHitRate(CacheStatsService.CacheLevel.L2);
            if (l2Alert != null) {
                alerts.add(l2Alert);
            }
        }
        
        // 发送告警通知
        for (CacheAlert alert : alerts) {
            sendNotifications(alert);
            addToHistory(alert);
        }
        
        return alerts;
    }
    
    @Override
    public CacheAlert checkHitRate(CacheStatsService.CacheLevel cacheLevel) {
        if (cacheLevel == null) {
            return null;
        }
        
        // 检查冷却时间
        if (isInCooldown(cacheLevel)) {
            log.debug("{} 缓存告警处于冷却期，跳过检查", cacheLevel.getDescription());
            return null;
        }
        
        // 获取缓存统计
        double hitRate;
        long totalRequests;
        
        if (cacheLevel == CacheStatsService.CacheLevel.L1) {
            L1CacheStats stats = cacheStatsService.getL1Stats();
            hitRate = stats.getHitRate();
            totalRequests = stats.getHitCount() + stats.getMissCount();
        } else {
            L2CacheStats stats = cacheStatsService.getL2Stats();
            if (!stats.isAvailable()) {
                log.debug("L2 缓存不可用，跳过告警检查");
                return null;
            }
            hitRate = stats.getHitRate();
            totalRequests = stats.getHitCount() + stats.getMissCount();
        }
        
        // 检查最小请求数阈值
        if (totalRequests < thresholdConfig.getMinRequestCount()) {
            log.debug("{} 缓存请求数 {} 低于最小阈值 {}，跳过告警检查",
                    cacheLevel.getDescription(), totalRequests, thresholdConfig.getMinRequestCount());
            return null;
        }
        
        // 评估告警级别
        CacheAlert.AlertLevel alertLevel = evaluateAlertLevel(hitRate);
        if (alertLevel == null) {
            return null;
        }
        
        // 创建告警
        double threshold = alertLevel == CacheAlert.AlertLevel.CRITICAL 
                ? thresholdConfig.getWarningThreshold() 
                : thresholdConfig.getHealthyThreshold();
        
        CacheAlert alert = CacheAlert.builder()
                .alertId(generateAlertId())
                .alertTime(LocalDateTime.now())
                .cacheLevel(cacheLevel)
                .alertLevel(alertLevel)
                .currentHitRate(hitRate)
                .threshold(threshold)
                .message(CacheAlert.generateMessage(cacheLevel, alertLevel, hitRate, threshold))
                .notified(false)
                .build();
        
        // 更新上次告警时间
        lastAlertTime.put(cacheLevel, LocalDateTime.now());
        
        log.info("触发缓存告警: {}", alert.getMessage());
        
        return alert;
    }
    
    @Override
    public AlertThresholdConfig getThresholdConfig() {
        return thresholdConfig;
    }
    
    @Override
    public void updateThresholdConfig(AlertThresholdConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("配置不能为空");
        }
        config.validate();
        this.thresholdConfig = config;
        log.info("更新告警阈值配置 - 健康阈值: {}%, 警告阈值: {}%",
                config.getHealthyThreshold() * 100,
                config.getWarningThreshold() * 100);
    }
    
    @Override
    public void registerNotifier(AlertNotifier notifier) {
        if (notifier != null && !notifiers.contains(notifier)) {
            notifiers.add(notifier);
            log.info("注册告警通知器: {}", notifier.getName());
        }
    }
    
    @Override
    public void removeNotifier(AlertNotifier notifier) {
        if (notifier != null) {
            notifiers.remove(notifier);
            log.info("移除告警通知器: {}", notifier.getName());
        }
    }
    
    @Override
    public List<CacheAlert> getRecentAlerts(int limit) {
        if (limit <= 0) {
            return Collections.emptyList();
        }
        
        synchronized (alertHistory) {
            int size = alertHistory.size();
            int fromIndex = Math.max(0, size - limit);
            return new ArrayList<>(alertHistory.subList(fromIndex, size));
        }
    }
    
    @Override
    public void clearAlertHistory() {
        alertHistory.clear();
        log.info("告警历史已清除");
    }
    
    @Override
    public void setEnabled(boolean enabled) {
        this.enabled.set(enabled);
        log.info("缓存告警服务已{}", enabled ? "启用" : "禁用");
    }
    
    @Override
    public boolean isEnabled() {
        return enabled.get();
    }
    
    // ==================== 私有辅助方法 ====================
    
    /**
     * 评估告警级别
     * 
     * @param hitRate 命中率
     * @return 告警级别，如果命中率健康则返回 null
     */
    private CacheAlert.AlertLevel evaluateAlertLevel(double hitRate) {
        if (hitRate < thresholdConfig.getWarningThreshold()) {
            return CacheAlert.AlertLevel.CRITICAL;
        } else if (hitRate < thresholdConfig.getHealthyThreshold()) {
            return CacheAlert.AlertLevel.WARNING;
        }
        return null;
    }
    
    /**
     * 检查是否在冷却期内
     */
    private boolean isInCooldown(CacheStatsService.CacheLevel cacheLevel) {
        LocalDateTime lastAlert = lastAlertTime.get(cacheLevel);
        if (lastAlert == null) {
            return false;
        }
        
        long cooldownSeconds = thresholdConfig.getAlertCooldownSeconds();
        return lastAlert.plusSeconds(cooldownSeconds).isAfter(LocalDateTime.now());
    }
    
    /**
     * 发送告警通知
     */
    private void sendNotifications(CacheAlert alert) {
        for (AlertNotifier notifier : notifiers) {
            try {
                if (notifier.isAvailable()) {
                    notifier.notify(alert);
                }
            } catch (Exception e) {
                log.error("发送告警通知失败 [{}]: {}", notifier.getName(), e.getMessage(), e);
            }
        }
        alert.setNotified(true);
    }
    
    /**
     * 添加告警到历史记录
     */
    private void addToHistory(CacheAlert alert) {
        synchronized (alertHistory) {
            alertHistory.add(alert);
            // 限制历史记录大小
            while (alertHistory.size() > MAX_ALERT_HISTORY) {
                alertHistory.remove(0);
            }
        }
    }
    
    /**
     * 生成告警ID
     */
    private String generateAlertId() {
        return "ALERT-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
