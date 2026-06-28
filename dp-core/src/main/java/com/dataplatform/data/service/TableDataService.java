package com.dataplatform.data.service;

import com.dataplatform.common.exception.BusinessException;
import com.dataplatform.common.exception.ErrorCode;
import com.dataplatform.data.entity.DataSource;
import com.dataplatform.data.mapper.DataSourceMapper;
import com.dataplatform.data.service.DbConnectionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.sql.*;
import java.util.*;

/**
 * 数据表管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TableDataService {
    
    @org.springframework.beans.factory.annotation.Value("${query.max-page-size:10000}")
    private int maxPageSize;
    
    @org.springframework.beans.factory.annotation.Value("${query.max-result-rows:50000}")
    private int maxResultRows;
    
    @org.springframework.beans.factory.annotation.Value("${db.query.timeout:300}")
    private int queryTimeout;

    private final DataSourceMapper dataSourceMapper;
    private final DbConnectionUtil dbConnectionUtil;
    private final com.dataplatform.data.service.DataSourceConnectionPoolManager connectionPoolManager;

    /**
     * 获取数据库连接（使用连接池）
     */
    private Connection getConnection(Long dataSourceId) throws Exception {
        DataSource ds = dataSourceMapper.selectById(dataSourceId);
        if (ds == null) {
            throw new BusinessException(ErrorCode.DATA_SOURCE_NOT_FOUND, "数据源不存在: " + dataSourceId);
        }
        
        String driverClass = dbConnectionUtil.getDriverClassName(ds.getDbType());
        if (driverClass != null && !driverClass.isEmpty()) {
            Class.forName(driverClass);
        }
        
        return connectionPoolManager.getConnection(ds);
    }
    

    /**
     * 获取表列表
     */
    public List<Map<String, Object>> getTables(Long dataSourceId) {
        List<Map<String, Object>> tables = new ArrayList<>();
        try (Connection conn = getConnection(dataSourceId)) {
            DatabaseMetaData metaData = conn.getMetaData();
            String catalog = conn.getCatalog();
            
            try (ResultSet rs = metaData.getTables(catalog, null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    Map<String, Object> table = new HashMap<>();
                    table.put("tableName", rs.getString("TABLE_NAME"));
                    table.put("remarks", rs.getString("REMARKS"));
                    tables.add(table);
                }
            }
        } catch (Exception e) {
            log.error("获取表列表失败", e);
            throw new BusinessException(ErrorCode.DATA_OPERATION_FAILED, "获取表列表失败: " + e.getMessage());
        }
        return tables;
    }

    /**
     * 获取表结构
     */
    public List<Map<String, Object>> getTableStructure(Long dataSourceId, String tableName) {
        List<Map<String, Object>> columns = new ArrayList<>();
        Set<String> primaryKeys = new HashSet<>();
        
        try (Connection conn = getConnection(dataSourceId)) {
            DatabaseMetaData metaData = conn.getMetaData();
            String catalog = conn.getCatalog();
            
            // 获取主键
            try (ResultSet pkRs = metaData.getPrimaryKeys(catalog, null, tableName)) {
                while (pkRs.next()) {
                    primaryKeys.add(pkRs.getString("COLUMN_NAME"));
                }
            }
            
            // 获取列信息
            try (ResultSet rs = metaData.getColumns(catalog, null, tableName, "%")) {
                while (rs.next()) {
                    Map<String, Object> column = new HashMap<>();
                    String columnName = rs.getString("COLUMN_NAME");
                    column.put("columnName", columnName);
                    column.put("dataType", rs.getString("TYPE_NAME"));
                    column.put("columnSize", rs.getInt("COLUMN_SIZE"));
                    column.put("nullable", rs.getString("IS_NULLABLE"));
                    column.put("defaultValue", rs.getString("COLUMN_DEF"));
                    column.put("remarks", rs.getString("REMARKS"));
                    column.put("isPrimaryKey", primaryKeys.contains(columnName));
                    columns.add(column);
                }
            }
        } catch (Exception e) {
            log.error("获取表结构失败", e);
            throw new BusinessException(ErrorCode.DATA_OPERATION_FAILED, "获取表结构失败: " + e.getMessage());
        }
        return columns;
    }

    /**
     * 查询表数据
     */
    public Map<String, Object> queryTableData(Long dataSourceId, String tableName, 
            int page, int pageSize, String where, String orderBy) {
        return queryTableData(dataSourceId, tableName, page, pageSize, where, orderBy, null, null);
    }
    
    /**
     * 查询表数据（支持安全的关键词搜索）
     * @param searchKeyword 搜索关键词（参数化查询，防止SQL注入）
     * @param searchColumns 搜索列名列表（仅允许字母、数字、下划线）
     */
    public Map<String, Object> queryTableData(Long dataSourceId, String tableName, 
            int page, int pageSize, String where, String orderBy,
            String searchKeyword, List<String> searchColumns) {
        // 参数校验
        if (tableName == null || tableName.trim().isEmpty()) {
            throw new IllegalArgumentException("表名不能为空");
        }
        
        // 限制pageSize防止内存溢出
        if (pageSize > maxPageSize) {
            pageSize = maxPageSize;
            log.warn("pageSize超过最大限制，已调整为: {}", maxPageSize);
        }
        if (page < 1) page = 1;
        
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> list = new ArrayList<>();
        int total = 0;
        
        try (Connection conn = getConnection(dataSourceId)) {
            // 构建SELECT SQL
            StringBuilder sql = new StringBuilder("SELECT * FROM ").append(escapeTableName(tableName));
            StringBuilder countSql = new StringBuilder("SELECT COUNT(*) FROM ").append(escapeTableName(tableName));
            List<Object> params = new ArrayList<>();
            
            // 优先使用参数化关键词搜索（安全）
            if (searchKeyword != null && !searchKeyword.trim().isEmpty()
                    && searchColumns != null && !searchColumns.isEmpty()) {
                StringBuilder condition = new StringBuilder(" WHERE (");
                String likeParam = "%" + searchKeyword.trim() + "%";
                for (int i = 0; i < searchColumns.size(); i++) {
                    String col = searchColumns.get(i);
                    // 列名安全校验：仅允许字母、数字、下划线
                    if (!col.matches("^[a-zA-Z0-9_]+$")) {
                        log.warn("忽略不安全的列名: {}", col);
                        continue;
                    }
                    if (i > 0) condition.append(" OR ");
                    condition.append(escapeColumnName(col)).append(" LIKE ?");
                    params.add(likeParam);
                }
                condition.append(")");
                if (!params.isEmpty()) {
                    sql.append(condition);
                    countSql.append(condition);
                }
            } else if (where != null && !where.trim().isEmpty()) {
                // 向后兼容：使用传统where字符串（加强安全过滤）
                String safeWhere = sanitizeWhereClause(where);
                if (!safeWhere.isEmpty()) {
                    sql.append(" WHERE ").append(safeWhere);
                    countSql.append(" WHERE ").append(safeWhere);
                }
            }
            
            if (orderBy != null && !orderBy.trim().isEmpty()) {
                // orderBy安全过滤：仅允许字母、数字、下划线、空格、逗号、点号和ASC/DESC
                String safeOrderBy = orderBy.replaceAll("[^a-zA-Z0-9_,. ]", "");
                if (!safeOrderBy.isEmpty()) {
                    sql.append(" ORDER BY ").append(safeOrderBy);
                }
            }
            
            // 分页
            int offset = (page - 1) * pageSize;
            sql.append(" LIMIT ").append(pageSize).append(" OFFSET ").append(offset);
            
            // 查询总数
            try (PreparedStatement pstmt = conn.prepareStatement(countSql.toString())) {
                pstmt.setQueryTimeout(queryTimeout);
                for (int i = 0; i < params.size(); i++) {
                    pstmt.setObject(i + 1, params.get(i));
                }
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        total = rs.getInt(1);
                    }
                }
            }
            
            // 查询数据
            try (PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
                pstmt.setQueryTimeout(queryTimeout);
                pstmt.setFetchSize(500);
                pstmt.setMaxRows(maxResultRows);
                for (int i = 0; i < params.size(); i++) {
                    pstmt.setObject(i + 1, params.get(i));
                }
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    int rowCount = 0;
                    
                    while (rs.next() && rowCount < maxResultRows) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        for (int i = 1; i <= columnCount; i++) {
                            Object value = rs.getObject(i);
                            // 处理大文本字段
                            if (value instanceof String && ((String) value).length() > 10000) {
                                value = ((String) value).substring(0, 10000) + "...[截断]";
                            }
                            row.put(metaData.getColumnName(i), value);
                        }
                        list.add(row);
                        rowCount++;
                    }
                }
            }
        } catch (Exception e) {
            log.error("查询表数据失败", e);
            throw new BusinessException(ErrorCode.DATA_OPERATION_FAILED, "查询表数据失败: " + e.getMessage());
        }
        
        result.put("list", list);
        result.put("total", total);
        return result;
    }
    
    /**
     * 对传入的where子句进行安全过滤（向后兼容用，新代码应使用searchKeyword参数化查询）
     */
    private String sanitizeWhereClause(String where) {
        if (where == null) return "";
        // 移除危险字符和SQL注入常见手法
        String safe = where
            .replaceAll(";", "")
            .replaceAll("--", "")
            .replaceAll("/\\*.*?\\*/", "")
            .replaceAll("(?i)\\bUNION\\b", "")
            .replaceAll("(?i)\\bINTO\\s+OUTFILE\\b", "")
            .replaceAll("(?i)\\bINTO\\s+DUMPFILE\\b", "")
            .replaceAll("(?i)\\bLOAD_FILE\\b", "")
            .replaceAll("(?i)\\bSLEEP\\s*\\(", "")
            .replaceAll("(?i)\\bBENCHMARK\\s*\\(", "")
            .trim();
        return safe;
    }

    /**
     * 新增数据行
     */
    public int insertRow(Long dataSourceId, String tableName, Map<String, Object> data) {
        try (Connection conn = getConnection(dataSourceId)) {
            // 过滤空值
            Map<String, Object> filteredData = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().toString().isEmpty()) {
                    filteredData.put(entry.getKey(), entry.getValue());
                }
            }
            
            if (filteredData.isEmpty()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "没有有效的数据");
            }
            
            StringBuilder sql = new StringBuilder("INSERT INTO ").append(escapeTableName(tableName)).append(" (");
            StringBuilder values = new StringBuilder(" VALUES (");
            
            List<Object> params = new ArrayList<>();
            int i = 0;
            for (Map.Entry<String, Object> entry : filteredData.entrySet()) {
                if (i > 0) {
                    sql.append(", ");
                    values.append(", ");
                }
                sql.append(escapeColumnName(entry.getKey()));
                values.append("?");
                params.add(entry.getValue());
                i++;
            }
            sql.append(")").append(values).append(")");
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
                for (int j = 0; j < params.size(); j++) {
                    pstmt.setObject(j + 1, params.get(j));
                }
                return pstmt.executeUpdate();
            }
        } catch (Exception e) {
            log.error("新增数据失败", e);
            throw new BusinessException(ErrorCode.DATA_OPERATION_FAILED, "新增数据失败: " + e.getMessage());
        }
    }

    /**
     * 更新数据行
     */
    public int updateRow(Long dataSourceId, String tableName, Map<String, Object> data, 
            String primaryKey, Object primaryValue) {
        try (Connection conn = getConnection(dataSourceId)) {
            StringBuilder sql = new StringBuilder("UPDATE ").append(escapeTableName(tableName)).append(" SET ");
            
            List<Object> params = new ArrayList<>();
            int i = 0;
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                if (entry.getKey().equals(primaryKey)) continue; // 跳过主键
                if (i > 0) sql.append(", ");
                sql.append(escapeColumnName(entry.getKey())).append(" = ?");
                params.add(entry.getValue());
                i++;
            }
            
            sql.append(" WHERE ").append(escapeColumnName(primaryKey)).append(" = ?");
            params.add(primaryValue);
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
                for (int j = 0; j < params.size(); j++) {
                    pstmt.setObject(j + 1, params.get(j));
                }
                return pstmt.executeUpdate();
            }
        } catch (Exception e) {
            log.error("更新数据失败", e);
            throw new BusinessException(ErrorCode.DATA_OPERATION_FAILED, "更新数据失败: " + e.getMessage());
        }
    }

    /**
     * 删除数据行
     */
    public int deleteRow(Long dataSourceId, String tableName, String primaryKey, Object primaryValue) {
        try (Connection conn = getConnection(dataSourceId)) {
            String sql = "DELETE FROM " + escapeTableName(tableName) + 
                        " WHERE " + escapeColumnName(primaryKey) + " = ?";
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setObject(1, primaryValue);
                return pstmt.executeUpdate();
            }
        } catch (Exception e) {
            log.error("删除数据失败", e);
            throw new BusinessException(ErrorCode.DATA_OPERATION_FAILED, "删除数据失败: " + e.getMessage());
        }
    }

    /**
     * 批量删除数据行
     */
    public int batchDeleteRows(Long dataSourceId, String tableName, String primaryKey, List<Object> primaryValues) {
        if (primaryValues == null || primaryValues.isEmpty()) {
            return 0;
        }
        
        try (Connection conn = getConnection(dataSourceId)) {
            StringBuilder sql = new StringBuilder("DELETE FROM ")
                .append(escapeTableName(tableName))
                .append(" WHERE ")
                .append(escapeColumnName(primaryKey))
                .append(" IN (");
            
            for (int i = 0; i < primaryValues.size(); i++) {
                if (i > 0) sql.append(", ");
                sql.append("?");
            }
            sql.append(")");
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
                for (int i = 0; i < primaryValues.size(); i++) {
                    pstmt.setObject(i + 1, primaryValues.get(i));
                }
                return pstmt.executeUpdate();
            }
        } catch (Exception e) {
            log.error("批量删除数据失败", e);
            throw new BusinessException(ErrorCode.DATA_OPERATION_FAILED, "批量删除数据失败: " + e.getMessage());
        }
    }

    /**
     * 更新数据行（支持复合主键）
     */
    public int updateRowWithCompositeKey(Long dataSourceId, String tableName, Map<String, Object> data, 
            List<String> primaryKeys, Map<String, Object> primaryValues) {
        try (Connection conn = getConnection(dataSourceId)) {
            StringBuilder sql = new StringBuilder("UPDATE ").append(escapeTableName(tableName)).append(" SET ");
            
            List<Object> params = new ArrayList<>();
            int i = 0;
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                // 跳过所有主键字段
                if (primaryKeys.contains(entry.getKey())) continue;
                if (i > 0) sql.append(", ");
                sql.append(escapeColumnName(entry.getKey())).append(" = ?");
                params.add(entry.getValue());
                i++;
            }
            
            // 构建复合主键 WHERE 条件
            sql.append(" WHERE ");
            for (int j = 0; j < primaryKeys.size(); j++) {
                if (j > 0) sql.append(" AND ");
                sql.append(escapeColumnName(primaryKeys.get(j))).append(" = ?");
                params.add(primaryValues.get(primaryKeys.get(j)));
            }
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
                for (int j = 0; j < params.size(); j++) {
                    pstmt.setObject(j + 1, params.get(j));
                }
                return pstmt.executeUpdate();
            }
        } catch (Exception e) {
            log.error("更新数据失败（复合主键）", e);
            throw new BusinessException(ErrorCode.DATA_OPERATION_FAILED, "更新数据失败: " + e.getMessage());
        }
    }

    /**
     * 删除数据行（支持复合主键）
     */
    public int deleteRowWithCompositeKey(Long dataSourceId, String tableName, 
            List<String> primaryKeys, Map<String, Object> primaryValues) {
        try (Connection conn = getConnection(dataSourceId)) {
            StringBuilder sql = new StringBuilder("DELETE FROM ")
                .append(escapeTableName(tableName))
                .append(" WHERE ");
            
            List<Object> params = new ArrayList<>();
            for (int i = 0; i < primaryKeys.size(); i++) {
                if (i > 0) sql.append(" AND ");
                sql.append(escapeColumnName(primaryKeys.get(i))).append(" = ?");
                params.add(primaryValues.get(primaryKeys.get(i)));
            }
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
                for (int i = 0; i < params.size(); i++) {
                    pstmt.setObject(i + 1, params.get(i));
                }
                return pstmt.executeUpdate();
            }
        } catch (Exception e) {
            log.error("删除数据失败（复合主键）", e);
            throw new BusinessException(ErrorCode.DATA_OPERATION_FAILED, "删除数据失败: " + e.getMessage());
        }
    }

    /**
     * 批量删除数据行（支持复合主键）
     */
    public int batchDeleteRowsWithCompositeKey(Long dataSourceId, String tableName, 
            List<String> primaryKeys, List<Map<String, Object>> primaryValuesArray) {
        if (primaryValuesArray == null || primaryValuesArray.isEmpty()) {
            return 0;
        }
        
        try (Connection conn = getConnection(dataSourceId)) {
            int totalDeleted = 0;
            
            // 对于复合主键，需要逐条删除（因为 IN 语法不适用于多列）
            StringBuilder sqlTemplate = new StringBuilder("DELETE FROM ")
                .append(escapeTableName(tableName))
                .append(" WHERE ");
            
            for (int i = 0; i < primaryKeys.size(); i++) {
                if (i > 0) sqlTemplate.append(" AND ");
                sqlTemplate.append(escapeColumnName(primaryKeys.get(i))).append(" = ?");
            }
            
            try (PreparedStatement pstmt = conn.prepareStatement(sqlTemplate.toString())) {
                for (Map<String, Object> primaryValues : primaryValuesArray) {
                    for (int i = 0; i < primaryKeys.size(); i++) {
                        pstmt.setObject(i + 1, primaryValues.get(primaryKeys.get(i)));
                    }
                    totalDeleted += pstmt.executeUpdate();
                }
            }
            
            return totalDeleted;
        } catch (Exception e) {
            log.error("批量删除数据失败（复合主键）", e);
            throw new BusinessException(ErrorCode.DATA_OPERATION_FAILED, "批量删除数据失败: " + e.getMessage());
        }
    }

    /**
     * 导入数据
     */
    public Map<String, Object> importData(MultipartFile file, Long dataSourceId, 
            String tableName, String format, String mode,
            String uniqueFields, String updateFieldMode, String updateFields) {
        Map<String, Object> result = new HashMap<>();
        int insertCount = 0;
        int updateCount = 0;
        int failCount = 0;
        List<String> errors = new ArrayList<>();
        
        try (Connection conn = getConnection(dataSourceId)) {
            // 如果是全量替换模式，先清空表
            if ("replace".equals(mode)) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("DELETE FROM " + escapeTableName(tableName));
                }
            }
            
            // 获取表结构
            List<Map<String, Object>> columns = getTableStructure(dataSourceId, tableName);
            List<String> columnNames = new ArrayList<>();
            for (Map<String, Object> col : columns) {
                columnNames.add((String) col.get("columnName"));
            }
            
            // 解析唯一字段和更新字段
            List<String> uniqueFieldList = new ArrayList<>();
            List<String> updateFieldList = new ArrayList<>();
            
            if ("increment".equals(mode) && uniqueFields != null && !uniqueFields.isEmpty()) {
                for (String field : uniqueFields.split(",")) {
                    if (!field.trim().isEmpty()) {
                        uniqueFieldList.add(field.trim());
                    }
                }
                
                if ("custom".equals(updateFieldMode) && updateFields != null && !updateFields.isEmpty()) {
                    for (String field : updateFields.split(",")) {
                        if (!field.trim().isEmpty()) {
                            updateFieldList.add(field.trim());
                        }
                    }
                }
            }
            
            // 解析文件
            List<Map<String, Object>> rows;
            if ("excel".equals(format)) {
                rows = parseExcel(file, columnNames);
            } else {
                rows = parseCsv(file, columnNames);
            }
            
            // 处理每行数据
            if ("increment".equals(mode) && !uniqueFieldList.isEmpty()) {
                // 增量导入：逐行检查+更新/插入
                for (Map<String, Object> row : rows) {
                    try {
                        boolean exists = checkRowExists(conn, tableName, row, uniqueFieldList);
                        if (exists) {
                            updateRowByUniqueFields(conn, tableName, row, uniqueFieldList, updateFieldList, updateFieldMode);
                            updateCount++;
                        } else {
                            insertRowDirect(conn, tableName, row);
                            insertCount++;
                        }
                    } catch (Exception e) {
                        failCount++;
                        if (errors.size() < 10) {
                            errors.add(e.getMessage());
                        }
                    }
                }
            } else if (!rows.isEmpty()) {
                // 追加或全量模式：批量插入，每500行提交一次
                final int BATCH_SIZE = 500;
                // 使用第一行的列集合构建PreparedStatement
                List<String> insertCols = new ArrayList<>();
                for (Map.Entry<String, Object> entry : rows.get(0).entrySet()) {
                    insertCols.add(entry.getKey());
                }
                
                StringBuilder batchSql = new StringBuilder("INSERT INTO ").append(escapeTableName(tableName)).append(" (");
                StringBuilder batchValues = new StringBuilder(" VALUES (");
                for (int ci = 0; ci < insertCols.size(); ci++) {
                    if (ci > 0) { batchSql.append(", "); batchValues.append(", "); }
                    batchSql.append(escapeColumnName(insertCols.get(ci)));
                    batchValues.append("?");
                }
                batchSql.append(")").append(batchValues).append(")");
                
                boolean origAutoCommit = conn.getAutoCommit();
                conn.setAutoCommit(false);
                try (PreparedStatement batchPs = conn.prepareStatement(batchSql.toString())) {
                    int batchCount = 0;
                    for (Map<String, Object> row : rows) {
                        try {
                            for (int ci = 0; ci < insertCols.size(); ci++) {
                                batchPs.setObject(ci + 1, row.get(insertCols.get(ci)));
                            }
                            batchPs.addBatch();
                            batchCount++;
                            
                            if (batchCount % BATCH_SIZE == 0) {
                                int[] results = batchPs.executeBatch();
                                for (int r : results) {
                                    if (r >= 0 || r == Statement.SUCCESS_NO_INFO) insertCount++;
                                    else failCount++;
                                }
                                conn.commit();
                            }
                        } catch (Exception e) {
                            failCount++;
                            if (errors.size() < 10) {
                                errors.add(e.getMessage());
                            }
                        }
                    }
                    // 提交剩余批次
                    if (batchCount % BATCH_SIZE != 0) {
                        int[] results = batchPs.executeBatch();
                        for (int r : results) {
                            if (r >= 0 || r == Statement.SUCCESS_NO_INFO) insertCount++;
                            else failCount++;
                        }
                        conn.commit();
                    }
                } catch (Exception e) {
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(origAutoCommit);
                }
            }
        } catch (Exception e) {
            log.error("导入数据失败", e);
            throw new BusinessException(ErrorCode.DATA_IMPORT_FAILED, "导入数据失败: " + e.getMessage());
        }
        
        result.put("insertCount", insertCount);
        result.put("updateCount", updateCount);
        result.put("successCount", insertCount + updateCount);
        result.put("failCount", failCount);
        result.put("errors", errors);
        return result;
    }
    
    /**
     * 检查行是否存在
     */
    private boolean checkRowExists(Connection conn, String tableName, Map<String, Object> row, List<String> uniqueFields) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM ")
            .append(escapeTableName(tableName))
            .append(" WHERE ");
        
        List<Object> params = new ArrayList<>();
        for (int i = 0; i < uniqueFields.size(); i++) {
            if (i > 0) sql.append(" AND ");
            String field = uniqueFields.get(i);
            Object value = row.get(field);
            if (value == null) {
                sql.append(escapeColumnName(field)).append(" IS NULL");
            } else {
                sql.append(escapeColumnName(field)).append(" = ?");
                params.add(value);
            }
        }
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }
    
    /**
     * 根据唯一字段更新行
     */
    private void updateRowByUniqueFields(Connection conn, String tableName, Map<String, Object> row, 
            List<String> uniqueFields, List<String> updateFieldList, String updateFieldMode) throws SQLException {
        StringBuilder sql = new StringBuilder("UPDATE ").append(escapeTableName(tableName)).append(" SET ");
        
        List<Object> params = new ArrayList<>();
        int setCount = 0;
        
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            String field = entry.getKey();
            // 跳过唯一字段
            if (uniqueFields.contains(field)) continue;
            // 如果是自定义更新字段模式，只更新指定字段
            if ("custom".equals(updateFieldMode) && !updateFieldList.isEmpty() && !updateFieldList.contains(field)) continue;
            
            if (setCount > 0) sql.append(", ");
            sql.append(escapeColumnName(field)).append(" = ?");
            params.add(entry.getValue());
            setCount++;
        }
        
        if (setCount == 0) return; // 没有需要更新的字段
        
        sql.append(" WHERE ");
        for (int i = 0; i < uniqueFields.size(); i++) {
            if (i > 0) sql.append(" AND ");
            String field = uniqueFields.get(i);
            Object value = row.get(field);
            if (value == null) {
                sql.append(escapeColumnName(field)).append(" IS NULL");
            } else {
                sql.append(escapeColumnName(field)).append(" = ?");
                params.add(value);
            }
        }
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            pstmt.executeUpdate();
        }
    }
    
    /**
     * 直接插入行（使用已有连接）
     */
    private void insertRowDirect(Connection conn, String tableName, Map<String, Object> data) throws SQLException {
        Map<String, Object> filteredData = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().toString().isEmpty()) {
                filteredData.put(entry.getKey(), entry.getValue());
            }
        }
        
        if (filteredData.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "没有有效的数据");
        }
        
        StringBuilder sql = new StringBuilder("INSERT INTO ").append(escapeTableName(tableName)).append(" (");
        StringBuilder values = new StringBuilder(" VALUES (");
        
        List<Object> params = new ArrayList<>();
        int i = 0;
        for (Map.Entry<String, Object> entry : filteredData.entrySet()) {
            if (i > 0) {
                sql.append(", ");
                values.append(", ");
            }
            sql.append(escapeColumnName(entry.getKey()));
            values.append("?");
            params.add(entry.getValue());
            i++;
        }
        sql.append(")").append(values).append(")");
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            for (int j = 0; j < params.size(); j++) {
                pstmt.setObject(j + 1, params.get(j));
            }
            pstmt.executeUpdate();
        }
    }

    /**
     * 解析Excel文件
     */
    private List<Map<String, Object>> parseExcel(MultipartFile file, List<String> columnNames) throws Exception {
        List<Map<String, Object>> rows = new ArrayList<>();
        
        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            
            // 读取表头
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) return rows;
            
            List<String> headers = new ArrayList<>();
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i);
                headers.add(cell != null ? cell.getStringCellValue() : "");
            }
            
            // 读取数据行
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                Map<String, Object> rowData = new LinkedHashMap<>();
                for (int j = 0; j < headers.size(); j++) {
                    String header = headers.get(j);
                    if (columnNames.contains(header)) {
                        Cell cell = row.getCell(j);
                        rowData.put(header, getCellValue(cell));
                    }
                }
                if (!rowData.isEmpty()) {
                    rows.add(rowData);
                }
            }
        }
        return rows;
    }

    /**
     * 获取单元格值
     */
    private Object getCellValue(Cell cell) {
        if (cell == null) return null;
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue();
                }
                double numValue = cell.getNumericCellValue();
                if (numValue == Math.floor(numValue)) {
                    return (long) numValue;
                }
                return numValue;
            case BOOLEAN:
                return cell.getBooleanCellValue();
            case FORMULA:
                try {
                    return cell.getNumericCellValue();
                } catch (Exception e) {
                    return cell.getStringCellValue();
                }
            default:
                return null;
        }
    }

    /**
     * 解析CSV文件
     */
    private List<Map<String, Object>> parseCsv(MultipartFile file, List<String> columnNames) throws Exception {
        List<Map<String, Object>> rows = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), "UTF-8"))) {
            String headerLine = reader.readLine();
            if (headerLine == null) return rows;
            
            String[] headers = headerLine.split(",");
            for (int i = 0; i < headers.length; i++) {
                headers[i] = headers[i].trim().replace("\"", "");
            }
            
            String line;
            while ((line = reader.readLine()) != null) {
                String[] values = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                Map<String, Object> rowData = new LinkedHashMap<>();
                
                for (int i = 0; i < headers.length && i < values.length; i++) {
                    String header = headers[i];
                    if (columnNames.contains(header)) {
                        String value = values[i].trim().replace("\"", "");
                        rowData.put(header, value.isEmpty() ? null : value);
                    }
                }
                if (!rowData.isEmpty()) {
                    rows.add(rowData);
                }
            }
        }
        return rows;
    }

    /**
     * 导出数据
     */
    public void exportData(Long dataSourceId, String tableName, String format, 
            String where, HttpServletResponse response) {
        try {
            // 查询所有数据
            Map<String, Object> queryResult = queryTableData(dataSourceId, tableName, 1, 100000, where, null);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> data = (List<Map<String, Object>>) queryResult.get("list");
            
            // 获取表结构
            List<Map<String, Object>> columns = getTableStructure(dataSourceId, tableName);
            
            if ("excel".equals(format)) {
                exportExcel(data, columns, tableName, response);
            } else {
                exportCsv(data, columns, tableName, response);
            }
        } catch (Exception e) {
            log.error("导出数据失败", e);
            throw new BusinessException(ErrorCode.DATA_EXPORT_FAILED, "导出数据失败: " + e.getMessage());
        }
    }

    /**
     * 导出Excel
     */
    private void exportExcel(List<Map<String, Object>> data, List<Map<String, Object>> columns, 
            String tableName, HttpServletResponse response) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(tableName);
            
            // 创建表头样式
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            
            // 写入表头
            Row headerRow = sheet.createRow(0);
            int colIndex = 0;
            for (Map<String, Object> col : columns) {
                Cell cell = headerRow.createCell(colIndex++);
                cell.setCellValue((String) col.get("columnName"));
                cell.setCellStyle(headerStyle);
            }
            
            // 写入数据
            int rowIndex = 1;
            for (Map<String, Object> row : data) {
                Row dataRow = sheet.createRow(rowIndex++);
                colIndex = 0;
                for (Map<String, Object> col : columns) {
                    Cell cell = dataRow.createCell(colIndex++);
                    Object value = row.get(col.get("columnName"));
                    if (value != null) {
                        if (value instanceof Number) {
                            cell.setCellValue(((Number) value).doubleValue());
                        } else if (value instanceof java.util.Date) {
                            cell.setCellValue((java.util.Date) value);
                        } else {
                            cell.setCellValue(value.toString());
                        }
                    }
                }
            }
            
            // 自动调整列宽
            for (int i = 0; i < columns.size(); i++) {
                sheet.autoSizeColumn(i);
            }
            
            // 设置响应头
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", 
                "attachment; filename=" + URLEncoder.encode(tableName + ".xlsx", "UTF-8"));
            
            workbook.write(response.getOutputStream());
        }
    }

    /**
     * 导出CSV
     */
    private void exportCsv(List<Map<String, Object>> data, List<Map<String, Object>> columns, 
            String tableName, HttpServletResponse response) throws Exception {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", 
            "attachment; filename=" + URLEncoder.encode(tableName + ".csv", "UTF-8"));
        
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(response.getOutputStream(), "UTF-8"))) {
            // 写入BOM
            writer.write('\ufeff');
            
            // 写入表头
            StringBuilder header = new StringBuilder();
            for (int i = 0; i < columns.size(); i++) {
                if (i > 0) header.append(",");
                header.append("\"").append(columns.get(i).get("columnName")).append("\"");
            }
            writer.println(header);
            
            // 写入数据
            for (Map<String, Object> row : data) {
                StringBuilder line = new StringBuilder();
                for (int i = 0; i < columns.size(); i++) {
                    if (i > 0) line.append(",");
                    Object value = row.get(columns.get(i).get("columnName"));
                    if (value != null) {
                        String strValue = value.toString().replace("\"", "\"\"");
                        line.append("\"").append(strValue).append("\"");
                    } else {
                        line.append("");
                    }
                }
                writer.println(line);
            }
        }
    }

    /**
     * 执行SQL
     */
    public Map<String, Object> executeSql(Long dataSourceId, String sql) {
        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();
        
        try (Connection conn = getConnection(dataSourceId);
             Statement stmt = conn.createStatement()) {
            
            String trimmedSql = sql.trim().toUpperCase();
            
            if (trimmedSql.startsWith("SELECT")) {
                try (ResultSet rs = stmt.executeQuery(sql)) {
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    
                    List<String> columnList = new ArrayList<>();
                    for (int i = 1; i <= columnCount; i++) {
                        columnList.add(metaData.getColumnName(i));
                    }
                    
                    List<Map<String, Object>> data = new ArrayList<>();
                    while (rs.next()) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        for (int i = 1; i <= columnCount; i++) {
                            row.put(metaData.getColumnName(i), rs.getObject(i));
                        }
                        data.add(row);
                    }
                    
                    result.put("type", "SELECT");
                    result.put("columns", columnList);
                    result.put("data", data);
                    result.put("success", true);
                    result.put("message", "查询成功，返回 " + data.size() + " 条记录");
                }
            } else {
                int affected = stmt.executeUpdate(sql);
                result.put("type", "UPDATE");
                result.put("success", true);
                result.put("message", "执行成功，影响 " + affected + " 行");
            }
        } catch (Exception e) {
            log.error("执行SQL失败", e);
            result.put("success", false);
            result.put("message", "执行失败: " + e.getMessage());
        }
        
        result.put("executeTime", System.currentTimeMillis() - startTime);
        return result;
    }

    /**
     * 带字段映射的数据导入
     * @param mapping 源字段名 -> 目标字段名 的映射
     * @param progressCallback 进度回调 (current, total)
     */
    public Map<String, Object> importDataWithMapping(
            org.springframework.web.multipart.MultipartFile file,
            Long dataSourceId, String tableName,
            Map<String, String> mapping,
            java.util.function.BiConsumer<Integer, Integer> progressCallback) {
        
        Map<String, Object> result = new HashMap<>();
        int successCount = 0, failCount = 0;
        
        try {
            // 解析文件
            String fileName = file.getOriginalFilename();
            String ext = fileName != null && fileName.contains(".") ? fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase() : "";
            
            List<Map<String, Object>> data;
            List<String> headers;
            try (java.io.InputStream is = file.getInputStream()) {
                if ("xlsx".equals(ext) || "xls".equals(ext)) {
                    org.apache.poi.ss.usermodel.Workbook wb = org.apache.poi.ss.usermodel.WorkbookFactory.create(is);
                    org.apache.poi.ss.usermodel.Sheet sheet = wb.getSheetAt(0);
                    org.apache.poi.ss.usermodel.Row headerRow = sheet.getRow(0);
                    headers = new java.util.ArrayList<>();
                    int colCount = headerRow.getLastCellNum();
                    for (int i = 0; i < colCount; i++) {
                        org.apache.poi.ss.usermodel.Cell cell = headerRow.getCell(i);
                        headers.add(cell != null ? cell.toString().trim() : "col_" + i);
                    }
                    data = new java.util.ArrayList<>();
                    for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                        org.apache.poi.ss.usermodel.Row row = sheet.getRow(r);
                        if (row == null) continue;
                        Map<String, Object> rowData = new java.util.LinkedHashMap<>();
                        for (int i = 0; i < headers.size(); i++) {
                            org.apache.poi.ss.usermodel.Cell cell = row.getCell(i);
                            rowData.put(headers.get(i), cell != null ? cell.toString() : null);
                        }
                        data.add(rowData);
                    }
                    wb.close();
                } else {
                    // CSV
                    java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8));
                    String headerLine = br.readLine();
                    if (headerLine != null && headerLine.startsWith("\uFEFF")) headerLine = headerLine.substring(1);
                    headers = java.util.Arrays.asList(headerLine.split(","));
                    data = new java.util.ArrayList<>();
                    String line;
                    while ((line = br.readLine()) != null) {
                        String[] values = line.split(",", -1);
                        Map<String, Object> rowData = new java.util.LinkedHashMap<>();
                        for (int i = 0; i < headers.size() && i < values.length; i++) {
                            rowData.put(headers.get(i).trim(), values[i].trim());
                        }
                        data.add(rowData);
                    }
                }
            }
            
            int total = data.size();
            if (progressCallback != null) progressCallback.accept(0, total);
            
            // 执行插入
            try (Connection conn = getConnection(dataSourceId)) {
                conn.setAutoCommit(false);
                
                // 构建INSERT SQL
                List<String> targetCols = new java.util.ArrayList<>(mapping.values());
                List<String> sourceCols = new java.util.ArrayList<>(mapping.keySet());
                
                String insertSql = "INSERT INTO " + escapeTableName(tableName) + " (" +
                    targetCols.stream().map(this::escapeColumnName).collect(java.util.stream.Collectors.joining(", ")) +
                    ") VALUES (" + targetCols.stream().map(c -> "?").collect(java.util.stream.Collectors.joining(", ")) + ")";
                
                try (java.sql.PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    int batchCount = 0;
                    for (int i = 0; i < data.size(); i++) {
                        Map<String, Object> row = data.get(i);
                        try {
                            for (int j = 0; j < sourceCols.size(); j++) {
                                Object val = row.get(sourceCols.get(j));
                                ps.setObject(j + 1, val);
                            }
                            ps.addBatch();
                            batchCount++;
                            
                            if (batchCount >= 500) {
                                ps.executeBatch();
                                successCount += batchCount;
                                batchCount = 0;
                                conn.commit();
                            }
                        } catch (Exception e) {
                            failCount++;
                        }
                        
                        if (progressCallback != null && (i + 1) % 100 == 0) {
                            progressCallback.accept(i + 1, total);
                        }
                    }
                    if (batchCount > 0) {
                        ps.executeBatch();
                        successCount += batchCount;
                        conn.commit();
                    }
                }
            }
            
            if (progressCallback != null) progressCallback.accept(total, total);
            
            result.put("success", true);
            result.put("successCount", successCount);
            result.put("failCount", failCount);
            result.put("totalCount", total);
        } catch (Exception e) {
            log.error("字段映射导入失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return result;
    }
    
    /**
     * 建表（DDL）
     */
    public Map<String, Object> createTable(Long dataSourceId, String tableName,
            List<Map<String, Object>> columns, String primaryKey, String tableComment) {
        Map<String, Object> result = new HashMap<>();
        try (Connection conn = getConnection(dataSourceId)) {
            StringBuilder sql = new StringBuilder("CREATE TABLE ").append(escapeTableName(tableName)).append(" (\n");
            for (int i = 0; i < columns.size(); i++) {
                Map<String, Object> col = columns.get(i);
                String colName = (String) col.get("columnName");
                String dataType = (String) col.get("dataType");
                Object colSize = col.get("columnSize");
                Boolean nullable = col.get("nullable") == null || Boolean.TRUE.equals(col.get("nullable"));
                String defaultValue = (String) col.get("defaultValue");
                String comment = (String) col.get("comment");
                
                if (i > 0) sql.append(",\n");
                sql.append("  ").append(escapeColumnName(colName)).append(" ").append(dataType);
                if (colSize != null && !colSize.toString().isEmpty()) {
                    sql.append("(").append(colSize).append(")");
                }
                if (!nullable) sql.append(" NOT NULL");
                if (defaultValue != null && !defaultValue.isEmpty()) {
                    sql.append(" DEFAULT '").append(defaultValue.replace("'", "''")).append("'");
                }
                if (comment != null && !comment.isEmpty()) {
                    sql.append(" COMMENT '").append(comment.replace("'", "''")).append("'");
                }
            }
            if (primaryKey != null && !primaryKey.isEmpty()) {
                sql.append(",\n  PRIMARY KEY (").append(escapeColumnName(primaryKey)).append(")");
            }
            sql.append("\n)");
            if (tableComment != null && !tableComment.isEmpty()) {
                sql.append(" COMMENT='").append(tableComment.replace("'", "''")).append("'");
            }
            
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(sql.toString());
            }
            result.put("success", true);
            result.put("message", "建表成功: " + tableName);
            result.put("sql", sql.toString());
        } catch (Exception e) {
            log.error("建表失败", e);
            result.put("success", false);
            result.put("message", "建表失败: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * 改表（DDL）— ADD/DROP/MODIFY
     */
    public Map<String, Object> alterTable(Long dataSourceId, String tableName,
            String action, List<Map<String, Object>> columns) {
        Map<String, Object> result = new HashMap<>();
        try (Connection conn = getConnection(dataSourceId)) {
            List<String> sqls = new ArrayList<>();
            
            if ("ADD".equalsIgnoreCase(action)) {
                for (Map<String, Object> col : columns) {
                    StringBuilder sb = new StringBuilder("ALTER TABLE ").append(escapeTableName(tableName))
                        .append(" ADD COLUMN ").append(escapeColumnName((String) col.get("columnName")))
                        .append(" ").append(col.get("dataType"));
                    Object colSize = col.get("columnSize");
                    if (colSize != null && !colSize.toString().isEmpty()) sb.append("(").append(colSize).append(")");
                    String comment = (String) col.get("comment");
                    if (comment != null && !comment.isEmpty()) sb.append(" COMMENT '").append(comment.replace("'", "''")).append("'");
                    sqls.add(sb.toString());
                }
            } else if ("DROP".equalsIgnoreCase(action)) {
                for (Map<String, Object> col : columns) {
                    sqls.add("ALTER TABLE " + escapeTableName(tableName) + " DROP COLUMN " + escapeColumnName((String) col.get("columnName")));
                }
            } else if ("MODIFY".equalsIgnoreCase(action)) {
                for (Map<String, Object> col : columns) {
                    StringBuilder sb = new StringBuilder("ALTER TABLE ").append(escapeTableName(tableName))
                        .append(" MODIFY COLUMN ").append(escapeColumnName((String) col.get("columnName")))
                        .append(" ").append(col.get("dataType"));
                    Object colSize = col.get("columnSize");
                    if (colSize != null && !colSize.toString().isEmpty()) sb.append("(").append(colSize).append(")");
                    sqls.add(sb.toString());
                }
            }
            
            try (Statement stmt = conn.createStatement()) {
                for (String sql : sqls) {
                    stmt.executeUpdate(sql);
                }
            }
            result.put("success", true);
            result.put("message", "表结构修改成功");
            result.put("executedSqls", sqls);
        } catch (Exception e) {
            log.error("改表失败", e);
            result.put("success", false);
            result.put("message", "改表失败: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * 转义表名
     */
    private String escapeTableName(String tableName) {
        return "`" + tableName.replace("`", "``") + "`";
    }

    /**
     * 转义列名
     */
    private String escapeColumnName(String columnName) {
        return "`" + columnName.replace("`", "``") + "`";
    }
}
