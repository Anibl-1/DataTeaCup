package com.dataplatform.data.entity;

import lombok.Data;
import java.util.Date;

/**
 * SQL执行历史记录实体
 */
@Data
public class SqlHistory {
    /** 记录ID */
    private Long id;
    
    /** 会话ID */
    private String sessionId;
    
    /** 数据库类型 */
    private String dbType;
    
    /** 数据库名称 */
    private String dbName;
    
    /** 执行的SQL语句 */
    private String sqlContent;
    
    /** SQL类型（SELECT/INSERT/UPDATE/DELETE/DDL等） */
    private String sqlType;
    
    /** 执行状态（success/failed） */
    private String status;
    
    /** 影响行数 */
    private Integer affectedRows;
    
    /** 执行耗时（毫秒） */
    private Long executeTime;
    
    /** 错误信息 */
    private String errorMessage;
    
    /** 执行时间 */
    private Date executeAt;
    
    /** 创建时间 */
    private Date createTime;
}
