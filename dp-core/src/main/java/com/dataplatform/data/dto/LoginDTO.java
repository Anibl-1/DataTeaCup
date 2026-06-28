package com.dataplatform.data.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

/**
 * 登录数据传输对象
 * 
 * @author dataplatform
 */
@Data
public class LoginDTO {
    /** 用户名 */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /** 密码 */
    @NotBlank(message = "密码不能为空")
    private String password;

    /** 验证码key（验证码启用时必填） */
    private String captchaKey;

    /** 验证码（验证码启用时必填） */
    private String captchaCode;
}

