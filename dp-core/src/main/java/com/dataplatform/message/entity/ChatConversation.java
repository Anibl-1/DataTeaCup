package com.dataplatform.message.entity;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 会话实体
 */
@Data
public class ChatConversation {

    /** 会话 ID */
    private Long id;

    /** 会话类型: private-私聊 group-群组 */
    private String type;

    /** 会话名称（群组必填） */
    private String name;

    /** 群组头像 URL */
    private String avatar;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后消息时间 */
    private LocalDateTime lastMessageTime;

    // ---- 以下为查询时由 Service 填充，不对应数据库列 ----

    /** 最后一条消息内容（查询时填充） */
    private String lastMessage;

    /** 当前用户未读消息数（查询时填充） */
    private Integer unreadCount;

    /** 会话成员列表（查询时填充） */
    private List<ChatConversationMember> members;
}
