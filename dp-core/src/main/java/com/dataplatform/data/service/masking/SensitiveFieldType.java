package com.dataplatform.data.service.masking;

import lombok.Getter;

/**
 * 敏感字段类型枚举
 * 定义系统支持识别的敏感数据类型及其匹配规则
 * 
 * @author dataplatform
 */
@Getter
public enum SensitiveFieldType {
    
    /**
     * 手机号（11位数字，1开头）
     */
    PHONE("手机号", "^1[3-9]\\d{9}$"),
    
    /**
     * 身份证号（15位或18位）
     */
    ID_CARD("身份证", "^(\\d{15}|\\d{17}[\\dXx])$"),
    
    /**
     * 银行卡号（16-19位数字）
     */
    BANK_CARD("银行卡", "^\\d{16,19}$"),
    
    /**
     * 邮箱地址
     */
    EMAIL("邮箱", "^[\\w.+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$"),
    
    /**
     * IPv4地址
     */
    IP_ADDRESS("IP地址", "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$"),
    
    /**
     * 中文姓名（2-4个汉字）
     */
    NAME("姓名", "^[\\u4e00-\\u9fa5]{2,4}$"),
    
    /**
     * 地址（包含省、市、区、县、街道、路等关键词）
     */
    ADDRESS("地址", ".*(省|市|区|县|街道|路|镇|乡|村|号|栋|单元|室).*"),
    
    /**
     * 自定义类型
     */
    CUSTOM("自定义", null);
    
    /**
     * 类型标签（中文名称）
     */
    private final String label;
    
    /**
     * 匹配正则表达式
     */
    private final String pattern;
    
    SensitiveFieldType(String label, String pattern) {
        this.label = label;
        this.pattern = pattern;
    }
    
    /**
     * 判断是否有匹配模式
     * 
     * @return 是否有匹配模式
     */
    public boolean hasPattern() {
        return pattern != null && !pattern.isEmpty();
    }
}
