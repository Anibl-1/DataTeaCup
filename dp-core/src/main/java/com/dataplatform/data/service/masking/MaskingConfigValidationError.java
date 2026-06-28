package com.dataplatform.data.service.masking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 脱敏配置验证错误
 * 表示单个配置验证问题的结构化信息
 *
 * @author dataplatform
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaskingConfigValidationError {

    /**
     * 错误码
     */
    private String errorCode;

    /**
     * 出错的规则ID（可为null）
     */
    private Long ruleId;

    /**
     * 出错的字段名或字段模式
     */
    private String field;

    /**
     * 人类可读的错误消息
     */
    private String message;

    /**
     * 错误严重级别
     */
    private Severity severity;

    /**
     * 错误严重级别枚举
     */
    public enum Severity {
        /** 错误 - 必须修复，阻止数据返回 */
        ERROR,
        /** 警告 - 建议修复，不阻止数据返回 */
        WARNING
    }
}
