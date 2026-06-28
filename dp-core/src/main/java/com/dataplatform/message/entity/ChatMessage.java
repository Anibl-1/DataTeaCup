package com.dataplatform.message.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 聊天消息实体
 */
@Data
public class ChatMessage {

    /** 消息 ID */
    private Long id;

    /** 会话 ID */
    private Long conversationId;

    /** 发送者 ID */
    private Long senderId;

    /** 消息类型: text-文本 image-图片 file-文件 */
    private String contentType;

    /** 消息内容 */
    private String content;

    /** 文件 URL */
    private String fileUrl;

    /** 文件名 */
    private String fileName;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 发送时间 */
    private LocalDateTime sendTime;
}
