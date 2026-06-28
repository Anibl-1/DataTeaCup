package com.dataplatform.data.service.ticket;

import java.util.List;
import java.util.Map;

/**
 * 工单服务接口
 * 需求: 32.1, 32.2, 32.3, 32.4, 32.5
 */
public interface TicketService {

    /** 提交工单 */
    Ticket submitTicket(Ticket ticket);

    /** 获取工单详情 */
    Ticket getTicket(Long id);

    /** 获取工单列表 */
    List<Ticket> getTickets(String status, String category, String priority, int page, int size);

    /** 获取用户的工单 */
    List<Ticket> getMyTickets(String userId);

    /** 更新工单状态 */
    Ticket updateStatus(Long id, String status, String resolution);

    /** 分配工单 */
    Ticket assignTicket(Long id, String assigneeId, String assigneeName);

    /** 添加评论 */
    TicketComment addComment(Long ticketId, TicketComment comment);

    /** 获取工单评论 */
    List<TicketComment> getComments(Long ticketId);

    /** 删除工单 */
    void deleteTicket(Long id);

    /** 获取工单统计 */
    Map<String, Object> getTicketStats();
}
