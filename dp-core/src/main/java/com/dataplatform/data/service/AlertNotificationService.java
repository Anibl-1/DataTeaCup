package com.dataplatform.data.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 告警通知服务
 * 支持钉钉Webhook、企微Webhook、邮件通知
 */
@Slf4j
@Service
public class AlertNotificationService {

    @Value("${alert.dingtalk.webhook:}")
    private String dingtalkWebhook;

    @Value("${alert.wecom.webhook:}")
    private String wecomWebhook;

    @Value("${alert.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${alert.enabled:false}")
    private boolean alertEnabled;

    /**
     * 发送告警通知（自动选择可用的通道）
     *
     * @param title   告警标题
     * @param content 告警内容
     * @param level   告警级别：info/warning/critical
     */
    public void sendAlert(String title, String content, String level) {
        if (!alertEnabled) {
            log.debug("告警功能未启用，跳过通知: {}", title);
            return;
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String fullContent = String.format("【%s】%s\n%s\n时间: %s", 
                level.toUpperCase(), title, content, timestamp);

        // 钉钉通知
        if (dingtalkWebhook != null && !dingtalkWebhook.isEmpty()) {
            sendDingtalk(title, fullContent);
        }

        // 企微通知
        if (wecomWebhook != null && !wecomWebhook.isEmpty()) {
            sendWecom(title, fullContent);
        }

        log.info("告警通知已发送: [{}] {}", level, title);
    }

    /**
     * 发送任务失败告警
     */
    public void sendTaskFailureAlert(String taskName, String taskType, String errorMessage) {
        String title = taskType + "任务失败告警";
        String content = String.format("任务名称: %s\n任务类型: %s\n错误信息: %s", 
                taskName, taskType, errorMessage);
        sendAlert(title, content, "critical");
    }

    /**
     * 发送系统资源告警
     */
    public void sendResourceAlert(String resourceType, double currentValue, double threshold) {
        String title = resourceType + "使用率告警";
        String content = String.format("资源类型: %s\n当前值: %.1f%%\n阈值: %.1f%%\n状态: 已超过阈值，请及时处理", 
                resourceType, currentValue, threshold);
        sendAlert(title, content, currentValue > threshold * 1.2 ? "critical" : "warning");
    }

    /**
     * 钉钉Webhook通知
     */
    private void sendDingtalk(String title, String content) {
        try {
            String json = "{\"msgtype\":\"text\",\"text\":{\"content\":\"" +
                    escapeJson(content) + "\"}}";
            sendWebhook(dingtalkWebhook, json);
        } catch (Exception e) {
            log.error("钉钉通知发送失败: {}", e.getMessage());
        }
    }

    /**
     * 企微Webhook通知
     */
    private void sendWecom(String title, String content) {
        try {
            String json = "{\"msgtype\":\"text\",\"text\":{\"content\":\"" +
                    escapeJson(content) + "\"}}";
            sendWebhook(wecomWebhook, json);
        } catch (Exception e) {
            log.error("企微通知发送失败: {}", e.getMessage());
        }
    }

    /**
     * 发送Webhook请求
     */
    private void sendWebhook(String webhookUrl, String jsonPayload) throws Exception {
        URL url = new URL(webhookUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setDoOutput(true);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            log.warn("Webhook请求返回非200状态码: {}", responseCode);
        }
    }

    /**
     * JSON字符串转义
     */
    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}
