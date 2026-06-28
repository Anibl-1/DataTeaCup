package com.dataplatform.data.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.util.Date;

/**
 * 数据传输任务执行日志
 */
@Data
public class DataxJobLog {
    private Long id;
    private Long jobId;
    private String jobName;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date endTime;
    
    private Integer status; // 0-失败, 1-成功, 2-运行中 (兼容老字段名)
    private Integer executeStatus; // 0-失败, 1-成功, 2-运行中
    private Integer triggerType; // 1-手动, 2-定时
    private Long readCount;
    private Long writeCount;
    private Long errorCount;
    private String errorMessage;
    private String errorMsg; // 兼容老字段名
    private String executeLog;
    private String executeParams; // 执行时传入的参数（JSON格式）
    private Long duration; // 执行时长(毫秒)
    private Long executeTime; // 执行时长(秒)
    
    // 兼容处理
    public Integer getExecuteStatus() {
        return executeStatus != null ? executeStatus : status;
    }
    
    public void setStatus(Integer status) {
        this.status = status;
        this.executeStatus = status;
    }
    
    public String getErrorMsg() {
        return errorMsg != null ? errorMsg : errorMessage;
    }
    
    public Long getExecuteTime() {
        if (executeTime != null) return executeTime;
        return duration != null ? duration / 1000 : null;
    }
}
