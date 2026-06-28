package com.dataplatform.data.service.ticket;

import java.time.LocalDateTime;

/**
 * 工单评论实体类
 * 需求: 32.5
 */
public class TicketComment {
    private Long id;
    private Long ticketId;
    private String userId;
    private String userName;
    private String content;
    private boolean internal;  // 是否内部备注
    private LocalDateTime createTime;

    public TicketComment() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public boolean isInternal() { return internal; }
    public void setInternal(boolean internal) { this.internal = internal; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
