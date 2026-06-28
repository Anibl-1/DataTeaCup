package com.dataplatform.data.service;

import com.dataplatform.common.exception.BusinessException;
import com.dataplatform.common.exception.ErrorCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.BiFunction;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 参数校验服务
 * 实现后端参数校验，与前端保持一致的校验规则
 * 
 * 功能：
 * - 支持多种校验规则：required, type, range, pattern, custom
 * - 返回清晰的错误信息
 * - 与前端校验规则保持一致
 * 
 * @validates 需求 13.4 - 实现前后端双重参数校验
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParameterValidationService {

    private final ObjectMapper objectMapper;
    
    /** 内置自定义校验器 */
    private final Map<String, BiFunction<Object, Map<String, Object>, ValidationError>> customValidators = new HashMap<>();
    
    /** 手机号正则 */
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    
    /** 邮箱正则 */
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    
    /** 身份证号正则 */
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("^(\\d{15}|\\d{17}[\\dXx])$");
    
    @PostConstruct
    public void init() {
        // 注册内置校验器
        registerBuiltInValidators();
        log.info("参数校验服务初始化完成，已注册 {} 个内置校验器", customValidators.size());
    }
    
    /**
     * 注册内置校验器
     */
    private void registerBuiltInValidators() {
        // 手机号校验
        customValidators.put("phone", (value, params) -> {
            if (isEmpty(value)) return null;
            if (!PHONE_PATTERN.matcher(String.valueOf(value)).matches()) {
                return new ValidationError("phone", "custom", "请输入有效的手机号", value);
            }
            return null;
        });
        
        // 邮箱校验
        customValidators.put("email", (value, params) -> {
            if (isEmpty(value)) return null;
            if (!EMAIL_PATTERN.matcher(String.valueOf(value)).matches()) {
                return new ValidationError("email", "custom", "请输入有效的邮箱地址", value);
            }
            return null;
        });
        
        // 身份证号校验
        customValidators.put("idCard", (value, params) -> {
            if (isEmpty(value)) return null;
            if (!ID_CARD_PATTERN.matcher(String.valueOf(value)).matches()) {
                return new ValidationError("idCard", "custom", "请输入有效的身份证号", value);
            }
            return null;
        });
        
        // URL校验
        customValidators.put("url", (value, params) -> {
            if (isEmpty(value)) return null;
            try {
                new java.net.URL(String.valueOf(value));
                return null;
            } catch (Exception e) {
                return new ValidationError("url", "custom", "请输入有效的URL", value);
            }
        });
        
        // 日期范围校验
        customValidators.put("dateRange", (value, params) -> {
            if (isEmpty(value)) return null;
            try {
                LocalDate date = parseDate(value);
                if (date == null) {
                    return new ValidationError("dateRange", "custom", "请输入有效的日期", value);
                }
                
                if (params != null) {
                    Object minDateObj = params.get("minDate");
                    if (minDateObj != null) {
                        LocalDate minDate = parseDate(minDateObj);
                        if (minDate != null && date.isBefore(minDate)) {
                            return new ValidationError("dateRange", "custom", 
                                    "日期不能早于 " + minDateObj, value);
                        }
                    }
                    
                    Object maxDateObj = params.get("maxDate");
                    if (maxDateObj != null) {
                        LocalDate maxDate = parseDate(maxDateObj);
                        if (maxDate != null && date.isAfter(maxDate)) {
                            return new ValidationError("dateRange", "custom", 
                                    "日期不能晚于 " + maxDateObj, value);
                        }
                    }
                }
                return null;
            } catch (Exception e) {
                return new ValidationError("dateRange", "custom", "请输入有效的日期", value);
            }
        });
        
        // 正整数校验
        customValidators.put("positiveInteger", (value, params) -> {
            if (isEmpty(value)) return null;
            try {
                long num = Long.parseLong(String.valueOf(value));
                if (num <= 0) {
                    return new ValidationError("positiveInteger", "custom", "请输入正整数", value);
                }
                return null;
            } catch (NumberFormatException e) {
                return new ValidationError("positiveInteger", "custom", "请输入正整数", value);
            }
        });
        
        // 非负整数校验
        customValidators.put("nonNegativeInteger", (value, params) -> {
            if (isEmpty(value)) return null;
            try {
                long num = Long.parseLong(String.valueOf(value));
                if (num < 0) {
                    return new ValidationError("nonNegativeInteger", "custom", "请输入非负整数", value);
                }
                return null;
            } catch (NumberFormatException e) {
                return new ValidationError("nonNegativeInteger", "custom", "请输入非负整数", value);
            }
        });
        
        // 小数位数校验
        customValidators.put("decimalPlaces", (value, params) -> {
            if (isEmpty(value)) return null;
            int maxPlaces = 2;
            if (params != null && params.get("maxPlaces") != null) {
                maxPlaces = ((Number) params.get("maxPlaces")).intValue();
            }
            
            String str = String.valueOf(value);
            int dotIndex = str.indexOf('.');
            if (dotIndex >= 0) {
                int decimalLength = str.length() - dotIndex - 1;
                if (decimalLength > maxPlaces) {
                    return new ValidationError("decimalPlaces", "custom", 
                            "小数位数不能超过 " + maxPlaces + " 位", value);
                }
            }
            return null;
        });
    }
    
    /**
     * 解析日期
     */
    private LocalDate parseDate(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDate) return (LocalDate) value;
        if (value instanceof LocalDateTime) return ((LocalDateTime) value).toLocalDate();
        
        String str = String.valueOf(value);
        try {
            return LocalDate.parse(str);
        } catch (DateTimeParseException e) {
            // 尝试其他格式
            try {
                return LocalDateTime.parse(str).toLocalDate();
            } catch (DateTimeParseException e2) {
                return null;
            }
        }
    }
    
    /**
     * 校验单个参数
     * 
     * @param value 参数值
     * @param config 校验配置
     * @return 校验结果
     */
    public ValidationResult validateParameter(Object value, ParameterValidationConfig config) {
        List<ValidationError> errors = new ArrayList<>();
        String displayName = StringUtils.hasText(config.getDisplayName()) 
                ? config.getDisplayName() 
                : config.getParamName();
        
        for (ValidationRule rule : config.getRules()) {
            // 跳过禁用的规则
            if (Boolean.FALSE.equals(rule.getEnabled())) continue;
            
            ValidationError error = validateRule(value, rule, displayName, config.getParamName());
            
            if (error != null) {
                errors.add(error);
                // 如果是必填校验失败，后续规则不再校验
                if ("required".equals(rule.getType())) break;
            }
        }
        
        ValidationResult result = new ValidationResult();
        result.setValid(errors.isEmpty());
        result.setErrors(errors);
        return result;
    }
    
    /**
     * 批量校验多个参数
     * 
     * @param values 参数值映射
     * @param configs 校验配置列表
     * @return 批量校验结果
     */
    public BatchValidationResult validateParameters(
            Map<String, Object> values, 
            List<ParameterValidationConfig> configs) {
        
        Map<String, List<ValidationError>> errorsByParam = new LinkedHashMap<>();
        List<ValidationError> allErrors = new ArrayList<>();
        
        for (ParameterValidationConfig config : configs) {
            Object value = values.get(config.getParamName());
            ValidationResult result = validateParameter(value, config);
            
            if (!result.isValid()) {
                errorsByParam.put(config.getParamName(), result.getErrors());
                allErrors.addAll(result.getErrors());
            }
        }
        
        BatchValidationResult batchResult = new BatchValidationResult();
        batchResult.setValid(allErrors.isEmpty());
        batchResult.setErrorsByParam(errorsByParam);
        batchResult.setAllErrors(allErrors);
        if (!allErrors.isEmpty()) {
            batchResult.setFirstError(allErrors.get(0).getMessage());
        }
        return batchResult;
    }
    
    /**
     * 校验单个规则
     */
    private ValidationError validateRule(Object value, ValidationRule rule, String displayName, String paramName) {
        switch (rule.getType()) {
            case "required":
                return validateRequired(value, rule, displayName, paramName);
            case "type":
                return validateType(value, rule, displayName, paramName);
            case "range":
                return validateRange(value, rule, displayName, paramName);
            case "length":
                return validateLength(value, rule, displayName, paramName);
            case "pattern":
                return validatePattern(value, rule, displayName, paramName);
            case "enum":
                return validateEnum(value, rule, displayName, paramName);
            case "custom":
                return validateCustom(value, rule, displayName, paramName);
            default:
                log.warn("Unknown validation rule type: {}", rule.getType());
                return null;
        }
    }
    
    /**
     * 校验必填规则
     */
    private ValidationError validateRequired(Object value, ValidationRule rule, String displayName, String paramName) {
        Boolean required = (Boolean) rule.getConfig().get("required");
        if (!Boolean.TRUE.equals(required)) return null;
        
        if (isEmpty(value)) {
            String message = StringUtils.hasText(rule.getMessage()) 
                    ? rule.getMessage() 
                    : displayName + "不能为空";
            return new ValidationError(paramName, "required", message, value);
        }
        return null;
    }
    
    /**
     * 校验类型规则
     */
    private ValidationError validateType(Object value, ValidationRule rule, String displayName, String paramName) {
        if (isEmpty(value)) return null;
        
        String dataType = (String) rule.getConfig().get("dataType");
        boolean valid = false;
        String expectedTypeLabel = dataType;
        
        switch (dataType) {
            case "string":
                valid = value instanceof String;
                expectedTypeLabel = "字符串";
                break;
            case "number":
                valid = value instanceof Number || isNumericString(value);
                expectedTypeLabel = "数字";
                break;
            case "integer":
                valid = isInteger(value);
                expectedTypeLabel = "整数";
                break;
            case "boolean":
                valid = value instanceof Boolean || isBooleanString(value);
                expectedTypeLabel = "布尔值";
                break;
            case "date":
            case "datetime":
                valid = isValidDate(value);
                expectedTypeLabel = "date".equals(dataType) ? "日期" : "日期时间";
                break;
            case "array":
                valid = value instanceof Collection || value.getClass().isArray();
                expectedTypeLabel = "数组";
                break;
            default:
                valid = true;
        }
        
        if (!valid) {
            String message = StringUtils.hasText(rule.getMessage()) 
                    ? rule.getMessage() 
                    : displayName + "必须是" + expectedTypeLabel + "类型";
            return new ValidationError(paramName, "type", message, value);
        }
        return null;
    }
    
    /**
     * 校验范围规则
     */
    private ValidationError validateRange(Object value, ValidationRule rule, String displayName, String paramName) {
        if (isEmpty(value)) return null;
        
        BigDecimal num;
        try {
            num = new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException e) {
            String message = StringUtils.hasText(rule.getMessage()) 
                    ? rule.getMessage() 
                    : displayName + "必须是有效的数字";
            return new ValidationError(paramName, "range", message, value);
        }
        
        Object minObj = rule.getConfig().get("min");
        Object maxObj = rule.getConfig().get("max");
        Boolean minInclusive = (Boolean) rule.getConfig().getOrDefault("minInclusive", true);
        Boolean maxInclusive = (Boolean) rule.getConfig().getOrDefault("maxInclusive", true);
        
        if (minObj != null) {
            BigDecimal min = new BigDecimal(String.valueOf(minObj));
            boolean minValid = minInclusive ? num.compareTo(min) >= 0 : num.compareTo(min) > 0;
            if (!minValid) {
                String boundary = minInclusive ? "大于等于" : "大于";
                String message = StringUtils.hasText(rule.getMessage()) 
                        ? rule.getMessage() 
                        : displayName + "必须" + boundary + minObj;
                return new ValidationError(paramName, "range", message, value);
            }
        }
        
        if (maxObj != null) {
            BigDecimal max = new BigDecimal(String.valueOf(maxObj));
            boolean maxValid = maxInclusive ? num.compareTo(max) <= 0 : num.compareTo(max) < 0;
            if (!maxValid) {
                String boundary = maxInclusive ? "小于等于" : "小于";
                String message = StringUtils.hasText(rule.getMessage()) 
                        ? rule.getMessage() 
                        : displayName + "必须" + boundary + maxObj;
                return new ValidationError(paramName, "range", message, value);
            }
        }
        
        return null;
    }
    
    /**
     * 校验长度规则
     */
    private ValidationError validateLength(Object value, ValidationRule rule, String displayName, String paramName) {
        if (isEmpty(value)) return null;
        
        int length;
        if (value instanceof String) {
            length = ((String) value).length();
        } else if (value instanceof Collection) {
            length = ((Collection<?>) value).size();
        } else if (value.getClass().isArray()) {
            length = java.lang.reflect.Array.getLength(value);
        } else {
            return null;
        }
        
        Object minLengthObj = rule.getConfig().get("minLength");
        Object maxLengthObj = rule.getConfig().get("maxLength");
        
        if (minLengthObj != null) {
            int minLength = ((Number) minLengthObj).intValue();
            if (length < minLength) {
                String message = StringUtils.hasText(rule.getMessage()) 
                        ? rule.getMessage() 
                        : displayName + "长度不能少于" + minLength;
                return new ValidationError(paramName, "length", message, value);
            }
        }
        
        if (maxLengthObj != null) {
            int maxLength = ((Number) maxLengthObj).intValue();
            if (length > maxLength) {
                String message = StringUtils.hasText(rule.getMessage()) 
                        ? rule.getMessage() 
                        : displayName + "长度不能超过" + maxLength;
                return new ValidationError(paramName, "length", message, value);
            }
        }
        
        return null;
    }
    
    /**
     * 校验正则表达式规则
     */
    private ValidationError validatePattern(Object value, ValidationRule rule, String displayName, String paramName) {
        if (isEmpty(value)) return null;
        
        String patternStr = (String) rule.getConfig().get("pattern");
        String flags = (String) rule.getConfig().get("flags");
        
        try {
            int patternFlags = 0;
            if (flags != null) {
                if (flags.contains("i")) patternFlags |= Pattern.CASE_INSENSITIVE;
                if (flags.contains("m")) patternFlags |= Pattern.MULTILINE;
            }
            
            Pattern pattern = Pattern.compile(patternStr, patternFlags);
            if (!pattern.matcher(String.valueOf(value)).matches()) {
                String message = StringUtils.hasText(rule.getMessage()) 
                        ? rule.getMessage() 
                        : displayName + "格式不正确";
                return new ValidationError(paramName, "pattern", message, value);
            }
        } catch (PatternSyntaxException e) {
            log.error("Invalid regex pattern: {}", patternStr, e);
            return new ValidationError(paramName, "pattern", "校验规则配置错误", value);
        }
        
        return null;
    }
    
    /**
     * 校验枚举规则
     */
    @SuppressWarnings("unchecked")
    private ValidationError validateEnum(Object value, ValidationRule rule, String displayName, String paramName) {
        if (isEmpty(value)) return null;
        
        List<Object> allowedValues = (List<Object>) rule.getConfig().get("values");
        if (allowedValues == null || allowedValues.isEmpty()) return null;
        
        // 转换为字符串比较
        String valueStr = String.valueOf(value);
        boolean found = allowedValues.stream()
                .anyMatch(v -> String.valueOf(v).equals(valueStr));
        
        if (!found) {
            String message = StringUtils.hasText(rule.getMessage()) 
                    ? rule.getMessage() 
                    : displayName + "的值不在允许的范围内";
            return new ValidationError(paramName, "enum", message, value);
        }
        
        return null;
    }
    
    /**
     * 校验自定义规则
     */
    @SuppressWarnings("unchecked")
    private ValidationError validateCustom(Object value, ValidationRule rule, String displayName, String paramName) {
        if (isEmpty(value)) return null;
        
        String validatorName = (String) rule.getConfig().get("validatorName");
        Map<String, Object> params = (Map<String, Object>) rule.getConfig().get("params");
        
        BiFunction<Object, Map<String, Object>, ValidationError> validator = customValidators.get(validatorName);
        if (validator == null) {
            log.warn("Custom validator not found: {}", validatorName);
            return null;
        }
        
        ValidationError error = validator.apply(value, params);
        if (error != null) {
            error.setParamName(paramName);
            if (StringUtils.hasText(rule.getMessage())) {
                error.setMessage(rule.getMessage());
            }
        }
        return error;
    }
    
    // ========================================================================
    // 辅助方法
    // ========================================================================
    
    /**
     * 检查值是否为空
     */
    private boolean isEmpty(Object value) {
        if (value == null) return true;
        if (value instanceof String && ((String) value).trim().isEmpty()) return true;
        if (value instanceof Collection && ((Collection<?>) value).isEmpty()) return true;
        if (value.getClass().isArray() && java.lang.reflect.Array.getLength(value) == 0) return true;
        return false;
    }
    
    /**
     * 检查是否为数字字符串
     */
    private boolean isNumericString(Object value) {
        try {
            new BigDecimal(String.valueOf(value));
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * 检查是否为整数
     */
    private boolean isInteger(Object value) {
        if (value instanceof Integer || value instanceof Long || value instanceof Short || value instanceof Byte) {
            return true;
        }
        try {
            String str = String.valueOf(value);
            Long.parseLong(str);
            return !str.contains(".");
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * 检查是否为布尔字符串
     */
    private boolean isBooleanString(Object value) {
        String str = String.valueOf(value).toLowerCase();
        return "true".equals(str) || "false".equals(str);
    }
    
    /**
     * 检查是否为有效日期
     */
    private boolean isValidDate(Object value) {
        if (value instanceof LocalDate || value instanceof LocalDateTime || value instanceof Date) {
            return true;
        }
        return parseDate(value) != null;
    }
    
    /**
     * 注册自定义校验器
     * 
     * @param name 校验器名称
     * @param validator 校验函数
     */
    public void registerValidator(String name, BiFunction<Object, Map<String, Object>, ValidationError> validator) {
        customValidators.put(name, validator);
        log.debug("Registered custom validator: {}", name);
    }
    
    /**
     * 获取所有内置校验器名称
     */
    public Set<String> getBuiltInValidatorNames() {
        return Collections.unmodifiableSet(customValidators.keySet());
    }
    
    /**
     * 校验并抛出异常
     * 
     * @param values 参数值映射
     * @param configs 校验配置列表
     * @throws BusinessException 校验失败时抛出
     */
    public void validateAndThrow(Map<String, Object> values, List<ParameterValidationConfig> configs) {
        BatchValidationResult result = validateParameters(values, configs);
        if (!result.isValid()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, result.getFirstError());
        }
    }
    
    // ========================================================================
    // 数据类
    // ========================================================================
    
    /**
     * 校验规则
     */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ValidationRule {
        /** 规则类型: required, type, range, length, pattern, enum, custom */
        private String type;
        /** 错误消息 */
        private String message;
        /** 是否启用 */
        private Boolean enabled = true;
        /** 规则配置 */
        private Map<String, Object> config = new HashMap<>();
    }
    
    /**
     * 参数校验配置
     */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ParameterValidationConfig {
        /** 参数名称 */
        private String paramName;
        /** 参数显示名称 */
        private String displayName;
        /** 校验规则列表 */
        private List<ValidationRule> rules = new ArrayList<>();
    }
    
    /**
     * 校验错误
     */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ValidationError {
        /** 参数名称 */
        private String paramName;
        /** 规则类型 */
        private String ruleType;
        /** 错误消息 */
        private String message;
        /** 实际值 */
        private Object actualValue;
        
        public ValidationError() {}
        
        public ValidationError(String paramName, String ruleType, String message, Object actualValue) {
            this.paramName = paramName;
            this.ruleType = ruleType;
            this.message = message;
            this.actualValue = actualValue;
        }
    }
    
    /**
     * 校验结果
     */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ValidationResult {
        /** 是否校验通过 */
        private boolean valid;
        /** 错误列表 */
        private List<ValidationError> errors = new ArrayList<>();
    }
    
    /**
     * 批量校验结果
     */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BatchValidationResult {
        /** 是否全部校验通过 */
        private boolean valid;
        /** 按参数名分组的错误 */
        private Map<String, List<ValidationError>> errorsByParam = new LinkedHashMap<>();
        /** 所有错误列表 */
        private List<ValidationError> allErrors = new ArrayList<>();
        /** 第一个错误消息 */
        private String firstError;
    }
}
