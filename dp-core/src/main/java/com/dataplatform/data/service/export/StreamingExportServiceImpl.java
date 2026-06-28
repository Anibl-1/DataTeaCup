package com.dataplatform.data.service.export;

import com.dataplatform.common.exception.BusinessException;
import com.dataplatform.common.exception.ErrorCode;
import com.dataplatform.data.dto.export.ExportConfig;
import com.dataplatform.data.dto.export.ExportProgress;
import com.dataplatform.data.dto.export.ExportRequest;
import com.dataplatform.data.entity.ChartDefinition;
import com.dataplatform.data.entity.DataSource;
import com.dataplatform.data.entity.ExportTask;
import com.dataplatform.data.entity.ReportDefinition;
import com.dataplatform.data.service.masking.MaskingRule;
import com.dataplatform.data.mapper.ChartDefinitionMapper;
import com.dataplatform.data.mapper.DataSourceMapper;
import com.dataplatform.data.mapper.ExportTaskMapper;
import com.dataplatform.data.mapper.ReportDefinitionMapper;
import com.dataplatform.data.service.ChartDataExportService;
import com.dataplatform.data.service.DataSourceService;
import com.dataplatform.data.service.masking.MaskingEngine;
import com.dataplatform.data.service.masking.MaskingRuleService;
import com.dataplatform.data.service.DataSourceConnectionPoolManager;
import com.dataplatform.data.service.DbConnectionUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 流式导出服务实现
 * 支持异步导出任务管理、进度跟踪和断点续传
 * 
 * 需求: 17.2 - 当导出数据量超过50000行时，创建后台异步任务处理
 */
@Slf4j
@Service
public class StreamingExportServiceImpl implements StreamingExportService {
    
    @Value("${export.file.path:../runtime/exports}")
    private String exportPath;
    
    @Value("${export.excel.max-rows-per-sheet:1000000}")
    private int maxRowsPerSheet;
    
    @Value("${export.excel.flush-rows:500}")
    private int flushRows;
    
    @Value("${export.file.expire.days:7}")
    private int expireDays;
    
    @Value("${export.async.threshold:50000}")
    private long asyncThreshold;
    
    @Value("${export.compression.threshold:10485760}")
    private long compressionThreshold; // 10MB
    
    @Value("${db.query.timeout:300}")
    private int queryTimeout;
    
    @Value("${export.max-concurrent:3}")
    private int maxConcurrent;
    
    @Value("${export.max-rows-per-export:5000000}")
    private long maxRowsPerExport;
    
    @Autowired
    private ExportTaskMapper exportTaskMapper;
    
    @Autowired
    private DataSourceMapper dataSourceMapper;
    
    @Autowired
    private DataSourceService dataSourceService;
    
    @Autowired
    private DbConnectionUtil dbConnectionUtil;
    
    @Autowired
    private DataSourceConnectionPoolManager connectionPoolManager;
    
    @Autowired(required = false)
    private MaskingEngine maskingEngine;
    
    @Autowired(required = false)
    private MaskingRuleService maskingRuleService;
    
    @Autowired(required = false)
    private ChartDataExportService chartDataExportService;
    
    @Autowired
    private ReportDefinitionMapper reportDefinitionMapper;
    
    @Autowired
    private ChartDefinitionMapper chartDefinitionMapper;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // 并发导出限流信号量（防止多个大文件导出并发导致OOM）
    private Semaphore exportSemaphore;
    
    @jakarta.annotation.PostConstruct
    public void init() {
        exportSemaphore = new Semaphore(maxConcurrent);
        log.info("导出服务初始化: maxConcurrent={}, maxRowsPerExport={}", maxConcurrent, maxRowsPerExport);
    }
    
    // 用于跟踪正在运行的任务，支持取消操作
    private final ConcurrentHashMap<Long, Boolean> runningTasks = new ConcurrentHashMap<>();
    
    // 用于跟踪任务的处理速度
    private final ConcurrentHashMap<Long, Long> taskStartTimes = new ConcurrentHashMap<>();
    
    /**
     * 定时清理过期导出文件
     * 每天凌晨2点执行，清理超过 expireDays 天的导出文件
     */
    @org.springframework.scheduling.annotation.Scheduled(cron = "0 0 2 * * ?")
    public void cleanupExpiredExportFiles() {
        // 1. 清理过期的物理文件
        File exportDir = new File(exportPath);
        int cleaned = 0;
        if (exportDir.exists() && exportDir.isDirectory()) {
            long cutoffMs = System.currentTimeMillis() - (long) expireDays * 24 * 3600 * 1000;
            File[] files = exportDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.lastModified() < cutoffMs) {
                        if (file.delete()) cleaned++;
                    }
                }
            }
        }
        
        // 2. 将已过期的DB任务记录标记为已过期，避免用户下载已删除的文件
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(expireDays);
            int dbCleaned = exportTaskMapper.markExpiredTasks(cutoffTime);
            if (cleaned > 0 || dbCleaned > 0) {
                log.info("定时清理过期导出: 文件 {} 个, DB记录 {} 条", cleaned, dbCleaned);
            }
        } catch (Exception e) {
            // DB清理失败不影响文件清理，仅记录日志
            log.warn("清理过期导出DB记录失败: {}", e.getMessage());
            if (cleaned > 0) {
                log.info("定时清理过期导出文件: 清理了 {} 个文件", cleaned);
            }
        }
    }
    
    @Override
    public void exportExcelStream(OutputStream out, String sql, Long dataSourceId,
                                  List<MaskingRule> maskingRules, ExportConfig config) {
        DataSource dataSource = dataSourceMapper.selectById(dataSourceId);
        if (dataSource == null) {
            throw new BusinessException(ErrorCode.DATA_SOURCE_NOT_FOUND, "数据源不存在");
        }
        
        ExportConfig effectiveConfig = config != null ? config : ExportConfig.defaultConfig();
        int batchSize = effectiveConfig.getBatchSize() != null ? effectiveConfig.getBatchSize() : 1000;
        
        SXSSFWorkbook workbook = null;
        try {
            workbook = new SXSSFWorkbook(flushRows);
            workbook.setCompressTempFiles(true);
            
            String sheetName = effectiveConfig.getSheetName() != null ? effectiveConfig.getSheetName() : "数据";
            Sheet sheet = workbook.createSheet(sheetName);
            
            try (Connection conn = connectionPoolManager.getConnection(dataSource);
                 PreparedStatement ps = conn.prepareStatement(sql,
                         ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
                
                ps.setQueryTimeout(queryTimeout);
                ps.setFetchSize(Integer.MIN_VALUE);
                
                try (ResultSet rs = ps.executeQuery()) {
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    
                    // 创建表头
                    if (Boolean.TRUE.equals(effectiveConfig.getIncludeHeader())) {
                        Row headerRow = sheet.createRow(0);
                        CellStyle headerStyle = createHeaderStyle(workbook);
                        for (int i = 1; i <= columnCount; i++) {
                            Cell cell = headerRow.createCell(i - 1);
                            String columnName = metaData.getColumnLabel(i);
                            if (effectiveConfig.getFieldLabels() != null) {
                                columnName = effectiveConfig.getFieldLabels()
                                        .getOrDefault(metaData.getColumnName(i), columnName);
                            }
                            cell.setCellValue(columnName);
                            cell.setCellStyle(headerStyle);
                        }
                    }
                    
                    int rowNum = Boolean.TRUE.equals(effectiveConfig.getIncludeHeader()) ? 1 : 0;
                    int sheetIndex = 1;
                    
                    while (rs.next()) {
                        // 检查是否需要创建新Sheet
                        if (rowNum >= maxRowsPerSheet) {
                            ((SXSSFSheet) sheet).flushRows();
                            sheetIndex++;
                            sheet = workbook.createSheet(sheetName + "_" + sheetIndex);
                            rowNum = 0;
                            
                            // 新Sheet也需要表头
                            if (Boolean.TRUE.equals(effectiveConfig.getIncludeHeader())) {
                                Row headerRow = sheet.createRow(0);
                                CellStyle headerStyle = createHeaderStyle(workbook);
                                for (int i = 1; i <= columnCount; i++) {
                                    Cell cell = headerRow.createCell(i - 1);
                                    String columnName = metaData.getColumnLabel(i);
                                    cell.setCellValue(columnName);
                                    cell.setCellStyle(headerStyle);
                                }
                                rowNum = 1;
                            }
                        }
                        
                        // 直接写入行数据（避免中间Map分配，减少GC压力）
                        Row row = sheet.createRow(rowNum++);
                        for (int i = 1; i <= columnCount; i++) {
                            Cell cell = row.createCell(i - 1);
                            setCellValue(cell, rs.getObject(i));
                        }
                        
                        // 定期刷新
                        if (rowNum % batchSize == 0) {
                            ((SXSSFSheet) sheet).flushRows();
                        }
                    }
                }
            }
            
            workbook.write(out);
            
        } catch (Exception e) {
            log.error("流式导出Excel失败", e);
            throw new BusinessException(ErrorCode.DATA_EXPORT_FAILED, "导出失败: " + e.getMessage());
        } finally {
            if (workbook != null) {
                try { workbook.dispose(); } catch (Exception ignored) {}
                try { workbook.close(); } catch (Exception ignored) {}
            }
        }
    }
    
    @Override
    public void exportCsvStream(OutputStream out, String sql, Long dataSourceId,
                               List<MaskingRule> maskingRules, ExportConfig config) {
        DataSource dataSource = dataSourceMapper.selectById(dataSourceId);
        if (dataSource == null) {
            throw new BusinessException(ErrorCode.DATA_SOURCE_NOT_FOUND, "数据源不存在");
        }
        
        ExportConfig effectiveConfig = config != null ? config : ExportConfig.defaultConfig();
        String separator = effectiveConfig.getSeparator() != null ? effectiveConfig.getSeparator() : ",";
        
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, "UTF-8"))) {
            try (Connection conn = connectionPoolManager.getConnection(dataSource);
                 PreparedStatement ps = conn.prepareStatement(sql,
                         ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
                
                ps.setQueryTimeout(queryTimeout);
                ps.setFetchSize(Integer.MIN_VALUE);
                
                try (ResultSet rs = ps.executeQuery()) {
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    
                    // 写入表头
                    if (Boolean.TRUE.equals(effectiveConfig.getIncludeHeader())) {
                        StringBuilder header = new StringBuilder();
                        for (int i = 1; i <= columnCount; i++) {
                            if (i > 1) header.append(separator);
                            String columnName = metaData.getColumnLabel(i);
                            if (effectiveConfig.getFieldLabels() != null) {
                                columnName = effectiveConfig.getFieldLabels()
                                        .getOrDefault(metaData.getColumnName(i), columnName);
                            }
                            header.append(escapeCsvValue(columnName, separator));
                        }
                        writer.println(header);
                    }
                    
                    while (rs.next()) {
                        // 直接写入行数据（避免中间Map分配，减少GC压力）
                        StringBuilder line = new StringBuilder();
                        for (int i = 1; i <= columnCount; i++) {
                            if (i > 1) line.append(separator);
                            Object value = rs.getObject(i);
                            line.append(escapeCsvValue(value != null ? value.toString() : "", separator));
                        }
                        writer.println(line);
                    }
                }
            }
        } catch (Exception e) {
            log.error("流式导出CSV失败", e);
            throw new BusinessException(ErrorCode.DATA_EXPORT_FAILED, "导出失败: " + e.getMessage());
        }
    }
    
    @Override
    public Long createAsyncExportTask(ExportRequest request) {
        // 创建导出任务
        ExportTask task = new ExportTask();
        task.setTaskName(request.getTaskName() != null ? request.getTaskName() : "导出任务_" + System.currentTimeMillis());
        task.setTaskType(request.getTaskType() != null ? request.getTaskType() : "excel");
        task.setRefId(request.getReportId());
        task.setRefCode(request.getReportCode());
        task.setFilters(request.getFilters());
        task.setParams(request.getParams());
        task.setStatus(0); // PENDING
        task.setProgress(0);
        task.setCreateBy(request.getUserId());
        task.setCreateTime(LocalDateTime.now());
        task.setExpireTime(LocalDateTime.now().plusDays(expireDays));
        
        exportTaskMapper.insert(task);
        
        log.info("创建异步导出任务: taskId={}, taskName={}, userId={}", 
                task.getId(), task.getTaskName(), request.getUserId());
        
        return task.getId();
    }
    
    @Override
    @Async
    public void executeAsyncExport(Long taskId) {
        ExportTask task = exportTaskMapper.selectById(taskId);
        if (task == null) {
            log.error("导出任务不存在: {}", taskId);
            return;
        }
        
        // 并发限流：获取信号量许可
        boolean acquired = false;
        try {
            acquired = exportSemaphore.tryAcquire(30, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            task.setStatus(3); // FAILED
            task.setErrorMsg("导出任务被中断");
            task.setFinishTime(LocalDateTime.now());
            exportTaskMapper.update(task);
            return;
        }
        if (!acquired) {
            log.warn("导出任务排队超时，当前并发已满: taskId={}", taskId);
            task.setStatus(3); // FAILED
            task.setErrorMsg("导出并发数已满，请稍后重试（当前限制: " + maxConcurrent + "个并发）");
            task.setFinishTime(LocalDateTime.now());
            exportTaskMapper.update(task);
            return;
        }
        
        // 标记任务开始运行
        runningTasks.put(taskId, true);
        taskStartTimes.put(taskId, System.currentTimeMillis());
        
        // 更新状态为处理中
        task.setStatus(1); // RUNNING
        task.setStartTime(LocalDateTime.now());
        exportTaskMapper.update(task);
        
        File outputFile = null;
        String fileName = null;
        String baseFileName = null;
        
        try {
            // 创建导出目录
            File exportDir = new File(exportPath);
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }
            
            baseFileName = task.getTaskName() + "_" + UUID.randomUUID().toString().substring(0, 8);
            long totalRows = 0;
            
            // 根据任务类型执行导出
            String taskType = task.getTaskType() != null ? task.getTaskType() : "excel";
            
            switch (taskType.toLowerCase()) {
                case "csv":
                    fileName = baseFileName + ".csv";
                    outputFile = new File(exportDir, fileName);
                    totalRows = executeExportToCsv(task, outputFile, taskId);
                    break;
                case "json":
                    fileName = baseFileName + ".json";
                    outputFile = new File(exportDir, fileName);
                    totalRows = executeExportToJson(task, outputFile, taskId);
                    break;
                default: // excel
                    fileName = baseFileName + ".xlsx";
                    outputFile = new File(exportDir, fileName);
                    totalRows = executeExportToExcel(task, outputFile, taskId);
                    break;
            }
            
            // 检查是否需要压缩
            String dataType = taskType;
            if (outputFile.length() > compressionThreshold) {
                File zipFile = new File(exportDir, baseFileName + ".zip");
                compressToZip(outputFile, zipFile);
                outputFile.delete();
                outputFile = zipFile;
                fileName = baseFileName + ".zip";
                dataType = "zip";
                log.info("文件超过{}MB，已压缩为ZIP: {}", compressionThreshold / 1024 / 1024, fileName);
            }
            
            // 更新任务状态为完成
            task.setStatus(2); // COMPLETED
            task.setProgress(100);
            task.setFilePath(outputFile.getAbsolutePath());
            task.setFileName(fileName);
            task.setFileSize(outputFile.length());
            task.setTotalRows(totalRows);
            task.setDataType(dataType);
            task.setFinishTime(LocalDateTime.now());
            task.setCheckpointOffset(totalRows);
            task.setProcessedRows(totalRows);
            task.setTempFilePath(null);
            exportTaskMapper.update(task);
            
            log.info("导出任务完成: taskId={}, totalRows={}, fileSize={}", 
                    taskId, totalRows, outputFile.length());
            
        } catch (Exception e) {
            log.error("导出任务失败: taskId={}", taskId, e);
            
            // 需求: 17.6 - 导出任务失败时保存已完成的部分并支持恢复
            // 尝试保存已完成的部分数据
            boolean partialSaved = savePartialExportOnFailure(task, outputFile, fileName, baseFileName, taskId);
            
            if (partialSaved) {
                // 设置为部分完成状态
                task.setStatus(6); // PARTIAL_COMPLETED
                log.info("导出任务部分完成: taskId={}, processedRows={}, filePath={}", 
                        taskId, task.getProcessedRows(), task.getFilePath());
            } else {
                // 没有部分数据可保存，设置为失败状态
                task.setStatus(3); // FAILED
            }
            
            task.setErrorMsg(e.getMessage());
            task.setFinishTime(LocalDateTime.now());
            exportTaskMapper.update(task);
        } finally {
            runningTasks.remove(taskId);
            taskStartTimes.remove(taskId);
            exportSemaphore.release();
        }
    }
    
    @Override
    public ExportProgress getProgress(Long taskId) {
        ExportTask task = exportTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "任务不存在");
        }
        
        ExportProgress.Status status = ExportProgress.Status.fromCode(task.getStatus());
        
        ExportProgress progress = ExportProgress.builder()
                .taskId(task.getId())
                .taskName(task.getTaskName())
                .status(status.name())
                .progressPercent(task.getProgress())
                .totalRows(task.getTotalRows())
                .processedRows(task.getProcessedRows() != null ? task.getProcessedRows() : calculateProcessedRows(task))
                .filePath(task.getFilePath())
                .fileName(task.getFileName())
                .fileSize(task.getFileSize())
                .errorMessage(task.getErrorMsg())
                .checkpointOffset(task.getCheckpointOffset())
                .createTime(task.getCreateTime())
                .startTime(task.getStartTime())
                .finishTime(task.getFinishTime())
                .expireTime(task.getExpireTime())
                .build();
        
        // 计算处理速度和预计剩余时间
        if (task.getStatus() == 1 && taskStartTimes.containsKey(taskId)) {
            long elapsedMs = System.currentTimeMillis() - taskStartTimes.get(taskId);
            if (elapsedMs > 0 && progress.getProcessedRows() != null && progress.getProcessedRows() > 0) {
                long rowsPerSecond = progress.getProcessedRows() * 1000 / elapsedMs;
                progress.setRowsPerSecond(rowsPerSecond);
                
                if (progress.getTotalRows() != null && progress.getTotalRows() > 0 && rowsPerSecond > 0) {
                    long remainingRows = progress.getTotalRows() - progress.getProcessedRows();
                    progress.setEstimatedRemainingSeconds(remainingRows / rowsPerSecond);
                }
            }
        }
        
        // 设置下载URL
        // 需求: 17.6 - 部分完成的任务也可以下载已完成的部分
        if ((task.getStatus() == 2 || task.getStatus() == 6) && task.getFilePath() != null) {
            progress.setDownloadUrl("/api/export/download/" + taskId);
        }
        
        return progress;
    }
    
    @Override
    public void resumeExport(Long taskId) {
        ExportTask task = exportTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "任务不存在");
        }
        
        // 只有失败、暂停或部分完成的任务可以恢复
        // 需求: 17.6 - 导出任务失败时保存已完成的部分并支持恢复
        if (task.getStatus() != 3 && task.getStatus() != 5 && task.getStatus() != 6) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "只有失败、暂停或部分完成的任务可以恢复");
        }
        
        // 检查是否有断点信息可以恢复
        Long checkpointOffset = task.getCheckpointOffset();
        boolean hasCheckpoint = checkpointOffset != null && checkpointOffset > 0;
        
        if (hasCheckpoint) {
            log.info("从断点恢复导出任务: taskId={}, checkpointOffset={}", taskId, checkpointOffset);
            // 从断点位置恢复
            executeResumeExport(taskId, checkpointOffset);
        } else {
            log.info("无断点信息，重新开始导出任务: taskId={}", taskId);
            // 没有断点信息，重新开始
            task.setStatus(0);
            task.setProgress(0);
            task.setErrorMsg(null);
            task.setCheckpointOffset(0L);
            task.setProcessedRows(0L);
            exportTaskMapper.update(task);
            executeAsyncExport(taskId);
        }
    }
    
    /**
     * 从断点位置恢复导出任务
     * 需求: 17.3 - 支持导出任务的断点续传功能
     * 
     * @param taskId 任务ID
     * @param checkpointOffset 断点位置（已处理的行数）
     */
    @Async
    public void executeResumeExport(Long taskId, Long checkpointOffset) {
        ExportTask task = exportTaskMapper.selectById(taskId);
        if (task == null) {
            log.error("导出任务不存在: {}", taskId);
            return;
        }
        
        // 并发限流：获取信号量许可（与executeAsyncExport保持一致）
        boolean acquired = false;
        try {
            acquired = exportSemaphore.tryAcquire(30, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            task.setStatus(3); // FAILED
            task.setErrorMsg("断点续传任务被中断");
            task.setFinishTime(LocalDateTime.now());
            exportTaskMapper.update(task);
            return;
        }
        if (!acquired) {
            log.warn("断点续传任务排队超时，当前并发已满: taskId={}", taskId);
            task.setStatus(3); // FAILED
            task.setErrorMsg("导出并发数已满，请稍后重试（当前限制: " + maxConcurrent + "个并发）");
            task.setFinishTime(LocalDateTime.now());
            exportTaskMapper.update(task);
            return;
        }
        
        // 标记任务开始运行
        runningTasks.put(taskId, true);
        taskStartTimes.put(taskId, System.currentTimeMillis());
        
        // 更新状态为处理中
        task.setStatus(1); // RUNNING
        task.setStartTime(LocalDateTime.now());
        task.setErrorMsg(null);
        exportTaskMapper.update(task);
        
        try {
            // 获取数据源
            DataSource dataSource = null;
            if (task.getDataSourceId() != null) {
                dataSource = dataSourceMapper.selectById(task.getDataSourceId());
            }
            if (dataSource == null) {
                dataSource = getDataSourceForTask(task);
            }
            
            // 获取SQL
            String sql = task.getExportSql();
            if (sql == null || sql.isEmpty()) {
                sql = buildSqlForTask(task, dataSource);
            }
            
            // 创建导出目录
            File exportDir = new File(exportPath);
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }
            
            String baseFileName = task.getTaskName() + "_" + UUID.randomUUID().toString().substring(0, 8);
            String taskType = task.getTaskType() != null ? task.getTaskType() : "excel";
            
            // 检查是否有临时文件可以继续
            File tempFile = null;
            if (task.getTempFilePath() != null && !task.getTempFilePath().isEmpty()) {
                tempFile = new File(task.getTempFilePath());
                if (!tempFile.exists()) {
                    tempFile = null;
                    checkpointOffset = 0L; // 临时文件不存在，需要重新开始
                }
            }
            
            String fileName;
            File outputFile;
            long totalRows;
            
            switch (taskType.toLowerCase()) {
                case "csv":
                    fileName = baseFileName + ".csv";
                    outputFile = new File(exportDir, fileName);
                    totalRows = executeResumeExportToCsv(task, outputFile, taskId, checkpointOffset, tempFile);
                    break;
                case "json":
                    fileName = baseFileName + ".json";
                    outputFile = new File(exportDir, fileName);
                    totalRows = executeResumeExportToJson(task, outputFile, taskId, checkpointOffset, tempFile);
                    break;
                default: // excel
                    fileName = baseFileName + ".xlsx";
                    outputFile = new File(exportDir, fileName);
                    totalRows = executeResumeExportToExcel(task, outputFile, taskId, checkpointOffset, tempFile);
                    break;
            }
            
            // 检查是否需要压缩
            String dataType = taskType;
            if (outputFile.length() > compressionThreshold) {
                File zipFile = new File(exportDir, baseFileName + ".zip");
                compressToZip(outputFile, zipFile);
                outputFile.delete();
                outputFile = zipFile;
                fileName = baseFileName + ".zip";
                dataType = "zip";
                log.info("文件超过{}MB，已压缩为ZIP: {}", compressionThreshold / 1024 / 1024, fileName);
            }
            
            // 清理临时文件
            if (tempFile != null && tempFile.exists() && !tempFile.equals(outputFile)) {
                tempFile.delete();
            }
            
            // 更新任务状态为完成
            task.setStatus(2); // COMPLETED
            task.setProgress(100);
            task.setFilePath(outputFile.getAbsolutePath());
            task.setFileName(fileName);
            task.setFileSize(outputFile.length());
            task.setTotalRows(totalRows);
            task.setDataType(dataType);
            task.setFinishTime(LocalDateTime.now());
            task.setCheckpointOffset(totalRows);
            task.setProcessedRows(totalRows);
            task.setTempFilePath(null);
            exportTaskMapper.update(task);
            
            log.info("断点续传导出任务完成: taskId={}, totalRows={}, fileSize={}", 
                    taskId, totalRows, outputFile.length());
            
        } catch (Exception e) {
            log.error("断点续传导出任务失败: taskId={}", taskId, e);
            // 保存当前进度作为断点
            task.setStatus(3); // FAILED
            task.setErrorMsg(e.getMessage());
            task.setFinishTime(LocalDateTime.now());
            exportTaskMapper.update(task);
        } finally {
            runningTasks.remove(taskId);
            taskStartTimes.remove(taskId);
            exportSemaphore.release();
        }
    }
    
    /**
     * 从断点恢复Excel导出
     */
    private long executeResumeExportToExcel(ExportTask task, File outputFile, Long taskId, 
                                            Long checkpointOffset, File tempFile) throws Exception {
        DataSource dataSource = task.getDataSourceId() != null ? 
                dataSourceMapper.selectById(task.getDataSourceId()) : getDataSourceForTask(task);
        String sql = task.getExportSql() != null ? task.getExportSql() : buildSqlForTask(task, dataSource);
        
        // 保存SQL和数据源信息用于后续断点续传
        if (task.getExportSql() == null || task.getDataSourceId() == null) {
            exportTaskMapper.updateExportInfo(taskId, sql, dataSource.getId());
        }
        
        SXSSFWorkbook workbook = new SXSSFWorkbook(flushRows);
        workbook.setCompressTempFiles(true);
        
        long totalRows = 0;
        long skippedRows = 0;
        
        try (Connection conn = connectionPoolManager.getConnection(dataSource)) {
            conn.setAutoCommit(false);
            
            try (PreparedStatement ps = conn.prepareStatement(sql,
                    ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
                
                ps.setQueryTimeout(queryTimeout * 3);
                ps.setFetchSize(Integer.MIN_VALUE);
                
                try (ResultSet rs = ps.executeQuery()) {
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    
                    // 创建表头样式
                    CellStyle headerStyle = createHeaderStyle(workbook);
                    
                    // 构建表头
                    String[] headers = new String[columnCount];
                    for (int i = 1; i <= columnCount; i++) {
                        headers[i - 1] = metaData.getColumnLabel(i);
                    }
                    
                    // 初始化第一个Sheet
                    int sheetIndex = 1;
                    Sheet currentSheet = workbook.createSheet("数据_" + sheetIndex);
                    createHeaderRow(currentSheet, headers, headerStyle);
                    int rowNumInSheet = 1;
                    
                    while (rs.next()) {
                        // 检查是否被取消
                        if (!isTaskRunning(taskId)) {
                            // 保存断点
                            saveCheckpoint(taskId, totalRows, outputFile);
                            log.info("导出任务被取消，已保存断点: taskId={}, checkpoint={}", taskId, totalRows);
                            break;
                        }
                        
                        // 跳过已处理的行（断点续传）
                        if (skippedRows < checkpointOffset) {
                            skippedRows++;
                            continue;
                        }
                        
                        // 检查是否需要创建新Sheet
                        if (rowNumInSheet >= maxRowsPerSheet) {
                            ((SXSSFSheet) currentSheet).flushRows();
                            sheetIndex++;
                            currentSheet = workbook.createSheet("数据_" + sheetIndex);
                            createHeaderRow(currentSheet, headers, headerStyle);
                            rowNumInSheet = 1;
                        }
                        
                        // 写入数据行
                        Row row = currentSheet.createRow(rowNumInSheet++);
                        for (int i = 1; i <= columnCount; i++) {
                            Cell cell = row.createCell(i - 1);
                            setCellValue(cell, rs.getObject(i));
                        }
                        
                        totalRows++;
                        
                        // 定期刷新和更新进度
                        if (totalRows % 1000 == 0) {
                            ((SXSSFSheet) currentSheet).flushRows();
                        }
                        
                        // 定期保存断点
                        if (totalRows % 10000 == 0) {
                            int progress = Math.min(95, (int) ((checkpointOffset + totalRows) / 1000));
                            exportTaskMapper.updateCheckpoint(taskId, checkpointOffset + totalRows, 
                                    checkpointOffset + totalRows, progress, null);
                        }
                    }
                }
            }
            
            // 写入文件
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                workbook.write(fos);
            }
            
        } finally {
            workbook.dispose();
            workbook.close();
        }
        
        return checkpointOffset + totalRows;
    }
    
    /**
     * 从断点恢复CSV导出
     */
    private long executeResumeExportToCsv(ExportTask task, File outputFile, Long taskId,
                                          Long checkpointOffset, File tempFile) throws Exception {
        DataSource dataSource = task.getDataSourceId() != null ? 
                dataSourceMapper.selectById(task.getDataSourceId()) : getDataSourceForTask(task);
        String sql = task.getExportSql() != null ? task.getExportSql() : buildSqlForTask(task, dataSource);
        
        // 保存SQL和数据源信息
        if (task.getExportSql() == null || task.getDataSourceId() == null) {
            exportTaskMapper.updateExportInfo(taskId, sql, dataSource.getId());
        }
        
        long totalRows = 0;
        long skippedRows = 0;
        boolean appendMode = tempFile != null && tempFile.exists() && checkpointOffset > 0;
        
        // 如果有临时文件且有断点，复制临时文件内容
        if (appendMode) {
            copyFile(tempFile, outputFile);
        }
        
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(outputFile, appendMode), "UTF-8"))) {
            
            try (Connection conn = connectionPoolManager.getConnection(dataSource)) {
                conn.setAutoCommit(false);
                
                try (PreparedStatement ps = conn.prepareStatement(sql,
                        ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
                    
                    ps.setQueryTimeout(queryTimeout * 3);
                    ps.setFetchSize(Integer.MIN_VALUE);
                    
                    try (ResultSet rs = ps.executeQuery()) {
                        ResultSetMetaData metaData = rs.getMetaData();
                        int columnCount = metaData.getColumnCount();
                        
                        // 如果不是追加模式，写入表头
                        if (!appendMode) {
                            StringBuilder header = new StringBuilder();
                            for (int i = 1; i <= columnCount; i++) {
                                if (i > 1) header.append(",");
                                header.append(escapeCsvValue(metaData.getColumnLabel(i), ","));
                            }
                            writer.println(header);
                        }
                        
                        while (rs.next()) {
                            // 检查是否被取消
                            if (!isTaskRunning(taskId)) {
                                writer.flush();
                                saveCheckpoint(taskId, checkpointOffset + totalRows, outputFile);
                                log.info("CSV导出任务被取消，已保存断点: taskId={}, checkpoint={}", 
                                        taskId, checkpointOffset + totalRows);
                                break;
                            }
                            
                            // 跳过已处理的行
                            if (skippedRows < checkpointOffset) {
                                skippedRows++;
                                continue;
                            }
                            
                            StringBuilder line = new StringBuilder();
                            for (int i = 1; i <= columnCount; i++) {
                                if (i > 1) line.append(",");
                                Object value = rs.getObject(i);
                                line.append(escapeCsvValue(value != null ? value.toString() : "", ","));
                            }
                            writer.println(line);
                            
                            totalRows++;
                            
                            // 定期保存断点
                            if (totalRows % 10000 == 0) {
                                writer.flush();
                                int progress = Math.min(95, (int) ((checkpointOffset + totalRows) / 1000));
                                exportTaskMapper.updateCheckpoint(taskId, checkpointOffset + totalRows,
                                        checkpointOffset + totalRows, progress, outputFile.getAbsolutePath());
                            }
                        }
                    }
                }
            }
        }
        
        return checkpointOffset + totalRows;
    }
    
    /**
     * 从断点恢复JSON导出
     */
    private long executeResumeExportToJson(ExportTask task, File outputFile, Long taskId,
                                           Long checkpointOffset, File tempFile) throws Exception {
        DataSource dataSource = task.getDataSourceId() != null ? 
                dataSourceMapper.selectById(task.getDataSourceId()) : getDataSourceForTask(task);
        String sql = task.getExportSql() != null ? task.getExportSql() : buildSqlForTask(task, dataSource);
        
        // 保存SQL和数据源信息
        if (task.getExportSql() == null || task.getDataSourceId() == null) {
            exportTaskMapper.updateExportInfo(taskId, sql, dataSource.getId());
        }
        
        long totalRows = 0;
        long skippedRows = 0;
        boolean appendMode = tempFile != null && tempFile.exists() && checkpointOffset > 0;
        
        // JSON格式比较特殊，需要处理数组的开始和结束
        // 如果有临时文件，需要去掉结尾的 ] 然后继续追加
        if (appendMode) {
            // 读取临时文件，去掉最后的 \n]
            String content = readFileContent(tempFile);
            if (content.endsWith("\n]")) {
                content = content.substring(0, content.length() - 2);
            } else if (content.endsWith("]")) {
                content = content.substring(0, content.length() - 1);
            }
            try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(
                    new FileOutputStream(outputFile), "UTF-8"))) {
                writer.print(content);
            }
        }
        
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(outputFile, appendMode), "UTF-8"))) {
            
            if (!appendMode) {
                writer.println("[");
            }
            
            try (Connection conn = connectionPoolManager.getConnection(dataSource)) {
                conn.setAutoCommit(false);
                
                try (PreparedStatement ps = conn.prepareStatement(sql,
                        ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
                    
                    ps.setQueryTimeout(queryTimeout * 3);
                    ps.setFetchSize(Integer.MIN_VALUE);
                    
                    try (ResultSet rs = ps.executeQuery()) {
                        ResultSetMetaData metaData = rs.getMetaData();
                        int columnCount = metaData.getColumnCount();
                        
                        boolean first = !appendMode || checkpointOffset == 0;
                        while (rs.next()) {
                            // 检查是否被取消
                            if (!isTaskRunning(taskId)) {
                                writer.println();
                                writer.println("]");
                                writer.flush();
                                saveCheckpoint(taskId, checkpointOffset + totalRows, outputFile);
                                log.info("JSON导出任务被取消，已保存断点: taskId={}, checkpoint={}", 
                                        taskId, checkpointOffset + totalRows);
                                break;
                            }
                            
                            // 跳过已处理的行
                            if (skippedRows < checkpointOffset) {
                                skippedRows++;
                                continue;
                            }
                            
                            if (!first) {
                                writer.println(",");
                            }
                            first = false;
                            
                            Map<String, Object> rowData = new LinkedHashMap<>();
                            for (int i = 1; i <= columnCount; i++) {
                                rowData.put(metaData.getColumnName(i), rs.getObject(i));
                            }
                            
                            writer.print(objectMapper.writeValueAsString(rowData));
                            
                            totalRows++;
                            
                            // 定期保存断点
                            if (totalRows % 10000 == 0) {
                                writer.flush();
                                int progress = Math.min(95, (int) ((checkpointOffset + totalRows) / 1000));
                                exportTaskMapper.updateCheckpoint(taskId, checkpointOffset + totalRows,
                                        checkpointOffset + totalRows, progress, outputFile.getAbsolutePath());
                            }
                        }
                    }
                }
            }
            
            writer.println();
            writer.println("]");
        }
        
        return checkpointOffset + totalRows;
    }
    
    /**
     * 保存断点信息
     */
    private void saveCheckpoint(Long taskId, Long checkpointOffset, File tempFile) {
        try {
            String tempFilePath = tempFile != null ? tempFile.getAbsolutePath() : null;
            exportTaskMapper.updateCheckpoint(taskId, checkpointOffset, checkpointOffset, 
                    Math.min(95, (int) (checkpointOffset / 1000)), tempFilePath);
        } catch (Exception e) {
            log.error("保存断点失败: taskId={}", taskId, e);
        }
    }
    
    /**
     * 导出失败时保存已完成的部分数据
     * 需求: 17.6 - 导出任务失败时保存已完成的部分并支持恢复
     * 
     * @param task 导出任务
     * @param outputFile 输出文件（可能为null或不完整）
     * @param fileName 文件名
     * @param baseFileName 基础文件名
     * @param taskId 任务ID
     * @return 是否成功保存了部分数据
     */
    private boolean savePartialExportOnFailure(ExportTask task, File outputFile, String fileName, 
                                                String baseFileName, Long taskId) {
        try {
            // 检查是否有已处理的行数
            Long processedRows = task.getProcessedRows();
            Long checkpointOffset = task.getCheckpointOffset();
            
            // 如果没有处理任何数据，则无需保存部分结果
            if ((processedRows == null || processedRows <= 0) && 
                (checkpointOffset == null || checkpointOffset <= 0)) {
                log.info("导出任务无已处理数据，跳过部分保存: taskId={}", taskId);
                return false;
            }
            
            // 优先使用checkpointOffset作为已处理行数
            long actualProcessedRows = checkpointOffset != null && checkpointOffset > 0 
                    ? checkpointOffset 
                    : (processedRows != null ? processedRows : 0);
            
            // 检查输出文件是否存在且有内容
            if (outputFile != null && outputFile.exists() && outputFile.length() > 0) {
                // 输出文件存在，直接使用
                String partialFileName = fileName != null ? fileName : (baseFileName + "_partial.xlsx");
                
                // 更新任务信息
                task.setFilePath(outputFile.getAbsolutePath());
                task.setFileName(partialFileName.replace(".xlsx", "_partial.xlsx")
                        .replace(".csv", "_partial.csv")
                        .replace(".json", "_partial.json"));
                task.setFileSize(outputFile.length());
                task.setProcessedRows(actualProcessedRows);
                task.setCheckpointOffset(actualProcessedRows);
                
                // 计算进度百分比（基于已处理行数）
                int progress = actualProcessedRows > 0 ? Math.min(95, (int) (actualProcessedRows / 100)) : 0;
                task.setProgress(progress);
                
                log.info("保存部分导出文件: taskId={}, processedRows={}, fileSize={}, filePath={}", 
                        taskId, actualProcessedRows, outputFile.length(), outputFile.getAbsolutePath());
                return true;
            }
            
            // 检查临时文件是否存在
            String tempFilePath = task.getTempFilePath();
            if (tempFilePath != null && !tempFilePath.isEmpty()) {
                File tempFile = new File(tempFilePath);
                if (tempFile.exists() && tempFile.length() > 0) {
                    // 将临时文件作为部分结果保存
                    File exportDir = new File(exportPath);
                    String partialFileName = baseFileName != null 
                            ? baseFileName + "_partial" + getFileExtension(tempFilePath)
                            : "export_partial_" + taskId + getFileExtension(tempFilePath);
                    File partialFile = new File(exportDir, partialFileName);
                    
                    // 复制临时文件到导出目录
                    copyFile(tempFile, partialFile);
                    
                    // 更新任务信息
                    task.setFilePath(partialFile.getAbsolutePath());
                    task.setFileName(partialFileName);
                    task.setFileSize(partialFile.length());
                    task.setProcessedRows(actualProcessedRows);
                    task.setCheckpointOffset(actualProcessedRows);
                    
                    // 计算进度百分比
                    int progress = actualProcessedRows > 0 ? Math.min(95, (int) (actualProcessedRows / 100)) : 0;
                    task.setProgress(progress);
                    
                    log.info("从临时文件保存部分导出: taskId={}, processedRows={}, fileSize={}, filePath={}", 
                            taskId, actualProcessedRows, partialFile.length(), partialFile.getAbsolutePath());
                    return true;
                }
            }
            
            // 如果有已处理的行数但没有文件，仍然记录断点信息以便恢复
            if (actualProcessedRows > 0) {
                task.setProcessedRows(actualProcessedRows);
                task.setCheckpointOffset(actualProcessedRows);
                int progress = Math.min(95, (int) (actualProcessedRows / 100));
                task.setProgress(progress);
                
                log.info("记录部分导出进度（无文件）: taskId={}, processedRows={}", taskId, actualProcessedRows);
                // 返回false因为没有实际的部分文件可下载
                return false;
            }
            
            return false;
        } catch (Exception e) {
            log.error("保存部分导出数据失败: taskId={}", taskId, e);
            return false;
        }
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return ".xlsx";
        }
        int lastDot = filePath.lastIndexOf('.');
        if (lastDot > 0) {
            return filePath.substring(lastDot);
        }
        return ".xlsx";
    }
    
    /**
     * 复制文件
     */
    private void copyFile(File source, File dest) throws IOException {
        try (FileInputStream fis = new FileInputStream(source);
             FileOutputStream fos = new FileOutputStream(dest)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
        }
    }
    
    /**
     * 读取文件内容
     */
    private String readFileContent(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(file), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (content.length() > 0) {
                    content.append("\n");
                }
                content.append(line);
            }
        }
        return content.toString();
    }
    
    @Override
    public boolean cancelExport(Long taskId) {
        ExportTask task = exportTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "任务不存在");
        }
        
        if (task.getStatus() == 2 || task.getStatus() == 4) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "任务已完成或已取消，无法取消");
        }
        
        // 标记任务为取消
        runningTasks.put(taskId, false);
        
        task.setStatus(4); // CANCELLED
        task.setFinishTime(LocalDateTime.now());
        exportTaskMapper.update(task);
        
        log.info("取消导出任务: taskId={}", taskId);
        return true;
    }
    
    @Override
    public boolean pauseExport(Long taskId) {
        ExportTask task = exportTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "任务不存在");
        }
        
        if (task.getStatus() != 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "只有运行中的任务可以暂停");
        }
        
        // 标记任务为暂停（会触发断点保存）
        runningTasks.put(taskId, false);
        
        task.setStatus(5); // PAUSED
        exportTaskMapper.update(task);
        
        log.info("暂停导出任务: taskId={}, checkpointOffset={}", taskId, task.getCheckpointOffset());
        return true;
    }
    
    @Override
    public ExportTask getExportTask(Long taskId) {
        return exportTaskMapper.selectById(taskId);
    }
    
    @Override
    public List<ExportTask> getUserExportTasks(Long userId, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        return exportTaskMapper.selectByUserId(userId, offset, pageSize);
    }
    
    @Override
    public String getDownloadUrl(Long taskId) {
        ExportTask task = exportTaskMapper.selectById(taskId);
        if (task == null || task.getStatus() != 2) {
            return null;
        }
        return "/api/export/download/" + taskId;
    }

    
    // ==================== Private Helper Methods ====================
    
    /**
     * 执行Excel导出
     * 需求: 17.3 - 支持导出任务的断点续传功能
     */
    private long executeExportToExcel(ExportTask task, File outputFile, Long taskId) throws Exception {
        DataSource dataSource = getDataSourceForTask(task);
        String sql = buildSqlForTask(task, dataSource);
        
        // 保存SQL和数据源信息用于后续断点续传
        exportTaskMapper.updateExportInfo(taskId, sql, dataSource.getId());
        
        SXSSFWorkbook workbook = new SXSSFWorkbook(flushRows);
        workbook.setCompressTempFiles(true);
        
        long totalRows = 0;
        
        try (Connection conn = connectionPoolManager.getConnection(dataSource)) {
            conn.setAutoCommit(false);
            
            try (PreparedStatement ps = conn.prepareStatement(sql,
                    ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
                
                ps.setQueryTimeout(queryTimeout * 3);
                ps.setFetchSize(Integer.MIN_VALUE);
                
                try (ResultSet rs = ps.executeQuery()) {
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    
                    // 创建表头样式
                    CellStyle headerStyle = createHeaderStyle(workbook);
                    
                    // 构建表头
                    String[] headers = new String[columnCount];
                    for (int i = 1; i <= columnCount; i++) {
                        headers[i - 1] = metaData.getColumnLabel(i);
                    }
                    
                    // 初始化第一个Sheet
                    int sheetIndex = 1;
                    Sheet currentSheet = workbook.createSheet("数据_" + sheetIndex);
                    createHeaderRow(currentSheet, headers, headerStyle);
                    int rowNumInSheet = 1;
                    
                    while (rs.next()) {
                        // 检查是否被取消
                        if (!isTaskRunning(taskId)) {
                            // 保存断点信息
                            saveCheckpoint(taskId, totalRows, outputFile);
                            log.info("导出任务被取消，已保存断点: taskId={}, checkpoint={}", taskId, totalRows);
                            break;
                        }
                        
                        // 检查是否需要创建新Sheet
                        if (rowNumInSheet >= maxRowsPerSheet) {
                            ((SXSSFSheet) currentSheet).flushRows();
                            sheetIndex++;
                            currentSheet = workbook.createSheet("数据_" + sheetIndex);
                            createHeaderRow(currentSheet, headers, headerStyle);
                            rowNumInSheet = 1;
                        }
                        
                        // 写入数据行
                        Row row = currentSheet.createRow(rowNumInSheet++);
                        for (int i = 1; i <= columnCount; i++) {
                            Cell cell = row.createCell(i - 1);
                            setCellValue(cell, rs.getObject(i));
                        }
                        
                        totalRows++;
                        
                        // 定期刷新和更新进度
                        if (totalRows % 1000 == 0) {
                            ((SXSSFSheet) currentSheet).flushRows();
                        }
                        
                        // 定期保存断点信息
                        if (totalRows % 10000 == 0) {
                            int progress = Math.min(95, (int) (totalRows / 1000));
                            exportTaskMapper.updateCheckpoint(taskId, totalRows, totalRows, progress, null);
                        }
                    }
                }
            }
            
            // 写入文件
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                workbook.write(fos);
            }
            
        } finally {
            workbook.dispose();
            workbook.close();
        }
        
        return totalRows;
    }
    
    /**
     * 执行CSV导出
     * 需求: 17.3 - 支持导出任务的断点续传功能
     */
    private long executeExportToCsv(ExportTask task, File outputFile, Long taskId) throws Exception {
        DataSource dataSource = getDataSourceForTask(task);
        String sql = buildSqlForTask(task, dataSource);
        
        // 保存SQL和数据源信息用于后续断点续传
        exportTaskMapper.updateExportInfo(taskId, sql, dataSource.getId());
        
        long totalRows = 0;
        
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(outputFile), "UTF-8"))) {
            
            try (Connection conn = connectionPoolManager.getConnection(dataSource)) {
                conn.setAutoCommit(false);
                
                try (PreparedStatement ps = conn.prepareStatement(sql,
                        ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
                    
                    ps.setQueryTimeout(queryTimeout * 3);
                    ps.setFetchSize(Integer.MIN_VALUE);
                    
                    try (ResultSet rs = ps.executeQuery()) {
                        ResultSetMetaData metaData = rs.getMetaData();
                        int columnCount = metaData.getColumnCount();
                        
                        // 写入表头
                        StringBuilder header = new StringBuilder();
                        for (int i = 1; i <= columnCount; i++) {
                            if (i > 1) header.append(",");
                            header.append(escapeCsvValue(metaData.getColumnLabel(i), ","));
                        }
                        writer.println(header);
                        
                        while (rs.next()) {
                            // 检查是否被取消
                            if (!isTaskRunning(taskId)) {
                                writer.flush();
                                saveCheckpoint(taskId, totalRows, outputFile);
                                log.info("CSV导出任务被取消，已保存断点: taskId={}, checkpoint={}", taskId, totalRows);
                                break;
                            }
                            
                            StringBuilder line = new StringBuilder();
                            for (int i = 1; i <= columnCount; i++) {
                                if (i > 1) line.append(",");
                                Object value = rs.getObject(i);
                                line.append(escapeCsvValue(value != null ? value.toString() : "", ","));
                            }
                            writer.println(line);
                            
                            totalRows++;
                            
                            // 定期保存断点信息
                            if (totalRows % 10000 == 0) {
                                writer.flush();
                                int progress = Math.min(95, (int) (totalRows / 1000));
                                exportTaskMapper.updateCheckpoint(taskId, totalRows, totalRows, progress, 
                                        outputFile.getAbsolutePath());
                            }
                        }
                    }
                }
            }
        }
        
        return totalRows;
    }
    
    /**
     * 执行JSON导出
     * 需求: 17.3 - 支持导出任务的断点续传功能
     */
    private long executeExportToJson(ExportTask task, File outputFile, Long taskId) throws Exception {
        DataSource dataSource = getDataSourceForTask(task);
        String sql = buildSqlForTask(task, dataSource);
        
        // 保存SQL和数据源信息用于后续断点续传
        exportTaskMapper.updateExportInfo(taskId, sql, dataSource.getId());
        
        long totalRows = 0;
        
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(outputFile), "UTF-8"))) {
            
            writer.println("[");
            
            try (Connection conn = connectionPoolManager.getConnection(dataSource)) {
                conn.setAutoCommit(false);
                
                try (PreparedStatement ps = conn.prepareStatement(sql,
                        ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
                    
                    ps.setQueryTimeout(queryTimeout * 3);
                    ps.setFetchSize(Integer.MIN_VALUE);
                    
                    try (ResultSet rs = ps.executeQuery()) {
                        ResultSetMetaData metaData = rs.getMetaData();
                        int columnCount = metaData.getColumnCount();
                        
                        boolean first = true;
                        while (rs.next()) {
                            // 检查是否被取消
                            if (!isTaskRunning(taskId)) {
                                writer.println();
                                writer.println("]");
                                writer.flush();
                                saveCheckpoint(taskId, totalRows, outputFile);
                                log.info("JSON导出任务被取消，已保存断点: taskId={}, checkpoint={}", taskId, totalRows);
                                break;
                            }
                            
                            if (!first) {
                                writer.println(",");
                            }
                            first = false;
                            
                            Map<String, Object> rowData = new LinkedHashMap<>();
                            for (int i = 1; i <= columnCount; i++) {
                                rowData.put(metaData.getColumnName(i), rs.getObject(i));
                            }
                            
                            writer.print(objectMapper.writeValueAsString(rowData));
                            
                            totalRows++;
                            
                            // 定期保存断点信息
                            if (totalRows % 10000 == 0) {
                                writer.flush();
                                int progress = Math.min(95, (int) (totalRows / 1000));
                                exportTaskMapper.updateCheckpoint(taskId, totalRows, totalRows, progress,
                                        outputFile.getAbsolutePath());
                            }
                        }
                    }
                }
            }
            
            writer.println();
            writer.println("]");
        }
        
        return totalRows;
    }
    
    /**
     * 获取任务关联的数据源
     * 优先级：task.dataSourceId > 关联报表/图表的dataSourceId > 报错
     */
    private DataSource getDataSourceForTask(ExportTask task) {
        Long dataSourceId = null;
        
        // 1. 优先使用任务自身配置的数据源ID
        if (task.getDataSourceId() != null) {
            dataSourceId = task.getDataSourceId();
        }
        
        // 2. 从关联的报表或图表获取数据源ID
        if (dataSourceId == null && task.getRefId() != null) {
            String taskType = task.getTaskType();
            if ("report".equals(taskType)) {
                ReportDefinition report = reportDefinitionMapper.selectById(task.getRefId());
                if (report != null) {
                    dataSourceId = report.getDataSourceId();
                }
            } else if ("chart".equals(taskType)) {
                ChartDefinition chart = chartDefinitionMapper.selectById(task.getRefId());
                if (chart != null) {
                    dataSourceId = chart.getDataSourceId();
                }
            }
        }
        
        if (dataSourceId == null) {
            throw new BusinessException(ErrorCode.DATA_SOURCE_NOT_FOUND, "导出任务未关联数据源，无法执行导出");
        }
        
        DataSource ds = dataSourceMapper.selectById(dataSourceId);
        if (ds == null) {
            throw new BusinessException(ErrorCode.DATA_SOURCE_NOT_FOUND, "数据源不存在: id=" + dataSourceId);
        }
        return ds;
    }
    
    /**
     * 构建任务的SQL
     * 优先级：task.exportSql > 关联报表的sqlContent > 关联图表的sqlContent > 报错
     */
    private String buildSqlForTask(ExportTask task, DataSource dataSource) {
        // 1. 优先使用任务自身保存的SQL（断点续传场景）
        if (task.getExportSql() != null && !task.getExportSql().isBlank()) {
            return task.getExportSql();
        }
        
        // 2. 从关联的报表或图表获取SQL
        if (task.getRefId() != null) {
            String taskType = task.getTaskType();
            if ("report".equals(taskType)) {
                ReportDefinition report = reportDefinitionMapper.selectById(task.getRefId());
                if (report != null && report.getSqlContent() != null && !report.getSqlContent().isBlank()) {
                    log.info("从报表定义获取SQL: reportId={}, reportName={}", report.getId(), report.getReportName());
                    return report.getSqlContent();
                }
            } else if ("chart".equals(taskType)) {
                ChartDefinition chart = chartDefinitionMapper.selectById(task.getRefId());
                if (chart != null && chart.getSqlContent() != null && !chart.getSqlContent().isBlank()) {
                    log.info("从图表定义获取SQL: chartId={}, chartName={}", chart.getId(), chart.getChartName());
                    return chart.getSqlContent();
                }
            }
        }
        
        throw new BusinessException(ErrorCode.PARAM_ERROR, "导出任务无可用SQL，请检查任务配置或关联的报表/图表");
    }
    
    /**
     * 检查任务是否仍在运行
     */
    private boolean isTaskRunning(Long taskId) {
        Boolean running = runningTasks.get(taskId);
        return running != null && running;
    }
    
    /**
     * 计算已处理行数
     */
    private Long calculateProcessedRows(ExportTask task) {
        if (task.getTotalRows() != null && task.getProgress() != null) {
            return task.getTotalRows() * task.getProgress() / 100;
        }
        return 0L;
    }
    
    /**
     * 创建表头样式
     */
    private CellStyle createHeaderStyle(SXSSFWorkbook workbook) {
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return headerStyle;
    }
    
    /**
     * 创建表头行
     */
    private void createHeaderRow(Sheet sheet, String[] headers, CellStyle headerStyle) {
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }
    
    /**
     * 设置单元格值
     */
    private void setCellValue(Cell cell, Object value) {
        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else if (value instanceof java.util.Date) {
            cell.setCellValue((java.util.Date) value);
        } else if (value instanceof java.sql.Date) {
            cell.setCellValue(((java.sql.Date) value).toLocalDate().toString());
        } else if (value instanceof java.sql.Timestamp) {
            cell.setCellValue(((java.sql.Timestamp) value).toLocalDateTime().toString());
        } else {
            String strValue = value.toString();
            if (strValue.length() > 32767) {
                strValue = strValue.substring(0, 32760) + "...[截断]";
            }
            cell.setCellValue(strValue);
        }
    }
    
    /**
     * CSV值转义
     */
    private String escapeCsvValue(String value, String separator) {
        if (value == null) {
            return "";
        }
        boolean needQuote = value.contains(separator) || value.contains("\"") || 
                           value.contains("\n") || value.contains("\r");
        if (needQuote) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
    
    /**
     * 压缩文件到ZIP
     */
    private void compressToZip(File sourceFile, File zipFile) throws IOException {
        byte[] buffer = new byte[8192];
        
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            zos.setLevel(6);
            ZipEntry entry = new ZipEntry(sourceFile.getName());
            zos.putNextEntry(entry);
            
            try (FileInputStream fis = new FileInputStream(sourceFile)) {
                int len;
                while ((len = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, len);
                }
            }
            zos.closeEntry();
        }
    }
}
