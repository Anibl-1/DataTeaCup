package com.dataplatform.data.service.masking.strategy;

import com.dataplatform.data.service.masking.MaskingStrategy;
import com.dataplatform.data.service.masking.MaskingStrategyType;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Set;

/**
 * 哈希脱敏策略
 * 使用不可逆加密算法处理敏感数据
 * 
 * 示例：
 * - "13812345678" -> "a1b2c3d4e5f6..." (SHA-256)
 * - "张三" -> "abc123..." (MD5)
 * 
 * 配置参数：
 * - algorithm: 哈希算法，支持 "SHA-256"（默认）和 "MD5"
 * - truncateLength: 截断长度，可选，null表示返回完整哈希值
 * 
 * **Validates: Requirements 5.4**
 * 
 * @author dataplatform
 * @see MaskingStrategy
 * @see MaskingStrategyType#HASH
 */
@Component
public class HashStrategy implements MaskingStrategy {
    
    /**
     * 默认哈希算法
     */
    private static final String DEFAULT_ALGORITHM = "SHA-256";
    
    /**
     * 支持的哈希算法集合
     */
    private static final Set<String> SUPPORTED_ALGORITHMS = Set.of("SHA-256", "MD5");
    
    @Override
    public Object mask(Object value, Map<String, Object> config) {
        if (value == null) {
            return null;
        }
        
        String str = value.toString();
        if (str.isEmpty()) {
            return str;
        }
        
        String algorithm = getConfigString(config, "algorithm", DEFAULT_ALGORITHM);
        Integer truncateLength = getConfigInteger(config, "truncateLength", null);
        
        return applyHash(str, algorithm, truncateLength);
    }
    
    /**
     * 应用哈希处理
     * 
     * @param str 原始字符串
     * @param algorithm 哈希算法
     * @param truncateLength 截断长度，null表示不截断
     * @return 哈希后的十六进制字符串
     */
    private String applyHash(String str, String algorithm, Integer truncateLength) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] hashBytes = digest.digest(str.getBytes(StandardCharsets.UTF_8));
            String hexHash = bytesToHex(hashBytes);
            
            // 如果指定了截断长度，则截断结果
            if (truncateLength != null && truncateLength > 0 && truncateLength < hexHash.length()) {
                return hexHash.substring(0, truncateLength);
            }
            
            return hexHash;
        } catch (NoSuchAlgorithmException e) {
            // 如果算法不支持，回退到默认算法
            if (!DEFAULT_ALGORITHM.equals(algorithm)) {
                return applyHash(str, DEFAULT_ALGORITHM, truncateLength);
            }
            // 这种情况不应该发生，因为SHA-256是标准算法
            throw new RuntimeException("Hash algorithm not available: " + algorithm, e);
        }
    }
    
    /**
     * 将字节数组转换为十六进制字符串
     * 
     * @param bytes 字节数组
     * @return 十六进制字符串（小写）
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
    
    @Override
    public String getStrategyType() {
        return MaskingStrategyType.HASH.name();
    }
    
    @Override
    public boolean validateConfig(Map<String, Object> config) {
        if (config == null) {
            return true; // 使用默认配置
        }
        
        // 验证 algorithm
        if (config.containsKey("algorithm")) {
            Object algorithm = config.get("algorithm");
            if (!(algorithm instanceof String)) {
                return false;
            }
            String algoStr = ((String) algorithm).toUpperCase();
            if (!SUPPORTED_ALGORITHMS.contains(algoStr)) {
                return false;
            }
        }
        
        // 验证 truncateLength
        if (config.containsKey("truncateLength")) {
            Object truncateLength = config.get("truncateLength");
            if (truncateLength != null) {
                if (!(truncateLength instanceof Number) && !(truncateLength instanceof String)) {
                    return false;
                }
                int length = parseInteger(truncateLength, -1);
                if (length < 0) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    @Override
    public Map<String, Object> getDefaultConfig() {
        return Map.of(
            "algorithm", DEFAULT_ALGORITHM
        );
    }
    
    /**
     * 从配置中获取字符串值
     * 
     * @param config 配置Map
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值或默认值
     */
    private String getConfigString(Map<String, Object> config, String key, String defaultValue) {
        if (config == null || !config.containsKey(key)) {
            return defaultValue;
        }
        Object value = config.get(key);
        if (value instanceof String) {
            return (String) value;
        }
        if (value != null) {
            return value.toString();
        }
        return defaultValue;
    }
    
    /**
     * 从配置中获取整数值（可为null）
     * 
     * @param config 配置Map
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值或默认值
     */
    private Integer getConfigInteger(Map<String, Object> config, String key, Integer defaultValue) {
        if (config == null || !config.containsKey(key)) {
            return defaultValue;
        }
        Object value = config.get(key);
        if (value == null) {
            return defaultValue;
        }
        return parseInteger(value, defaultValue);
    }
    
    /**
     * 解析整数值
     * 
     * @param value 值对象
     * @param defaultValue 默认值
     * @return 解析后的整数或默认值
     */
    private Integer parseInteger(Object value, Integer defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
}
