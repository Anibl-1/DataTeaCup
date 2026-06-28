package com.dataplatform.data.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 文件解析服务
 * 负责Excel和CSV文件的解析，遵循单一职责原则
 */
@Slf4j
@Service
public class FileParseService {
    private static final int MAX_ROWS = 1000;

    /**
     * 解析结果
     */
    public static class ParseResult {
        private final List<String> columns;
        private final List<Map<String, Object>> data;

        public ParseResult(List<String> columns, List<Map<String, Object>> data) {
            this.columns = columns;
            this.data = data;
        }

        public List<String> getColumns() { return columns; }
        public List<Map<String, Object>> getData() { return data; }
    }

    /**
     * 根据文件扩展名自动选择解析方式
     */
    public ParseResult parse(MultipartFile file) throws Exception {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("文件名为空");
        }

        String lowerName = filename.toLowerCase();
        if (lowerName.endsWith(".xlsx") || lowerName.endsWith(".xls")) {
            return parseExcel(file);
        } else if (lowerName.endsWith(".csv")) {
            return parseCsv(file);
        } else {
            throw new IllegalArgumentException("不支持的文件格式，请上传Excel或CSV文件");
        }
    }

    /**
     * 解析Excel文件
     */
    public ParseResult parseExcel(MultipartFile file) throws Exception {
        List<String> columns = new ArrayList<>();
        List<Map<String, Object>> data = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            // 读取表头
            if (rowIterator.hasNext()) {
                Row headerRow = rowIterator.next();
                for (Cell cell : headerRow) {
                    columns.add(getCellValueAsString(cell));
                }
            }

            // 读取数据
            int rowCount = 0;
            while (rowIterator.hasNext() && rowCount < MAX_ROWS) {
                Row row = rowIterator.next();
                Map<String, Object> rowData = new LinkedHashMap<>();
                for (int i = 0; i < columns.size(); i++) {
                    Cell cell = row.getCell(i);
                    rowData.put(columns.get(i), cell != null ? getCellValueAsString(cell) : "");
                }
                data.add(rowData);
                rowCount++;
            }
        }

        return new ParseResult(columns, data);
    }

    /**
     * 解析CSV文件
     */
    public ParseResult parseCsv(MultipartFile file) throws Exception {
        List<String> columns = new ArrayList<>();
        List<Map<String, Object>> data = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean isHeader = true;
            int rowCount = 0;

            while ((line = reader.readLine()) != null && rowCount < MAX_ROWS) {
                String[] values = line.split(",");

                if (isHeader) {
                    for (String value : values) {
                        columns.add(value.trim().replace("\"", ""));
                    }
                    isHeader = false;
                } else {
                    Map<String, Object> rowData = new LinkedHashMap<>();
                    for (int i = 0; i < columns.size() && i < values.length; i++) {
                        rowData.put(columns.get(i), values[i].trim().replace("\"", ""));
                    }
                    data.add(rowData);
                    rowCount++;
                }
            }
        }

        return new ParseResult(columns, data);
    }

    /**
     * 获取单元格值为字符串
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                double numValue = cell.getNumericCellValue();
                if (numValue == Math.floor(numValue)) {
                    return String.valueOf((long) numValue);
                }
                return String.valueOf(numValue);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return String.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    return cell.getStringCellValue();
                }
            default:
                return "";
        }
    }
}
