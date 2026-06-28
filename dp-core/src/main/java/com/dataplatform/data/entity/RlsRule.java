package com.dataplatform.data.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 行级权限规则实体
 * 
 * @author dataplatform
 */
@Data
@TableName("rls_rule")
public class RlsRule {
    
    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 角色ID */
    private Long roleId;
    
    /** 数据源ID */
    private Long dataSourceId;
    
    /** 表名 */
    private String tableName;
    
    /** 过滤字段 */
    private String filterField;
    
    /** 操作符（=, !=, >, <, IN, LIKE等） */
    private String filterOperator;
    
    /** 过滤值（支持变量如 ${user.deptId}） */
    private String filterValue;
    
    /** 创建时间 */
    private LocalDateTime createTime;
}
