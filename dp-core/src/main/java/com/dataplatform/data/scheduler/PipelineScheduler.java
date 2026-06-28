package com.dataplatform.data.scheduler;

import com.dataplatform.data.entity.Pipeline;
import com.dataplatform.data.mapper.PipelineMapper;
import com.dataplatform.data.service.PipelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据流程定时调度器
 */
@Slf4j
@Component
public class PipelineScheduler {
    
    @Autowired
    private PipelineMapper pipelineMapper;
    
    @Autowired
    private PipelineService pipelineService;
    
    // 记录每个流程的下次执行时间
    private final Map<Long, LocalDateTime> nextExecutionTimes = new ConcurrentHashMap<>();
    // 正在执行的流程ID集合，防止同一流程并发执行
    private final java.util.Set<Long> executingPipelines = ConcurrentHashMap.newKeySet();
    
    @PostConstruct
    public void init() {
        log.info("初始化流程调度器...");
        try {
            refreshScheduledPipelinesInternal();
        } catch (Exception e) {
            log.warn("初始化流程调度器失败，可能是数据表尚未创建: {}", e.getMessage());
        }
    }
    
    /**
     * 检查并执行定时流程（由统一调度器调用）
     */
    public void checkAndExecuteInternal() {
        LocalDateTime now = LocalDateTime.now();
        
        List<Pipeline> scheduledPipelines = pipelineMapper.findScheduledPipelines();
        
        for (Pipeline pipeline : scheduledPipelines) {
            try {
                if (shouldExecute(pipeline, now)) {
                    // 防止同一流程并发执行
                    if (!executingPipelines.add(pipeline.getId())) {
                        log.warn("流程 {} 仍在执行中，跳过本次调度", pipeline.getPipelineName());
                        continue;
                    }
                    log.info("定时执行流程: {} ({})", pipeline.getPipelineName(), pipeline.getCronExpression());
                    
                    try {
                        // 执行流程（系统用户ID为1）
                        pipelineService.execute(pipeline.getId(), 1L);
                    } finally {
                        // 异步执行，延迟移除标记（给runAsync足够时间启动）
                        // 实际完成后由执行回调清理，这里仅做基本保护
                        executingPipelines.remove(pipeline.getId());
                    }
                    
                    // 更新下次执行时间
                    updateNextExecutionTime(pipeline);
                }
            } catch (Exception e) {
                executingPipelines.remove(pipeline.getId());
                log.error("定时执行流程失败: {}", pipeline.getPipelineName(), e);
            }
        }
    }
    
    /**
     * 刷新定时流程列表（由统一调度器调用）
     */
    public void refreshScheduledPipelinesInternal() {
        log.debug("刷新定时流程列表...");
        
        List<Pipeline> scheduledPipelines = pipelineMapper.findScheduledPipelines();
        
        // 清理已删除或停用的流程
        nextExecutionTimes.keySet().removeIf(id -> 
            scheduledPipelines.stream().noneMatch(p -> p.getId().equals(id)));
        
        // 更新或添加新的定时流程
        for (Pipeline pipeline : scheduledPipelines) {
            if (!nextExecutionTimes.containsKey(pipeline.getId())) {
                updateNextExecutionTime(pipeline);
            }
        }
        
        log.debug("当前定时流程数量: {}", nextExecutionTimes.size());
    }
    
    /**
     * 判断流程是否应该执行
     */
    private boolean shouldExecute(Pipeline pipeline, LocalDateTime now) {
        if (pipeline.getCronExpression() == null || pipeline.getCronExpression().isEmpty()) {
            return false;
        }
        
        LocalDateTime nextTime = nextExecutionTimes.get(pipeline.getId());
        if (nextTime == null) {
            updateNextExecutionTime(pipeline);
            nextTime = nextExecutionTimes.get(pipeline.getId());
        }
        
        if (nextTime == null) {
            return false;
        }
        
        // 如果当前时间已经超过下次执行时间，则执行
        return !now.isBefore(nextTime);
    }
    
    /**
     * 更新流程的下次执行时间
     */
    private void updateNextExecutionTime(Pipeline pipeline) {
        try {
            if (pipeline.getCronExpression() == null || pipeline.getCronExpression().isEmpty()) {
                nextExecutionTimes.remove(pipeline.getId());
                return;
            }
            
            CronExpression cron = CronExpression.parse(pipeline.getCronExpression());
            LocalDateTime next = cron.next(LocalDateTime.now());
            
            if (next != null) {
                nextExecutionTimes.put(pipeline.getId(), next);
                log.debug("流程 {} 下次执行时间: {}", pipeline.getPipelineName(), next);
            }
        } catch (Exception e) {
            log.error("解析Cron表达式失败: {} - {}", pipeline.getPipelineName(), pipeline.getCronExpression(), e);
            nextExecutionTimes.remove(pipeline.getId());
        }
    }
    
    /**
     * 手动触发刷新（供外部调用）
     */
    public void triggerRefresh() {
        refreshScheduledPipelinesInternal();
    }
    
    /**
     * 获取流程的下次执行时间
     */
    public LocalDateTime getNextExecutionTime(Long pipelineId) {
        return nextExecutionTimes.get(pipelineId);
    }
}
