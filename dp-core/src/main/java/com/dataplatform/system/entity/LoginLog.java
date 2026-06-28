package com.dataplatform.system.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 登录日志实体
 */
@Data
public class LoginLog {
    private Long id;
    private String username;
    private String ipAddress;
    private String userAgent;
    private String browser;
    private String os;
    /** success / failure */
    private String status;
    private String message;
    private LocalDateTime loginTime;
}
