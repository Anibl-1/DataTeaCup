package com.dataplatform.data.service.health;

import lombok.Data;
import java.util.HashMap;
import java.util.Map;

/**
 * 组件健康状态
 */
@Data
public class ComponentHealth {
    
    /**
     * 组件名称
     */
    private String name;
    
    /**
     * 状态: UP, DOWN
     */
    private String status;
    
    /**
     * 状态消息
     */
    private String message;
    
    /**
     * 详细信息
     */
    private Map<String, Object> details = new HashMap<>();
    
    /**
     * 响应时间（毫秒）
     */
    private long responseTime;
    
    /**
     * 是否为关键组件
     */
    private boolean critical;
    
    public static final String STATUS_UP = "UP";
    public static final String STATUS_DOWN = "DOWN";
    
    public static ComponentHealth up(String name, String message) {
        ComponentHealth health = new ComponentHealth();
        health.setName(name);
        health.setStatus(STATUS_UP);
        health.setMessage(message);
        return health;
    }
    
    public static ComponentHealth down(String name, String message) {
        ComponentHealth health = new ComponentHealth();
        health.setName(name);
        health.setStatus(STATUS_DOWN);
        health.setMessage(message);
        return health;
    }
    
    public ComponentHealth withDetail(String key, Object value) {
        this.details.put(key, value);
        return this;
    }
    
    public boolean isUp() {
        return STATUS_UP.equals(status);
    }
}
