package com.dataplatform.data.service.alert;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 通用Webhook告警渠道
 * 需求: 15.1
 */
@Slf4j
@Component
public class WebhookAlertChannel implements AlertChannel {

    @Value("${alert.webhook.url:}")
    private String webhookUrl;

    @Override
    public void send(String title, String content, String level, List<String> receivers) {
        if (webhookUrl == null || webhookUrl.isEmpty()) return;
        try {
            String json = "{" +
                    "\"title\":\"" + escapeJson(title) + "\"," +
                    "\"content\":\"" + escapeJson(content) + "\"," +
                    "\"level\":\"" + level + "\"," +
                    "\"timestamp\":\"" + LocalDateTime.now() + "\"," +
                    "\"receivers\":[" + buildReceiverArray(receivers) + "]" +
                    "}";
            HttpURLConnection conn = (HttpURLConnection) URI.create(webhookUrl).toURL().openConnection();
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
            log.info("Webhook告警发送成功: {}", title);
        } catch (Exception e) {
            log.error("Webhook告警发送失败: {}", e.getMessage());
        }
    }

    @Override
    public String getChannelType() {
        return "webhook";
    }

    @Override
    public boolean isAvailable() {
        return webhookUrl != null && !webhookUrl.isEmpty();
    }

    private String buildReceiverArray(List<String> receivers) {
        if (receivers == null || receivers.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < receivers.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(escapeJson(receivers.get(i))).append("\"");
        }
        return sb.toString();
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "");
    }
}
