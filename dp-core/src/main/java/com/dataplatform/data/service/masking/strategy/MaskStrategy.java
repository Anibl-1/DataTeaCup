package com.dataplatform.data.service.masking.strategy;

import com.dataplatform.data.service.masking.MaskingStrategy;
import com.dataplatform.data.service.masking.MaskingStrategyType;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 掩码脱敏策略
 * 将敏感数据中间部分替换为掩码字符
 * 
 * 示例：
 * - "13812345678" -> "138****5678" (startIndex=3, endIndex=-4, maskChar='*')
 * - "张三丰" -> "张*丰" (startIndex=1, endIndex=-1, maskChar='*')
 * 
 * 配置参数：
 * - startIndex: 掩码起始位置（从0开始），默认3
 * - endIndex: 掩码结束位置（负数表示从末尾倒数），默认-4
 * - maskChar: 掩码字符，默认'*'
 * 
 * @author dataplatform
 * @see MaskingStrategy
 * @see MaskingStrategyType#MASK
 */
@Component
public class MaskStrategy implements MaskingStrategy {
    
    /**
     * 默认掩码起始位置
     */
    private static final int DEFAULT_START_INDEX = 3;
    
    /**
     * 默认掩码结束位置（从末尾倒数4位）
     */
    private static final int DEFAULT_END_INDEX = -4;
    
    /**
     * 默认掩码字符
     */
    private static final char DEFAULT_MASK_CHAR = '*';
    
    @Override
    public Object mask(Object value, Map<String, Object> config) {
        if (value == null) {
            return null;
        }
        
        String str = value.toString();
        if (str.isEmpty()) {
            return str;
        }
        
        int startIndex = getConfigInt(config, "startIndex", DEFAULT_START_INDEX);
        int endIndex = getConfigInt(config, "endIndex", DEFAULT_END_INDEX);
        char maskChar = getConfigChar(config, "maskChar", DEFAULT_MASK_CHAR);
        
        return applyMask(str, startIndex, endIndex, maskChar);
    }
    
    /**
     * 应用掩码处理
     * 
     * @param str 原始字符串
     * @param startIndex 掩码起始位置
     * @param endIndex 掩码结束位置（负数表示从末尾倒数）
     * @param maskChar 掩码字符
     * @return 掩码后的字符串
     */
    private String applyMask(String str, int startIndex, int endIndex, char maskChar) {
        int length = str.length();
        
        // 处理负数索引（从末尾倒数）
        int actualEndIndex = endIndex < 0 ? length + endIndex : endIndex;
        
        // 边界检查和调整
        int actualStartIndex = Math.max(0, Math.min(startIndex, length));
        actualEndIndex = Math.max(0, Math.min(actualEndIndex, length));
        
        // 如果起始位置大于等于结束位置，返回原字符串
        if (actualStartIndex >= actualEndIndex) {
            return str;
        }
        
        // 构建掩码字符串
        StringBuilder result = new StringBuilder(length);
        
        // 添加前缀（未掩码部分）
        result.append(str, 0, actualStartIndex);
        
        // 添加掩码部分
        int maskLength = actualEndIndex - actualStartIndex;
        for (int i = 0; i < maskLength; i++) {
            result.append(maskChar);
        }
        
        // 添加后缀（未掩码部分）
        result.append(str.substring(actualEndIndex));
        
        return result.toString();
    }
    
    @Override
    public String getStrategyType() {
        return MaskingStrategyType.MASK.name();
    }
    
    @Override
    public boolean validateConfig(Map<String, Object> config) {
        if (config == null) {
            return true; // 使用默认配置
        }
        
        // 验证 startIndex
        if (config.containsKey("startIndex")) {
            Object startIndex = config.get("startIndex");
            if (!(startIndex instanceof Number)) {
                return false;
            }
            if (((Number) startIndex).intValue() < 0) {
                return false;
            }
        }
        
        // 验证 maskChar
        if (config.containsKey("maskChar")) {
            Object maskChar = config.get("maskChar");
            if (maskChar instanceof String) {
                if (((String) maskChar).length() != 1) {
                    return false;
                }
            } else if (!(maskChar instanceof Character)) {
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public Map<String, Object> getDefaultConfig() {
        return Map.of(
            "startIndex", DEFAULT_START_INDEX,
            "endIndex", DEFAULT_END_INDEX,
            "maskChar", String.valueOf(DEFAULT_MASK_CHAR)
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
     * 从配置中获取字符值
     * 
     * @param config 配置Map
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值或默认值
     */
    private char getConfigChar(Map<String, Object> config, String key, char defaultValue) {
        if (config == null || !config.containsKey(key)) {
            return defaultValue;
        }
        Object value = config.get(key);
        if (value instanceof Character) {
            return (Character) value;
        }
        if (value instanceof String && !((String) value).isEmpty()) {
            return ((String) value).charAt(0);
        }
        return defaultValue;
    }
}
