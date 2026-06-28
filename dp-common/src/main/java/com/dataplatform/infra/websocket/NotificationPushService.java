package com.dataplatform.infra.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 通知推送服务
 * 通过 STOMP WebSocket 向指定用户推送实时通知，替代前端轮询机制。
 * 推送目标：/topic/notification/{userId}
 *
 * Validates: Requirements 14.1
 */
@Slf4j
@Service
public class NotificationPushService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private static final String NOTIFICATION_TOPIC = "/topic/notification/";

    /**
     * 向指定用户推送通知
     *
     * @param userId  目标用户 ID
     * @param payload 通知内容（应包含 id, title, content, type 等字段）
     */
    public void pushNotification(Long userId, Map<String, Object> payload) {
        if (userId == null) {
            log.warn("[NotificationPush] userId 为空，跳过推送");
            return;
        }
        try {
            // 确保 createTime 存在
            payload.putIfAbsent("createTime",
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            messagingTemplate.convertAndSend(NOTIFICATION_TOPIC + userId, payload);
            log.debug("[NotificationPush] 推送通知给用户 {}: {}", userId, payload.get("title"));
        } catch (Exception e) {
            log.error("[NotificationPush] 推送通知失败, userId={}", userId, e);
        }
    }

    /**
     * 推送聊天消息通知
     *
     * @param userId         目标用户 ID
     * @param conversationId 关联的聊天会话 ID
     * @param senderName     发送者昵称
     * @param content        消息内容预览
     */
    public void pushChatNotification(Long userId, Long conversationId,
                                     String senderName, String content) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", System.currentTimeMillis());
        payload.put("title", senderName + " 发来一条消息");
        payload.put("content", content != null && content.length() > 100
                ? content.substring(0, 100) + "..." : content);
        payload.put("type", "chat");
        payload.put("conversationId", conversationId);
        pushNotification(userId, payload);
    }

    /**
     * 推送系统通知
     *
     * @param userId  目标用户 ID
     * @param title   通知标题
     * @param content 通知内容
     */
    public void pushSystemNotification(Long userId, String title, String content) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", System.currentTimeMillis());
        payload.put("title", title);
        payload.put("content", content);
        payload.put("type", "system");
        pushNotification(userId, payload);
    }
}
