package com.dataplatform.data.scheduler;

import com.dataplatform.data.entity.CollectTask;
import com.dataplatform.data.mapper.CollectTaskMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 采集任务调度器
 * 每分钟检查一次需要执行的定时任务
 */
@Slf4j
@Component
public class CollectTaskScheduler {

    @Autowired
    private CollectTaskMapper taskMapper;
    
    // 不直接注入DataCollectService，避免循环依赖
    // 通过ApplicationContext获取
    @Autowired
    private org.springframework.context.ApplicationContext applicationContext;
    
    // 记录任务上次执行时间，避免重复执行
    private final Map<Long, LocalDateTime> lastExecuteTimeMap = new ConcurrentHashMap<>();
    
    @jakarta.annotation.PostConstruct
    public void init() {
        log.info("========== CollectTaskScheduler 初始化完成 ==========");
    }
    
    /**
     * 检查并执行定时任务（由统一调度器调用）
     */
    public void checkAndExecuteTasksInternal() {
        log.debug("[CollectTaskScheduler] 开始检查定时任务...");
        try {
            // 查询所有运行中且启用定时的任务
            List<CollectTask> tasks = taskMapper.selectScheduledRunningTasks();
            log.debug("[CollectTaskScheduler] 查询到 {} 个运行中的定时任务", tasks.size());
            
            LocalDateTime now = LocalDateTime.now();
            
            for (CollectTask task : tasks) {
                log.debug("[CollectTaskScheduler] 检查任务: id={}, name={}, cron={}", 
                    task.getId(), task.getTaskName(), task.getCronExpression());
                try {
                    if (shouldExecute(task, now)) {
                        log.info("定时采集任务触发执行: taskId={}, taskName={}, cron={}", 
                                task.getId(), task.getTaskName(), task.getCronExpression());
                        
                        // 通过ApplicationContext获取DataCollectService，避免循环依赖
                        com.dataplatform.data.service.DataCollectService collectService = 
                            applicationContext.getBean(com.dataplatform.data.service.DataCollectService.class);
                        collectService.executeTaskOnce(task.getId());
                        
                        // 记录执行时间
                        lastExecuteTimeMap.put(task.getId(), now);
                    }
                } catch (Exception e) {
                    log.error("定时采集任务执行失败: taskId={}, error={}", task.getId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("检查定时采集任务失败", e);
        }
    }
    
    /**
     * 判断任务是否应该执行
     */
    private boolean shouldExecute(CollectTask task, LocalDateTime now) {
        String cronExpression = task.getCronExpression();
        if (cronExpression == null || cronExpression.trim().isEmpty()) {
            return false;
        }
        
        try {
            CronExpression cron = CronExpression.parse(cronExpression);
            
            // 获取上次执行时间（优先使用内存中的记录）
            LocalDateTime lastExecute = lastExecuteTimeMap.get(task.getId());
            
            // 如果内存中没有记录，说明是首次检查或服务重启
            if (lastExecute == null) {
                return true;
            }
            
            // 计算从上次执行时间到现在，是否有应该执行的时间点
            LocalDateTime nextExecute = cron.next(lastExecute);
            
            if (nextExecute != null && !nextExecute.isAfter(now)) {
                return true;
            }
        } catch (Exception e) {
            log.warn("解析Cron表达式失败: taskId={}, cron={}, error={}", 
                    task.getId(), cronExpression, e.getMessage());
        }
        
        return false;
    }
    
    /**
     * 清除任务的执行记录
     */
    public void clearTaskExecuteRecord(Long taskId) {
        lastExecuteTimeMap.remove(taskId);
        log.debug("[CollectTaskScheduler] 清除任务 {} 的执行记录", taskId);
    }
}
