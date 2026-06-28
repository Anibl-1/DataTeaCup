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

/**
 * 短信通知服务
 * 通过 HTTP API 调用短信服务商发送短信
 * 支持阿里云、腾讯云等主流短信平台，配置从 SystemConfigService 读取
 */
@Slf4j
@Service
public class SmsNotifyService {

    @Autowired
    private com.dataplatform.common.service.SystemConfigProvider systemConfigService;
    
    @Autowired(required = false)
    private MessageChannelService messageChannelService;

    /**
     * 发送短信通知
     *
     * @param config  节点配置 (phoneNumbers, templateId, templateParams, sendCondition, channelId)
     * @param context 流程上下文变量
     */
    public void send(Map<String, Object> config, Map<String, String> context) {
        List<String> phoneNumbers = (List<String>) config.get("phoneNumbers");
        String smsMode = (String) config.getOrDefault("smsMode", "content"); // content 或 template
        String content = (String) config.get("content");
        String templateId = (String) config.get("templateId");
        List<Map<String, String>> templateParams = (List<Map<String, String>>) config.get("templateParams");

        if (phoneNumbers == null || phoneNumbers.isEmpty()) {
            log.warn("短信手机号列表为空，跳过发送");
            return;
        }
        
        // 优先使用通道配置
        Long channelId = null;
        Object channelIdObj = config.get("channelId");
        if (channelIdObj != null) {
            channelId = channelIdObj instanceof Number ? ((Number) channelIdObj).longValue() : Long.parseLong(channelIdObj.toString());
        }
        
        Map<String, Object> channelConfig = null;
        if (channelId != null && messageChannelService != null) {
            com.dataplatform.data.entity.MessageChannel channel = messageChannelService.getChannel(channelId, "sms");
            if (channel != null) {
                channelConfig = messageChannelService.parseConfig(channel);
                log.info("使用通道配置发送短信: channelId={}", channelId);
            }
        }

        String provider = channelConfig != null ? (String) channelConfig.get("provider") : systemConfigService.getValueByKey("sms.provider");
        if (provider == null || provider.isEmpty()) {
            provider = "generic";
        }

        try {
            // 直接内容模式 - 适用于内网网关或简单场景
            if ("content".equals(smsMode) || templateId == null || templateId.isEmpty()) {
                if (content != null && !content.isEmpty()) {
                    content = replaceVariables(content, context);
                    sendDirectContent(phoneNumbers, content, channelConfig);
                    log.info("短信通知发送成功(直接内容): phones={}", phoneNumbers);
                    return;
                }
            }
            
            // 模板模式 - 云服务商
            String templateParamJson = buildTemplateParams(templateParams, context);
            switch (provider.toLowerCase()) {
                case "aliyun":
                    sendAliyun(phoneNumbers, templateId, templateParamJson);
                    break;
                case "tencent":
                    sendTencent(phoneNumbers, templateId, templateParamJson);
                    break;
                default:
                    sendGeneric(phoneNumbers, templateId, templateParamJson);
                    break;
            }
            log.info("短信通知发送成功(模板): phones={}, template={}", phoneNumbers, templateId);
        } catch (Exception e) {
            log.error("短信通知发送失败: {}", e.getMessage());
            throw new BusinessException(ErrorCode.NOTIFICATION_SEND_FAILED, "短信通知发送失败: " + e.getMessage());
        }
    }
    
    /**
     * 直接内容发送（适用于内网短信网关）
     */
    private void sendDirectContent(List<String> phones, String content, Map<String, Object> channelConfig) throws Exception {
        String gatewayUrl = channelConfig != null ? (String) channelConfig.get("gatewayUrl") : systemConfigService.getValueByKey("sms.gateway.url");
        if (gatewayUrl == null || gatewayUrl.isEmpty()) {
            log.warn("短信网关地址未配置，跳过发送");
            return;
        }
        
        String phoneParam = channelConfig != null ? (String) channelConfig.getOrDefault("phoneParam", "phone") : "phone";
        String contentParam = channelConfig != null ? (String) channelConfig.getOrDefault("contentParam", "content") : "content";
        String httpMethod = channelConfig != null ? (String) channelConfig.getOrDefault("httpMethod", "POST") : "POST";
        String contentType = channelConfig != null ? (String) channelConfig.getOrDefault("contentType", "application/json") : "application/json";
        
        for (String phone : phones) {
            String payload;
            if ("application/json".equals(contentType)) {
                payload = String.format("{\"%s\":\"%s\",\"%s\":\"%s\"}", phoneParam, phone, contentParam, escapeJson(content));
            } else {
                payload = String.format("%s=%s&%s=%s", phoneParam, java.net.URLEncoder.encode(phone, "UTF-8"), contentParam, java.net.URLEncoder.encode(content, "UTF-8"));
            }
            
            java.net.URL url = new java.net.URL(gatewayUrl);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod(httpMethod);
            conn.setRequestProperty("Content-Type", contentType);
            conn.setDoOutput(true);
            
            try (java.io.OutputStream os = conn.getOutputStream()) {
                os.write(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            
            int responseCode = conn.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                log.warn("短信发送响应异常: phone={}, code={}", phone, responseCode);
            }
        }
    }

    /**
     * 阿里云短信 API（带签名验证）
     * 使用 POP 签名方式，参数排序 + HMAC-SHA1 + Base64
     */
    private void sendAliyun(List<String> phones, String templateId, String templateParam) throws Exception {
        String accessKeyId = systemConfigService.getValueByKey("sms.aliyun.accessKeyId");
        String accessKeySecret = systemConfigService.getValueByKey("sms.aliyun.accessKeySecret");
        String signName = systemConfigService.getValueByKey("sms.aliyun.signName");

        if (accessKeyId == null || accessKeySecret == null) {
            log.warn("阿里云短信 AccessKey 未配置，跳过发送");
            return;
        }

        String phoneStr = String.join(",", phones);

        // 构建公共参数 + 业务参数
        java.util.TreeMap<String, String> params = new java.util.TreeMap<>();
        params.put("AccessKeyId", accessKeyId);
        params.put("Action", "SendSms");
        params.put("Format", "JSON");
        params.put("PhoneNumbers", phoneStr);
        params.put("RegionId", "cn-hangzhou");
        params.put("SignName", signName != null ? signName : "");
        params.put("SignatureMethod", "HMAC-SHA1");
        params.put("SignatureNonce", java.util.UUID.randomUUID().toString());
        params.put("SignatureVersion", "1.0");
        params.put("TemplateCode", templateId != null ? templateId : "");
        params.put("TemplateParam", templateParam);
        params.put("Timestamp", new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'") {{
            setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        }}.format(new java.util.Date()));
        params.put("Version", "2017-05-25");

        // 构造待签名字符串
        StringBuilder sortedQueryBuilder = new StringBuilder();
        for (java.util.Map.Entry<String, String> e : params.entrySet()) {
            if (sortedQueryBuilder.length() > 0) sortedQueryBuilder.append("&");
            sortedQueryBuilder.append(percentEncode(e.getKey())).append("=").append(percentEncode(e.getValue()));
        }
        String stringToSign = "GET&" + percentEncode("/") + "&" + percentEncode(sortedQueryBuilder.toString());

        // HMAC-SHA1 签名
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA1");
        mac.init(new javax.crypto.spec.SecretKeySpec((accessKeySecret + "&").getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
        byte[] signBytes = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
        String signature = java.util.Base64.getEncoder().encodeToString(signBytes);
        params.put("Signature", signature);

        // 构造最终URL
        StringBuilder urlBuilder = new StringBuilder("https://dysmsapi.aliyuncs.com/?");
        boolean first = true;
        for (java.util.Map.Entry<String, String> e : params.entrySet()) {
            if (!first) urlBuilder.append("&");
            urlBuilder.append(java.net.URLEncoder.encode(e.getKey(), "UTF-8"))
                      .append("=")
                      .append(java.net.URLEncoder.encode(e.getValue(), "UTF-8"));
            first = false;
        }

        log.info("阿里云短信 API 调用: phones={}, template={}", phoneStr, templateId);
        sendHttpGet(urlBuilder.toString());
    }

    /**
     * 腾讯云短信 API（TC3-HMAC-SHA256 签名）
     */
    private void sendTencent(List<String> phones, String templateId, String templateParam) throws Exception {
        String secretId = systemConfigService.getValueByKey("sms.tencent.secretId");
        String secretKey = systemConfigService.getValueByKey("sms.tencent.secretKey");
        String appId = systemConfigService.getValueByKey("sms.tencent.appId");
        String signName = systemConfigService.getValueByKey("sms.tencent.signName");

        if (secretId == null || secretKey == null) {
            log.warn("腾讯云短信密钥未配置，跳过发送");
            return;
        }

        // 构建请求体
        StringBuilder phoneArray = new StringBuilder("[");
        for (int i = 0; i < phones.size(); i++) {
            if (i > 0) phoneArray.append(",");
            String phone = phones.get(i);
            if (!phone.startsWith("+")) phone = "+86" + phone;
            phoneArray.append("\"").append(phone).append("\"");
        }
        phoneArray.append("]");

        String requestBody = String.format(
            "{\"SmsSdkAppId\":\"%s\",\"SignName\":\"%s\",\"TemplateId\":\"%s\",\"PhoneNumberSet\":%s,\"TemplateParamSet\":[]}",
            appId != null ? appId : "", signName != null ? signName : "",
            templateId != null ? templateId : "", phoneArray);

        // TC3-HMAC-SHA256 签名
        String service = "sms";
        String host = "sms.tencentcloudapi.com";
        long timestampSec = System.currentTimeMillis() / 1000;
        String date = new java.text.SimpleDateFormat("yyyy-MM-dd") {{
            setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        }}.format(new java.util.Date(timestampSec * 1000));

        // CanonicalRequest
        String hashedPayload = sha256Hex(requestBody);
        String canonicalRequest = "POST\n/\n\ncontent-type:application/json; charset=utf-8\nhost:" + host + "\n\ncontent-type;host\n" + hashedPayload;

        // StringToSign
        String credentialScope = date + "/" + service + "/tc3_request";
        String stringToSign = "TC3-HMAC-SHA256\n" + timestampSec + "\n" + credentialScope + "\n" + sha256Hex(canonicalRequest);

        // 计算签名
        byte[] secretDate = hmacSha256(("TC3" + secretKey).getBytes(StandardCharsets.UTF_8), date);
        byte[] secretService = hmacSha256(secretDate, service);
        byte[] secretSigning = hmacSha256(secretService, "tc3_request");
        String signature = bytesToHex(hmacSha256(secretSigning, stringToSign));

        String authorization = "TC3-HMAC-SHA256 Credential=" + secretId + "/" + credentialScope +
                ", SignedHeaders=content-type;host, Signature=" + signature;

        // 发送请求
        java.net.URL url = new java.net.URL("https://" + host);
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setRequestProperty("Host", host);
        conn.setRequestProperty("Authorization", authorization);
        conn.setRequestProperty("X-TC-Action", "SendSms");
        conn.setRequestProperty("X-TC-Version", "2021-01-11");
        conn.setRequestProperty("X-TC-Timestamp", String.valueOf(timestampSec));
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        try (java.io.OutputStream os = conn.getOutputStream()) {
            os.write(requestBody.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        log.info("腾讯云短信 API 调用: phones={}, template={}, status={}", phones, templateId, responseCode);
        if (responseCode != 200) {
            throw new BusinessException(ErrorCode.NOTIFICATION_SEND_FAILED, "腾讯云短信API返回: " + responseCode);
        }
    }

    /**
     * 通用 HTTP 短信网关
     */
    private void sendGeneric(List<String> phones, String templateId, String templateParam) throws Exception {
        String apiUrl = systemConfigService.getValueByKey("sms.generic.apiUrl");
        String apiKey = systemConfigService.getValueByKey("sms.generic.apiKey");

        if (apiUrl == null || apiUrl.isEmpty()) {
            log.warn("通用短信网关 URL 未配置，跳过发送");
            return;
        }

        String phoneStr = String.join(",", phones);
        String body = String.format(
            "{\"apiKey\":\"%s\",\"phones\":\"%s\",\"templateId\":\"%s\",\"params\":%s}",
            apiKey != null ? apiKey : "", phoneStr, templateId != null ? templateId : "", templateParam);

        sendHttpPost(apiUrl, body);
    }

    private String buildTemplateParams(List<Map<String, String>> templateParams, Map<String, String> context) {
        if (templateParams == null || templateParams.isEmpty()) return "{}";
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map<String, String> param : templateParams) {
            String key = param.get("key");
            String value = param.get("value");
            if (key == null || key.isEmpty()) continue;
            // 变量替换
            if (value != null && context != null) {
                for (Map.Entry<String, String> entry : context.entrySet()) {
                    value = value.replace("${" + entry.getKey() + "}", entry.getValue() != null ? entry.getValue() : "");
                }
            }
            if (!first) sb.append(",");
            sb.append("\"").append(escapeJson(key)).append("\":\"").append(escapeJson(value)).append("\"");
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    private void sendHttpPost(String apiUrl, String jsonBody) throws Exception {
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new BusinessException(ErrorCode.NOTIFICATION_SEND_FAILED, "短信 API 返回非200状态码: " + responseCode);
        }
    }

    private void sendHttpGet(String apiUrl) throws Exception {
        java.net.URL url = new java.net.URL(apiUrl);
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new BusinessException(ErrorCode.NOTIFICATION_SEND_FAILED, "短信 API 返回非200状态码: " + responseCode);
        }
    }

    private String percentEncode(String value) throws Exception {
        return value != null
            ? java.net.URLEncoder.encode(value, "UTF-8")
                .replace("+", "%20")
                .replace("*", "%2A")
                .replace("%7E", "~")
            : "";
    }

    private String sha256Hex(String data) throws Exception {
        java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(data.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash);
    }

    private byte[] hmacSha256(byte[] key, String data) throws Exception {
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        mac.init(new javax.crypto.spec.SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r");
    }
    
    private String replaceVariables(String template, Map<String, String> context) {
        if (template == null || context == null) return template;
        for (Map.Entry<String, String> entry : context.entrySet()) {
            template = template.replace("${" + entry.getKey() + "}", entry.getValue() != null ? entry.getValue() : "");
        }
        return template;
    }
}
