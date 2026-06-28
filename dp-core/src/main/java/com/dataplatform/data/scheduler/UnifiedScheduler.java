package com.dataplatform.data.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 统一定时任务调度器
 * 将所有定时任务统一管理，每分钟扫描一次
 * 可通过application.properties配置
 */
@Slf4j
@Component
public class UnifiedScheduler {

    @Autowired
    private CollectTaskScheduler collectTaskScheduler;

    @Autowired
    private DataxJobScheduler dataxJobScheduler;

    @Autowired
    private PipelineScheduler pipelineScheduler;
    
    @Autowired
    private com.dataplatform.data.service.ExportTaskService exportTaskService;

    @Value("${scheduler.enabled:true}")
    private boolean schedulerEnabled;

    @Value("${scheduler.collect-task.enabled:true}")
    private boolean collectTaskEnabled;

    @Value("${scheduler.datax-job.enabled:true}")
    private boolean dataxJobEnabled;

    @Value("${scheduler.pipeline.enabled:true}")
    private boolean pipelineEnabled;

    @Value("${scheduler.pipeline.refresh-interval:5}")
    private int pipelineRefreshInterval;

    /** 最大并发任务数 */
    @Value("${scheduler.max-concurrent-tasks:10}")
    private int maxConcurrentTasks;

    private final AtomicInteger tickCount = new AtomicInteger(0);
    
    /** 并发控制信号量 */
    private Semaphore taskSemaphore;

    @PostConstruct
    public void init() {
        taskSemaphore = new Semaphore(maxConcurrentTasks);
        log.info("========== 统一调度器初始化 ==========");
        log.info("调度器总开关: {}", schedulerEnabled);
        log.info("采集任务调度: {}", collectTaskEnabled);
        log.info("DataX任务调度: {}", dataxJobEnabled);
        log.info("流程任务调度: {}", pipelineEnabled);
        log.info("最大并发任务数: {}", maxConcurrentTasks);
        log.info("流程刷新间隔: {}分钟", pipelineRefreshInterval);
        log.info("======================================");
    }

    /**
     * 统一调度入口 - 每分钟执行一次
     * 通过配置可以开关各个子任务
     */
    @Scheduled(cron = "${scheduler.cron:0 * * * * ?}")
    public void unifiedSchedule() {
        if (!schedulerEnabled) {
            return;
        }

        int tick = tickCount.incrementAndGet();
        LocalDateTime now = LocalDateTime.now();
        log.debug("[UnifiedScheduler] 第{}次调度执行, 时间: {}", tick, now);

        // 检查并发限制
        int available = taskSemaphore.availablePermits();
        if (available <= 0) {
            log.warn("[UnifiedScheduler] 并发任务达到上限({})，跳过本次调度", maxConcurrentTasks);
            return;
        }

        // 1. 采集任务调度
        if (collectTaskEnabled && taskSemaphore.tryAcquire()) {
            try {
                collectTaskScheduler.checkAndExecuteTasksInternal();
            } catch (Exception e) {
                log.error("[UnifiedScheduler] 采集任务调度异常: {}", e.getMessage());
            } finally {
                taskSemaphore.release();
            }
        }

        // 2. DataX任务调度
        if (dataxJobEnabled && taskSemaphore.tryAcquire()) {
            try {
                dataxJobScheduler.checkAndExecuteJobsInternal();
            } catch (Exception e) {
                log.error("[UnifiedScheduler] DataX任务调度异常: {}", e.getMessage());
            } finally {
                taskSemaphore.release();
            }
        }

        // 3. 流程任务调度
        if (pipelineEnabled && taskSemaphore.tryAcquire()) {
            try {
                pipelineScheduler.checkAndExecuteInternal();
                
                // 每N分钟刷新一次流程列表
                if (tick % pipelineRefreshInterval == 0) {
                    pipelineScheduler.refreshScheduledPipelinesInternal();
                }
            } catch (Exception e) {
                log.error("[UnifiedScheduler] 流程任务调度异常: {}", e.getMessage());
            } finally {
                taskSemaphore.release();
            }
        }
        
        // 4. 每5分钟检查并恢复卡住的导出任务
        if (tick % 5 == 0) {
            try {
                exportTaskService.recoverStuckTasks();
            } catch (Exception e) {
                log.error("[UnifiedScheduler] 导出任务恢复异常: {}", e.getMessage());
            }
        }
    }

    /**
     * 获取调度器状态信息
     */
    public String getStatus() {
        return String.format(
            "UnifiedScheduler[enabled=%s, tick=%d, collect=%s, datax=%s, pipeline=%s, concurrent=%d/%d]",
            schedulerEnabled, tickCount.get(), collectTaskEnabled, dataxJobEnabled, pipelineEnabled,
            maxConcurrentTasks - taskSemaphore.availablePermits(), maxConcurrentTasks
        );
    }

    /**
     * 手动触发一次调度（用于测试）
     */
    public void triggerOnce() {
        log.info("[UnifiedScheduler] 手动触发调度");
        unifiedSchedule();
    }
}
