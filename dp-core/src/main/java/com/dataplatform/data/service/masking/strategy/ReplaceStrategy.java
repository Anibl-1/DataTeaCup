package com.dataplatform.data.service.masking.strategy;

import com.dataplatform.data.service.masking.MaskingStrategy;
import com.dataplatform.data.service.masking.MaskingStrategyType;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 替换脱敏策略
 * 将敏感数据替换为固定值
 * 
 * 示例：
 * - "13812345678" -> "[HIDDEN]" (replacement="[HIDDEN]")
 * - "张三丰" -> "***" (replacement="***")
 * - "test@example.com" -> "[REDACTED]" (replacement="[REDACTED]")
 * 
 * 配置参数：
 * - replacement: 替换的固定值，默认"[HIDDEN]"
 * 
 * **Validates: Requirements 5.5**
 * 
 * @author dataplatform
 * @see MaskingStrategy
 * @see MaskingStrategyType#REPLACE
 */
@Component
public class ReplaceStrategy implements MaskingStrategy {
    
    /**
     * 默认替换值
     */
    private static final String DEFAULT_REPLACEMENT = "[HIDDEN]";
    
    @Override
    public Object mask(Object value, Map<String, Object> config) {
        if (value == null) {
            return null;
        }
        
        String str = value.toString();
        if (str.isEmpty()) {
            return str;
        }
        
        String replacement = getConfigString(config, "replacement", DEFAULT_REPLACEMENT);
        
        return replacement;
    }
    
    @Override
    public String getStrategyType() {
        return MaskingStrategyType.REPLACE.name();
    }
    
    @Override
    public boolean validateConfig(Map<String, Object> config) {
        if (config == null) {
            return true; // 使用默认配置
        }
        
        // 验证 replacement（必须是字符串或可转换为字符串的值）
        if (config.containsKey("replacement")) {
            Object replacement = config.get("replacement");
            // null 值不允许作为替换值
            if (replacement == null) {
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public Map<String, Object> getDefaultConfig() {
        return Map.of(
            "replacement", DEFAULT_REPLACEMENT
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
}
