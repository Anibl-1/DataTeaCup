package com.dataplatform.data.entity;

import lombok.Data;
import java.util.Date;

/**
 * 健康检查记录实体
 * 对应表 sys_health_check
 */
@Data
public class HealthCheck {
    private Long id;
    /** 检查时间 */
    private Date checkTime;
    /** 检查类型: database/redis/mq/api */
    private String checkType;
    /** 检查名称 */
    private String checkName;
    /** 状态: healthy/unhealthy/degraded */
    private String status;
    /** 响应时间(ms) */
    private Long responseTime;
    /** 错误信息 */
    private String errorMessage;
    /** 详细信息JSON */
    private String details;
    private Date createTime;
}
