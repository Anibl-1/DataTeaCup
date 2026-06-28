package com.dataplatform.data.dto;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

/**
 * 重置密码DTO
 * 
 * @author dataplatform
 */
@Data
public class ResetPasswordDTO {
    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;
}

