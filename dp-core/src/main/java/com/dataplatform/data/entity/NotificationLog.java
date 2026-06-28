package com.dataplatform.data.entity;

import lombok.Data;
import java.util.Date;

/**
 * 通知投递日志实体
 * 对应表 sys_notification_log
 */
@Data
public class NotificationLog {
    private Long id;
    /** 通知类型: alert/export/task/report/system */
    private String notificationType;
    /** 发送渠道: site/email/sms/wecom/dingtalk/websocket */
    private String channel;
    /** 接收人(邮箱/手机号/用户ID/webhook) */
    private String recipient;
    /** 标题 */
    private String subject;
    /** 内容摘要 */
    private String content;
    /** 状态: pending/success/failed */
    private String status;
    /** 错误信息 */
    private String errorMessage;
    /** 重试次数 */
    private Integer retryCount;
    /** 发送时间 */
    private Date sendTime;
    /** 创建时间 */
    private Date createTime;
}
