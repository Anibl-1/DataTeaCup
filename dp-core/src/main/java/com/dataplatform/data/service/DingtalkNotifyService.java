package com.dataplatform.data.service;

import com.dataplatform.common.exception.BusinessException;
import com.dataplatform.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * 钉钉通知服务
 * 支持 text / markdown / actionCard 消息类型
 * 支持加签安全验证（HmacSHA256）
 * 支持 @指定人（通过手机号）
 */
@Slf4j
@Service
public class DingtalkNotifyService {

    @Autowired
    private com.dataplatform.common.service.SystemConfigProvider systemConfigService;
    
    @Autowired(required = false)
    private MessageChannelService messageChannelService;

    /**
     * 发送钉钉通知（流程节点调用）
     *
     * @param config  节点配置 (webhookUrl, secret, msgType, content, title, mentionMobiles, btnTitle, btnUrl, channelId)
     * @param context 流程上下文变量
     */
    public void send(Map<String, Object> config, Map<String, String> context) {
        String webhookUrl = (String) config.get("webhookUrl");
        String secret = (String) config.get("secret");
        
        // 优先使用通道配置
        Long channelId = null;
        Object channelIdObj = config.get("channelId");
        if (channelIdObj != null) {
            channelId = channelIdObj instanceof Number ? ((Number) channelIdObj).longValue() : Long.parseLong(channelIdObj.toString());
        }
        
        if (channelId != null && messageChannelService != null) {
            com.dataplatform.data.entity.MessageChannel channel = messageChannelService.getChannel(channelId, "dingtalk");
            if (channel != null) {
                Map<String, Object> channelConfig = messageChannelService.parseConfig(channel);
                webhookUrl = (String) channelConfig.get("webhookUrl");
                secret = (String) channelConfig.get("secret");
                log.info("使用通道配置发送钉钉通知: channelId={}", channelId);
            }
        }
        
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            webhookUrl = systemConfigService.getValueByKey("dingtalk.webhook.default");
        }
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            log.warn("钉钉 Webhook 地址未配置，跳过通知");
            return;
        }

        if (secret == null || secret.isEmpty()) {
            secret = systemConfigService.getValueByKey("dingtalk.webhook.secret");
        }

        // 加签
        if (secret != null && !secret.isEmpty()) {
            webhookUrl = appendSignature(webhookUrl, secret);
        }

        String msgType = (String) config.getOrDefault("msgType", "text");
        String content = (String) config.getOrDefault("content", "");
        String title = (String) config.getOrDefault("title", "通知");
        List<String> mentionMobiles = (List<String>) config.get("mentionMobiles");

        // 变量替换
        content = replaceVariables(content, context);
        title = replaceVariables(title, context);

        String jsonPayload;
        switch (msgType) {
            case "markdown":
                jsonPayload = buildMarkdownPayload(title, content, mentionMobiles);
                break;
            case "actionCard":
                String btnTitle = (String) config.getOrDefault("btnTitle", "查看详情");
                String btnUrl = (String) config.getOrDefault("btnUrl", "");
                btnUrl = replaceVariables(btnUrl, context);
                jsonPayload = buildActionCardPayload(title, content, btnTitle, btnUrl);
                break;
            default:
                jsonPayload = buildTextPayload(content, mentionMobiles);
                break;
        }

        try {
            sendWebhook(webhookUrl, jsonPayload);
            log.info("钉钉通知发送成功: type={}", msgType);
        } catch (Exception e) {
            log.error("钉钉通知发送失败: {}", e.getMessage());
            throw new BusinessException(ErrorCode.NOTIFICATION_SEND_FAILED, "钉钉通知发送失败: " + e.getMessage());
        }
    }

    /**
     * 发送简单文本消息（告警快捷方法）
     */
    public void sendText(String content) {
        String webhookUrl = systemConfigService.getValueByKey("dingtalk.webhook.default");
        if (webhookUrl == null || webhookUrl.isEmpty()) return;

        String secret = systemConfigService.getValueByKey("dingtalk.webhook.secret");
        if (secret != null && !secret.isEmpty()) {
            webhookUrl = appendSignature(webhookUrl, secret);
        }

        try {
            sendWebhook(webhookUrl, buildTextPayload(content, null));
        } catch (Exception e) {
            log.error("钉钉文本消息发送失败: {}", e.getMessage());
        }
    }

    /**
     * 发送 Markdown 消息（告警快捷方法）
     */
    public void sendMarkdown(String title, String markdownContent) {
        String webhookUrl = systemConfigService.getValueByKey("dingtalk.webhook.default");
        if (webhookUrl == null || webhookUrl.isEmpty()) return;

        String secret = systemConfigService.getValueByKey("dingtalk.webhook.secret");
        if (secret != null && !secret.isEmpty()) {
            webhookUrl = appendSignature(webhookUrl, secret);
        }

        try {
            sendWebhook(webhookUrl, buildMarkdownPayload(title, markdownContent, null));
        } catch (Exception e) {
            log.error("钉钉Markdown消息发送失败: {}", e.getMessage());
        }
    }

    // ==================== 消息构建 ====================

    private String buildTextPayload(String content, List<String> mentionMobiles) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"msgtype\":\"text\",\"text\":{\"content\":\"").append(escapeJson(content)).append("\"}");
        appendAt(sb, mentionMobiles);
        sb.append("}");
        return sb.toString();
    }

    private String buildMarkdownPayload(String title, String content, List<String> mentionMobiles) {
        // Markdown 中 @人需要在文本中添加 @手机号
        if (mentionMobiles != null && !mentionMobiles.isEmpty()) {
            StringBuilder mentionStr = new StringBuilder("\n\n");
            for (String mobile : mentionMobiles) {
                if ("all".equals(mobile)) {
                    mentionStr.append("@所有人 ");
                } else {
                    mentionStr.append("@").append(mobile).append(" ");
                }
            }
            content = content + mentionStr;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{\"msgtype\":\"markdown\",\"markdown\":{\"title\":\"").append(escapeJson(title))
          .append("\",\"text\":\"").append(escapeJson(content)).append("\"}");
        appendAt(sb, mentionMobiles);
        sb.append("}");
        return sb.toString();
    }

    private String buildActionCardPayload(String title, String content, String btnTitle, String btnUrl) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"msgtype\":\"actionCard\",\"actionCard\":{")
          .append("\"title\":\"").append(escapeJson(title)).append("\",")
          .append("\"text\":\"").append(escapeJson(content)).append("\",")
          .append("\"btnOrientation\":\"0\",")
          .append("\"singleTitle\":\"").append(escapeJson(btnTitle)).append("\",")
          .append("\"singleURL\":\"").append(escapeJson(btnUrl)).append("\"")
          .append("}}");
        return sb.toString();
    }

    private void appendAt(StringBuilder sb, List<String> mentionMobiles) {
        if (mentionMobiles != null && !mentionMobiles.isEmpty()) {
            sb.append(",\"at\":{\"atMobiles\":[");
            boolean isAll = false;
            for (int i = 0; i < mentionMobiles.size(); i++) {
                if ("all".equals(mentionMobiles.get(i))) {
                    isAll = true;
                    continue;
                }
                if (i > 0) sb.append(",");
                sb.append("\"").append(escapeJson(mentionMobiles.get(i))).append("\"");
            }
            sb.append("],\"isAtAll\":").append(isAll).append("}");
        }
    }

    // ==================== 加签 ====================

    private String appendSignature(String webhookUrl, String secret) {
        try {
            long timestamp = System.currentTimeMillis();
            String stringToSign = timestamp + "\n" + secret;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
            String sign = URLEncoder.encode(Base64.getEncoder().encodeToString(signData), "UTF-8");
            String separator = webhookUrl.contains("?") ? "&" : "?";
            return webhookUrl + separator + "timestamp=" + timestamp + "&sign=" + sign;
        } catch (Exception e) {
            log.error("钉钉加签失败: {}", e.getMessage());
            return webhookUrl;
        }
    }

    // ==================== HTTP ====================

    private void sendWebhook(String webhookUrl, String jsonPayload) throws Exception {
        URL url = new URL(webhookUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new BusinessException(ErrorCode.NOTIFICATION_SEND_FAILED, "钉钉API返回非200: " + responseCode);
        }
    }

    private String replaceVariables(String template, Map<String, String> context) {
        if (template == null || context == null) return template;
        for (Map.Entry<String, String> entry : context.entrySet()) {
            template = template.replace("${" + entry.getKey() + "}", entry.getValue() != null ? entry.getValue() : "");
        }
        return template;
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}
