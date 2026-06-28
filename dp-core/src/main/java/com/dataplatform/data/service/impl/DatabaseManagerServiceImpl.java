package com.dataplatform.data.service.impl;

import com.dataplatform.data.service.DatabaseManagerService;
import com.dataplatform.data.service.DbConnectionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * 数据库管理服务实现类
 */
@Slf4j
@Service
public class DatabaseManagerServiceImpl implements DatabaseManagerService {

    @org.springframework.beans.factory.annotation.Value("${db.connection.timeout:30}")
    private int connectionTimeout;
    
    @org.springframework.beans.factory.annotation.Value("${db.query.timeout:300}")
    private int queryTimeout;
    
    @org.springframework.beans.factory.annotation.Value("${query.db-manager.max-rows:5000}")
    private int maxResultRows;
    
    @org.springframework.beans.factory.annotation.Value("${connection.idle-timeout-minutes:30}")
    private int connectionIdleTimeoutMinutes;
    
    @org.springframework.beans.factory.annotation.Value("${connection.max-per-user:5}")
    private int maxConnectionsPerUser;

    // 代理连接池
    private final Map<String, Connection> proxyConnections = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> connectionInfoCache = new ConcurrentHashMap<>();
    private final Map<String, Long> connectionLastActiveTime = new ConcurrentHashMap<>();  // 连接最后活跃时间
    
    // 定时清理任务
    private ScheduledExecutorService cleanupScheduler;

    @Autowired
    private DbConnectionUtil dbConnectionUtil;
    
    @Autowired
    private com.dataplatform.data.mapper.SqlHistoryMapper sqlHistoryMapper;

    /**
     * 初始化连接清理定时任务
     */
    @PostConstruct
    public void init() {
        cleanupScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "db-connection-cleanup");
            t.setDaemon(true);
            return t;
        });
        // 每5分钟清理一次过期连接
        cleanupScheduler.scheduleAtFixedRate(this::cleanupIdleConnections, 5, 5, TimeUnit.MINUTES);
        log.info("数据库连接清理任务已启动");
    }

    /**
     * 销毁时关闭所有连接
     */
    @PreDestroy
    public void destroy() {
        log.info("正在关闭所有代理连接...");
        if (cleanupScheduler != null) {
            cleanupScheduler.shutdown();
        }
        // 关闭所有连接
        for (String sessionId : new ArrayList<>(proxyConnections.keySet())) {
            closeProxyConnection(sessionId);
        }
        log.info("所有代理连接已关闭");
    }

    /**
     * 清理空闲超时的连接
     */
    private void cleanupIdleConnections() {
        long now = System.currentTimeMillis();
        long timeoutMillis = connectionIdleTimeoutMinutes * 60 * 1000L;
        int cleanedCount = 0;
        
        for (Map.Entry<String, Long> entry : connectionLastActiveTime.entrySet()) {
            String sessionId = entry.getKey();
            Long lastActiveTime = entry.getValue();
            
            if (now - lastActiveTime > timeoutMillis) {
                log.info("连接空闲超时，自动关闭: {}", sessionId);
                closeProxyConnection(sessionId);
                cleanedCount++;
            }
        }
        
        if (cleanedCount > 0) {
            log.info("清理了 {} 个空闲连接，当前活跃连接数: {}", cleanedCount, proxyConnections.size());
        }
    }

    /**
     * 更新连接最后活跃时间
     */
    private void updateConnectionActiveTime(String sessionId) {
        connectionLastActiveTime.put(sessionId, System.currentTimeMillis());
    }

    @Override
    public String createProxyConnection(Map<String, Object> connectionInfo) throws Exception {
        // 检查连接数限制
        if (proxyConnections.size() >= maxConnectionsPerUser * 10) {
            log.warn("连接数过多，尝试清理空闲连接");
            cleanupIdleConnections();
        }
        
        String sessionId = generateSessionId();
        Connection conn = createConnection(connectionInfo);

        if (!conn.isValid(5)) {
            closeConnection(conn);
            throw new SQLException("数据库连接无效");
        }

        proxyConnections.put(sessionId, conn);
        connectionInfoCache.put(sessionId, connectionInfo);
        connectionLastActiveTime.put(sessionId, System.currentTimeMillis());

        log.info("创建代理连接成功，会话ID: {}, 数据库: {}, 当前连接数: {}", 
                 sessionId, connectionInfo.get("dbName"), proxyConnections.size());
        return sessionId;
    }

    @Override
    public void closeProxyConnection(String sessionId) {
        Connection conn = proxyConnections.remove(sessionId);
        connectionInfoCache.remove(sessionId);
        connectionLastActiveTime.remove(sessionId);
        if (conn != null) {
            closeConnection(conn);
            log.info("代理连接已关闭，会话ID: {}, 剩余连接数: {}", sessionId, proxyConnections.size());
        }
    }

    @Override
    public String testConnection(Map<String, Object> connectionInfo) throws Exception {
        Connection conn = null;
        try {
            conn = createConnection(connectionInfo);
            DatabaseMetaData metaData = conn.getMetaData();
            String dbProductName = metaData.getDatabaseProductName();
            String dbVersion = metaData.getDatabaseProductVersion();
            return "连接成功 - " + dbProductName + " " + dbVersion;
        } finally {
            closeConnection(conn);
        }
    }

    @Override
    public List<Map<String, Object>> getTablesProxy(String sessionId) throws Exception {
        Connection conn = getProxyConnection(sessionId);
        Map<String, Object> connectionInfo = connectionInfoCache.get(sessionId);
        List<Map<String, Object>> tables = new ArrayList<>();

        String dbType = (String) connectionInfo.get("dbType");
        String dbName = (String) connectionInfo.get("dbName");
        String username = (String) connectionInfo.get("username");

        DatabaseMetaData metaData = conn.getMetaData();
        ResultSet rs = null;

        try {
            String upperDbType = dbType.toUpperCase();
            switch (upperDbType) {
                case "ORACLE":
                case "DM":
                    // Oracle和达梦使用用户名作为schema
                    rs = metaData.getTables(null, username != null ? username.toUpperCase() : null, "%", new String[]{"TABLE"});
                    break;
                case "MYSQL":
                case "MARIADB":
                case "TIDB":
                case "OCEANBASE":
                    rs = metaData.getTables(dbName, null, "%", new String[]{"TABLE"});
                    break;
                case "POSTGRESQL":
                case "KINGBASE":
                    rs = metaData.getTables(dbName, "public", "%", new String[]{"TABLE"});
                    break;
                case "SQLSERVER":
                    rs = metaData.getTables(dbName, "dbo", "%", new String[]{"TABLE"});
                    break;
                case "CLICKHOUSE":
                    rs = metaData.getTables(dbName, null, "%", new String[]{"TABLE"});
                    break;
                case "PRESTO":
                    // Presto/Trino: catalog.schema
                    rs = metaData.getTables(null, null, "%", new String[]{"TABLE"});
                    break;
                case "SQLITE":
                    rs = metaData.getTables(null, null, "%", new String[]{"TABLE"});
                    break;
                case "GBASE":
                    rs = metaData.getTables(dbName, null, "%", new String[]{"TABLE"});
                    break;
                default:
                    rs = metaData.getTables(dbName, null, "%", new String[]{"TABLE"});
            }

            while (rs.next()) {
                Map<String, Object> table = new LinkedHashMap<>();
                table.put("tableName", rs.getString("TABLE_NAME"));
                table.put("remarks", rs.getString("REMARKS"));
                tables.add(table);
            }
        } finally {
            closeResultSet(rs);
        }

        log.info("获取表列表成功，共 {} 个表", tables.size());
        return tables;
    }


    @Override
    public List<Map<String, Object>> getViewsProxy(String sessionId) throws Exception {
        Connection conn = getProxyConnection(sessionId);
        Map<String, Object> connectionInfo = connectionInfoCache.get(sessionId);
        List<Map<String, Object>> views = new ArrayList<>();

        String dbType = (String) connectionInfo.get("dbType");
        String dbName = (String) connectionInfo.get("dbName");
        String username = (String) connectionInfo.get("username");

        DatabaseMetaData metaData = conn.getMetaData();
        ResultSet rs = null;

        try {
            String upperDbType = dbType.toUpperCase();
            switch (upperDbType) {
                case "ORACLE":
                case "DM":
                    rs = metaData.getTables(null, username != null ? username.toUpperCase() : null, "%", new String[]{"VIEW"});
                    break;
                case "MYSQL":
                case "MARIADB":
                case "TIDB":
                case "OCEANBASE":
                    rs = metaData.getTables(dbName, null, "%", new String[]{"VIEW"});
                    break;
                case "POSTGRESQL":
                case "KINGBASE":
                    rs = metaData.getTables(dbName, "public", "%", new String[]{"VIEW"});
                    break;
                case "SQLSERVER":
                    rs = metaData.getTables(dbName, "dbo", "%", new String[]{"VIEW"});
                    break;
                default:
                    rs = metaData.getTables(dbName, null, "%", new String[]{"VIEW"});
            }

            while (rs.next()) {
                Map<String, Object> view = new LinkedHashMap<>();
                view.put("viewName", rs.getString("TABLE_NAME"));
                view.put("remarks", rs.getString("REMARKS"));
                views.add(view);
            }
        } finally {
            closeResultSet(rs);
        }

        return views;
    }

    @Override
    public List<Map<String, Object>> getProceduresProxy(String sessionId) throws Exception {
        Connection conn = getProxyConnection(sessionId);
        Map<String, Object> connectionInfo = connectionInfoCache.get(sessionId);
        List<Map<String, Object>> procedures = new ArrayList<>();

        String dbType = (String) connectionInfo.get("dbType");
        String dbName = (String) connectionInfo.get("dbName");
        String username = (String) connectionInfo.get("username");

        DatabaseMetaData metaData = conn.getMetaData();
        ResultSet rs = null;

        try {
            String upperDbType = dbType.toUpperCase();
            switch (upperDbType) {
                case "ORACLE":
                case "DM":
                    rs = metaData.getProcedures(null, username != null ? username.toUpperCase() : null, "%");
                    break;
                case "POSTGRESQL":
                case "KINGBASE":
                    rs = metaData.getProcedures(dbName, "public", "%");
                    break;
                case "SQLSERVER":
                    rs = metaData.getProcedures(dbName, "dbo", "%");
                    break;
                default:
                    rs = metaData.getProcedures(dbName, null, "%");
            }

            while (rs.next()) {
                Map<String, Object> procedure = new LinkedHashMap<>();
                procedure.put("procedureName", rs.getString("PROCEDURE_NAME"));
                procedure.put("procedureType", rs.getShort("PROCEDURE_TYPE"));
                procedure.put("remarks", rs.getString("REMARKS"));
                procedures.add(procedure);
            }
        } finally {
            closeResultSet(rs);
        }

        return procedures;
    }

    @Override
    public List<Map<String, Object>> getTableStructureProxy(String sessionId, Map<String, Object> params) throws Exception {
        Connection conn = getProxyConnection(sessionId);
        Map<String, Object> connectionInfo = connectionInfoCache.get(sessionId);
        List<Map<String, Object>> columns = new ArrayList<>();

        String tableName = (String) params.get("tableName");
        String dbType = (String) connectionInfo.get("dbType");
        String dbName = (String) connectionInfo.get("dbName");
        String username = (String) connectionInfo.get("username");

        DatabaseMetaData metaData = conn.getMetaData();
        ResultSet rs = null;

        try {
            if ("ORACLE".equalsIgnoreCase(dbType)) {
                rs = metaData.getColumns(null, username != null ? username.toUpperCase() : null, tableName.toUpperCase(), "%");
            } else {
                rs = metaData.getColumns(dbName, null, tableName, "%");
            }

            while (rs.next()) {
                Map<String, Object> column = new HashMap<>();
                column.put("columnName", rs.getString("COLUMN_NAME"));
                column.put("dataType", rs.getString("TYPE_NAME"));
                column.put("columnSize", rs.getInt("COLUMN_SIZE"));
                column.put("nullable", rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable ? "YES" : "NO");
                column.put("remarks", rs.getString("REMARKS"));
                column.put("defaultValue", rs.getString("COLUMN_DEF"));
                columns.add(column);
            }
        } finally {
            closeResultSet(rs);
        }

        // 获取主键信息
        ResultSet pkRs = null;
        try {
            if ("ORACLE".equalsIgnoreCase(dbType)) {
                pkRs = metaData.getPrimaryKeys(null, username != null ? username.toUpperCase() : null, tableName.toUpperCase());
            } else {
                pkRs = metaData.getPrimaryKeys(dbName, null, tableName);
            }

            Set<String> primaryKeys = new HashSet<>();
            while (pkRs.next()) {
                primaryKeys.add(pkRs.getString("COLUMN_NAME"));
            }

            for (Map<String, Object> column : columns) {
                column.put("isPrimaryKey", primaryKeys.contains(column.get("columnName")) ? "YES" : "NO");
            }
        } finally {
            closeResultSet(pkRs);
        }

        return columns;
    }

    @Override
    public Map<String, Object> queryTableDataProxy(String sessionId, Map<String, Object> params) throws Exception {
        Connection conn = getProxyConnection(sessionId);
        Map<String, Object> connectionInfo = connectionInfoCache.get(sessionId);
        Map<String, Object> result = new HashMap<>();

        String tableName = (String) params.get("tableName");
        Integer page = params.get("page") != null ? ((Number) params.get("page")).intValue() : 1;
        Integer pageSize = params.get("pageSize") != null ? ((Number) params.get("pageSize")).intValue() : 100;
        String whereClause = (String) params.get("whereClause");
        String dbType = (String) connectionInfo.get("dbType");

        if (page < 1) page = 1;
        if (pageSize < 1) pageSize = 100;
        if (pageSize > maxResultRows) pageSize = maxResultRows;

        String baseQuery = "SELECT * FROM " + escapeIdentifier(tableName, dbType);
        String countQuery = "SELECT COUNT(*) as total FROM " + escapeIdentifier(tableName, dbType);

        if (whereClause != null && !whereClause.trim().isEmpty()) {
            baseQuery += " WHERE " + whereClause;
            countQuery += " WHERE " + whereClause;
        }

        Statement stmt = null;
        ResultSet rs = null;

        try {
            stmt = conn.createStatement();
            stmt.setQueryTimeout(queryTimeout);

            // 获取总数
            rs = stmt.executeQuery(countQuery);
            int total = 0;
            if (rs.next()) {
                total = rs.getInt(1);
            }
            rs.close();

            // 分页查询
            String pagedQuery = buildPagedQuery(baseQuery, dbType, page, pageSize);
            rs = stmt.executeQuery(pagedQuery);

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            List<String> columnNames = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                String colName = metaData.getColumnName(i);
                if (!"RN".equalsIgnoreCase(colName) && !"ROWNUM".equalsIgnoreCase(colName)) {
                    columnNames.add(colName);
                }
            }

            List<Map<String, Object>> rows = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (String colName : columnNames) {
                    Object value = rs.getObject(colName);
                    if (value instanceof Clob) {
                        Clob clob = (Clob) value;
                        value = clob.getSubString(1, (int) Math.min(clob.length(), 1000));
                    } else if (value instanceof Blob) {
                        value = "[BLOB数据]";
                    } else if (value instanceof byte[]) {
                        value = "[二进制数据]";
                    }
                    row.put(colName, value);
                }
                rows.add(row);
            }

            result.put("columns", columnNames);
            result.put("data", rows);
            result.put("total", total);
            result.put("page", page);
            result.put("pageSize", pageSize);

        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
        }

        return result;
    }


    @Override
    public Map<String, Object> executeSqlProxy(String sessionId, Map<String, Object> params) throws Exception {
        Connection conn = getProxyConnection(sessionId);
        Map<String, Object> connectionInfo = connectionInfoCache.get(sessionId);
        Map<String, Object> result = new HashMap<>();

        String sql = (String) params.get("sql");
        if (sql == null || sql.trim().isEmpty()) {
            throw new IllegalArgumentException("SQL语句不能为空");
        }

        String dbType = (String) connectionInfo.get("dbType");
        
        // 预处理：移除 DELIMITER 语句（JDBC 不支持）
        sql = sql.replaceAll("(?i)DELIMITER\\s+[^\\s;]+\\s*;?", "").trim();
        
        // 检查是否是存储过程/函数创建语句
        String upperSql = sql.trim().toUpperCase();
        boolean isProcedureCreation = upperSql.contains("CREATE PROCEDURE") || 
                                       upperSql.contains("CREATE FUNCTION") ||
                                       upperSql.contains("CREATE OR REPLACE PROCEDURE") ||
                                       upperSql.contains("CREATE OR REPLACE FUNCTION") ||
                                       upperSql.contains("CREATE DEFINER");

        Statement stmt = null;
        ResultSet rs = null;

        try {
            stmt = conn.createStatement();
            stmt.setQueryTimeout(queryTimeout);
            stmt.setMaxRows(maxResultRows);

            long startTime = System.currentTimeMillis();
            
            if (isProcedureCreation) {
                // 存储过程/函数创建需要特殊处理
                List<String> messages = new ArrayList<>();
                
                // 1. 先检查并执行 DROP 语句
                String dropSql = extractDropStatement(sql);
                if (dropSql != null) {
                    log.info("执行删除语句: {}", dropSql);
                    try {
                        stmt.execute(dropSql);
                        messages.add("删除旧对象成功");
                    } catch (SQLException e) {
                        // 忽略"不存在"的错误
                        if (!e.getMessage().toLowerCase().contains("does not exist") &&
                            !e.getMessage().toLowerCase().contains("doesn't exist")) {
                            log.warn("删除旧对象失败: {}", e.getMessage());
                        }
                    }
                }
                
                // 2. 提取并执行 CREATE 语句
                String procedureSql = extractProcedureStatement(sql);
                log.info("执行存储过程创建: {}", procedureSql);
                
                stmt.execute(procedureSql);
                messages.add("创建成功");
                
                long endTime = System.currentTimeMillis();
                result.put("executeTime", endTime - startTime);
                result.put("type", "UPDATE");
                result.put("affectedRows", 0);
                result.put("message", "存储过程/函数" + String.join("，", messages));
                result.put("success", true);
                
            } else {
                // 普通 SQL 执行 - 支持多条语句
                // 智能分割SQL语句（考虑字符串中的分号）
                List<String> sqlStatements = splitSqlStatements(sql);
                
                if (sqlStatements.size() == 1) {
                    // 单条语句，直接执行
                    String singleSql = sqlStatements.get(0).trim();
                    boolean isQuery = stmt.execute(singleSql);
                    long endTime = System.currentTimeMillis();

                    result.put("executeTime", endTime - startTime);

                    if (isQuery) {
                        rs = stmt.getResultSet();
                        ResultSetMetaData metaData = rs.getMetaData();
                        int columnCount = metaData.getColumnCount();

                        List<String> columnNames = new ArrayList<>();
                        for (int i = 1; i <= columnCount; i++) {
                            columnNames.add(metaData.getColumnName(i));
                        }

                        List<Map<String, Object>> rows = new ArrayList<>();
                        int rowCount = 0;
                        while (rs.next() && rowCount < maxResultRows) {
                            Map<String, Object> row = new LinkedHashMap<>();
                            for (int i = 1; i <= columnCount; i++) {
                                Object value = rs.getObject(i);
                                if (value instanceof Clob) {
                                    Clob clob = (Clob) value;
                                    value = clob.getSubString(1, (int) Math.min(clob.length(), 1000));
                                } else if (value instanceof Blob) {
                                    value = "[BLOB数据]";
                                } else if (value instanceof byte[]) {
                                    value = "[二进制数据]";
                                }
                                row.put(metaData.getColumnName(i), value);
                            }
                            rows.add(row);
                            rowCount++;
                        }

                        result.put("type", "SELECT");
                        result.put("columns", columnNames);
                        result.put("data", rows);
                        result.put("rowCount", rows.size());
                        result.put("message", "查询成功，返回 " + rows.size() + " 行");
                        result.put("success", true);
                    } else {
                        int updateCount = stmt.getUpdateCount();
                        result.put("type", "UPDATE");
                        result.put("affectedRows", updateCount);
                        result.put("message", "执行成功，影响 " + updateCount + " 行");
                        result.put("success", true);
                    }
                } else {
                    // 多条语句，逐条执行
                    int totalAffected = 0;
                    int successCount = 0;
                    Map<String, Object> lastQueryResult = null;
                    
                    for (String singleSql : sqlStatements) {
                        singleSql = singleSql.trim();
                        if (singleSql.isEmpty()) continue;
                        
                        log.info("执行SQL语句: {}", singleSql);
                        
                        boolean isQuery = stmt.execute(singleSql);
                        successCount++;
                        
                        if (isQuery) {
                            rs = stmt.getResultSet();
                            ResultSetMetaData metaData = rs.getMetaData();
                            int columnCount = metaData.getColumnCount();

                            List<String> columnNames = new ArrayList<>();
                            for (int i = 1; i <= columnCount; i++) {
                                columnNames.add(metaData.getColumnName(i));
                            }

                            List<Map<String, Object>> rows = new ArrayList<>();
                            while (rs.next() && rows.size() < maxResultRows) {
                                Map<String, Object> row = new LinkedHashMap<>();
                                for (int i = 1; i <= columnCount; i++) {
                                    Object value = rs.getObject(i);
                                    if (value instanceof Clob) {
                                        Clob clob = (Clob) value;
                                        value = clob.getSubString(1, (int) Math.min(clob.length(), 1000));
                                    } else if (value instanceof Blob) {
                                        value = "[BLOB数据]";
                                    } else if (value instanceof byte[]) {
                                        value = "[二进制数据]";
                                    }
                                    row.put(metaData.getColumnName(i), value);
                                }
                                rows.add(row);
                            }
                            rs.close();
                            
                            lastQueryResult = new HashMap<>();
                            lastQueryResult.put("columns", columnNames);
                            lastQueryResult.put("data", rows);
                            lastQueryResult.put("rowCount", rows.size());
                        } else {
                            int count = stmt.getUpdateCount();
                            if (count >= 0) totalAffected += count;
                        }
                    }
                    
                    long endTime = System.currentTimeMillis();
                    result.put("executeTime", endTime - startTime);
                    
                    // 如果最后一条是查询，返回查询结果
                    if (lastQueryResult != null) {
                        result.put("type", "SELECT");
                        result.putAll(lastQueryResult);
                        result.put("message", "执行成功 " + successCount + " 条语句，最后查询返回 " + lastQueryResult.get("rowCount") + " 行");
                    } else {
                        result.put("type", "UPDATE");
                        result.put("affectedRows", totalAffected);
                        result.put("message", "执行成功 " + successCount + " 条语句，共影响 " + totalAffected + " 行");
                    }
                    result.put("success", true);
                }
            }

        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
        }

        // 异步记录SQL历史
        try {
            saveSqlHistory(sessionId, connectionInfo, (String) params.get("sql"), result);
        } catch (Exception historyEx) {
            log.warn("保存SQL历史记录失败: {}", historyEx.getMessage());
        }

        return result;
    }
    
    /**
     * 保存SQL执行历史
     */
    private void saveSqlHistory(String sessionId, Map<String, Object> connectionInfo, 
                                 String sql, Map<String, Object> result) {
        com.dataplatform.data.entity.SqlHistory history = new com.dataplatform.data.entity.SqlHistory();
        history.setSessionId(sessionId);
        history.setDbType((String) connectionInfo.get("dbType"));
        history.setDbName((String) connectionInfo.get("dbName"));
        history.setSqlContent(sql != null && sql.length() > 5000 ? sql.substring(0, 5000) : sql);
        history.setSqlType((String) result.get("type"));
        history.setStatus(Boolean.TRUE.equals(result.get("success")) ? "success" : "failed");
        history.setAffectedRows(result.get("affectedRows") != null ? 
                ((Number) result.get("affectedRows")).intValue() : null);
        history.setExecuteTime(result.get("executeTime") != null ? 
                ((Number) result.get("executeTime")).longValue() : null);
        history.setExecuteAt(new java.util.Date());
        sqlHistoryMapper.insert(history);
    }

    @Override
    public Map<String, Object> getSqlHistory(String sessionId, String keyword, String status,
                                              Integer page, Integer pageSize) {
        if (page == null || page < 1) page = 1;
        if (pageSize == null || pageSize < 1) pageSize = 20;
        int offset = (page - 1) * pageSize;
        
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("list", sqlHistoryMapper.selectList(sessionId, null, keyword, status, offset, pageSize));
        result.put("total", sqlHistoryMapper.count(sessionId, null, keyword, status));
        result.put("page", page);
        result.put("pageSize", pageSize);
        return result;
    }

    @Override
    public void clearSqlHistory(String sessionId) {
        sqlHistoryMapper.deleteBySessionId(sessionId);
    }

    @Override
    public void saveSqlDraft(String sessionId, String sqlContent) {
        Map<String, Object> connectionInfo = connectionInfoCache.get(sessionId);
        com.dataplatform.data.entity.SqlHistory history = new com.dataplatform.data.entity.SqlHistory();
        history.setSessionId(sessionId);
        history.setDbType(connectionInfo != null ? (String) connectionInfo.get("dbType") : null);
        history.setDbName(connectionInfo != null ? (String) connectionInfo.get("dbName") : null);
        history.setSqlContent(sqlContent != null && sqlContent.length() > 5000 ? sqlContent.substring(0, 5000) : sqlContent);
        history.setSqlType("SAVED");
        history.setStatus("saved");
        history.setExecuteAt(new java.util.Date());
        sqlHistoryMapper.insert(history);
    }

    @Override
    public Map<String, Object> explainSql(String sessionId, Map<String, Object> params) throws Exception {
        Connection conn = getProxyConnection(sessionId);
        Map<String, Object> connectionInfo = connectionInfoCache.get(sessionId);
        Map<String, Object> result = new java.util.HashMap<>();

        String sql = (String) params.get("sql");
        if (sql == null || sql.trim().isEmpty()) {
            throw new IllegalArgumentException("SQL语句不能为空");
        }

        String dbType = (String) connectionInfo.get("dbType");
        String explainSql = buildExplainSql(sql.trim(), dbType);

        Statement stmt = null;
        ResultSet rs = null;

        try {
            stmt = conn.createStatement();
            stmt.setQueryTimeout(queryTimeout);
            long startTime = System.currentTimeMillis();
            rs = stmt.executeQuery(explainSql);
            long endTime = System.currentTimeMillis();

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            List<String> columnNames = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                columnNames.add(metaData.getColumnName(i));
            }

            List<Map<String, Object>> rows = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> row = new java.util.LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(metaData.getColumnName(i), rs.getObject(i));
                }
                rows.add(row);
            }

            result.put("columns", columnNames);
            result.put("data", rows);
            result.put("executeTime", endTime - startTime);
            result.put("explainSql", explainSql);
            result.put("success", true);
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
        }

        return result;
    }

    /**
     * 根据数据库类型构建 EXPLAIN 语句
     */
    private String buildExplainSql(String sql, String dbType) {
        switch (dbType.toUpperCase()) {
            case "MYSQL":
            case "MARIADB":
            case "TIDB":
            case "OCEANBASE":
                return "EXPLAIN " + sql;
            case "POSTGRESQL":
            case "KINGBASE":
                return "EXPLAIN (ANALYZE false, FORMAT TEXT) " + sql;
            case "ORACLE":
            case "DM":
                return "EXPLAIN PLAN FOR " + sql;
            case "SQLSERVER":
                // SQL Server 使用 SET SHOWPLAN_TEXT
                return "SET SHOWPLAN_ALL ON; " + sql + "; SET SHOWPLAN_ALL OFF";
            case "CLICKHOUSE":
                return "EXPLAIN " + sql;
            default:
                return "EXPLAIN " + sql;
        }
    }

    /**
     * 提取 DROP PROCEDURE/FUNCTION 语句
     */
    private String extractDropStatement(String sql) {
        // 匹配 DROP PROCEDURE/FUNCTION IF EXISTS xxx 或 DROP PROCEDURE/FUNCTION xxx
        java.util.regex.Pattern dropPattern = java.util.regex.Pattern.compile(
            "(?i)DROP\\s+(?:PROCEDURE|FUNCTION)\\s+(?:IF\\s+EXISTS\\s+)?[`'\"]?([\\w]+)[`'\"]?",
            java.util.regex.Pattern.MULTILINE
        );
        
        java.util.regex.Matcher matcher = dropPattern.matcher(sql);
        if (matcher.find()) {
            return matcher.group(0).trim();
        }
        return null;
    }
    
    /**
     * 分离存储过程创建语句
     * 将 DROP、CREATE 和后续语句（如 CALL）分开
     */
    private List<String> splitProcedureStatements(String sql) {
        List<String> statements = new ArrayList<>();
        
        // 移除 DELIMITER 语句（如果有）
        sql = sql.replaceAll("(?i)DELIMITER\\s+[^\\s]+\\s*;?", "").trim();
        
        // 使用正则匹配 DROP 语句
        java.util.regex.Pattern dropPattern = java.util.regex.Pattern.compile(
            "(?i)(DROP\\s+(?:PROCEDURE|FUNCTION)\\s+(?:IF\\s+EXISTS\\s+)?[`'\"]?\\w+[`'\"]?)\\s*;?",
            java.util.regex.Pattern.DOTALL
        );
        
        java.util.regex.Matcher dropMatcher = dropPattern.matcher(sql);
        if (dropMatcher.find()) {
            statements.add(dropMatcher.group(1).trim());
            sql = sql.substring(dropMatcher.end()).trim();
        }
        
        // 查找 CREATE PROCEDURE/FUNCTION ... END; 的完整语句
        // 使用正则匹配 CREATE ... END; 块
        java.util.regex.Pattern createPattern = java.util.regex.Pattern.compile(
            "(?is)(CREATE\\s+(?:OR\\s+REPLACE\\s+)?(?:PROCEDURE|FUNCTION)\\s+.+?\\bEND\\s*;)",
            java.util.regex.Pattern.DOTALL
        );
        
        java.util.regex.Matcher createMatcher = createPattern.matcher(sql);
        if (createMatcher.find()) {
            statements.add(createMatcher.group(1).trim());
            sql = sql.substring(createMatcher.end()).trim();
        }
        
        // 处理 END; 后面的其他语句（如 CALL）
        if (!sql.isEmpty()) {
            // 按分号分割剩余语句
            String[] remainingStatements = sql.split(";");
            for (String stmt : remainingStatements) {
                stmt = stmt.trim();
                if (!stmt.isEmpty()) {
                    statements.add(stmt);
                }
            }
        }
        
        log.info("分离存储过程语句: {}", statements);
        return statements;
    }
    
    /**
     * 提取完整的存储过程/函数创建语句
     * 处理 BEGIN...END 块，确保不会在块内的分号处截断
     */
    private String extractProcedureStatement(String sql) {
        // 移除 DELIMITER 语句
        sql = sql.replaceAll("(?i)DELIMITER\\s+[^\\s]+\\s*", "").trim();
        
        // 移除末尾单独的分号行
        sql = sql.replaceAll("\\s*;\\s*$", "").trim();
        
        // 查找 CREATE PROCEDURE/FUNCTION ... END 的完整语句
        String upperSql = sql.toUpperCase();
        
        int createIndex = -1;
        if (upperSql.contains("CREATE DEFINER")) {
            createIndex = upperSql.indexOf("CREATE DEFINER");
        } else if (upperSql.contains("CREATE PROCEDURE")) {
            createIndex = upperSql.indexOf("CREATE PROCEDURE");
        } else if (upperSql.contains("CREATE FUNCTION")) {
            createIndex = upperSql.indexOf("CREATE FUNCTION");
        } else if (upperSql.contains("CREATE OR REPLACE PROCEDURE")) {
            createIndex = upperSql.indexOf("CREATE OR REPLACE PROCEDURE");
        } else if (upperSql.contains("CREATE OR REPLACE FUNCTION")) {
            createIndex = upperSql.indexOf("CREATE OR REPLACE FUNCTION");
        }
        
        if (createIndex == -1) {
            return sql;
        }
        
        // 从 CREATE 开始
        String fromCreate = sql.substring(createIndex);
        String upperFromCreate = fromCreate.toUpperCase();
        
        int beginCount = 0;
        int endIndex = -1;
        boolean foundBegin = false;
        boolean inString = false;
        
        // 简单的状态机来匹配 BEGIN...END
        int i = 0;
        while (i < fromCreate.length()) {
            char c = fromCreate.charAt(i);
            
            // 跳过字符串
            if (c == '\'' && !inString) {
                inString = true;
                i++;
                continue;
            }
            if (c == '\'' && inString) {
                // 检查是否是转义的引号 ''
                if (i + 1 < fromCreate.length() && fromCreate.charAt(i + 1) == '\'') {
                    i += 2;
                    continue;
                }
                inString = false;
                i++;
                continue;
            }
            if (inString) {
                i++;
                continue;
            }
            
            // 检查 BEGIN（作为独立单词）
            if (i + 5 <= upperFromCreate.length() && 
                upperFromCreate.substring(i, i + 5).equals("BEGIN") &&
                (i == 0 || !Character.isLetterOrDigit(fromCreate.charAt(i - 1))) &&
                (i + 5 >= fromCreate.length() || !Character.isLetterOrDigit(fromCreate.charAt(i + 5)))) {
                beginCount++;
                foundBegin = true;
                i += 5;
                continue;
            }
            
            // 检查 END（作为独立单词）
            if (foundBegin && i + 3 <= upperFromCreate.length() && 
                upperFromCreate.substring(i, i + 3).equals("END") &&
                (i == 0 || !Character.isLetterOrDigit(fromCreate.charAt(i - 1))) &&
                (i + 3 >= fromCreate.length() || !Character.isLetterOrDigit(fromCreate.charAt(i + 3)))) {
                beginCount--;
                if (beginCount == 0) {
                    endIndex = i + 3;
                    break;
                }
                i += 3;
                continue;
            }
            
            i++;
        }
        
        if (endIndex > 0) {
            return fromCreate.substring(0, endIndex).trim();
        }
        
        // 如果没找到 END，返回原始 SQL
        return fromCreate.trim();
    }
    
    /**
     * 智能分割SQL语句
     * 考虑字符串中的分号，不会错误分割
     */
    private List<String> splitSqlStatements(String sql) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inBacktick = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;
        
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            char next = (i + 1 < sql.length()) ? sql.charAt(i + 1) : '\0';
            
            // 处理行注释
            if (!inSingleQuote && !inDoubleQuote && !inBacktick && !inBlockComment) {
                if (c == '-' && next == '-') {
                    inLineComment = true;
                }
                if (c == '#') {
                    inLineComment = true;
                }
            }
            if (inLineComment && (c == '\n' || c == '\r')) {
                inLineComment = false;
            }
            
            // 处理块注释
            if (!inSingleQuote && !inDoubleQuote && !inBacktick && !inLineComment) {
                if (c == '/' && next == '*') {
                    inBlockComment = true;
                }
                if (c == '*' && next == '/' && inBlockComment) {
                    inBlockComment = false;
                    current.append(c);
                    current.append(next);
                    i++;
                    continue;
                }
            }
            
            // 处理引号
            if (!inLineComment && !inBlockComment) {
                if (c == '\'' && !inDoubleQuote && !inBacktick) {
                    inSingleQuote = !inSingleQuote;
                } else if (c == '"' && !inSingleQuote && !inBacktick) {
                    inDoubleQuote = !inDoubleQuote;
                } else if (c == '`' && !inSingleQuote && !inDoubleQuote) {
                    inBacktick = !inBacktick;
                }
            }
            
            // 检查分号
            if (c == ';' && !inSingleQuote && !inDoubleQuote && !inBacktick && !inLineComment && !inBlockComment) {
                String stmt = current.toString().trim();
                if (!stmt.isEmpty()) {
                    statements.add(stmt);
                }
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        
        // 添加最后一条语句（如果没有分号结尾）
        String lastStmt = current.toString().trim();
        if (!lastStmt.isEmpty()) {
            statements.add(lastStmt);
        }
        
        return statements;
    }

    @Override
    public String getViewDefinitionProxy(String sessionId, Map<String, Object> params) throws Exception {
        Connection conn = getProxyConnection(sessionId);
        Map<String, Object> connectionInfo = connectionInfoCache.get(sessionId);

        String viewName = (String) params.get("viewName");
        String dbType = (String) connectionInfo.get("dbType");

        String sql;
        switch (dbType.toUpperCase()) {
            case "MYSQL":
                sql = "SHOW CREATE VIEW `" + viewName + "`";
                break;
            case "ORACLE":
                sql = "SELECT TEXT FROM USER_VIEWS WHERE VIEW_NAME = '" + viewName.toUpperCase() + "'";
                break;
            case "SQLSERVER":
                sql = "SELECT OBJECT_DEFINITION(OBJECT_ID('" + viewName + "')) AS definition";
                break;
            case "POSTGRESQL":
                sql = "SELECT pg_get_viewdef('" + viewName + "'::regclass, true) AS definition";
                break;
            default:
                return "不支持的数据库类型: " + dbType;
        }

        Statement stmt = null;
        ResultSet rs = null;

        try {
            stmt = conn.createStatement();
            stmt.setQueryTimeout(queryTimeout);
            rs = stmt.executeQuery(sql);

            if (rs.next()) {
                switch (dbType.toUpperCase()) {
                    case "MYSQL":
                        return rs.getString("Create View");
                    case "ORACLE":
                        return rs.getString("TEXT");
                    default:
                        return rs.getString(1);
                }
            }
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
        }

        return "未找到视图定义";
    }

    @Override
    public String getProcedureDefinitionProxy(String sessionId, Map<String, Object> params) throws Exception {
        Connection conn = getProxyConnection(sessionId);
        Map<String, Object> connectionInfo = connectionInfoCache.get(sessionId);

        String procedureName = (String) params.get("procedureName");
        String dbType = (String) connectionInfo.get("dbType");

        String sql;
        switch (dbType.toUpperCase()) {
            case "MYSQL":
                sql = "SHOW CREATE PROCEDURE `" + procedureName + "`";
                break;
            case "ORACLE":
                sql = "SELECT TEXT FROM USER_SOURCE WHERE NAME = '" + procedureName.toUpperCase() + "' ORDER BY LINE";
                break;
            case "SQLSERVER":
                sql = "SELECT OBJECT_DEFINITION(OBJECT_ID('" + procedureName + "')) AS definition";
                break;
            case "POSTGRESQL":
                sql = "SELECT pg_get_functiondef(p.oid) AS definition FROM pg_proc p " +
                      "JOIN pg_namespace n ON p.pronamespace = n.oid " +
                      "WHERE p.proname = '" + procedureName + "' AND n.nspname = 'public'";
                break;
            default:
                return "不支持的数据库类型: " + dbType;
        }

        Statement stmt = null;
        ResultSet rs = null;

        try {
            stmt = conn.createStatement();
            stmt.setQueryTimeout(queryTimeout);
            rs = stmt.executeQuery(sql);

            StringBuilder definition = new StringBuilder();
            while (rs.next()) {
                switch (dbType.toUpperCase()) {
                    case "MYSQL":
                        return rs.getString("Create Procedure");
                    case "ORACLE":
                        definition.append(rs.getString("TEXT"));
                        break;
                    default:
                        return rs.getString(1);
                }
            }

            return definition.length() > 0 ? definition.toString() : "未找到存储过程/函数定义";

        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
        }
    }

    @Override
    public List<Map<String, Object>> getTableIndexesProxy(String sessionId, Map<String, Object> params) throws Exception {
        Connection conn = getProxyConnection(sessionId);
        Map<String, Object> connectionInfo = connectionInfoCache.get(sessionId);
        List<Map<String, Object>> indexes = new ArrayList<>();

        String tableName = (String) params.get("tableName");
        String dbName = (String) connectionInfo.get("dbName");

        DatabaseMetaData metaData = conn.getMetaData();
        ResultSet rs = null;

        try {
            rs = metaData.getIndexInfo(dbName, null, tableName, false, false);
            while (rs.next()) {
                Map<String, Object> index = new HashMap<>();
                index.put("indexName", rs.getString("INDEX_NAME"));
                index.put("columnName", rs.getString("COLUMN_NAME"));
                index.put("nonUnique", rs.getBoolean("NON_UNIQUE"));
                indexes.add(index);
            }
        } finally {
            closeResultSet(rs);
        }

        return indexes;
    }

    // ==================== 私有辅助方法 ====================

    private Connection getProxyConnection(String sessionId) throws Exception {
        Connection conn = proxyConnections.get(sessionId);
        if (conn == null) {
            throw new SQLException("代理连接不存在，会话ID: " + sessionId);
        }
        if (conn.isClosed() || !conn.isValid(5)) {
            Map<String, Object> connectionInfo = connectionInfoCache.get(sessionId);
            if (connectionInfo != null) {
                conn = createConnection(connectionInfo);
                proxyConnections.put(sessionId, conn);
                log.info("代理连接已重新建立，会话ID: {}", sessionId);
            } else {
                throw new SQLException("无法重新建立代理连接，会话ID: " + sessionId);
            }
        }
        // 更新连接活跃时间
        updateConnectionActiveTime(sessionId);
        return conn;
    }

    private String generateSessionId() {
        return "proxy_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 10000);
    }

    private Connection createConnection(Map<String, Object> connectionInfo) throws Exception {
        String dbType = (String) connectionInfo.get("dbType");
        String host = (String) connectionInfo.get("host");
        String port = (String) connectionInfo.get("port");
        String dbName = (String) connectionInfo.get("dbName");
        String username = (String) connectionInfo.get("username");
        String password = (String) connectionInfo.get("password");

        String url = buildJdbcUrl(dbType, host, port, dbName);
        String driver = dbConnectionUtil.getDriverClassName(dbType);

        Class.forName(driver);
        DriverManager.setLoginTimeout(connectionTimeout);
        Connection conn = DriverManager.getConnection(url, username, password);
        log.info("数据库连接成功: {}", url);
        return conn;
    }

    private String buildJdbcUrl(String dbType, String host, String port, String dbName) {
        switch (dbType.toUpperCase()) {
            case "MYSQL":
                return "jdbc:mysql://" + host + ":" + port + "/" + dbName +
                       "?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=GMT%2B8";
            case "MARIADB":
                return "jdbc:mariadb://" + host + ":" + port + "/" + dbName +
                       "?useUnicode=true&characterEncoding=utf8";
            case "ORACLE":
                if (dbName.contains("/")) {
                    return "jdbc:oracle:thin:@//" + host + ":" + port + "/" + dbName;
                } else {
                    return "jdbc:oracle:thin:@" + host + ":" + port + ":" + dbName;
                }
            case "SQLSERVER":
                return "jdbc:sqlserver://" + host + ":" + port + ";DatabaseName=" + dbName + ";encrypt=false;trustServerCertificate=true";
            case "POSTGRESQL":
                return "jdbc:postgresql://" + host + ":" + port + "/" + dbName + "?characterEncoding=utf8";
            case "SQLITE":
                return "jdbc:sqlite:" + dbName;
            case "DM":
                return "jdbc:dm://" + host + ":" + port + "/" + dbName + "?characterEncoding=utf8";
            case "KINGBASE":
                return "jdbc:kingbase8://" + host + ":" + port + "/" + dbName + "?characterEncoding=utf8";
            case "GBASE":
                return "jdbc:gbase://" + host + ":" + port + "/" + dbName + "?characterEncoding=utf8";
            case "TIDB":
            case "OCEANBASE":
                // 兼容MySQL协议
                return "jdbc:mysql://" + host + ":" + port + "/" + dbName +
                       "?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=GMT%2B8";
            case "CLICKHOUSE":
                return "jdbc:clickhouse://" + host + ":" + port + "/" + dbName + "?charset=utf8";
            case "PRESTO":
                // Presto/Trino 支持查询Hive数据
                return "jdbc:trino://" + host + ":" + port + "/" + dbName;
            default:
                throw new IllegalArgumentException("不支持的数据库类型: " + dbType);
        }
    }

    private String buildPagedQuery(String baseQuery, String dbType, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        switch (dbType.toUpperCase()) {
            case "MYSQL":
            case "MARIADB":
            case "POSTGRESQL":
            case "SQLITE":
            case "TIDB":
            case "OCEANBASE":
            case "CLICKHOUSE":
            case "PRESTO":
            case "KINGBASE":
                return baseQuery + " LIMIT " + pageSize + " OFFSET " + offset;
            case "ORACLE":
            case "DM":
            case "GBASE":
                return "SELECT * FROM (SELECT t.*, ROWNUM rn FROM (" + baseQuery + ") t WHERE ROWNUM <= " + (offset + pageSize) + ") WHERE rn > " + offset;
            case "SQLSERVER":
                return baseQuery + " ORDER BY (SELECT NULL) OFFSET " + offset + " ROWS FETCH NEXT " + pageSize + " ROWS ONLY";
            default:
                return baseQuery + " LIMIT " + pageSize + " OFFSET " + offset;
        }
    }

    private String escapeIdentifier(String identifier, String dbType) {
        switch (dbType.toUpperCase()) {
            case "MYSQL":
            case "MARIADB":
            case "TIDB":
            case "OCEANBASE":
                return "`" + identifier + "`";
            case "SQLSERVER":
                return "[" + identifier + "]";
            case "POSTGRESQL":
            case "KINGBASE":
            case "ORACLE":
            case "DM":
            case "GBASE":
                return "\"" + identifier + "\"";
            default:
                return identifier;
        }
    }

    private void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                log.error("关闭连接失败", e);
            }
        }
    }

    private void closeStatement(Statement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                log.error("关闭Statement失败", e);
            }
        }
    }

    private void closeResultSet(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                log.error("关闭ResultSet失败", e);
            }
        }
    }
}
