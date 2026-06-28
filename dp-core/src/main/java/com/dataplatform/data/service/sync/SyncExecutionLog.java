package com.dataplatform.data.service.sync;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 同步执行日志
 * 需求: 18.7
 */
@Data
public class SyncExecutionLog {
    private String id;
    private String taskId;
    private String taskName;
    private String syncMode;
    private long syncedRows;
    private long durationMs;
    private String status; // SUCCESS, FAILED, PARTIAL
    private String errorMessage;
    private int retryCount;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
