package com.dataplatform.data.service.cache;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 缓存告警信息
 * 
 * 表示一个缓存告警事件，包含告警级别、缓存级别、命中率等信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheAlert {
    
    /**
     * 告警ID
     */
    private String alertId;
    
    /**
     * 告警时间
     */
    private LocalDateTime alertTime;
    
    /**
     * 缓存级别（L1 或 L2）
     */
    private CacheStatsService.CacheLevel cacheLevel;
    
    /**
     * 告警级别
     */
    private AlertLevel alertLevel;
    
    /**
     * 当前命中率
     */
    private double currentHitRate;
    
    /**
     * 告警阈值
     */
    private double threshold;
    
    /**
     * 告警消息
     */
    private String message;
    
    /**
     * 是否已通知
     */
    private boolean notified;
    
    /**
     * 告警级别枚举
     */
    public enum AlertLevel {
        /**
         * 警告级别：命中率 50% - 70%
         */
        WARNING("警告", "yellow", 1),
        
        /**
         * 危险级别：命中率 < 50%
         */
        CRITICAL("危险", "red", 2);
        
        private final String label;
        private final String color;
        private final int severity;
        
        AlertLevel(String label, String color, int severity) {
            this.label = label;
            this.color = color;
            this.severity = severity;
        }
        
        public String getLabel() {
            return label;
        }
        
        public String getColor() {
            return color;
        }
        
        public int getSeverity() {
            return severity;
        }
    }
    
    /**
     * 生成告警消息
     */
    public static String generateMessage(CacheStatsService.CacheLevel cacheLevel, 
                                          AlertLevel alertLevel, 
                                          double hitRate, 
                                          double threshold) {
        return String.format("%s 缓存命中率告警 [%s]: 当前命中率 %.1f%% 低于阈值 %.1f%%",
                cacheLevel.getDescription(),
                alertLevel.getLabel(),
                hitRate * 100,
                threshold * 100);
    }
}
