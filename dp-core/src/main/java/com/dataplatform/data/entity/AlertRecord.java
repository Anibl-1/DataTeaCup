package com.dataplatform.data.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 告警记录实体
 * 对应表 sys_alert_record
 */
@Data
public class AlertRecord {
    private Long id;
    /** 规则ID */
    private Long ruleId;
    /** 规则名称 */
    private String ruleName;
    /** 指标类型 */
    private String metricType;
    /** 指标名称 */
    private String metricName;
    /** 指标值 */
    private BigDecimal metricValue;
    /** 阈值 */
    private BigDecimal thresholdValue;
    /** 告警级别 */
    private String alertLevel;
    /** 告警消息 */
    private String alertMessage;
    /** 告警时间 */
    private Date alertTime;
    /** 是否已通知 */
    private Integer isNotified;
    /** 通知时间 */
    private Date notificationTime;
    /** 是否已解决 */
    private Integer isResolved;
    /** 解决时间 */
    private Date resolveTime;
    /** 解决人 */
    private Long resolveBy;
    /** 解决备注 */
    private String resolveNote;
    private Date createTime;
}
