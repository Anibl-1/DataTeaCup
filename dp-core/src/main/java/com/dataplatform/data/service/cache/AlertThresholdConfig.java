package com.dataplatform.data.service.cache;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 告警阈值配置
 * 
 * 定义缓存命中率告警的阈值配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertThresholdConfig {
    
    /**
     * 健康阈值（默认 70%）
     * 命中率 >= 此值时为健康状态
     */
    @Builder.Default
    private double healthyThreshold = 0.70;
    
    /**
     * 警告阈值（默认 50%）
     * 命中率 >= 此值且 < healthyThreshold 时为警告状态
     */
    @Builder.Default
    private double warningThreshold = 0.50;
    
    /**
     * 是否启用 L1 缓存告警
     */
    @Builder.Default
    private boolean l1AlertEnabled = true;
    
    /**
     * 是否启用 L2 缓存告警
     */
    @Builder.Default
    private boolean l2AlertEnabled = true;
    
    /**
     * 最小请求数阈值
     * 只有当请求数超过此值时才触发告警，避免冷启动时的误报
     */
    @Builder.Default
    private long minRequestCount = 100;
    
    /**
     * 告警冷却时间（秒）
     * 同一缓存级别的告警在此时间内不会重复触发
     */
    @Builder.Default
    private long alertCooldownSeconds = 300;
    
    /**
     * 创建默认配置
     */
    public static AlertThresholdConfig defaultConfig() {
        return AlertThresholdConfig.builder().build();
    }
    
    /**
     * 验证配置有效性
     * 
     * @throws IllegalArgumentException 如果配置无效
     */
    public void validate() {
        if (healthyThreshold <= 0 || healthyThreshold > 1) {
            throw new IllegalArgumentException("健康阈值必须在 (0, 1] 范围内");
        }
        if (warningThreshold <= 0 || warningThreshold > 1) {
            throw new IllegalArgumentException("警告阈值必须在 (0, 1] 范围内");
        }
        if (warningThreshold >= healthyThreshold) {
            throw new IllegalArgumentException("警告阈值必须小于健康阈值");
        }
        if (minRequestCount < 0) {
            throw new IllegalArgumentException("最小请求数不能为负数");
        }
        if (alertCooldownSeconds < 0) {
            throw new IllegalArgumentException("告警冷却时间不能为负数");
        }
    }
}
