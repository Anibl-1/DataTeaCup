package com.dataplatform.data.service.masking.strategy;

import com.dataplatform.data.service.masking.MaskingStrategy;
import com.dataplatform.data.service.masking.MaskingStrategyType;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 正则替换脱敏策略
 * 使用自定义正则表达式进行模式匹配和替换
 * 
 * 示例：
 * - "13812345678" -> "138****5678" (pattern="(\d{3})\d{4}(\d{4})", replacement="$1****$2")
 * - "test@example.com" -> "t***@example.com" (pattern="^(.)[^@]*(@.*)", replacement="$1***$2")
 * - "192.168.1.100" -> "192.168.x.x" (pattern="\d+\.\d+$", replacement="x.x")
 * 
 * 配置参数：
 * - pattern: 正则表达式模式（必需）
 * - replacement: 替换字符串，支持分组引用如$1, $2（必需）
 * - flags: 正则表达式标志，如"i"表示忽略大小写（可选）
 * 
 * **Validates: Requirements 5.10**
 * 
 * @author dataplatform
 * @see MaskingStrategy
 * @see MaskingStrategyType#REGEX
 */
@Component
public class RegexStrategy implements MaskingStrategy {
    
    /**
     * 默认正则模式（匹配所有内容）
     */
    private static final String DEFAULT_PATTERN = ".*";
    
    /**
     * 默认替换字符串
     */
    private static final String DEFAULT_REPLACEMENT = "[MASKED]";
    
    @Override
    public Object mask(Object value, Map<String, Object> config) {
        if (value == null) {
            return null;
        }
        
        String str = value.toString();
        if (str.isEmpty()) {
            return str;
        }
        
        String patternStr = getConfigString(config, "pattern", DEFAULT_PATTERN);
        String replacement = getConfigString(config, "replacement", DEFAULT_REPLACEMENT);
        String flags = getConfigString(config, "flags", "");
        
        return applyRegex(str, patternStr, replacement, flags);
    }
    
    /**
     * 应用正则替换处理
     * 
     * @param str 原始字符串
     * @param patternStr 正则表达式模式
     * @param replacement 替换字符串
     * @param flags 正则表达式标志
     * @return 替换后的字符串
     */
    private String applyRegex(String str, String patternStr, String replacement, String flags) {
        try {
            int patternFlags = parseFlags(flags);
            Pattern pattern = Pattern.compile(patternStr, patternFlags);
            return pattern.matcher(str).replaceAll(replacement);
        } catch (PatternSyntaxException e) {
            // 正则表达式无效，返回原值
            return str;
        } catch (IllegalArgumentException e) {
            // 替换字符串无效（如无效的分组引用），返回原值
            return str;
        }
    }
    
    /**
     * 解析正则表达式标志字符串
     * 
     * @param flags 标志字符串，如"im"表示忽略大小写和多行模式
     * @return Pattern标志位组合
     */
    private int parseFlags(String flags) {
        if (flags == null || flags.isEmpty()) {
            return 0;
        }
        
        int result = 0;
        for (char c : flags.toLowerCase().toCharArray()) {
            switch (c) {
                case 'i':
                    result |= Pattern.CASE_INSENSITIVE;
                    break;
                case 'm':
                    result |= Pattern.MULTILINE;
                    break;
                case 's':
                    result |= Pattern.DOTALL;
                    break;
                case 'u':
                    result |= Pattern.UNICODE_CASE;
                    break;
                case 'x':
                    result |= Pattern.COMMENTS;
                    break;
                default:
                    // 忽略未知标志
                    break;
            }
        }
        return result;
    }
    
    @Override
    public String getStrategyType() {
        return MaskingStrategyType.REGEX.name();
    }
    
    @Override
    public boolean validateConfig(Map<String, Object> config) {
        if (config == null) {
            return true; // 使用默认配置
        }
        
        // 验证 pattern（必须是有效的正则表达式）
        if (config.containsKey("pattern")) {
            Object pattern = config.get("pattern");
            if (pattern == null) {
                return false;
            }
            if (!(pattern instanceof String)) {
                return false;
            }
            String patternStr = (String) pattern;
            if (patternStr.isEmpty()) {
                return false;
            }
            // 验证正则表达式语法
            try {
                Pattern.compile(patternStr);
            } catch (PatternSyntaxException e) {
                return false;
            }
        }
        
        // 验证 replacement（必须是字符串）
        if (config.containsKey("replacement")) {
            Object replacement = config.get("replacement");
            if (replacement != null && !(replacement instanceof String)) {
                return false;
            }
        }
        
        // 验证 flags（如果存在，必须是字符串）
        if (config.containsKey("flags")) {
            Object flags = config.get("flags");
            if (flags != null && !(flags instanceof String)) {
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public Map<String, Object> getDefaultConfig() {
        return Map.of(
            "pattern", DEFAULT_PATTERN,
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
