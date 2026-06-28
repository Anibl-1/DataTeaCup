package com.dataplatform.data.service.masking;

import lombok.Getter;

/**
 * 脱敏策略类型枚举
 * 定义系统支持的脱敏策略
 * 
 * @author dataplatform
 */
@Getter
public enum MaskingStrategyType {
    
    /**
     * 掩码策略
     * 将敏感数据中间部分替换为星号
     * 示例：138****8888
     */
    MASK("掩码", "将数据中间部分替换为掩码字符"),
    
    /**
     * 截断策略
     * 只显示数据的前N位
     * 示例：张*
     */
    TRUNCATE("截断", "只显示数据的前N位"),
    
    /**
     * 哈希策略
     * 使用不可逆加密算法处理数据
     * 示例：a1b2c3d4...
     */
    HASH("哈希", "使用不可逆加密算法处理"),
    
    /**
     * 替换策略
     * 将敏感数据替换为固定值
     * 示例：***
     */
    REPLACE("替换", "将数据替换为固定值"),
    
    /**
     * 区间化策略
     * 将数值转换为区间范围
     * 示例：10000-50000
     */
    RANGE("区间化", "将数值转换为区间范围"),
    
    /**
     * 正则替换策略
     * 使用自定义正则表达式进行替换
     */
    REGEX("正则替换", "使用自定义正则表达式替换");
    
    /**
     * 策略名称
     */
    private final String name;
    
    /**
     * 策略描述
     */
    private final String description;
    
    MaskingStrategyType(String name, String description) {
        this.name = name;
        this.description = description;
    }
    
    /**
     * 根据类型名称获取枚举值
     * 
     * @param typeName 类型名称
     * @return 枚举值，未找到返回null
     */
    public static MaskingStrategyType fromName(String typeName) {
        if (typeName == null) {
            return null;
        }
        for (MaskingStrategyType type : values()) {
            if (type.name().equalsIgnoreCase(typeName)) {
                return type;
            }
        }
        return null;
    }
}
