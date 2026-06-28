package com.dataplatform.data.service.masking;

import lombok.Getter;

/**
 * 脱敏配置异常
 * 当脱敏规则配置存在阻止数据返回的错误时抛出
 *
 * **Validates: Requirements 6.5**
 *
 * @author dataplatform
 */
@Getter
public class MaskingConfigException extends RuntimeException {

    private final MaskingConfigValidationResult validationResult;

    public MaskingConfigException(MaskingConfigValidationResult validationResult) {
        super(validationResult.getSummary());
        this.validationResult = validationResult;
    }

    public MaskingConfigException(String message, MaskingConfigValidationResult validationResult) {
        super(message);
        this.validationResult = validationResult;
    }
}
