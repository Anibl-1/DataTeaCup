package com.dataplatform.data.service.template;

import com.dataplatform.data.dto.TemplateParameter;

import java.util.List;
import java.util.Map;

/**
 * 模板参数服务接口
 * 支持模板参数化配置（需求 11.3）
 * 
 * 主要功能：
 * 1. 解析SQL模板中的参数占位符
 * 2. 解析和验证参数配置
 * 3. 替换SQL模板中的参数值
 * 4. 验证参数值是否符合配置要求
 * 5. 处理参数默认值
 * 
 * @author dataplatform
 */
public interface TemplateParameterService {
    
    // ==================== 参数解析 ====================
    
    /**
     * 从SQL模板中解析参数占位符
     * 支持 ${paramName} 格式的占位符
     * 
     * @param sqlTemplate SQL模板
     * @return 参数名列表（按出现顺序，去重）
     */
    List<String> parseParameterPlaceholders(String sqlTemplate);
    
    /**
     * 解析参数配置JSON为参数对象列表
     * 
     * @param paramsConfigJson 参数配置JSON字符串
     * @return 参数配置列表
     */
    List<TemplateParameter> parseParameterConfig(String paramsConfigJson);
    
    /**
     * 将参数配置列表序列化为JSON字符串
     * 
     * @param parameters 参数配置列表
     * @return JSON字符串
     */
    String serializeParameterConfig(List<TemplateParameter> parameters);
    
    // ==================== 参数替换 ====================
    
    /**
     * 替换SQL模板中的参数占位符
     * 根据参数配置自动处理引号和类型转换
     * 
     * @param sqlTemplate SQL模板
     * @param params 参数值映射
     * @param parameterConfigs 参数配置列表（可选，用于类型感知替换）
     * @return 替换后的SQL
     */
    String replaceParameters(String sqlTemplate, Map<String, Object> params, 
                            List<TemplateParameter> parameterConfigs);
    
    /**
     * 替换SQL模板中的参数占位符（简化版本）
     * 
     * @param sqlTemplate SQL模板
     * @param params 参数值映射
     * @return 替换后的SQL
     */
    String replaceParameters(String sqlTemplate, Map<String, Object> params);
    
    /**
     * 格式化单个参数值为SQL安全的字符串
     * 
     * @param value 参数值
     * @param paramConfig 参数配置（可选）
     * @return 格式化后的SQL值字符串
     */
    String formatParameterValue(Object value, TemplateParameter paramConfig);
    
    // ==================== 参数验证 ====================
    
    /**
     * 验证参数值是否符合配置要求
     * 
     * @param params 参数值映射
     * @param parameterConfigs 参数配置列表
     * @return 验证错误列表，如果验证通过返回空列表
     */
    List<String> validateParameters(Map<String, Object> params, 
                                   List<TemplateParameter> parameterConfigs);
    
    /**
     * 验证单个参数值
     * 
     * @param paramName 参数名
     * @param value 参数值
     * @param paramConfig 参数配置
     * @return 验证错误列表
     */
    List<String> validateParameter(String paramName, Object value, TemplateParameter paramConfig);
    
    // ==================== 默认值处理 ====================
    
    /**
     * 应用参数默认值
     * 对于未提供值的参数，使用配置中的默认值填充
     * 
     * @param params 参数值映射（会被修改）
     * @param parameterConfigs 参数配置列表
     * @return 应用默认值后的参数映射
     */
    Map<String, Object> applyDefaultValues(Map<String, Object> params, 
                                          List<TemplateParameter> parameterConfigs);
    
    /**
     * 获取所有参数的默认值映射
     * 
     * @param parameterConfigs 参数配置列表
     * @return 默认值映射
     */
    Map<String, Object> getDefaultValues(List<TemplateParameter> parameterConfigs);
    
    // ==================== 参数配置合并 ====================
    
    /**
     * 合并SQL模板中的参数和配置中的参数
     * 确保所有SQL中使用的参数都有对应的配置
     * 
     * @param sqlTemplate SQL模板
     * @param existingConfigs 现有参数配置
     * @return 合并后的参数配置列表
     */
    List<TemplateParameter> mergeParameterConfigs(String sqlTemplate, 
                                                  List<TemplateParameter> existingConfigs);
    
    /**
     * 为SQL模板中的参数生成默认配置
     * 
     * @param paramName 参数名
     * @return 默认参数配置
     */
    TemplateParameter createDefaultParameterConfig(String paramName);
}
