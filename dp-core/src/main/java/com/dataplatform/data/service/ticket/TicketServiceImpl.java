package com.dataplatform.data.service.ticket;

import com.dataplatform.data.mapper.TicketMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 工单服务实现 - 数据库持久化版本
 */
@Service
public class TicketServiceImpl implements TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketServiceImpl.class);

    private final TicketMapper ticketMapper;

    public TicketServiceImpl(TicketMapper ticketMapper) {
        this.ticketMapper = ticketMapper;
    }

    @Override
    public Ticket submitTicket(Ticket ticket) {
        // 生成工单编号
        Long maxId = ticketMapper.selectMaxId();
        long nextId = (maxId != null ? maxId : 0) + 1;
        ticket.setTicketNo("TK-" + String.format("%06d", nextId));
        ticket.setStatus("pending");
        ticketMapper.insertTicket(ticket);
        log.info("工单已提交: {}", ticket.getTicketNo());
        return ticket;
    }

    @Override
    public Ticket getTicket(Long id) {
        return ticketMapper.selectById(id);
    }

    @Override
    public List<Ticket> getTickets(String status, String category, String priority, int page, int size) {
        int offset = Math.max(0, page - 1) * size;
        return ticketMapper.selectList(status, category, priority, offset, size);
    }

    @Override
    public List<Ticket> getMyTickets(String userId) {
        return ticketMapper.selectBySubmitterId(userId);
    }

    @Override
    public Ticket updateStatus(Long id, String status, String resolution) {
        ticketMapper.updateStatus(id, status, resolution);
        log.info("工单状态更新: {} -> {}", id, status);
        return ticketMapper.selectById(id);
    }

    @Override
    public Ticket assignTicket(Long id, String assigneeId, String assigneeName) {
        ticketMapper.updateAssignee(id, assigneeId, assigneeName, "processing");
        log.info("工单已分配: {} -> {}", id, assigneeName);
        return ticketMapper.selectById(id);
    }

    @Override
    public TicketComment addComment(Long ticketId, TicketComment comment) {
        comment.setTicketId(ticketId);
        ticketMapper.insertComment(comment);
        return comment;
    }

    @Override
    public List<TicketComment> getComments(Long ticketId) {
        return ticketMapper.selectCommentsByTicketId(ticketId);
    }

    @Override
    public void deleteTicket(Long id) {
        ticketMapper.deleteCommentsByTicketId(id);
        ticketMapper.deleteTicket(id);
        log.info("工单已撤销: {}", id);
    }

    @Override
    public Map<String, Object> getTicketStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (String s : List.of("pending", "processing", "resolved", "closed")) {
            byStatus.put(s, ticketMapper.countByStatus(s));
        }
        stats.put("byStatus", byStatus);
        return stats;
    }
}
