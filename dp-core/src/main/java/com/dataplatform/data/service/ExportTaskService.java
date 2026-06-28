package com.dataplatform.data.service;

import com.dataplatform.common.PageResult;
import com.dataplatform.common.exception.BusinessException;
import com.dataplatform.common.exception.ErrorCode;
import com.dataplatform.data.entity.DataSource;
import com.dataplatform.data.entity.ExportTask;
import com.dataplatform.data.entity.ReportDefinition;
import com.dataplatform.data.entity.ReportField;
import com.dataplatform.data.mapper.DataSourceMapper;
import com.dataplatform.data.mapper.ExportTaskMapper;
import com.dataplatform.data.mapper.ReportDefinitionMapper;
import com.dataplatform.data.mapper.ReportFieldMapper;
import com.dataplatform.data.service.DbConnectionUtil;
import com.dataplatform.common.util.SqlParamUtil;
import com.dataplatform.common.util.SqlSecurityUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 导出任务服务
 */
@Slf4j
@Service
public class ExportTaskService {
    
    @Value("${export.excel.max-rows-per-sheet:1000000}")
    private int maxRowsPerSheet;
    
    @Value("${export.excel.flush-rows:500}")
    private int flushRows;
    
    @Value("${export.file.path:../runtime/exports}")
    private String exportPath;
    
    // 导出策略阈值（从配置文件读取）
    @Value("${export.strategy.zip.threshold:2000000}")
    private long zipThreshold;
    
    @Value("${export.strategy.split.threshold:10000000}")
    private long splitThreshold;
    
    @Value("${export.strategy.rows.per.file:2000000}")
    private long rowsPerFile;
    
    @Value("${export.file.expire.days:7}")
    private int expireDays;
    
    @Value("${db.query.timeout:300}")
    private int queryTimeout;
    
    @Autowired
    private ExportTaskMapper exportTaskMapper;
    
    @Autowired
    private ReportDefinitionMapper reportDefinitionMapper;
    
    @Autowired
    private ReportFieldMapper reportFieldMapper;
    
    @Autowired
    private DataSourceMapper dataSourceMapper;
    
    @Autowired
    private DataSourceService dataSourceService;
    
    @Autowired
    private DbConnectionUtil dbConnectionUtil;
    
    @Autowired
    private com.dataplatform.data.service.DataSourceConnectionPoolManager connectionPoolManager;
    
    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Autowired
    private LicenseLimitService licenseLimitService;
    
    @Autowired(required = false)
    private NotificationChannelRouter notificationChannelRouter;
    
    /**
     * 创建导出任务
     */
    public ExportTask createExportTask(String taskName, String taskType, Long refId, 
                                        String refCode, String filters, String params, Long userId) {
        ExportTask task = new ExportTask();
        task.setTaskName(taskName);
        task.setTaskType(taskType);
        task.setRefId(refId);
        task.setRefCode(refCode);
        task.setFilters(filters);
        task.setParams(params);
        task.setStatus(0);  // 等待中
        task.setProgress(0);
        task.setCreateBy(userId);
        task.setCreateTime(LocalDateTime.now());
        task.setExpireTime(LocalDateTime.now().plusDays(expireDays));
        
        exportTaskMapper.insert(task);
        return task;
    }
    
    /**
     * 获取用户的导出任务列表
     */
    public PageResult<ExportTask> getTaskList(Long userId, int page, int pageSize) {
        return getTaskList(userId, page, pageSize, null, null, null);
    }
    
    /**
     * 获取用户的导出任务列表（带查询条件）
     */
    public PageResult<ExportTask> getTaskList(Long userId, int page, int pageSize, 
                                               String taskName, String startDate, String endDate) {
        int offset = (page - 1) * pageSize;
        List<ExportTask> list = exportTaskMapper.selectByUserIdWithConditions(userId, offset, pageSize, taskName, startDate, endDate);
        long total = exportTaskMapper.countByUserIdWithConditions(userId, taskName, startDate, endDate);
        return new PageResult<>(list, total);
    }
    
    /**
     * 获取用户进行中的任务
     */
    public List<ExportTask> getPendingTasks(Long userId) {
        return exportTaskMapper.selectPendingByUserId(userId);
    }
    
    /**
     * 获取任务详情
     */
    public ExportTask getTaskById(Long taskId) {
        return exportTaskMapper.selectById(taskId);
    }
    
    /**
     * 删除任务
     */
    public boolean deleteTask(Long taskId, Long userId) {
        ExportTask task = exportTaskMapper.selectById(taskId);
        if (task != null && task.getFilePath() != null) {
            // 删除文件
            File file = new File(task.getFilePath());
            if (file.exists()) {
                file.delete();
            }
        }
        return exportTaskMapper.deleteByIdAndUser(taskId, userId) > 0;
    }
    
    /**
     * 获取导出文件
     */
    public File getExportFile(Long taskId, Long userId) {
        ExportTask task = exportTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "任务不存在");
        }
        if (!task.getCreateBy().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问此文件");
        }
        if (task.getStatus() != 2) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "任务未完成");
        }
        if (task.getFilePath() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件不存在");
        }
        
        File file = new File(task.getFilePath());
        if (!file.exists()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件已过期或被删除");
        }
        return file;
    }
    
    /**
     * 异步执行报表导出
     */
    @Async("taskExecutor")
    public void executeReportExportAsync(Long taskId) {
        log.info("异步导出任务开始执行: taskId={}, thread={}", taskId, Thread.currentThread().getName());
        ExportTask task = null;
        try {
            task = exportTaskMapper.selectById(taskId);
            if (task == null) {
                log.error("导出任务不存在: {}", taskId);
                return;
            }
            
            // 更新状态为处理中
            task.setStatus(1);
            task.setStartTime(LocalDateTime.now());
            exportTaskMapper.update(task);
        } catch (Exception e) {
            log.error("导出任务初始化失败: taskId={}", taskId, e);
            // 尝试将任务标记为失败
            try {
                exportTaskMapper.updateProgress(taskId, 3, 0);
            } catch (Exception ex) {
                log.error("更新任务失败状态也失败了: taskId={}", taskId, ex);
            }
            return;
        }
        
        try {
            // 获取报表定义
            ReportDefinition report = reportDefinitionMapper.selectById(task.getRefId());
            if (report == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "报表定义不存在");
            }
            
            DataSource dataSource = dataSourceMapper.selectById(report.getDataSourceId());
            if (dataSource == null) {
                throw new BusinessException(ErrorCode.DATA_SOURCE_NOT_FOUND, "数据源不存在");
            }
            
            // 创建导出目录
            File exportDir = new File(exportPath);
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }
            
            String baseFileName = task.getTaskName() + "_" + UUID.randomUUID().toString().substring(0, 8);
            
            String customParams = task.getParams();
            
            // 先查询总数确定导出策略
            long estimatedRows = countTotalRows(report, dataSource, task.getFilters(), customParams);
            log.info("预估行数: {}, taskId={}", estimatedRows, taskId);
            licenseLimitService.assertReportExportRowsAllowed(estimatedRows);
            
            File outputFile;
            String fileName;
            String dataType;
            long totalRows;
            
            if (estimatedRows >= splitThreshold) {
                // 1000万以上：分多个Excel文件，打包成ZIP
                log.info("大数据量导出（分片模式）: taskId={}, estimatedRows={}", taskId, estimatedRows);
                List<File> excelFiles = exportToMultipleFiles(report, dataSource, task.getFilters(), customParams, exportDir, baseFileName, taskId);
                fileName = baseFileName + ".zip";
                outputFile = new File(exportDir, fileName);
                totalRows = compressToZip(excelFiles, outputFile);
                dataType = "zip";
                // 删除临时Excel文件
                for (File f : excelFiles) {
                    f.delete();
                }
            } else if (estimatedRows >= zipThreshold) {
                // 200万-1000万：单个Excel压缩成ZIP
                log.info("中等数据量导出（压缩模式）: taskId={}, estimatedRows={}", taskId, estimatedRows);
                String tempFileName = baseFileName + ".xlsx";
                File tempFile = new File(exportDir, tempFileName);
                totalRows = exportToFile(report, dataSource, task.getFilters(), customParams, tempFile, taskId);
                fileName = baseFileName + ".zip";
                outputFile = new File(exportDir, fileName);
                compressToZip(List.of(tempFile), outputFile);
                dataType = "zip";
                tempFile.delete();
            } else {
                // 200万以下：直接生成Excel
                log.info("小数据量导出（直接模式）: taskId={}, estimatedRows={}", taskId, estimatedRows);
                fileName = baseFileName + ".xlsx";
                outputFile = new File(exportDir, fileName);
                totalRows = exportToFile(report, dataSource, task.getFilters(), customParams, outputFile, taskId);
                dataType = "xlsx";
            }
            
            // 更新任务状态
            task.setStatus(2);  // 已完成
            task.setProgress(100);
            task.setFilePath(outputFile.getAbsolutePath());
            task.setFileName(fileName);
            task.setFileSize(outputFile.length());
            task.setTotalRows(totalRows);
            task.setDataType(dataType);
            task.setFinishTime(LocalDateTime.now());
            exportTaskMapper.update(task);
            
            log.info("导出任务完成: taskId={}, totalRows={}, fileSize={}, dataType={}", 
                     taskId, totalRows, outputFile.length(), dataType);
            
            // 插入导出完成通知
            insertExportNotification(task);
            
        } catch (Exception e) {
            log.error("导出任务失败: taskId={}", taskId, e);
            task.setStatus(3);  // 失败
            task.setErrorMsg(e.getMessage());
            task.setFinishTime(LocalDateTime.now());
            exportTaskMapper.update(task);
        }
    }
    
    /**
     * 插入导出完成通知（站内 + 多渠道）
     */
    private void insertExportNotification(ExportTask task) {
        String title = "导出完成";
        String content = "您的导出任务\"" + task.getTaskName() + "\"已完成，共" + task.getTotalRows() + "行数据，可前往导出中心下载。";
        // 站内通知（原有逻辑保留）
        try {
            String sql = "INSERT INTO sys_notification (user_id, title, content, type, is_read, create_time) " +
                    "VALUES (?, ?, ?, ?, 0, NOW())";
            jdbcTemplate.update(sql, task.getCreateBy(), title, content, "export");
        } catch (Exception e) {
            log.warn("插入导出完成通知失败: {}", e.getMessage());
        }
        // 多渠道通知（通过统一路由）
        if (notificationChannelRouter != null) {
            try {
                String recipient = task.getCreateBy() != null ? String.valueOf(task.getCreateBy()) : null;
                notificationChannelRouter.route("email,wecom,dingtalk", "export", title, content, recipient, null);
            } catch (Exception e) {
                log.warn("导出完成多渠道通知失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 查询总行数
     */
    private long countTotalRows(ReportDefinition report, DataSource dataSource, String filters, String customParams) throws Exception {
        String url = dataSourceService.buildJdbcUrl(dataSource);
        String driver = dbConnectionUtil.getDriverClassName(dataSource.getDbType());
        
        String sql = report.getSqlContent();
        
        // 处理自定义参数 ${}
        List<Object> customParamValues = new ArrayList<>();
        if (customParams != null && !customParams.isBlank()) {
            Map<String, String> paramMap = SqlParamUtil.parseParamsJson(customParams);
            Object[] paramResult = SqlParamUtil.replaceCustomParams(sql, paramMap);
            sql = (String) paramResult[0];
            @SuppressWarnings("unchecked")
            List<Object> cpv = (List<Object>) paramResult[1];
            customParamValues = cpv;
        } else {
            sql = SqlParamUtil.stripCustomParamsForTest(sql);
        }
        
        List<com.dataplatform.common.dto.FilterCondition> filterList = 
            com.dataplatform.common.util.FilterUtil.parseFilters(filters);
        com.dataplatform.common.util.FilterUtil.FilterWhereClause whereClause = 
            com.dataplatform.common.util.FilterUtil.buildDynamicWhereClause(filterList);
        
        String filteredSql = insertFilterConditions(sql, whereClause.getWhereClause());
        String countSql = "SELECT COUNT(*) FROM (" + filteredSql + ") t_count";
        
        Class.forName(driver);
        try (Connection conn = connectionPoolManager.getConnection(dataSource);
             PreparedStatement ps = conn.prepareStatement(countSql)) {
            ps.setQueryTimeout(queryTimeout);
            int paramIndex = 1;
            for (Object val : customParamValues) {
                ps.setObject(paramIndex++, val);
            }
            List<Object> parameters = whereClause.getParameters();
            for (int i = 0; i < parameters.size(); i++) {
                ps.setObject(paramIndex++, parameters.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return 0;
    }
    
    /**
     * 导出到多个Excel文件（1000万以上数据）
     */
    private List<File> exportToMultipleFiles(ReportDefinition report, DataSource dataSource, 
                                              String filters, String customParams, File exportDir, String baseFileName, 
                                              Long taskId) throws Exception {
        List<File> files = new ArrayList<>();
        String url = dataSourceService.buildJdbcUrl(dataSource);
        String driver = dbConnectionUtil.getDriverClassName(dataSource.getDbType());
        
        String sql = report.getSqlContent();
        SqlSecurityUtil.validateSql(SqlParamUtil.stripCustomParamsForTest(sql));
        
        // 处理自定义参数 ${}
        List<Object> customParamValues = new ArrayList<>();
        if (customParams != null && !customParams.isBlank()) {
            Map<String, String> paramMap = SqlParamUtil.parseParamsJson(customParams);
            Object[] paramResult = SqlParamUtil.replaceCustomParams(sql, paramMap);
            sql = (String) paramResult[0];
            @SuppressWarnings("unchecked")
            List<Object> cpv = (List<Object>) paramResult[1];
            customParamValues = cpv;
        } else {
            sql = SqlParamUtil.stripCustomParamsForTest(sql);
        }
        
        Class.forName(driver);
        try (Connection conn = connectionPoolManager.getConnection(dataSource)) {
            List<com.dataplatform.common.dto.FilterCondition> filterList = 
                com.dataplatform.common.util.FilterUtil.parseFilters(filters);
            com.dataplatform.common.util.FilterUtil.FilterWhereClause whereClause = 
                com.dataplatform.common.util.FilterUtil.buildDynamicWhereClause(filterList);
            
            String filteredSql = insertFilterConditions(sql, whereClause.getWhereClause());
            
            conn.setAutoCommit(false);
            
            try (PreparedStatement ps = conn.prepareStatement(filteredSql,
                    ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
                ps.setQueryTimeout(queryTimeout * 3);
                ps.setFetchSize(Integer.MIN_VALUE);
                
                int paramIndex = 1;
                for (Object val : customParamValues) {
                    ps.setObject(paramIndex++, val);
                }
                List<Object> parameters = whereClause.getParameters();
                for (int i = 0; i < parameters.size(); i++) {
                    ps.setObject(paramIndex++, parameters.get(i));
                }
                
                log.info("开始分片导出，taskId={}", taskId);
                
                try (ResultSet rs = ps.executeQuery()) {
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    
                    // 获取字段配置
                    List<ReportField> fields = reportFieldMapper.selectByReportId(report.getId());
                    Map<String, String> fieldLabelMap = new HashMap<>();
                    for (ReportField field : fields) {
                        if (StringUtils.hasText(field.getFieldLabel())) {
                            fieldLabelMap.put(field.getFieldName(), field.getFieldLabel());
                        }
                    }
                    
                    // 构建表头
                    String[] headerLabels = buildHeaderLabels(metaData, columnCount, fieldLabelMap);
                    
                    int fileIndex = 1;
                    long totalRows = 0;
                    long rowsInCurrentFile = 0;
                    SXSSFWorkbook currentWorkbook = null;
                    Sheet currentSheet = null;
                    CellStyle headerStyle = null;
                    int sheetIndex = 1;
                    int rowNumInSheet = 0;
                    
                    while (rs.next()) {
                        // 需要创建新文件
                        if (currentWorkbook == null || rowsInCurrentFile >= rowsPerFile) {
                            // 保存当前文件
                            if (currentWorkbook != null) {
                                String fileName = baseFileName + "_" + fileIndex + ".xlsx";
                                File outputFile = new File(exportDir, fileName);
                                try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                                    currentWorkbook.write(fos);
                                }
                                currentWorkbook.dispose();
                                currentWorkbook.close();
                                files.add(outputFile);
                                fileIndex++;
                                log.info("完成分片文件: {}, rows={}", fileName, rowsInCurrentFile);
                            }
                            
                            // 创建新文件
                            currentWorkbook = new SXSSFWorkbook(flushRows);
                            currentWorkbook.setCompressTempFiles(true);
                            headerStyle = createHeaderStyle(currentWorkbook);
                            sheetIndex = 1;
                            currentSheet = currentWorkbook.createSheet("数据_" + sheetIndex);
                            createHeaderRow(currentSheet, headerLabels, headerStyle);
                            rowNumInSheet = 1;
                            rowsInCurrentFile = 0;
                        }
                        
                        // Sheet已满，创建新Sheet
                        if (rowNumInSheet >= maxRowsPerSheet) {
                            ((SXSSFSheet) currentSheet).flushRows();
                            sheetIndex++;
                            currentSheet = currentWorkbook.createSheet("数据_" + sheetIndex);
                            createHeaderRow(currentSheet, headerLabels, headerStyle);
                            rowNumInSheet = 1;
                        }
                        
                        // 写入数据行
                        Row row = currentSheet.createRow(rowNumInSheet++);
                        writeDataRow(row, rs, columnCount);
                        
                        totalRows++;
                        licenseLimitService.assertReportExportRowsAllowed(totalRows);
                        rowsInCurrentFile++;
                        
                        // 每1000行刷新
                        if (totalRows % 1000 == 0) {
                            ((SXSSFSheet) currentSheet).flushRows();
                        }
                        
                        // 更新进度
                        if (totalRows % 50000 == 0) {
                            int progress = (int) Math.min(90, totalRows / 100000);
                            exportTaskMapper.updateProgress(taskId, 1, progress);
                        }
                    }
                    
                    // 保存最后一个文件
                    if (currentWorkbook != null && rowsInCurrentFile > 0) {
                        String fileName = baseFileName + "_" + fileIndex + ".xlsx";
                        File outputFile = new File(exportDir, fileName);
                        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                            currentWorkbook.write(fos);
                        }
                        currentWorkbook.dispose();
                        currentWorkbook.close();
                        files.add(outputFile);
                        log.info("完成分片文件: {}, rows={}", fileName, rowsInCurrentFile);
                    }
                }
            }
        }
        
        return files;
    }
    
    /**
     * 压缩文件到ZIP
     */
    private long compressToZip(List<File> files, File zipFile) throws Exception {
        long totalRows = 0;
        byte[] buffer = new byte[8192];
        
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            zos.setLevel(6);  // 压缩级别
            for (File file : files) {
                ZipEntry entry = new ZipEntry(file.getName());
                zos.putNextEntry(entry);
                try (FileInputStream fis = new FileInputStream(file)) {
                    int len;
                    while ((len = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, len);
                    }
                }
                zos.closeEntry();
            }
        }
        
        log.info("压缩完成: {}, 包含{}个文件, 压缩后大小: {}", 
                 zipFile.getName(), files.size(), zipFile.length());
        return totalRows;
    }
    
    /**
     * 构建表头标签
     */
    private String[] buildHeaderLabels(ResultSetMetaData metaData, int columnCount, 
                                        Map<String, String> fieldLabelMap) throws SQLException {
        String[] headerLabels = new String[columnCount];
        for (int i = 1; i <= columnCount; i++) {
            String columnName = metaData.getColumnName(i);
            String headerLabel = fieldLabelMap.getOrDefault(columnName, null);
            if (!StringUtils.hasText(headerLabel)) {
                String columnLabel = metaData.getColumnLabel(i);
                headerLabel = StringUtils.hasText(columnLabel) && !columnLabel.equals(columnName)
                    ? columnLabel : columnName;
            }
            headerLabels[i - 1] = headerLabel;
        }
        return headerLabels;
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
     * 写入数据行
     */
    private void writeDataRow(Row row, ResultSet rs, int columnCount) throws SQLException {
        for (int i = 1; i <= columnCount; i++) {
            Cell cell = row.createCell(i - 1);
            Object value = rs.getObject(i);
            if (value != null) {
                String strValue = value.toString();
                if (strValue.length() > 32767) {
                    strValue = strValue.substring(0, 32760) + "...[截断]";
                }
                cell.setCellValue(strValue);
            }
        }
    }
    
    /**
     * 执行导出到文件
     */
    private long exportToFile(ReportDefinition report, DataSource dataSource, 
                               String filters, String customParams, File outputFile, Long taskId) throws Exception {
        String url = dataSourceService.buildJdbcUrl(dataSource);
        String driver = dbConnectionUtil.getDriverClassName(dataSource.getDbType());
        
        String sql = report.getSqlContent();
        SqlSecurityUtil.validateSql(SqlParamUtil.stripCustomParamsForTest(sql));
        
        // 处理自定义参数 ${}
        List<Object> customParamValues = new ArrayList<>();
        if (customParams != null && !customParams.isBlank()) {
            Map<String, String> paramMap = SqlParamUtil.parseParamsJson(customParams);
            Object[] paramResult = SqlParamUtil.replaceCustomParams(sql, paramMap);
            sql = (String) paramResult[0];
            @SuppressWarnings("unchecked")
            List<Object> cpv = (List<Object>) paramResult[1];
            customParamValues = cpv;
        } else {
            sql = SqlParamUtil.stripCustomParamsForTest(sql);
        }
        
        SXSSFWorkbook workbook = new SXSSFWorkbook(flushRows);
        workbook.setCompressTempFiles(true);
        
        long totalRows = 0;
        
        try {
            Class.forName(driver);
            try (Connection conn = connectionPoolManager.getConnection(dataSource)) {
                // 解析筛选条件
                List<com.dataplatform.common.dto.FilterCondition> filterList = 
                    com.dataplatform.common.util.FilterUtil.parseFilters(filters);
                com.dataplatform.common.util.FilterUtil.FilterWhereClause whereClause = 
                    com.dataplatform.common.util.FilterUtil.buildDynamicWhereClause(filterList);
                
                String filteredSql = insertFilterConditions(sql, whereClause.getWhereClause());
                
                conn.setAutoCommit(false);
                
                try (PreparedStatement ps = conn.prepareStatement(filteredSql,
                        ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
                    ps.setQueryTimeout(queryTimeout * 3);
                    ps.setFetchSize(Integer.MIN_VALUE);
                    
                    int paramIndex = 1;
                    for (Object val : customParamValues) {
                        ps.setObject(paramIndex++, val);
                    }
                    List<Object> parameters = whereClause.getParameters();
                    for (int i = 0; i < parameters.size(); i++) {
                        ps.setObject(paramIndex++, parameters.get(i));
                    }
                    
                    log.info("开始异步导出，taskId={}, SQL: {}", taskId, filteredSql);
                    
                    try (ResultSet rs = ps.executeQuery()) {
                        ResultSetMetaData metaData = rs.getMetaData();
                        int columnCount = metaData.getColumnCount();
                        
                        // 获取字段配置
                        List<ReportField> fields = reportFieldMapper.selectByReportId(report.getId());
                        Map<String, String> fieldLabelMap = new HashMap<>();
                        for (ReportField field : fields) {
                            if (StringUtils.hasText(field.getFieldLabel())) {
                                fieldLabelMap.put(field.getFieldName(), field.getFieldLabel());
                            }
                        }
                        
                        // 构建表头
                        String[] headerLabels = new String[columnCount];
                        for (int i = 1; i <= columnCount; i++) {
                            String columnName = metaData.getColumnName(i);
                            String headerLabel = fieldLabelMap.getOrDefault(columnName, null);
                            if (!StringUtils.hasText(headerLabel)) {
                                String columnLabel = metaData.getColumnLabel(i);
                                headerLabel = StringUtils.hasText(columnLabel) && !columnLabel.equals(columnName)
                                    ? columnLabel : columnName;
                            }
                            headerLabels[i - 1] = headerLabel;
                        }
                        
                        // 创建样式
                        CellStyle headerStyle = workbook.createCellStyle();
                        Font headerFont = workbook.createFont();
                        headerFont.setBold(true);
                        headerStyle.setFont(headerFont);
                        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
                        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                        
                        // 初始化第一个Sheet
                        int sheetIndex = 1;
                        Sheet currentSheet = workbook.createSheet("数据_" + sheetIndex);
                        createHeaderRow(currentSheet, headerLabels, headerStyle);
                        
                        int rowNumInSheet = 1;
                        int lastProgress = 0;
                        
                        while (rs.next()) {
                            if (rowNumInSheet >= maxRowsPerSheet) {
                                ((SXSSFSheet) currentSheet).flushRows();
                                sheetIndex++;
                                currentSheet = workbook.createSheet("数据_" + sheetIndex);
                                createHeaderRow(currentSheet, headerLabels, headerStyle);
                                rowNumInSheet = 1;
                            }
                            
                            Row row = currentSheet.createRow(rowNumInSheet++);
                            for (int i = 1; i <= columnCount; i++) {
                                Cell cell = row.createCell(i - 1);
                                Object value = rs.getObject(i);
                                if (value != null) {
                                    String strValue = value.toString();
                                    if (strValue.length() > 32767) {
                                        strValue = strValue.substring(0, 32760) + "...[截断]";
                                    }
                                    cell.setCellValue(strValue);
                                }
                            }
                            
                            totalRows++;
                            licenseLimitService.assertReportExportRowsAllowed(totalRows);
                            
                            // 每1000行刷新
                            if (totalRows % 1000 == 0) {
                                ((SXSSFSheet) currentSheet).flushRows();
                            }
                            
                            // 每10000行更新一次进度（假设最多1000万行）
                            if (totalRows % 10000 == 0) {
                                int progress = (int) Math.min(95, totalRows / 100000);
                                if (progress > lastProgress) {
                                    exportTaskMapper.updateProgress(taskId, 1, progress);
                                    lastProgress = progress;
                                }
                            }
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
    
    private void createHeaderRow(Sheet sheet, String[] headerLabels, CellStyle headerStyle) {
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headerLabels.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headerLabels[i]);
            cell.setCellStyle(headerStyle);
        }
    }
    
    private String insertFilterConditions(String sql, String whereClause) {
        if (whereClause == null || whereClause.isEmpty()) {
            return sql;
        }
        
        String upperSql = sql.toUpperCase().trim();
        int insertPosition = sql.length();
        
        int orderByPos = upperSql.lastIndexOf("ORDER BY");
        if (orderByPos > 0) {
            insertPosition = orderByPos;
        }
        
        if (insertPosition == sql.length()) {
            int limitPos = upperSql.lastIndexOf("LIMIT");
            if (limitPos > 0) {
                insertPosition = limitPos;
            }
        }
        
        String beforeInsert = sql.substring(0, insertPosition).trim();
        String afterInsert = insertPosition < sql.length() ? " " + sql.substring(insertPosition).trim() : "";
        
        if (upperSql.contains(" WHERE ")) {
            String andClause = whereClause.substring(7);
            return beforeInsert + " AND " + andClause + afterInsert;
        } else {
            return beforeInsert + whereClause + afterInsert;
        }
    }
    
    /**
     * 清理过期任务
     */
    public int cleanExpiredTasks() {
        // 先删除过期文件
        List<ExportTask> expiredTasks = exportTaskMapper.selectByUserId(0L, 0, 1000);
        for (ExportTask task : expiredTasks) {
            if (task.getExpireTime() != null && task.getExpireTime().isBefore(LocalDateTime.now())) {
                if (task.getFilePath() != null) {
                    File file = new File(task.getFilePath());
                    if (file.exists()) {
                        file.delete();
                    }
                }
            }
        }
        return exportTaskMapper.deleteExpired();
    }
    
    /**
     * 恢复卡住的任务：将超过30分钟仍在"等待中"或"处理中"的任务标记为失败
     */
    public int recoverStuckTasks() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(30);
        List<ExportTask> stuckTasks = exportTaskMapper.selectStuckTasks(threshold);
        int recovered = 0;
        for (ExportTask task : stuckTasks) {
            log.warn("发现卡住的导出任务: taskId={}, status={}, createTime={}", 
                     task.getId(), task.getStatus(), task.getCreateTime());
            task.setStatus(3);
            task.setErrorMsg("任务超时未完成，已自动标记为失败。请重新创建导出任务。");
            task.setFinishTime(LocalDateTime.now());
            exportTaskMapper.update(task);
            recovered++;
        }
        if (recovered > 0) {
            log.info("已恢复 {} 个卡住的导出任务", recovered);
        }
        return recovered;
    }
}
