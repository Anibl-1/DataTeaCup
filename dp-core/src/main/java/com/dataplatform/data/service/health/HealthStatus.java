package com.dataplatform.data.service.health;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 系统健康状态
 */
@Data
public class HealthStatus {
    
    /**
     * 整体状态: UP, DOWN, DEGRADED
     */
    private String status;
    
    /**
     * 各组件健康状态
     */
    private Map<String, ComponentHealth> components;
    
    /**
     * 检查时间
     */
    private LocalDateTime checkTime;
    
    /**
     * 健康组件数
     */
    private int healthyCount;
    
    /**
     * 不健康组件数
     */
    private int unhealthyCount;
    
    /**
     * 总检查耗时（毫秒）
     */
    private long totalCheckTime;
    
    public static final String STATUS_UP = "UP";
    public static final String STATUS_DOWN = "DOWN";
    public static final String STATUS_DEGRADED = "DEGRADED";
}
