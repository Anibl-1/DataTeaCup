package com.dataplatform.data.entity;

import lombok.Data;
import java.util.Date;

/**
 * 采集任务实体类
 * 
 * @author dataplatform
 */
@Data
public class CollectTask {
    /** 任务ID */
    private Long id;
    
    /** 任务名称 */
    private String taskName;
    
    /** 源数据源ID */
    private Long dataSourceId;
    
    /** 源数据源名称（冗余字段，便于展示） */
    private String dataSourceName;
    
    /** 目标数据源ID（可选，为空则导入到本地数据库） */
    private Long targetDataSourceId;
    
    /** 目标数据源名称（冗余字段，便于展示） */
    private String targetDataSourceName;
    
    /** 源表名 */
    private String tableName;
    
    /** 目标表名（可选，为空则使用源表名） */
    private String targetTableName;
    
    /** 采集模式：full-全量采集，incremental-增量采集，custom-自定义SQL */
    private String collectMode;
    
    /** 自定义SQL查询（当collectMode=custom时使用） */
    private String customSql;
    
    /** 增量字段（当collectMode=incremental时使用，如：update_time） */
    private String incrementalField;
    
    /** 增量字段类型（timestamp-时间戳，id-自增ID） */
    private String incrementalType;
    
    /** 上次采集值（记录上次采集的时间戳或ID） */
    private String lastCollectValue;
    
    /** 字段映射配置（JSON格式，源字段->目标字段） */
    private String fieldMapping;
    
    /** 数据转换规则（JSON格式） */
    private String transformRules;
    
    /** 批量大小（每批次插入的记录数，默认1000） */
    private Integer batchSize;
    
    /** 是否自动创建目标表 */
    private Boolean autoCreateTable;
    
    /** 状态：running-运行中，stopped-已停止，error-错误 */
    private String status;
    
    /** 是否启用定时任务 */
    private Boolean scheduleEnabled;
    
    /** Cron表达式（定时任务调度） */
    private String cronExpression;
    
    /** 定时任务描述（如：每天凌晨2点执行） */
    private String scheduleDescription;
    
    /** 下次执行时间 */
    private Date nextExecuteTime;
    
    /** 上次执行时间 */
    private Date lastExecuteTime;
    
    /** 上次执行结果（成功/失败/记录数） */
    private String lastExecuteResult;
    
    /** 执行次数统计 */
    private Integer executeCount;
    
    /** 成功次数统计 */
    private Integer successCount;
    
    /** 失败次数统计 */
    private Integer failCount;
    
    /** 最大重试次数（0表示不重试，默认3） */
    private Integer maxRetryCount;
    
    /** 重试间隔（秒，默认30） */
    private Integer retryInterval;
    
    /** 创建时间 */
    private Date createTime;
    
    /** 更新时间 */
    private Date updateTime;
}

