package com.dataplatform.message.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户在线状态实体
 */
@Data
public class ChatUserStatus {

    /** 用户 ID（主键） */
    private Long userId;

    /** 状态: online-在线 offline-离线 */
    private String status;

    /** 最后活跃时间 */
    private LocalDateTime lastActiveTime;
}
