package com.dataplatform.data.service.comment;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 评论
 * 需求: 23.1, 23.2, 23.3
 */
@Data
public class Comment {
    private String id;
    private String resourceType; // dashboard, report, chart
    private String resourceId;
    private String parentId; // 父评论ID（支持嵌套回复）
    private String userId;
    private String content;
    private List<String> mentions = new ArrayList<>(); // @提及的用户ID
    private String mentionsJson; // DB持久化用JSON字符串
    private boolean resolved; // 是否已解决
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
