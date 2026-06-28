package com.dataplatform.data.service.ticket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 工单通知服务
 * 状态变更时通知相关人员
 * 需求: 32.6
 */
@Service
public class TicketNotificationService {

    private static final Logger log = LoggerFactory.getLogger(TicketNotificationService.class);

    /**
     * 发送工单状态变更通知
     */
    public void notifyStatusChange(Ticket ticket, String oldStatus, String newStatus) {
        log.info("工单状态变更通知: {} {} -> {}, 提交人: {}, 处理人: {}",
                ticket.getTicketNo(), oldStatus, newStatus,
                ticket.getSubmitterName(), ticket.getAssigneeName());

        // 通知提交人
        if (ticket.getSubmitterId() != null) {
            sendNotification(ticket.getSubmitterId(),
                    String.format("您的工单 %s 状态已更新为: %s", ticket.getTicketNo(), translateStatus(newStatus)));
        }

        // 通知处理人
        if (ticket.getAssigneeId() != null && !"pending".equals(newStatus)) {
            sendNotification(ticket.getAssigneeId(),
                    String.format("工单 %s 状态已更新为: %s", ticket.getTicketNo(), translateStatus(newStatus)));
        }
    }

    /**
     * 发送工单分配通知
     */
    public void notifyAssignment(Ticket ticket) {
        if (ticket.getAssigneeId() != null) {
            sendNotification(ticket.getAssigneeId(),
                    String.format("您有新的工单需要处理: %s - %s", ticket.getTicketNo(), ticket.getTitle()));
        }
    }

    /**
     * 发送新评论通知
     */
    public void notifyNewComment(Ticket ticket, TicketComment comment) {
        // 通知工单相关人员（排除评论者自己）
        if (ticket.getSubmitterId() != null && !ticket.getSubmitterId().equals(comment.getUserId())) {
            sendNotification(ticket.getSubmitterId(),
                    String.format("工单 %s 有新的回复", ticket.getTicketNo()));
        }
        if (ticket.getAssigneeId() != null && !ticket.getAssigneeId().equals(comment.getUserId())) {
            sendNotification(ticket.getAssigneeId(),
                    String.format("工单 %s 有新的回复", ticket.getTicketNo()));
        }
    }

    private void sendNotification(String userId, String message) {
        // 实际场景中集成消息通知系统（邮件/站内信/WebSocket）
        log.info("发送通知给用户 {}: {}", userId, message);
    }

    private String translateStatus(String status) {
        switch (status) {
            case "pending": return "待处理";
            case "processing": return "处理中";
            case "resolved": return "已解决";
            case "closed": return "已关闭";
            default: return status;
        }
    }
}
