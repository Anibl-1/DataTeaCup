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
 * 企业微信告警渠道
 * 需求: 15.1
 */
@Slf4j
@Component
public class WecomAlertChannel implements AlertChannel {

    @Value("${alert.wecom.webhook:}")
    private String webhook;

    @Override
    public void send(String title, String content, String level, List<String> receivers) {
        if (webhook == null || webhook.isEmpty()) return;
        try {
            String text = "[" + level.toUpperCase() + "] " + title + "\n" + content;
            String json = "{\"msgtype\":\"text\",\"text\":{\"content\":\"" + escapeJson(text) + "\"}}";
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
            log.info("企微告警发送成功: {}", title);
        } catch (Exception e) {
            log.error("企微告警发送失败: {}", e.getMessage());
        }
    }

    @Override
    public String getChannelType() {
        return "wecom";
    }

    @Override
    public boolean isAvailable() {
        return webhook != null && !webhook.isEmpty();
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "");
    }
}
