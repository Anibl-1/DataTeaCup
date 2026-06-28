package com.dataplatform.data.service.masking.strategy;

import com.dataplatform.data.service.masking.MaskingStrategy;
import com.dataplatform.data.service.masking.MaskingStrategyType;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 截断脱敏策略
 * 只显示数据的前N位，后面追加指定后缀
 * 
 * 示例：
 * - "张三丰" -> "张*" (keepLength=1, suffix="*")
 * - "13812345678" -> "138..." (keepLength=3, suffix="...")
 * - "test@example.com" -> "tes***" (keepLength=3, suffix="***")
 * 
 * 配置参数：
 * - keepLength: 保留的字符数（从开头），默认1
 * - suffix: 截断后追加的后缀，默认"*"
 * 
 * **Validates: Requirements 5.3**
 * 
 * @author dataplatform
 * @see MaskingStrategy
 * @see MaskingStrategyType#TRUNCATE
 */
@Component
public class TruncateStrategy implements MaskingStrategy {
    
    /**
     * 默认保留长度
     */
    private static final int DEFAULT_KEEP_LENGTH = 1;
    
    /**
     * 默认后缀
     */
    private static final String DEFAULT_SUFFIX = "*";
    
    @Override
    public Object mask(Object value, Map<String, Object> config) {
        if (value == null) {
            return null;
        }
        
        String str = value.toString();
        if (str.isEmpty()) {
            return str;
        }
        
        int keepLength = getConfigInt(config, "keepLength", DEFAULT_KEEP_LENGTH);
        String suffix = getConfigString(config, "suffix", DEFAULT_SUFFIX);
        
        return applyTruncate(str, keepLength, suffix);
    }
    
    /**
     * 应用截断处理
     * 
     * @param str 原始字符串
     * @param keepLength 保留的字符数
     * @param suffix 截断后追加的后缀
     * @return 截断后的字符串
     */
    private String applyTruncate(String str, int keepLength, String suffix) {
        int length = str.length();
        
        // 保留长度不能为负数
        int actualKeepLength = Math.max(0, keepLength);
        
        // 如果保留长度大于等于字符串长度，返回原字符串
        if (actualKeepLength >= length) {
            return str;
        }
        
        // 构建截断后的字符串
        return str.substring(0, actualKeepLength) + suffix;
    }
    
    @Override
    public String getStrategyType() {
        return MaskingStrategyType.TRUNCATE.name();
    }
    
    @Override
    public boolean validateConfig(Map<String, Object> config) {
        if (config == null) {
            return true; // 使用默认配置
        }
        
        // 验证 keepLength
        if (config.containsKey("keepLength")) {
            Object keepLength = config.get("keepLength");
            if (!(keepLength instanceof Number) && !(keepLength instanceof String)) {
                return false;
            }
            if (keepLength instanceof Number && ((Number) keepLength).intValue() < 0) {
                return false;
            }
            if (keepLength instanceof String) {
                try {
                    int parsed = Integer.parseInt((String) keepLength);
                    if (parsed < 0) {
                        return false;
                    }
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        }
        
        // 验证 suffix（必须是字符串）
        if (config.containsKey("suffix")) {
            Object suffix = config.get("suffix");
            if (!(suffix instanceof String)) {
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public Map<String, Object> getDefaultConfig() {
        return Map.of(
            "keepLength", DEFAULT_KEEP_LENGTH,
            "suffix", DEFAULT_SUFFIX
        );
    }
    
    /**
     * 从配置中获取整数值
     * 
     * @param config 配置Map
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值或默认值
     */
    private int getConfigInt(Map<String, Object> config, String key, int defaultValue) {
        if (config == null || !config.containsKey(key)) {
            return defaultValue;
        }
        Object value = config.get(key);
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
}
