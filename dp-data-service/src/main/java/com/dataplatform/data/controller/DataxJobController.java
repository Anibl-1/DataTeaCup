package com.dataplatform.data.controller;

import com.dataplatform.common.annotation.OperationLog;
import com.dataplatform.common.annotation.RequirePermission;
import com.dataplatform.common.PageResult;
import com.dataplatform.common.Result;
import com.dataplatform.common.exception.BusinessException;
import com.dataplatform.common.exception.ErrorCode;
import com.dataplatform.data.entity.DataxJob;
import com.dataplatform.data.entity.DataxJobLog;
import com.dataplatform.data.service.DataxJobService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * DataX鏁版嵁浼犺緭浠诲姟鎺у埗鍣?
 */
@Slf4j
@RestController
@RequestMapping("/datax/job")
@RequirePermission("datax:manage")
public class DataxJobController {

    @Autowired
    private DataxJobService jobService;
    
    @Autowired
    private com.dataplatform.data.engine.TransferEngineSelector engineSelector;

    /**
     * 鍒嗛〉鏌ヨ浠诲姟鍒楄〃
     */
    @GetMapping("/list")
    public Result<PageResult<DataxJob>> getJobList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer jobStatus) {
        PageResult<DataxJob> result = jobService.getJobList(page, Math.min(pageSize, 200), keyword, jobStatus);
        return Result.success(result);
    }

    /**
     * 鏍规嵁ID鑾峰彇浠诲姟璇︽儏
     */
    @GetMapping("/{id}")
    public Result<DataxJob> getJobById(@PathVariable Long id) {
        DataxJob job = jobService.getJobById(id);
        return Result.success(job);
    }

    /**
     * 鍒涘缓浠诲姟
     */
    @OperationLog(module = "鏁版嵁浼犺緭", type = OperationLog.OperationType.CREATE, description = "鍒涘缓DataX浠诲姟")
    @PostMapping("/create")
    public Result<Long> createJob(@RequestBody DataxJob job) {
        Long id = jobService.createJob(job);
        return Result.success(id);
    }

    /**
     * 鏇存柊浠诲姟
     */
    @OperationLog(module = "鏁版嵁浼犺緭", type = OperationLog.OperationType.UPDATE, description = "鏇存柊DataX浠诲姟")
    @PostMapping("/update")
    public Result<Void> updateJob(@RequestBody DataxJob job) {
        jobService.updateJob(job);
        return Result.success(null);
    }

    /**
     * 鍒犻櫎浠诲姟
     */
    @OperationLog(module = "鏁版嵁浼犺緭", type = OperationLog.OperationType.DELETE, description = "鍒犻櫎DataX浠诲姟")
    @DeleteMapping("/{id}")
    public Result<Void> deleteJob(@PathVariable Long id) {
        jobService.deleteJob(id);
        return Result.success(null);
    }

    /**
     * 鎵归噺鍒犻櫎浠诲姟
     */
    @OperationLog(module = "鏁版嵁浼犺緭", type = OperationLog.OperationType.DELETE, description = "鎵归噺鍒犻櫎DataX浠诲姟")
    @PostMapping("/batch-delete")
    public Result<Void> batchDeleteJobs(@RequestBody List<Long> ids) {
        if (ids.size() > 100) {
            return Result.error("批量删除最多支持100条");
        }
        for (Long id : ids) {
            jobService.deleteJob(id);
        }
        return Result.success(null);
    }

    /**
     * 澶嶅埗浠诲姟
     */
    @OperationLog(module = "鏁版嵁浼犺緭", type = OperationLog.OperationType.CREATE, description = "澶嶅埗DataX浠诲姟")
    @PostMapping("/{id}/copy")
    public Result<Long> copyJob(@PathVariable Long id) {
        Long newId = jobService.copyJob(id);
        return Result.success(newId);
    }

    /**
     * 鎵ц浠诲姟锛堢珛鍗虫墽琛屼竴娆★級
     */
    @OperationLog(module = "鏁版嵁浼犺緭", type = OperationLog.OperationType.UPDATE, description = "鎵цDataX浠诲姟")
    @PostMapping("/{id}/execute")
    public Result<Long> executeJob(@PathVariable Long id) {
        try {
            log.info("寮€濮嬫墽琛孌ataX浠诲姟: {}", id);
            Long logId = jobService.executeJob(id);
            log.info("DataX浠诲姟宸叉彁浜ゆ墽琛? jobId: {}, logId: {}", id, logId);
            return Result.success(logId);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.DATAX_JOB_EXECUTION_FAILED, "鎵ц澶辫触: " + e.getMessage());
        }
    }
    
    /**
     * 甯﹀弬鏁版墽琛屼换鍔?
     * 璇锋眰浣撶ず渚? {"startDate": "2024-01-01", "endDate": "2024-12-31"}
     */
    @PostMapping("/{id}/execute-with-params")
    public Result<Long> executeJobWithParams(@PathVariable Long id, @RequestBody Map<String, Object> parameters) {
        try {
            log.info("寮€濮嬫墽琛孌ataX浠诲姟(甯﹀弬鏁?: {}, 鍙傛暟: {}", id, parameters);
            Long logId = jobService.executeJobWithParams(id, parameters);
            log.info("DataX浠诲姟宸叉彁浜ゆ墽琛? jobId: {}, logId: {}", id, logId);
            return Result.success(logId);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.DATAX_JOB_EXECUTION_FAILED, "鎵ц澶辫触: " + e.getMessage());
        }
    }

    /**
     * 鎵归噺鎵ц浠诲姟
     */
    @OperationLog(module = "鏁版嵁浼犺緭", type = OperationLog.OperationType.UPDATE, description = "鎵归噺鎵цDataX浠诲姟")
    @PostMapping("/batch-execute")
    public Result<Void> batchExecuteJobs(@RequestBody List<Long> ids) {
        if (ids.size() > 50) {
            return Result.error("批量执行最多支持50个任务");
        }
        for (Long id : ids) {
            try {
                jobService.executeJob(id);
            } catch (Exception e) {
                log.error("鎵归噺鎵ц浠诲姟澶辫触, jobId: {}", id, e);
            }
        }
        return Result.success(null);
    }

    /**
     * 鍚姩浠诲姟璋冨害
     */
    @OperationLog(module = "鏁版嵁浼犺緭", type = OperationLog.OperationType.UPDATE, description = "鍚姩DataX浠诲姟璋冨害")
    @PostMapping("/{id}/start")
    public Result<Void> startJob(@PathVariable Long id) {
        try {
            jobService.startJob(id);
            return Result.success(null);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.DATAX_JOB_EXECUTION_FAILED, "鍚姩浠诲姟璋冨害澶辫触: " + e.getMessage());
        }
    }

    /**
     * 鍋滄浠诲姟璋冨害
     */
    @OperationLog(module = "鏁版嵁浼犺緭", type = OperationLog.OperationType.UPDATE, description = "鍋滄DataX浠诲姟璋冨害")
    @PostMapping("/{id}/stop")
    public Result<Void> stopJob(@PathVariable Long id) {
        try {
            jobService.stopJob(id);
            return Result.success(null);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.DATAX_JOB_EXECUTION_FAILED, "鍋滄浠诲姟璋冨害澶辫触: " + e.getMessage());
        }
    }

    /**
     * 鏆傚仠浠诲姟璋冨害
     */
    @PostMapping("/{id}/pause")
    public Result<Void> pauseJob(@PathVariable Long id) {
        try {
            jobService.pauseJob(id);
            return Result.success(null);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.DATAX_JOB_EXECUTION_FAILED, "鏆傚仠浠诲姟璋冨害澶辫触: " + e.getMessage());
        }
    }

    /**
     * 鎭㈠浠诲姟璋冨害
     */
    @PostMapping("/{id}/resume")
    public Result<Void> resumeJob(@PathVariable Long id) {
        try {
            jobService.resumeJob(id);
            return Result.success(null);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.DATAX_JOB_EXECUTION_FAILED, "鎭㈠浠诲姟璋冨害澶辫触: " + e.getMessage());
        }
    }

    /**
     * 鑾峰彇鎵ц杩涘害
     */
    @GetMapping("/progress/{logId}")
    public Result<Map<String, Object>> getProgress(@PathVariable Long logId) {
        Map<String, Object> progress = jobService.getExecuteProgress(logId);
        return Result.success(progress);
    }

    /**
     * 鑾峰彇浠诲姟鏃ュ織鍒楄〃
     */
    @GetMapping("/logs")
    public Result<PageResult<DataxJobLog>> getJobLogList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long jobId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String jobName) {
        PageResult<DataxJobLog> result = jobService.getJobLogList(page, Math.min(pageSize, 200), jobId, status, jobName);
        return Result.success(result);
    }

    /**
     * 鑾峰彇鏃ュ織璇︽儏
     */
    @GetMapping("/log/{id}")
    public Result<DataxJobLog> getLogById(@PathVariable Long id) {
        DataxJobLog log = jobService.getLogById(id);
        return Result.success(log);
    }

    /**
     * 鑾峰彇鏃ュ織缁熻
     */
    @GetMapping("/log-statistics")
    public Result<Map<String, Object>> getLogStatistics(@RequestParam(required = false) Long jobId) {
        Map<String, Object> statistics = jobService.getLogStatistics(jobId);
        return Result.success(statistics);
    }

    /**
     * 棰勮DataX JSON閰嶇疆
     */
    @PostMapping("/preview-json")
    public Result<String> previewJson(@RequestBody DataxJob job) {
        String json = jobService.previewDataxJson(job);
        return Result.success(json);
    }

    /**
     * 鑾峰彇浠诲姟鍙傛暟瀹氫箟
     */
    @GetMapping("/{id}/parameters")
    public Result<List<Map<String, Object>>> getJobParameters(@PathVariable Long id) {
        List<Map<String, Object>> parameters = jobService.getJobParameters(id);
        return Result.success(parameters);
    }

    /**
     * 鏇存柊浠诲姟鍙傛暟瀹氫箟
     */
    @PostMapping("/{id}/parameters")
    public Result<Void> updateJobParameters(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        String parameterDefinition = (String) params.get("parameterDefinition");
        String defaultParameters = (String) params.get("defaultParameters");
        jobService.updateJobParameters(id, parameterDefinition, defaultParameters);
        return Result.success(null);
    }

    /**
     * 鑾峰彇婧愯〃瀛楁鍒楄〃
     */
    @GetMapping("/columns")
    public Result<List<Map<String, Object>>> getSourceColumns(
            @RequestParam Long dataSourceId,
            @RequestParam String tableName) {
        if (tableName == null || !tableName.matches("^[a-zA-Z0-9_.]+$")) {
            return Result.error("无效的表名");
        }
        List<Map<String, Object>> columns = jobService.getSourceColumns(dataSourceId, tableName);
        return Result.success(columns);
    }

    // ==================== 闃熷垪绠＄悊 ====================

    /**
     * 鑾峰彇杩愯涓殑浠诲姟闃熷垪
     */
    @GetMapping("/queue")
    public Result<List<Map<String, Object>>> getRunningQueue() {
        List<Map<String, Object>> queue = jobService.getRunningQueue();
        return Result.success(queue);
    }

    /**
     * 瀵规瘮涓ゆ鎵ц璁板綍
     */
    @PostMapping("/compare-executions")
    public Result<Map<String, Object>> compareExecutions(@RequestBody Map<String, Long> params) {
        Long logId1 = params.get("logId1");
        Long logId2 = params.get("logId2");
        if (logId1 == null || logId2 == null) {
            return Result.error("璇锋彁渚涗袱涓棩蹇桰D");
        }
        Map<String, Object> result = jobService.compareExecutions(logId1, logId2);
        return Result.success(result);
    }

    // ==================== 浠诲姟妯℃澘鍔熻兘 ====================

    /**
     * 鑾峰彇浠诲姟妯℃澘鍒楄〃
     */
    @GetMapping("/templates")
    public Result<List<DataxJob>> getTemplates() {
        List<DataxJob> templates = jobService.getTemplates();
        return Result.success(templates);
    }

    /**
     * 淇濆瓨涓烘ā鏉?
     */
    @PostMapping("/{id}/save-as-template")
    public Result<Long> saveAsTemplate(@PathVariable Long id, @RequestParam String templateName) {
        Long templateId = jobService.saveAsTemplate(id, templateName);
        return Result.success(templateId);
    }

    /**
     * 浠庢ā鏉垮垱寤轰换鍔?
     */
    @PostMapping("/create-from-template")
    public Result<Long> createFromTemplate(
            @RequestParam Long templateId,
            @RequestParam String jobName) {
        Long newJobId = jobService.createFromTemplate(templateId, jobName);
        return Result.success(newJobId);
    }

    /**
     * 鍒犻櫎妯℃澘
     */
    @DeleteMapping("/template/{id}")
    public Result<Void> deleteTemplate(@PathVariable Long id) {
        jobService.deleteTemplate(id);
        return Result.success(null);
    }

    // ==================== 澧為噺鍚屾閰嶇疆 ====================

    /**
     * 鑾峰彇澧為噺鍚屾鐘舵€?
     */
    @GetMapping("/{id}/increment-status")
    public Result<Map<String, Object>> getIncrementStatus(@PathVariable Long id) {
        Map<String, Object> status = jobService.getIncrementStatus(id);
        return Result.success(status);
    }

    /**
     * 閲嶇疆澧為噺鍚屾浣嶇疆
     */
    @PostMapping("/{id}/reset-increment")
    public Result<Void> resetIncrement(@PathVariable Long id) {
        jobService.resetIncrement(id);
        return Result.success(null);
    }

    // ==================== 缁熻涓庣洃鎺?====================

    /**
     * 鑾峰彇浠诲姟鎵ц瓒嬪娍锛堟渶杩?澶╋級
     */
    @GetMapping("/execution-trend")
    public Result<List<Map<String, Object>>> getExecutionTrend() {
        List<Map<String, Object>> trend = jobService.getExecutionTrend();
        return Result.success(trend);
    }

    /**
     * 鑾峰彇浠诲姟姒傝缁熻
     */
    @GetMapping("/overview-statistics")
    public Result<Map<String, Object>> getOverviewStatistics() {
        Map<String, Object> stats = jobService.getOverviewStatistics();
        return Result.success(stats);
    }

    /**
     * 获取可用的执行引擎列表
     */
    @GetMapping("/engines")
    public Result<List<String>> getAvailableEngines() {
        return Result.success(engineSelector.getAvailableEngines());
    }
}
