package com.dataplatform.data.service.masking;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 脱敏配置验证结果
 * 聚合所有验证错误，提供便捷的查询方法
 *
 * @author dataplatform
 */
@Data
public class MaskingConfigValidationResult {

    private final List<MaskingConfigValidationError> errors = new ArrayList<>();

    /**
     * 添加一个验证错误
     */
    public void addError(MaskingConfigValidationError error) {
        errors.add(error);
    }

    /**
     * 是否存在阻止数据返回的错误（ERROR级别）
     */
    public boolean hasErrors() {
        return errors.stream()
                .anyMatch(e -> e.getSeverity() == MaskingConfigValidationError.Severity.ERROR);
    }

    /**
     * 是否存在警告
     */
    public boolean hasWarnings() {
        return errors.stream()
                .anyMatch(e -> e.getSeverity() == MaskingConfigValidationError.Severity.WARNING);
    }

    /**
     * 是否完全通过验证（无错误也无警告）
     */
    public boolean isValid() {
        return errors.isEmpty();
    }

    /**
     * 获取所有ERROR级别的错误
     */
    public List<MaskingConfigValidationError> getBlockingErrors() {
        return errors.stream()
                .filter(e -> e.getSeverity() == MaskingConfigValidationError.Severity.ERROR)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有WARNING级别的错误
     */
    public List<MaskingConfigValidationError> getWarnings() {
        return errors.stream()
                .filter(e -> e.getSeverity() == MaskingConfigValidationError.Severity.WARNING)
                .collect(Collectors.toList());
    }

    /**
     * 获取格式化的错误摘要
     */
    public String getSummary() {
        if (isValid()) {
            return "验证通过";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("脱敏配置验证失败: ");
        List<MaskingConfigValidationError> blocking = getBlockingErrors();
        List<MaskingConfigValidationError> warnings = getWarnings();
        if (!blocking.isEmpty()) {
            sb.append(blocking.size()).append("个错误");
        }
        if (!warnings.isEmpty()) {
            if (!blocking.isEmpty()) sb.append(", ");
            sb.append(warnings.size()).append("个警告");
        }
        sb.append(". ");
        for (MaskingConfigValidationError e : errors) {
            sb.append("[").append(e.getErrorCode()).append("] ").append(e.getMessage()).append("; ");
        }
        return sb.toString().trim();
    }
}
