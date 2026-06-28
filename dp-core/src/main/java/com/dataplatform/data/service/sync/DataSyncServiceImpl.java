package com.dataplatform.data.service.sync;

import com.dataplatform.data.mapper.SyncExecutionLogMapper;
import com.dataplatform.data.mapper.SyncTaskMapper;
import com.dataplatform.data.service.connector.ConnectorConfig;
import com.dataplatform.data.service.connector.ConnectorManager;
import com.dataplatform.data.service.connector.ConnectorResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.regex.Pattern;

/**
 * 数据同步服务实现
 * 需求: 18.2, 18.3, 18.5, 18.6, 18.7, 18.8
 */
@Slf4j
@Service
public class DataSyncServiceImpl implements DataSyncService {

    @Autowired
    private ConnectorManager connectorManager;

    @Autowired(required = false)
    private SyncTaskMapper syncTaskMapper;

    @Autowired(required = false)
    private SyncExecutionLogMapper syncExecutionLogMapper;

    // 内存回退存储（当Mapper不可用时）
    private final Map<String, SyncTask> taskMap = new ConcurrentHashMap<>();
    private final Map<String, Deque<SyncExecutionLog>> executionLogs = new ConcurrentHashMap<>();

    private static final Pattern VALID_TABLE_NAME = Pattern.compile("^[a-zA-Z0-9_.]+$");

    @Override
    public SyncTask createTask(SyncTask task) {
        // 校验sourceTable防止注入
        if (task.getSourceTable() != null && !VALID_TABLE_NAME.matcher(task.getSourceTable()).matches()) {
            throw new IllegalArgumentException("sourceTable名称不合法，仅允许字母、数字、下划线和点号");
        }
        if (task.getId() == null || task.getId().isEmpty()) {
            task.setId(UUID.randomUUID().toString().substring(0, 12));
        }
        task.setStatus("CREATED");
        task.setCreatedAt(LocalDateTime.now());
        if (syncTaskMapper != null) {
            try {
                syncTaskMapper.insert(task);
            } catch (Exception e) {
                log.warn("[数据同步] 持久化任务失败，降级到内存: {}", e.getMessage());
                taskMap.put(task.getId(), task);
            }
        } else {
            taskMap.put(task.getId(), task);
        }
        log.info("[数据同步] 创建任务: id={}, name={}, mode={}", task.getId(), task.getName(), task.getSyncMode());
        return task;
    }

    @Override
    public SyncExecutionLog executeTask(String taskId) {
        SyncTask task = getTask(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }
        if ("PAUSED".equals(task.getStatus())) {
            throw new IllegalStateException("任务已暂停: " + taskId);
        }

        // 检查依赖任务
        if (!checkDependencies(task)) {
            throw new IllegalStateException("依赖任务未完成");
        }

        return "incremental".equals(task.getSyncMode())
                ? incrementalSync(task)
                : fullSync(task);
    }

    @Override
    public SyncExecutionLog incrementalSync(SyncTask task) {
        SyncExecutionLog execLog = startExecution(task);
        task.setStatus("RUNNING");

        try {
            ConnectorConfig config = buildConnectorConfig(task);
            Map<String, Object> params = new HashMap<>();
            params.put("syncField", task.getSyncField());
            params.put("lastSyncTime", task.getLastRunAt());

            ConnectorResult result = connectorManager.readData(config, task.getSourceTable(), params);
            long syncedRows = result.getTotalRows();

            completeExecution(execLog, syncedRows, "SUCCESS", null);
            task.setStatus("COMPLETED");
            task.setLastRunAt(LocalDateTime.now());
            log.info("[增量同步] 完成: taskId={}, rows={}", task.getId(), syncedRows);
        } catch (Exception e) {
            log.warn("[增量同步] 首次执行失败, 开始重试: taskId={}, error={}", task.getId(), e.getMessage());
            boolean retrySuccess = false;
            for (int retry = 1; retry <= task.getMaxRetries(); retry++) {
                try {
                    long sleepMs = Math.min(task.getRetryIntervalMs() * retry, 30000L);
                    Thread.sleep(sleepMs);
                    log.info("[增量同步] 第{}次重试: taskId={}", retry, task.getId());
                    ConnectorConfig config = buildConnectorConfig(task);
                    ConnectorResult result = connectorManager.readData(config, task.getSourceTable(), Map.of());
                    completeExecution(execLog, result.getTotalRows(), "SUCCESS", null);
                    execLog.setRetryCount(retry);
                    task.setStatus("COMPLETED");
                    task.setLastRunAt(LocalDateTime.now());
                    log.info("[增量同步] 第{}次重试成功: taskId={}", retry, task.getId());
                    retrySuccess = true;
                    break;
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    completeExecution(execLog, 0, "FAILED", "同步被中断");
                    execLog.setRetryCount(retry);
                    task.setStatus("FAILED");
                    retrySuccess = true;
                    break;
                } catch (Exception retryEx) {
                    log.warn("[增量同步] 第{}次重试失败: taskId={}, error={}", retry, task.getId(), retryEx.getMessage());
                    if (retry == task.getMaxRetries()) {
                        completeExecution(execLog, 0, "FAILED", retryEx.getMessage());
                        execLog.setRetryCount(retry);
                        task.setStatus("FAILED");
                    }
                }
            }
            if (!retrySuccess && task.getMaxRetries() <= 0) {
                completeExecution(execLog, 0, "FAILED", e.getMessage());
                task.setStatus("FAILED");
            }
        }

        return execLog;
    }

    @Override
    public SyncExecutionLog fullSync(SyncTask task) {
        SyncExecutionLog execLog = startExecution(task);
        task.setStatus("RUNNING");

        try {
            ConnectorConfig config = buildConnectorConfig(task);
            Map<String, Object> params = new HashMap<>();
            params.put("batchSize", task.getBatchSize());

            long totalSynced = 0;
            boolean hasMore = true;
            int offset = 0;

            while (hasMore) {
                params.put("offset", offset);
                ConnectorResult result = connectorManager.readData(config, task.getSourceTable(), params);
                totalSynced += result.getTotalRows();
                hasMore = result.isHasMore();
                offset += task.getBatchSize();
            }

            completeExecution(execLog, totalSynced, "SUCCESS", null);
            task.setStatus("COMPLETED");
            task.setLastRunAt(LocalDateTime.now());
            log.info("[全量同步] 完成: taskId={}, rows={}", task.getId(), totalSynced);
        } catch (Exception e) {
            completeExecution(execLog, 0, "FAILED", e.getMessage());
            task.setStatus("FAILED");
            log.error("[全量同步] 失败: taskId={}", task.getId(), e);
        }

        return execLog;
    }

    @Override
    public void pauseTask(String taskId) {
        SyncTask task = getTask(taskId);
        if (task != null) {
            task.setStatus("PAUSED");
            persistTaskStatus(taskId, "PAUSED");
            log.info("[数据同步] 暂停任务: {}", taskId);
        }
    }

    @Override
    public void resumeTask(String taskId) {
        SyncTask task = getTask(taskId);
        if (task != null && "PAUSED".equals(task.getStatus())) {
            task.setStatus("CREATED");
            persistTaskStatus(taskId, "CREATED");
            log.info("[数据同步] 恢复任务: {}", taskId);
        }
    }

    @Override
    public SyncTask getTask(String taskId) {
        if (syncTaskMapper != null) {
            try {
                SyncTask task = syncTaskMapper.selectById(taskId);
                if (task != null) return task;
            } catch (Exception e) {
                log.debug("[数据同步] 从DB查询任务失败，降级到内存: {}", e.getMessage());
            }
        }
        return taskMap.get(taskId);
    }

    @Override
    public List<SyncTask> listTasks() {
        if (syncTaskMapper != null) {
            try {
                List<SyncTask> tasks = syncTaskMapper.selectAll();
                if (tasks != null) return tasks;
            } catch (Exception e) {
                log.debug("[数据同步] 从DB查询任务列表失败，降级到内存: {}", e.getMessage());
            }
        }
        return new ArrayList<>(taskMap.values());
    }

    @Override
    public List<SyncExecutionLog> getExecutionLogs(String taskId, int limit) {
        if (syncExecutionLogMapper != null) {
            try {
                List<SyncExecutionLog> logs = syncExecutionLogMapper.selectByTaskId(taskId, limit);
                if (logs != null) return logs;
            } catch (Exception e) {
                log.debug("[数据同步] 从DB查询执行日志失败，降级到内存: {}", e.getMessage());
            }
        }
        Deque<SyncExecutionLog> logs = executionLogs.get(taskId);
        if (logs == null) return Collections.emptyList();
        List<SyncExecutionLog> result = new ArrayList<>(logs);
        if (result.size() > limit) {
            return result.subList(result.size() - limit, result.size());
        }
        return result;
    }

    private boolean checkDependencies(SyncTask task) {
        for (String depId : task.getDependsOn()) {
            SyncTask dep = getTask(depId);
            if (dep == null || !"COMPLETED".equals(dep.getStatus())) {
                return false;
            }
        }
        return true;
    }

    private ConnectorConfig buildConnectorConfig(SyncTask task) {
        ConnectorConfig config = new ConnectorConfig();
        config.setType(task.getSourceConnectorType());
        config.setProperties(task.getSourceConfig());
        return config;
    }

    private SyncExecutionLog startExecution(SyncTask task) {
        SyncExecutionLog execLog = new SyncExecutionLog();
        execLog.setId(UUID.randomUUID().toString().substring(0, 12));
        execLog.setTaskId(task.getId());
        execLog.setTaskName(task.getName());
        execLog.setSyncMode(task.getSyncMode());
        execLog.setStartTime(LocalDateTime.now());
        execLog.setStatus("RUNNING");
        return execLog;
    }

    private void completeExecution(SyncExecutionLog execLog, long syncedRows, String status, String error) {
        execLog.setSyncedRows(syncedRows);
        execLog.setStatus(status);
        execLog.setErrorMessage(error);
        execLog.setEndTime(LocalDateTime.now());
        execLog.setDurationMs(java.time.Duration.between(execLog.getStartTime(), execLog.getEndTime()).toMillis());

        // 持久化执行日志
        if (syncExecutionLogMapper != null) {
            try {
                syncExecutionLogMapper.insert(execLog);
            } catch (Exception e) {
                log.warn("[数据同步] 持久化执行日志失败，降级到内存: {}", e.getMessage());
                executionLogs.computeIfAbsent(execLog.getTaskId(), k -> new ConcurrentLinkedDeque<>()).addLast(execLog);
            }
        } else {
            executionLogs.computeIfAbsent(execLog.getTaskId(), k -> new ConcurrentLinkedDeque<>()).addLast(execLog);
        }
    }

    private void persistTaskStatus(String taskId, String status) {
        if (syncTaskMapper != null) {
            try {
                syncTaskMapper.updateStatus(taskId, status);
            } catch (Exception e) {
                log.debug("[数据同步] 更新任务状态失败: {}", e.getMessage());
            }
        }
    }
}
