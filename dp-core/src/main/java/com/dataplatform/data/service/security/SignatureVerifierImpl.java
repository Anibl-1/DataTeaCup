package com.dataplatform.data.service.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 签名验证器实现类
 * 使用HMAC-SHA256算法实现API请求签名验证
 * 
 * 需求 3.4: THE Security_Engine SHALL 实现API请求签名验证，防止请求被篡改
 *
 * 签名算法：
 * 1. 将timestamp、nonce、请求参数按字典序排序
 * 2. 拼接成字符串：timestamp=xxx&nonce=xxx&param1=value1&param2=value2...
 * 3. 使用HMAC-SHA256算法和密钥计算签名
 * 4. 将签名转换为十六进制字符串
 *
 * @author dataplatform
 */
@Slf4j
@Service
public class SignatureVerifierImpl implements SignatureVerifier {

    /** HMAC-SHA256算法名称 */
    private static final String HMAC_SHA256 = "HmacSHA256";
    
    /** 签名请求头名称 */
    private static final String HEADER_SIGNATURE = "X-Signature";
    
    /** 时间戳请求头名称 */
    private static final String HEADER_TIMESTAMP = "X-Timestamp";
    
    /** 随机数请求头名称 */
    private static final String HEADER_NONCE = "X-Nonce";
    
    /** 随机数长度 */
    private static final int NONCE_LENGTH = 32;
    
    /** 十六进制字符 */
    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    /** 安全随机数生成器 */
    private final SecureRandom secureRandom = new SecureRandom();

    /** 签名密钥 */
    @Value("${security.signature.secret-key:default-secret-key-please-change}")
    private String defaultSecretKey;

    /** 签名过期时间（秒），默认5分钟 */
    @Value("${security.signature.expire-seconds:300}")
    private long signatureExpireSeconds;

    @Override
    public SignatureVerificationResult verifyRequest(HttpServletRequest request) {
        // 1. 获取签名相关请求头
        String signature = request.getHeader(HEADER_SIGNATURE);
        String timestampStr = request.getHeader(HEADER_TIMESTAMP);
        String nonce = request.getHeader(HEADER_NONCE);

        // 2. 验证必要参数是否存在
        if (!StringUtils.hasText(signature)) {
            log.warn("签名验证失败: 缺少签名参数 X-Signature");
            return SignatureVerificationResult.missingParameter(HEADER_SIGNATURE);
        }
        if (!StringUtils.hasText(timestampStr)) {
            log.warn("签名验证失败: 缺少时间戳参数 X-Timestamp");
            return SignatureVerificationResult.missingParameter(HEADER_TIMESTAMP);
        }
        if (!StringUtils.hasText(nonce)) {
            log.warn("签名验证失败: 缺少随机数参数 X-Nonce");
            return SignatureVerificationResult.missingParameter(HEADER_NONCE);
        }

        // 3. 解析时间戳
        long timestamp;
        try {
            timestamp = Long.parseLong(timestampStr);
        } catch (NumberFormatException e) {
            log.warn("签名验证失败: 无效的时间戳格式 timestamp={}", timestampStr);
            return SignatureVerificationResult.invalidTimestamp();
        }

        // 4. 构建请求参数字符串
        String params = buildParamsString(request);

        // 5. 验证签名
        return verifySignature(timestamp, nonce, params, signature, defaultSecretKey);
    }

    @Override
    public SignatureVerificationResult verifySignature(long timestamp, String nonce, 
                                                        String params, String signature, 
                                                        String secretKey) {
        // 1. 验证时间戳是否过期
        if (!isTimestampValid(timestamp)) {
            log.warn("签名验证失败: 时间戳已过期 timestamp={}", timestamp);
            return SignatureVerificationResult.timestampExpired();
        }

        // 2. 计算预期签名
        String expectedSignature;
        try {
            expectedSignature = generateSignature(timestamp, nonce, params, secretKey);
        } catch (Exception e) {
            log.error("签名计算错误", e);
            return SignatureVerificationResult.failure(
                SignatureErrorCode.SIGNATURE_COMPUTATION_ERROR, 
                "签名计算错误: " + e.getMessage()
            );
        }

        // 3. 比较签名（使用时间恒定比较防止时序攻击）
        if (!constantTimeEquals(expectedSignature, signature)) {
            log.warn("签名验证失败: 签名不匹配");
            return SignatureVerificationResult.signatureMismatch();
        }

        log.debug("签名验证成功");
        return SignatureVerificationResult.success();
    }

    @Override
    public String generateSignature(long timestamp, String nonce, String params, String secretKey) {
        // 1. 构建待签名字符串
        String dataToSign = buildSignatureData(timestamp, nonce, params);
        
        // 2. 计算HMAC-SHA256签名
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                secretKey.getBytes(StandardCharsets.UTF_8), 
                HMAC_SHA256
            );
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(dataToSign.getBytes(StandardCharsets.UTF_8));
            
            // 3. 转换为十六进制字符串
            return bytesToHex(hmacBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("签名计算失败", e);
        }
    }

    @Override
    public String generateNonce() {
        byte[] bytes = new byte[NONCE_LENGTH / 2];
        secureRandom.nextBytes(bytes);
        return bytesToHex(bytes);
    }

    @Override
    public boolean isTimestampValid(long timestamp) {
        long currentTime = System.currentTimeMillis();
        long diff = Math.abs(currentTime - timestamp);
        // 转换为毫秒进行比较
        return diff <= signatureExpireSeconds * 1000;
    }

    @Override
    public long getSignatureExpireSeconds() {
        return signatureExpireSeconds;
    }

    /**
     * 构建请求参数字符串
     * 将请求参数按字典序排序后拼接
     *
     * @param request HTTP请求
     * @return 排序后的参数字符串
     */
    private String buildParamsString(HttpServletRequest request) {
        Map<String, String[]> parameterMap = request.getParameterMap();
        if (parameterMap == null || parameterMap.isEmpty()) {
            return "";
        }

        // 按参数名字典序排序
        return parameterMap.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .flatMap(entry -> {
                String key = entry.getKey();
                String[] values = entry.getValue();
                // 对于多值参数，每个值都生成一个键值对
                return Arrays.stream(values)
                    .sorted()
                    .map(value -> key + "=" + (value != null ? value : ""));
            })
            .collect(Collectors.joining("&"));
    }

    /**
     * 构建待签名数据
     * 格式：timestamp=xxx&nonce=xxx&params
     *
     * @param timestamp 时间戳
     * @param nonce     随机数
     * @param params    请求参数字符串
     * @return 待签名数据
     */
    private String buildSignatureData(long timestamp, String nonce, String params) {
        StringBuilder sb = new StringBuilder();
        sb.append("timestamp=").append(timestamp);
        sb.append("&nonce=").append(nonce != null ? nonce : "");
        if (StringUtils.hasText(params)) {
            sb.append("&").append(params);
        }
        return sb.toString();
    }

    /**
     * 字节数组转十六进制字符串
     *
     * @param bytes 字节数组
     * @return 十六进制字符串
     */
    private String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            hexChars[i * 2] = HEX_CHARS[v >>> 4];
            hexChars[i * 2 + 1] = HEX_CHARS[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * 时间恒定的字符串比较
     * 防止时序攻击
     *
     * @param a 字符串a
     * @param b 字符串b
     * @return 是否相等
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return a == b;
        }
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    /**
     * 设置默认密钥（用于测试）
     */
    void setDefaultSecretKey(String secretKey) {
        this.defaultSecretKey = secretKey;
    }

    /**
     * 设置签名过期时间（用于测试）
     */
    void setSignatureExpireSeconds(long seconds) {
        this.signatureExpireSeconds = seconds;
    }
}
