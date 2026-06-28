package com.dataplatform.data.service.sync;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 数据同步任务定义
 */
@Data
public class SyncTask {
    private String id;
    private String name;
    private String sourceConnectorType;
    private Map<String, String> sourceConfig = new HashMap<>();
    private String sourceTable;
    private String targetTable;
    private String syncMode; // incremental, full
    private String syncField; // 增量字段（时间戳或自增ID）
    private int batchSize = 1000;
    private int maxRetries = 3;
    private long retryIntervalMs = 5000;
    private String cronExpression;
    private List<String> dependsOn = new ArrayList<>(); // 依赖的任务ID
    private String status; // CREATED, RUNNING, PAUSED, COMPLETED, FAILED
    private LocalDateTime createdAt;
    private LocalDateTime lastRunAt;
}
