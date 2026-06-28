package com.dataplatform.data.vo;

import lombok.Data;
import java.util.Date;
import java.util.List;

/**
 * 用户视图对象
 * 用于前端展示，不包含敏感信息（如密码）
 * 
 * @author dataplatform
 */
@Data
public class UserVO {
    /** 用户ID */
    private Long id;
    
    /** 用户名 */
    private String username;
    
    /** 昵称 */
    private String nickname;
    
    /** 邮箱 */
    private String email;
    
    /** 头像URL */
    private String avatar;
    
    /** 状态：1-启用，0-禁用 */
    private Integer status;
    
    /** 创建时间 */
    private Date createTime;
    
    /** 更新时间 */
    private Date updateTime;
    
    /** 角色列表 */
    private List<String> roles;
    
    /** 权限列表 */
    private List<String> permissions;
}

