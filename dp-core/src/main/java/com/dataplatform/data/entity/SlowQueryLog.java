package com.dataplatform.data.entity;

import lombok.Data;
import java.util.Date;

/**
 * 慢查询日志实体
 * 对应表 sys_slow_query_log
 */
@Data
public class SlowQueryLog {
    private Long id;
    /** 数据源ID */
    private Long dataSourceId;
    /** 数据源名称 */
    private String dataSourceName;
    /** SQL语句 */
    private String sqlText;
    /** SQL哈希 */
    private String sqlHash;
    /** 执行时间(ms) */
    private Long executionTime;
    /** 扫描行数 */
    private Long rowsExamined;
    /** 返回行数 */
    private Long rowsReturned;
    /** 查询时间 */
    private Date queryTime;
    /** 用户名 */
    private String userName;
    /** 客户端IP */
    private String clientIp;
    /** 数据库名 */
    private String databaseName;
    private Date createTime;
}
