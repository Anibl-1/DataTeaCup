package com.dataplatform.data.service;

import com.dataplatform.common.exception.BusinessException;
import com.dataplatform.common.exception.ErrorCode;
import com.dataplatform.data.entity.CollectTask;
import com.dataplatform.data.mapper.CollectTaskMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
// 注：定时调度已移至统一调度器UnifiedScheduler
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时任务调度服务
 * 负责执行数据采集的定时任务
 * 
 * @author dataplatform
 */
@Slf4j
@Service
public class ScheduledTaskService {
    
    @Autowired
    private CollectTaskMapper collectTaskMapper;
    
    @Autowired
    private DataCollectService dataCollectService;
    
    /**
     * 检查并执行定时任务（已废弃，使用CollectTaskScheduler替代）
     * @deprecated 请使用 CollectTaskScheduler.checkAndExecuteTasksInternal()
     */
    @Deprecated
    public void checkAndExecuteScheduledTasks() {
        try {
            // 查询所有启用定时任务且到达执行时间的任务
            List<CollectTask> tasks = collectTaskMapper.selectScheduledTasksToExecute();
            
            if (tasks == null || tasks.isEmpty()) {
                return;
            }
            
            log.info("发现 {} 个待执行的定时任务", tasks.size());
            
            for (CollectTask task : tasks) {
                try {
                    executeScheduledTask(task);
                } catch (Exception e) {
                    log.error("执行定时任务失败: taskId={}, taskName={}, error={}", 
                        task.getId(), task.getTaskName(), e.getMessage());
                    // 更新失败次数
                    updateTaskFailCount(task.getId());
                }
            }
        } catch (Exception e) {
            log.error("检查定时任务时发生错误: {}", e.getMessage());
        }
    }
    
    /**
     * 执行单个定时任务
     */
    private void executeScheduledTask(CollectTask task) {
        log.info("开始执行定时任务: taskId={}, taskName={}", task.getId(), task.getTaskName());
        
        try {
            // 调用采集任务服务执行任务
            dataCollectService.startCollectTask(task.getId());
            
            // 更新执行统计
            updateTaskSuccessCount(task.getId());
            
            // 计算并更新下次执行时间
            updateNextExecuteTime(task);
            
            log.info("定时任务执行成功: taskId={}, taskName={}", task.getId(), task.getTaskName());
        } catch (Exception e) {
            log.error("定时任务执行失败: taskId={}, taskName={}, error={}", 
                task.getId(), task.getTaskName(), e.getMessage());
            updateTaskFailCount(task.getId());
            throw e;
        }
    }
    
    /**
     * 更新任务成功次数
     */
    private void updateTaskSuccessCount(Long taskId) {
        try {
            collectTaskMapper.incrementSuccessCount(taskId);
            collectTaskMapper.incrementExecuteCount(taskId);
        } catch (Exception e) {
            log.error("更新任务成功次数失败: taskId={}", taskId);
        }
    }
    
    /**
     * 更新任务失败次数
     */
    private void updateTaskFailCount(Long taskId) {
        try {
            collectTaskMapper.incrementFailCount(taskId);
            collectTaskMapper.incrementExecuteCount(taskId);
        } catch (Exception e) {
            log.error("更新任务失败次数失败: taskId={}", taskId);
        }
    }
    
    /**
     * 计算并更新下次执行时间
     */
    private void updateNextExecuteTime(CollectTask task) {
        try {
            if (task.getCronExpression() == null || task.getCronExpression().isEmpty()) {
                return;
            }
            
            CronExpression cronExpression = CronExpression.parse(task.getCronExpression());
            LocalDateTime nextTime = cronExpression.next(LocalDateTime.now());
            
            if (nextTime != null) {
                collectTaskMapper.updateNextExecuteTime(task.getId(), nextTime);
                log.info("更新下次执行时间: taskId={}, nextTime={}", task.getId(), nextTime);
            }
        } catch (Exception e) {
            log.error("计算下次执行时间失败: taskId={}, cron={}, error={}", 
                task.getId(), task.getCronExpression(), e.getMessage());
        }
    }
    
    /**
     * 手动触发任务执行（用于测试）
     */
    public void triggerTask(Long taskId) {
        CollectTask task = collectTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.COLLECT_TASK_NOT_FOUND, "任务不存在: " + taskId);
        }
        executeScheduledTask(task);
    }
}
