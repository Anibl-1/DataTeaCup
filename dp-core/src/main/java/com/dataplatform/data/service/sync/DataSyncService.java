package com.dataplatform.data.service.sync;

import java.util.List;

/**
 * 数据同步服务接口
 * 需求: 18.2, 18.3
 */
public interface DataSyncService {

    /**
     * 创建同步任务
     */
    SyncTask createTask(SyncTask task);

    /**
     * 执行同步任务
     */
    SyncExecutionLog executeTask(String taskId);

    /**
     * 增量同步
     */
    SyncExecutionLog incrementalSync(SyncTask task);

    /**
     * 全量同步
     */
    SyncExecutionLog fullSync(SyncTask task);

    /**
     * 暂停任务
     */
    void pauseTask(String taskId);

    /**
     * 恢复任务
     */
    void resumeTask(String taskId);

    /**
     * 获取任务
     */
    SyncTask getTask(String taskId);

    /**
     * 获取所有任务
     */
    List<SyncTask> listTasks();

    /**
     * 获取执行日志
     */
    List<SyncExecutionLog> getExecutionLogs(String taskId, int limit);
}
