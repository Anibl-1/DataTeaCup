package com.dataplatform.data.entity;

import lombok.Data;
import java.util.Date;

/**
 * 通知模板实体
 * 对应表 sys_notification_template
 */
@Data
public class NotificationTemplate {
    private Long id;
    /** 模板编码(唯一) */
    private String templateCode;
    /** 模板名称 */
    private String templateName;
    /** 适用渠道: email/sms/wecom/dingtalk/all */
    private String channel;
    /** 通知类型: alert/export/task/report/system */
    private String notificationType;
    /** 标题模板 */
    private String subject;
    /** 内容模板(支持 ${variable} 占位符) */
    private String content;
    /** 可用变量列表(JSON数组) */
    private String variables;
    /** 是否启用 */
    private Integer isEnabled;
    private Date createTime;
    private Date updateTime;
}
