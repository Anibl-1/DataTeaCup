package com.dataplatform.data.service;

import com.dataplatform.common.exception.BusinessException;
import com.dataplatform.common.exception.ErrorCode;
import com.dataplatform.data.entity.DataSource;
import com.dataplatform.data.mapper.DataSourceMapper;
import com.dataplatform.data.service.DbConnectionUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/**
 * 数据导入服务类
 * 处理Excel和TXT文件导入到数据库
 * 
 * @author dataplatform
 */
@Slf4j
@Service
public class DataImportService {
    @Autowired
    private DataSourceMapper dataSourceMapper;
    
    @Autowired
    private DataSourceService dataSourceService;
    
    @Autowired
    private DbConnectionUtil dbConnectionUtil;

    @Autowired
    private com.dataplatform.data.service.DataSourceConnectionPoolManager connectionPoolManager;

    /**
     * 导入数据文件到数据库
     * 
     * @param file 上传的文件（Excel或TXT）
     * @param dataSourceId 目标数据源ID
     * @param tableName 目标表名（如果为空则自动创建）
     * @param autoCreateTable 是否自动创建表
     * @param firstRowAsHeader 第一行是否作为表头
     * @return 导入结果（成功行数、失败行数等）
     */
    // 最大文件大小：100MB
    private static final long MAX_FILE_SIZE = 100 * 1024 * 1024;
    
    /**
     * 生成导入模板（含表头的空Excel文件）
     *
     * @param dataSourceId 数据源ID
     * @param tableName    表名
     * @return Excel字节数组
     */
    public byte[] generateImportTemplate(Long dataSourceId, String tableName) {
        DataSource dataSource = dataSourceMapper.selectById(dataSourceId);
        if (dataSource == null) {
            throw new BusinessException(ErrorCode.DATA_SOURCE_NOT_FOUND, "数据源不存在");
        }
        String url = dataSourceService.buildJdbcUrl(dataSource);
        String driver = dbConnectionUtil.getDriverClassName(dataSource.getDbType());
        try {
            Class.forName(driver);
            try (java.sql.Connection conn = connectionPoolManager.getConnection(dataSource)) {
                java.sql.DatabaseMetaData meta = conn.getMetaData();
                java.sql.ResultSet rs;
                String dbType = dataSource.getDbType().toLowerCase();
                if ("postgresql".equals(dbType)) {
                    rs = meta.getColumns(null, "public", tableName, "%");
                } else if ("oracle".equals(dbType)) {
                    rs = meta.getColumns(null, dataSource.getUsername() != null ? dataSource.getUsername().toUpperCase() : null, tableName.toUpperCase(), "%");
                } else {
                    rs = meta.getColumns(dataSource.getDatabase(), null, tableName, "%");
                }
                List<String> columns = new ArrayList<>();
                List<String> comments = new ArrayList<>();
                while (rs.next()) {
                    columns.add(rs.getString("COLUMN_NAME"));
                    String remark = rs.getString("REMARKS");
                    comments.add(remark != null ? remark : "");
                }
                rs.close();
                if (columns.isEmpty()) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "无法获取表结构，请检查表名是否正确");
                }
                // 生成Excel
                try (XSSFWorkbook wb = new XSSFWorkbook()) {
                    Sheet sheet = wb.createSheet(tableName);
                    // 表头行
                    Row headerRow = sheet.createRow(0);
                    CellStyle headerStyle = wb.createCellStyle();
                    Font headerFont = wb.createFont();
                    headerFont.setBold(true);
                    headerStyle.setFont(headerFont);
                    headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
                    headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                    for (int i = 0; i < columns.size(); i++) {
                        Cell cell = headerRow.createCell(i);
                        String label = comments.get(i).isEmpty() ? columns.get(i) : columns.get(i) + "(" + comments.get(i) + ")";
                        cell.setCellValue(label);
                        cell.setCellStyle(headerStyle);
                        sheet.setColumnWidth(i, 5000);
                    }
                    java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
                    wb.write(bos);
                    return bos.toByteArray();
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.ERROR, "生成导入模板失败: " + e.getMessage());
        }
    }

    /**
     * 预览文件内容（不实际导入）
     * 
     * @param file 上传的文件
     * @param firstRowAsHeader 第一行是否作为表头
     * @param previewRows 预览行数
     * @return 预览数据（表头、前N行数据、总行数等）
     */
    public Map<String, Object> previewFile(MultipartFile file, boolean firstRowAsHeader, int previewRows) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "上传文件不能为空");
        }
        
        // 检查文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, 
                String.format("文件大小超过限制，最大允许 %dMB，当前文件大小: %.2fMB", 
                    MAX_FILE_SIZE / 1024 / 1024, file.getSize() / 1024.0 / 1024.0));
        }
        
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件名不能为空");
        }
        
        int lastDotIndex = fileName.lastIndexOf(".");
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件缺少扩展名，无法识别文件类型");
        }
        
        String fileExtension = fileName.substring(lastDotIndex + 1).toLowerCase();
        
        try {
            List<Map<String, Object>> allDataList;
            List<String> headers = new ArrayList<>();
            
            if ("xlsx".equals(fileExtension) || "xls".equals(fileExtension)) {
                Map<String, Object> excelData = parseExcelFile(file, firstRowAsHeader);
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> excelDataList = (List<Map<String, Object>>) excelData.get("data");
                @SuppressWarnings("unchecked")
                List<String> excelHeaders = (List<String>) excelData.get("headers");
                allDataList = excelDataList;
                headers = excelHeaders;
            } else if ("txt".equals(fileExtension) || "csv".equals(fileExtension)) {
                Map<String, Object> txtData = parseTxtFile(file, firstRowAsHeader);
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> txtDataList = (List<Map<String, Object>>) txtData.get("data");
                @SuppressWarnings("unchecked")
                List<String> txtHeaders = (List<String>) txtData.get("headers");
                allDataList = txtDataList;
                headers = txtHeaders;
            } else {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的文件格式");
            }
            
            // 只返回前N行数据用于预览
            int totalRows = allDataList.size();
            List<Map<String, Object>> previewData = allDataList.subList(0, Math.min(previewRows, totalRows));
            
            Map<String, Object> result = new HashMap<>();
            result.put("headers", headers);
            result.put("previewData", previewData);
            result.put("totalRows", totalRows);
            result.put("previewRows", previewData.size());
            result.put("fileName", fileName);
            result.put("fileSize", file.getSize());
            
            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.ERROR, "预览文件失败: " + e.getMessage());
        }
    }
    
    public Map<String, Object> importData(MultipartFile file, Long dataSourceId, String tableName, 
                                         boolean autoCreateTable, boolean firstRowAsHeader, boolean truncateFirst) {
        return importData(file, dataSourceId, tableName, autoCreateTable, firstRowAsHeader, truncateFirst, "full", null, null, null);
    }

    /**
     * 导入数据（支持导入模式）
     *
     * @param importMode         导入模式：full/incremental/conditional
     * @param deduplicateField   去重字段（增量模式使用）
     * @param incrementField     增量字段（数据库增量模式使用）
     * @param filterConditionsJson 筛选条件JSON（按字段条件模式使用）
     */
    public Map<String, Object> importData(MultipartFile file, Long dataSourceId, String tableName, 
                                         boolean autoCreateTable, boolean firstRowAsHeader, boolean truncateFirst,
                                         String importMode, String deduplicateField, String incrementField, String filterConditionsJson) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "上传文件不能为空");
        }
        
        // 检查文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, 
                String.format("文件大小超过限制，最大允许 %dMB，当前文件大小: %.2fMB", 
                    MAX_FILE_SIZE / 1024 / 1024, file.getSize() / 1024.0 / 1024.0));
        }
        
        if (dataSourceId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "数据源ID不能为空");
        }
        
        DataSource dataSource = dataSourceMapper.selectById(dataSourceId);
        if (dataSource == null) {
            throw new BusinessException(ErrorCode.DATA_SOURCE_NOT_FOUND, "数据源不存在");
        }
        
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件名不能为空");
        }
        
        // 检查文件扩展名
        int lastDotIndex = fileName.lastIndexOf(".");
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件缺少扩展名，无法识别文件类型");
        }
        
        String fileExtension = fileName.substring(lastDotIndex + 1).toLowerCase();
        
        try {
            List<Map<String, Object>> dataList;
            List<String> headers = new ArrayList<>();
            
            if ("xlsx".equals(fileExtension) || "xls".equals(fileExtension)) {
                // 处理Excel文件
                Map<String, Object> excelData = parseExcelFile(file, firstRowAsHeader);
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> excelDataList = (List<Map<String, Object>>) excelData.get("data");
                @SuppressWarnings("unchecked")
                List<String> excelHeaders = (List<String>) excelData.get("headers");
                dataList = excelDataList;
                headers = excelHeaders;
            } else if ("txt".equals(fileExtension) || "csv".equals(fileExtension)) {
                // 处理TXT/CSV文件
                Map<String, Object> txtData = parseTxtFile(file, firstRowAsHeader);
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> txtDataList = (List<Map<String, Object>>) txtData.get("data");
                @SuppressWarnings("unchecked")
                List<String> txtHeaders = (List<String>) txtData.get("headers");
                dataList = txtDataList;
                headers = txtHeaders;
            } else {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的文件格式，仅支持Excel(.xlsx, .xls)和文本文件(.txt, .csv)");
            }
            
            if (dataList == null || dataList.isEmpty()) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "文件内容为空");
            }
            
            // 验证数据量
            if (dataList.size() > 1000000) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, 
                    String.format("数据量过大（%d行），超过最大限制（100万行），请分批导入", dataList.size()));
            }
            
            // 验证表头数量
            if (headers.isEmpty()) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "无法识别表头，请确保文件格式正确");
            }
            
            // 确定表名
            String finalTableName = tableName;
            if (!StringUtils.hasText(finalTableName)) {
                // 从文件名生成表名
                String fileNameWithoutExt = fileName.substring(0, fileName.lastIndexOf("."));
                finalTableName = normalizeTableName(fileNameWithoutExt);
            } else {
                // 规范化用户输入的表名，保留中文
                finalTableName = normalizeTableName(finalTableName);
            }
            
            // 验证表名不为空
            if (finalTableName.isEmpty()) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "表名格式不正确");
            }
            
            // 根据导入模式处理数据
            if ("incremental".equals(importMode) && StringUtils.hasText(deduplicateField)) {
                // 增量模式：去除与目标表重复的行
                truncateFirst = false;
                dataList = filterIncrementalData(dataSource, finalTableName, deduplicateField, headers, dataList);
            } else if ("conditional".equals(importMode) && StringUtils.hasText(filterConditionsJson)) {
                // 按条件模式：在内存中过滤不满足条件的行
                truncateFirst = false;
                dataList = filterConditionalData(filterConditionsJson, dataList);
            } else if ("full".equals(importMode)) {
                // 全量模式：保持 truncateFirst 逻辑不变
            }

            // 导入数据到数据库（表名已规范化，保留中文）
            return importToDatabase(dataSource, finalTableName, headers, dataList, autoCreateTable, truncateFirst);
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.ERROR, "导入数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 解析Excel文件
     */
    private Map<String, Object> parseExcelFile(MultipartFile file, boolean firstRowAsHeader) throws Exception {
        List<Map<String, Object>> dataList = new ArrayList<>();
        List<String> headers = new ArrayList<>();
        
        try (InputStream is = file.getInputStream()) {
            Workbook workbook;
            String fileName = file.getOriginalFilename();
            if (fileName != null && fileName.endsWith(".xlsx")) {
                workbook = new XSSFWorkbook(is);
            } else {
                workbook = new HSSFWorkbook(is);
            }
            
            Sheet sheet = workbook.getSheetAt(0);
            int startRow = 0;
            
            // 读取表头
            if (firstRowAsHeader && sheet.getPhysicalNumberOfRows() > 0) {
                Row headerRow = sheet.getRow(0);
                if (headerRow != null) {
                    for (Cell cell : headerRow) {
                        String header = getCellValue(cell);
                        headers.add(StringUtils.hasText(header) ? header : "column_" + headers.size());
                    }
                }
                startRow = 1;
            } else {
                // 如果没有表头，根据第一行数据生成列名
                if (sheet.getPhysicalNumberOfRows() > 0) {
                    Row firstRow = sheet.getRow(0);
                    if (firstRow != null) {
                        int cellCount = firstRow.getPhysicalNumberOfCells();
                        for (int i = 0; i < cellCount; i++) {
                            headers.add("column_" + (i + 1));
                        }
                    }
                }
            }
            
            // 读取数据
            for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                Map<String, Object> rowData = new HashMap<>();
                for (int j = 0; j < headers.size(); j++) {
                    Cell cell = row.getCell(j);
                    String value = getCellValue(cell);
                    rowData.put(headers.get(j), value);
                }
                dataList.add(rowData);
            }
            
            workbook.close();
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("data", dataList);
        result.put("headers", headers);
        return result;
    }
    
    /**
     * 解析TXT/CSV文件
     */
    private Map<String, Object> parseTxtFile(MultipartFile file, boolean firstRowAsHeader) throws Exception {
        List<Map<String, Object>> dataList = new ArrayList<>();
        List<String> headers = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            
            String line;
            boolean headerRead = false;
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                
                // 解析CSV行（支持引号内的逗号）
                List<String> values = parseCsvLine(line);
                
                if (firstRowAsHeader && !headerRead) {
                    headers.addAll(values);
                    headerRead = true;
                } else if (!headerRead) {
                    // 第一行作为数据，生成列名
                    for (int i = 0; i < values.size(); i++) {
                        headers.add("column_" + (i + 1));
                    }
                    headerRead = true;
                    // 将第一行数据也加入
                    Map<String, Object> rowData = new HashMap<>();
                    for (int i = 0; i < headers.size() && i < values.size(); i++) {
                        rowData.put(headers.get(i), values.get(i));
                    }
                    dataList.add(rowData);
                } else {
                    // 数据行
                    Map<String, Object> rowData = new HashMap<>();
                    for (int i = 0; i < headers.size() && i < values.size(); i++) {
                        rowData.put(headers.get(i), values.get(i));
                    }
                    dataList.add(rowData);
                }
            }
            
            // 如果没有表头且没有数据，生成默认列名
            if (!headerRead && dataList.isEmpty()) {
                headers.add("column_1");
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("data", dataList);
        result.put("headers", headers);
        return result;
    }
    
    /**
     * 解析CSV行（简单实现，支持引号）
     */
    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder currentValue = new StringBuilder();
        
        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                values.add(currentValue.toString().trim());
                currentValue = new StringBuilder();
            } else {
                currentValue.append(c);
            }
        }
        values.add(currentValue.toString().trim());
        
        return values;
    }
    
    /**
     * 获取单元格值
     */
    private String getCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    // 避免科学计数法
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == (long) numericValue) {
                        return String.valueOf((long) numericValue);
                    } else {
                        return String.valueOf(numericValue);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
    
    /**
     * 导入数据到数据库
     */
    private Map<String, Object> importToDatabase(DataSource dataSource, String tableName, 
                                                 List<String> headers, List<Map<String, Object>> dataList,
                                                 boolean autoCreateTable, boolean truncateFirst) throws Exception {
        String url = dataSourceService.buildJdbcUrl(dataSource);
        String driver = dbConnectionUtil.getDriverClassName(dataSource.getDbType());
        
        Class.forName(driver);
        try (Connection conn = connectionPoolManager.getConnection(dataSource)) {
            // 检查表是否存在
            boolean tableExists = checkTableExists(conn, tableName, dataSource.getDbType());
            boolean tableCreated = false;
            
            log.debug("[DataImport] 表名: {}, 表存在: {}, 自动创建: {}, 清空表: {}", tableName, tableExists, autoCreateTable, truncateFirst);
            
            if (!tableExists && autoCreateTable) {
                // 自动创建表
                createTable(conn, tableName, headers, dataList, dataSource.getDbType());
                tableCreated = true;
                log.info("[DataImport] 已创建新表: {}", tableName);
            } else if (!tableExists) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, 
                    "表不存在且未启用自动创建表功能，请先创建表或启用自动创建");
            }
            
            // 如果需要清空表且表已存在（不是新创建的），先执行 TRUNCATE
            boolean tableTruncated = false;
            if (truncateFirst && tableExists) {
                log.debug("[DataImport] 正在清空表: {}", tableName);
                truncateTable(conn, tableName, dataSource.getDbType());
                tableTruncated = true;
                log.info("[DataImport] 表已清空: {}", tableName);
            }
            
            // 插入数据
            int successCount = insertData(conn, tableName, headers, dataList, dataSource.getDbType());
            int failCount = dataList.size() - successCount;
            
            log.info("[DataImport] 导入完成: 成功 {} 条, 失败 {} 条", successCount, failCount);
            
            Map<String, Object> result = new HashMap<>();
            result.put("successCount", successCount);
            result.put("failCount", failCount);
            result.put("totalCount", dataList.size());
            // 返回规范化后的表名（保留中文）
            result.put("tableName", normalizeTableName(tableName));
            result.put("tableCreated", tableCreated);
            result.put("tableTruncated", tableTruncated);
            
            return result;
        }
    }
    
    /**
     * 清空表数据
     */
    private void truncateTable(Connection conn, String tableName, String dbType) throws SQLException {
        String normalizedTableName = normalizeTableName(tableName);
        String sql;
        
        if ("mysql".equalsIgnoreCase(dbType)) {
            sql = "TRUNCATE TABLE `" + normalizedTableName + "`";
        } else if ("postgresql".equalsIgnoreCase(dbType)) {
            sql = "TRUNCATE TABLE \"" + normalizedTableName + "\" RESTART IDENTITY";
        } else {
            sql = "DELETE FROM `" + normalizedTableName + "`";
        }
        
        log.debug("[DataImport] 执行清空表SQL: {}", sql);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            log.debug("[DataImport] 清空表成功");
        } catch (SQLException e) {
            log.error("[DataImport] 清空表失败: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * 检查表是否存在
     */
    private boolean checkTableExists(Connection conn, String tableName, String dbType) throws SQLException {
        // 规范化表名（保留中文）
        String normalizedTableName = normalizeTableName(tableName);
        
        String sql;
        if ("mysql".equalsIgnoreCase(dbType)) {
            sql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?";
        } else if ("postgresql".equalsIgnoreCase(dbType)) {
            sql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = ?";
        } else {
            // 默认尝试查询表（使用反引号包裹表名）
            try {
                conn.prepareStatement("SELECT 1 FROM `" + normalizedTableName + "` LIMIT 1").executeQuery();
                return true;
            } catch (SQLException e) {
                return false;
            }
        }
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, normalizedTableName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }
    
    /**
     * 规范化列名，确保符合SQL标识符规范
     * 处理纯数字列名、特殊字符等，但保留中文字符
     */
    private String normalizeColumnName(String originalName, int index) {
        if (originalName == null || originalName.trim().isEmpty()) {
            return "column_" + (index + 1);
        }
        
        String cleaned = originalName.trim();
        
        // 只移除真正危险的字符（SQL注入相关），保留中文、字母、数字、下划线
        // 移除：反引号、单引号、双引号、分号、注释符号等
        cleaned = cleaned.replaceAll("[`'\"\\;\\-\\-\\/\\*\\[\\]\\{\\}]", "");
        
        // 移除控制字符和空白字符（但保留空格，因为MySQL支持）
        cleaned = cleaned.replaceAll("[\\x00-\\x1F\\x7F]", "");
        
        // 如果清理后为空，使用默认名称
        if (cleaned.isEmpty()) {
            cleaned = "column_" + (index + 1);
        }
        
        // 对于纯数字列名，添加前缀以确保MySQL兼容性
        // 但保留中文数字（如"一"、"二"等）
        if (cleaned.matches("^\\d+$")) {
            cleaned = "col_" + cleaned;
        }
        
        // 如果列名以数字开头（且不是中文），添加前缀
        // 但保留中文开头的列名
        if (cleaned.matches("^\\d") && !cleaned.matches("^[\\u4e00-\\u9fa5]")) {
            cleaned = "col_" + cleaned;
        }
        
        return cleaned;
    }
    
    /**
     * 规范化表名，保留中文字符
     */
    private String normalizeTableName(String originalName) {
        if (originalName == null || originalName.trim().isEmpty()) {
            return "imported_data_" + System.currentTimeMillis();
        }
        
        String cleaned = originalName.trim();
        
        // 只移除真正危险的字符（SQL注入相关），保留中文、字母、数字、下划线
        // 移除：反引号、单引号、双引号、分号、斜杠、星号、方括号、花括号
        cleaned = cleaned.replaceAll("[`'\";\\/\\*\\[\\]\\{\\}]", "");
        
        // 移除连续的减号（SQL注释）
        cleaned = cleaned.replaceAll("--+", "");
        
        // 移除控制字符
        cleaned = cleaned.replaceAll("[\\x00-\\x1F\\x7F]", "");
        
        // 如果清理后为空，使用默认名称
        if (cleaned.isEmpty()) {
            cleaned = "imported_data_" + System.currentTimeMillis();
        }
        
        return cleaned;
    }
    
    /**
     * 创建表
     */
    private void createTable(Connection conn, String tableName, List<String> headers, 
                            List<Map<String, Object>> dataList, String dbType) throws SQLException, BusinessException {
        if (headers.isEmpty() || dataList.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "无法创建表：表头或数据为空");
        }
        
        // 分析第一行数据确定列类型
        Map<String, Object> firstRow = dataList.get(0);
        // 规范化表名（保留中文）
        String normalizedTableName = normalizeTableName(tableName);
        // 使用反引号包裹表名，确保包含中文的特殊表名也能正常工作
        StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS `").append(normalizedTableName).append("` (");
        
        for (int i = 0; i < headers.size(); i++) {
            String originalHeader = headers.get(i);
            String normalizedHeader = normalizeColumnName(originalHeader, i);
            
            Object value = firstRow.get(originalHeader);
            String columnType = inferColumnType(value, dbType);
            
            if (i > 0) sql.append(", ");
            // 使用反引号包裹列名，确保特殊列名（如纯数字）也能正常工作
            sql.append("`").append(normalizedHeader).append("` ").append(columnType);
        }
        
        sql.append(")");
        
        if ("mysql".equalsIgnoreCase(dbType)) {
            sql.append(" ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        }
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql.toString());
        }
    }
    
    /**
     * 推断列类型
     */
    private String inferColumnType(Object value, String dbType) {
        if (value == null || value.toString().trim().isEmpty()) {
            return "mysql".equalsIgnoreCase(dbType) ? "VARCHAR(255)" : "VARCHAR(255)";
        }
        
        String strValue = value.toString().trim();
        
        // 尝试解析为数字
        try {
            Double.parseDouble(strValue);
            // 检查是否为整数
            if (strValue.matches("^-?\\d+$")) {
                return "mysql".equalsIgnoreCase(dbType) ? "BIGINT" : "BIGINT";
            } else {
                return "mysql".equalsIgnoreCase(dbType) ? "DOUBLE" : "DOUBLE PRECISION";
            }
        } catch (NumberFormatException e) {
            // 不是数字，检查是否为日期
            if (strValue.matches("\\d{4}-\\d{2}-\\d{2}.*")) {
                return "mysql".equalsIgnoreCase(dbType) ? "DATETIME" : "TIMESTAMP";
            } else {
                // 默认为文本，根据长度选择类型
                int length = strValue.length();
                if (length > 500) {
                    return "mysql".equalsIgnoreCase(dbType) ? "TEXT" : "TEXT";
                } else {
                    int varcharLength = Math.max(255, (length + 50) / 50 * 50); // 向上取整到50的倍数
                    return "VARCHAR(" + varcharLength + ")";
                }
            }
        }
    }
    
    /**
     * 插入数据
     */
    private int insertData(Connection conn, String tableName, List<String> headers, 
                           List<Map<String, Object>> dataList, String dbType) throws SQLException {
        if (headers.isEmpty()) {
            log.warn("[DataImport] 表头为空，无法插入数据");
            return 0;
        }
        
        if (dataList.isEmpty()) {
            log.debug("[DataImport] 数据列表为空，无需插入");
            return 0;
        }
        
        // 构建INSERT SQL
        StringBuilder safeHeadersBuilder = new StringBuilder();
        StringBuilder placeholdersBuilder = new StringBuilder();
        
        for (int i = 0; i < headers.size(); i++) {
            if (i > 0) {
                safeHeadersBuilder.append(", ");
                placeholdersBuilder.append(", ");
            }
            String originalHeader = headers.get(i);
            // 使用与创建表时相同的规范化方法
            String safeHeader = normalizeColumnName(originalHeader, i);
            
            // 使用反引号包裹列名，确保特殊列名（如纯数字）也能正常工作
            safeHeadersBuilder.append("`").append(safeHeader).append("`");
            placeholdersBuilder.append("?");
        }
        
        // 规范化表名（保留中文）
        String normalizedTableName = normalizeTableName(tableName);
        String sql = "INSERT INTO `" + normalizedTableName + "` (" + safeHeadersBuilder.toString() + ") VALUES (" + placeholdersBuilder.toString() + ")";
        
        log.debug("[DataImport] INSERT SQL: {}", sql);
        log.debug("[DataImport] 待插入数据行数: {}", dataList.size());
        
        int successCount = 0;
        int batchCount = 0;
        int batchSize = 1000; // 批量插入大小
        boolean autoCommit = conn.getAutoCommit();
        
        try {
            // 关闭自动提交以提高性能
            conn.setAutoCommit(false);
            
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (int rowIndex = 0; rowIndex < dataList.size(); rowIndex++) {
                    Map<String, Object> row = dataList.get(rowIndex);
                    try {
                        for (int i = 0; i < headers.size(); i++) {
                            Object value = row.get(headers.get(i));
                            // 处理空值
                            if (value == null || (value instanceof String && ((String) value).trim().isEmpty())) {
                                ps.setNull(i + 1, Types.VARCHAR);
                            } else {
                                ps.setObject(i + 1, value);
                            }
                        }
                        ps.addBatch();
                        batchCount++;
                        
                        // 批量执行
                        if (batchCount >= batchSize) {
                            int[] results = ps.executeBatch();
                            conn.commit();
                            for (int result : results) {
                                if (result >= 0 || result == Statement.SUCCESS_NO_INFO) {
                                    successCount++;
                                }
                            }
                            log.debug("[DataImport] 已执行批次，当前成功: {}", successCount);
                            batchCount = 0;
                        }
                    } catch (SQLException e) {
                        log.warn("[DataImport] 准备数据行 {} 失败: {}", rowIndex, e.getMessage());
                    }
                }
                
                // 执行剩余的批次
                if (batchCount > 0) {
                    log.debug("[DataImport] 执行剩余批次，行数: {}", batchCount);
                    int[] results = ps.executeBatch();
                    conn.commit();
                    for (int result : results) {
                        if (result >= 0 || result == Statement.SUCCESS_NO_INFO) {
                            successCount++;
                        }
                    }
                }
            }
            
            log.info("[DataImport] 插入完成，成功行数: {}", successCount);
            
        } catch (SQLException e) {
            log.error("[DataImport] 批量插入失败: {}", e.getMessage(), e);
            // 发生错误时回滚
            try {
                conn.rollback();
            } catch (SQLException rollbackEx) {
                log.error("[DataImport] 回滚失败: {}", rollbackEx.getMessage());
            }
            throw e;
        } finally {
            // 恢复自动提交设置
            try {
                conn.setAutoCommit(autoCommit);
            } catch (SQLException e) {
                log.warn("[DataImport] 恢复自动提交设置失败: {}", e.getMessage());
            }
        }
        
        return successCount;
    }
    
    /**
     * 校验导入数据（不实际导入，仅返回校验结果）
     */
    public Map<String, Object> validateImportData(MultipartFile file, Long dataSourceId, String tableName, boolean firstRowAsHeader) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "上传文件不能为空");
        }
        DataSource dataSource = dataSourceMapper.selectById(dataSourceId);
        if (dataSource == null) {
            throw new BusinessException(ErrorCode.DATA_SOURCE_NOT_FOUND, "数据源不存在");
        }
        
        // 解析文件获取数据
        Map<String, Object> preview = previewFile(file, firstRowAsHeader, Integer.MAX_VALUE);
        @SuppressWarnings("unchecked")
        List<String> headers = (List<String>) preview.get("headers");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dataList = (List<Map<String, Object>>) preview.get("previewData");
        int totalRows = dataList.size();
        
        List<Map<String, Object>> errors = new ArrayList<>();
        int passedRows = 0;
        int failedRows = 0;
        int warningRows = 0;
        
        // 获取目标表的列信息用于校验
        Map<String, String> columnTypes = new HashMap<>();
        Set<String> notNullColumns = new HashSet<>();
        try {
            String url = dataSourceService.buildJdbcUrl(dataSource);
            String driver = dbConnectionUtil.getDriverClassName(dataSource.getDbType());
            Class.forName(driver);
            try (Connection conn = connectionPoolManager.getConnection(dataSource)) {
                DatabaseMetaData meta = conn.getMetaData();
                String dbType = dataSource.getDbType().toLowerCase();
                ResultSet rs;
                if ("postgresql".equals(dbType)) {
                    rs = meta.getColumns(null, "public", tableName, "%");
                } else {
                    rs = meta.getColumns(dataSource.getDatabase(), null, tableName, "%");
                }
                while (rs.next()) {
                    String colName = rs.getString("COLUMN_NAME");
                    String typeName = rs.getString("TYPE_NAME");
                    int nullable = rs.getInt("NULLABLE");
                    columnTypes.put(colName, typeName);
                    if (nullable == DatabaseMetaData.columnNoNulls) {
                        notNullColumns.add(colName);
                    }
                }
                rs.close();
            }
        } catch (Exception e) {
            log.warn("[数据校验] 无法获取目标表结构，跳过类型校验: {}", e.getMessage());
        }
        
        // 逐行校验
        for (int i = 0; i < dataList.size(); i++) {
            Map<String, Object> row = dataList.get(i);
            boolean rowHasError = false;
            boolean rowHasWarning = false;
            
            for (String header : headers) {
                Object value = row.get(header);
                boolean isEmpty = value == null || value.toString().trim().isEmpty();
                
                // NOT NULL 校验
                if (isEmpty && notNullColumns.contains(header)) {
                    Map<String, Object> err = new HashMap<>();
                    err.put("row", i + 1);
                    err.put("field", header);
                    err.put("message", "必填字段不能为空");
                    err.put("type", "error");
                    errors.add(err);
                    rowHasError = true;
                }
                
                // 类型校验
                if (!isEmpty && columnTypes.containsKey(header)) {
                    String colType = columnTypes.get(header).toUpperCase();
                    String strVal = value.toString().trim();
                    if ((colType.contains("INT") || colType.contains("DECIMAL") || colType.contains("NUMERIC") || colType.contains("FLOAT") || colType.contains("DOUBLE")) 
                        && !strVal.matches("^-?\\d+(\\.\\d+)?$")) {
                        Map<String, Object> err = new HashMap<>();
                        err.put("row", i + 1);
                        err.put("field", header);
                        err.put("message", "值 '" + strVal + "' 与目标列类型 " + colType + " 不匹配");
                        err.put("type", "warning");
                        errors.add(err);
                        rowHasWarning = true;
                    }
                }
            }
            
            if (rowHasError) failedRows++;
            else if (rowHasWarning) warningRows++;
            else passedRows++;
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("totalRows", totalRows);
        result.put("passedRows", passedRows);
        result.put("failedRows", failedRows);
        result.put("warningRows", warningRows);
        result.put("errors", errors);
        return result;
    }
    
    /**
     * 获取某字段的最大值（用于增量导入起始值）
     */
    public Map<String, Object> getFieldMaxValue(Long dataSourceId, String tableName, String fieldName) {
        DataSource dataSource = dataSourceMapper.selectById(dataSourceId);
        if (dataSource == null) {
            throw new BusinessException(ErrorCode.DATA_SOURCE_NOT_FOUND, "数据源不存在");
        }
        
        String url = dataSourceService.buildJdbcUrl(dataSource);
        String driver = dbConnectionUtil.getDriverClassName(dataSource.getDbType());
        
        try {
            Class.forName(driver);
            try (Connection conn = connectionPoolManager.getConnection(dataSource)) {
                String normalizedTable = normalizeTableName(tableName);
                String normalizedField = normalizeColumnName(fieldName, 0);
                String sql = "SELECT MAX(`" + normalizedField + "`) AS max_val FROM `" + normalizedTable + "`";
                try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                    Map<String, Object> result = new HashMap<>();
                    if (rs.next()) {
                        result.put("maxValue", rs.getObject("max_val"));
                    } else {
                        result.put("maxValue", null);
                    }
                    return result;
                }
            }
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.ERROR, "获取字段最大值失败: " + e.getMessage());
        }
    }

    /**
     * 增量导入过滤：查询目标表已有的去重字段值，过滤掉重复的行
     */
    private List<Map<String, Object>> filterIncrementalData(DataSource dataSource, String tableName,
                                                            String deduplicateField, List<String> headers,
                                                            List<Map<String, Object>> dataList) {
        Set<String> existingValues = new HashSet<>();
        try {
            String url = dataSourceService.buildJdbcUrl(dataSource);
            String driver = dbConnectionUtil.getDriverClassName(dataSource.getDbType());
            Class.forName(driver);
            try (Connection conn = connectionPoolManager.getConnection(dataSource)) {
                String normalizedTable = normalizeTableName(tableName);
                String normalizedField = normalizeColumnName(deduplicateField, 0);
                String sql = "SELECT DISTINCT `" + normalizedField + "` FROM `" + normalizedTable + "`";
                try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                    while (rs.next()) {
                        Object val = rs.getObject(1);
                        if (val != null) existingValues.add(val.toString());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[增量导入] 查询目标表已有数据失败，将导入全部数据: {}", e.getMessage());
            return dataList;
        }

        if (existingValues.isEmpty()) return dataList;

        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> row : dataList) {
            Object val = row.get(deduplicateField);
            if (val == null || !existingValues.contains(val.toString())) {
                filtered.add(row);
            }
        }
        log.info("[增量导入] 原始 {} 行，去重后 {} 行", dataList.size(), filtered.size());
        return filtered;
    }

    /**
     * 条件导入过滤：根据筛选条件在内存中过滤数据行
     */
    private List<Map<String, Object>> filterConditionalData(String filterConditionsJson, List<Map<String, Object>> dataList) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            List<Map<String, String>> conditions = mapper.readValue(filterConditionsJson,
                new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, String>>>() {});

            if (conditions == null || conditions.isEmpty()) return dataList;

            List<Map<String, Object>> filtered = new ArrayList<>();
            for (Map<String, Object> row : dataList) {
                boolean match = true;
                for (Map<String, String> cond : conditions) {
                    String field = cond.get("field");
                    String operator = cond.get("operator");
                    String value = cond.get("value");
                    if (field == null || operator == null) continue;

                    Object cellVal = row.get(field);
                    String strVal = cellVal != null ? cellVal.toString() : "";

                    switch (operator) {
                        case "=": match = strVal.equals(value); break;
                        case "!=": match = !strVal.equals(value); break;
                        case "LIKE": match = strVal.contains(value != null ? value : ""); break;
                        case ">": match = compareNumeric(strVal, value) > 0; break;
                        case "<": match = compareNumeric(strVal, value) < 0; break;
                        case ">=": match = compareNumeric(strVal, value) >= 0; break;
                        case "<=": match = compareNumeric(strVal, value) <= 0; break;
                        case "IS NULL": match = strVal.isEmpty(); break;
                        case "IS NOT NULL": match = !strVal.isEmpty(); break;
                        default: break;
                    }
                    if (!match) break;
                }
                if (match) filtered.add(row);
            }
            log.info("[条件导入] 原始 {} 行，筛选后 {} 行", dataList.size(), filtered.size());
            return filtered;
        } catch (Exception e) {
            log.warn("[条件导入] 解析筛选条件失败，将导入全部数据: {}", e.getMessage());
            return dataList;
        }
    }

    private int compareNumeric(String a, String b) {
        try {
            return Double.compare(Double.parseDouble(a), Double.parseDouble(b != null ? b : "0"));
        } catch (NumberFormatException e) {
            return a.compareTo(b != null ? b : "");
        }
    }

    /**
     * 数据库到数据库导入
     * 从源数据源的表导入数据到目标数据源的表
     */
    public Map<String, Object> importFromDatabase(Long sourceDataSourceId, String sourceTable,
                                                   Long targetDataSourceId, String targetTable,
                                                   boolean autoCreateTable, boolean truncateFirst,
                                                   String whereClause) {
        log.info("[DataImport] 数据库导入开始");
        log.debug("[DataImport] 源数据源ID: {}, 源表: {}", sourceDataSourceId, sourceTable);
        log.debug("[DataImport] 目标数据源ID: {}, 目标表: {}", targetDataSourceId, targetTable);
        
        if (sourceDataSourceId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "源数据源ID不能为空");
        }
        if (!StringUtils.hasText(sourceTable)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "源表名不能为空");
        }
        if (targetDataSourceId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "目标数据源ID不能为空");
        }
        
        // 如果目标表名为空，使用源表名
        String finalTargetTable = StringUtils.hasText(targetTable) ? targetTable : sourceTable;
        
        DataSource sourceDs = dataSourceMapper.selectById(sourceDataSourceId);
        if (sourceDs == null) {
            throw new BusinessException(ErrorCode.DATA_SOURCE_NOT_FOUND, "源数据源不存在");
        }
        
        DataSource targetDs = dataSourceMapper.selectById(targetDataSourceId);
        if (targetDs == null) {
            throw new BusinessException(ErrorCode.DATA_SOURCE_NOT_FOUND, "目标数据源不存在");
        }
        
        try {
            // 1. 从源数据源读取数据
            String sourceUrl = dataSourceService.buildJdbcUrl(sourceDs);
            String sourceDriver = dbConnectionUtil.getDriverClassName(sourceDs.getDbType());
            Class.forName(sourceDriver);
            
            List<String> headers = new ArrayList<>();
            List<Map<String, Object>> dataList = new ArrayList<>();
            
            try (Connection sourceConn = connectionPoolManager.getConnection(sourceDs)) {
                // 规范化源表名
                String normalizedSourceTable = normalizeTableName(sourceTable);
                log.debug("[DataImport] 原始源表名: [{}], 规范化源表名: [{}]", sourceTable, normalizedSourceTable);
                
                // 构建查询SQL
                StringBuilder sqlBuilder = new StringBuilder();
                sqlBuilder.append("SELECT * FROM `").append(normalizedSourceTable).append("`");
                
                if (StringUtils.hasText(whereClause)) {
                    // 确保 WHERE 子句前有空格，并清理输入
                    String trimmedWhere = whereClause.trim();
                    log.debug("[DataImport] WHERE条件: [{}]", trimmedWhere);
                    sqlBuilder.append(" WHERE ").append(trimmedWhere);
                }
                
                String querySql = sqlBuilder.toString();
                log.debug("[DataImport] 最终查询SQL: [{}]", querySql);
                
                try (Statement stmt = sourceConn.createStatement();
                     ResultSet rs = stmt.executeQuery(querySql)) {
                    
                    // 获取列信息
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    for (int i = 1; i <= columnCount; i++) {
                        headers.add(metaData.getColumnName(i));
                    }
                    
                    log.debug("[DataImport] 列数: {}, 列名: {}", columnCount, headers);
                    
                    // 读取数据
                    while (rs.next()) {
                        Map<String, Object> row = new HashMap<>();
                        for (int i = 1; i <= columnCount; i++) {
                            row.put(headers.get(i - 1), rs.getObject(i));
                        }
                        dataList.add(row);
                    }
                }
            }
            
            log.info("[DataImport] 从源表读取数据行数: {}", dataList.size());
            
            if (dataList.isEmpty()) {
                Map<String, Object> result = new HashMap<>();
                result.put("successCount", 0);
                result.put("failCount", 0);
                result.put("totalCount", 0);
                result.put("tableName", finalTargetTable);
                result.put("tableCreated", false);
                result.put("tableTruncated", false);
                result.put("message", "源表无数据");
                return result;
            }
            
            // 2. 导入到目标数据源
            return importToDatabase(targetDs, finalTargetTable, headers, dataList, autoCreateTable, truncateFirst);
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[DataImport] 数据库导入失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.ERROR, "数据库导入失败: " + e.getMessage());
        }
    }
}

