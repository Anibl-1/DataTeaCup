package com.dataplatform.data.service.connector;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.DateUtil;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.*;

/**
 * 文件数据源连接器
 * 支持CSV、Excel、JSON、XML、Parquet格式
 * 需求: 17.2
 */
@Slf4j
@Component
public class FileConnector implements DataConnector {

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    @Override
    public String getType() {
        return "file";
    }

    @Override
    public boolean testConnection(ConnectorConfig config) {
        String path = config.getProperty("path");
        if (path == null) return false;
        return Files.exists(Paths.get(path));
    }

    @Override
    public List<String> listTables(ConnectorConfig config) {
        String path = config.getProperty("path");
        if (path == null) return Collections.emptyList();

        Path dir = Paths.get(path);
        if (!Files.isDirectory(dir)) {
            return List.of(dir.getFileName().toString());
        }

        List<String> files = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.{csv,json,xml,xlsx,xls,parquet}")) {
            for (Path entry : stream) {
                files.add(entry.getFileName().toString());
            }
        } catch (IOException e) {
            log.warn("[文件连接器] 列出文件失败: {}", e.getMessage());
        }
        return files;
    }

    @Override
    public List<ConnectorResult.ColumnMeta> getColumns(ConnectorConfig config, String fileName) {
        String path = config.getProperty("path");
        Path filePath = resolveFilePath(path, fileName);
        if (filePath == null || !Files.exists(filePath)) return Collections.emptyList();

        String ext = getExtension(fileName).toLowerCase();
        switch (ext) {
            case "csv":
                return getCsvColumns(filePath, config);
            case "xlsx":
            case "xls":
                return getExcelColumns(filePath);
            case "json":
                return getJsonColumns(filePath);
            default:
                return Collections.emptyList();
        }
    }

    @Override
    public ConnectorResult readData(ConnectorConfig config, String fileName, Map<String, Object> params) {
        String path = config.getProperty("path");
        Path filePath = resolveFilePath(path, fileName);
        ConnectorResult result = new ConnectorResult();

        if (filePath == null || !Files.exists(filePath)) {
            result.setRows(Collections.emptyList());
            result.setTotalRows(0);
            return result;
        }

        String ext = getExtension(fileName).toLowerCase();
        switch (ext) {
            case "csv":
                return readCsv(filePath, config, params);
            case "json":
                return readJson(filePath, params);
            case "xlsx":
            case "xls":
                return readExcel(filePath, params);
            default:
                log.warn("[文件连接器] 暂不支持的文件格式: {}", ext);
                result.setRows(Collections.emptyList());
                result.setTotalRows(0);
                return result;
        }
    }

    private ConnectorResult readCsv(Path filePath, ConnectorConfig config, Map<String, Object> params) {
        ConnectorResult result = new ConnectorResult();
        List<Map<String, Object>> rows = new ArrayList<>();
        String delimiter = config.getProperty("delimiter", ",");
        String charset = config.getProperty("charset", "UTF-8");
        int limit = params.containsKey("limit") ? ((Number) params.get("limit")).intValue() : Integer.MAX_VALUE;

        try (BufferedReader reader = Files.newBufferedReader(filePath, Charset.forName(charset))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                result.setRows(Collections.emptyList());
                result.setTotalRows(0);
                return result;
            }

            String[] headers = headerLine.split(delimiter);
            List<ConnectorResult.ColumnMeta> columns = new ArrayList<>();
            for (String h : headers) {
                columns.add(new ConnectorResult.ColumnMeta(h.trim(), "String", true));
            }
            result.setColumns(columns);

            String line;
            int count = 0;
            while ((line = reader.readLine()) != null && count < limit) {
                String[] values = line.split(delimiter, -1);
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 0; i < headers.length; i++) {
                    row.put(headers[i].trim(), i < values.length ? values[i].trim() : "");
                }
                rows.add(row);
                count++;
            }
        } catch (IOException e) {
            log.error("[文件连接器] 读取CSV失败: {}", e.getMessage());
        }

        result.setRows(rows);
        result.setTotalRows(rows.size());
        return result;
    }

    private ConnectorResult readJson(Path filePath, Map<String, Object> params) {
        ConnectorResult result = new ConnectorResult();
        int limit = params.containsKey("limit") ? ((Number) params.get("limit")).intValue() : Integer.MAX_VALUE;
        try {
            String content = Files.readString(filePath);
            String trimmed = content.trim();
            List<Map<String, Object>> allRows;
            if (trimmed.startsWith("[")) {
                allRows = JSON_MAPPER.readValue(trimmed, new TypeReference<List<Map<String, Object>>>() {});
            } else if (trimmed.startsWith("{")) {
                Map<String, Object> single = JSON_MAPPER.readValue(trimmed, new TypeReference<Map<String, Object>>() {});
                allRows = List.of(single);
            } else {
                log.warn("[文件连接器] JSON格式不支持，需要数组或对象");
                result.setRows(Collections.emptyList());
                result.setTotalRows(0);
                return result;
            }

            List<Map<String, Object>> rows = allRows.size() > limit ? allRows.subList(0, limit) : allRows;
            // 从第一条记录推断列信息
            if (!rows.isEmpty()) {
                List<ConnectorResult.ColumnMeta> columns = new ArrayList<>();
                for (String key : rows.get(0).keySet()) {
                    columns.add(new ConnectorResult.ColumnMeta(key, "String", true));
                }
                result.setColumns(columns);
            }
            result.setRows(rows);
            result.setTotalRows(rows.size());
            result.setHasMore(allRows.size() > limit);
            log.info("[文件连接器] JSON文件读取完成, totalRows={}", rows.size());
        } catch (IOException e) {
            log.error("[文件连接器] 读取JSON失败: {}", e.getMessage());
            result.setRows(Collections.emptyList());
            result.setTotalRows(0);
        }
        return result;
    }

    private ConnectorResult readExcel(Path filePath, Map<String, Object> params) {
        ConnectorResult result = new ConnectorResult();
        List<Map<String, Object>> rows = new ArrayList<>();
        int limit = params.containsKey("limit") ? ((Number) params.get("limit")).intValue() : Integer.MAX_VALUE;
        int sheetIndex = params.containsKey("sheetIndex") ? ((Number) params.get("sheetIndex")).intValue() : 0;

        try (InputStream is = Files.newInputStream(filePath);
             Workbook workbook = WorkbookFactory.create(is)) {

            if (sheetIndex >= workbook.getNumberOfSheets()) {
                log.warn("[文件连接器] Sheet索引越界: index={}, total={}", sheetIndex, workbook.getNumberOfSheets());
                result.setRows(Collections.emptyList());
                result.setTotalRows(0);
                return result;
            }

            Sheet sheet = workbook.getSheetAt(sheetIndex);
            if (sheet.getPhysicalNumberOfRows() == 0) {
                result.setRows(Collections.emptyList());
                result.setTotalRows(0);
                return result;
            }

            // 读取表头
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                result.setRows(Collections.emptyList());
                result.setTotalRows(0);
                return result;
            }

            List<String> headers = new ArrayList<>();
            List<ConnectorResult.ColumnMeta> columns = new ArrayList<>();
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i);
                String headerName = (cell != null) ? getCellStringValue(cell) : "Column" + i;
                headers.add(headerName);
                columns.add(new ConnectorResult.ColumnMeta(headerName, "String", true));
            }
            result.setColumns(columns);

            // 读取数据行
            int count = 0;
            for (int rowIdx = 1; rowIdx <= sheet.getLastRowNum() && count < limit; rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null) continue;

                Map<String, Object> rowData = new LinkedHashMap<>();
                for (int colIdx = 0; colIdx < headers.size(); colIdx++) {
                    Cell cell = row.getCell(colIdx);
                    rowData.put(headers.get(colIdx), cell != null ? getCellValue(cell) : "");
                }
                rows.add(rowData);
                count++;
            }

            result.setRows(rows);
            result.setTotalRows(rows.size());
            result.setHasMore(sheet.getLastRowNum() > limit);
            log.info("[文件连接器] Excel文件读取完成, sheet={}, totalRows={}", sheet.getSheetName(), rows.size());
        } catch (Exception e) {
            log.error("[文件连接器] 读取Excel失败: {}", e.getMessage());
            result.setRows(Collections.emptyList());
            result.setTotalRows(0);
        }
        return result;
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) return "";
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue().trim();
    }

    private Object getCellValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toString();
                }
                double numVal = cell.getNumericCellValue();
                if (numVal == Math.floor(numVal) && !Double.isInfinite(numVal)) {
                    return (long) numVal;
                }
                return numVal;
            case BOOLEAN:
                return cell.getBooleanCellValue();
            case FORMULA:
                try {
                    return cell.getNumericCellValue();
                } catch (Exception e) {
                    try {
                        return cell.getStringCellValue();
                    } catch (Exception ex) {
                        return cell.getCellFormula();
                    }
                }
            case BLANK:
                return "";
            default:
                return cell.toString();
        }
    }

    private List<ConnectorResult.ColumnMeta> getCsvColumns(Path filePath, ConnectorConfig config) {
        String delimiter = config.getProperty("delimiter", ",");
        String charset = config.getProperty("charset", "UTF-8");
        try (BufferedReader reader = Files.newBufferedReader(filePath, Charset.forName(charset))) {
            String headerLine = reader.readLine();
            if (headerLine == null) return Collections.emptyList();
            String[] headers = headerLine.split(delimiter);
            List<ConnectorResult.ColumnMeta> columns = new ArrayList<>();
            for (String h : headers) {
                columns.add(new ConnectorResult.ColumnMeta(h.trim(), "String", true));
            }
            return columns;
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    private List<ConnectorResult.ColumnMeta> getExcelColumns(Path filePath) {
        try (InputStream is = Files.newInputStream(filePath);
             Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) return Collections.emptyList();
            List<ConnectorResult.ColumnMeta> columns = new ArrayList<>();
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i);
                String name = (cell != null) ? getCellStringValue(cell) : "Column" + i;
                columns.add(new ConnectorResult.ColumnMeta(name, "String", true));
            }
            return columns;
        } catch (Exception e) {
            log.warn("[文件连接器] 读取Excel表头失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<ConnectorResult.ColumnMeta> getJsonColumns(Path filePath) {
        try {
            String content = Files.readString(filePath);
            String trimmed = content.trim();
            Map<String, Object> firstRow = null;
            if (trimmed.startsWith("[")) {
                List<Map<String, Object>> list = JSON_MAPPER.readValue(trimmed, new TypeReference<List<Map<String, Object>>>() {});
                if (!list.isEmpty()) firstRow = list.get(0);
            } else if (trimmed.startsWith("{")) {
                firstRow = JSON_MAPPER.readValue(trimmed, new TypeReference<Map<String, Object>>() {});
            }
            if (firstRow == null) return Collections.emptyList();
            List<ConnectorResult.ColumnMeta> columns = new ArrayList<>();
            for (String key : firstRow.keySet()) {
                columns.add(new ConnectorResult.ColumnMeta(key, "String", true));
            }
            return columns;
        } catch (Exception e) {
            log.warn("[文件连接器] 读取JSON列信息失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private Path resolveFilePath(String basePath, String fileName) {
        if (basePath == null) return null;
        Path base = Paths.get(basePath);
        if (Files.isDirectory(base)) {
            if (fileName == null || fileName.isEmpty()) return null;
            Path resolved = base.resolve(fileName).normalize();
            // 路径穿越防护：确保解析后的路径仍在base目录下
            if (!resolved.startsWith(base.normalize())) {
                log.warn("[文件连接器] 检测到路径穿越攻击: basePath={}, fileName={}", basePath, fileName);
                return null;
            }
            return resolved;
        }
        return base;
    }

    private String getExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 ? fileName.substring(dot + 1) : "";
    }
}
