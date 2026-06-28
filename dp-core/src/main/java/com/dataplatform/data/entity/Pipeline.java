package com.dataplatform.data.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.util.Date;

/**
 * 数据处理流程实体
 */
@Data
public class Pipeline {
    private Long id;
    private String pipelineName;
    private String pipelineCode;
    private String pipelineDesc;
    private Integer pipelineType; // 1-ETL流程, 2-数据清洗, 3-数据同步, 4-数据聚合
    private String flowJson; // 流程图JSON配置
    private String cronExpression;
    private Integer scheduleType; // 0-手动, 1-定时, 2-事件触发
    private Integer pipelineStatus; // 0-草稿, 1-已发布, 2-已停用
    private Integer version;
    private Integer timeoutSeconds;
    private Integer retryCount;
    private Integer alertOnFailure;
    private String scheduleStatus; // running/paused/stopped - 调度状态
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date lastExecuteTime;
    
    private Integer lastExecuteStatus;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;
    
    private Long createBy;
    private Integer delFlag;
    
    // 非数据库字段
    private transient String createByName;
    private transient String statusText;
    private transient String scheduleTypeText;
}
