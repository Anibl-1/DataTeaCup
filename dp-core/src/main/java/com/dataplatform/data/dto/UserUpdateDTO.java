package com.dataplatform.data.dto;

import lombok.Data;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 用户更新数据传输对象
 * 
 * @author dataplatform
 */
@Data
public class UserUpdateDTO {
    /** 用户ID */
    @NotNull(message = "用户ID不能为空")
    private Long id;

    /** 用户名 */
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50个字符之间")
    private String username;

    /** 密码（编辑时可选，不传则不修改） */
    @Size(min = 6, max = 20, message = "密码长度必须在6-20个字符之间")
    private String password;

    /** 密码为空字符串时视为null，跳过校验 */
    public void setPassword(String password) {
        this.password = (password != null && password.isEmpty()) ? null : password;
    }

    /** 昵称 */
    private String nickname;

    /** 邮箱 */
    @Email(message = "邮箱格式不正确")
    private String email;

    /** 头像URL */
    private String avatar;

    /** 部门ID */
    private Long deptId;

    /** 手机号 */
    private String phone;

    /** 性别: 0-未知 1-男 2-女 */
    private Integer gender;

    /** 状态：1-启用，0-禁用 */
    private Integer status;
}

