package com.dataplatform.data.service;

import com.dataplatform.common.exception.BusinessException;
import com.dataplatform.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 企业微信 Webhook 通知服务
 * 支持 text/markdown/image 消息类型、@成员、模板变量替换
 */
@Slf4j
@Service
public class WecomNotifyService {

    @Autowired
    private com.dataplatform.common.service.SystemConfigProvider systemConfigService;
    
    @Autowired(required = false)
    private MessageChannelService messageChannelService;

    // access_token 缓存: key=corpId, value=[token, expireTimestamp]
    private final ConcurrentHashMap<String, Object[]> tokenCache = new ConcurrentHashMap<>();
    private static final long TOKEN_CACHE_TTL = 7000 * 1000L; // 7000秒（略小于7200秒有效期）

    /**
     * 发送企业微信通知
     *
     * @param config  节点配置 (webhookUrl, msgType, mentionUsers, content, sendCondition, channelId)
     * @param context 流程上下文变量 (pipeline.name, node.name, node.status, timestamp)
     */
    public void send(Map<String, Object> config, Map<String, String> context) {
        String webhookUrl = (String) config.get("webhookUrl");
        
        // 优先使用通道配置
        Long channelId = null;
        Object channelIdObj = config.get("channelId");
        if (channelIdObj != null) {
            channelId = channelIdObj instanceof Number ? ((Number) channelIdObj).longValue() : Long.parseLong(channelIdObj.toString());
        }
        
        if (channelId != null && messageChannelService != null) {
            com.dataplatform.data.entity.MessageChannel channel = messageChannelService.getChannel(channelId, "wecom");
            if (channel != null) {
                Map<String, Object> channelConfig = messageChannelService.parseConfig(channel);
                webhookUrl = (String) channelConfig.get("webhookUrl");
                log.info("使用通道配置发送企微通知: channelId={}", channelId);
            }
        }
        
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            // 从系统配置读取默认 Webhook
            webhookUrl = systemConfigService.getValueByKey("wecom.webhook.default");
        }
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            log.warn("企业微信 Webhook 地址未配置，跳过通知");
            return;
        }

        String msgType = (String) config.getOrDefault("msgType", "text");
        String content = (String) config.getOrDefault("content", "");
        List<String> mentionUsers = (List<String>) config.get("mentionUsers");

        // 变量替换
        content = replaceVariables(content, context);

        String jsonPayload;
        switch (msgType) {
            case "markdown":
                jsonPayload = buildMarkdownPayload(content, mentionUsers);
                break;
            case "image":
                jsonPayload = buildImagePayload(content);
                break;
            default:
                jsonPayload = buildTextPayload(content, mentionUsers);
                break;
        }

        try {
            sendWebhook(webhookUrl, jsonPayload);
            log.info("企业微信通知发送成功");
        } catch (Exception e) {
            log.error("企业微信通知发送失败: {}", e.getMessage());
            throw new BusinessException(ErrorCode.NOTIFICATION_SEND_FAILED, "企业微信通知发送失败: " + e.getMessage());
        }
    }

    private String buildTextPayload(String content, List<String> mentionUsers) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"msgtype\":\"text\",\"text\":{\"content\":\"").append(escapeJson(content)).append("\"");
        if (mentionUsers != null && !mentionUsers.isEmpty()) {
            sb.append(",\"mentioned_list\":[");
            for (int i = 0; i < mentionUsers.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append("\"").append(escapeJson(mentionUsers.get(i))).append("\"");
            }
            sb.append("]");
        }
        sb.append("}}");
        return sb.toString();
    }

    private String buildMarkdownPayload(String content, List<String> mentionUsers) {
        if (mentionUsers != null && !mentionUsers.isEmpty()) {
            StringBuilder mentionStr = new StringBuilder("\n");
            for (String user : mentionUsers) {
                if ("@all".equals(user)) {
                    mentionStr.append("<@all> ");
                } else {
                    mentionStr.append("<@").append(user).append("> ");
                }
            }
            content = content + mentionStr;
        }
        return "{\"msgtype\":\"markdown\",\"markdown\":{\"content\":\"" + escapeJson(content) + "\"}}";
    }

    private String buildImagePayload(String content) {
        // content should be base64 encoded image or md5
        return "{\"msgtype\":\"image\",\"image\":{\"base64\":\"" + escapeJson(content) + "\",\"md5\":\"\"}}";
    }

    // ==================== 应用消息模式 ====================

    /**
     * 通过企业微信应用消息API发送消息到指定员工
     * 需配置: wecom.app.corpId, wecom.app.corpSecret, wecom.app.agentId
     *
     * @param toUser  接收人userId列表("|"分隔)，如 "user1|user2"，"@all"表示全部
     * @param content 消息内容
     * @param msgType 消息类型: text/textcard
     */
    public void sendAppMessage(String toUser, String content, String msgType) {
        String corpId = systemConfigService.getValueByKey("wecom.app.corpId");
        String corpSecret = systemConfigService.getValueByKey("wecom.app.corpSecret");
        String agentId = systemConfigService.getValueByKey("wecom.app.agentId");
        String enabled = systemConfigService.getValueByKey("wecom.app.enabled", "false");

        if (!"true".equals(enabled) || corpId == null || corpSecret == null || agentId == null) {
            log.warn("企业微信应用消息未启用或未配置，跳过发送");
            return;
        }

        try {
            String accessToken = getAccessToken(corpId, corpSecret);
            if (accessToken == null) {
                log.error("获取企业微信 access_token 失败");
                return;
            }

            String json;
            if ("textcard".equals(msgType)) {
                json = String.format(
                    "{\"touser\":\"%s\",\"msgtype\":\"textcard\",\"agentid\":%s,\"textcard\":{\"title\":\"系统通知\",\"description\":\"%s\",\"url\":\"\"}}",
                    escapeJson(toUser), agentId, escapeJson(content));
            } else {
                json = String.format(
                    "{\"touser\":\"%s\",\"msgtype\":\"text\",\"agentid\":%s,\"text\":{\"content\":\"%s\"}}",
                    escapeJson(toUser), agentId, escapeJson(content));
            }

            String apiUrl = "https://qyapi.weixin.qq.com/cgi-bin/message/send?access_token=" + accessToken;
            sendWebhook(apiUrl, json);
            log.info("企业微信应用消息发送成功: toUser={}", toUser);
        } catch (Exception e) {
            log.error("企业微信应用消息发送失败: {}", e.getMessage());
            throw new BusinessException(ErrorCode.NOTIFICATION_SEND_FAILED, "企业微信应用消息发送失败: " + e.getMessage());
        }
    }

    /**
     * 获取企业微信 access_token（带缓存，7000秒过期）
     */
    private String getAccessToken(String corpId, String corpSecret) {
        // 检查缓存
        Object[] cached = tokenCache.get(corpId);
        if (cached != null && System.currentTimeMillis() < (long) cached[1]) {
            log.debug("使用缓存的企业微信access_token: corpId={}", corpId);
            return (String) cached[0];
        }

        try {
            String apiUrl = String.format(
                "https://qyapi.weixin.qq.com/cgi-bin/gettoken?corpid=%s&corpsecret=%s", corpId, corpSecret);
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int code = conn.getResponseCode();
            if (code == 200) {
                java.io.InputStream is = conn.getInputStream();
                byte[] data = is.readAllBytes();
                String resp = new String(data, StandardCharsets.UTF_8);
                int idx = resp.indexOf("\"access_token\":\"");
                if (idx > 0) {
                    int start = idx + 16;
                    int end = resp.indexOf("\"", start);
                    String token = resp.substring(start, end);
                    // 缓存 token
                    tokenCache.put(corpId, new Object[]{token, System.currentTimeMillis() + TOKEN_CACHE_TTL});
                    log.info("获取企业微信access_token成功并缓存: corpId={}", corpId);
                    return token;
                }
            }
        } catch (Exception e) {
            log.error("获取企业微信access_token失败: {}", e.getMessage());
        }
        return null;
    }

    private String replaceVariables(String template, Map<String, String> context) {
        if (template == null || context == null) return template;
        for (Map.Entry<String, String> entry : context.entrySet()) {
            template = template.replace("${" + entry.getKey() + "}", entry.getValue() != null ? entry.getValue() : "");
        }
        return template;
    }

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
            throw new BusinessException(ErrorCode.NOTIFICATION_SEND_FAILED, "Webhook 返回非200状态码: " + responseCode);
        }
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
