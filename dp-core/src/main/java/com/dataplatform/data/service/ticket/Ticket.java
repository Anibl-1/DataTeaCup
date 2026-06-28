package com.dataplatform.data.service.ticket;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工单实体类
 * 需求: 32.1, 32.2, 32.3, 32.4
 */
public class Ticket {
    private Long id;
    private String ticketNo;          // 工单编号
    private String title;             // 标题
    private String description;       // 描述
    private String category;          // 分类: bug, feature_request, consultation
    private String priority;          // 优先级: low, medium, high, urgent
    private String status;            // 状态: pending, processing, resolved, closed
    private String submitterId;       // 提交人ID
    private String submitterName;     // 提交人姓名
    private String assigneeId;        // 处理人ID
    private String assigneeName;      // 处理人姓名
    private List<String> attachments; // 附件路径列表
    private String resolution;        // 解决方案
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime resolvedTime;

    public Ticket() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTicketNo() { return ticketNo; }
    public void setTicketNo(String ticketNo) { this.ticketNo = ticketNo; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSubmitterId() { return submitterId; }
    public void setSubmitterId(String submitterId) { this.submitterId = submitterId; }
    public String getSubmitterName() { return submitterName; }
    public void setSubmitterName(String submitterName) { this.submitterName = submitterName; }
    public String getAssigneeId() { return assigneeId; }
    public void setAssigneeId(String assigneeId) { this.assigneeId = assigneeId; }
    public String getAssigneeName() { return assigneeName; }
    public void setAssigneeName(String assigneeName) { this.assigneeName = assigneeName; }
    public List<String> getAttachments() { return attachments; }
    public void setAttachments(List<String> attachments) { this.attachments = attachments; }
    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public LocalDateTime getResolvedTime() { return resolvedTime; }
    public void setResolvedTime(LocalDateTime resolvedTime) { this.resolvedTime = resolvedTime; }
}
