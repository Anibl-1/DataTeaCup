package com.dataplatform.data.service.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 日志告警通知器
 * 
 * 将告警信息输出到日志系统
 */
@Slf4j
@Component
public class LogAlertNotifier implements AlertNotifier {
    
    private static final String NOTIFIER_NAME = "LogNotifier";
    
    @Override
    public void notify(CacheAlert alert) {
        if (alert == null) {
            return;
        }
        
        String logMessage = formatAlertMessage(alert);
        
        switch (alert.getAlertLevel()) {
            case CRITICAL:
                log.error("[缓存告警] {}", logMessage);
                break;
            case WARNING:
                log.warn("[缓存告警] {}", logMessage);
                break;
            default:
                log.info("[缓存告警] {}", logMessage);
        }
    }
    
    @Override
    public String getName() {
        return NOTIFIER_NAME;
    }
    
    @Override
    public boolean isAvailable() {
        return true;
    }
    
    /**
     * 格式化告警消息
     */
    private String formatAlertMessage(CacheAlert alert) {
        return String.format(
                "告警ID: %s | 级别: %s | 缓存: %s | 命中率: %.2f%% | 阈值: %.2f%% | 消息: %s",
                alert.getAlertId(),
                alert.getAlertLevel().getLabel(),
                alert.getCacheLevel().getDescription(),
                alert.getCurrentHitRate() * 100,
                alert.getThreshold() * 100,
                alert.getMessage()
        );
    }
}
