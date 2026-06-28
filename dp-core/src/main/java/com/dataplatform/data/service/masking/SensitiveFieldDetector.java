package com.dataplatform.data.service.masking;

import java.util.List;
import java.util.Map;

/**
 * 敏感字段检测器接口
 * 负责自动识别数据中的敏感字段类型
 * 
 * 检测方式：
 * 1. 正则表达式匹配：手机号、身份证、银行卡、邮箱、IP地址
 * 2. 关键词匹配：姓名、地址
 * 3. 字段名推断：基于字段名称推断敏感类型
 * 4. 采样分析：分析样本数据特征推断类型
 * 
 * @author dataplatform
 */
public interface SensitiveFieldDetector {
    
    /**
     * 检测表中的敏感字段
     * 
     * @param tableName 表名
     * @param columns 列名列表
     * @return 敏感字段信息映射（字段名 -> 敏感类型）
     */
    Map<String, SensitiveFieldType> detectSensitiveFields(String tableName, List<String> columns);
    
    /**
     * 检测单个字段的敏感类型
     * 基于字段名和样本值进行综合判断
     * 
     * @param fieldName 字段名
     * @param sampleValues 样本值列表（建议100条以内）
     * @return 检测到的敏感类型，未识别返回null
     */
    SensitiveFieldType detectFieldType(String fieldName, List<Object> sampleValues);
    
    /**
     * 基于字段名推断敏感类型
     * 
     * @param fieldName 字段名
     * @return 推断的敏感类型，未识别返回null
     */
    SensitiveFieldType inferTypeByFieldName(String fieldName);
    
    /**
     * 基于值内容检测敏感类型
     * 
     * @param value 待检测的值
     * @return 检测到的敏感类型，未识别返回null
     */
    SensitiveFieldType detectTypeByValue(Object value);
    
    /**
     * 批量检测数据中的敏感字段
     * 
     * @param data 数据列表
     * @param fieldNames 字段名列表
     * @return 敏感字段检测结果
     */
    List<SensitiveFieldInfo> detectSensitiveFields(List<Map<String, Object>> data, List<String> fieldNames);
}
