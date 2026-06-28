package com.dataplatform.serviceapi.system.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 跨服务用户信息 DTO（轻量级，不暴露密码等敏感字段）
 */
@Data
public class UserInfoDTO implements Serializable {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String email;
    private String phone;
    private Long departmentId;
    private String departmentName;
    private String status;
}
