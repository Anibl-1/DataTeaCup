package com.dataplatform.data.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.util.Date;

/**
 * 流程执行记录实体
 */
@Data
public class PipelineExecution {
    private Long id;
    private Long pipelineId;
    private String pipelineName;
    private String executionNo;
    private Integer triggerType; // 1-手动, 2-定时, 3-事件
    private Integer status; // 0-失败, 1-成功, 2-运行中, 3-已取消
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date endTime;
    
    private Long duration;
    private Long inputCount;
    private Long outputCount;
    private Long errorCount;
    private String errorMessage;
    private String executeLog;
    private Long executeBy;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
    
    // 非数据库字段
    private transient String executeByName;
    private transient String statusText;
    private transient String triggerTypeText;
}
