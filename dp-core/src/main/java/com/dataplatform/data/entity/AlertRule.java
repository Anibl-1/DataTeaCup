package com.dataplatform.data.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 告警规则实体
 * 对应表 sys_alert_rule
 */
@Data
public class AlertRule {
    private Long id;
    /** 规则名称 */
    @NotBlank(message = "规则名称不能为空")
    @Size(max = 100, message = "规则名称不能超过100字符")
    private String ruleName;
    /** 规则编码 */
    private String ruleCode;
    /** 指标类型: cpu/memory/disk/jvm/db/task */
    @NotBlank(message = "指标类型不能为空")
    private String metricType;
    /** 指标名称 */
    @NotBlank(message = "指标名称不能为空")
    private String metricName;
    /** 阈值类型: gt/lt/eq/gte/lte */
    @NotBlank(message = "阈值类型不能为空")
    private String thresholdType;
    /** 阈值 */
    @NotNull(message = "阈值不能为空")
    private BigDecimal thresholdValue;
    /** 持续时间(秒) */
    private Integer durationSeconds;
    /** 告警级别: info/warning/error/critical */
    @NotBlank(message = "告警级别不能为空")
    private String alertLevel;
    /** 告警消息模板 */
    private String alertMessage;
    /** 通知渠道: email,sms,dingtalk */
    private String notificationChannels;
    /** 通知用户ID列表 */
    private String notificationUsers;
    /** 是否启用 */
    private Integer isEnabled;
    /** 创建人 */
    private Long createBy;
    private Date createTime;
    private Date updateTime;
}
