package com.dataplatform.data.service;

import com.dataplatform.data.entity.NotificationLog;
import com.dataplatform.data.mapper.NotificationLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 内部通知路由器
 * 仅支持站内通知(site)和WebSocket推送
 * 钉钉/企微/短信/邮件等外部渠道由数据流程(Pipeline)节点直接调用各自服务
 */
@Slf4j
@Service
public class NotificationChannelRouter {

    @Autowired
    private NotificationLogMapper notificationLogMapper;

    @Autowired
    @Nullable
    private NotificationService notificationService;

    @Autowired
    @Nullable
    private MonitorWebSocketService webSocketService;

    /**
     * 路由通知到多个渠道
     *
     * @param channels         逗号分隔的渠道列表（如 "email,wecom,site"）
     * @param notificationType 通知类型: alert/export/task/report/system
     * @param subject          标题
     * @param content          内容
     * @param recipient        接收人信息（邮箱/手机号/userId，取决于渠道）
     * @param context          上下文变量（用于模板替换）
     */
    @Async
    public void route(String channels, String notificationType, String subject,
                      String content, String recipient, Map<String, String> context) {
        if (channels == null || channels.isEmpty()) {
            channels = "site";
        }

        String[] channelArray = channels.split(",");
        for (String channel : channelArray) {
            channel = channel.trim().toLowerCase();
            if (channel.isEmpty()) continue;

            try {
                sendToChannel(channel, notificationType, subject, content, recipient, context);
            } catch (Exception e) {
                log.error("[NotificationRouter] 发送到渠道 {} 失败: {}", channel, e.getMessage());
            }
        }
    }

    /**
     * 发送到指定渠道（仅支持 site 和 websocket）
     */
    public void sendToChannel(String channel, String notificationType, String subject,
                              String content, String recipient, Map<String, String> context) {
        NotificationLog logEntry = new NotificationLog();
        logEntry.setNotificationType(notificationType);
        logEntry.setChannel(channel);
        logEntry.setRecipient(recipient);
        logEntry.setSubject(subject);
        logEntry.setContent(content != null && content.length() > 500 ? content.substring(0, 500) : content);
        logEntry.setStatus("pending");
        logEntry.setRetryCount(0);
        logEntry.setSendTime(new Date());

        try {
            switch (channel) {
                case "site":
                    sendSiteNotification(subject, content, recipient);
                    break;
                case "websocket":
                    sendWebSocket(subject, content);
                    break;
                default:
                    log.warn("[NotificationRouter] 不支持的渠道: {}（外部渠道请在数据流程中配置）", channel);
                    logEntry.setStatus("skipped");
                    logEntry.setErrorMessage("渠道 " + channel + " 应通过数据流程配置");
                    saveLog(logEntry);
                    return;
            }

            logEntry.setStatus("success");
            log.debug("[NotificationRouter] 发送成功: channel={}, type={}", channel, notificationType);

        } catch (Exception e) {
            logEntry.setStatus("failed");
            logEntry.setErrorMessage(e.getMessage());
            log.error("[NotificationRouter] 发送失败: channel={}, error={}", channel, e.getMessage());
        }

        saveLog(logEntry);
    }

    // ==================== 渠道发送实现 ====================

    private void sendSiteNotification(String subject, String content, String recipient) {
        if (notificationService == null) {
            throw new RuntimeException("NotificationService 未配置");
        }
        com.dataplatform.data.entity.Notification notification = new com.dataplatform.data.entity.Notification();
        notification.setTitle(subject);
        notification.setContent(content);
        notification.setNotificationType("alert");
        notification.setPriority("high");
        if (recipient != null && !recipient.isEmpty()) {
            try {
                notification.setTargetUserId(Long.parseLong(recipient));
            } catch (NumberFormatException ignored) {
            }
        }
        notificationService.send(notification);
    }

    private void sendWebSocket(String subject, String content) {
        if (webSocketService == null) return;
        Map<String, Object> alertData = new HashMap<>();
        alertData.put("title", subject);
        alertData.put("message", content);
        alertData.put("timestamp", System.currentTimeMillis());
        webSocketService.broadcastAlert(alertData);
    }

    // ==================== 工具方法 ====================

    private void saveLog(NotificationLog logEntry) {
        try {
            notificationLogMapper.insert(logEntry);
        } catch (Exception e) {
            log.warn("[NotificationRouter] 保存投递日志失败: {}", e.getMessage  ());
        }
    }
}
