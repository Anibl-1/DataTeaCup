package com.dataplatform.data.service;

import com.dataplatform.data.dto.export.*;
import com.dataplatform.data.entity.MaskingRuleEntity;
import com.dataplatform.data.service.export.MiniChartImageGenerator;
import com.dataplatform.data.service.export.WatermarkGenerator;
import com.dataplatform.data.service.masking.MaskingEngine;
import com.dataplatform.data.service.masking.MaskingRule;
import com.dataplatform.data.service.masking.MaskingRuleService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 图表数据导出服务
 * 支持导出为Excel、CSV格式
 * 集成数据脱敏功能，确保导出数据应用脱敏规则
 * 支持导出样式保留（条件格式化、迷你图表转静态图片）
 * 支持打印设置和水印功能
 * 
 * **Validates: Requirements 6.2, 23.1, 23.2, 23.3, 23.4, 23.5, 23.6**
 * - 6.2: 导出数据应用脱敏规则
 * - 23.1: 导出时保留所有条件格式化样式
 * - 23.2: 导出时保留迷你图表（转换为静态图片）
 * - 23.3: 支持配置打印页面设置
 * - 23.4: 支持配置打印表头表尾
 * - 23.5: 支持配置分页规则
 * - 23.6: 支持水印配置
 * 
 * @author dataplatform
 */
@Slf4j
@Service
public class ChartDataExportService {
    
    // Excel单Sheet最大行数
    private static final int MAX_ROWS_PER_SHEET = 1000000;
    // 内存中保留的行数
    private static final int FLUSH_ROWS = 500;
    
    private final MaskingEngine maskingEngine;
    private final MaskingRuleService maskingRuleService;
    private final MiniChartImageGenerator miniChartImageGenerator;
    private final WatermarkGenerator watermarkGenerator;
    private final LicenseLimitService licenseLimitService;
    
    @Autowired
    public ChartDataExportService(MaskingEngine maskingEngine, 
                                   MaskingRuleService maskingRuleService,
                                   MiniChartImageGenerator miniChartImageGenerator,
                                   WatermarkGenerator watermarkGenerator,
                                   LicenseLimitService licenseLimitService) {
        this.maskingEngine = maskingEngine;
        this.maskingRuleService = maskingRuleService;
        this.miniChartImageGenerator = miniChartImageGenerator;
        this.watermarkGenerator = watermarkGenerator;
        this.licenseLimitService = licenseLimitService;
    }
    
    /**
     * 导出为Excel格式（支持大数据量分Sheet）
     */
    public byte[] exportToExcel(List<Map<String, Object>> data, String sheetName) throws IOException {
        // 无脱敏规则时直接导出
        return exportToExcelWithMasking(data, sheetName, null, null, null);
    }
    
    /**
     * 导出为Excel格式（带脱敏）
     * 根据用户角色应用脱敏规则后导出
     * 
     * **Validates: Requirements 6.2**
     * 
     * @param data 原始数据
     * @param sheetName Sheet名称
     * @param userId 用户ID
     * @param roleIds 用户角色ID列表
     * @param dataSourceId 数据源ID（用于获取脱敏规则）
     * @return Excel文件字节数组
     * @throws IOException IO异常
     */
    public byte[] exportToExcelWithMasking(List<Map<String, Object>> data, String sheetName,
                                            Long userId, List<Long> roleIds, Long dataSourceId) throws IOException {
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("没有数据可导出");
        }
        
        // 应用脱敏规则
        List<Map<String, Object>> maskedData = applyMaskingRules(data, userId, roleIds, dataSourceId);
        
        int totalRows = maskedData.size();
        assertExportRowsAllowed(totalRows);
        log.info("开始导出Excel，数据量: {} 行，用户ID: {}, 角色: {}", totalRows, userId, roleIds);
        
        // 使用SXSSFWorkbook支持流式写入，避免内存溢出
        SXSSFWorkbook workbook = new SXSSFWorkbook(FLUSH_ROWS);
        workbook.setCompressTempFiles(true);
        
        try {
            // 获取所有列名
            List<String> headers = maskedData.get(0).keySet().stream().toList();
            
            // 创建标题行样式
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            
            // 创建数字样式
            CellStyle numberStyle = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            numberStyle.setDataFormat(format.getFormat("#,##0.00"));
            
            int sheetIndex = 1;
            int rowNumInSheet = 0;
            int processedRows = 0;
            Sheet currentSheet = null;
            
            for (Map<String, Object> rowData : maskedData) {
                // 创建新Sheet（首次或达到行数上限）
                if (currentSheet == null || rowNumInSheet >= MAX_ROWS_PER_SHEET) {
                    if (currentSheet != null) {
                        try {
                            ((SXSSFSheet) currentSheet).flushRows();
                        } catch (Exception e) {
                            // 忽略
                        }
                    }
                    
                    String currentSheetName = totalRows > MAX_ROWS_PER_SHEET ? 
                        (sheetName != null ? sheetName : "数据") + "_" + sheetIndex++ : 
                        (sheetName != null ? sheetName : "Sheet1");
                    currentSheet = workbook.createSheet(currentSheetName);
                    rowNumInSheet = 0;
                    
                    // 写入表头
                    Row headerRow = currentSheet.createRow(rowNumInSheet++);
                    for (int i = 0; i < headers.size(); i++) {
                        Cell cell = headerRow.createCell(i);
                        cell.setCellValue(headers.get(i));
                        cell.setCellStyle(headerStyle);
                    }
                    
                    // 冻结首行
                    currentSheet.createFreezePane(0, 1);
                    
                    log.info("创建Sheet: {}", currentSheetName);
                }
                
                // 写入数据行
                Row row = currentSheet.createRow(rowNumInSheet++);
                for (int colIdx = 0; colIdx < headers.size(); colIdx++) {
                    Cell cell = row.createCell(colIdx);
                    Object value = rowData.get(headers.get(colIdx));
                    
                    if (value == null) {
                        cell.setCellValue("");
                    } else if (value instanceof Number) {
                        cell.setCellValue(((Number) value).doubleValue());
                        cell.setCellStyle(numberStyle);
                    } else if (value instanceof Boolean) {
                        cell.setCellValue((Boolean) value);
                    } else {
                        String strValue = value.toString();
                        // Excel单元格最大32767字符
                        if (strValue.length() > 32767) {
                            strValue = strValue.substring(0, 32760) + "...";
                        }
                        cell.setCellValue(strValue);
                    }
                }
                
                processedRows++;
                
                // 定期刷新，释放内存
                if (rowNumInSheet % FLUSH_ROWS == 0) {
                    try {
                        ((SXSSFSheet) currentSheet).flushRows();
                    } catch (Exception e) {
                        // 忽略
                    }
                }
                
                // 每10万行记录日志
                if (processedRows % 100000 == 0) {
                    log.info("Excel导出进度: {}/{} 行", processedRows, totalRows);
                }
            }
            
            log.info("Excel导出完成，总计: {} 行，{} 个Sheet", processedRows, sheetIndex - 1 > 0 ? sheetIndex - 1 : 1);
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } finally {
            workbook.dispose();
            workbook.close();
        }
    }
    
    /**
     * 导出为CSV格式（带脱敏）
     * 根据用户角色应用脱敏规则后导出
     * 
     * **Validates: Requirements 6.2**
     * 
     * @param data 原始数据
     * @param separator 分隔符
     * @param userId 用户ID
     * @param roleIds 用户角色ID列表
     * @param dataSourceId 数据源ID
     * @return CSV文件字节数组
     * @throws IOException IO异常
     */
    public byte[] exportToCsvWithMasking(List<Map<String, Object>> data, String separator,
                                          Long userId, List<Long> roleIds, Long dataSourceId) throws IOException {
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("没有数据可导出");
        }
        
        List<Map<String, Object>> maskedData = applyMaskingRules(data, userId, roleIds, dataSourceId);
        assertExportRowsAllowed(maskedData.size());
        return exportToCsvInternal(maskedData, separator);
    }
    
    /**
     * 导出为CSV格式
     */
    public byte[] exportToCsv(List<Map<String, Object>> data, String separator) throws IOException {
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("没有数据可导出");
        }
        assertExportRowsAllowed(data.size());
        return exportToCsvInternal(data, separator);
    }
    
    /**
     * 导出为JSON格式（带脱敏）
     * 根据用户角色应用脱敏规则后导出
     * 
     * **Validates: Requirements 6.2**
     * 
     * @param data 原始数据
     * @param userId 用户ID
     * @param roleIds 用户角色ID列表
     * @param dataSourceId 数据源ID
     * @return JSON字符串
     */
    public String exportToJsonWithMasking(List<Map<String, Object>> data,
                                           Long userId, List<Long> roleIds, Long dataSourceId) {
        if (data == null || data.isEmpty()) {
            return "[]";
        }
        
        List<Map<String, Object>> maskedData = applyMaskingRules(data, userId, roleIds, dataSourceId);
        assertExportRowsAllowed(maskedData.size());
        return exportToJsonInternal(maskedData);
    }
    
    /**
     * 导出为JSON格式
     */
    public String exportToJson(List<Map<String, Object>> data) {
        if (data == null || data.isEmpty()) {
            return "[]";
        }
        assertExportRowsAllowed(data.size());
        return exportToJsonInternal(data);
    }
    
    // ==================== 流式导出方法 ====================
    
    /**
     * 流式数据提供者接口
     * 用于分批获取大数据量，避免一次性加载到内存
     * 
     * **Validates: Requirements 17.1**
     */
    @FunctionalInterface
    public interface StreamingDataProvider {
        /**
         * 获取下一批数据
         * @param offset 起始偏移量
         * @param batchSize 批次大小
         * @return 数据批次，返回空列表表示没有更多数据
         */
        List<Map<String, Object>> fetchBatch(long offset, int batchSize);
    }
    
    /**
     * 流式导出进度回调接口
     */
    @FunctionalInterface
    public interface ExportProgressCallback {
        /**
         * 报告导出进度
         * @param processedRows 已处理行数
         * @param totalRows 总行数（如果已知，否则为-1）
         */
        void onProgress(long processedRows, long totalRows);
    }
    
    /**
     * 流式导出Excel到输出流
     * 使用分批获取数据的方式，避免一次性加载全部数据到内存
     * 
     * **Validates: Requirements 17.1**
     * - 当导出数据量超过10000行时，使用流式写入方式分批处理
     * 
     * @param outputStream 输出流
     * @param dataProvider 数据提供者，用于分批获取数据
     * @param headers 列头列表
     * @param sheetName Sheet名称
     * @param batchSize 每批获取的数据量，默认1000
     * @param totalRows 总行数（如果已知，用于进度计算，-1表示未知）
     * @param progressCallback 进度回调（可为null）
     * @throws IOException IO异常
     */
    public void exportToExcelStream(OutputStream outputStream, 
                                     StreamingDataProvider dataProvider,
                                     List<String> headers,
                                     String sheetName,
                                     int batchSize,
                                     long totalRows,
                                     ExportProgressCallback progressCallback) throws IOException {
        exportToExcelStreamWithMasking(outputStream, dataProvider, headers, sheetName, 
                batchSize, totalRows, progressCallback, null, null, null);
    }
    
    /**
     * 流式导出Excel到输出流（带脱敏）
     * 使用分批获取数据的方式，避免一次性加载全部数据到内存
     * 
     * **Validates: Requirements 17.1, 6.2**
     * - 17.1: 当导出数据量超过10000行时，使用流式写入方式分批处理
     * - 6.2: 导出数据应用脱敏规则
     * 
     * @param outputStream 输出流
     * @param dataProvider 数据提供者，用于分批获取数据
     * @param headers 列头列表
     * @param sheetName Sheet名称
     * @param batchSize 每批获取的数据量，默认1000
     * @param totalRows 总行数（如果已知，用于进度计算，-1表示未知）
     * @param progressCallback 进度回调（可为null）
     * @param userId 用户ID
     * @param roleIds 用户角色ID列表
     * @param dataSourceId 数据源ID
     * @throws IOException IO异常
     */
    public void exportToExcelStreamWithMasking(OutputStream outputStream, 
                                                StreamingDataProvider dataProvider,
                                                List<String> headers,
                                                String sheetName,
                                                int batchSize,
                                                long totalRows,
                                                ExportProgressCallback progressCallback,
                                                Long userId, 
                                                List<Long> roleIds, 
                                                Long dataSourceId) throws IOException {
        if (dataProvider == null) {
            throw new IllegalArgumentException("数据提供者不能为空");
        }
        if (headers == null || headers.isEmpty()) {
            throw new IllegalArgumentException("列头不能为空");
        }
        if (batchSize <= 0) {
            batchSize = 1000; // 默认批次大小
        }
        if (totalRows >= 0) {
            assertExportRowsAllowed(totalRows);
        }
        
        log.info("开始流式导出Excel，批次大小: {}，预计总行数: {}", batchSize, totalRows);
        
        // 使用SXSSFWorkbook支持流式写入，避免内存溢出
        SXSSFWorkbook workbook = new SXSSFWorkbook(FLUSH_ROWS);
        workbook.setCompressTempFiles(true);
        
        try {
            // 创建标题行样式
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            
            // 创建数字样式
            CellStyle numberStyle = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            numberStyle.setDataFormat(format.getFormat("#,##0.00"));
            
            int sheetIndex = 1;
            int rowNumInSheet = 0;
            long processedRows = 0;
            long offset = 0;
            Sheet currentSheet = null;
            
            // 获取脱敏规则（如果需要）
            List<MaskingRule> maskingRules = getMaskingRulesForExport(userId, roleIds, dataSourceId);
            
            // 分批获取并处理数据
            while (true) {
                List<Map<String, Object>> batch = dataProvider.fetchBatch(offset, batchSize);
                
                if (batch == null || batch.isEmpty()) {
                    log.info("流式导出：没有更多数据，结束导出");
                    break;
                }
                
                // 应用脱敏规则
                if (maskingRules != null && !maskingRules.isEmpty() && userId != null) {
                    List<Long> effectiveRoleIds = roleIds != null ? roleIds : Collections.emptyList();
                    batch = maskingEngine.maskDataByRole(batch, maskingRules, userId, effectiveRoleIds);
                }
                
                // 处理当前批次数据
                for (Map<String, Object> rowData : batch) {
                    // 创建新Sheet（首次或达到行数上限）
                    if (currentSheet == null || rowNumInSheet >= MAX_ROWS_PER_SHEET) {
                        if (currentSheet != null) {
                            flushSheet(currentSheet);
                        }
                        
                        String currentSheetName = (totalRows > MAX_ROWS_PER_SHEET || totalRows < 0) ? 
                            (sheetName != null ? sheetName : "数据") + "_" + sheetIndex++ : 
                            (sheetName != null ? sheetName : "Sheet1");
                        currentSheet = workbook.createSheet(currentSheetName);
                        rowNumInSheet = 0;
                        
                        // 写入表头
                        Row headerRow = currentSheet.createRow(rowNumInSheet++);
                        for (int i = 0; i < headers.size(); i++) {
                            Cell cell = headerRow.createCell(i);
                            cell.setCellValue(headers.get(i));
                            cell.setCellStyle(headerStyle);
                        }
                        
                        // 冻结首行
                        currentSheet.createFreezePane(0, 1);
                        
                        log.info("流式导出：创建Sheet: {}", currentSheetName);
                    }
                    
                    // 写入数据行
                    Row row = currentSheet.createRow(rowNumInSheet++);
                    for (int colIdx = 0; colIdx < headers.size(); colIdx++) {
                        Cell cell = row.createCell(colIdx);
                        Object value = rowData.get(headers.get(colIdx));
                        
                        if (value == null) {
                            cell.setCellValue("");
                        } else if (value instanceof Number) {
                            cell.setCellValue(((Number) value).doubleValue());
                            cell.setCellStyle(numberStyle);
                        } else if (value instanceof Boolean) {
                            cell.setCellValue((Boolean) value);
                        } else {
                            String strValue = value.toString();
                            // Excel单元格最大32767字符
                            if (strValue.length() > 32767) {
                                strValue = strValue.substring(0, 32760) + "...";
                            }
                            cell.setCellValue(strValue);
                        }
                    }
                    
                    processedRows++;
                    assertExportRowsAllowed(processedRows);
                    
                    // 定期刷新，释放内存
                    if (rowNumInSheet % FLUSH_ROWS == 0) {
                        flushSheet(currentSheet);
                    }
                }
                
                offset += batch.size();
                
                // 报告进度
                if (progressCallback != null) {
                    progressCallback.onProgress(processedRows, totalRows);
                }
                
                // 每10万行记录日志
                if (processedRows % 100000 == 0) {
                    log.info("流式导出进度: {} 行已处理", processedRows);
                }
            }
            
            log.info("流式导出Excel完成，总计: {} 行，{} 个Sheet", 
                    processedRows, sheetIndex - 1 > 0 ? sheetIndex - 1 : 1);
            
            workbook.write(outputStream);
            outputStream.flush();
        } finally {
            workbook.dispose();
            workbook.close();
        }
    }
    
    /**
     * 流式导出CSV到输出流
     * 使用分批获取数据的方式，避免一次性加载全部数据到内存
     * 
     * **Validates: Requirements 17.1**
     * - 当导出数据量超过10000行时，使用流式写入方式分批处理
     * 
     * @param outputStream 输出流
     * @param dataProvider 数据提供者，用于分批获取数据
     * @param headers 列头列表
     * @param separator 分隔符
     * @param batchSize 每批获取的数据量
     * @param totalRows 总行数（如果已知，用于进度计算，-1表示未知）
     * @param progressCallback 进度回调（可为null）
     * @throws IOException IO异常
     */
    public void exportToCsvStream(OutputStream outputStream,
                                   StreamingDataProvider dataProvider,
                                   List<String> headers,
                                   String separator,
                                   int batchSize,
                                   long totalRows,
                                   ExportProgressCallback progressCallback) throws IOException {
        exportToCsvStreamWithMasking(outputStream, dataProvider, headers, separator, 
                batchSize, totalRows, progressCallback, null, null, null);
    }
    
    /**
     * 流式导出CSV到输出流（带脱敏）
     * 使用分批获取数据的方式，避免一次性加载全部数据到内存
     * 
     * **Validates: Requirements 17.1, 6.2**
     * - 17.1: 当导出数据量超过10000行时，使用流式写入方式分批处理
     * - 6.2: 导出数据应用脱敏规则
     * 
     * @param outputStream 输出流
     * @param dataProvider 数据提供者，用于分批获取数据
     * @param headers 列头列表
     * @param separator 分隔符
     * @param batchSize 每批获取的数据量
     * @param totalRows 总行数（如果已知，用于进度计算，-1表示未知）
     * @param progressCallback 进度回调（可为null）
     * @param userId 用户ID
     * @param roleIds 用户角色ID列表
     * @param dataSourceId 数据源ID
     * @throws IOException IO异常
     */
    public void exportToCsvStreamWithMasking(OutputStream outputStream,
                                              StreamingDataProvider dataProvider,
                                              List<String> headers,
                                              String separator,
                                              int batchSize,
                                              long totalRows,
                                              ExportProgressCallback progressCallback,
                                              Long userId,
                                              List<Long> roleIds,
                                              Long dataSourceId) throws IOException {
        if (dataProvider == null) {
            throw new IllegalArgumentException("数据提供者不能为空");
        }
        if (headers == null || headers.isEmpty()) {
            throw new IllegalArgumentException("列头不能为空");
        }
        if (separator == null) {
            separator = ",";
        }
        if (batchSize <= 0) {
            batchSize = 1000;
        }
        if (totalRows >= 0) {
            assertExportRowsAllowed(totalRows);
        }
        
        log.info("开始流式导出CSV，批次大小: {}，预计总行数: {}", batchSize, totalRows);
        
        // 使用Writer进行流式写入
        Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
        
        try {
            // 写入BOM以支持中文
            writer.write('\uFEFF');
            
            // 写入标题行
            writer.write(String.join(separator, headers));
            writer.write("\n");
            
            // 获取脱敏规则（如果需要）
            List<MaskingRule> maskingRules = getMaskingRulesForExport(userId, roleIds, dataSourceId);
            
            long processedRows = 0;
            long offset = 0;
            final String sep = separator;
            
            // 分批获取并处理数据
            while (true) {
                List<Map<String, Object>> batch = dataProvider.fetchBatch(offset, batchSize);
                
                if (batch == null || batch.isEmpty()) {
                    log.info("流式导出CSV：没有更多数据，结束导出");
                    break;
                }
                
                // 应用脱敏规则
                if (maskingRules != null && !maskingRules.isEmpty() && userId != null) {
                    List<Long> effectiveRoleIds = roleIds != null ? roleIds : Collections.emptyList();
                    batch = maskingEngine.maskDataByRole(batch, maskingRules, userId, effectiveRoleIds);
                }
                
                // 处理当前批次数据
                for (Map<String, Object> rowData : batch) {
                    StringBuilder rowSb = new StringBuilder();
                    for (int i = 0; i < headers.size(); i++) {
                        if (i > 0) {
                            rowSb.append(sep);
                        }
                        Object value = rowData.get(headers.get(i));
                        String strValue = value != null ? value.toString() : "";
                        
                        // 处理包含分隔符或换行的值
                        if (strValue.contains(sep) || strValue.contains("\n") || strValue.contains("\"")) {
                            strValue = "\"" + strValue.replace("\"", "\"\"") + "\"";
                        }
                        rowSb.append(strValue);
                    }
                    rowSb.append("\n");
                    writer.write(rowSb.toString());
                    
                    processedRows++;
                    assertExportRowsAllowed(processedRows);
                }
                
                offset += batch.size();
                
                // 定期刷新
                if (processedRows % 10000 == 0) {
                    writer.flush();
                }
                
                // 报告进度
                if (progressCallback != null) {
                    progressCallback.onProgress(processedRows, totalRows);
                }
                
                // 每10万行记录日志
                if (processedRows % 100000 == 0) {
                    log.info("流式导出CSV进度: {} 行已处理", processedRows);
                }
            }
            
            writer.flush();
            log.info("流式导出CSV完成，总计: {} 行", processedRows);
        } finally {
            // 注意：不关闭writer，因为它会关闭底层的outputStream
            // 调用者负责关闭outputStream
        }
    }
    
    /**
     * 流式导出JSON到输出流
     * 使用分批获取数据的方式，避免一次性加载全部数据到内存
     * 
     * **Validates: Requirements 17.1**
     * - 当导出数据量超过10000行时，使用流式写入方式分批处理
     * 
     * @param outputStream 输出流
     * @param dataProvider 数据提供者，用于分批获取数据
     * @param batchSize 每批获取的数据量
     * @param totalRows 总行数（如果已知，用于进度计算，-1表示未知）
     * @param progressCallback 进度回调（可为null）
     * @throws IOException IO异常
     */
    public void exportToJsonStream(OutputStream outputStream,
                                    StreamingDataProvider dataProvider,
                                    int batchSize,
                                    long totalRows,
                                    ExportProgressCallback progressCallback) throws IOException {
        exportToJsonStreamWithMasking(outputStream, dataProvider, batchSize, totalRows, 
                progressCallback, null, null, null);
    }
    
    /**
     * 流式导出JSON到输出流（带脱敏）
     * 使用分批获取数据的方式，避免一次性加载全部数据到内存
     * 
     * **Validates: Requirements 17.1, 6.2**
     * - 17.1: 当导出数据量超过10000行时，使用流式写入方式分批处理
     * - 6.2: 导出数据应用脱敏规则
     * 
     * @param outputStream 输出流
     * @param dataProvider 数据提供者，用于分批获取数据
     * @param batchSize 每批获取的数据量
     * @param totalRows 总行数（如果已知，用于进度计算，-1表示未知）
     * @param progressCallback 进度回调（可为null）
     * @param userId 用户ID
     * @param roleIds 用户角色ID列表
     * @param dataSourceId 数据源ID
     * @throws IOException IO异常
     */
    public void exportToJsonStreamWithMasking(OutputStream outputStream,
                                               StreamingDataProvider dataProvider,
                                               int batchSize,
                                               long totalRows,
                                               ExportProgressCallback progressCallback,
                                               Long userId,
                                               List<Long> roleIds,
                                               Long dataSourceId) throws IOException {
        if (dataProvider == null) {
            throw new IllegalArgumentException("数据提供者不能为空");
        }
        if (batchSize <= 0) {
            batchSize = 1000;
        }
        if (totalRows >= 0) {
            assertExportRowsAllowed(totalRows);
        }
        
        log.info("开始流式导出JSON，批次大小: {}，预计总行数: {}", batchSize, totalRows);
        
        Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
        
        try {
            writer.write("[\n");
            
            // 获取脱敏规则（如果需要）
            List<MaskingRule> maskingRules = getMaskingRulesForExport(userId, roleIds, dataSourceId);
            
            long processedRows = 0;
            long offset = 0;
            boolean isFirstRow = true;
            
            // 分批获取并处理数据
            while (true) {
                List<Map<String, Object>> batch = dataProvider.fetchBatch(offset, batchSize);
                
                if (batch == null || batch.isEmpty()) {
                    log.info("流式导出JSON：没有更多数据，结束导出");
                    break;
                }
                
                // 应用脱敏规则
                if (maskingRules != null && !maskingRules.isEmpty() && userId != null) {
                    List<Long> effectiveRoleIds = roleIds != null ? roleIds : Collections.emptyList();
                    batch = maskingEngine.maskDataByRole(batch, maskingRules, userId, effectiveRoleIds);
                }
                
                // 处理当前批次数据
                for (Map<String, Object> rowData : batch) {
                    if (!isFirstRow) {
                        writer.write(",\n");
                    }
                    isFirstRow = false;
                    
                    writer.write("  {");
                    boolean isFirstField = true;
                    for (Map.Entry<String, Object> entry : rowData.entrySet()) {
                        if (!isFirstField) {
                            writer.write(", ");
                        }
                        isFirstField = false;
                        
                        writer.write("\"");
                        writer.write(escapeJson(entry.getKey()));
                        writer.write("\": ");
                        
                        Object value = entry.getValue();
                        if (value == null) {
                            writer.write("null");
                        } else if (value instanceof Number) {
                            writer.write(value.toString());
                        } else if (value instanceof Boolean) {
                            writer.write(value.toString());
                        } else {
                            writer.write("\"");
                            writer.write(escapeJson(value.toString()));
                            writer.write("\"");
                        }
                    }
                    writer.write("}");
                    
                    processedRows++;
                    assertExportRowsAllowed(processedRows);
                }
                
                offset += batch.size();
                
                // 定期刷新
                if (processedRows % 10000 == 0) {
                    writer.flush();
                }
                
                // 报告进度
                if (progressCallback != null) {
                    progressCallback.onProgress(processedRows, totalRows);
                }
                
                // 每10万行记录日志
                if (processedRows % 100000 == 0) {
                    log.info("流式导出JSON进度: {} 行已处理", processedRows);
                }
            }
            
            writer.write("\n]");
            writer.flush();
            log.info("流式导出JSON完成，总计: {} 行", processedRows);
        } finally {
            // 注意：不关闭writer，因为它会关闭底层的outputStream
        }
    }
    
    /**
     * 获取导出用的脱敏规则
     */
    private List<MaskingRule> getMaskingRulesForExport(Long userId, List<Long> roleIds, Long dataSourceId) {
        if (userId == null) {
            return null;
        }
        
        try {
            List<MaskingRuleEntity> ruleEntities;
            if (dataSourceId != null) {
                ruleEntities = maskingRuleService.findByDataSource(dataSourceId);
            } else {
                ruleEntities = maskingRuleService.getAllRules();
            }
            
            if (ruleEntities == null || ruleEntities.isEmpty()) {
                return null;
            }
            
            return maskingRuleService.toMaskingRules(ruleEntities);
        } catch (Exception e) {
            log.error("Error getting masking rules for streaming export", e);
            return null;
        }
    }
    
    // ==================== 带样式导出方法 ====================
    
    /**
     * 导出为Excel格式（带样式保留）
     * 保留条件格式化样式和迷你图表
     * 
     * **Validates: Requirements 23.1, 23.2**
     * - 23.1: 导出时保留所有条件格式化样式
     * - 23.2: 导出时保留迷你图表（转换为静态图片）
     * 
     * @param data 原始数据
     * @param sheetName Sheet名称
     * @param styleConfig 样式配置
     * @param userId 用户ID
     * @param roleIds 用户角色ID列表
     * @param dataSourceId 数据源ID
     * @return Excel文件字节数组
     * @throws IOException IO异常
     */
    public byte[] exportToExcelWithStyle(List<Map<String, Object>> data, String sheetName,
                                          ExportStyleConfig styleConfig,
                                          Long userId, List<Long> roleIds, Long dataSourceId) throws IOException {
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("没有数据可导出");
        }
        
        // 应用脱敏规则
        List<Map<String, Object>> maskedData = applyMaskingRules(data, userId, roleIds, dataSourceId);
        
        int totalRows = maskedData.size();
        assertExportRowsAllowed(totalRows);
        log.info("开始导出Excel（带样式），数据量: {} 行，用户ID: {}", totalRows, userId);
        
        // 使用SXSSFWorkbook支持流式写入
        SXSSFWorkbook workbook = new SXSSFWorkbook(FLUSH_ROWS);
        workbook.setCompressTempFiles(true);
        
        try {
            // 获取所有列名
            List<String> headers = new ArrayList<>(maskedData.get(0).keySet());
            
            // 创建样式缓存
            Map<String, CellStyle> styleCache = new HashMap<>();
            
            // 创建表头样式
            CellStyle headerStyle = createHeaderStyle(workbook, styleConfig);
            
            // 创建默认数据样式
            CellStyle defaultStyle = createDefaultDataStyle(workbook);
            
            // 创建数字格式
            DataFormat dataFormat = workbook.createDataFormat();
            
            int sheetIndex = 1;
            int rowNumInSheet = 0;
            int processedRows = 0;
            Sheet currentSheet = null;
            
            for (int dataRowIdx = 0; dataRowIdx < maskedData.size(); dataRowIdx++) {
                Map<String, Object> rowData = maskedData.get(dataRowIdx);
                
                // 创建新Sheet
                if (currentSheet == null || rowNumInSheet >= MAX_ROWS_PER_SHEET) {
                    if (currentSheet != null) {
                        flushSheet(currentSheet);
                    }
                    
                    String currentSheetName = totalRows > MAX_ROWS_PER_SHEET ? 
                        (sheetName != null ? sheetName : "数据") + "_" + sheetIndex++ : 
                        (sheetName != null ? sheetName : "Sheet1");
                    currentSheet = workbook.createSheet(currentSheetName);
                    rowNumInSheet = 0;
                    
                    // 写入表头
                    Row headerRow = currentSheet.createRow(rowNumInSheet++);
                    for (int i = 0; i < headers.size(); i++) {
                        Cell cell = headerRow.createCell(i);
                        cell.setCellValue(headers.get(i));
                        cell.setCellStyle(headerStyle);
                    }
                    
                    // 冻结首行
                    currentSheet.createFreezePane(0, 1);
                    
                    log.info("创建Sheet: {}", currentSheetName);
                }
                
                // 写入数据行
                Row row = currentSheet.createRow(rowNumInSheet++);
                boolean isEvenRow = (dataRowIdx % 2 == 1);
                
                for (int colIdx = 0; colIdx < headers.size(); colIdx++) {
                    String fieldName = headers.get(colIdx);
                    Cell cell = row.createCell(colIdx);
                    Object value = rowData.get(fieldName);
                    
                    // 检查是否有迷你图表配置
                    MiniChartExportConfig miniChartConfig = getMiniChartConfig(styleConfig, fieldName);
                    
                    if (miniChartConfig != null && styleConfig.isConvertMiniChartsToImages()) {
                        // 生成迷你图表图片并插入
                        byte[] imageBytes = miniChartImageGenerator.generateMiniChartImage(value, miniChartConfig);
                        if (imageBytes != null) {
                            insertImageToCell(workbook, currentSheet, row.getRowNum(), colIdx, imageBytes);
                            // 设置单元格为空，图片会覆盖
                            cell.setCellValue("");
                        } else {
                            setCellValue(cell, value, defaultStyle);
                        }
                    } else {
                        // 设置单元格值
                        setCellValue(cell, value, defaultStyle);
                        
                        // 应用样式
                        CellStyle cellStyle = computeCellStyle(workbook, styleConfig, fieldName, 
                                value, rowData, isEvenRow, dataFormat, styleCache);
                        if (cellStyle != null) {
                            cell.setCellStyle(cellStyle);
                        }
                    }
                }
                
                processedRows++;
                
                // 定期刷新
                if (rowNumInSheet % FLUSH_ROWS == 0) {
                    flushSheet(currentSheet);
                }
                
                // 每10万行记录日志
                if (processedRows % 100000 == 0) {
                    log.info("Excel导出进度: {}/{} 行", processedRows, totalRows);
                }
            }
            
            log.info("Excel导出完成（带样式），总计: {} 行", processedRows);
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } finally {
            workbook.dispose();
            workbook.close();
        }
    }
    
    /**
     * 导出为Excel格式（带样式和打印设置）
     * 保留条件格式化样式、迷你图表，并应用打印设置
     * 
     * **Validates: Requirements 23.1, 23.2, 23.3, 23.4, 23.5**
     * - 23.1: 导出时保留所有条件格式化样式
     * - 23.2: 导出时保留迷你图表（转换为静态图片）
     * - 23.3: 支持配置打印页面设置（纸张大小、方向、边距）
     * - 23.4: 支持配置打印表头表尾（每页重复）
     * - 23.5: 支持配置分页规则（按行数、按分组）
     * 
     * @param data 原始数据
     * @param sheetName Sheet名称
     * @param styleConfig 样式配置
     * @param printSettings 打印设置
     * @param userId 用户ID
     * @param roleIds 用户角色ID列表
     * @param dataSourceId 数据源ID
     * @return Excel文件字节数组
     * @throws IOException IO异常
     */
    public byte[] exportToExcelWithStyleAndPrint(List<Map<String, Object>> data, String sheetName,
                                                   ExportStyleConfig styleConfig,
                                                   PrintSettings printSettings,
                                                   Long userId, List<Long> roleIds, Long dataSourceId) throws IOException {
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("没有数据可导出");
        }
        
        // 应用脱敏规则
        List<Map<String, Object>> maskedData = applyMaskingRules(data, userId, roleIds, dataSourceId);
        
        int totalRows = maskedData.size();
        assertExportRowsAllowed(totalRows);
        log.info("开始导出Excel（带样式和打印设置），数据量: {} 行", totalRows);
        
        // 使用SXSSFWorkbook支持流式写入
        SXSSFWorkbook workbook = new SXSSFWorkbook(FLUSH_ROWS);
        workbook.setCompressTempFiles(true);
        
        try {
            // 获取所有列名
            List<String> headers = new ArrayList<>(maskedData.get(0).keySet());
            
            // 创建样式缓存
            Map<String, CellStyle> styleCache = new HashMap<>();
            
            // 创建表头样式
            CellStyle headerStyle = createHeaderStyle(workbook, styleConfig);
            
            // 创建默认数据样式
            CellStyle defaultStyle = createDefaultDataStyle(workbook);
            
            // 创建数字格式
            DataFormat dataFormat = workbook.createDataFormat();
            
            int sheetIndex = 1;
            int rowNumInSheet = 0;
            int processedRows = 0;
            Sheet currentSheet = null;
            
            for (int dataRowIdx = 0; dataRowIdx < maskedData.size(); dataRowIdx++) {
                Map<String, Object> rowData = maskedData.get(dataRowIdx);
                
                // 创建新Sheet
                if (currentSheet == null || rowNumInSheet >= MAX_ROWS_PER_SHEET) {
                    if (currentSheet != null) {
                        flushSheet(currentSheet);
                    }
                    
                    String currentSheetName = totalRows > MAX_ROWS_PER_SHEET ? 
                        (sheetName != null ? sheetName : "数据") + "_" + sheetIndex++ : 
                        (sheetName != null ? sheetName : "Sheet1");
                    currentSheet = workbook.createSheet(currentSheetName);
                    rowNumInSheet = 0;
                    
                    // 应用打印设置
                    if (printSettings != null) {
                        applyPrintSettings(currentSheet, printSettings, headers.size());
                    }
                    
                    // 写入表头
                    Row headerRow = currentSheet.createRow(rowNumInSheet++);
                    for (int i = 0; i < headers.size(); i++) {
                        Cell cell = headerRow.createCell(i);
                        cell.setCellValue(headers.get(i));
                        cell.setCellStyle(headerStyle);
                    }
                    
                    // 冻结首行
                    currentSheet.createFreezePane(0, 1);
                    
                    log.info("创建Sheet: {}", currentSheetName);
                }
                
                // 写入数据行
                Row row = currentSheet.createRow(rowNumInSheet++);
                boolean isEvenRow = (dataRowIdx % 2 == 1);
                
                for (int colIdx = 0; colIdx < headers.size(); colIdx++) {
                    String fieldName = headers.get(colIdx);
                    Cell cell = row.createCell(colIdx);
                    Object value = rowData.get(fieldName);
                    
                    // 检查是否有迷你图表配置
                    MiniChartExportConfig miniChartConfig = getMiniChartConfig(styleConfig, fieldName);
                    
                    if (miniChartConfig != null && styleConfig != null && styleConfig.isConvertMiniChartsToImages()) {
                        // 生成迷你图表图片并插入
                        byte[] imageBytes = miniChartImageGenerator.generateMiniChartImage(value, miniChartConfig);
                        if (imageBytes != null) {
                            insertImageToCell(workbook, currentSheet, row.getRowNum(), colIdx, imageBytes);
                            cell.setCellValue("");
                        } else {
                            setCellValue(cell, value, defaultStyle);
                        }
                    } else {
                        // 设置单元格值
                        setCellValue(cell, value, defaultStyle);
                        
                        // 应用样式
                        CellStyle cellStyle = computeCellStyle(workbook, styleConfig, fieldName, 
                                value, rowData, isEvenRow, dataFormat, styleCache);
                        if (cellStyle != null) {
                            cell.setCellStyle(cellStyle);
                        }
                    }
                }
                
                processedRows++;
                
                // 定期刷新
                if (rowNumInSheet % FLUSH_ROWS == 0) {
                    flushSheet(currentSheet);
                }
            }
            
            log.info("Excel导出完成（带样式和打印设置），总计: {} 行", processedRows);
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } finally {
            workbook.dispose();
            workbook.close();
        }
    }
    
    /**
     * 应用打印设置到Sheet
     * 
     * **Validates: Requirements 23.3, 23.4, 23.5**
     * 
     * @param sheet 工作表
     * @param settings 打印设置
     * @param columnCount 列数
     */
    private void applyPrintSettings(Sheet sheet, PrintSettings settings, int columnCount) {
        PrintSetup printSetup = sheet.getPrintSetup();
        
        // 23.3: 页面设置
        // 纸张大小
        printSetup.setPaperSize(settings.getPaperSize().getPoiValue());
        
        // 页面方向
        if (settings.getOrientation() == PrintSettings.PageOrientation.LANDSCAPE) {
            printSetup.setLandscape(true);
        } else {
            printSetup.setLandscape(false);
        }
        
        // 边距设置
        sheet.setMargin(Sheet.TopMargin, settings.getMarginTop());
        sheet.setMargin(Sheet.BottomMargin, settings.getMarginBottom());
        sheet.setMargin(Sheet.LeftMargin, settings.getMarginLeft());
        sheet.setMargin(Sheet.RightMargin, settings.getMarginRight());
        sheet.setMargin(Sheet.HeaderMargin, settings.getHeaderMargin());
        sheet.setMargin(Sheet.FooterMargin, settings.getFooterMargin());
        
        // 缩放设置
        if (settings.isFitToPageWidth() || settings.isFitToPageHeight()) {
            printSetup.setFitWidth((short) (settings.isFitToPageWidth() ? 1 : 0));
            printSetup.setFitHeight((short) (settings.isFitToPageHeight() ? 1 : 0));
            sheet.setFitToPage(true);
        } else {
            printSetup.setScale((short) settings.getScale());
        }
        
        // 打印选项
        printSetup.setNoColor(settings.isBlackAndWhite());
        printSetup.setDraft(settings.isDraftQuality());
        printSetup.setLeftToRight(settings.getPrintOrder() == PrintSettings.PrintOrder.OVER_THEN_DOWN);
        
        // 23.4: 表头表尾设置
        // 重复表头行
        if (settings.isRepeatHeaderOnEachPage() && settings.getRepeatHeaderRows() > 0) {
            sheet.setRepeatingRows(new org.apache.poi.ss.util.CellRangeAddress(
                    0, settings.getRepeatHeaderRows() - 1, -1, -1));
        }
        
        // 重复左侧列
        if (settings.isRepeatLeftColumns() && settings.getRepeatLeftColumnsCount() > 0) {
            sheet.setRepeatingColumns(new org.apache.poi.ss.util.CellRangeAddress(
                    -1, -1, 0, settings.getRepeatLeftColumnsCount() - 1));
        }
        
        // 页眉页脚
        Header header = sheet.getHeader();
        Footer footer = sheet.getFooter();
        
        if (settings.getHeaderLeft() != null) {
            header.setLeft(settings.getHeaderLeft());
        }
        if (settings.getHeaderCenter() != null) {
            header.setCenter(settings.getHeaderCenter());
        }
        if (settings.getHeaderRight() != null) {
            header.setRight(settings.getHeaderRight());
        }
        
        if (settings.getFooterLeft() != null) {
            footer.setLeft(settings.getFooterLeft());
        }
        if (settings.getFooterCenter() != null) {
            // 替换占位符
            String footerText = settings.getFooterCenter()
                    .replace("{page}", "&P")
                    .replace("{pages}", "&N")
                    .replace("{date}", "&D")
                    .replace("{time}", "&T");
            footer.setCenter(footerText);
        }
        if (settings.getFooterRight() != null) {
            footer.setRight(settings.getFooterRight());
        }
        
        // 打印网格线
        sheet.setPrintGridlines(settings.isPrintGridlines());
        
        // 打印行号列号
        sheet.setPrintRowAndColumnHeadings(settings.isPrintRowColHeadings());
        
        log.debug("Applied print settings: paper={}, orientation={}, margins=[{},{},{},{}]",
                settings.getPaperSize(), settings.getOrientation(),
                settings.getMarginTop(), settings.getMarginBottom(),
                settings.getMarginLeft(), settings.getMarginRight());
    }
    
    /**
     * 导出为Excel格式（完整功能：样式、打印设置、水印）
     * 
     * **Validates: Requirements 23.1, 23.2, 23.3, 23.4, 23.5, 23.6**
     * 
     * @param data 原始数据
     * @param sheetName Sheet名称
     * @param styleConfig 样式配置
     * @param printSettings 打印设置
     * @param watermarkConfig 水印配置
     * @param userId 用户ID
     * @param roleIds 用户角色ID列表
     * @param dataSourceId 数据源ID
     * @return Excel文件字节数组
     * @throws IOException IO异常
     */
    public byte[] exportToExcelFull(List<Map<String, Object>> data, String sheetName,
                                     ExportStyleConfig styleConfig,
                                     PrintSettings printSettings,
                                     WatermarkConfig watermarkConfig,
                                     Long userId, List<Long> roleIds, Long dataSourceId) throws IOException {
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("没有数据可导出");
        }
        
        // 应用脱敏规则
        List<Map<String, Object>> maskedData = applyMaskingRules(data, userId, roleIds, dataSourceId);
        
        int totalRows = maskedData.size();
        assertExportRowsAllowed(totalRows);
        log.info("开始导出Excel（完整功能），数据量: {} 行，水印: {}", 
                totalRows, watermarkConfig != null && watermarkConfig.isEnabled());
        
        // 使用SXSSFWorkbook支持流式写入
        SXSSFWorkbook workbook = new SXSSFWorkbook(FLUSH_ROWS);
        workbook.setCompressTempFiles(true);
        
        try {
            // 获取所有列名
            List<String> headers = new ArrayList<>(maskedData.get(0).keySet());
            
            // 创建样式缓存
            Map<String, CellStyle> styleCache = new HashMap<>();
            
            // 创建表头样式
            CellStyle headerStyle = createHeaderStyle(workbook, styleConfig);
            
            // 创建默认数据样式
            CellStyle defaultStyle = createDefaultDataStyle(workbook);
            
            // 创建数字格式
            DataFormat dataFormat = workbook.createDataFormat();
            
            int sheetIndex = 1;
            int rowNumInSheet = 0;
            int processedRows = 0;
            Sheet currentSheet = null;
            
            for (int dataRowIdx = 0; dataRowIdx < maskedData.size(); dataRowIdx++) {
                Map<String, Object> rowData = maskedData.get(dataRowIdx);
                
                // 创建新Sheet
                if (currentSheet == null || rowNumInSheet >= MAX_ROWS_PER_SHEET) {
                    if (currentSheet != null) {
                        // 为上一个Sheet添加水印
                        if (watermarkConfig != null && watermarkConfig.isEnabled()) {
                            watermarkGenerator.addWatermark(workbook, currentSheet, watermarkConfig);
                        }
                        flushSheet(currentSheet);
                    }
                    
                    String currentSheetName = totalRows > MAX_ROWS_PER_SHEET ? 
                        (sheetName != null ? sheetName : "数据") + "_" + sheetIndex++ : 
                        (sheetName != null ? sheetName : "Sheet1");
                    currentSheet = workbook.createSheet(currentSheetName);
                    rowNumInSheet = 0;
                    
                    // 应用打印设置
                    if (printSettings != null) {
                        applyPrintSettings(currentSheet, printSettings, headers.size());
                    }
                    
                    // 写入表头
                    Row headerRow = currentSheet.createRow(rowNumInSheet++);
                    for (int i = 0; i < headers.size(); i++) {
                        Cell cell = headerRow.createCell(i);
                        cell.setCellValue(headers.get(i));
                        cell.setCellStyle(headerStyle);
                    }
                    
                    // 冻结首行
                    currentSheet.createFreezePane(0, 1);
                    
                    log.info("创建Sheet: {}", currentSheetName);
                }
                
                // 写入数据行
                Row row = currentSheet.createRow(rowNumInSheet++);
                boolean isEvenRow = (dataRowIdx % 2 == 1);
                
                for (int colIdx = 0; colIdx < headers.size(); colIdx++) {
                    String fieldName = headers.get(colIdx);
                    Cell cell = row.createCell(colIdx);
                    Object value = rowData.get(fieldName);
                    
                    // 检查是否有迷你图表配置
                    MiniChartExportConfig miniChartConfig = getMiniChartConfig(styleConfig, fieldName);
                    
                    if (miniChartConfig != null && styleConfig != null && styleConfig.isConvertMiniChartsToImages()) {
                        // 生成迷你图表图片并插入
                        byte[] imageBytes = miniChartImageGenerator.generateMiniChartImage(value, miniChartConfig);
                        if (imageBytes != null) {
                            insertImageToCell(workbook, currentSheet, row.getRowNum(), colIdx, imageBytes);
                            cell.setCellValue("");
                        } else {
                            setCellValue(cell, value, defaultStyle);
                        }
                    } else {
                        // 设置单元格值
                        setCellValue(cell, value, defaultStyle);
                        
                        // 应用样式
                        CellStyle cellStyle = computeCellStyle(workbook, styleConfig, fieldName, 
                                value, rowData, isEvenRow, dataFormat, styleCache);
                        if (cellStyle != null) {
                            cell.setCellStyle(cellStyle);
                        }
                    }
                }
                
                processedRows++;
                
                // 定期刷新
                if (rowNumInSheet % FLUSH_ROWS == 0) {
                    flushSheet(currentSheet);
                }
            }
            
            // 为最后一个Sheet添加水印
            if (currentSheet != null && watermarkConfig != null && watermarkConfig.isEnabled()) {
                watermarkGenerator.addWatermark(workbook, currentSheet, watermarkConfig);
            }
            
            log.info("Excel导出完成（完整功能），总计: {} 行", processedRows);
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } finally {
            workbook.dispose();
            workbook.close();
        }
    }
    
    /**
     * 创建表头样式
     */
    private CellStyle createHeaderStyle(Workbook workbook, ExportStyleConfig styleConfig) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        
        TableExportStyle tableStyle = styleConfig != null ? styleConfig.getTableStyle() : null;
        
        if (tableStyle != null) {
            font.setBold("bold".equalsIgnoreCase(tableStyle.getHeaderFontWeight()));
            font.setFontHeightInPoints((short) tableStyle.getHeaderFontSize());
            
            if (tableStyle.getHeaderFontColor() != null) {
                font.setColor(getColorIndex(tableStyle.getHeaderFontColor()));
            }
            
            if (tableStyle.getHeaderBackgroundColor() != null) {
                style.setFillForegroundColor(getColorIndex(tableStyle.getHeaderBackgroundColor()));
                style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            }
            
            if ("center".equalsIgnoreCase(tableStyle.getHeaderHorizontalAlign())) {
                style.setAlignment(HorizontalAlignment.CENTER);
            }
        } else {
            // 默认表头样式
            font.setBold(true);
            style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        
        style.setFont(font);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        
        // 添加边框
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        
        return style;
    }
    
    /**
     * 创建默认数据样式
     */
    private CellStyle createDefaultDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setTopBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setLeftBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setRightBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        return style;
    }
    
    /**
     * 计算单元格样式
     * 
     * **Validates: Requirements 23.1**
     * - 导出时保留所有条件格式化样式
     */
    private CellStyle computeCellStyle(Workbook workbook, ExportStyleConfig styleConfig,
                                        String fieldName, Object value, Map<String, Object> rowData,
                                        boolean isEvenRow, DataFormat dataFormat,
                                        Map<String, CellStyle> styleCache) {
        if (styleConfig == null) {
            return null;
        }
        
        // 构建样式键用于缓存
        StringBuilder styleKeyBuilder = new StringBuilder();
        styleKeyBuilder.append(fieldName).append("_").append(isEvenRow);
        
        // 检查条件格式化规则
        ConditionalFormatRule matchedRule = null;
        if (styleConfig.isPreserveConditionalFormatting() && styleConfig.getConditionalRules() != null) {
            for (ConditionalFormatRule rule : styleConfig.getConditionalRules()) {
                if (rule.isEnabled() && isRuleApplicable(rule, fieldName) && rule.evaluate(value, rowData)) {
                    matchedRule = rule;
                    styleKeyBuilder.append("_rule_").append(rule.getId());
                    break; // 使用第一个匹配的规则（按优先级排序）
                }
            }
        }
        
        String styleKey = styleKeyBuilder.toString();
        
        // 检查缓存
        if (styleCache.containsKey(styleKey)) {
            return styleCache.get(styleKey);
        }
        
        // 创建新样式
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        
        // 应用列样式
        ColumnExportStyle columnStyle = getColumnStyle(styleConfig, fieldName);
        if (columnStyle != null) {
            applyColumnStyle(style, font, columnStyle, dataFormat);
        }
        
        // 应用斑马纹
        if (styleConfig.isEnableZebraStripes() && isEvenRow) {
            String zebraColor = styleConfig.getZebraStripeColor();
            if (zebraColor != null) {
                style.setFillForegroundColor(getColorIndex(zebraColor));
                style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            } else {
                style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
                style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            }
        }
        
        // 应用条件格式化样式（覆盖基础样式）
        if (matchedRule != null) {
            applyConditionalStyle(style, font, matchedRule);
        }
        
        style.setFont(font);
        
        // 添加边框
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        
        // 缓存样式
        styleCache.put(styleKey, style);
        
        return style;
    }
    
    /**
     * 检查规则是否适用于指定字段
     */
    private boolean isRuleApplicable(ConditionalFormatRule rule, String fieldName) {
        String applyToFields = rule.getApplyToFields();
        if (applyToFields == null || applyToFields.isEmpty()) {
            return true; // 应用到所有字段
        }
        
        String[] fields = applyToFields.split(",");
        for (String field : fields) {
            if (field.trim().equalsIgnoreCase(fieldName)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 获取列样式配置
     */
    private ColumnExportStyle getColumnStyle(ExportStyleConfig styleConfig, String fieldName) {
        if (styleConfig.getColumnStyles() == null) {
            return null;
        }
        return styleConfig.getColumnStyles().get(fieldName);
    }
    
    /**
     * 获取迷你图表配置
     */
    private MiniChartExportConfig getMiniChartConfig(ExportStyleConfig styleConfig, String fieldName) {
        if (styleConfig == null || styleConfig.getMiniChartConfigs() == null) {
            return null;
        }
        return styleConfig.getMiniChartConfigs().get(fieldName);
    }
    
    /**
     * 应用列样式
     */
    private void applyColumnStyle(CellStyle style, Font font, ColumnExportStyle columnStyle, DataFormat dataFormat) {
        // 字体样式
        if (columnStyle.getFontFamily() != null) {
            font.setFontName(columnStyle.getFontFamily());
        }
        if (columnStyle.getFontSize() != null) {
            font.setFontHeightInPoints(columnStyle.getFontSize().shortValue());
        }
        if ("bold".equalsIgnoreCase(columnStyle.getFontWeight())) {
            font.setBold(true);
        }
        if ("italic".equalsIgnoreCase(columnStyle.getFontStyle())) {
            font.setItalic(true);
        }
        if (columnStyle.getFontColor() != null) {
            font.setColor(getColorIndex(columnStyle.getFontColor()));
        }
        if ("underline".equalsIgnoreCase(columnStyle.getTextDecoration())) {
            font.setUnderline(Font.U_SINGLE);
        } else if ("line-through".equalsIgnoreCase(columnStyle.getTextDecoration())) {
            font.setStrikeout(true);
        }
        
        // 对齐方式
        if (columnStyle.getHorizontalAlign() != null) {
            switch (columnStyle.getHorizontalAlign().toLowerCase()) {
                case "left":
                    style.setAlignment(HorizontalAlignment.LEFT);
                    break;
                case "center":
                    style.setAlignment(HorizontalAlignment.CENTER);
                    break;
                case "right":
                    style.setAlignment(HorizontalAlignment.RIGHT);
                    break;
            }
        }
        if (columnStyle.getVerticalAlign() != null) {
            switch (columnStyle.getVerticalAlign().toLowerCase()) {
                case "top":
                    style.setVerticalAlignment(VerticalAlignment.TOP);
                    break;
                case "middle":
                    style.setVerticalAlignment(VerticalAlignment.CENTER);
                    break;
                case "bottom":
                    style.setVerticalAlignment(VerticalAlignment.BOTTOM);
                    break;
            }
        }
        if (Boolean.TRUE.equals(columnStyle.getWrapText())) {
            style.setWrapText(true);
        }
        if (columnStyle.getRotation() != null) {
            style.setRotation(columnStyle.getRotation().shortValue());
        }
        
        // 背景颜色
        if (columnStyle.getBackgroundColor() != null) {
            style.setFillForegroundColor(getColorIndex(columnStyle.getBackgroundColor()));
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        
        // 数据格式
        if (columnStyle.getFormatType() != null) {
            String formatStr = buildFormatString(columnStyle);
            if (formatStr != null) {
                style.setDataFormat(dataFormat.getFormat(formatStr));
            }
        }
    }
    
    /**
     * 应用条件格式化样式
     */
    private void applyConditionalStyle(CellStyle style, Font font, ConditionalFormatRule rule) {
        if (rule.getFontColor() != null) {
            font.setColor(getColorIndex(rule.getFontColor()));
        }
        if ("bold".equalsIgnoreCase(rule.getFontWeight())) {
            font.setBold(true);
        }
        if ("italic".equalsIgnoreCase(rule.getFontStyle())) {
            font.setItalic(true);
        }
        if (rule.getBackgroundColor() != null) {
            style.setFillForegroundColor(getColorIndex(rule.getBackgroundColor()));
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
    }
    
    /**
     * 构建数据格式字符串
     */
    private String buildFormatString(ColumnExportStyle columnStyle) {
        if ("number".equalsIgnoreCase(columnStyle.getFormatType())) {
            StringBuilder format = new StringBuilder();
            
            // 前缀
            if (columnStyle.getPrefix() != null) {
                format.append("\"").append(columnStyle.getPrefix()).append("\"");
            }
            
            // 数字格式
            if (Boolean.TRUE.equals(columnStyle.getUseThousandsSeparator())) {
                format.append("#,##0");
            } else {
                format.append("0");
            }
            
            // 小数位数
            if (columnStyle.getDecimalPlaces() != null && columnStyle.getDecimalPlaces() > 0) {
                format.append(".");
                for (int i = 0; i < columnStyle.getDecimalPlaces(); i++) {
                    format.append("0");
                }
            }
            
            // 后缀
            if (columnStyle.getSuffix() != null) {
                format.append("\"").append(columnStyle.getSuffix()).append("\"");
            }
            
            // 负数格式
            if ("parentheses".equalsIgnoreCase(columnStyle.getNegativeFormat())) {
                String positiveFormat = format.toString();
                format.append(";(").append(positiveFormat).append(")");
            } else if ("red".equalsIgnoreCase(columnStyle.getNegativeFormat())) {
                String positiveFormat = format.toString();
                format.append(";[Red]").append(positiveFormat);
            } else if ("redParentheses".equalsIgnoreCase(columnStyle.getNegativeFormat())) {
                String positiveFormat = format.toString();
                format.append(";[Red](").append(positiveFormat).append(")");
            }
            
            return format.toString();
        } else if ("date".equalsIgnoreCase(columnStyle.getFormatType())) {
            return columnStyle.getDatePattern() != null ? columnStyle.getDatePattern() : "yyyy-mm-dd";
        }
        
        return null;
    }
    
    /**
     * 设置单元格值
     */
    private void setCellValue(Cell cell, Object value, CellStyle defaultStyle) {
        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else if (value instanceof java.util.Date) {
            cell.setCellValue((java.util.Date) value);
        } else {
            String strValue = value.toString();
            // Excel单元格最大32767字符
            if (strValue.length() > 32767) {
                strValue = strValue.substring(0, 32760) + "...";
            }
            cell.setCellValue(strValue);
        }
        
        if (defaultStyle != null) {
            cell.setCellStyle(defaultStyle);
        }
    }
    
    /**
     * 插入图片到单元格
     * 
     * **Validates: Requirements 23.2**
     * - 导出时保留迷你图表（转换为静态图片）
     */
    private void insertImageToCell(Workbook workbook, Sheet sheet, int rowNum, int colNum, byte[] imageBytes) {
        try {
            int pictureIdx = workbook.addPicture(imageBytes, Workbook.PICTURE_TYPE_PNG);
            
            CreationHelper helper = workbook.getCreationHelper();
            Drawing<?> drawing = sheet.createDrawingPatriarch();
            
            ClientAnchor anchor = helper.createClientAnchor();
            anchor.setCol1(colNum);
            anchor.setRow1(rowNum);
            anchor.setCol2(colNum + 1);
            anchor.setRow2(rowNum + 1);
            anchor.setAnchorType(ClientAnchor.AnchorType.MOVE_AND_RESIZE);
            
            drawing.createPicture(anchor, pictureIdx);
            
        } catch (Exception e) {
            log.warn("Failed to insert mini chart image at row {}, col {}: {}", rowNum, colNum, e.getMessage());
        }
    }
    
    /**
     * 刷新Sheet
     */
    private void flushSheet(Sheet sheet) {
        try {
            if (sheet instanceof SXSSFSheet) {
                ((SXSSFSheet) sheet).flushRows();
            }
        } catch (Exception e) {
            // 忽略
        }
    }
    
    /**
     * 获取颜色索引
     */
    private short getColorIndex(String colorStr) {
        if (colorStr == null || colorStr.isEmpty()) {
            return IndexedColors.BLACK.getIndex();
        }
        
        // 尝试匹配预定义颜色
        String upperColor = colorStr.toUpperCase().replace("#", "").replace("-", "_");
        
        // 常用颜色映射
        switch (upperColor) {
            case "FF0000":
            case "RED":
                return IndexedColors.RED.getIndex();
            case "00FF00":
            case "GREEN":
            case "52C41A":
                return IndexedColors.GREEN.getIndex();
            case "0000FF":
            case "BLUE":
            case "1890FF":
                return IndexedColors.BLUE.getIndex();
            case "FFFF00":
            case "YELLOW":
            case "FAAD14":
                return IndexedColors.YELLOW.getIndex();
            case "FFFFFF":
            case "WHITE":
                return IndexedColors.WHITE.getIndex();
            case "000000":
            case "BLACK":
                return IndexedColors.BLACK.getIndex();
            case "808080":
            case "GREY":
            case "GRAY":
                return IndexedColors.GREY_50_PERCENT.getIndex();
            case "FF4D4F":
                return IndexedColors.RED.getIndex();
            case "FAFAFA":
            case "F5F7FA":
            case "F0F0F0":
                return IndexedColors.GREY_25_PERCENT.getIndex();
            default:
                return IndexedColors.AUTOMATIC.getIndex();
        }
    }
    
    // ==================== 私有辅助方法 ====================
    
    /**
     * 应用脱敏规则到数据
     * 获取所有启用的脱敏规则，根据用户角色过滤后应用
     * 
     * @param data 原始数据
     * @param userId 用户ID（可为null）
     * @param roleIds 用户角色ID列表（可为null）
     * @param dataSourceId 数据源ID（可为null）
     * @return 脱敏后的数据
     */
    private List<Map<String, Object>> applyMaskingRules(List<Map<String, Object>> data,
                                                         Long userId, List<Long> roleIds, Long dataSourceId) {
        if (data == null || data.isEmpty()) {
            return data;
        }
        
        // 如果没有用户信息，不应用脱敏
        if (userId == null) {
            log.debug("No userId provided, skipping masking for export");
            return data;
        }
        
        try {
            // 获取适用的脱敏规则
            List<MaskingRuleEntity> ruleEntities;
            if (dataSourceId != null) {
                ruleEntities = maskingRuleService.findByDataSource(dataSourceId);
            } else {
                ruleEntities = maskingRuleService.getAllRules();
            }
            
            if (ruleEntities == null || ruleEntities.isEmpty()) {
                log.debug("No masking rules found for export, dataSourceId: {}", dataSourceId);
                return data;
            }
            
            // 转换为DTO
            List<MaskingRule> rules = maskingRuleService.toMaskingRules(ruleEntities);
            
            // 基于角色应用脱敏
            List<Long> effectiveRoleIds = roleIds != null ? roleIds : Collections.emptyList();
            List<Map<String, Object>> maskedData = maskingEngine.maskDataByRole(data, rules, userId, effectiveRoleIds);
            
            log.info("Applied masking rules to export data: {} rules, {} rows, userId: {}, roles: {}", 
                    rules.size(), data.size(), userId, effectiveRoleIds);
            
            return maskedData;
        } catch (Exception e) {
            log.error("Error applying masking rules during export, userId: {}, dataSourceId: {}", 
                    userId, dataSourceId, e);
            // 脱敏失败时返回原始数据，避免导出完全失败
            return data;
        }
    }
    
    /**
     * CSV导出内部实现
     */
    private byte[] exportToCsvInternal(List<Map<String, Object>> data, String separator) throws IOException {
        assertExportRowsAllowed(data.size());
        if (separator == null) {
            separator = ",";
        }
        
        StringBuilder sb = new StringBuilder();
        
        // 获取所有列名
        List<String> headers = data.get(0).keySet().stream().toList();
        
        // 写入标题行
        sb.append(String.join(separator, headers)).append("\n");
        
        // 写入数据行
        for (Map<String, Object> row : data) {
            StringBuilder rowSb = new StringBuilder();
            for (int i = 0; i < headers.size(); i++) {
                if (i > 0) {
                    rowSb.append(separator);
                }
                Object value = row.get(headers.get(i));
                String strValue = value != null ? value.toString() : "";
                
                // 处理包含分隔符或换行的值
                if (strValue.contains(separator) || strValue.contains("\n") || strValue.contains("\"")) {
                    strValue = "\"" + strValue.replace("\"", "\"\"") + "\"";
                }
                rowSb.append(strValue);
            }
            sb.append(rowSb).append("\n");
        }
        
        // 添加BOM以支持中文
        byte[] bom = new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
        byte[] content = sb.toString().getBytes("UTF-8");
        byte[] result = new byte[bom.length + content.length];
        System.arraycopy(bom, 0, result, 0, bom.length);
        System.arraycopy(content, 0, result, bom.length, content.length);
        
        return result;
    }
    
    /**
     * JSON导出内部实现
     */
    private String exportToJsonInternal(List<Map<String, Object>> data) {
        assertExportRowsAllowed(data.size());
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        
        for (int i = 0; i < data.size(); i++) {
            Map<String, Object> row = data.get(i);
            sb.append("  {");
            
            int j = 0;
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                if (j > 0) {
                    sb.append(", ");
                }
                sb.append("\"").append(escapeJson(entry.getKey())).append("\": ");
                
                Object value = entry.getValue();
                if (value == null) {
                    sb.append("null");
                } else if (value instanceof Number) {
                    sb.append(value);
                } else if (value instanceof Boolean) {
                    sb.append(value);
                } else {
                    sb.append("\"").append(escapeJson(value.toString())).append("\"");
                }
                j++;
            }
            
            sb.append("}");
            if (i < data.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        
        sb.append("]");
        return sb.toString();
    }

    private void assertExportRowsAllowed(long rows) {
        licenseLimitService.assertReportExportRowsAllowed(rows);
    }
    
    /**
     * 转义JSON字符串
     */
    private String escapeJson(String str) {
        if (str == null) return "";
        return str
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }
}
