package com.dataplatform.data.service.health;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 健康检查记录
 */
@Data
public class HealthRecord {
    
    /**
     * 组件名称
     */
    private String componentName;
    
    /**
     * 状态
     */
    private String status;
    
    /**
     * 消息
     */
    private String message;
    
    /**
     * 响应时间
     */
    private long responseTime;
    
    /**
     * 检查时间
     */
    private LocalDateTime checkTime;
}
