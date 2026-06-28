package com.dataplatform.data.controller;

import com.dataplatform.common.Result;
import com.dataplatform.common.annotation.RequirePermission;
import com.dataplatform.common.annotation.RequireRole;
import com.dataplatform.common.exception.BusinessException;
import com.dataplatform.common.exception.ErrorCode;
import com.dataplatform.data.service.DatabaseManagerService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

/**
 * 数据库管理控制器
 * 提供类似Navicat的数据库管理功能
 */
@Slf4j
@RestController
@RequestMapping("/db-manager")
@RequirePermission("db:manager")
public class DatabaseManagerController {

    @Autowired
    private DatabaseManagerService databaseManagerService;

    @Value("${spring.datasource.url:}")
    private String systemDbUrl;

    @Value("${spring.datasource.username:}")
    private String systemDbUsername;

    @Value("${spring.datasource.password:}")
    private String systemDbPassword;

    @Value("${db-manager.access-password:admin123}")
    private String accessPassword;

    /**
     * 验证访问密码
     */
    @PostMapping("/verify-password")
    public Result<Boolean> verifyPassword(@RequestBody Map<String, Object> params) {
        String password = (String) params.get("password");
        if (password != null && password.equals(accessPassword)) {
            return Result.success(true);
        }
        return Result.error("密码错误");
    }

    /**
     * 一键连接系统数据库
     */
    @PostMapping("/connect-system")
    public Result<String> connectSystemDb() {
        try {
            if (systemDbUrl == null || systemDbUrl.trim().isEmpty()) {
                return Result.error("系统未配置数据库连接");
            }

            Map<String, Object> config = new java.util.HashMap<>();
            String url = systemDbUrl.toLowerCase();
            
            if (url.contains("mysql")) {
                config.put("dbType", "MYSQL");
                String[] parts = systemDbUrl.split("//")[1].split("/")[0].split(":");
                config.put("host", parts[0]);
                config.put("port", parts.length > 1 ? parts[1] : "3306");
                String dbPart = systemDbUrl.split("//")[1].split("/")[1];
                String dbName = dbPart.contains("?") ? dbPart.split("\\?")[0] : dbPart;
                config.put("dbName", dbName);
            } else if (url.contains("oracle")) {
                config.put("dbType", "ORACLE");
                config.put("port", "1521");
                config.put("host", "localhost");
                config.put("dbName", "orcl");
            } else if (url.contains("sqlserver")) {
                config.put("dbType", "SQLSERVER");
                config.put("port", "1433");
                config.put("host", "localhost");
                config.put("dbName", "master");
            } else if (url.contains("postgresql")) {
                config.put("dbType", "POSTGRESQL");
                config.put("port", "5432");
                config.put("host", "localhost");
                config.put("dbName", "postgres");
            } else {
                return Result.error("不支持的数据库类型");
            }

            config.put("username", systemDbUsername);
            config.put("password", systemDbPassword);

            String sessionId = databaseManagerService.createProxyConnection(config);
            log.info("系统数据库连接成功，会话ID: {}", sessionId);
            return Result.success(sessionId);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.DATA_SOURCE_CONNECTION_FAILED, "系统数据库连接失败: " + e.getMessage());
        }
    }

    /**
     * 创建代理连接
     */
    @PostMapping("/connect")
    public Result<String> createProxyConnection(@RequestBody Map<String, Object> connectionInfo) {
        try {
            log.info("创建代理连接: type={}, host={}, dbName={}",
                    connectionInfo.get("dbType"),
                    connectionInfo.get("host"),
                    connectionInfo.get("dbName"));
            String sessionId = databaseManagerService.createProxyConnection(connectionInfo);
            return Result.success(sessionId);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.DATA_SOURCE_CONNECTION_FAILED, "创建代理连接失败: " + e.getMessage());
        }
    }

    /**
     * 关闭代理连接
     */
    @PostMapping("/disconnect")
    public Result<String> closeProxyConnection(@RequestBody Map<String, Object> params) {
        try {
            String sessionId = (String) params.get("sessionId");
            databaseManagerService.closeProxyConnection(sessionId);
            return Result.success("代理连接已关闭");
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.ERROR, "关闭代理连接失败: " + e.getMessage());
        }
    }

    /**
     * 测试数据库连接
     */
    @PostMapping("/test")
    public Result<String> testConnection(@RequestBody Map<String, Object> connectionInfo) {
        try {
            String result = databaseManagerService.testConnection(connectionInfo);
            return Result.success(result);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.DATA_SOURCE_CONNECTION_FAILED, "连接失败: " + e.getMessage());
        }
    }

    /**
     * 获取表列表
     */
    @PostMapping("/tables")
    public Result<List<Map<String, Object>>> getTables(@RequestBody Map<String, Object> params) {
        try {
            String sessionId = (String) params.get("sessionId");
            List<Map<String, Object>> tables = databaseManagerService.getTablesProxy(sessionId);
            return Result.success(tables);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.DATA_OPERATION_FAILED, "获取表列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取视图列表
     */
    @PostMapping("/views")
    public Result<List<Map<String, Object>>> getViews(@RequestBody Map<String, Object> params) {
        try {
            String sessionId = (String) params.get("sessionId");
            List<Map<String, Object>> views = databaseManagerService.getViewsProxy(sessionId);
            return Result.success(views);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.DATA_OPERATION_FAILED, "获取视图列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取存储过程/函数列表
     */
    @PostMapping("/procedures")
    public Result<List<Map<String, Object>>> getProcedures(@RequestBody Map<String, Object> params) {
        try {
            String sessionId = (String) params.get("sessionId");
            List<Map<String, Object>> procedures = databaseManagerService.getProceduresProxy(sessionId);
            return Result.success(procedures);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.DATA_OPERATION_FAILED, "获取存储过程/函数列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取表结构
     */
    @PostMapping("/table-structure")
    public Result<List<Map<String, Object>>> getTableStructure(@RequestBody Map<String, Object> params) {
        try {
            String sessionId = (String) params.get("sessionId");
            List<Map<String, Object>> structure = databaseManagerService.getTableStructureProxy(sessionId, params);
            return Result.success(structure);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.DATA_OPERATION_FAILED, "获取表结构失败: " + e.getMessage());
        }
    }

    /**
     * 查询表数据
     */
    @PostMapping("/query-data")
    public Result<Map<String, Object>> queryTableData(@RequestBody Map<String, Object> params) {
        try {
            String sessionId = (String) params.get("sessionId");
            Map<String, Object> data = databaseManagerService.queryTableDataProxy(sessionId, params);
            return Result.success(data);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.DATA_OPERATION_FAILED, "查询数据失败: " + e.getMessage());
        }
    }

    /**
     * 执行SQL语句
     */
    @RequireRole("admin")
    @PostMapping("/execute-sql")
    public Result<Map<String, Object>> executeSql(@RequestBody Map<String, Object> params) {
        try {
            String sessionId = (String) params.get("sessionId");
            Map<String, Object> result = databaseManagerService.executeSqlProxy(sessionId, params);
            return Result.success(result);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SQL_EXECUTION_FAILED, "SQL执行失败: " + e.getMessage());
        }
    }

    /**
     * 获取视图定义
     */
    @PostMapping("/view-definition")
    public Result<String> getViewDefinition(@RequestBody Map<String, Object> params) {
        try {
            String sessionId = (String) params.get("sessionId");
            String definition = databaseManagerService.getViewDefinitionProxy(sessionId, params);
            return Result.success(definition);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.DATA_OPERATION_FAILED, "获取视图定义失败: " + e.getMessage());
        }
    }

    /**
     * 获取存储过程/函数定义
     */
    @PostMapping("/procedure-definition")
    public Result<String> getProcedureDefinition(@RequestBody Map<String, Object> params) {
        try {
            String sessionId = (String) params.get("sessionId");
            String definition = databaseManagerService.getProcedureDefinitionProxy(sessionId, params);
            return Result.success(definition);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.DATA_OPERATION_FAILED, "获取存储过程/函数定义失败: " + e.getMessage());
        }
    }

    /**
     * 获取表索引
     */
    @PostMapping("/table-indexes")
    public Result<List<Map<String, Object>>> getTableIndexes(@RequestBody Map<String, Object> params) {
        try {
            String sessionId = (String) params.get("sessionId");
            List<Map<String, Object>> indexes = databaseManagerService.getTableIndexesProxy(sessionId, params);
            return Result.success(indexes);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.DATA_OPERATION_FAILED, "获取索引信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取SQL执行历史
     */
    @GetMapping("/sql-history")
    public Result<Map<String, Object>> getSqlHistory(
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        try {
            Map<String, Object> result = databaseManagerService.getSqlHistory(
                    sessionId, keyword, status, page, Math.min(pageSize, 100));
            return Result.success(result);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.DATA_OPERATION_FAILED, "获取SQL历史失败: " + e.getMessage());
        }
    }

    /**
     * 保存SQL到历史（草稿）
     */
    @PostMapping("/save-sql-history")
    public Result<String> saveSqlHistory(@RequestBody Map<String, Object> params) {
        try {
            String sessionId = (String) params.get("sessionId");
            String sqlContent = (String) params.get("sql");
            if (sqlContent == null || sqlContent.trim().isEmpty()) {
                return Result.error("SQL内容不能为空");
            }
            databaseManagerService.saveSqlDraft(sessionId, sqlContent);
            return Result.success("已保存到SQL历史");
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.ERROR, "保存失败: " + e.getMessage());
        }
    }

    /**
     * 清空会话SQL历史
     */
    @DeleteMapping("/sql-history/{sessionId}")
    public Result<String> clearSqlHistory(@PathVariable String sessionId) {
        try {
            databaseManagerService.clearSqlHistory(sessionId);
            return Result.success("历史记录已清空");
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.ERROR, "清空历史失败: " + e.getMessage());
        }
    }

    /**
     * 导出查询结果为 Excel
     */
    @PostMapping("/export-query")
    public ResponseEntity<?> exportQueryResult(@RequestBody Map<String, Object> params) {
        try {
            String sessionId = (String) params.get("sessionId");
            Map<String, Object> queryResult = databaseManagerService.executeSqlProxy(sessionId, params);

            @SuppressWarnings("unchecked")
            List<String> columns = (List<String>) queryResult.get("columns");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> data = (List<Map<String, Object>>) queryResult.get("data");

            if (columns == null || data == null) {
                return ResponseEntity.badRequest().body(Result.error("该SQL不是SELECT查询，无法导出"));
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
                Sheet sheet = workbook.createSheet("查询结果");

                // 表头
                CellStyle headerStyle = workbook.createCellStyle();
                Font headerFont = workbook.createFont();
                headerFont.setBold(true);
                headerStyle.setFont(headerFont);
                headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
                headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

                Row headerRow = sheet.createRow(0);
                for (int i = 0; i < columns.size(); i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(columns.get(i));
                    cell.setCellStyle(headerStyle);
                }

                // 数据行
                for (int r = 0; r < data.size(); r++) {
                    Row row = sheet.createRow(r + 1);
                    Map<String, Object> rowData = data.get(r);
                    for (int c = 0; c < columns.size(); c++) {
                        Cell cell = row.createCell(c);
                        Object val = rowData.get(columns.get(c));
                        if (val == null) {
                            cell.setCellValue("");
                        } else if (val instanceof Number) {
                            cell.setCellValue(((Number) val).doubleValue());
                        } else {
                            cell.setCellValue(val.toString());
                        }
                    }
                }

                workbook.write(baos);
                workbook.dispose();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            String encodedName = java.net.URLEncoder.encode("查询结果.xlsx", "UTF-8").replaceAll("\\+", "%20");
            headers.set("Content-Disposition",
                    "attachment; filename=\"query_result.xlsx\"; filename*=UTF-8''" + encodedName);
            headers.set("Access-Control-Expose-Headers", "Content-Disposition");
            return ResponseEntity.ok().headers(headers).body(baos.toByteArray());
        } catch (Exception e) {
            log.error("导出查询结果失败", e);
            return ResponseEntity.status(500).body(Result.error("导出失败: " + e.getMessage()));
        }
    }

    /**
     * 执行 EXPLAIN 执行计划
     */
    @PostMapping("/explain")
    public Result<Map<String, Object>> explainSql(@RequestBody Map<String, Object> params) {
        try {
            String sessionId = (String) params.get("sessionId");
            Map<String, Object> result = databaseManagerService.explainSql(sessionId, params);
            return Result.success(result);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SQL_EXECUTION_FAILED, "EXPLAIN执行失败: " + e.getMessage());
        }
    }

    // ==================== SQL收藏 ====================

    @Autowired
    private com.dataplatform.data.mapper.SqlSnippetMapper sqlSnippetMapper;

    @PostMapping("/sql-snippet")
    public Result<com.dataplatform.data.entity.SqlSnippet> saveSnippet(@RequestBody Map<String, Object> params) {
        try {
            String name = (String) params.get("name");
            String sqlContent = (String) params.get("sqlContent");
            if (name == null || name.trim().isEmpty()) return Result.error("名称不能为空");
            if (sqlContent == null || sqlContent.trim().isEmpty()) return Result.error("SQL内容不能为空");
            com.dataplatform.data.entity.SqlSnippet snippet = new com.dataplatform.data.entity.SqlSnippet();
            snippet.setName(name.trim());
            snippet.setSqlContent(sqlContent);
            snippet.setDescription((String) params.get("description"));
            snippet.setDbType((String) params.get("dbType"));
            sqlSnippetMapper.insert(snippet);
            return Result.success(snippet);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.ERROR, "保存失败: " + e.getMessage());
        }
    }

    @PutMapping("/sql-snippet/{id}")
    public Result<String> updateSnippet(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        try {
            com.dataplatform.data.entity.SqlSnippet snippet = sqlSnippetMapper.selectById(id);
            if (snippet == null) return Result.error("记录不存在");
            if (params.containsKey("name")) snippet.setName((String) params.get("name"));
            if (params.containsKey("sqlContent")) snippet.setSqlContent((String) params.get("sqlContent"));
            if (params.containsKey("description")) snippet.setDescription((String) params.get("description"));
            if (params.containsKey("dbType")) snippet.setDbType((String) params.get("dbType"));
            sqlSnippetMapper.update(snippet);
            return Result.success("更新成功");
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.ERROR, "更新失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/sql-snippet/{id}")
    public Result<String> deleteSnippet(@PathVariable Long id) {
        try {
            sqlSnippetMapper.deleteById(id);
            return Result.success("删除成功");
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.ERROR, "删除失败: " + e.getMessage());
        }
    }

    @GetMapping("/sql-snippets")
    public Result<Map<String, Object>> listSnippets(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String dbType,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "50") Integer pageSize) {
        try {
            int safePageSize = Math.min(pageSize, 200);
            int offset = (page - 1) * safePageSize;
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("list", sqlSnippetMapper.selectList(keyword, dbType, offset, safePageSize));
            result.put("total", sqlSnippetMapper.count(keyword, dbType));
            return Result.success(result);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.DATA_OPERATION_FAILED, "查询失败: " + e.getMessage());
        }
    }
}
