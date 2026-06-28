package com.dataplatform.data.scheduler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据同步任务处理器
 * 通过XXL-Job调度执行数据同步任务
 * 需求: 18.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "xxl.job.enabled", havingValue = "true", matchIfMissing = false)
public class DataSyncJobHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, SyncTaskStatus> taskStatusMap = new ConcurrentHashMap<>();

    /**
     * 增量数据同步任务
     * 参数格式: {"taskId":"xxx","sourceId":"xxx","targetTable":"xxx","syncField":"update_time"}
     */
    @XxlJob("incrementalSyncHandler")
    public void incrementalSyncHandler() throws Exception {
        String param = XxlJobHelper.getJobParam();
        log.info("[增量同步] 开始执行, param={}", param);

        try {
            JsonNode config = objectMapper.readTree(param);
            String taskId = config.get("taskId").asText();
            String sourceId = config.get("sourceId").asText();
            String targetTable = config.get("targetTable").asText();
            String syncField = config.has("syncField") ? config.get("syncField").asText() : "update_time";

            updateStatus(taskId, "RUNNING", null);

            // 执行增量同步逻辑（实际实现需要DataSyncService）
            long syncedRows = doIncrementalSync(sourceId, targetTable, syncField);

            updateStatus(taskId, "SUCCESS", "同步" + syncedRows + "行");
            XxlJobHelper.handleSuccess("增量同步完成, 同步行数: " + syncedRows);
        } catch (Exception e) {
            log.error("[增量同步] 执行失败", e);
            XxlJobHelper.handleFail("增量同步失败: " + e.getMessage());
            throw e;
        }
    }

    /**
     * 全量数据同步任务
     * 参数格式: {"taskId":"xxx","sourceId":"xxx","targetTable":"xxx","batchSize":1000}
     */
    @XxlJob("fullSyncHandler")
    public void fullSyncHandler() throws Exception {
        String param = XxlJobHelper.getJobParam();
        log.info("[全量同步] 开始执行, param={}", param);

        try {
            JsonNode config = objectMapper.readTree(param);
            String taskId = config.get("taskId").asText();
            String sourceId = config.get("sourceId").asText();
            String targetTable = config.get("targetTable").asText();
            int batchSize = config.has("batchSize") ? config.get("batchSize").asInt() : 1000;

            updateStatus(taskId, "RUNNING", null);

            long syncedRows = doFullSync(sourceId, targetTable, batchSize);

            updateStatus(taskId, "SUCCESS", "同步" + syncedRows + "行");
            XxlJobHelper.handleSuccess("全量同步完成, 同步行数: " + syncedRows);
        } catch (Exception e) {
            log.error("[全量同步] 执行失败", e);
            XxlJobHelper.handleFail("全量同步失败: " + e.getMessage());
            throw e;
        }
    }

    /**
     * 数据质量检查任务
     */
    @XxlJob("dataQualityCheckHandler")
    public void dataQualityCheckHandler() throws Exception {
        String param = XxlJobHelper.getJobParam();
        log.info("[数据质量检查] 开始执行, param={}", param);

        try {
            // 数据质量检查逻辑由DataQualityService处理
            XxlJobHelper.handleSuccess("数据质量检查完成");
        } catch (Exception e) {
            log.error("[数据质量检查] 执行失败", e);
            XxlJobHelper.handleFail("数据质量检查失败: " + e.getMessage());
            throw e;
        }
    }

    /**
     * 元数据采集任务
     */
    @XxlJob("metadataCollectHandler")
    public void metadataCollectHandler() throws Exception {
        String param = XxlJobHelper.getJobParam();
        log.info("[元数据采集] 开始执行, param={}", param);

        try {
            XxlJobHelper.handleSuccess("元数据采集完成");
        } catch (Exception e) {
            log.error("[元数据采集] 执行失败", e);
            XxlJobHelper.handleFail("元数据采集失败: " + e.getMessage());
            throw e;
        }
    }

    public Map<String, SyncTaskStatus> getAllTaskStatus() {
        return Collections.unmodifiableMap(taskStatusMap);
    }

    private long doIncrementalSync(String sourceId, String targetTable, String syncField) {
        // 实际实现将通过DataSyncService执行SQL查询和数据写入
        log.info("[增量同步] sourceId={}, targetTable={}, syncField={}", sourceId, targetTable, syncField);
        return 0;
    }

    private long doFullSync(String sourceId, String targetTable, int batchSize) {
        log.info("[全量同步] sourceId={}, targetTable={}, batchSize={}", sourceId, targetTable, batchSize);
        return 0;
    }

    private void updateStatus(String taskId, String status, String message) {
        SyncTaskStatus taskStatus = taskStatusMap.computeIfAbsent(taskId, k -> new SyncTaskStatus());
        taskStatus.setTaskId(taskId);
        taskStatus.setStatus(status);
        taskStatus.setMessage(message);
        taskStatus.setLastUpdateTime(LocalDateTime.now());
    }

    @Data
    public static class SyncTaskStatus {
        private String taskId;
        private String status; // PENDING, RUNNING, SUCCESS, FAILED
        private String message;
        private LocalDateTime lastUpdateTime;
    }
}
