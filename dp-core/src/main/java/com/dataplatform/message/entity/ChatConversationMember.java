package com.dataplatform.message.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 会话成员实体
 */
@Data
public class ChatConversationMember {

    /** 主键 */
    private Long id;

    /** 会话 ID */
    private Long conversationId;

    /** 用户 ID */
    private Long userId;

    /** 未读消息数 */
    private Integer unreadCount;

    /** 加入时间 */
    private LocalDateTime joinTime;

    // ---- 查询时由关联查询填充，不对应数据库列 ----

    /** 用户昵称（关联查询填充） */
    private String nickname;

    /** 用户名（关联查询填充） */
    private String username;
}
