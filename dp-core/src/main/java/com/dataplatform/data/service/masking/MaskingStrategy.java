package com.dataplatform.data.service.masking;

import java.util.Map;

/**
 * 脱敏策略接口
 * 定义不同脱敏方式的统一接口，支持策略模式实现
 * 
 * 支持的策略类型：
 * - MASK: 掩码（如：138****8888）
 * - TRUNCATE: 截断（如：张*）
 * - HASH: 哈希（不可逆加密）
 * - REPLACE: 替换（固定值替换）
 * - RANGE: 区间化（如：10000-50000）
 * - REGEX: 自定义正则替换
 * 
 * @author dataplatform
 */
public interface MaskingStrategy {
    
    /**
     * 对值应用脱敏处理
     * 
     * @param value 原始值
     * @param config 脱敏配置参数
     * @return 脱敏后的值
     */
    Object mask(Object value, Map<String, Object> config);
    
    /**
     * 获取策略类型名称
     * 
     * @return 策略类型标识
     */
    String getStrategyType();
    
    /**
     * 验证配置参数是否有效
     * 
     * @param config 脱敏配置参数
     * @return 配置是否有效
     */
    default boolean validateConfig(Map<String, Object> config) {
        return true;
    }
    
    /**
     * 获取策略的默认配置
     * 
     * @return 默认配置参数
     */
    default Map<String, Object> getDefaultConfig() {
        return Map.of();
    }
}
