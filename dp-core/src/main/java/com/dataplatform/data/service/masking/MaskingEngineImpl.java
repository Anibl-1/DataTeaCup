package com.dataplatform.data.service.masking;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 脱敏引擎实现类
 * 
 * 主要功能：
 * 1. 加载和缓存脱敏策略
 * 2. 应用脱敏规则到查询结果
 * 3. 支持流式处理大数据量脱敏
 * 4. 基于角色的差异化脱敏
 * 5. 规则预览和验证
 * 
 * **Validates: Requirements 6.1, 6.3**
 * 
 * @author dataplatform
 */
@Slf4j
@Service
public class MaskingEngineImpl implements MaskingEngine {
    
    /**
     * 策略缓存：策略类型 -> 策略实例
     */
    private final Map<String, MaskingStrategy> strategyCache = new ConcurrentHashMap<>();
    
    /**
     * 编译后的正则模式缓存：模式字符串 -> 编译后的Pattern
     */
    private final Map<String, Pattern> patternCache = new ConcurrentHashMap<>();
    
    /**
     * 敏感字段检测器
     */
    private final SensitiveFieldDetector sensitiveFieldDetector;
    
    /**
     * 所有可用的脱敏策略列表
     */
    private final List<MaskingStrategy> strategies;

    /**
     * 脱敏审计服务（可选依赖，不影响核心脱敏功能）
     */
    private final MaskingAuditService maskingAuditService;

    /**
     * 脱敏配置验证器
     */
    private final MaskingConfigValidator configValidator;
    
    @Autowired
    public MaskingEngineImpl(SensitiveFieldDetector sensitiveFieldDetector, 
                             List<MaskingStrategy> strategies,
                             @Autowired(required = false) MaskingAuditService maskingAuditService,
                             MaskingConfigValidator configValidator) {
        this.sensitiveFieldDetector = sensitiveFieldDetector;
        this.strategies = strategies;
        this.maskingAuditService = maskingAuditService;
        this.configValidator = configValidator;
    }
    
    /**
     * 初始化策略缓存
     */
    @PostConstruct
    public void init() {
        log.info("Initializing MaskingEngine with {} strategies", strategies.size());
        for (MaskingStrategy strategy : strategies) {
            String type = strategy.getStrategyType();
            strategyCache.put(type, strategy);
            log.debug("Registered masking strategy: {}", type);
        }
        log.info("MaskingEngine initialized successfully");
    }
    
    @Override
    public List<Map<String, Object>> maskData(List<Map<String, Object>> data, List<MaskingRule> rules) {
        if (data == null || data.isEmpty()) {
            return data;
        }
        if (rules == null || rules.isEmpty()) {
            return data;
        }
        
        // 验证规则配置，有错误则阻止数据返回
        validateAndThrowOnError(rules);
        
        // 过滤有效规则并按优先级排序
        List<MaskingRule> validRules = filterAndSortRules(rules);
        if (validRules.isEmpty()) {
            return data;
        }
        
        // 预处理：构建字段到规则的映射
        Map<String, MaskingRule> fieldRuleMap = buildFieldRuleMap(data.get(0).keySet(), validRules);
        
        long startTime = System.currentTimeMillis();
        
        // 应用脱敏
        List<Map<String, Object>> result = data.stream()
                .map(row -> maskRow(row, fieldRuleMap))
                .collect(Collectors.toList());

        // 记录审计日志
        recordAuditLog(null, "query", null, null, fieldRuleMap, data.size(), 
                System.currentTimeMillis() - startTime);

        return result;
    }
    
    @Override
    public Object maskField(Object value, MaskingStrategy strategy) {
        return maskField(value, strategy, null);
    }
    
    @Override
    public Object maskField(Object value, MaskingStrategy strategy, Map<String, Object> config) {
        if (value == null || strategy == null) {
            return value;
        }
        
        Map<String, Object> effectiveConfig = config != null ? config : strategy.getDefaultConfig();
        return strategy.mask(value, effectiveConfig);
    }
    
    @Override
    public Stream<Map<String, Object>> maskDataStream(Stream<Map<String, Object>> dataStream, 
                                                       List<MaskingRule> rules) {
        if (dataStream == null) {
            return Stream.empty();
        }
        if (rules == null || rules.isEmpty()) {
            return dataStream;
        }
        
        // 验证规则配置，有错误则阻止数据返回
        validateAndThrowOnError(rules);
        
        // 过滤有效规则并按优先级排序
        List<MaskingRule> validRules = filterAndSortRules(rules);
        if (validRules.isEmpty()) {
            return dataStream;
        }
        
        // Track row count and timing for audit
        AtomicInteger rowCounter = new AtomicInteger(0);
        long startTime = System.currentTimeMillis();
        // Capture field rule map from first row for audit
        Map<String, MaskingRule> auditFieldRuleMap = new ConcurrentHashMap<>();
        
        // 使用流式处理，延迟计算字段规则映射
        return dataStream.map(row -> {
            Map<String, MaskingRule> fieldRuleMap = buildFieldRuleMap(row.keySet(), validRules);
            if (auditFieldRuleMap.isEmpty() && !fieldRuleMap.isEmpty()) {
                auditFieldRuleMap.putAll(fieldRuleMap);
            }
            rowCounter.incrementAndGet();
            return maskRow(row, fieldRuleMap);
        }).onClose(() -> {
            // Record audit when stream is closed
            if (!auditFieldRuleMap.isEmpty()) {
                recordAuditLog(null, "query", null, null, auditFieldRuleMap, 
                        rowCounter.get(), System.currentTimeMillis() - startTime);
            }
        });
    }
    
    @Override
    public List<Map<String, Object>> maskDataByRole(List<Map<String, Object>> data, 
                                                     List<MaskingRule> rules,
                                                     Long userId, 
                                                     List<Long> roleIds) {
        if (data == null || data.isEmpty()) {
            return data;
        }
        if (rules == null || rules.isEmpty()) {
            return data;
        }
        
        // 验证规则配置，有错误则阻止数据返回
        validateAndThrowOnError(rules);
        
        // 过滤适用于当前角色的规则
        List<MaskingRule> applicableRules = rules.stream()
                .filter(rule -> rule.isValid() && rule.appliesToRoles(roleIds))
                .sorted(Comparator.comparingInt(r -> r.getPriority() != null ? r.getPriority() : Integer.MAX_VALUE))
                .collect(Collectors.toList());
        
        if (applicableRules.isEmpty()) {
            return data;
        }
        
        log.debug("Applying {} masking rules for user {} with roles {}", 
                applicableRules.size(), userId, roleIds);
        
        // 预处理：构建字段到规则的映射
        Map<String, MaskingRule> fieldRuleMap = buildFieldRuleMap(data.get(0).keySet(), applicableRules);
        
        long startTime = System.currentTimeMillis();
        
        // 应用脱敏
        List<Map<String, Object>> result = data.stream()
                .map(row -> maskRow(row, fieldRuleMap))
                .collect(Collectors.toList());

        // 记录审计日志
        recordAuditLog(userId, "query", null, null, fieldRuleMap, data.size(),
                System.currentTimeMillis() - startTime);

        return result;
    }
    
    @Override
    public List<SensitiveFieldInfo> detectSensitiveFields(List<Map<String, Object>> data, 
                                                           List<String> fieldNames) {
        return sensitiveFieldDetector.detectSensitiveFields(data, fieldNames);
    }
    
    @Override
    public Object previewMasking(Object sampleValue, MaskingRule rule) {
        if (sampleValue == null || rule == null) {
            return sampleValue;
        }
        
        String validationError = validateRule(rule);
        if (validationError != null) {
            log.warn("Invalid masking rule for preview: {}", validationError);
            return sampleValue;
        }
        
        MaskingStrategy strategy = getStrategy(rule.getStrategyType());
        if (strategy == null) {
            log.warn("Unknown strategy type for preview: {}", rule.getStrategyType());
            return sampleValue;
        }
        
        return strategy.mask(sampleValue, rule.getStrategyConfig());
    }
    
    @Override
    public String validateRule(MaskingRule rule) {
        if (rule == null) {
            return "规则不能为空";
        }
        
        // 验证字段匹配配置
        if (rule.getFieldName() == null && rule.getFieldPattern() == null) {
            return "必须指定字段名或字段匹配模式";
        }
        
        // 验证字段模式的正则表达式
        if (rule.getFieldPattern() != null && !rule.getFieldPattern().isEmpty()) {
            try {
                Pattern.compile(rule.getFieldPattern());
            } catch (PatternSyntaxException e) {
                return "字段匹配模式正则表达式无效: " + e.getMessage();
            }
        }
        
        // 验证策略类型
        if (rule.getStrategyType() == null || rule.getStrategyType().isEmpty()) {
            return "必须指定脱敏策略类型";
        }
        
        MaskingStrategy strategy = getStrategy(rule.getStrategyType());
        if (strategy == null) {
            return "不支持的脱敏策略类型: " + rule.getStrategyType();
        }
        
        // 验证策略配置
        if (rule.getStrategyConfig() != null && !strategy.validateConfig(rule.getStrategyConfig())) {
            return "脱敏策略配置无效";
        }
        
        return null; // 验证通过
    }
    
    @Override
    public MaskingStrategy getStrategy(String strategyType) {
        if (strategyType == null || strategyType.isEmpty()) {
            return null;
        }
        return strategyCache.get(strategyType.toUpperCase());
    }
    
    @Override
    public List<String> getAvailableStrategyTypes() {
        return new ArrayList<>(strategyCache.keySet());
    }

    @Override
    public MaskingConfigValidationResult validateRules(List<MaskingRule> rules) {
        return configValidator.validate(rules);
    }

    
    // ==================== 私有辅助方法 ====================

    /**
     * 验证规则配置，如果存在ERROR级别错误则抛出异常阻止数据返回
     *
     * @param rules 脱敏规则列表
     * @throws MaskingConfigException 当存在阻止数据返回的配置错误时
     */
    private void validateAndThrowOnError(List<MaskingRule> rules) {
        MaskingConfigValidationResult validationResult = configValidator.validate(rules);
        if (validationResult.hasErrors()) {
            log.error("Masking configuration validation failed: {}", validationResult.getSummary());
            throw new MaskingConfigException(validationResult);
        }
        if (validationResult.hasWarnings()) {
            log.warn("Masking configuration has warnings: {}", validationResult.getSummary());
        }
    }
    
    /**
     * 过滤有效规则并按优先级排序
     * 
     * @param rules 原始规则列表
     * @return 过滤并排序后的规则列表
     */
    private List<MaskingRule> filterAndSortRules(List<MaskingRule> rules) {
        return rules.stream()
                .filter(MaskingRule::isValid)
                .sorted(Comparator.comparingInt(r -> r.getPriority() != null ? r.getPriority() : Integer.MAX_VALUE))
                .collect(Collectors.toList());
    }
    
    /**
     * 构建字段到规则的映射
     * 每个字段只匹配优先级最高的规则
     * 
     * @param fieldNames 字段名集合
     * @param rules 已排序的规则列表
     * @return 字段名到规则的映射
     */
    private Map<String, MaskingRule> buildFieldRuleMap(Set<String> fieldNames, List<MaskingRule> rules) {
        Map<String, MaskingRule> fieldRuleMap = new HashMap<>();
        
        for (String fieldName : fieldNames) {
            for (MaskingRule rule : rules) {
                if (matchesField(fieldName, rule)) {
                    fieldRuleMap.put(fieldName, rule);
                    break; // 使用优先级最高的匹配规则
                }
            }
        }
        
        return fieldRuleMap;
    }
    
    /**
     * 检查字段是否匹配规则
     * 
     * @param fieldName 字段名
     * @param rule 脱敏规则
     * @return 是否匹配
     */
    private boolean matchesField(String fieldName, MaskingRule rule) {
        if (fieldName == null) {
            return false;
        }
        
        // 精确匹配（忽略大小写）
        if (rule.getFieldName() != null && rule.getFieldName().equalsIgnoreCase(fieldName)) {
            return true;
        }
        
        // 模式匹配
        String pattern = rule.getFieldPattern();
        if (pattern != null && !pattern.isEmpty()) {
            Pattern compiledPattern = getOrCompilePattern(pattern);
            if (compiledPattern != null) {
                return compiledPattern.matcher(fieldName).matches();
            }
        }
        
        return false;
    }
    
    /**
     * 获取或编译正则表达式模式
     * 使用缓存避免重复编译
     * 
     * @param pattern 正则表达式字符串
     * @return 编译后的Pattern，编译失败返回null
     */
    private Pattern getOrCompilePattern(String pattern) {
        return patternCache.computeIfAbsent(pattern, p -> {
            try {
                return Pattern.compile(p, Pattern.CASE_INSENSITIVE);
            } catch (PatternSyntaxException e) {
                log.warn("Invalid regex pattern: {}", p);
                return null;
            }
        });
    }
    
    /**
     * 对单行数据应用脱敏
     * 
     * @param row 原始数据行
     * @param fieldRuleMap 字段到规则的映射
     * @return 脱敏后的数据行
     */
    private Map<String, Object> maskRow(Map<String, Object> row, Map<String, MaskingRule> fieldRuleMap) {
        if (row == null || fieldRuleMap.isEmpty()) {
            return row;
        }
        
        Map<String, Object> maskedRow = new LinkedHashMap<>(row);
        
        for (Map.Entry<String, MaskingRule> entry : fieldRuleMap.entrySet()) {
            String fieldName = entry.getKey();
            MaskingRule rule = entry.getValue();
            
            if (maskedRow.containsKey(fieldName)) {
                Object originalValue = maskedRow.get(fieldName);
                Object maskedValue = applyMaskingRule(originalValue, rule);
                maskedRow.put(fieldName, maskedValue);
            }
        }
        
        return maskedRow;
    }
    
    /**
     * 应用单个脱敏规则
     * 
     * @param value 原始值
     * @param rule 脱敏规则
     * @return 脱敏后的值
     */
    private Object applyMaskingRule(Object value, MaskingRule rule) {
        if (value == null) {
            return null;
        }
        
        MaskingStrategy strategy = getStrategy(rule.getStrategyType());
        if (strategy == null) {
            log.warn("Unknown masking strategy: {}", rule.getStrategyType());
            return value;
        }
        
        try {
            Map<String, Object> config = rule.getStrategyConfig();
            return strategy.mask(value, config != null ? config : strategy.getDefaultConfig());
        } catch (Exception e) {
            log.error("Error applying masking rule {} to value: {}", rule.getId(), e.getMessage());
            return value; // 脱敏失败时返回原值，避免数据丢失
        }
    }

    /**
     * 记录脱敏审计日志（异步，不影响主流程）
     *
     * @param userId        用户ID
     * @param operationType 操作类型
     * @param dataSourceId  数据源ID
     * @param sqlHash       SQL哈希
     * @param fieldRuleMap  字段规则映射
     * @param rowCount      处理行数
     * @param executionTime 执行时间（毫秒）
     */
    private void recordAuditLog(Long userId, String operationType, Long dataSourceId,
                                String sqlHash, Map<String, MaskingRule> fieldRuleMap,
                                int rowCount, long executionTime) {
        if (maskingAuditService != null && !fieldRuleMap.isEmpty()) {
            try {
                maskingAuditService.recordAudit(userId, operationType, dataSourceId,
                        sqlHash, fieldRuleMap, rowCount, executionTime);
            } catch (Exception e) {
                log.warn("Failed to record masking audit log: {}", e.getMessage());
            }
        }
    }
}
