package com.dataplatform.data.service.cache;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 缓存健康状态
 * 
 * 基于命中率和内存使用情况评估缓存系统的健康状态
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheHealthStatus {
    
    /**
     * 整体健康状态
     */
    private HealthLevel overallStatus;
    
    /**
     * L1 缓存健康状态
     */
    private HealthLevel l1Status;
    
    /**
     * L2 缓存健康状态
     */
    private HealthLevel l2Status;
    
    /**
     * L1 命中率
     */
    private double l1HitRate;
    
    /**
     * L2 命中率
     */
    private double l2HitRate;
    
    /**
     * 总体命中率
     */
    private double overallHitRate;
    
    /**
     * L1 内存使用率
     */
    private double l1MemoryUsageRate;
    
    /**
     * L2 内存使用率
     */
    private double l2MemoryUsageRate;
    
    /**
     * 健康问题列表
     */
    @Builder.Default
    private List<String> issues = new ArrayList<>();
    
    /**
     * 优化建议列表
     */
    @Builder.Default
    private List<String> suggestions = new ArrayList<>();
    
    /**
     * 健康级别枚举
     */
    public enum HealthLevel {
        /**
         * 健康：命中率 >= 70%
         */
        HEALTHY("健康", "green"),
        
        /**
         * 警告：命中率 50% - 70%
         */
        WARNING("警告", "yellow"),
        
        /**
         * 危险：命中率 < 50%
         */
        CRITICAL("危险", "red"),
        
        /**
         * 不可用：服务不可用
         */
        UNAVAILABLE("不可用", "gray");
        
        private final String label;
        private final String color;
        
        HealthLevel(String label, String color) {
            this.label = label;
            this.color = color;
        }
        
        public String getLabel() {
            return label;
        }
        
        public String getColor() {
            return color;
        }
    }
    
    /**
     * 添加健康问题
     */
    public void addIssue(String issue) {
        if (issues == null) {
            issues = new ArrayList<>();
        }
        issues.add(issue);
    }
    
    /**
     * 添加优化建议
     */
    public void addSuggestion(String suggestion) {
        if (suggestions == null) {
            suggestions = new ArrayList<>();
        }
        suggestions.add(suggestion);
    }
}
