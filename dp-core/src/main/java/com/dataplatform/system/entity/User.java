package com.dataplatform.system.entity;

import lombok.Data;
import java.util.Date;

/**
 * 用户实体类
 * 
 * @author dataplatform
 */
@Data
public class User {
    /** 用户ID */
    private Long id;
    
    /** 用户名 */
    private String username;
    
    /** 密码（MD5加密） */
    private String password;
    
    /** 昵称 */
    private String nickname;
    
    /** 邮箱 */
    private String email;
    
    /** 头像URL */
    private String avatar;
    
    /** 部门ID */
    private Long deptId;
    
    /** 岗位ID */
    private Long postId;
    
    /** 岗位名称（关联查询，非持久化字段） */
    private transient String postName;
    
    /** 岗位状态（关联查询，非持久化字段） */
    private transient Integer postStatus;
    
    /** 角色ID */
    private Long roleId;
    
    /** 手机号 */
    private String phone;
    
    /** 性别: 0-未知 1-男 2-女 */
    private Integer gender;
    
    /** 状态：1-启用，0-禁用 */
    private Integer status;
    
    /** 是否必须修改密码（1-是，0-否） */
    private Integer mustChangePassword;
    
    /** 创建时间 */
    private Date createTime;
    
    /** 更新时间 */
    private Date updateTime;
}
