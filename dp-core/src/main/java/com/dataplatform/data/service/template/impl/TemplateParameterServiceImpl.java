package com.dataplatform.data.service.template.impl;

import com.dataplatform.data.dto.TemplateParameter;
import com.dataplatform.data.service.template.TemplateParameterService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 模板参数服务实现
 * 支持模板参数化配置（需求 11.3）
 * 
 * @author dataplatform
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateParameterServiceImpl implements TemplateParameterService {
    
    private final ObjectMapper objectMapper;
    
    /** 参数占位符正则表达式：${paramName} */
    private static final Pattern PARAM_PATTERN = Pattern.compile("\\$\\{([a-zA-Z_][a-zA-Z0-9_]*)\\}");
    
    /** 日期格式 */
    private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    private static final String DEFAULT_DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    
    // ==================== 参数解析 ====================
    
    @Override
    public List<String> parseParameterPlaceholders(String sqlTemplate) {
        if (!StringUtils.hasText(sqlTemplate)) {
            return Collections.emptyList();
        }
        
        Set<String> params = new LinkedHashSet<>();
        Matcher matcher = PARAM_PATTERN.matcher(sqlTemplate);
        while (matcher.find()) {
            params.add(matcher.group(1));
        }
        return new ArrayList<>(params);
    }
    
    @Override
    public List<TemplateParameter> parseParameterConfig(String paramsConfigJson) {
        if (!StringUtils.hasText(paramsConfigJson)) {
            return Collections.emptyList();
        }
        
        try {
            return objectMapper.readValue(paramsConfigJson, 
                    new TypeReference<List<TemplateParameter>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse parameter config JSON: {}", paramsConfigJson, e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public String serializeParameterConfig(List<TemplateParameter> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return "[]";
        }
        
        try {
            return objectMapper.writeValueAsString(parameters);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize parameter config", e);
            return "[]";
        }
    }
    
    // ==================== 参数替换 ====================
    
    @Override
    public String replaceParameters(String sqlTemplate, Map<String, Object> params,
                                   List<TemplateParameter> parameterConfigs) {
        if (!StringUtils.hasText(sqlTemplate)) {
            return sqlTemplate;
        }
        
        if (params == null || params.isEmpty()) {
            return sqlTemplate;
        }
        
        // 构建参数配置映射
        Map<String, TemplateParameter> configMap = new HashMap<>();
        if (parameterConfigs != null) {
            for (TemplateParameter config : parameterConfigs) {
                if (config.getName() != null) {
                    configMap.put(config.getName(), config);
                }
            }
        }
        
        String result = sqlTemplate;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String paramName = entry.getKey();
            Object value = entry.getValue();
            String placeholder = "${" + paramName + "}";
            
            TemplateParameter config = configMap.get(paramName);
            String formattedValue = formatParameterValue(value, config);
            
            result = result.replace(placeholder, formattedValue);
        }
        
        return result;
    }
    
    @Override
    public String replaceParameters(String sqlTemplate, Map<String, Object> params) {
        return replaceParameters(sqlTemplate, params, null);
    }
    
    @Override
    public String formatParameterValue(Object value, TemplateParameter paramConfig) {
        if (value == null) {
            return "NULL";
        }
        
        // 处理集合类型（用于IN子句）
        if (value instanceof Collection) {
            Collection<?> collection = (Collection<?>) value;
            return collection.stream()
                    .map(v -> formatSingleValue(v, paramConfig))
                    .collect(Collectors.joining(", "));
        }
        
        return formatSingleValue(value, paramConfig);
    }
    
    /**
     * 格式化单个值
     */
    private String formatSingleValue(Object value, TemplateParameter paramConfig) {
        if (value == null) {
            return "NULL";
        }
        
        // 根据参数配置确定是否需要引号
        boolean needQuote = paramConfig != null ? paramConfig.shouldQuoteInSql() : true;
        String type = paramConfig != null ? paramConfig.getType() : null;
        
        // 数字类型
        if (value instanceof Number) {
            return value.toString();
        }
        
        // 布尔类型
        if (value instanceof Boolean) {
            return (Boolean) value ? "1" : "0";
        }
        
        // 根据配置的类型处理
        if (type != null) {
            switch (type.toLowerCase()) {
                case "number":
                case "integer":
                    try {
                        return new BigDecimal(value.toString()).toPlainString();
                    } catch (NumberFormatException e) {
                        // 如果转换失败，作为字符串处理
                        break;
                    }
                case "boolean":
                    String boolStr = value.toString().toLowerCase();
                    if ("true".equals(boolStr) || "1".equals(boolStr)) {
                        return "1";
                    } else if ("false".equals(boolStr) || "0".equals(boolStr)) {
                        return "0";
                    }
                    break;
            }
        }
        
        // 字符串类型，需要转义单引号并添加引号
        String strValue = value.toString();
        strValue = escapeSqlString(strValue);
        
        if (needQuote) {
            return "'" + strValue + "'";
        }
        return strValue;
    }
    
    /**
     * 转义SQL字符串中的特殊字符
     */
    private String escapeSqlString(String value) {
        if (value == null) {
            return null;
        }
        // 转义单引号
        return value.replace("'", "''");
    }
    
    // ==================== 参数验证 ====================
    
    @Override
    public List<String> validateParameters(Map<String, Object> params,
                                          List<TemplateParameter> parameterConfigs) {
        List<String> errors = new ArrayList<>();
        
        if (parameterConfigs == null || parameterConfigs.isEmpty()) {
            return errors;
        }
        
        Map<String, Object> effectiveParams = params != null ? params : Collections.emptyMap();
        
        for (TemplateParameter config : parameterConfigs) {
            String paramName = config.getName();
            if (paramName == null) {
                continue;
            }
            
            Object value = effectiveParams.get(paramName);
            
            // 检查必填参数
            if (config.isRequired() && (value == null || isEmptyValue(value))) {
                errors.add("缺少必需参数: " + paramName);
                continue;
            }
            
            // 如果有值，进行类型和范围验证
            if (value != null && !isEmptyValue(value)) {
                errors.addAll(validateParameter(paramName, value, config));
            }
        }
        
        return errors;
    }
    
    @Override
    public List<String> validateParameter(String paramName, Object value, 
                                         TemplateParameter paramConfig) {
        List<String> errors = new ArrayList<>();
        
        if (paramConfig == null || value == null) {
            return errors;
        }
        
        String type = paramConfig.getType();
        if (!StringUtils.hasText(type)) {
            return errors;
        }
        
        switch (type.toLowerCase()) {
            case "number":
            case "integer":
                errors.addAll(validateNumberParameter(paramName, value, paramConfig));
                break;
            case "date":
                errors.addAll(validateDateParameter(paramName, value, paramConfig, DEFAULT_DATE_FORMAT));
                break;
            case "datetime":
                errors.addAll(validateDateParameter(paramName, value, paramConfig, DEFAULT_DATETIME_FORMAT));
                break;
            case "boolean":
                errors.addAll(validateBooleanParameter(paramName, value));
                break;
            case "string":
                errors.addAll(validateStringParameter(paramName, value, paramConfig));
                break;
            case "select":
                errors.addAll(validateSelectParameter(paramName, value, paramConfig, false));
                break;
            case "multiselect":
                errors.addAll(validateSelectParameter(paramName, value, paramConfig, true));
                break;
        }
        
        return errors;
    }
    
    /**
     * 验证数字类型参数
     */
    private List<String> validateNumberParameter(String paramName, Object value, 
                                                 TemplateParameter config) {
        List<String> errors = new ArrayList<>();
        
        BigDecimal numValue;
        try {
            if (value instanceof Number) {
                numValue = new BigDecimal(value.toString());
            } else {
                numValue = new BigDecimal(value.toString());
            }
        } catch (NumberFormatException e) {
            errors.add("参数 " + paramName + " 必须是数字类型");
            return errors;
        }
        
        // 整数类型检查
        if ("integer".equalsIgnoreCase(config.getType())) {
            if (numValue.scale() > 0 && numValue.stripTrailingZeros().scale() > 0) {
                errors.add("参数 " + paramName + " 必须是整数");
            }
        }
        
        // 最小值检查
        if (config.getMinValue() != null) {
            BigDecimal minValue = new BigDecimal(config.getMinValue().toString());
            if (numValue.compareTo(minValue) < 0) {
                errors.add("参数 " + paramName + " 不能小于 " + config.getMinValue());
            }
        }
        
        // 最大值检查
        if (config.getMaxValue() != null) {
            BigDecimal maxValue = new BigDecimal(config.getMaxValue().toString());
            if (numValue.compareTo(maxValue) > 0) {
                errors.add("参数 " + paramName + " 不能大于 " + config.getMaxValue());
            }
        }
        
        return errors;
    }
    
    /**
     * 验证日期类型参数
     */
    private List<String> validateDateParameter(String paramName, Object value,
                                              TemplateParameter config, String defaultFormat) {
        List<String> errors = new ArrayList<>();
        
        String strValue = value.toString();
        String format = StringUtils.hasText(config.getDateFormat()) ? 
                config.getDateFormat() : defaultFormat;
        
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
            if (format.contains("HH") || format.contains("mm") || format.contains("ss")) {
                LocalDateTime.parse(strValue, formatter);
            } else {
                LocalDate.parse(strValue, formatter);
            }
        } catch (DateTimeParseException e) {
            errors.add("参数 " + paramName + " 日期格式不正确，期望格式: " + format);
        }
        
        return errors;
    }
    
    /**
     * 验证布尔类型参数
     */
    private List<String> validateBooleanParameter(String paramName, Object value) {
        List<String> errors = new ArrayList<>();
        
        if (value instanceof Boolean) {
            return errors;
        }
        
        String strValue = value.toString().toLowerCase();
        if (!strValue.equals("true") && !strValue.equals("false") &&
            !strValue.equals("1") && !strValue.equals("0")) {
            errors.add("参数 " + paramName + " 必须是布尔类型 (true/false/1/0)");
        }
        
        return errors;
    }
    
    /**
     * 验证字符串类型参数
     */
    private List<String> validateStringParameter(String paramName, Object value,
                                                TemplateParameter config) {
        List<String> errors = new ArrayList<>();
        
        String strValue = value.toString();
        
        // 最小长度检查
        if (config.getMinLength() != null && strValue.length() < config.getMinLength()) {
            errors.add("参数 " + paramName + " 长度不能小于 " + config.getMinLength());
        }
        
        // 最大长度检查
        if (config.getMaxLength() != null && strValue.length() > config.getMaxLength()) {
            errors.add("参数 " + paramName + " 长度不能大于 " + config.getMaxLength());
        }
        
        // 正则表达式验证
        if (StringUtils.hasText(config.getPattern())) {
            try {
                if (!strValue.matches(config.getPattern())) {
                    errors.add("参数 " + paramName + " 格式不正确");
                }
            } catch (Exception e) {
                log.warn("Invalid regex pattern for parameter {}: {}", paramName, config.getPattern());
            }
        }
        
        return errors;
    }
    
    /**
     * 验证选择类型参数
     */
    private List<String> validateSelectParameter(String paramName, Object value,
                                                TemplateParameter config, boolean isMulti) {
        List<String> errors = new ArrayList<>();
        
        List<TemplateParameter.ParameterOption> options = config.getOptions();
        if (options == null || options.isEmpty()) {
            return errors; // 没有选项配置，跳过验证
        }
        
        Set<Object> validValues = options.stream()
                .filter(opt -> !Boolean.TRUE.equals(opt.getDisabled()))
                .map(TemplateParameter.ParameterOption::getValue)
                .collect(Collectors.toSet());
        
        if (isMulti && value instanceof Collection) {
            Collection<?> values = (Collection<?>) value;
            for (Object v : values) {
                if (!validValues.contains(v) && !validValues.contains(v.toString())) {
                    errors.add("参数 " + paramName + " 包含无效选项: " + v);
                }
            }
        } else {
            if (!validValues.contains(value) && !validValues.contains(value.toString())) {
                errors.add("参数 " + paramName + " 的值不在有效选项中");
            }
        }
        
        return errors;
    }
    
    /**
     * 检查值是否为空
     */
    private boolean isEmptyValue(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String) {
            return ((String) value).trim().isEmpty();
        }
        if (value instanceof Collection) {
            return ((Collection<?>) value).isEmpty();
        }
        return false;
    }
    
    // ==================== 默认值处理 ====================
    
    @Override
    public Map<String, Object> applyDefaultValues(Map<String, Object> params,
                                                 List<TemplateParameter> parameterConfigs) {
        if (parameterConfigs == null || parameterConfigs.isEmpty()) {
            return params != null ? params : new HashMap<>();
        }
        
        Map<String, Object> result = params != null ? new HashMap<>(params) : new HashMap<>();
        
        for (TemplateParameter config : parameterConfigs) {
            String paramName = config.getName();
            if (paramName == null) {
                continue;
            }
            
            // 如果参数没有值，使用默认值
            if (!result.containsKey(paramName) || result.get(paramName) == null) {
                Object defaultValue = config.getDefaultValue();
                if (defaultValue != null) {
                    result.put(paramName, defaultValue);
                }
            }
        }
        
        return result;
    }
    
    @Override
    public Map<String, Object> getDefaultValues(List<TemplateParameter> parameterConfigs) {
        Map<String, Object> defaults = new HashMap<>();
        
        if (parameterConfigs == null || parameterConfigs.isEmpty()) {
            return defaults;
        }
        
        for (TemplateParameter config : parameterConfigs) {
            if (config.getName() != null && config.getDefaultValue() != null) {
                defaults.put(config.getName(), config.getDefaultValue());
            }
        }
        
        return defaults;
    }
    
    // ==================== 参数配置合并 ====================
    
    @Override
    public List<TemplateParameter> mergeParameterConfigs(String sqlTemplate,
                                                        List<TemplateParameter> existingConfigs) {
        List<String> sqlParams = parseParameterPlaceholders(sqlTemplate);
        
        if (sqlParams.isEmpty()) {
            return existingConfigs != null ? existingConfigs : Collections.emptyList();
        }
        
        // 构建现有配置映射
        Map<String, TemplateParameter> configMap = new LinkedHashMap<>();
        if (existingConfigs != null) {
            for (TemplateParameter config : existingConfigs) {
                if (config.getName() != null) {
                    configMap.put(config.getName(), config);
                }
            }
        }
        
        // 合并：确保所有SQL参数都有配置
        List<TemplateParameter> result = new ArrayList<>();
        int sortOrder = 0;
        
        for (String paramName : sqlParams) {
            TemplateParameter config = configMap.get(paramName);
            if (config == null) {
                // 创建默认配置
                config = createDefaultParameterConfig(paramName);
            }
            config.setSortOrder(sortOrder++);
            result.add(config);
            configMap.remove(paramName);
        }
        
        // 添加剩余的配置（SQL中未使用但已配置的参数）
        for (TemplateParameter config : configMap.values()) {
            config.setSortOrder(sortOrder++);
            result.add(config);
        }
        
        return result;
    }
    
    @Override
    public TemplateParameter createDefaultParameterConfig(String paramName) {
        TemplateParameter config = new TemplateParameter();
        config.setName(paramName);
        config.setLabel(paramName);
        config.setType(inferParameterType(paramName));
        config.setRequired(false);
        config.setDescription(null);
        config.setPlaceholder("请输入" + paramName);
        return config;
    }
    
    /**
     * 根据参数名推断参数类型
     */
    private String inferParameterType(String paramName) {
        if (paramName == null) {
            return "string";
        }
        
        String lowerName = paramName.toLowerCase();
        
        // 日期相关
        if (lowerName.contains("date") || lowerName.contains("time") ||
            lowerName.endsWith("_at") || lowerName.endsWith("At")) {
            if (lowerName.contains("time") || lowerName.contains("datetime")) {
                return "datetime";
            }
            return "date";
        }
        
        // 数字相关
        if (lowerName.contains("id") || lowerName.contains("count") ||
            lowerName.contains("num") || lowerName.contains("amount") ||
            lowerName.contains("price") || lowerName.contains("quantity") ||
            lowerName.contains("total") || lowerName.contains("sum")) {
            return "number";
        }
        
        // 布尔相关
        if (lowerName.startsWith("is") || lowerName.startsWith("has") ||
            lowerName.contains("flag") || lowerName.contains("enabled") ||
            lowerName.contains("active") || lowerName.contains("status")) {
            return "boolean";
        }
        
        return "string";
    }
}
