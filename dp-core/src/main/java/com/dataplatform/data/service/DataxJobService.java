package com.dataplatform.data.service;

import com.dataplatform.common.PageResult;
import com.dataplatform.common.exception.BusinessException;
import com.dataplatform.common.exception.ErrorCode;
import com.dataplatform.data.entity.DataSource;
import com.dataplatform.data.entity.DataxJob;
import com.dataplatform.data.entity.DataxJobLog;
import com.dataplatform.data.mapper.DataSourceMapper;
import com.dataplatform.data.mapper.DataxJobMapper;
import com.dataplatform.data.mapper.DataxJobLogMapper;
import com.dataplatform.data.service.DbConnectionUtil;
import com.dataplatform.data.scheduler.DataxJobScheduler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据传输任务服务
 */
@Slf4j
@Service
public class DataxJobService {

    @Autowired
    private DataxJobMapper jobMapper;
    
    @Autowired
    private DataxJobLogMapper logMapper;
    
    @Autowired
    private DataSourceMapper dataSourceMapper;
    
    @Autowired
    private DataSourceService dataSourceService;
    
    @Autowired
    private DbConnectionUtil dbConnectionUtil;
    
    @Autowired
    private com.dataplatform.data.service.DataSourceConnectionPoolManager connectionPoolManager;
    
    @Lazy
    @Autowired
    private DataxJobScheduler dataxJobScheduler;
    
    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    
    @Autowired
    private com.dataplatform.data.engine.TransferEngineSelector engineSelector;
    
    // 存储运行中的任务进度
    private static final Map<Long, Map<String, Object>> runningJobs = new ConcurrentHashMap<>();
    private static final java.util.concurrent.ScheduledExecutorService progressCleanupExecutor =
            java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "datax-progress-cleanup");
                t.setDaemon(true);
                return t;
            });

    /**
     * 分页查询任务列表
     */
    public PageResult<DataxJob> getJobList(Integer page, Integer pageSize, String keyword, Integer jobStatus) {
        if (page == null || page < 1) page = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;
        
        int offset = (page - 1) * pageSize;
        List<DataxJob> list = jobMapper.selectList(offset, pageSize, keyword, jobStatus);
        long total = jobMapper.count(keyword, jobStatus);
        
        return new PageResult<>(list, total);
    }

    /**
     * 根据ID获取任务
     */
    public DataxJob getJobById(Long id) {
        return jobMapper.selectById(id);
    }

    /**
     * 创建任务
     */
    @Transactional
    public Long createJob(DataxJob job) {
        // 验证数据源
        if (job.getSourceDataSourceId() != null) {
            DataSource source = dataSourceMapper.selectById(job.getSourceDataSourceId());
            if (source == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "源数据源不存在");
            }
        }
        if (job.getTargetDataSourceId() != null) {
            DataSource target = dataSourceMapper.selectById(job.getTargetDataSourceId());
            if (target == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "目标数据源不存在");
            }
        }
        
        job.setJobStatus(0);
        job.setDelFlag(0);
        job.setCreateTime(new Date());
        job.setUpdateTime(new Date());
        if (job.getChannelCount() == null) {
            job.setChannelCount(3);
        }
        if (job.getBatchSize() == null) {
            job.setBatchSize(1000);
        }
        if (job.getIncrementType() == null) {
            job.setIncrementType(0);
        }
        if (job.getJobType() == null) {
            job.setJobType(1);
        }
        
        jobMapper.insert(job);
        return job.getId();
    }

    /**
     * 更新任务
     */
    @Transactional
    public void updateJob(DataxJob job) {
        DataxJob existing = jobMapper.selectById(job.getId());
        if (existing == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "任务不存在");
        }
        
        job.setUpdateTime(new Date());
        jobMapper.update(job);
    }

    /**
     * 删除任务
     */
    @Transactional
    public void deleteJob(Long id) {
        DataxJob job = jobMapper.selectById(id);
        if (job == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "任务不存在");
        }
        
        // 如果任务正在运行，先停止
        if (job.getJobStatus() == 1) {
            stopJob(id);
        }
        
        jobMapper.delete(id);
    }

    /**
     * 执行任务
     */
    public Long executeJob(Long jobId) {
        return executeJob(jobId, 1, null); // 默认手动触发，无参数
    }
    
    /**
     * 执行任务（带参数）
     * @param jobId 任务ID
     * @param parameters 运行时参数（JSON格式或Map）
     */
    public Long executeJobWithParams(Long jobId, Map<String, Object> parameters) {
        return executeJob(jobId, 1, parameters);
    }
    
    /**
     * 执行任务（带触发类型）
     * @param jobId 任务ID
     * @param triggerType 触发类型：1-手动, 2-定时
     */
    public Long executeJob(Long jobId, int triggerType) {
        return executeJob(jobId, triggerType, null);
    }
    
    /**
     * 执行任务（完整参数）
     * @param jobId 任务ID
     * @param triggerType 触发类型：1-手动, 2-定时
     * @param parameters 运行时参数
     */
    public Long executeJob(Long jobId, int triggerType, Map<String, Object> parameters) {
        DataxJob job = jobMapper.selectById(jobId);
        if (job == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "任务不存在");
        }
        
        // 合并默认参数和运行时参数
        Map<String, Object> mergedParams = mergeParameters(job, parameters);
        
        // 创建执行日志
        DataxJobLog jobLog = new DataxJobLog();
        jobLog.setJobId(jobId);
        jobLog.setJobName(job.getJobName());
        jobLog.setStartTime(new Date());
        jobLog.setStatus(2); // 运行中
        jobLog.setTriggerType(triggerType);
        // 记录执行参数
        if (mergedParams != null && !mergedParams.isEmpty()) {
            try {
                jobLog.setExecuteParams(objectMapper.writeValueAsString(mergedParams));
            } catch (Exception e) {
                log.warn("序列化执行参数失败: {}", e.getMessage());
            }
        }
        logMapper.insert(jobLog);
        
        // 异步执行任务
        executeJobAsync(job, jobLog, mergedParams);
        
        return jobLog.getId();
    }
    
    /**
     * 合并默认参数和运行时参数
     */
    private Map<String, Object> mergeParameters(DataxJob job, Map<String, Object> runtimeParams) {
        Map<String, Object> merged = new HashMap<>();
        
        // 先加载默认参数
        if (StringUtils.hasText(job.getDefaultParameters())) {
            try {
                Map<String, Object> defaults = objectMapper.readValue(job.getDefaultParameters(), 
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
                merged.putAll(defaults);
            } catch (Exception e) {
                log.warn("解析默认参数失败: {}", e.getMessage());
            }
        }
        
        // 运行时参数覆盖默认参数
        if (runtimeParams != null) {
            merged.putAll(runtimeParams);
        }
        
        return merged;
    }
    
    /**
     * 替换SQL中的参数占位符
     * 支持格式: ${paramName} 或 :paramName
     */
    private String replaceParameters(String sql, Map<String, Object> parameters) {
        if (sql == null || parameters == null || parameters.isEmpty()) {
            return sql;
        }
        
        String result = sql;
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            String paramName = entry.getKey();
            Object value = entry.getValue();
            String strValue = value != null ? value.toString() : "";
            // 转义单引号防止SQL注入
            String safeValue = strValue.replace("'", "''");
            
            // 替换 ${paramName} 格式（转义后插入）
            result = result.replace("${" + paramName + "}", safeValue);
            // 替换 :paramName 格式（注意边界，用引号包裹）
            result = result.replaceAll(":" + paramName + "(?![a-zA-Z0-9_])", "'" + safeValue + "'");
        }
        
        return result;
    }

    /**
     * 异步执行数据传输任务（无参数）
     */
    @Async
    public void executeJobAsync(DataxJob job, DataxJobLog jobLog) {
        executeJobAsync(job, jobLog, null);
    }
    
    /**
     * 异步执行数据传输任务（带参数）
     * 委托给 TransferEngine（JDBC / DataX）执行
     */
    @Async
    public void executeJobAsync(DataxJob job, DataxJobLog jobLog, Map<String, Object> parameters) {
        // 初始化进度
        Map<String, Object> progress = new ConcurrentHashMap<>();
        progress.put("status", 2);
        progress.put("percent", 0);
        progress.put("readCount", 0L);
        progress.put("writeCount", 0L);
        progress.put("engineType", job.getEngineType() != null ? job.getEngineType() : "jdbc");
        progress.put("message", "正在初始化...");
        if (parameters != null && !parameters.isEmpty()) {
            progress.put("parameters", parameters);
        }
        runningJobs.put(jobLog.getId(), progress);

        com.dataplatform.data.engine.TransferEngine.TransferResult result;
        try {
            // 获取数据源
            DataSource sourceDs = dataSourceMapper.selectById(job.getSourceDataSourceId());
            if (sourceDs == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR,
                        "源数据源不存在(ID:" + job.getSourceDataSourceId() + ")，请检查数据源配置或重新选择");
            }
            DataSource targetDs = dataSourceMapper.selectById(job.getTargetDataSourceId());
            if (targetDs == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR,
                        "目标数据源不存在(ID:" + job.getTargetDataSourceId() + ")，请检查数据源配置或重新选择");
            }

            // 选择引擎
            com.dataplatform.data.engine.TransferEngine engine = engineSelector.select(job.getEngineType());
            log.info("任务 {} 使用引擎: {} ({})", job.getId(), engine.getType(), engine.getClass().getSimpleName());
            progress.put("message", "使用 " + engine.getType().toUpperCase() + " 引擎执行中...");

            // 进度回调：引擎内部的进度实时更新到 runningJobs
            java.util.function.Consumer<Map<String, Object>> progressCallback = update -> {
                progress.putAll(update);
            };

            // 委托引擎执行
            result = engine.execute(job, sourceDs, targetDs, parameters, progressCallback);

            // 更新增量值和最后执行时间
            if (result.success()) {
                if (job.getLastIncrementValue() != null) {
                    job.setIncrementValue(job.getLastIncrementValue());
                }
                job.setLastExecuteTime(new Date());
                jobMapper.update(job);
            }

        } catch (Exception e) {
            log.error("数据传输任务执行失败: jobId={}", job.getId(), e);
            result = com.dataplatform.data.engine.TransferEngine.TransferResult.fail(e.getMessage());
        }

        // 更新日志
        jobLog.setEndTime(new Date());
        jobLog.setStatus(result.success() ? 1 : 0);
        jobLog.setReadCount(result.readCount());
        jobLog.setWriteCount(result.writeCount());
        jobLog.setErrorMessage(result.errorMessage());
        jobLog.setDuration(result.durationMs());
        if (result.engineLog() != null) {
            jobLog.setExecuteLog(result.engineLog());
        }
        logMapper.update(jobLog);

        // 更新进度
        progress.put("status", result.success() ? 1 : 0);
        progress.put("percent", 100);
        progress.put("readCount", result.readCount());
        progress.put("writeCount", result.writeCount());
        progress.put("message", result.success() ? "执行完成" : "执行失败: " + result.errorMessage());

        // 延迟移除进度信息
        final Long logId = jobLog.getId();
        progressCleanupExecutor.schedule(() -> runningJobs.remove(logId), 60, java.util.concurrent.TimeUnit.SECONDS);
    }

    /**
     * 构建插入SQL
     */
    private String buildInsertSql(String tableName, List<String> columns, String writeMode) {
        StringBuilder sql = new StringBuilder();
        
        if ("replace".equalsIgnoreCase(writeMode)) {
            sql.append("REPLACE INTO ");
        } else {
            sql.append("INSERT INTO ");
        }
        
        sql.append(tableName).append(" (");
        sql.append(String.join(", ", columns));
        sql.append(") VALUES (");
        sql.append(String.join(", ", Collections.nCopies(columns.size(), "?")));
        sql.append(")");
        
        return sql.toString();
    }

    /**
     * 获取执行进度
     */
    public Map<String, Object> getExecuteProgress(Long logId) {
        Map<String, Object> progress = runningJobs.get(logId);
        if (progress != null) {
            return progress;
        }
        
        // 从数据库查询
        DataxJobLog jobLog = logMapper.selectById(logId);
        if (jobLog != null) {
            Map<String, Object> result = new HashMap<>();
            result.put("status", jobLog.getStatus());
            result.put("percent", jobLog.getStatus() == 2 ? 50 : 100);
            result.put("readCount", jobLog.getReadCount());
            result.put("writeCount", jobLog.getWriteCount());
            result.put("message", jobLog.getStatus() == 1 ? "执行完成" : 
                       (jobLog.getStatus() == 0 ? "执行失败: " + jobLog.getErrorMessage() : "执行中"));
            return result;
        }
        
        return null;
    }

    /**
     * 启动任务调度
     * 任务状态设为1后，DataxJobScheduler会自动按cron表达式调度执行
     */
    public void startJob(Long id) {
        DataxJob job = jobMapper.selectById(id);
        if (job == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "任务不存在");
        }
        
        // 验证cron表达式
        if (job.getCronExpression() == null || job.getCronExpression().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请先设置Cron表达式");
        }
        
        try {
            org.springframework.scheduling.support.CronExpression.parse(job.getCronExpression());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Cron表达式格式错误: " + e.getMessage());
        }
        
        job.setJobStatus(1);
        jobMapper.update(job);
        
        // 任务状态更新后，DataxJobScheduler会自动检测并按cron调度执行
        log.info("任务调度已启动: jobId={}, cron={}", id, job.getCronExpression());
    }

    /**
     * 停止任务调度
     * 任务状态设为0后，DataxJobScheduler会自动停止调度
     */
    public void stopJob(Long id) {
        DataxJob job = jobMapper.selectById(id);
        if (job == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "任务不存在");
        }
        
        job.setJobStatus(0);
        jobMapper.update(job);
        
        // 清除调度器中的执行记录
        try {
            dataxJobScheduler.clearJobExecuteRecord(id);
        } catch (Exception e) {
            log.warn("清除任务执行记录失败: {}", e.getMessage());
        }
        
        log.info("任务调度已停止: jobId={}", id);
    }

    /**
     * 暂停任务调度
     */
    public void pauseJob(Long id) {
        DataxJob job = jobMapper.selectById(id);
        if (job == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "任务不存在");
        }
        job.setScheduleStatus("paused");
        job.setUpdateTime(new Date());
        jobMapper.update(job);
        log.info("任务调度已暂停: jobId={}", id);
    }

    /**
     * 恢复任务调度
     */
    public void resumeJob(Long id) {
        DataxJob job = jobMapper.selectById(id);
        if (job == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "任务不存在");
        }
        if (job.getJobStatus() != 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "任务未启动，无法恢复");
        }
        job.setScheduleStatus("running");
        job.setUpdateTime(new Date());
        jobMapper.update(job);
        log.info("任务调度已恢复: jobId={}", id);
    }

    /**
     * 复制任务
     */
    @Transactional
    public Long copyJob(Long id) {
        DataxJob source = jobMapper.selectById(id);
        if (source == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "任务不存在");
        }
        
        DataxJob copy = new DataxJob();
        copy.setJobName(source.getJobName() + "_copy");
        copy.setJobDesc(source.getJobDesc());
        copy.setJobType(source.getJobType());
        copy.setSourceDataSourceId(source.getSourceDataSourceId());
        copy.setSourceTable(source.getSourceTable());
        copy.setSourceQuerySql(source.getSourceQuerySql());
        copy.setTargetDataSourceId(source.getTargetDataSourceId());
        copy.setTargetTable(source.getTargetTable());
        copy.setWriteMode(source.getWriteMode());
        copy.setColumnMapping(source.getColumnMapping());
        copy.setCronExpression(source.getCronExpression());
        copy.setIncrementType(source.getIncrementType());
        copy.setIncrementColumn(source.getIncrementColumn());
        copy.setChannelCount(source.getChannelCount());
        copy.setEngineType(source.getEngineType());
        copy.setDataxHome(source.getDataxHome());
        copy.setBatchSize(source.getBatchSize());
        copy.setJobStatus(0);
        copy.setDelFlag(0);
        copy.setCreateTime(new Date());
        copy.setUpdateTime(new Date());
        
        jobMapper.insert(copy);
        return copy.getId();
    }

    /**
     * 获取任务日志列表
     */
    public PageResult<DataxJobLog> getJobLogList(Integer page, Integer pageSize, Long jobId, Integer status, String jobName) {
        if (page == null || page < 1) page = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;
        
        int offset = (page - 1) * pageSize;
        List<DataxJobLog> list = logMapper.selectList(offset, pageSize, jobId, status, jobName);
        long total = logMapper.count(jobId, status, jobName);
        
        return new PageResult<>(list, total);
    }

    /**
     * 根据ID获取日志详情
     */
    public DataxJobLog getLogById(Long id) {
        return logMapper.selectById(id);
    }

    /**
     * 获取日志统计
     */
    public Map<String, Object> getLogStatistics(Long jobId) {
        Map<String, Object> statistics = new HashMap<>();
        
        long total = logMapper.countByStatus(jobId, null);
        long success = logMapper.countByStatus(jobId, 1);
        long fail = logMapper.countByStatus(jobId, 0);
        
        statistics.put("total", total);
        statistics.put("success", success);
        statistics.put("fail", fail);
        
        String successRate = "0%";
        if (total > 0) {
            successRate = String.format("%.1f%%", (success * 100.0 / total));
        }
        statistics.put("successRate", successRate);
        
        return statistics;
    }

    /**
     * 生成DataX JSON配置
     */
    public String generateDataxJson(DataxJob job) {
        try {
            DataSource sourceDs = dataSourceMapper.selectById(job.getSourceDataSourceId());
            DataSource targetDs = dataSourceMapper.selectById(job.getTargetDataSourceId());
            
            if (sourceDs == null || targetDs == null) {
                return "{}";
            }
            
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"job\": {\n");
            json.append("    \"setting\": {\n");
            json.append("      \"speed\": { \"channel\": ").append(job.getChannelCount() != null ? job.getChannelCount() : 3).append(" },\n");
            json.append("      \"errorLimit\": { \"record\": 0, \"percentage\": 0.02 }\n");
            json.append("    },\n");
            json.append("    \"content\": [{\n");
            
            // Reader配置
            json.append("      \"reader\": {\n");
            json.append("        \"name\": \"").append(getReaderName(sourceDs.getDbType())).append("\",\n");
            json.append("        \"parameter\": {\n");
            json.append("          \"username\": \"").append(sourceDs.getUsername()).append("\",\n");
            json.append("          \"password\": \"").append(sourceDs.getPassword() != null ? sourceDs.getPassword() : "").append("\",\n");
            json.append("          \"connection\": [{\n");
            json.append("            \"jdbcUrl\": [\"").append(dataSourceService.buildJdbcUrl(sourceDs)).append("\"],\n");
            
            if (StringUtils.hasText(job.getSourceQuerySql())) {
                json.append("            \"querySql\": [\"").append(escapeJson(job.getSourceQuerySql())).append("\"]\n");
            } else {
                json.append("            \"table\": [\"").append(job.getSourceTable()).append("\"]\n");
            }
            json.append("          }]\n");
            if (!StringUtils.hasText(job.getSourceQuerySql())) {
                json.append("          ,\"column\": [\"*\"]\n");
            }
            json.append("        }\n");
            json.append("      },\n");
            
            // Writer配置
            json.append("      \"writer\": {\n");
            json.append("        \"name\": \"").append(getWriterName(targetDs.getDbType())).append("\",\n");
            json.append("        \"parameter\": {\n");
            json.append("          \"username\": \"").append(targetDs.getUsername()).append("\",\n");
            json.append("          \"password\": \"").append(targetDs.getPassword() != null ? targetDs.getPassword() : "").append("\",\n");
            json.append("          \"writeMode\": \"").append(job.getWriteMode() != null ? job.getWriteMode() : "insert").append("\",\n");
            json.append("          \"column\": [\"*\"],\n");
            json.append("          \"connection\": [{\n");
            json.append("            \"jdbcUrl\": \"").append(dataSourceService.buildJdbcUrl(targetDs)).append("\",\n");
            String targetTable = StringUtils.hasText(job.getTargetTable()) ? job.getTargetTable() : job.getSourceTable();
            json.append("            \"table\": [\"").append(targetTable).append("\"]\n");
            json.append("          }]\n");
            json.append("        }\n");
            json.append("      }\n");
            
            json.append("    }]\n");
            json.append("  }\n");
            json.append("}");
            
            return json.toString();
        } catch (Exception e) {
            log.error("生成DataX JSON配置失败", e);
            return "{}";
        }
    }

    /**
     * 预览DataX JSON配置
     */
    public String previewDataxJson(DataxJob job) {
        return generateDataxJson(job);
    }

    /**
     * 获取源表字段列表
     */
    public List<Map<String, Object>> getSourceColumns(Long dataSourceId, String tableName) {
        List<Map<String, Object>> columns = new ArrayList<>();
        DataSource ds = dataSourceMapper.selectById(dataSourceId);
        if (ds == null) {
            return columns;
        }
        
        try (Connection conn = connectionPoolManager.getConnection(ds)) {
            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet rs = metaData.getColumns(null, ds.getDatabase(), tableName, "%")) {
                while (rs.next()) {
                    Map<String, Object> column = new HashMap<>();
                    column.put("columnName", rs.getString("COLUMN_NAME"));
                    column.put("dataType", rs.getString("TYPE_NAME"));
                    column.put("columnSize", rs.getInt("COLUMN_SIZE"));
                    column.put("nullable", rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable);
                    column.put("remarks", rs.getString("REMARKS"));
                    columns.add(column);
                }
            }
        } catch (Exception e) {
            log.error("获取表字段失败", e);
        }
        
        return columns;
    }

    private String getReaderName(String dbType) {
        switch (dbType.toUpperCase()) {
            case "MYSQL": 
            case "MARIADB":
            case "TIDB":
            case "OCEANBASE":
                return "mysqlreader";
            case "ORACLE": 
            case "DM":
                return "oraclereader";
            case "SQLSERVER": return "sqlserverreader";
            case "POSTGRESQL":
            case "KINGBASE":
                return "postgresqlreader";
            case "CLICKHOUSE": return "clickhousereader";
            default: return "rdbmsreader";
        }
    }

    private String getWriterName(String dbType) {
        switch (dbType.toUpperCase()) {
            case "MYSQL":
            case "MARIADB":
            case "TIDB":
            case "OCEANBASE":
                return "mysqlwriter";
            case "ORACLE":
            case "DM":
                return "oraclewriter";
            case "SQLSERVER": return "sqlserverwriter";
            case "POSTGRESQL":
            case "KINGBASE":
                return "postgresqlwriter";
            case "CLICKHOUSE": return "clickhousewriter";
            default: return "rdbmswriter";
        }
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    private void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                log.error("关闭连接失败", e);
            }
        }
    }

    // ==================== 任务模板功能 ====================

    /**
     * 获取任务模板列表
     */
    public List<DataxJob> getTemplates() {
        return jobMapper.selectTemplates();
    }

    /**
     * 保存为模板
     */
    @Transactional
    public Long saveAsTemplate(Long jobId, String templateName) {
        DataxJob job = jobMapper.selectById(jobId);
        if (job == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "任务不存在");
        }
        
        DataxJob template = new DataxJob();
        template.setJobName(templateName);
        template.setJobDesc(job.getJobDesc());
        template.setJobType(2); // 2表示模板
        template.setSourceDataSourceId(job.getSourceDataSourceId());
        template.setSourceTable(job.getSourceTable());
        template.setSourceQuerySql(job.getSourceQuerySql());
        template.setTargetDataSourceId(job.getTargetDataSourceId());
        template.setTargetTable(job.getTargetTable());
        template.setColumnMapping(job.getColumnMapping());
        template.setWriteMode(job.getWriteMode());
        template.setChannelCount(job.getChannelCount());
        template.setEngineType(job.getEngineType());
        template.setDataxHome(job.getDataxHome());
        template.setBatchSize(job.getBatchSize());
        template.setIncrementType(job.getIncrementType());
        template.setIncrementColumn(job.getIncrementColumn());
        template.setDefaultParameters(job.getDefaultParameters());
        template.setJobStatus(0);
        template.setDelFlag(0);
        template.setCreateTime(new Date());
        template.setUpdateTime(new Date());
        
        jobMapper.insert(template);
        return template.getId();
    }

    /**
     * 从模板创建任务
     */
    @Transactional
    public Long createFromTemplate(Long templateId, String jobName) {
        DataxJob template = jobMapper.selectById(templateId);
        if (template == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "模板不存在");
        }
        
        DataxJob job = new DataxJob();
        job.setJobName(jobName);
        job.setJobDesc(template.getJobDesc());
        job.setJobType(1); // 1表示普通任务
        job.setSourceDataSourceId(template.getSourceDataSourceId());
        job.setSourceTable(template.getSourceTable());
        job.setSourceQuerySql(template.getSourceQuerySql());
        job.setTargetDataSourceId(template.getTargetDataSourceId());
        job.setTargetTable(template.getTargetTable());
        job.setColumnMapping(template.getColumnMapping());
        job.setWriteMode(template.getWriteMode());
        job.setChannelCount(template.getChannelCount());
        job.setEngineType(template.getEngineType());
        job.setDataxHome(template.getDataxHome());
        job.setBatchSize(template.getBatchSize());
        job.setIncrementType(template.getIncrementType());
        job.setIncrementColumn(template.getIncrementColumn());
        job.setDefaultParameters(template.getDefaultParameters());
        job.setJobStatus(0);
        job.setDelFlag(0);
        job.setCreateTime(new Date());
        job.setUpdateTime(new Date());
        
        jobMapper.insert(job);
        return job.getId();
    }

    /**
     * 删除模板
     */
    @Transactional
    public void deleteTemplate(Long id) {
        DataxJob template = jobMapper.selectById(id);
        if (template == null || template.getJobType() != 2) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "模板不存在");
        }
        jobMapper.delete(id);
    }

    // ==================== 增量同步状态 ====================

    /**
     * 获取增量同步状态
     */
    public Map<String, Object> getIncrementStatus(Long jobId) {
        DataxJob job = jobMapper.selectById(jobId);
        if (job == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "任务不存在");
        }
        
        Map<String, Object> status = new HashMap<>();
        status.put("incrementType", job.getIncrementType());
        status.put("incrementColumn", job.getIncrementColumn());
        status.put("lastIncrementValue", job.getLastIncrementValue());
        status.put("lastExecuteTime", job.getLastExecuteTime());
        
        return status;
    }

    /**
     * 重置增量同步位置
     */
    @Transactional
    public void resetIncrement(Long jobId) {
        DataxJob job = jobMapper.selectById(jobId);
        if (job == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "任务不存在");
        }
        
        job.setLastIncrementValue(null);
        job.setUpdateTime(new Date());
        jobMapper.update(job);
        
        log.info("已重置任务 {} 的增量同步位置", jobId);
    }

    // ==================== 统计与监控 ====================

    /**
     * 获取任务执行趋势（最近7天）
     */
    public List<Map<String, Object>> getExecutionTrend() {
        return logMapper.selectExecutionTrend(7);
    }

    /**
     * 获取任务概览统计
     */
    public Map<String, Object> getOverviewStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // 任务统计
        stats.put("totalJobs", jobMapper.count(null, null));
        stats.put("runningJobs", jobMapper.count(null, 1));
        stats.put("templateCount", jobMapper.countTemplates());
        
        // 今日执行统计
        Map<String, Object> todayStats = logMapper.selectTodayStatistics();
        if (todayStats != null) {
            stats.putAll(todayStats);
        } else {
            stats.put("todayTotal", 0);
            stats.put("todaySuccess", 0);
            stats.put("todayFailed", 0);
        }
        
        // 数据源统计
        stats.put("dataSourceCount", dataSourceMapper.count(null));
        
        return stats;
    }

    // ==================== 队列管理 ====================

    /**
     * 获取运行中的任务队列
     */
    public List<Map<String, Object>> getRunningQueue() {
        List<Map<String, Object>> queue = new ArrayList<>();
        for (Map.Entry<Long, Map<String, Object>> entry : runningJobs.entrySet()) {
            Map<String, Object> item = new HashMap<>(entry.getValue());
            item.put("logId", entry.getKey());
            queue.add(item);
        }
        return queue;
    }

    /**
     * 对比两次执行记录
     */
    public Map<String, Object> compareExecutions(Long logId1, Long logId2) {
        DataxJobLog log1 = logMapper.selectById(logId1);
        DataxJobLog log2 = logMapper.selectById(logId2);
        
        Map<String, Object> result = new HashMap<>();
        if (log1 == null || log2 == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "日志记录不存在");
        }
        
        Map<String, Object> exec1 = buildLogSummary(log1);
        Map<String, Object> exec2 = buildLogSummary(log2);
        result.put("execution1", exec1);
        result.put("execution2", exec2);
        
        // 计算差异
        Map<String, Object> diff = new HashMap<>();
        diff.put("readCountDiff", (log2.getReadCount() != null ? log2.getReadCount() : 0) - (log1.getReadCount() != null ? log1.getReadCount() : 0));
        diff.put("writeCountDiff", (log2.getWriteCount() != null ? log2.getWriteCount() : 0) - (log1.getWriteCount() != null ? log1.getWriteCount() : 0));
        diff.put("durationDiff", (log2.getDuration() != null ? log2.getDuration() : 0) - (log1.getDuration() != null ? log1.getDuration() : 0));
        
        long read1 = log1.getReadCount() != null ? log1.getReadCount() : 0;
        long read2 = log2.getReadCount() != null ? log2.getReadCount() : 0;
        long dur1 = log1.getDuration() != null ? log1.getDuration() : 1;
        long dur2 = log2.getDuration() != null ? log2.getDuration() : 1;
        diff.put("throughput1", dur1 > 0 ? (read1 * 1000.0 / dur1) : 0);
        diff.put("throughput2", dur2 > 0 ? (read2 * 1000.0 / dur2) : 0);
        result.put("diff", diff);
        
        return result;
    }
    
    private Map<String, Object> buildLogSummary(DataxJobLog logEntry) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("id", logEntry.getId());
        summary.put("jobName", logEntry.getJobName());
        summary.put("status", logEntry.getStatus());
        summary.put("startTime", logEntry.getStartTime());
        summary.put("endTime", logEntry.getEndTime());
        summary.put("readCount", logEntry.getReadCount());
        summary.put("writeCount", logEntry.getWriteCount());
        summary.put("duration", logEntry.getDuration());
        summary.put("errorMessage", logEntry.getErrorMessage());
        return summary;
    }

    // ==================== 参数管理 ====================

    /**
     * 获取任务参数定义
     */
    public List<Map<String, Object>> getJobParameters(Long jobId) {
        DataxJob job = jobMapper.selectById(jobId);
        if (job == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "任务不存在");
        }
        
        List<Map<String, Object>> parameters = new ArrayList<>();
        
        // 解析参数定义JSON
        if (StringUtils.hasText(job.getParameterDefinition())) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                parameters = mapper.readValue(job.getParameterDefinition(), 
                    new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
                
                // 填充默认值
                if (StringUtils.hasText(job.getDefaultParameters())) {
                    Map<String, Object> defaults = mapper.readValue(job.getDefaultParameters(),
                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
                    for (Map<String, Object> param : parameters) {
                        String name = (String) param.get("name");
                        if (name != null && defaults.containsKey(name)) {
                            param.put("defaultValue", defaults.get(name));
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("解析任务参数定义失败: {}", e.getMessage());
            }
        }
        
        return parameters;
    }

    /**
     * 更新任务参数定义
     */
    @Transactional
    public void updateJobParameters(Long jobId, String parameterDefinition, String defaultParameters) {
        DataxJob job = jobMapper.selectById(jobId);
        if (job == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "任务不存在");
        }
        
        job.setParameterDefinition(parameterDefinition);
        job.setDefaultParameters(defaultParameters);
        job.setUpdateTime(new Date());
        jobMapper.update(job);
    }
}
