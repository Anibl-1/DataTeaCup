package com.dataplatform.data.service;

import com.dataplatform.common.exception.BusinessException;
import com.dataplatform.common.exception.ErrorCode;
import com.dataplatform.data.entity.DataSource;
import com.dataplatform.data.mapper.DataSourceMapper;
import com.dataplatform.data.service.DbConnectionUtil;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 报表服务类
 * 处理报表查询和导出功能
 *
 * @author dataplatform
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {
    
    @org.springframework.beans.factory.annotation.Value("${export.excel.max-rows-per-sheet:1000000}")
    private int maxRowsPerSheet;
    
    @org.springframework.beans.factory.annotation.Value("${query.export.max-rows:10000000}")
    private int maxExportRows;
    
    @org.springframework.beans.factory.annotation.Value("${query.report.max-rows:100000}")
    private int maxQueryRows;
    
    @org.springframework.beans.factory.annotation.Value("${db.query.timeout:300}")
    private int queryTimeout;
    
    @org.springframework.beans.factory.annotation.Value("${db.export.timeout:600}")
    private int exportTimeout;
    
    private final DataSourceMapper dataSourceMapper;
    private final DataSourceService dataSourceService;
    private final DbConnectionUtil dbConnectionUtil;
    private final com.dataplatform.data.service.DataSourceConnectionPoolManager connectionPoolManager;
    private final LicenseLimitService licenseLimitService;

    /**
     * 查询报表数据
     *
     * @param dataSourceId 数据源ID
     * @param tableName 表名
     * @param page 页码（从1开始）
     * @param pageSize 每页大小
     * @return 数据列表
     * @throws BusinessException 查询失败时抛出
     */
    public List<Map<String, Object>> queryReport(Long dataSourceId, String tableName, Integer page, Integer pageSize, String filters) {
        if (dataSourceId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "数据源ID不能为空");
        }
        if (!StringUtils.hasText(tableName)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "表名不能为空");
        }
        if (page == null || page < 1) {
            page = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }
        // 限制单次查询最大行数，防止内存溢出
        if (pageSize > maxQueryRows) {
            pageSize = maxQueryRows;
            log.warn("pageSize超过最大限制，已调整为: {}", maxQueryRows);
        }
        
        DataSource dataSource = dataSourceMapper.selectById(dataSourceId);
        if (dataSource == null) {
            throw new BusinessException(ErrorCode.DATA_SOURCE_NOT_FOUND, "数据源不存在");
        }
        
        // 验证数据源配置
        String database = dataSource.getDatabase();
        if (!StringUtils.hasText(database)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, 
                String.format("数据源的数据库名不能为空，请先完善数据源配置。数据源ID: %d", dataSourceId));
        }
        if (!StringUtils.hasText(dataSource.getHost())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "数据源的主机地址不能为空");
        }
        if (dataSource.getPort() == null || dataSource.getPort() < 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "数据源的端口配置无效");
        }
        
        String url = dataSourceService.buildJdbcUrl(dataSource);
        String driver = dbConnectionUtil.getDriverClassName(dataSource.getDbType());
        
        // 验证表名安全性
        String safeTableName = tableName.trim().replaceAll("[^a-zA-Z0-9_]", "");
        if (safeTableName.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "表名格式不正确，只能包含字母、数字和下划线");
        }

        // 解析筛选条件
        List<com.dataplatform.common.dto.FilterCondition> filterList = com.dataplatform.common.util.FilterUtil.parseFilters(filters);
        com.dataplatform.common.util.FilterUtil.FilterWhereClause whereClause = com.dataplatform.common.util.FilterUtil.buildDynamicWhereClause(filterList);

        try {
            Class.forName(driver);
            List<Map<String, Object>> result = new ArrayList<>();
            try (Connection conn = connectionPoolManager.getConnection(dataSource)) {
                int offset = (page - 1) * pageSize;
                String dbType = dataSource.getDbType().toLowerCase();
                
                // 构建基础SQL（包含WHERE子句）
                String baseSql = "SELECT * FROM " + safeTableName + whereClause.getWhereClause();
                String sql;
                
                if ("mysql".equals(dbType)) {
                    sql = baseSql + " LIMIT " + offset + ", " + pageSize;
                } else if ("postgresql".equals(dbType)) {
                    sql = baseSql + " LIMIT " + pageSize + " OFFSET " + offset;
                } else if ("oracle".equals(dbType)) {
                    sql = "SELECT * FROM (SELECT ROWNUM rn, t.* FROM (" + baseSql + ") t WHERE ROWNUM <= " + (offset + pageSize) + ") WHERE rn > " + offset;
                } else if ("sqlserver".equals(dbType)) {
                    sql = "SELECT * FROM (SELECT ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS rn, * FROM (" + baseSql + ") t) t WHERE rn > " + offset + " AND rn <= " + (offset + pageSize);
                } else {
                    sql = baseSql + " LIMIT " + offset + ", " + pageSize;
                }
                
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    // 设置查询超时（从配置读取）
                    ps.setQueryTimeout(queryTimeout);
                    // 设置fetchSize优化大数据量查询
                    ps.setFetchSize(500);
                    
                    // 设置筛选条件的参数
                    List<Object> parameters = whereClause.getParameters();
                    for (int i = 0; i < parameters.size(); i++) {
                        ps.setObject(i + 1, parameters.get(i));
                    }
                    
                    try (ResultSet rs = ps.executeQuery()) {
                        ResultSetMetaData metaData = rs.getMetaData();
                        int columnCount = metaData.getColumnCount();
                        int rowCount = 0;
                        while (rs.next() && rowCount < maxQueryRows) {
                            Map<String, Object> row = new HashMap<>();
                            for (int i = 1; i <= columnCount; i++) {
                                Object value = rs.getObject(i);
                                // 处理大文本字段，防止内存溢出
                                if (value instanceof String && ((String) value).length() > 10000) {
                                    value = ((String) value).substring(0, 10000) + "...[截断]";
                                }
                                row.put(metaData.getColumnName(i), value);
                            }
                            result.add(row);
                            rowCount++;
                        }
                    }
                }
            }
            return result;
        } catch (ClassNotFoundException e) {
            throw new BusinessException(ErrorCode.DATA_SOURCE_CONNECTION_FAILED, "数据库驱动加载失败: " + e.getMessage());
        } catch (SQLException e) {
            String errorMsg = e.getMessage();
            // 提供更友好的错误信息
            if (errorMsg != null && errorMsg.contains("Table") && errorMsg.contains("doesn't exist")) {
                throw new BusinessException(ErrorCode.DATA_SOURCE_CONNECTION_FAILED, 
                    "表不存在: " + tableName + "，请检查表名是否正确");
            } else if (errorMsg != null && errorMsg.contains("Access denied")) {
                throw new BusinessException(ErrorCode.DATA_SOURCE_CONNECTION_FAILED, 
                    "数据库访问被拒绝，请检查数据源的用户名和密码");
            } else {
                throw new BusinessException(ErrorCode.DATA_SOURCE_CONNECTION_FAILED, 
                    "数据库查询失败: " + errorMsg);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.ERROR, "查询数据失败: " + e.getMessage());
        }
    }

    /**
     * 统计报表数据总数
     *
     * @param dataSourceId 数据源ID
     * @param tableName 表名
     * @param filters 筛选条件（JSON字符串）
     * @return 数据总数
     * @throws BusinessException 查询失败时抛出
     */
    public long countReport(Long dataSourceId, String tableName, String filters) {
        if (dataSourceId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "数据源ID不能为空");
        }
        if (!StringUtils.hasText(tableName)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "表名不能为空");
        }
        
        DataSource dataSource = dataSourceMapper.selectById(dataSourceId);
        if (dataSource == null) {
            throw new BusinessException(ErrorCode.DATA_SOURCE_NOT_FOUND, "数据源不存在");
        }
        
        // 验证数据源配置
        String database = dataSource.getDatabase();
        if (!StringUtils.hasText(database)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, 
                String.format("数据源的数据库名不能为空，请先完善数据源配置。数据源ID: %d", dataSourceId));
        }
        
        String url = dataSourceService.buildJdbcUrl(dataSource);
        String driver = dbConnectionUtil.getDriverClassName(dataSource.getDbType());
        
        // 验证表名安全性
        String safeTableName = tableName.replaceAll("[^a-zA-Z0-9_]", "");
        if (safeTableName.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "表名格式不正确");
        }

        // 解析筛选条件
        List<com.dataplatform.common.dto.FilterCondition> filterList = com.dataplatform.common.util.FilterUtil.parseFilters(filters);
        com.dataplatform.common.util.FilterUtil.FilterWhereClause whereClause = com.dataplatform.common.util.FilterUtil.buildDynamicWhereClause(filterList);

        try {
            Class.forName(driver);
            try (Connection conn = connectionPoolManager.getConnection(dataSource)) {
                String sql = "SELECT COUNT(*) FROM " + safeTableName + whereClause.getWhereClause();
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    // 设置筛选条件的参数
                    List<Object> parameters = whereClause.getParameters();
                    for (int i = 0; i < parameters.size(); i++) {
                        ps.setObject(i + 1, parameters.get(i));
                    }
                    
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            return rs.getLong(1);
                        }
                    }
                }
            }
            return 0;
        } catch (ClassNotFoundException e) {
            throw new BusinessException(ErrorCode.DATA_SOURCE_CONNECTION_FAILED, "数据库驱动加载失败: " + e.getMessage());
        } catch (SQLException e) {
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.contains("Table") && errorMsg.contains("doesn't exist")) {
                throw new BusinessException(ErrorCode.DATA_SOURCE_CONNECTION_FAILED, 
                    "表不存在: " + tableName + "，请检查表名是否正确");
            } else {
                throw new BusinessException(ErrorCode.DATA_SOURCE_CONNECTION_FAILED, 
                    "数据库查询失败: " + errorMsg);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.ERROR, "统计数据失败: " + e.getMessage());
        }
    }

    /**
     * 导出报表为CSV
     *
     * @param dataSourceId 数据源ID
     * @param tableName 表名
     * @param filters 筛选条件（JSON字符串，可选）
     * @return CSV文件字节数组
     * @throws BusinessException 导出失败时抛出
     */
    public byte[] exportReportAsCsv(Long dataSourceId, String tableName, String filters) {
        if (dataSourceId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "数据源ID不能为空");
        }
        if (!StringUtils.hasText(tableName)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "表名不能为空");
        }
        
        DataSource dataSource = dataSourceMapper.selectById(dataSourceId);
        if (dataSource == null) {
            throw new BusinessException(ErrorCode.DATA_SOURCE_NOT_FOUND, "数据源不存在");
        }
        
        String database = dataSource.getDatabase();
        if (!StringUtils.hasText(database)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, 
                String.format("数据源的数据库名不能为空，请先完善数据源配置。数据源ID: %d", dataSourceId));
        }
        
        String url = dataSourceService.buildJdbcUrl(dataSource);
        String driver = dbConnectionUtil.getDriverClassName(dataSource.getDbType());
        String safeTableName = tableName.replaceAll("[^a-zA-Z0-9_]", "");
        if (safeTableName.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "表名格式不正确");
        }

        long totalCount = countReport(dataSourceId, tableName, filters);
        licenseLimitService.assertReportExportRowsAllowed(totalCount);

        try {
            StringBuilder csvContent = new StringBuilder();
            Class.forName(driver);
            try (Connection conn = connectionPoolManager.getConnection(dataSource)) {
                List<com.dataplatform.common.dto.FilterCondition> filterList = com.dataplatform.common.util.FilterUtil.parseFilters(filters);
                com.dataplatform.common.util.FilterUtil.FilterWhereClause whereClause = com.dataplatform.common.util.FilterUtil.buildDynamicWhereClause(filterList);
                
                String sql = "SELECT * FROM " + safeTableName;
                String filteredSql = sql;
                if (!whereClause.getWhereClause().isEmpty()) {
                    filteredSql = sql + whereClause.getWhereClause();
                }
                
                try (PreparedStatement ps = conn.prepareStatement(filteredSql)) {
                    List<Object> parameters = whereClause.getParameters();
                    for (int i = 0; i < parameters.size(); i++) {
                        ps.setObject(i + 1, parameters.get(i));
                    }
                    
                    try (ResultSet rs = ps.executeQuery()) {
                        ResultSetMetaData metaData = rs.getMetaData();
                        int columnCount = metaData.getColumnCount();

                        // 写入表头
                        for (int i = 1; i <= columnCount; i++) {
                            if (i > 1) csvContent.append(",");
                            csvContent.append(escapeCsvValue(metaData.getColumnName(i)));
                        }
                        csvContent.append("\n");

                        // 写入数据
                        int rowCount = 0;
                        while (rs.next()) {
                            for (int i = 1; i <= columnCount; i++) {
                                if (i > 1) csvContent.append(",");
                                Object value = rs.getObject(i);
                                csvContent.append(escapeCsvValue(value != null ? value.toString() : ""));
                            }
                            csvContent.append("\n");
                            rowCount++;
                            licenseLimitService.assertReportExportRowsAllowed(rowCount);
                            
                            // 每10000行刷新一次
                            if (rowCount % 10000 == 0) {
                                // 可以在这里添加进度回调
                            }
                        }
                    }
                }
            }
            
            return csvContent.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        } catch (ClassNotFoundException e) {
            throw new BusinessException(ErrorCode.DATA_SOURCE_CONNECTION_FAILED, "数据库驱动加载失败: " + e.getMessage());
        } catch (SQLException e) {
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.contains("Table") && errorMsg.contains("doesn't exist")) {
                throw new BusinessException(ErrorCode.DATA_SOURCE_CONNECTION_FAILED, 
                    "表不存在: " + tableName + "，请检查表名是否正确");
            } else {
                throw new BusinessException(ErrorCode.DATA_SOURCE_CONNECTION_FAILED, 
                    "数据库查询失败: " + errorMsg);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.ERROR, "导出CSV失败: " + e.getMessage());
        }
    }
    
    /**
     * 转义CSV值（处理逗号、引号、换行符）
     */
    private String escapeCsvValue(String value) {
        if (value == null) {
            return "";
        }
        // 如果包含逗号、引号或换行符，需要用引号包裹，并转义引号
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
    
    /**
     * 导出报表为Excel
     *
     * @param dataSourceId 数据源ID
     * @param tableName 表名
     * @param filters 筛选条件（JSON字符串，可选）
     * @return Excel文件字节数组
     * @throws BusinessException 导出失败时抛出
     */
    public byte[] exportReport(Long dataSourceId, String tableName, String filters) {
        if (dataSourceId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "数据源ID不能为空");
        }
        if (!StringUtils.hasText(tableName)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "表名不能为空");
        }
        
        DataSource dataSource = dataSourceMapper.selectById(dataSourceId);
        if (dataSource == null) {
            throw new BusinessException(ErrorCode.DATA_SOURCE_NOT_FOUND, "数据源不存在");
        }
        
        // 验证数据源配置
        String database = dataSource.getDatabase();
        if (!StringUtils.hasText(database)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, 
                String.format("数据源的数据库名不能为空，请先完善数据源配置。数据源ID: %d", dataSourceId));
        }
        
        String url = dataSourceService.buildJdbcUrl(dataSource);
        String driver = dbConnectionUtil.getDriverClassName(dataSource.getDbType());
        
        // 验证表名安全性
        String safeTableName = tableName.replaceAll("[^a-zA-Z0-9_]", "");
        if (safeTableName.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "表名格式不正确");
        }

        // 先检查数据量，防止导出过大数据
        long totalCount = countReport(dataSourceId, tableName, filters);
        licenseLimitService.assertReportExportRowsAllowed(totalCount);
        if (totalCount > maxExportRows) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, 
                String.format("数据量过大（%d行），超过最大导出限制（%d行）。请添加筛选条件减少数据量。", 
                    totalCount, maxExportRows));
        }
        
        log.info("开始导出Excel，预计数据量: {} 行", totalCount);
        
        // 使用SXSSFWorkbook支持百万级数据导出，设置内存中保留500行
        SXSSFWorkbook workbook = new SXSSFWorkbook(500);
        workbook.setCompressTempFiles(true); // 压缩临时文件，减少磁盘占用
        
        try {
            Class.forName(driver);
            try (Connection conn = connectionPoolManager.getConnection(dataSource)) {
                // 设置流式读取，避免一次性加载全部数据到内存
                conn.setAutoCommit(false);
                
                // 解析和应用筛选条件
                List<com.dataplatform.common.dto.FilterCondition> filterList = com.dataplatform.common.util.FilterUtil.parseFilters(filters);
                com.dataplatform.common.util.FilterUtil.FilterWhereClause whereClause = com.dataplatform.common.util.FilterUtil.buildDynamicWhereClause(filterList);
                
                // 构建带筛选条件的SQL
                String sql = "SELECT * FROM " + safeTableName;
                String filteredSql = sql;
                if (!whereClause.getWhereClause().isEmpty()) {
                    filteredSql = sql + whereClause.getWhereClause();
                }
                
                // 使用流式查询，设置fetchSize避免内存溢出
                try (PreparedStatement ps = conn.prepareStatement(filteredSql, 
                        ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
                    ps.setFetchSize(1000); // 每次从数据库获取1000行
                    
                    // 设置筛选条件的参数
                    List<Object> parameters = whereClause.getParameters();
                    for (int i = 0; i < parameters.size(); i++) {
                        ps.setObject(i + 1, parameters.get(i));
                    }
                    
                    try (ResultSet rs = ps.executeQuery()) {
                        ResultSetMetaData metaData = rs.getMetaData();
                        int columnCount = metaData.getColumnCount();
                        String[] columnNames = new String[columnCount];
                        for (int i = 0; i < columnCount; i++) {
                            columnNames[i] = metaData.getColumnName(i + 1);
                        }
                        
                        // 创建表头样式（只创建一次）
                        CellStyle headerStyle = workbook.createCellStyle();
                        Font headerFont = workbook.createFont();
                        headerFont.setBold(true);
                        headerStyle.setFont(headerFont);
                        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
                        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

                        int sheetIndex = 1;
                        int rowNumInSheet = 0;
                        int totalRowNum = 0;
                        Sheet currentSheet = null;
                        
                        while (rs.next()) {
                            // 创建新Sheet（首次或达到行数上限）
                            if (currentSheet == null || rowNumInSheet >= maxRowsPerSheet) {
                                if (currentSheet != null) {
                                    // 刷新上一个Sheet的剩余行
                                    try {
                                        ((SXSSFSheet) currentSheet).flushRows();
                                    } catch (Exception e) {
                                        log.trace("刷新Sheet行数据异常: {}", e.getMessage());
                                    }
                                }
                                
                                String sheetName = totalCount > maxRowsPerSheet ? 
                                    "数据_" + sheetIndex++ : "数据";
                                currentSheet = workbook.createSheet(sheetName);
                                rowNumInSheet = 0;
                                
                                // 写入表头
                                Row headerRow = currentSheet.createRow(rowNumInSheet++);
                                for (int i = 0; i < columnCount; i++) {
                                    Cell cell = headerRow.createCell(i);
                                    cell.setCellValue(columnNames[i]);
                                    cell.setCellStyle(headerStyle);
                                }
                                
                                log.info("创建Sheet: {}", sheetName);
                            }
                            
                            // 写入数据行
                            Row row = currentSheet.createRow(rowNumInSheet++);
                            for (int i = 0; i < columnCount; i++) {
                                Cell cell = row.createCell(i);
                                Object value = rs.getObject(i + 1);
                                if (value != null) {
                                    if (value instanceof Number) {
                                        cell.setCellValue(((Number) value).doubleValue());
                                    } else {
                                        String strValue = value.toString();
                                        // Excel单元格最大32767字符
                                        if (strValue.length() > 32767) {
                                            strValue = strValue.substring(0, 32760) + "...";
                                        }
                                        cell.setCellValue(strValue);
                                    }
                                }
                            }
                            
                            totalRowNum++;
                            licenseLimitService.assertReportExportRowsAllowed(totalRowNum);
                            
                            // 每1000行刷新一次，释放内存
                            if (rowNumInSheet % 1000 == 0) {
                                try {
                                    ((SXSSFSheet) currentSheet).flushRows();
                                } catch (Exception e) {
                                        log.trace("定期刷新Sheet异常: {}", e.getMessage());
                                }
                                
                                // 每10万行记录一次日志
                                if (totalRowNum % 100000 == 0) {
                                    log.info("Excel导出进度: {} 行", totalRowNum);
                                }
                            }
                        }
                        
                        log.info("Excel导出完成，总计: {} 行，{} 个Sheet", totalRowNum, sheetIndex - 1);
                    }
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (ClassNotFoundException e) {
            throw new BusinessException(ErrorCode.DATA_SOURCE_CONNECTION_FAILED, "数据库驱动加载失败: " + e.getMessage());
        } catch (SQLException e) {
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.contains("Table") && errorMsg.contains("doesn't exist")) {
                throw new BusinessException(ErrorCode.DATA_SOURCE_CONNECTION_FAILED, 
                    "表不存在: " + tableName + "，请检查表名是否正确");
            } else {
                throw new BusinessException(ErrorCode.DATA_SOURCE_CONNECTION_FAILED, 
                    "数据库查询失败: " + errorMsg);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.ERROR, "导出数据失败: " + e.getMessage());
        } finally {
            try {
                // SXSSFWorkbook需要清理临时文件
                workbook.dispose();
                workbook.close();
            } catch (Exception e) {
                log.trace("关闭SXSSFWorkbook异常: {}", e.getMessage());
            }
        }
    }
}
