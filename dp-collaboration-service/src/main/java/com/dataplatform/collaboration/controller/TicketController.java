package com.dataplatform.collaboration.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.dataplatform.common.annotation.RequirePermission;
import com.dataplatform.common.Result;
import com.dataplatform.system.mapper.UserMapper;
import com.dataplatform.data.service.ticket.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 工单管理控制器
 * 权限规则:
 * - 创建工单: 自动填充提交人信息
 * - 分配处理人: 只有创建人可以分配
 * - 更新状态: 只有被指派的处理人可以更新
 * - 评论: 自动填充评论人信息
 */
@Tag(name = "工单管理", description = "技术支持工单系统")
@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
@RequirePermission("ticket:read")
public class TicketController {

    private final TicketService ticketService;
    private final KnowledgeService knowledgeService;
    private final UserMapper userMapper;

    /** 获取当前用户ID */
    private String currentUserId() {
        return String.valueOf(StpUtil.getLoginId());
    }

    /** 获取当前用户姓名 */
    private String currentUserName() {
        try {
            String uid = currentUserId();
            var user = userMapper.selectById(Long.parseLong(uid));
            if (user != null) {
                if (user.getNickname() != null && !user.getNickname().isBlank()) {
                    return user.getNickname();
                }
                if (user.getUsername() != null && !user.getUsername().isBlank()) {
                    return user.getUsername();
                }
            }
            return "用户" + uid;
        } catch (Exception e) {
            return "用户" + currentUserId();
        }
    }

    @Operation(summary = "提交工单")
    @PostMapping
    public Result<Ticket> submitTicket(@RequestBody Ticket ticket) {
        // 自动填充提交人信息
        ticket.setSubmitterId(currentUserId());
        ticket.setSubmitterName(currentUserName());
        // 如果创建时指定了处理人，保留；否则为空
        return Result.success(ticketService.submitTicket(ticket));
    }

    @Operation(summary = "获取工单详情")
    @GetMapping("/{id}")
    public Result<Ticket> getTicket(@PathVariable Long id) {
        return Result.success(ticketService.getTicket(id));
    }

    @Operation(summary = "获取工单列表")
    @GetMapping
    public Result<List<Ticket>> getTickets(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String priority,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(ticketService.getTickets(status, category, priority, page, size));
    }

    @Operation(summary = "获取我的工单")
    @GetMapping("/my/{userId}")
    public Result<List<Ticket>> getMyTickets(@PathVariable String userId) {
        return Result.success(ticketService.getMyTickets(userId));
    }

    @Operation(summary = "更新工单状态 - 只有被指派的处理人可以操作")
    @PutMapping("/{id}/status")
    public Result<Ticket> updateStatus(@PathVariable Long id, @RequestBody StatusUpdateRequest request) {
        Ticket ticket = ticketService.getTicket(id);
        if (ticket == null) return Result.error("工单不存在");

        String userId = currentUserId();
        // 只有被指派的处理人可以更新状态
        if (ticket.getAssigneeId() == null || !ticket.getAssigneeId().equals(userId)) {
            return Result.error("只有被指派的处理人才能更新工单状态");
        }

        return Result.success(ticketService.updateStatus(id, request.getStatus(), request.getResolution()));
    }

    @Operation(summary = "分配工单 - 只有创建人可以分配")
    @PutMapping("/{id}/assign")
    public Result<Ticket> assignTicket(@PathVariable Long id, @RequestBody AssignRequest request) {
        Ticket ticket = ticketService.getTicket(id);
        if (ticket == null) return Result.error("工单不存在");

        String userId = currentUserId();
        // 只有创建人可以分配处理人
        if (!userId.equals(ticket.getSubmitterId())) {
            return Result.error("只有工单创建人才能分配处理人");
        }

        return Result.success(ticketService.assignTicket(id, request.getAssigneeId(), request.getAssigneeName()));
    }

    @Operation(summary = "添加评论 - 自动填充评论人信息")
    @PostMapping("/{ticketId}/comments")
    public Result<TicketComment> addComment(@PathVariable Long ticketId, @RequestBody TicketComment comment) {
        // 自动填充评论人信息
        comment.setUserId(currentUserId());
        comment.setUserName(currentUserName());
        return Result.success(ticketService.addComment(ticketId, comment));
    }

    @Operation(summary = "获取工单评论")
    @GetMapping("/{ticketId}/comments")
    public Result<List<TicketComment>> getComments(@PathVariable Long ticketId) {
        return Result.success(ticketService.getComments(ticketId));
    }

    @Operation(summary = "撤销（删除）工单 - 只有创建人可以操作")
    @DeleteMapping("/{id}")
    public Result<Void> deleteTicket(@PathVariable Long id) {
        Ticket ticket = ticketService.getTicket(id);
        if (ticket == null) return Result.error("工单不存在");

        String userId = currentUserId();
        if (!userId.equals(ticket.getSubmitterId())) {
            return Result.error("只有工单创建人才能撤销工单");
        }

        ticketService.deleteTicket(id);
        return Result.success();
    }

    @Operation(summary = "获取工单统计")
    @GetMapping("/stats")
    public Result<Map<String, Object>> getTicketStats() {
        return Result.success(ticketService.getTicketStats());
    }

    // ==================== 知识库接口 ====================

    @Operation(summary = "搜索知识库")
    @GetMapping("/knowledge")
    public Result<List<KnowledgeArticle>> searchKnowledge(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category) {
        return Result.success(knowledgeService.searchArticles(keyword, category));
    }

    @Operation(summary = "获取知识库文章")
    @GetMapping("/knowledge/{id}")
    public Result<KnowledgeArticle> getKnowledgeArticle(@PathVariable Long id) {
        return Result.success(knowledgeService.getArticle(id));
    }

    @RequirePermission("ticket:knowledge")
    @Operation(summary = "创建知识库文章")
    @PostMapping("/knowledge")
    public Result<KnowledgeArticle> createKnowledgeArticle(@RequestBody KnowledgeArticle article) {
        article.setAuthorId(currentUserId());
        article.setAuthorName(currentUserName());
        return Result.success(knowledgeService.createArticle(article));
    }

    @RequirePermission("ticket:knowledge")
    @Operation(summary = "更新知识库文章")
    @PutMapping("/knowledge/{id}")
    public Result<KnowledgeArticle> updateKnowledgeArticle(@PathVariable Long id, @RequestBody KnowledgeArticle article) {
        KnowledgeArticle updated = knowledgeService.updateArticle(id, article);
        return updated != null ? Result.success(updated) : Result.error("文章不存在");
    }

    @RequirePermission("ticket:knowledge")
    @Operation(summary = "删除知识库文章")
    @DeleteMapping("/knowledge/{id}")
    public Result<Void> deleteKnowledgeArticle(@PathVariable Long id) {
        return knowledgeService.deleteArticle(id) ? Result.success() : Result.error("文章不存在");
    }

    @Operation(summary = "获取热门文章")
    @GetMapping("/knowledge/popular")
    public Result<List<KnowledgeArticle>> getPopularArticles(@RequestParam(defaultValue = "10") int topN) {
        return Result.success(knowledgeService.getPopularArticles(topN));
    }

    @Operation(summary = "标记文章有帮助")
    @PutMapping("/knowledge/{id}/helpful")
    public Result<Void> markHelpful(@PathVariable Long id) {
        knowledgeService.markHelpful(id);
        return Result.success();
    }

    @Data
    public static class StatusUpdateRequest {
        private String status;
        private String resolution;
    }

    @Data
    public static class AssignRequest {
        private String assigneeId;
        private String assigneeName;
    }
}
