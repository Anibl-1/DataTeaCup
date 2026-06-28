package com.dataplatform.data.service.team;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 团队空间聊天消息
 */
@Data
public class TeamSpaceMessage {
    private String id;
    private String spaceId;
    private String senderId;
    private String senderName;
    private String content;
    private LocalDateTime createdAt;
    private boolean isMine;
}
