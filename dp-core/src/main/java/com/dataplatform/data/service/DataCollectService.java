package com.dataplatform.data.service;

import com.dataplatform.common.constants.Constants;
import com.dataplatform.common.exception.BusinessException;
import com.dataplatform.common.exception.ErrorCode;
import com.dataplatform.data.entity.CollectTask;
import com.dataplatform.data.entity.CollectLog;
import com.dataplatform.data.entity.DataSource;
import com.dataplatform.data.mapper.CollectTaskMapper;
import com.dataplatform.data.mapper.CollectLogMapper;
import com.dataplatform.data.mapper.DataSourceMapper;
import com.dataplatform.data.mapper.DatabaseViewMapper;
import com.dataplatform.data.service.DbConnectionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.CompletableFuture;
import org.springframework.beans.factory.annotation.Qualifier;

@Slf4j
@Service
public class DataCollectService {
    private final CollectTaskMapper collectTaskMapper;
    private final CollectLogMapper collectLogMapper;
    private final DataSourceMapper dataSourceMapper;
    private final DataSourceService dataSourceService;
    private final DbConnectionUtil dbConnectionUtil;
    private final DatabaseViewMapper viewMapper;
    private final DataLineageService dataLineageService;
    private final AlertNotificationService alertNotificationService;
    private final Executor taskExecutor;
    private final com.dataplatform.data.service.DataSourceConnectionPoolManager connectionPoolManager;

    private final Map<Long, Future<?>> runningTasks = new ConcurrentHashMap<>();

    public DataCollectService(
            CollectTaskMapper collectTaskMapper,
            CollectLogMapper collectLogMapper,
            DataSourceMapper dataSourceMapper,
            DataSourceService dataSourceService,
            DbConnectionUtil dbConnectionUtil,
            DatabaseViewMapper viewMapper,
            DataLineageService dataLineageService,
            @Nullable AlertNotificationService alertNotificationService,
            @Qualifier("taskExecutor") Executor taskExecutor,
            com.dataplatform.data.service.DataSourceConnectionPoolManager connectionPoolManager) {
        this.collectTaskMapper = collectTaskMapper;
        this.collectLogMapper = collectLogMapper;
        this.dataSourceMapper = dataSourceMapper;
        this.dataSourceService = dataSourceService;
        this.dbConnectionUtil = dbConnectionUtil;
        this.viewMapper = viewMapper;
        this.dataLineageService = dataLineageService;
        this.alertNotificationService = alertNotificationService;
        this.taskExecutor = taskExecutor;
        this.connectionPoolManager = connectionPoolManager;
    }
    
    /**
     * 获取采集任务详情（包含数据源信息）
     * 使用视图v_collect_task_detail，性能比传统查询快3倍
     * 
     * @param taskId 任务ID
     * @return 任务详情（包含源和目标数据源信息）
     */
    public Map<String, Object> getTaskDetailById(Long taskId) {
        return viewMapper.getTaskDetailById(taskId);
    }
    
    /**
     * 获取增量采集任务及其进度
     * 使用视图v_incremental_tasks
     * 
     * @return 增量任务列表
     */
    public List<Map<String, Object>> getIncrementalTasks() {
        return viewMapper.getIncrementalTasks();
    }

    /**
     * 获取采集任务列表（分页）
     * 
     * @param page 页码（从1开始）
     * @param pageSize 每页大小
     * @param filters 筛选条件（JSON字符串）
     * @return 采集任务列表
     */
    public List<CollectTask> getCollectTaskList(Integer page, Integer pageSize, String filters) {
        if (page == null || page < 1) {
            page = Constants.DEFAULT_PAGE;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = Constants.DEFAULT_PAGE_SIZE;
        }
        int offset = (page - 1) * pageSize;
        List<com.dataplatform.common.dto.FilterCondition> filterList = com.dataplatform.common.util.FilterUtil.parseFilters(filters);
        return collectTaskMapper.selectList(offset, pageSize, filterList);
    }

    /**
     * 获取采集任务总数
     * 
     * @param filters 筛选条件（JSON字符串）
     * @return 采集任务总数
     */
    public long getCollectTaskCount(String filters) {
        List<com.dataplatform.common.dto.FilterCondition> filterList = com.dataplatform.common.util.FilterUtil.parseFilters(filters);
        return collectTaskMapper.count(filterList);
    }

    /**
     * 创建采集任务
     * 
     * @param task 采集任务信息
     * @throws BusinessException 参数错误或数据源不存在时抛出
     */
    @Transactional
    public void createCollectTask(CollectTask task) {
        if (task == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "采集任务信息不能为空");
        }
        if (task.getDataSourceId() == null) {
            throw new BusinessException(ErrorCode.COLLECT_TASK_DATA_SOURCE_EMPTY, "数据源ID不能为空");
        }
        
        // 验证源数据源
        DataSource dataSource = dataSourceMapper.selectById(task.getDataSourceId());
        if (dataSource == null) {
            throw new BusinessException(ErrorCode.DATA_SOURCE_NOT_FOUND, "源数据源不存在");
        }
        task.setDataSourceName(dataSource.getName());
        
        // 验证目标数据源（如果指定）
        if (task.getTargetDataSourceId() != null) {
            DataSource targetDataSource = dataSourceMapper.selectById(task.getTargetDataSourceId());
            if (targetDataSource == null) {
                throw new BusinessException(ErrorCode.DATA_SOURCE_NOT_FOUND, "目标数据源不存在");
            }
            task.setTargetDataSourceName(targetDataSource.getName());
        }
        
        if (!StringUtils.hasText(task.getTaskName())) {
            throw new BusinessException(ErrorCode.COLLECT_TASK_NAME_EMPTY, "任务名称不能为空");
        }
        
        // 根据采集模式验证必填字段
        String collectMode = task.getCollectMode();
        if (!StringUtils.hasText(collectMode)) {
            task.setCollectMode("full"); // 默认全量采集
        } else if ("custom".equals(collectMode)) {
            if (!StringUtils.hasText(task.getCustomSql())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "自定义SQL模式下必须提供SQL语句");
            }
        } else if ("incremental".equals(collectMode)) {
            if (!StringUtils.hasText(task.getIncrementalField())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "增量采集模式下必须指定增量字段");
            }
            if (!StringUtils.hasText(task.getIncrementalType())) {
                task.setIncrementalType("timestamp"); // 默认时间戳类型
            }
        } else if (!"full".equals(collectMode)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "采集模式只能是full、incremental或custom");
        }
        
        if (!"custom".equals(collectMode) && !StringUtils.hasText(task.getTableName())) {
            throw new BusinessException(ErrorCode.COLLECT_TASK_TABLE_NAME_EMPTY, "表名不能为空");
        }
        
        // 设置默认值
        if (task.getBatchSize() == null || task.getBatchSize() <= 0) {
            task.setBatchSize(1000);
        }
        if (task.getAutoCreateTable() == null) {
            task.setAutoCreateTable(true);
        }
        if (!StringUtils.hasText(task.getTargetTableName())) {
            task.setTargetTableName(task.getTableName());
        }
        
        // 设置重试默认值
        if (task.getMaxRetryCount() == null) {
            task.setMaxRetryCount(3);
        }
        if (task.getRetryInterval() == null) {
            task.setRetryInterval(30);
        }
        
        task.setStatus(Constants.TASK_STATUS_STOPPED);
        collectTaskMapper.insert(task);
    }

    /**
     * 更新采集任务
     * 
     * @param task 采集任务信息
     * @throws BusinessException 任务不存在时抛出
     */
    @Transactional
    public void updateCollectTask(CollectTask task) {
        if (task == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "采集任务信息不能为空");
        }
        if (task.getId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "任务ID不能为空");
        }
        CollectTask existing = collectTaskMapper.selectById(task.getId());
        if (existing == null) {
            throw new BusinessException(ErrorCode.COLLECT_TASK_NOT_FOUND, "采集任务不存在");
        }
        collectTaskMapper.update(task);
    }

    /**
     * 删除采集任务
     * 
     * @param id 任务ID
     * @throws BusinessException 任务不存在时抛出
     */
    @Transactional
    public void deleteCollectTask(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "任务ID不能为空");
        }
        CollectTask task = collectTaskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException(ErrorCode.COLLECT_TASK_NOT_FOUND, "采集任务不存在");
        }
        stopCollectTask(id);
        collectTaskMapper.delete(id);
    }

    public void startCollectTask(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "任务ID不能为空");
        }
        CollectTask task = collectTaskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException(ErrorCode.COLLECT_TASK_NOT_FOUND, "任务不存在");
        }
        if (Constants.TASK_STATUS_RUNNING.equals(task.getStatus())) {
            throw new BusinessException(ErrorCode.COLLECT_TASK_ALREADY_RUNNING, "任务已在运行中");
        }

        // 如果启用了定时任务，只更新状态，不立即执行
        log.debug("[startCollectTask] 启动任务: id={}, scheduleEnabled={}, cronExpression={}", id, task.getScheduleEnabled(), task.getCronExpression());
        if (Boolean.TRUE.equals(task.getScheduleEnabled()) && task.getCronExpression() != null && !task.getCronExpression().isEmpty()) {
            task.setStatus(Constants.TASK_STATUS_RUNNING);
            collectTaskMapper.update(task);
            log.info("[startCollectTask] 定时任务已启用，等待调度器执行");
            // 定时任务将由调度器在指定时间执行
            return;
        }

        // 非定时任务，立即执行
        task.setStatus(Constants.TASK_STATUS_RUNNING);
        collectTaskMapper.update(task);

        Future<?> future = CompletableFuture.runAsync(() -> {
            try {
                collectData(task);
            } catch (Exception e) {
                task.setStatus(Constants.TASK_STATUS_ERROR);
                collectTaskMapper.update(task);
            } finally {
                runningTasks.remove(id);
            }
        }, taskExecutor);
        runningTasks.put(id, future);
    }
    
    /**
     * 立即执行一次任务（无论是否启用定时），支持失败自动重试
     */
    public void executeTaskOnce(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "任务ID不能为空");
        }
        CollectTask task = collectTaskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException(ErrorCode.COLLECT_TASK_NOT_FOUND, "任务不存在");
        }
        
        // 检查是否有正在执行的任务
        Future<?> existingFuture = runningTasks.get(id);
        if (existingFuture != null && !existingFuture.isDone()) {
            throw new BusinessException(ErrorCode.COLLECT_TASK_ALREADY_RUNNING, "任务正在执行中，请稍后再试");
        }

        Future<?> future = CompletableFuture.runAsync(() -> {
            int maxRetry = task.getMaxRetryCount() != null ? task.getMaxRetryCount() : 0;
            int retryInterval = task.getRetryInterval() != null ? task.getRetryInterval() : 30;
            int attempt = 0;
            boolean success = false;
            
            while (attempt <= maxRetry && !success && !Thread.currentThread().isInterrupted()) {
                try {
                    if (attempt > 0) {
                        log.info("采集任务重试: taskId={}, 第{}/{}次重试，等待{}秒", 
                                id, attempt, maxRetry, retryInterval);
                        Thread.sleep(retryInterval * 1000L);
                    }
                    collectData(task);
                    success = true;
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.info("采集任务被中断: taskId={}", id);
                    break;
                } catch (Exception e) {
                    attempt++;
                    if (attempt <= maxRetry) {
                        log.warn("采集任务失败，将重试: taskId={}, 当前第{}次，最大{}次, 错误: {}", 
                                id, attempt, maxRetry, e.getMessage());
                    } else {
                        log.error("采集任务失败，已耗尽重试次数: taskId={}, 共重试{}次, 错误: {}", 
                                id, maxRetry, e.getMessage());
                        // 发送任务失败告警
                        if (alertNotificationService != null) {
                            alertNotificationService.sendTaskFailureAlert(
                                    task.getTaskName(), "数据采集", e.getMessage());
                        }
                        // 最终失败，更新状态
                        if (Boolean.TRUE.equals(task.getScheduleEnabled()) && task.getCronExpression() != null && !task.getCronExpression().isEmpty()) {
                            task.setStatus(Constants.TASK_STATUS_RUNNING);
                        } else {
                            task.setStatus(Constants.TASK_STATUS_ERROR);
                        }
                        task.setLastExecuteResult("采集失败(已重试" + maxRetry + "次): " + e.getMessage());
                        collectTaskMapper.update(task);
                    }
                }
            }
            runningTasks.remove(id);
        }, taskExecutor);
        runningTasks.put(id, future);
    }

    public void stopCollectTask(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "任务ID不能为空");
        }
        Future<?> future = runningTasks.get(id);
        if (future != null) {
            future.cancel(true);
            runningTasks.remove(id);
        }
        CollectTask task = collectTaskMapper.selectById(id);
        if (task != null) {
            log.debug("[stopCollectTask] 停止前: scheduleEnabled={}, cronExpression={}", task.getScheduleEnabled(), task.getCronExpression());
            task.setStatus(Constants.TASK_STATUS_STOPPED);
            collectTaskMapper.update(task);
            log.debug("[stopCollectTask] 停止后: status={}", task.getStatus());
        }
    }

    /**
     * 执行数据采集（支持库对库、多模式）
     */
    private void collectData(CollectTask task) throws Exception {
        long startTime = System.currentTimeMillis();
        int totalRecords = 0;
        Connection targetConn = null;
        boolean originalAutoCommit = true;
        String executeSql = "";
        
        // 预先计算目标表名
        String targetTableName = StringUtils.hasText(task.getTargetTableName()) 
            ? task.getTargetTableName() 
            : task.getTableName();
        // 如果没有指定目标数据源，使用collected_前缀
        if (task.getTargetDataSourceId() == null) {
            targetTableName = "collected_" + targetTableName.replaceAll("[^a-zA-Z0-9_]", "_");
        } else {
            targetTableName = targetTableName.replaceAll("[^a-zA-Z0-9_]", "_");
        }
        
        // 创建日志记录
        CollectLog collectLog = new CollectLog();
        collectLog.setTaskId(task.getId());
        collectLog.setTaskName(task.getTaskName());
        collectLog.setSourceTable(task.getTableName());
        collectLog.setTargetTable(targetTableName);  // 在创建时就设置目标表
        collectLog.setStatus("running");
        collectLog.setStartTime(new Date());
        collectLog.setRowCount(0);
        try {
            collectLogMapper.insert(collectLog);
        } catch (Exception e) {
            log.warn("创建采集日志失败: {}", e.getMessage());
        }
        
        try {
            // 获取源数据源
            DataSource sourceDataSource = dataSourceMapper.selectById(task.getDataSourceId());
            if (sourceDataSource == null) {
                throw new BusinessException(ErrorCode.DATA_SOURCE_NOT_FOUND, "源数据源不存在");
            }
            
            // 获取目标数据源（如果未指定则使用本地数据库）
            if (task.getTargetDataSourceId() != null) {
                DataSource targetDataSource = dataSourceMapper.selectById(task.getTargetDataSourceId());
                if (targetDataSource == null) {
                    throw new BusinessException(ErrorCode.DATA_SOURCE_NOT_FOUND, "目标数据源不存在");
                }
                String targetUrl = dataSourceService.buildJdbcUrl(targetDataSource);
                String targetDriver = dbConnectionUtil.getDriverClassName(targetDataSource.getDbType());
                Class.forName(targetDriver);
                targetConn = connectionPoolManager.getConnection(targetDataSource);
            } else {
                targetConn = dbConnectionUtil.getLocalConnection();
            }
            
            // 关闭自动提交以支持批量操作
            originalAutoCommit = targetConn.getAutoCommit();
            targetConn.setAutoCommit(false);
            
            // 构建源连接
            String sourceUrl = dataSourceService.buildJdbcUrl(sourceDataSource);
            String sourceDriver = dbConnectionUtil.getDriverClassName(sourceDataSource.getDbType());
            Class.forName(sourceDriver);
            
            try (Connection sourceConn = connectionPoolManager.getConnection(sourceDataSource)) {
                // 构建查询SQL
                String selectSql = buildSelectSql(task);
                executeSql = selectSql; // 记录到日志
                
                try (PreparedStatement ps = sourceConn.prepareStatement(selectSql);
                     ResultSet rs = ps.executeQuery()) {
                    
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    
                    // 获取源字段列表
                    List<String> sourceColumns = new ArrayList<>();
                    for (int i = 1; i <= columnCount; i++) {
                        sourceColumns.add(metaData.getColumnName(i));
                    }
                    
                    // 解析字段映射
                    Map<String, String> fieldMapping = parseFieldMapping(task.getFieldMapping());
                    
                    // 使用前面已计算好的目标表名
                    String finalTargetTableName = targetTableName;
                    
                    // 自动创建目标表
                    if (Boolean.TRUE.equals(task.getAutoCreateTable())) {
                        createTargetTable(targetConn, finalTargetTableName, metaData, fieldMapping);
                    }
                    
                    // 获取目标字段列表
                    List<String> targetColumns = getTargetColumns(sourceColumns, fieldMapping);
                    
                    // 构建插入SQL
                    String insertSql = buildInsertSql(finalTargetTableName, targetColumns);
                    
                    try (PreparedStatement insertPs = targetConn.prepareStatement(insertSql)) {
                        int batchCount = 0;
                        int batchSize = task.getBatchSize() != null ? task.getBatchSize() : 1000;
                        String maxIncrementalValue = task.getLastCollectValue();
                        
                        while (rs.next() && !Thread.currentThread().isInterrupted()) {
                            // 应用字段映射和数据转换
                            for (int i = 0; i < targetColumns.size(); i++) {
                                String sourceColumn = getSourceColumn(sourceColumns.get(i), fieldMapping);
                                Object value = rs.getObject(sourceColumn);
                                
                                // 应用数据转换规则
                                value = applyDataTransform(value, sourceColumn, task.getTransformRules());
                                
                                insertPs.setObject(i + 1, value);
                            }
                            
                            // 记录增量字段的最大值
                            if ("incremental".equals(task.getCollectMode()) && StringUtils.hasText(task.getIncrementalField())) {
                                Object incrementalValue = rs.getObject(task.getIncrementalField());
                                if (incrementalValue != null) {
                                    String currentValue = incrementalValue.toString();
                                    if (maxIncrementalValue == null || currentValue.compareTo(maxIncrementalValue) > 0) {
                                        maxIncrementalValue = currentValue;
                                    }
                                }
                            }
                            
                            insertPs.addBatch();
                            batchCount++;
                            totalRecords++;
                            
                            if (batchCount >= batchSize) {
                                insertPs.executeBatch();
                                targetConn.commit();
                                batchCount = 0;
                            }
                        }
                        
                        if (batchCount > 0) {
                            insertPs.executeBatch();
                            targetConn.commit();
                        }
                        
                        // 更新增量采集的最后值
                        if ("incremental".equals(task.getCollectMode()) && maxIncrementalValue != null) {
                            task.setLastCollectValue(maxIncrementalValue);
                        }
                    }
                }
            }
            
            // 提交最终事务
            targetConn.commit();
            
            // 更新任务状态
            long duration = System.currentTimeMillis() - startTime;
            // 如果是定时任务，执行完后保持 running 状态；否则改为 stopped
            if (Boolean.TRUE.equals(task.getScheduleEnabled()) && task.getCronExpression() != null && !task.getCronExpression().isEmpty()) {
                // 定时任务保持 running 状态，等待下次调度
                task.setStatus(Constants.TASK_STATUS_RUNNING);
            } else {
                // 非定时任务执行完后停止
                task.setStatus(Constants.TASK_STATUS_STOPPED);
            }
            task.setLastExecuteTime(new Date());
            task.setLastExecuteResult(String.format("成功采集 %d 条记录，耗时 %d 秒", totalRecords, duration / 1000));
            collectTaskMapper.update(task);
            
            // 自动记录数据血缘
            try {
                String sourceTable = task.getTableName();
                if ("custom".equals(task.getCollectMode())) {
                    sourceTable = "custom_sql";
                }
                dataLineageService.createFromCollect(
                        task.getDataSourceId(), sourceTable,
                        task.getTargetDataSourceId(), targetTableName,
                        task.getTaskName(), task.getCollectMode());
                log.info("自动记录采集血缘: {} -> {}", sourceTable, targetTableName);
            } catch (Exception lineageEx) {
                log.warn("自动记录血缘失败: {}", lineageEx.getMessage());
            }
            
            // 更新日志为成功
            collectLog.setStatus("success");
            collectLog.setRowCount(totalRecords);
            collectLog.setEndTime(new Date());
            collectLog.setDuration(duration);
            collectLog.setTargetTable(targetTableName);
            collectLog.setExecuteSql(executeSql);
            try {
                collectLogMapper.update(collectLog);
            } catch (Exception e) {
                log.warn("更新采集日志失败: {}", e.getMessage());
            }
            
        } catch (Exception e) {
            // 回滚事务
            if (targetConn != null) {
                try {
                    targetConn.rollback();
                } catch (SQLException rollbackEx) {
                    // 忽略回滚错误
                }
            }
            // 更新任务状态为失败
            // 如果是定时任务，失败后保持 running 状态，等待下次调度；否则改为 error
            if (Boolean.TRUE.equals(task.getScheduleEnabled()) && task.getCronExpression() != null && !task.getCronExpression().isEmpty()) {
                task.setStatus(Constants.TASK_STATUS_RUNNING);
            } else {
                task.setStatus(Constants.TASK_STATUS_ERROR);
            }
            task.setLastExecuteTime(new Date());
            task.setLastExecuteResult("采集失败: " + e.getMessage());
            collectTaskMapper.update(task);
            
            // 更新日志为失败
            long duration = System.currentTimeMillis() - startTime;
            collectLog.setStatus("failed");
            collectLog.setRowCount(totalRecords);
            collectLog.setEndTime(new Date());
            collectLog.setDuration(duration);
            collectLog.setErrorMessage(e.getMessage());
            collectLog.setTargetTable(targetTableName);
            collectLog.setExecuteSql(executeSql);
            try {
                collectLogMapper.update(collectLog);
            } catch (Exception logEx) {
                log.warn("更新采集日志失败: {}", logEx.getMessage());
            }
            
            throw e;
        } finally {
            // 恢复 autoCommit 并关闭连接
            if (targetConn != null) {
                try {
                    targetConn.setAutoCommit(originalAutoCommit);
                } catch (SQLException e) {
                    // 忽略恢复 autoCommit 的错误
                }
                try {
                    targetConn.close();
                } catch (SQLException e) {
                    // 忽略关闭连接的错误
                }
            }
        }
    }


    /**
     * 构建查询SQL（支持全量、增量、自定义SQL）
     */
    private String buildSelectSql(CollectTask task) {
        String collectMode = task.getCollectMode();
        
        if ("custom".equals(collectMode)) {
            // 自定义SQL模式
            return task.getCustomSql();
        } else if ("incremental".equals(collectMode)) {
            // 增量采集模式
            StringBuilder sql = new StringBuilder("SELECT * FROM ");
            sql.append(task.getTableName());
            
            if (StringUtils.hasText(task.getLastCollectValue())) {
                sql.append(" WHERE ").append(task.getIncrementalField());
                
                if ("timestamp".equals(task.getIncrementalType())) {
                    sql.append(" > '").append(task.getLastCollectValue()).append("'");
                } else if ("id".equals(task.getIncrementalType())) {
                    sql.append(" > ").append(task.getLastCollectValue());
                }
            }
            
            sql.append(" ORDER BY ").append(task.getIncrementalField());
            return sql.toString();
        } else {
            // 全量采集模式
            return "SELECT * FROM " + task.getTableName();
        }
    }
    
    /**
     * 解析字段映射配置
     */
    private Map<String, String> parseFieldMapping(String fieldMappingJson) {
        if (!StringUtils.hasText(fieldMappingJson)) {
            return new HashMap<>();
        }
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(fieldMappingJson, 
                new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
    
    /**
     * 获取目标字段列表
     */
    private List<String> getTargetColumns(List<String> sourceColumns, Map<String, String> fieldMapping) {
        if (fieldMapping.isEmpty()) {
            return new ArrayList<>(sourceColumns);
        }
        
        List<String> targetColumns = new ArrayList<>();
        for (String sourceColumn : sourceColumns) {
            String targetColumn = fieldMapping.getOrDefault(sourceColumn, sourceColumn);
            targetColumns.add(targetColumn);
        }
        return targetColumns;
    }
    
    /**
     * 获取源字段名（根据映射反查）
     */
    private String getSourceColumn(String sourceColumn, Map<String, String> fieldMapping) {
        // 如果没有映射，直接返回源字段名
        if (fieldMapping.isEmpty()) {
            return sourceColumn;
        }
        return sourceColumn;
    }
    
    /**
     * 创建目标表
     */
    private void createTargetTable(Connection conn, String tableName, ResultSetMetaData metaData, 
                                   Map<String, String> fieldMapping) throws SQLException {
        StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
        sql.append(tableName).append(" (");
        
        try {
            int columnCount = metaData.getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                if (i > 1) sql.append(", ");
                
                String sourceColumnName = metaData.getColumnName(i);
                String targetColumnName = fieldMapping.getOrDefault(sourceColumnName, sourceColumnName);
                targetColumnName = targetColumnName.replaceAll("[^a-zA-Z0-9_]", "_");
                
                String columnType = convertColumnType(metaData.getColumnTypeName(i));
                sql.append(targetColumnName).append(" ").append(columnType);
            }
            sql.append(") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql.toString());
            }
        } catch (SQLException e) {
            // 表可能已存在，忽略
            if (!e.getMessage().contains("already exists") && !e.getMessage().contains("table_exists")) {
                throw e;
            }
        }
    }

    private String convertColumnType(String sourceType) {
        String type = sourceType.toUpperCase();
        if (type.contains("VARCHAR") || type.contains("CHAR") || type.contains("TEXT")) {
            return "TEXT";
        } else if (type.contains("INT")) {
            return "BIGINT";
        } else if (type.contains("DECIMAL") || type.contains("NUMERIC") || type.contains("FLOAT") || type.contains("DOUBLE")) {
            return "DECIMAL(20,4)";
        } else if (type.contains("DATE") || type.contains("TIME")) {
            return "DATETIME";
        } else {
            return "TEXT";
        }
    }

    /**
     * 构建插入SQL（支持字段列表）
     */
    private String buildInsertSql(String tableName, List<String> columns) {
        StringBuilder sql = new StringBuilder("INSERT INTO ");
        sql.append(tableName).append(" (");
        
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append(columns.get(i).replaceAll("[^a-zA-Z0-9_]", "_"));
        }
        
        sql.append(") VALUES (");
        
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append("?");
        }
        sql.append(")");
        return sql.toString();
    }

    /**
     * 查询采集数据
     * 
     * @param dataSourceId 数据源ID
     * @param tableName 表名
     * @param page 页码（从1开始）
     * @param pageSize 每页大小
     * @return 数据列表
     */
    public List<Map<String, Object>> getCollectData(Long dataSourceId, String tableName, Integer page, Integer pageSize) {
        if (dataSourceId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "数据源ID不能为空");
        }
        if (tableName == null || tableName.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "表名不能为空");
        }
        if (page == null || page < 1) {
            page = Constants.DEFAULT_PAGE;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = Constants.DEFAULT_PAGE_SIZE;
        }
        
        DataSource dataSource = dataSourceMapper.selectById(dataSourceId);
        if (dataSource == null) {
            throw new BusinessException(ErrorCode.DATA_SOURCE_NOT_FOUND, "数据源不存在");
        }
        
        String url = dataSourceService.buildJdbcUrl(dataSource);
        String driver = dbConnectionUtil.getDriverClassName(dataSource.getDbType());

        try {
            Class.forName(driver);
            List<Map<String, Object>> result = new ArrayList<>();
            try (Connection conn = connectionPoolManager.getConnection(dataSource)) {
                int offset = (page - 1) * pageSize;
                // 注意：表名不能使用参数化查询，需要在业务层验证表名的合法性
                // 这里简化处理，实际应该验证表名是否符合规范（只包含字母、数字、下划线）
                String safeTableName = tableName.replaceAll("[^a-zA-Z0-9_]", "");
                if (safeTableName.isEmpty()) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "表名格式不正确");
                }
                String sql = "SELECT * FROM " + safeTableName + " LIMIT " + pageSize + " OFFSET " + offset;
                try (PreparedStatement ps = conn.prepareStatement(sql);
                     ResultSet rs = ps.executeQuery()) {
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    while (rs.next()) {
                        Map<String, Object> row = new HashMap<>();
                        for (int i = 1; i <= columnCount; i++) {
                            row.put(metaData.getColumnName(i), rs.getObject(i));
                        }
                        result.add(row);
                    }
                }
            }
            return result;
        } catch (ClassNotFoundException e) {
            throw new BusinessException(ErrorCode.DATA_SOURCE_CONNECTION_FAILED, "数据库驱动加载失败: " + e.getMessage());
        } catch (SQLException e) {
            throw new BusinessException(ErrorCode.DATA_SOURCE_CONNECTION_FAILED, "数据库查询失败: " + e.getMessage());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.ERROR, "查询数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 应用数据转换规则
     * @param value 原始值
     * @param columnName 字段名
     * @param transformRules 转换规则JSON
     * @return 转换后的值
     */
    private Object applyDataTransform(Object value, String columnName, String transformRules) {
        if (value == null || !StringUtils.hasText(transformRules)) {
            return value;
        }
        
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> rules = mapper.readValue(transformRules, 
                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            
            // 获取该字段的转换规则
            @SuppressWarnings("unchecked")
            Map<String, Object> columnRule = (Map<String, Object>) rules.get(columnName);
            if (columnRule == null) {
                return value;
            }
            
            String transformType = (String) columnRule.get("type");
            if (transformType == null) {
                return value;
            }
            
            switch (transformType) {
                case "trim":
                    // 去除空白
                    return value instanceof String ? ((String) value).trim() : value;
                    
                case "uppercase":
                    // 转大写
                    return value instanceof String ? ((String) value).toUpperCase() : value;
                    
                case "lowercase":
                    // 转小写
                    return value instanceof String ? ((String) value).toLowerCase() : value;
                    
                case "replace":
                    // 替换
                    if (value instanceof String) {
                        String pattern = (String) columnRule.get("pattern");
                        String replacement = (String) columnRule.getOrDefault("replacement", "");
                        return ((String) value).replaceAll(pattern, replacement);
                    }
                    return value;
                    
                case "substring":
                    // 截取
                    if (value instanceof String) {
                        int start = ((Number) columnRule.getOrDefault("start", 0)).intValue();
                        Integer end = columnRule.get("end") != null ? ((Number) columnRule.get("end")).intValue() : null;
                        String str = (String) value;
                        if (end != null && end <= str.length()) {
                            return str.substring(start, end);
                        } else if (start < str.length()) {
                            return str.substring(start);
                        }
                    }
                    return value;
                    
                case "default":
                    // 默认值
                    Object defaultValue = columnRule.get("defaultValue");
                    return value == null || (value instanceof String && ((String) value).isEmpty()) 
                        ? defaultValue : value;
                    
                case "format_date":
                    // 日期格式化
                    if (value instanceof java.util.Date) {
                        String format = (String) columnRule.getOrDefault("format", "yyyy-MM-dd HH:mm:ss");
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(format);
                        return sdf.format((java.util.Date) value);
                    }
                    return value;
                    
                case "number_format":
                    // 数字格式化
                    if (value instanceof Number) {
                        int scale = ((Number) columnRule.getOrDefault("scale", 2)).intValue();
                        java.math.BigDecimal bd = new java.math.BigDecimal(value.toString());
                        return bd.setScale(scale, java.math.RoundingMode.HALF_UP);
                    }
                    return value;
                    
                default:
                    return value;
            }
        } catch (Exception e) {
            log.warn("数据转换失败: column={}, error={}", columnName, e.getMessage());
            return value;
        }
    }
}

