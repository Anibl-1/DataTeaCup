package com.dataplatform.infra.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天 WebSocket 消息处理器
 * <p>
 * 维护在线用户映射（userId → WebSocketSession），
 * 解析 JSON 消息的 type 字段路由到对应处理器，
 * 支持单播（指定 userId）和组播（会话所有成员）。
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 用户上下线回调接口。
     * 由上层模块实现并通过 setConnectionListener 注入。
     */
    public interface ConnectionListener {
        void onUserOnline(Long userId);
        void onUserOffline(Long userId);
    }

    private volatile ConnectionListener connectionListener;

    public void setConnectionListener(ConnectionListener listener) {
        this.connectionListener = listener;
    }

    /** 在线用户映射：userId → WebSocketSession */
    private final ConcurrentHashMap<Long, WebSocketSession> onlineSessions = new ConcurrentHashMap<>();

    /**
     * 消息路由回调接口。
     * 由业务层（如 ChatService、OnlineStatusManager）实现并注册。
     */
    public interface MessageRouter {
        /**
         * 处理指定类型的 WebSocket 消息。
         *
         * @param userId  发送者 userId
         * @param payload JSON 消息的 payload 节点
         */
        void handle(Long userId, JsonNode payload);
    }

    /** 消息类型 → 路由处理器 */
    private final ConcurrentHashMap<String, MessageRouter> routers = new ConcurrentHashMap<>();

    /**
     * 注册消息路由处理器。
     *
     * @param type   消息类型（如 "chat"、"status"）
     * @param router 处理器
     */
    public void registerRouter(String type, MessageRouter router) {
        routers.put(type, router);
        log.info("[ChatWS] 注册消息路由: type={}", type);
    }

    // ==================== 在线用户管理 ====================

    /**
     * 获取在线用户映射（只读视图）。
     */
    public Map<Long, WebSocketSession> getOnlineSessions() {
        return Collections.unmodifiableMap(onlineSessions);
    }

    /**
     * 判断用户是否在线。
     */
    public boolean isOnline(Long userId) {
        WebSocketSession session = onlineSessions.get(userId);
        return session != null && session.isOpen();
    }

    // ==================== WebSocket 生命周期 ====================

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = getUserId(session);
        if (userId == null) {
            log.warn("[ChatWS] 连接建立但缺少 userId，关闭连接");
            closeQuietly(session);
            return;
        }

        // 如果同一用户已有旧连接，关闭旧连接
        WebSocketSession oldSession = onlineSessions.put(userId, session);
        if (oldSession != null && oldSession.isOpen()) {
            log.info("[ChatWS] 用户 {} 重复连接，关闭旧会话", userId);
            closeQuietly(oldSession);
        }

        log.info("[ChatWS] 用户上线: userId={}, 当前在线人数={}", userId, onlineSessions.size());

        // 通知上层监听器
        if (connectionListener != null) {
            try { connectionListener.onUserOnline(userId); } catch (Exception e) {
                log.warn("[ChatWS] 上线回调失败: userId={}", userId);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = getUserId(session);
        if (userId != null) {
            // 仅当映射中的 session 与当前 session 相同时才移除（避免误删新连接）
            onlineSessions.remove(userId, session);
            log.info("[ChatWS] 用户下线: userId={}, closeStatus={}, 当前在线人数={}",
                    userId, status, onlineSessions.size());

            // 通知上层监听器
            if (connectionListener != null) {
                try { connectionListener.onUserOffline(userId); } catch (Exception e) {
                    log.warn("[ChatWS] 下线回调失败: userId={}", userId);
                }
            }
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        Long userId = getUserId(session);
        if (userId == null) {
            log.warn("[ChatWS] 收到消息但缺少 userId，忽略");
            return;
        }

        String payload = message.getPayload();
        try {
            JsonNode root = objectMapper.readTree(payload);
            String type = root.has("type") ? root.get("type").asText() : null;

            if (type == null || type.isBlank()) {
                log.warn("[ChatWS] 消息缺少 type 字段, userId={}", userId);
                return;
            }

            MessageRouter router = routers.get(type);
            if (router != null) {
                router.handle(userId, root.get("payload"));
            } else {
                log.warn("[ChatWS] 未知消息类型: type={}, userId={}", type, userId);
            }
        } catch (Exception e) {
            log.warn("[ChatWS] 消息解析失败: userId={}, error={}", userId, e.getMessage());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        Long userId = getUserId(session);
        log.error("[ChatWS] 传输错误: userId={}, error={}", userId, exception.getMessage());
        closeQuietly(session);
    }

    // ==================== 消息发送 ====================

    /**
     * 单播：向指定用户发送消息。
     *
     * @param userId  目标用户 ID
     * @param message JSON 消息字符串
     */
    public void sendToUser(Long userId, String message) {
        WebSocketSession session = onlineSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                log.error("[ChatWS] 发送消息失败: userId={}, error={}", userId, e.getMessage());
            }
        }
    }

    /**
     * 组播：向指定用户列表发送消息。
     *
     * @param userIds 目标用户 ID 列表
     * @param message JSON 消息字符串
     */
    public void sendToUsers(List<Long> userIds, String message) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        TextMessage textMessage = new TextMessage(message);
        for (Long userId : userIds) {
            WebSocketSession session = onlineSessions.get(userId);
            if (session != null && session.isOpen()) {
                try {
                    session.sendMessage(textMessage);
                } catch (IOException e) {
                    log.error("[ChatWS] 组播发送失败: userId={}, error={}", userId, e.getMessage());
                }
            }
        }
    }

    // ==================== 工具方法 ====================

    private Long getUserId(WebSocketSession session) {
        Object userId = session.getAttributes().get("userId");
        return userId instanceof Long ? (Long) userId : null;
    }

    private void closeQuietly(WebSocketSession session) {
        try {
            if (session.isOpen()) {
                session.close();
            }
        } catch (IOException e) {
            log.debug("[ChatWS] 关闭会话异常: {}", e.getMessage());
        }
    }
}
