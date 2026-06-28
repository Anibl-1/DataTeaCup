package com.dataplatform.data.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 采集任务创建数据传输对象
 * 
 * @author dataplatform
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CollectTaskCreateDTO {
    /** 任务名称 */
    @NotBlank(message = "任务名称不能为空")
    private String taskName;

    /** 源数据源ID */
    @NotNull(message = "数据源ID不能为空")
    private Long dataSourceId;

    /** 目标数据源ID（可选，为空则导入到本地数据库） */
    private Long targetDataSourceId;

    /** 源表名 */
    private String tableName;
    
    /** 目标表名（可选，为空则使用源表名） */
    private String targetTableName;
    
    /** 采集模式：full-全量采集，incremental-增量采集，custom-自定义SQL */
    private String collectMode;
    
    /** 自定义SQL查询（当collectMode=custom时必填） */
    private String customSql;
    
    /** 增量字段（当collectMode=incremental时必填） */
    private String incrementalField;
    
    /** 增量字段类型（timestamp-时间戳，id-自增ID） */
    private String incrementalType;
    
    /** 字段映射配置（JSON格式） */
    private String fieldMapping;
    
    /** 数据转换规则（JSON格式） */
    private String transformRules;
    
    /** 批量大小（默认1000） */
    private Integer batchSize;
    
    /** 是否自动创建目标表（默认true） */
    private Boolean autoCreateTable;
    
    /** 是否启用定时任务 */
    private Boolean scheduleEnabled;
    
    /** Cron表达式（定时任务调度） */
    private String cronExpression;
    
    /** 定时任务描述 */
    private String scheduleDescription;
}

