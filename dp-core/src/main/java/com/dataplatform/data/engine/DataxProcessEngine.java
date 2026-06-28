package com.dataplatform.data.engine;

import com.dataplatform.data.entity.DataSource;
import com.dataplatform.data.entity.DataxJob;
import com.dataplatform.data.service.DataSourceConnectionPoolManager;
import com.dataplatform.data.service.DataSourceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DataX 进程引擎
 * 生成 DataX JSON 配置 → 调用 python datax.py → 解析输出日志获取统计
 */
@Slf4j
@Component
public class DataxProcessEngine implements TransferEngine {

    @Autowired
    private DataSourceService dataSourceService;

    @Autowired
    private DataSourceConnectionPoolManager connectionPoolManager;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${datax.home:}")
    private String globalDataxHome;

    @Value("${datax.python:python}")
    private String pythonCmd;

    @Value("${datax.timeout-minutes:60}")
    private int timeoutMinutes;

    // DataX 输出统计行正则: 任务启动时刻   ...  读出记录总数  ...  读写失败总数
    private static final Pattern STATS_PATTERN = Pattern.compile(
            "任务启动时刻.*读出记录总数\\s*:\\s*(\\d+).*读写失败总数\\s*:\\s*(\\d+)",
            Pattern.DOTALL
    );
    // 备用英文正则
    private static final Pattern STATS_PATTERN_EN = Pattern.compile(
            "Total\\s+(\\d+)\\s+records.*Total\\s+(\\d+)\\s+error",
            Pattern.DOTALL
    );

    @Override
    public String getType() {
        return "datax";
    }

    @Override
    public TransferResult execute(DataxJob job, DataSource sourceDs, DataSource targetDs,
                                  Map<String, Object> parameters, Consumer<Map<String, Object>> progressCallback) {
        long startTime = System.currentTimeMillis();

        // 1. 确定 DataX Home
        String dataxHome = resolveDataxHome(job);
        if (!StringUtils.hasText(dataxHome)) {
            return TransferResult.fail("未配置 DataX 安装路径。请在任务配置或系统设置中设置 datax.home");
        }

        Path dataxPy = Path.of(dataxHome, "bin", "datax.py");
        if (!Files.exists(dataxPy)) {
            return TransferResult.fail("DataX 启动脚本不存在: " + dataxPy + "，请检查 DataX 安装路径");
        }

        // 2. 如果是增量同步，提前查询源表增量字段的当前最大值
        String newIncrementValue = null;
        if (job.getIncrementType() != null && job.getIncrementType() == 1
                && StringUtils.hasText(job.getIncrementColumn())) {
            try {
                newIncrementValue = queryMaxIncrementValue(sourceDs, job.getSourceTable(), job.getIncrementColumn());
                log.info("[DataxEngine] 预查询增量最大值: col={}, value={}", job.getIncrementColumn(), newIncrementValue);
            } catch (Exception e) {
                log.warn("[DataxEngine] 预查询增量最大值失败（不影响执行）: {}", e.getMessage());
            }
        }

        Path tempJsonFile = null;
        try {
            // 3. 生成 DataX JSON 配置
            String jsonConfig = buildDataxJson(job, sourceDs, targetDs, parameters);
            tempJsonFile = Files.createTempFile("datax_job_" + job.getId() + "_", ".json");
            Files.writeString(tempJsonFile, jsonConfig, StandardCharsets.UTF_8);
            log.info("[DataxEngine] 配置文件: {}", tempJsonFile);

            if (progressCallback != null) {
                progressCallback.accept(Map.of("message", "DataX 配置已生成，启动 DataX 进程..."));
            }

            // 4. 启动 DataX 进程
            ProcessBuilder pb = new ProcessBuilder(pythonCmd, dataxPy.toString(), tempJsonFile.toString());
            pb.redirectErrorStream(true);
            pb.environment().put("DATAX_HOME", dataxHome);
            Process process = pb.start();

            // 5. 异步读取输出（防止主线程阻塞，配合 waitFor 超时生效）
            final StringBuilder outputLog = new StringBuilder();
            final Process proc = process;
            CompletableFuture<Void> readerTask = CompletableFuture.runAsync(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        synchronized (outputLog) {
                            outputLog.append(line).append("\n");
                        }
                        if (progressCallback != null && (line.contains("已完成") || line.contains("records"))) {
                            try {
                                progressCallback.accept(Map.of("message",
                                        line.length() > 200 ? line.substring(0, 200) : line));
                            } catch (Exception ignored) {
                            }
                        }
                    }
                } catch (IOException e) {
                    log.warn("[DataxEngine] 读取输出流失败: {}", e.getMessage());
                }
            });

            boolean finished = process.waitFor(timeoutMinutes, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                readerTask.cancel(true);
                long duration = System.currentTimeMillis() - startTime;
                return TransferResult.fail("DataX 执行超时（" + timeoutMinutes + " 分钟）", 0, 0, duration);
            }

            try {
                readerTask.get(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                readerTask.cancel(true);
            }

            int exitCode = process.exitValue();
            long duration = System.currentTimeMillis() - startTime;
            String logStr;
            synchronized (outputLog) {
                logStr = outputLog.toString();
            }

            // 6. 解析输出统计
            long readCount = 0;
            long errorCount = 0;

            Matcher matcher = STATS_PATTERN.matcher(logStr);
            if (matcher.find()) {
                readCount = Long.parseLong(matcher.group(1));
                errorCount = Long.parseLong(matcher.group(2));
            } else {
                Matcher matcherEn = STATS_PATTERN_EN.matcher(logStr);
                if (matcherEn.find()) {
                    readCount = Long.parseLong(matcherEn.group(1));
                    errorCount = Long.parseLong(matcherEn.group(2));
                }
            }

            String trimmedLog = logStr.length() > 5000 ? logStr.substring(logStr.length() - 5000) : logStr;

            if (exitCode == 0) {
                if (newIncrementValue != null) {
                    job.setLastIncrementValue(newIncrementValue);
                }
                log.info("[DataxEngine] 执行成功: read={}, errors={}, 耗时={}ms", readCount, errorCount, duration);
                return TransferResult.ok(readCount, readCount - errorCount, duration, trimmedLog);
            } else {
                String errorMsg = extractErrorMessage(logStr, exitCode);
                log.error("[DataxEngine] 执行失败: exitCode={}, error={}", exitCode, errorMsg);
                return new TransferResult(false, readCount, readCount - errorCount, duration, errorMsg, trimmedLog);
            }

        } catch (Exception e) {
            log.error("[DataxEngine] 执行异常", e);
            long duration = System.currentTimeMillis() - startTime;
            return TransferResult.fail("DataX 执行异常: " + e.getMessage(), 0, 0, duration);
        } finally {
            if (tempJsonFile != null) {
                try {
                    Files.deleteIfExists(tempJsonFile);
                } catch (IOException ignored) {
                }
            }
        }
    }

    private String resolveDataxHome(DataxJob job) {
        if (StringUtils.hasText(job.getDataxHome())) {
            return job.getDataxHome();
        }
        return globalDataxHome;
    }

    /**
     * 生成 DataX JSON 配置（使用 Jackson 保证格式正确）
     */
    private String buildDataxJson(DataxJob job, DataSource sourceDs, DataSource targetDs,
                                  Map<String, Object> parameters) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode jobNode = root.putObject("job");

        // setting
        ObjectNode setting = jobNode.putObject("setting");
        ObjectNode speed = setting.putObject("speed");
        speed.put("channel", job.getChannelCount() != null ? job.getChannelCount() : 3);
        ObjectNode errorLimit = setting.putObject("errorLimit");
        errorLimit.put("record", 0);
        errorLimit.put("percentage", 0.02);

        // content
        ArrayNode contentArray = jobNode.putArray("content");
        ObjectNode content = contentArray.addObject();

        // reader
        ObjectNode reader = content.putObject("reader");
        reader.put("name", getReaderName(sourceDs.getDbType()));
        ObjectNode readerParam = reader.putObject("parameter");
        readerParam.put("username", sourceDs.getUsername());
        readerParam.put("password", sourceDs.getPassword() != null ? sourceDs.getPassword() : "");

        ObjectNode readerConn = readerParam.putArray("connection").addObject();
        readerConn.putArray("jdbcUrl").add(dataSourceService.buildJdbcUrl(sourceDs));

        String querySql = buildQuerySql(job, parameters);
        if (StringUtils.hasText(job.getSourceQuerySql()) || querySql.toUpperCase().contains("WHERE")) {
            readerConn.putArray("querySql").add(querySql);
        } else {
            readerConn.putArray("table").add(job.getSourceTable());
            readerParam.putArray("column").add("*");
        }

        // writer
        ObjectNode writer = content.putObject("writer");
        writer.put("name", getWriterName(targetDs.getDbType()));
        ObjectNode writerParam = writer.putObject("parameter");
        writerParam.put("username", targetDs.getUsername());
        writerParam.put("password", targetDs.getPassword() != null ? targetDs.getPassword() : "");
        writerParam.put("writeMode", job.getWriteMode() != null ? job.getWriteMode() : "insert");
        writerParam.putArray("column").add("*");
        if (job.getBatchSize() != null && job.getBatchSize() > 0) {
            writerParam.put("batchSize", job.getBatchSize());
        }

        ObjectNode writerConn = writerParam.putArray("connection").addObject();
        writerConn.put("jdbcUrl", dataSourceService.buildJdbcUrl(targetDs));
        String targetTable = StringUtils.hasText(job.getTargetTable()) ? job.getTargetTable() : job.getSourceTable();
        writerConn.putArray("table").add(targetTable);

        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
    }

    private String buildQuerySql(DataxJob job, Map<String, Object> parameters) {
        String sql;
        if (StringUtils.hasText(job.getSourceQuerySql())) {
            sql = job.getSourceQuerySql();
        } else {
            sql = "SELECT * FROM " + job.getSourceTable();
        }

        // 参数替换
        if (parameters != null) {
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                String safeValue = entry.getValue() != null ? entry.getValue().toString().replace("'", "''") : "";
                sql = sql.replace("${" + entry.getKey() + "}", safeValue);
            }
        }

        // 增量条件
        if (job.getIncrementType() != null && job.getIncrementType() == 1
                && StringUtils.hasText(job.getIncrementColumn())
                && StringUtils.hasText(job.getIncrementValue())) {
            String incCol = job.getIncrementColumn().trim();
            String safeValue = job.getIncrementValue().replace("'", "''");
            String condition = incCol + " > '" + safeValue + "'";
            if (sql.toUpperCase().contains("WHERE")) {
                sql += " AND " + condition;
            } else {
                sql += " WHERE " + condition;
            }
        }

        return sql;
    }

    private String extractErrorMessage(String log, int exitCode) {
        // 尝试提取 DataX 的 ErrorMessage
        int idx = log.lastIndexOf("ErrorMessage");
        if (idx >= 0) {
            String tail = log.substring(idx, Math.min(idx + 500, log.length()));
            return tail.split("\n")[0];
        }
        // 尝试提取异常
        idx = log.lastIndexOf("Exception");
        if (idx >= 0) {
            int start = Math.max(0, idx - 100);
            return log.substring(start, Math.min(idx + 200, log.length())).trim();
        }
        return "DataX 进程退出码: " + exitCode;
    }

    /**
     * 执行前查询源表增量字段的当前最大值，用于 DataX 成功后更新 lastIncrementValue
     * 独立使用短连接，与 DataX 进程互不影响
     */
    private String queryMaxIncrementValue(DataSource sourceDs, String tableName, String incrementColumn) throws Exception {
        String col = incrementColumn.trim();
        if (!col.matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("增量字段名不合法: " + col);
        }
        if (tableName == null || !tableName.matches("^[a-zA-Z0-9_.`]+$")) {
            throw new IllegalArgumentException("表名不合法: " + tableName);
        }
        String sql = "SELECT MAX(`" + col + "`) FROM " + tableName;
        try (Connection conn = connectionPoolManager.getConnection(sourceDs);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                Object v = rs.getObject(1);
                return v != null ? v.toString() : null;
            }
        }
        return null;
    }

    private String getReaderName(String dbType) {
        return switch (dbType.toUpperCase()) {
            case "MYSQL", "MARIADB", "TIDB", "OCEANBASE" -> "mysqlreader";
            case "ORACLE", "DM" -> "oraclereader";
            case "SQLSERVER" -> "sqlserverreader";
            case "POSTGRESQL", "KINGBASE" -> "postgresqlreader";
            case "CLICKHOUSE" -> "clickhousereader";
            default -> "rdbmsreader";
        };
    }

    private String getWriterName(String dbType) {
        return switch (dbType.toUpperCase()) {
            case "MYSQL", "MARIADB", "TIDB", "OCEANBASE" -> "mysqlwriter";
            case "ORACLE", "DM" -> "oraclewriter";
            case "SQLSERVER" -> "sqlserverwriter";
            case "POSTGRESQL", "KINGBASE" -> "postgresqlwriter";
            case "CLICKHOUSE" -> "clickhousewriter";
            default -> "rdbmswriter";
        };
    }
}
