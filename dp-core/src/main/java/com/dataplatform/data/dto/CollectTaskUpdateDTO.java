package com.dataplatform.data.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

/**
 * 采集任务更新数据传输对象
 * 
 * @author dataplatform
 */
@Data
public class CollectTaskUpdateDTO {
    /** 任务ID */
    @NotNull(message = "任务ID不能为空")
    private Long id;

    /** 任务名称 */
    private String taskName;

    /** 源数据源ID */
    private Long dataSourceId;
    
    /** 目标数据源ID */
    private Long targetDataSourceId;

    /** 源表名 */
    private String tableName;
    
    /** 目标表名 */
    private String targetTableName;
    
    /** 采集模式 */
    private String collectMode;
    
    /** 自定义SQL查询 */
    private String customSql;
    
    /** 增量字段 */
    private String incrementalField;
    
    /** 增量字段类型 */
    private String incrementalType;
    
    /** 字段映射配置 */
    private String fieldMapping;
    
    /** 数据转换规则 */
    private String transformRules;
    
    /** 批量大小 */
    private Integer batchSize;
    
    /** 是否自动创建目标表 */
    private Boolean autoCreateTable;

    /** 状态：running-运行中，stopped-已停止，error-错误 */
    private String status;
    
    /** 是否启用定时任务 */
    private Boolean scheduleEnabled;
    
    /** Cron表达式（定时任务调度） */
    private String cronExpression;
    
    /** 定时任务描述 */
    private String scheduleDescription;
}
