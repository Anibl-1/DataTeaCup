package com.dataplatform.data.service.masking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 敏感字段信息
 * 用于描述检测到的敏感字段详情
 * 
 * @author dataplatform
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensitiveFieldInfo {
    
    /**
     * 字段名
     */
    private String fieldName;
    
    /**
     * 敏感类型
     */
    private SensitiveFieldType sensitiveType;
    
    /**
     * 检测置信度（0-1）
     */
    private Double confidence;
    
    /**
     * 检测方式
     */
    private DetectionMethod detectionMethod;
    
    /**
     * 样本匹配数量
     */
    private Integer matchedSampleCount;
    
    /**
     * 总样本数量
     */
    private Integer totalSampleCount;
    
    /**
     * 检测方式枚举
     */
    public enum DetectionMethod {
        /**
         * 正则表达式匹配
         */
        REGEX_MATCH,
        
        /**
         * 字段名推断
         */
        FIELD_NAME_INFERENCE,
        
        /**
         * 采样分析
         */
        SAMPLE_ANALYSIS,
        
        /**
         * 关键词匹配
         */
        KEYWORD_MATCH,
        
        /**
         * 综合判断
         */
        COMBINED
    }
}
