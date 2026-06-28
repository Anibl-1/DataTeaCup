package com.dataplatform.collaboration.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.dataplatform.common.Result;
import com.dataplatform.infra.websocket.ChatWebSocketHandler;
import com.dataplatform.message.entity.ChatConversation;
import com.dataplatform.message.entity.ChatMessage;
import com.dataplatform.message.entity.ChatUserStatus;
import com.dataplatform.message.mapper.ChatConversationMemberMapper;
import com.dataplatform.message.entity.ChatConversationMember;
import com.dataplatform.message.service.ChatMessageService;
import com.dataplatform.message.service.ConversationService;
import com.dataplatform.message.service.OnlineStatusManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 聊天 REST 控制器
 * 暴露会话 CRUD、消息查询/发送、在线状态查询等 API
 */
@RestController
@RequestMapping("/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private ChatMessageService chatMessageService;

    @Autowired
    private OnlineStatusManager onlineStatusManager;

    @Autowired
    private ChatConversationMemberMapper memberMapper;

    @Autowired(required = false)
    private ChatWebSocketHandler chatWebSocketHandler;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        // 将在线状态管理器桥接到 WebSocket 连接回调
        if (chatWebSocketHandler != null && onlineStatusManager != null) {
            chatWebSocketHandler.setConnectionListener(new ChatWebSocketHandler.ConnectionListener() {
                @Override
                public void onUserOnline(Long userId) {
                    onlineStatusManager.userOnline(userId);
                }
                @Override
                public void onUserOffline(Long userId) {
                    onlineStatusManager.userOffline(userId);
                }
            });
            log.info("[ChatController] WebSocket 在线状态回调已注册");
        }
    }

    // ==================== 会话 API ====================

    /**
     * 获取当前用户的会话列表
     */
    @GetMapping("/conversations")
    public Result<List<ChatConversation>> getConversations() {
        Long userId = StpUtil.getLoginIdAsLong();
        List<ChatConversation> conversations = conversationService.getUserConversations(userId);
        return Result.success(conversations);
    }

    /**
     * 创建私聊会话
     */
    @PostMapping("/conversations/private")
    public Result<ChatConversation> createPrivateConversation(@RequestBody Map<String, Long> body) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        Long targetUserId = body.get("targetUserId");
        ChatConversation conversation = conversationService.createPrivateConversation(currentUserId, targetUserId);
        return Result.success(conversation);
    }

    /**
     * 创建群组会话
     */
    @SuppressWarnings("unchecked")
    @PostMapping("/conversations/group")
    public Result<ChatConversation> createGroupConversation(@RequestBody Map<String, Object> body) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        String name = (String) body.get("name");
        List<Number> rawIds = (List<Number>) body.get("memberIds");
        List<Long> memberIds = rawIds.stream()
                .map(Number::longValue)
                .collect(Collectors.toList());
        // 确保创建者自身也在群组成员中
        if (!memberIds.contains(currentUserId)) {
            memberIds.add(0, currentUserId);
        }
        ChatConversation conversation = conversationService.createGroupConversation(name, memberIds);
        return Result.success(conversation);
    }

    // ==================== 消息 API ====================

    /**
     * 获取会话消息（游标分页）
     */
    @GetMapping("/conversations/{conversationId}/messages")
    public Result<List<ChatMessage>> getMessages(
            @PathVariable Long conversationId,
            @RequestParam(required = false) Long cursor) {
        List<ChatMessage> messages = chatMessageService.getMessages(conversationId, cursor);
        return Result.success(messages);
    }

    /**
     * 发送消息
     */
    @PostMapping("/messages")
    public Result<ChatMessage> sendMessage(@RequestBody Map<String, Object> body) {
        Long userId = StpUtil.getLoginIdAsLong();
        Long conversationId = ((Number) body.get("conversationId")).longValue();
        String contentType = (String) body.get("contentType");
        String content = (String) body.get("content");
        String fileUrl = (String) body.get("fileUrl");
        String fileName = (String) body.get("fileName");
        Long fileSize = body.get("fileSize") != null ? ((Number) body.get("fileSize")).longValue() : null;

        ChatMessage message = chatMessageService.sendMessage(
                userId, conversationId, contentType, content, fileUrl, fileName, fileSize);

        // WebSocket 推送给会话成员
        pushMessageToMembers(conversationId, message);

        return Result.success(message);
    }

    // ==================== 已读状态 API ====================

    /**
     * 标记会话消息已读
     */
    @PutMapping("/conversations/{conversationId}/read")
    public Result<Void> markAsRead(@PathVariable Long conversationId) {
        Long userId = StpUtil.getLoginIdAsLong();
        conversationService.markAsRead(conversationId, userId);
        return Result.success(null);
    }

    // ==================== 在线状态 API ====================

    /**
     * 查询用户在线状态（userIds 可选，不传则返回所有在线用户）
     */
    @GetMapping("/online-status")
    public Result<List<ChatUserStatus>> getOnlineStatus(
            @RequestParam(required = false) String userIds) {
        if (userIds == null || userIds.trim().isEmpty()) {
            List<ChatUserStatus> allOnline = onlineStatusManager.getAllOnlineUsers();
            return Result.success(allOnline);
        }
        List<Long> idList = Arrays.stream(userIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toList());
        List<ChatUserStatus> statuses = onlineStatusManager.getOnlineStatus(idList);
        return Result.success(statuses);
    }

    // ==================== 内部方法 ====================

    /**
     * 通过 WebSocket 推送消息给会话所有成员
     */
    private void pushMessageToMembers(Long conversationId, ChatMessage message) {
        if (chatWebSocketHandler == null) {
            return;
        }
        try {
            List<ChatConversationMember> members = memberMapper.selectByConversationId(conversationId);
            List<Long> memberUserIds = members.stream()
                    .map(ChatConversationMember::getUserId)
                    .collect(Collectors.toList());
            // 前端 WsMessage 格式: { type, payload, timestamp }
            Map<String, Object> wsEnvelope = new java.util.HashMap<>();
            wsEnvelope.put("type", "chat");
            wsEnvelope.put("payload", message);
            wsEnvelope.put("timestamp", System.currentTimeMillis());
            String envelopeJson = objectMapper.writeValueAsString(wsEnvelope);
            chatWebSocketHandler.sendToUsers(memberUserIds, envelopeJson);
        } catch (Exception e) {
            log.error("[ChatController] WebSocket 推送失败: conversationId={}, error={}", conversationId, e.getMessage());
        }
    }
}
