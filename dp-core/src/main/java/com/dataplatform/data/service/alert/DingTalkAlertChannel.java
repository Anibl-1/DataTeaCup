package com.dataplatform.data.service.alert;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 钉钉告警渠道
 * 需求: 15.1
 */
@Slf4j
@Component
public class DingTalkAlertChannel implements AlertChannel {

    @Value("${alert.dingtalk.webhook:}")
    private String webhook;

    @Override
    public void send(String title, String content, String level, List<String> receivers) {
        if (webhook == null || webhook.isEmpty()) return;
        try {
            StringBuilder markdown = new StringBuilder();
            markdown.append("### ").append(getLevelEmoji(level)).append(" ").append(title).append("\n\n");
            markdown.append(content).append("\n\n");
            if (!receivers.isEmpty()) {
                for (String r : receivers) {
                    markdown.append("@").append(r).append(" ");
                }
            }

            String json = "{\"msgtype\":\"markdown\",\"markdown\":{\"title\":\"" +
                    escapeJson(title) + "\",\"text\":\"" + escapeJson(markdown.toString()) + "\"}}";
            sendWebhook(json);
            log.info("钉钉告警发送成功: {}", title);
        } catch (Exception e) {
            log.error("钉钉告警发送失败: {}", e.getMessage());
        }
    }

    @Override
    public String getChannelType() {
        return "dingtalk";
    }

    @Override
    public boolean isAvailable() {
        return webhook != null && !webhook.isEmpty();
    }

    private void sendWebhook(String json) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) URI.create(webhook).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setDoOutput(true);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }
        conn.getResponseCode();
        conn.disconnect();
    }

    private String getLevelEmoji(String level) {
        return switch (level.toLowerCase()) {
            case "emergency" -> "🔴";
            case "critical" -> "🟠";
            case "warning" -> "🟡";
            default -> "🔵";
        };
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "");
    }
}
