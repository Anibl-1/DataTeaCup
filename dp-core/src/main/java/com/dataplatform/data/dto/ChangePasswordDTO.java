package com.dataplatform.data.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 修改密码DTO
 * 
 * @author dataplatform
 */
@Data
public class ChangePasswordDTO {
    /**
     * 旧密码
     */
    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;
    
    /**
     * 新密码
     */
    @NotBlank(message = "新密码不能为空")
    private String newPassword;
}

