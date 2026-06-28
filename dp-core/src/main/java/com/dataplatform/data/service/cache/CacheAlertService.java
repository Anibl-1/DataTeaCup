package com.dataplatform.data.service.cache;

import java.util.List;

/**
 * 缓存告警服务接口
 * 
 * 提供缓存命中率监控和告警功能：
 * - 监控 L1 和 L2 缓存命中率
 * - 当命中率低于阈值时触发告警
 * - 支持多种告警通知渠道
 * - 支持告警阈值配置
 * 
 * 需求引用：
 * - 需求 10.4: 当缓存命中率低于70%时，触发告警通知
 * 
 * @see CacheStatsService 缓存统计服务
 * @see CacheHealthStatus 缓存健康状态
 */
public interface CacheAlertService {
    
    /**
     * 检查缓存命中率并触发告警
     * 
     * 检查 L1 和 L2 缓存的命中率，当低于配置的阈值时触发告警
     * 
     * @return 触发的告警列表
     */
    List<CacheAlert> checkAndAlert();
    
    /**
     * 检查指定缓存级别的命中率
     * 
     * @param cacheLevel 缓存级别
     * @return 如果命中率低于阈值，返回告警信息；否则返回 null
     */
    CacheAlert checkHitRate(CacheStatsService.CacheLevel cacheLevel);
    
    /**
     * 获取当前告警阈值配置
     * 
     * @return 告警阈值配置
     */
    AlertThresholdConfig getThresholdConfig();
    
    /**
     * 更新告警阈值配置
     * 
     * @param config 新的阈值配置
     */
    void updateThresholdConfig(AlertThresholdConfig config);
    
    /**
     * 注册告警通知器
     * 
     * @param notifier 告警通知器
     */
    void registerNotifier(AlertNotifier notifier);
    
    /**
     * 移除告警通知器
     * 
     * @param notifier 告警通知器
     */
    void removeNotifier(AlertNotifier notifier);
    
    /**
     * 获取最近的告警历史
     * 
     * @param limit 返回的最大告警数量
     * @return 告警历史列表
     */
    List<CacheAlert> getRecentAlerts(int limit);
    
    /**
     * 清除告警历史
     */
    void clearAlertHistory();
    
    /**
     * 启用或禁用告警
     * 
     * @param enabled 是否启用
     */
    void setEnabled(boolean enabled);
    
    /**
     * 检查告警是否启用
     * 
     * @return 是否启用
     */
    boolean isEnabled();
}
