package com.dataplatform.org.entity;

import lombok.Data;
import java.util.Date;
import java.util.List;

/**
 * 部门实体类
 * 
 * @author dataplatform
 */
@Data
public class Department {
    /** 部门ID */
    private Long id;
    
    /** 部门名称 */
    private String deptName;
    
    /** 部门编码 */
    private String deptCode;
    
    /** 父部门ID，0为顶级部门 */
    private Long parentId;
    
    /** 祖级列表（如 0,1,2） */
    private String ancestors;
    
    /** 负责人 */
    private String leader;
    
    /** 联系电话 */
    private String phone;
    
    /** 邮箱 */
    private String email;
    
    /** 排序 */
    private Integer sortOrder;
    
    /** 状态: 0-禁用 1-启用 */
    private Integer status;
    
    /** 删除标志: 0-正常 1-删除 */
    private Integer delFlag;
    
    /** 创建时间 */
    private Date createTime;
    
    /** 更新时间 */
    private Date updateTime;
    
    /** 子部门列表（非数据库字段） */
    private transient List<Department> children;
    
    /** 父部门名称（非数据库字段） */
    private transient String parentName;
}
