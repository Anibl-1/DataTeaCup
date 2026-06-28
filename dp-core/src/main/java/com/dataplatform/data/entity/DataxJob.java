package com.dataplatform.data.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.util.Date;

/**
 * 数据传输任务实体
 */
@Data
public class DataxJob {
    private Long id;
    private String jobName;
    private String jobDesc;
    private Integer jobType; // 1-数据库同步, 2-文件传输
    private Long sourceDataSourceId;
    private String sourceTable;
    private String sourceQuerySql;
    private Long targetDataSourceId;
    private String targetTable;
    private String writeMode; // insert, update, replace
    private String columnMapping; // JSON格式字段映射
    private String cronExpression;
    private Integer jobStatus; // 0-停止, 1-运行中
    private String scheduleStatus; // running/paused/stopped - 调度状态
    private Integer incrementType; // 0-全量, 1-增量
    private String incrementColumn;
    private String incrementValue;
    private String lastIncrementValue; // 上次增量同步的值
    private Integer channelCount;
    
    /**
     * 批量提交大小（默认1000）
     */
    private Integer batchSize;
    
    /**
     * 执行引擎类型: jdbc-内置JDBC流式引擎, datax-DataX进程引擎
     * 默认jdbc
     */
    private String engineType;
    
    /**
     * DataX Home 路径（仅 datax 引擎使用，为空则取全局配置）
     */
    private String dataxHome;
    
    /**
     * 参数定义（JSON格式）
     * 示例: [{"name":"startDate","label":"开始日期","type":"date","required":true},{"name":"endDate","label":"结束日期","type":"date"}]
     */
    private String parameterDefinition;
    
    /**
     * 默认参数值（JSON格式）
     * 示例: {"startDate":"2024-01-01","endDate":"2024-12-31"}
     */
    private String defaultParameters;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date lastExecuteTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;
    
    private Long createBy;
    private Integer delFlag;
    
    // 非数据库字段
    private transient String sourceDataSourceName;
    private transient String targetDataSourceName;
}
