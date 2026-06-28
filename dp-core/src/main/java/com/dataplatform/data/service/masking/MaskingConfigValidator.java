package com.dataplatform.data.service.masking;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

/**
 * 脱敏配置验证器
 * 
 * 提供全面的脱敏规则配置验证，包括：
 * 1. 无效策略类型检测
 * 2. 无效正则表达式检测
 * 3. 缺失必需配置参数检测
 * 4. 冲突规则检测（同一字段多条规则）
 * 5. 结构化错误报告
 *
 * **Validates: Requirements 6.5**
 *
 * @author dataplatform
 */
@Slf4j
@Component
public class MaskingConfigValidator {

    /** 错误码常量 */
    public static final String ERR_NULL_RULE = "MASKING_RULE_NULL";
    public static final String ERR_MISSING_FIELD = "MASKING_MISSING_FIELD";
    public static final String ERR_INVALID_STRATEGY = "MASKING_INVALID_STRATEGY";
    public static final String ERR_MISSING_STRATEGY = "MASKING_MISSING_STRATEGY";
    public static final String ERR_INVALID_REGEX = "MASKING_INVALID_REGEX";
    public static final String ERR_INVALID_CONFIG = "MASKING_INVALID_CONFIG";
    public static final String ERR_CONFLICTING_RULES = "MASKING_CONFLICTING_RULES";

    private final Map<String, MaskingStrategy> strategyMap;

    public MaskingConfigValidator(List<MaskingStrategy> strategies) {
        this.strategyMap = new HashMap<>();
        if (strategies != null) {
            for (MaskingStrategy s : strategies) {
                strategyMap.put(s.getStrategyType().toUpperCase(), s);
            }
        }
    }

    /**
     * 验证一组脱敏规则
     *
     * @param rules 规则列表
     * @return 验证结果
     */
    public MaskingConfigValidationResult validate(List<MaskingRule> rules) {
        MaskingConfigValidationResult result = new MaskingConfigValidationResult();

        if (rules == null || rules.isEmpty()) {
            return result; // 空规则列表是合法的
        }

        // 逐条验证
        for (int i = 0; i < rules.size(); i++) {
            MaskingRule rule = rules.get(i);
            validateSingleRule(rule, i, result);
        }

        // 检测冲突规则
        detectConflictingRules(rules, result);

        return result;
    }

    /**
     * 验证单条脱敏规则
     *
     * @param rule   规则
     * @param index  规则在列表中的索引
     * @param result 验证结果收集器
     */
    void validateSingleRule(MaskingRule rule, int index, MaskingConfigValidationResult result) {
        if (rule == null) {
            result.addError(MaskingConfigValidationError.builder()
                    .errorCode(ERR_NULL_RULE)
                    .message("规则[" + index + "]为空")
                    .severity(MaskingConfigValidationError.Severity.ERROR)
                    .build());
            return;
        }

        String fieldRef = rule.getFieldName() != null ? rule.getFieldName()
                : (rule.getFieldPattern() != null ? rule.getFieldPattern() : "unknown");

        // 1. 检查字段标识
        if (rule.getFieldName() == null && rule.getFieldPattern() == null) {
            result.addError(MaskingConfigValidationError.builder()
                    .errorCode(ERR_MISSING_FIELD)
                    .ruleId(rule.getId())
                    .field(fieldRef)
                    .message("规则[" + index + "]必须指定字段名(fieldName)或字段匹配模式(fieldPattern)")
                    .severity(MaskingConfigValidationError.Severity.ERROR)
                    .build());
        }

        // 2. 检查字段模式正则表达式
        if (rule.getFieldPattern() != null && !rule.getFieldPattern().isEmpty()) {
            try {
                Pattern.compile(rule.getFieldPattern());
            } catch (PatternSyntaxException e) {
                result.addError(MaskingConfigValidationError.builder()
                        .errorCode(ERR_INVALID_REGEX)
                        .ruleId(rule.getId())
                        .field(fieldRef)
                        .message("规则[" + index + "]的字段匹配模式正则表达式无效: " + e.getDescription())
                        .severity(MaskingConfigValidationError.Severity.ERROR)
                        .build());
            }
        }

        // 3. 检查策略类型
        if (rule.getStrategyType() == null || rule.getStrategyType().isEmpty()) {
            result.addError(MaskingConfigValidationError.builder()
                    .errorCode(ERR_MISSING_STRATEGY)
                    .ruleId(rule.getId())
                    .field(fieldRef)
                    .message("规则[" + index + "]必须指定脱敏策略类型(strategyType)")
                    .severity(MaskingConfigValidationError.Severity.ERROR)
                    .build());
        } else {
            MaskingStrategy strategy = strategyMap.get(rule.getStrategyType().toUpperCase());
            if (strategy == null) {
                result.addError(MaskingConfigValidationError.builder()
                        .errorCode(ERR_INVALID_STRATEGY)
                        .ruleId(rule.getId())
                        .field(fieldRef)
                        .message("规则[" + index + "]的脱敏策略类型不支持: " + rule.getStrategyType()
                                + ". 支持的类型: " + String.join(", ", strategyMap.keySet()))
                        .severity(MaskingConfigValidationError.Severity.ERROR)
                        .build());
            } else {
                // 4. 检查策略配置参数
                if (rule.getStrategyConfig() != null
                        && !strategy.validateConfig(rule.getStrategyConfig())) {
                    result.addError(MaskingConfigValidationError.builder()
                            .errorCode(ERR_INVALID_CONFIG)
                            .ruleId(rule.getId())
                            .field(fieldRef)
                            .message("规则[" + index + "]的策略配置参数无效(策略类型: " + rule.getStrategyType() + ")")
                            .severity(MaskingConfigValidationError.Severity.ERROR)
                            .build());
                }
            }
        }
    }

    /**
     * 检测冲突规则：同一字段名被多条规则精确匹配
     */
    void detectConflictingRules(List<MaskingRule> rules, MaskingConfigValidationResult result) {
        // 按精确字段名分组（只考虑启用的规则）
        Map<String, List<MaskingRule>> fieldNameGroups = rules.stream()
                .filter(r -> r != null && r.getFieldName() != null && !r.getFieldName().isEmpty())
                .filter(r -> r.getEnabled() == null || r.getEnabled())
                .collect(Collectors.groupingBy(r -> r.getFieldName().toLowerCase()));

        for (Map.Entry<String, List<MaskingRule>> entry : fieldNameGroups.entrySet()) {
            List<MaskingRule> group = entry.getValue();
            if (group.size() > 1) {
                // 检查是否有相同优先级的冲突
                Map<Integer, List<MaskingRule>> byPriority = group.stream()
                        .collect(Collectors.groupingBy(r -> r.getPriority() != null ? r.getPriority() : Integer.MAX_VALUE));

                for (Map.Entry<Integer, List<MaskingRule>> pEntry : byPriority.entrySet()) {
                    if (pEntry.getValue().size() > 1) {
                        // 同一字段、同一优先级的多条规则 → 警告
                        List<Long> ruleIds = pEntry.getValue().stream()
                                .map(MaskingRule::getId)
                                .collect(Collectors.toList());
                        result.addError(MaskingConfigValidationError.builder()
                                .errorCode(ERR_CONFLICTING_RULES)
                                .field(entry.getKey())
                                .message("字段'" + entry.getKey() + "'存在" + pEntry.getValue().size()
                                        + "条相同优先级(" + pEntry.getKey() + ")的规则(IDs: " + ruleIds
                                        + ")，可能导致不确定的脱敏行为")
                                .severity(MaskingConfigValidationError.Severity.WARNING)
                                .build());
                    }
                }
            }
        }
    }
}
