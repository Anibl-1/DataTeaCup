package com.dataplatform.data.engine;

import com.dataplatform.data.entity.DataSource;
import com.dataplatform.data.entity.DataxJob;
import com.dataplatform.data.service.DataSourceConnectionPoolManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * 内置 JDBC 流式传输引擎
 * 核心改进:
 * 1. MySQL 流式游标 (setFetchSize=Integer.MIN_VALUE) 解决大表 OOM
 * 2. 批量 executeBatch + 分段 commit 控制内存
 * 3. 进度实时回调
 */
@Slf4j
@Component
public class JdbcStreamEngine implements TransferEngine {

    @Autowired
    private DataSourceConnectionPoolManager connectionPoolManager;

    @Override
    public String getType() {
        return "jdbc";
    }

    @Override
    public TransferResult execute(DataxJob job, DataSource sourceDs, DataSource targetDs,
                                  Map<String, Object> parameters, Consumer<Map<String, Object>> progressCallback) {
        long startTime = System.currentTimeMillis();
        AtomicLong readCount = new AtomicLong(0);
        AtomicLong writeCount = new AtomicLong(0);

        Connection sourceConn = null;
        Connection targetConn = null;
        boolean originalTargetAutoCommit = true;

        try {
            // 1. 获取连接
            sourceConn = connectionPoolManager.getConnection(sourceDs);
            targetConn = connectionPoolManager.getConnection(targetDs);
            // 记录原始 autoCommit 状态以便在 finally 中还原（防止污染连接池复用的连接）
            originalTargetAutoCommit = targetConn.getAutoCommit();
            targetConn.setAutoCommit(false);

            // 2. 构建查询 SQL
            String querySql = buildQuerySql(job, parameters);
            log.info("[JdbcStream] 开始执行, 源={}.{}, SQL长度={}", sourceDs.getName(), job.getSourceTable(), querySql.length());

            // 3. 流式读取 + 批量写入
            int batchSize = job.getBatchSize() != null && job.getBatchSize() > 0 ? job.getBatchSize() : 1000;

            // MySQL 流式游标: fetchSize=Integer.MIN_VALUE 启用逐行读取，避免一次性加载全部到内存
            try (Statement stmt = sourceConn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
                configureStreamingRead(stmt, sourceDs.getDbType());

                try (ResultSet rs = stmt.executeQuery(querySql)) {
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();

                    // 构建列名列表
                    List<String> columns = new ArrayList<>(columnCount);
                    for (int i = 1; i <= columnCount; i++) {
                        columns.add(metaData.getColumnName(i));
                    }

                    // 构建 INSERT SQL
                    String targetTable = StringUtils.hasText(job.getTargetTable()) ? job.getTargetTable() : job.getSourceTable();
                    String insertSql = buildInsertSql(targetTable, columns, job.getWriteMode());

                    try (PreparedStatement pstmt = targetConn.prepareStatement(insertSql)) {
                        String lastIncrementValue = null;

                        while (rs.next()) {
                            long currentRead = readCount.incrementAndGet();

                            for (int i = 1; i <= columnCount; i++) {
                                pstmt.setObject(i, rs.getObject(i));

                                // 记录增量字段值
                                if (job.getIncrementType() != null && job.getIncrementType() == 1
                                        && metaData.getColumnName(i).equalsIgnoreCase(job.getIncrementColumn())) {
                                    Object val = rs.getObject(i);
                                    if (val != null) {
                                        lastIncrementValue = val.toString();
                                    }
                                }
                            }
                            pstmt.addBatch();

                            // 每 batchSize 行提交一次
                            if (currentRead % batchSize == 0) {
                                int[] results = pstmt.executeBatch();
                                long written = countWritten(results);
                                writeCount.addAndGet(written);
                                targetConn.commit();

                                // 进度回调（异常隔离，不影响主流程）
                                if (progressCallback != null) {
                                    try {
                                        progressCallback.accept(Map.of(
                                                "readCount", currentRead,
                                                "writeCount", writeCount.get(),
                                                "message", String.format("已读取 %d 行, 写入 %d 行", currentRead, writeCount.get())
                                        ));
                                    } catch (Exception ignored) {
                                    }
                                }

                                if (currentRead % (batchSize * 10) == 0) {
                                    log.info("[JdbcStream] 进度: read={}, write={}", currentRead, writeCount.get());
                                }
                            }
                        }

                        // 处理剩余数据
                        if (readCount.get() % batchSize != 0) {
                            int[] results = pstmt.executeBatch();
                            writeCount.addAndGet(countWritten(results));
                            targetConn.commit();
                        }

                        // 更新增量值
                        if (job.getIncrementType() != null && job.getIncrementType() == 1 && lastIncrementValue != null) {
                            job.setLastIncrementValue(lastIncrementValue);
                        }
                    }
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("[JdbcStream] 完成: read={}, write={}, 耗时={}ms", readCount.get(), writeCount.get(), duration);
            return TransferResult.ok(readCount.get(), writeCount.get(), duration);

        } catch (Exception e) {
            log.error("[JdbcStream] 执行失败: {}", e.getMessage(), e);

            if (targetConn != null) {
                try {
                    targetConn.rollback();
                } catch (SQLException ex) {
                    log.error("[JdbcStream] 回滚失败", ex);
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            return TransferResult.fail(e.getMessage(), readCount.get(), writeCount.get(), duration);

        } finally {
            // 还原 autoCommit 防止污染连接池复用的连接
            if (targetConn != null) {
                try {
                    targetConn.setAutoCommit(originalTargetAutoCommit);
                } catch (SQLException e) {
                    log.warn("[JdbcStream] 还原 autoCommit 失败: {}", e.getMessage());
                }
            }
            closeQuietly(sourceConn);
            closeQuietly(targetConn);
        }
    }

    /**
     * 配置流式读取（不同数据库方式不同）
     */
    private void configureStreamingRead(Statement stmt, String dbType) throws SQLException {
        if (dbType == null) {
            stmt.setFetchSize(500);
            return;
        }
        switch (dbType.toUpperCase()) {
            case "MYSQL", "MARIADB", "TIDB", "OCEANBASE" ->
                    // MySQL 流式读取：必须用 Integer.MIN_VALUE
                    stmt.setFetchSize(Integer.MIN_VALUE);
            case "POSTGRESQL", "KINGBASE" ->
                    // PostgreSQL 需要关闭 autoCommit 并设 fetchSize
                    stmt.setFetchSize(500);
            default ->
                    stmt.setFetchSize(500);
        }
    }

    private String buildQuerySql(DataxJob job, Map<String, Object> parameters) {
        String querySql;
        if (StringUtils.hasText(job.getSourceQuerySql())) {
            querySql = job.getSourceQuerySql();
        } else {
            querySql = "SELECT * FROM " + job.getSourceTable();
        }

        // 参数替换
        if (parameters != null && !parameters.isEmpty()) {
            querySql = replaceParameters(querySql, parameters);
        }

        // 增量条件
        if (job.getIncrementType() != null && job.getIncrementType() == 1
                && StringUtils.hasText(job.getIncrementColumn())
                && StringUtils.hasText(job.getIncrementValue())) {
            String incCol = job.getIncrementColumn().trim();
            if (!incCol.matches("^[a-zA-Z0-9_]+$")) {
                throw new IllegalArgumentException("增量字段名包含不安全字符: " + incCol);
            }
            String safeIncValue = job.getIncrementValue().replace("'", "''");
            String incCondition = "`" + incCol + "` > '" + safeIncValue + "'";
            if (querySql.toUpperCase().contains("WHERE")) {
                querySql += " AND " + incCondition;
            } else {
                querySql += " WHERE " + incCondition;
            }
        }

        return querySql;
    }

    private String replaceParameters(String sql, Map<String, Object> parameters) {
        String result = sql;
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            String paramName = entry.getKey();
            Object value = entry.getValue();
            String strValue = value != null ? value.toString() : "";
            String safeValue = strValue.replace("'", "''");
            result = result.replace("${" + paramName + "}", safeValue);
            result = result.replaceAll(":" + paramName + "(?![a-zA-Z0-9_])", "'" + safeValue + "'");
        }
        return result;
    }

    private String buildInsertSql(String tableName, List<String> columns, String writeMode) {
        StringBuilder sql = new StringBuilder();
        if ("replace".equalsIgnoreCase(writeMode)) {
            sql.append("REPLACE INTO ");
        } else {
            sql.append("INSERT INTO ");
        }
        sql.append(tableName).append(" (");
        sql.append(String.join(", ", columns));
        sql.append(") VALUES (");
        sql.append(String.join(", ", Collections.nCopies(columns.size(), "?")));
        sql.append(")");
        return sql.toString();
    }

    private long countWritten(int[] results) {
        long count = 0;
        for (int r : results) {
            if (r >= 0 || r == Statement.SUCCESS_NO_INFO) {
                count++;
            }
        }
        return count;
    }

    private void closeQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                log.warn("[JdbcStream] 关闭连接失败: {}", e.getMessage());
            }
        }
    }
}
