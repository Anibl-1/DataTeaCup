package com.dataplatform.data.mapper;

import com.dataplatform.data.service.ticket.Ticket;
import com.dataplatform.data.service.ticket.TicketComment;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TicketMapper {

    void insertTicket(Ticket ticket);

    Ticket selectById(Long id);

    List<Ticket> selectList(@Param("status") String status,
                            @Param("category") String category,
                            @Param("priority") String priority,
                            @Param("offset") int offset,
                            @Param("size") int size);

    long count(@Param("status") String status,
               @Param("category") String category,
               @Param("priority") String priority);

    List<Ticket> selectBySubmitterId(@Param("submitterId") String submitterId);

    void updateStatus(@Param("id") Long id,
                      @Param("status") String status,
                      @Param("resolution") String resolution);

    void updateAssignee(@Param("id") Long id,
                        @Param("assigneeId") String assigneeId,
                        @Param("assigneeName") String assigneeName,
                        @Param("status") String status);

    // 评论
    void insertComment(TicketComment comment);

    List<TicketComment> selectCommentsByTicketId(@Param("ticketId") Long ticketId);

    // 统计
    long countByStatus(@Param("status") String status);

    // 获取下一个工单编号序列
    Long selectMaxId();

    // 删除工单
    void deleteTicket(@Param("id") Long id);

    // 删除工单的所有评论
    void deleteCommentsByTicketId(@Param("ticketId") Long ticketId);
}
