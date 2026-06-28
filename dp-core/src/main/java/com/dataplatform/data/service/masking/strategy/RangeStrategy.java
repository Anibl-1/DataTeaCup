package com.dataplatform.data.service.masking.strategy;

import com.dataplatform.data.service.masking.MaskingStrategy;
import com.dataplatform.data.service.masking.MaskingStrategyType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 区间化脱敏策略
 * 将数值转换为区间范围表示
 * 
 * 示例：
 * - 35000 -> "30000-40000" (rangeSize=10000)
 * - 1234 -> "1000-2000" (rangeSize=1000)
 * - 55 -> "50-60" (rangeSize=10)
 * 
 * 配置参数：
 * - rangeSize: 区间大小，默认10000
 * - format: 输出格式模板，默认"{min}-{max}"
 * 
 * **Validates: Requirements 5.6**
 * 
 * @author dataplatform
 * @see MaskingStrategy
 * @see MaskingStrategyType#RANGE
 */
@Component
public class RangeStrategy implements MaskingStrategy {
    
    /**
     * 默认区间大小
     */
    private static final long DEFAULT_RANGE_SIZE = 10000L;
    
    /**
     * 默认输出格式
     */
    private static final String DEFAULT_FORMAT = "{min}-{max}";
    
    @Override
    public Object mask(Object value, Map<String, Object> config) {
        if (value == null) {
            return null;
        }
        
        // 尝试解析为数值
        Long numericValue = parseNumericValue(value);
        if (numericValue == null) {
            // 非数值类型，返回原值
            return value;
        }
        
        long rangeSize = getConfigLong(config, "rangeSize", DEFAULT_RANGE_SIZE);
        String format = getConfigString(config, "format", DEFAULT_FORMAT);
        
        return applyRange(numericValue, rangeSize, format);
    }
    
    /**
     * 应用区间化处理
     * 
     * @param value 原始数值
     * @param rangeSize 区间大小
     * @param format 输出格式
     * @return 区间化后的字符串
     */
    private String applyRange(long value, long rangeSize, String format) {
        // 确保区间大小至少为1
        long actualRangeSize = Math.max(1, rangeSize);
        
        // 计算区间的最小值和最大值
        long min = (value / actualRangeSize) * actualRangeSize;
        long max = min + actualRangeSize;
        
        // 应用格式模板
        return format
            .replace("{min}", String.valueOf(min))
            .replace("{max}", String.valueOf(max));
    }
    
    /**
     * 尝试将值解析为Long类型
     * 
     * @param value 原始值
     * @return 解析后的Long值，无法解析返回null
     */
    private Long parseNumericValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        
        if (value instanceof String) {
            String str = ((String) value).trim();
            if (str.isEmpty()) {
                return null;
            }
            
            try {
                // 尝试解析为BigDecimal以支持各种数字格式
                BigDecimal decimal = new BigDecimal(str);
                return decimal.longValue();
            } catch (NumberFormatException e) {
                return null;
            }
        }
        
        return null;
    }
    
    @Override
    public String getStrategyType() {
        return MaskingStrategyType.RANGE.name();
    }
    
    @Override
    public boolean validateConfig(Map<String, Object> config) {
        if (config == null) {
            return true; // 使用默认配置
        }
        
        // 验证 rangeSize
        if (config.containsKey("rangeSize")) {
            Object rangeSize = config.get("rangeSize");
            if (rangeSize != null) {
                Long parsed = parseLongValue(rangeSize);
                if (parsed == null || parsed <= 0) {
                    return false;
                }
            }
        }
        
        // 验证 format
        if (config.containsKey("format")) {
            Object format = config.get("format");
            if (format != null && !(format instanceof String)) {
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public Map<String, Object> getDefaultConfig() {
        return Map.of(
            "rangeSize", DEFAULT_RANGE_SIZE,
            "format", DEFAULT_FORMAT
        );
    }
    
    /**
     * 从配置中获取长整型值
     * 
     * @param config 配置Map
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值或默认值
     */
    private long getConfigLong(Map<String, Object> config, String key, long defaultValue) {
        if (config == null || !config.containsKey(key)) {
            return defaultValue;
        }
        Object value = config.get(key);
        Long parsed = parseLongValue(value);
        return parsed != null ? parsed : defaultValue;
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
     * 解析长整型值
     * 
     * @param value 值对象
     * @return 解析后的Long值，无法解析返回null
     */
    private Long parseLongValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
