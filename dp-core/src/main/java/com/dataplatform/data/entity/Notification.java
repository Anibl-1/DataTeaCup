package com.dataplatform.data.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 系统通知实体
 */
@Data
public class Notification {

    private Long id;

    /** 标题 */
    private String title;

    /** 内容 */
    private String content;

    /** 通知类型 */
    private String notificationType;

    /** 优先级: normal/high/urgent */
    private String priority;

    /** 目标用户ID */
    private Long targetUserId;

    /** 发送人ID */
    private Long senderId;

    /** 发送人名称 */
    private String senderName;

    /** 目标部门ID（按部门发送时） */
    private Long deptId;

    /** 是否已读 */
    private Boolean isRead;

    /** 阅读时间 */
    private LocalDateTime readTime;

    /** 关联类型 */
    private String relatedType;

    /** 关联ID */
    private Long relatedId;

    /** 附件JSON */
    private String attachments;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;
}
