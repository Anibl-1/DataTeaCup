package com.dataplatform.data.service.masking;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 数据脱敏引擎接口
 * 负责协调敏感字段检测和脱敏策略执行
 * 
 * 主要功能：
 * 1. 识别数据中的敏感字段
 * 2. 应用脱敏规则处理数据
 * 3. 支持流式处理大数据量
 * 4. 支持基于角色的差异化脱敏
 * 
 * @author dataplatform
 */
public interface MaskingEngine {
    
    /**
     * 对数据应用脱敏规则
     * 
     * @param data 原始数据列表
     * @param rules 脱敏规则列表
     * @return 脱敏后的数据列表
     */
    List<Map<String, Object>> maskData(List<Map<String, Object>> data, List<MaskingRule> rules);
    
    /**
     * 对单个字段值应用脱敏
     * 
     * @param value 原始值
     * @param strategy 脱敏策略
     * @return 脱敏后的值
     */
    Object maskField(Object value, MaskingStrategy strategy);
    
    /**
     * 对单个字段值应用脱敏（带配置）
     * 
     * @param value 原始值
     * @param strategy 脱敏策略
     * @param config 脱敏配置
     * @return 脱敏后的值
     */
    Object maskField(Object value, MaskingStrategy strategy, Map<String, Object> config);
    
    /**
     * 流式脱敏处理
     * 适用于大数据量场景，避免内存溢出
     * 
     * @param dataStream 数据流
     * @param rules 脱敏规则列表
     * @return 脱敏后的数据流
     */
    Stream<Map<String, Object>> maskDataStream(Stream<Map<String, Object>> dataStream, List<MaskingRule> rules);
    
    /**
     * 基于用户角色应用脱敏
     * 
     * @param data 原始数据列表
     * @param rules 脱敏规则列表
     * @param userId 用户ID
     * @param roleIds 用户角色ID列表
     * @return 脱敏后的数据列表
     */
    List<Map<String, Object>> maskDataByRole(List<Map<String, Object>> data, List<MaskingRule> rules, 
                                              Long userId, List<Long> roleIds);
    
    /**
     * 识别数据中的敏感字段
     * 
     * @param data 数据列表
     * @param fieldNames 字段名列表
     * @return 敏感字段信息列表
     */
    List<SensitiveFieldInfo> detectSensitiveFields(List<Map<String, Object>> data, List<String> fieldNames);
    
    /**
     * 预览脱敏效果
     * 
     * @param sampleValue 样本值
     * @param rule 脱敏规则
     * @return 脱敏后的值
     */
    Object previewMasking(Object sampleValue, MaskingRule rule);
    
    /**
     * 验证脱敏规则配置
     * 
     * @param rule 脱敏规则
     * @return 验证结果，null表示验证通过，否则返回错误信息
     */
    String validateRule(MaskingRule rule);

    /**
     * 验证一组脱敏规则配置
     * 返回结构化的验证结果，包含所有检测到的错误和警告
     *
     * @param rules 脱敏规则列表
     * @return 验证结果
     */
    MaskingConfigValidationResult validateRules(List<MaskingRule> rules);

    
    /**
     * 获取指定类型的脱敏策略
     * 
     * @param strategyType 策略类型
     * @return 脱敏策略实例
     */
    MaskingStrategy getStrategy(String strategyType);
    
    /**
     * 获取所有可用的脱敏策略类型
     * 
     * @return 策略类型列表
     */
    List<String> getAvailableStrategyTypes();
}
