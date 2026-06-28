package com.dataplatform.data.entity;

import java.util.Date;

/**
 * 采集日志实体
 */
public class CollectLog {
    private Long id;
    private Long taskId;
    private String taskName;
    private String sourceTable;
    private String targetTable;
    private String status; // running, success, failed
    private Integer rowCount;
    private Date startTime;
    private Date endTime;
    private Long duration; // 毫秒
    private String errorMessage;
    private String executeSql;
    private Date createTime;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }

    public String getSourceTable() { return sourceTable; }
    public void setSourceTable(String sourceTable) { this.sourceTable = sourceTable; }

    public String getTargetTable() { return targetTable; }
    public void setTargetTable(String targetTable) { this.targetTable = targetTable; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getRowCount() { return rowCount; }
    public void setRowCount(Integer rowCount) { this.rowCount = rowCount; }

    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }

    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }

    public Long getDuration() { return duration; }
    public void setDuration(Long duration) { this.duration = duration; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getExecuteSql() { return executeSql; }
    public void setExecuteSql(String executeSql) { this.executeSql = executeSql; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
