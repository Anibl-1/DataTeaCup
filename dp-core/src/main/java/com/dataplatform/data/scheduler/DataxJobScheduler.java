package com.dataplatform.data.scheduler;

import com.dataplatform.data.entity.DataxJob;
import com.dataplatform.data.mapper.DataxJobMapper;
import com.dataplatform.data.service.DataxJobService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DataX 任务调度器
 * 每分钟检查一次需要执行的定时任务
 */
@Slf4j
@Component
public class DataxJobScheduler {

    @Autowired
    private DataxJobMapper jobMapper;
    
    @Autowired
    private DataxJobService jobService;
    
    // 记录任务上次执行时间，避免重复执行
    private final Map<Long, LocalDateTime> lastExecuteTimeMap = new ConcurrentHashMap<>();
    
    /**
     * 检查并执行定时任务（由统一调度器调用）
     */
    public void checkAndExecuteJobsInternal() {
        try {
            // 查询所有运行中的任务（jobStatus = 1）
            List<DataxJob> jobs = jobMapper.selectList(0, 1000, null, 1);
            
            LocalDateTime now = LocalDateTime.now();
            
            for (DataxJob job : jobs) {
                try {
                    if (shouldExecute(job, now)) {
                        log.info("定时任务触发执行: jobId={}, jobName={}, cron={}", 
                                job.getId(), job.getJobName(), job.getCronExpression());
                        
                        // 执行任务（定时触发，triggerType=2）
                        jobService.executeJob(job.getId(), 2);
                        
                        // 记录执行时间
                        lastExecuteTimeMap.put(job.getId(), now);
                    }
                } catch (Exception e) {
                    log.error("定时任务执行失败: jobId={}, error={}", job.getId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("检查定时任务失败", e);
        }
    }
    
    /**
     * 判断任务是否应该执行
     */
    private boolean shouldExecute(DataxJob job, LocalDateTime now) {
        String cronExpression = job.getCronExpression();
        if (cronExpression == null || cronExpression.trim().isEmpty()) {
            return false;
        }
        
        try {
            CronExpression cron = CronExpression.parse(cronExpression);
            
            // 获取上次执行时间
            LocalDateTime lastExecute = lastExecuteTimeMap.get(job.getId());
            if (lastExecute == null) {
                // 如果没有记录，使用数据库中的最后执行时间
                if (job.getLastExecuteTime() != null) {
                    lastExecute = job.getLastExecuteTime().toInstant()
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDateTime();
                } else {
                    // 首次执行，设置为1分钟前
                    lastExecute = now.minusMinutes(1);
                }
            }
            
            // 计算下次执行时间
            LocalDateTime nextExecute = cron.next(lastExecute);
            
            if (nextExecute != null) {
                // 如果下次执行时间在当前时间之前或等于当前时间（精确到分钟）
                long diffSeconds = ChronoUnit.SECONDS.between(nextExecute, now);
                if (diffSeconds >= 0 && diffSeconds < 60) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.warn("解析Cron表达式失败: jobId={}, cron={}, error={}", 
                    job.getId(), cronExpression, e.getMessage());
        }
        
        return false;
    }
    
    /**
     * 清除任务的执行记录（停止任务时调用）
     */
    public void clearJobExecuteRecord(Long jobId) {
        lastExecuteTimeMap.remove(jobId);
    }
}
