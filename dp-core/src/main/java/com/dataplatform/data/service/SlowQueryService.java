package com.dataplatform.data.service;

import com.dataplatform.data.entity.SlowQueryLog;
import com.dataplatform.data.mapper.SlowQueryLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 慢查询服务
 * 自动记录超过阈值（默认3秒）的查询，提供查询日志、统计分析、优化建议
 */
@Slf4j
@Service
public class SlowQueryService {

    /** 默认慢查询阈值（毫秒） */
    public static final long DEFAULT_THRESHOLD_MS = 3000L;

    @Autowired
    private SlowQueryLogMapper slowQueryLogMapper;

    /** 慢查询阈值，可通过配置 slow-query.threshold-ms 修改，默认 3000ms */
    @Value("${slow-query.threshold-ms:3000}")
    private long thresholdMs;

    /**
     * 检测并记录慢查询。
     * 当 executionTime >= thresholdMs 时，异步写入 sys_slow_query_log 表。
     *
     * @param dataSourceId   数据源ID
     * @param dataSourceName 数据源名称（可为null）
     * @param sql            执行的SQL语句
     * @param executionTime  执行耗时（毫秒）
     * @param rowsExamined   扫描行数（可为null）
     * @param rowsReturned   返回行数（可为null）
     * @param userName       执行用户（可为null）
     * @param clientIp       客户端IP（可为null）
     * @param databaseName   数据库名（可为null）
     */
    @Async
    public void recordIfSlow(Long dataSourceId, String dataSourceName, String sql,
                             long executionTime, Long rowsExamined, Long rowsReturned,
                             String userName, String clientIp, String databaseName) {
        if (executionTime < thresholdMs) {
            return;
        }
        try {
            SlowQueryLog entry = new SlowQueryLog();
            entry.setDataSourceId(dataSourceId);
            entry.setDataSourceName(dataSourceName);
            entry.setSqlText(truncateSql(sql, 4000));
            entry.setSqlHash(computeSqlHash(sql));
            entry.setExecutionTime(executionTime);
            entry.setRowsExamined(rowsExamined);
            entry.setRowsReturned(rowsReturned);
            entry.setQueryTime(new Date());
            entry.setUserName(userName);
            entry.setClientIp(clientIp);
            entry.setDatabaseName(databaseName);

            slowQueryLogMapper.insert(entry);
            log.warn("慢查询已记录: executionTime={}ms, sqlHash={}, dataSource={}",
                    executionTime, entry.getSqlHash(), dataSourceName);
        } catch (Exception e) {
            log.error("记录慢查询失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 简化版：仅需 dataSourceId、sql、executionTime 即可检测慢查询
     */
    @Async
    public void recordIfSlow(Long dataSourceId, String sql, long executionTime) {
        recordIfSlow(dataSourceId, null, sql, executionTime, null, null, null, null, null);
    }

    /**
     * 获取当前慢查询阈值（毫秒）
     */
    public long getThresholdMs() {
        return thresholdMs;
    }

    /**
     * 动态更新慢查询阈值（毫秒），运行时生效
     */
    public void setThresholdMs(long newThresholdMs) {
        if (newThresholdMs < 100) {
            throw new IllegalArgumentException("阈值不能小于100ms");
        }
        log.info("慢查询阈值更新: {}ms -> {}ms", this.thresholdMs, newThresholdMs);
        this.thresholdMs = newThresholdMs;
    }

    /**
     * 分页查询慢查询日志
     */
    public Map<String, Object> listSlowQueries(int page, int size) {
        return listSlowQueries(page, size, null, null, null, null);
    }

    /**
     * 分页查询慢查询日志（带过滤条件）
     */
    public Map<String, Object> listSlowQueries(int page, int size, Long dataSourceId, Long minTime,
                                                String startDate, String endDate) {
        int offset = (page - 1) * size;
        List<SlowQueryLog> list = slowQueryLogMapper.findByPageFiltered(offset, size, dataSourceId, minTime, startDate, endDate);
        long total = slowQueryLogMapper.countFiltered(dataSourceId, minTime, startDate, endDate);
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    /**
     * 获取出现过慢查询的数据源列表
     */
    public List<Map<String, Object>> getDistinctDataSources() {
        return slowQueryLogMapper.findDistinctDataSources();
    }

    /**
     * 获取慢查询统计（按数据源分组）
     */
    public List<Map<String, Object>> getStatsByDataSource(int hours) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR_OF_DAY, -hours);
        return slowQueryLogMapper.findStats(cal.getTime());
    }

    /**
     * 获取TOP慢查询
     */
    public List<Map<String, Object>> getTopSlowQueries(int hours) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR_OF_DAY, -hours);
        return slowQueryLogMapper.findTopSlowQueries(cal.getTime());
    }

    /**
     * 生成简单优化建议
     */
    public List<Map<String, Object>> getOptimizationSuggestions(int hours) {
        List<Map<String, Object>> topQueries = getTopSlowQueries(hours);
        List<Map<String, Object>> suggestions = new ArrayList<>();

        for (Map<String, Object> query : topQueries) {
            String sql = (String) query.get("sql_text");
            if (sql == null) continue;
            String upperSql = sql.toUpperCase().trim();

            Map<String, Object> suggestion = new LinkedHashMap<>();
            suggestion.put("sqlHash", query.get("sql_hash"));
            suggestion.put("sqlText", sql.length() > 200 ? sql.substring(0, 200) + "..." : sql);
            suggestion.put("occurrence", query.get("occurrence"));
            suggestion.put("avgTime", query.get("avg_time"));

            List<String> tips = new ArrayList<>();
            if (!upperSql.contains("WHERE") && (upperSql.startsWith("SELECT") || upperSql.startsWith("UPDATE") || upperSql.startsWith("DELETE"))) {
                tips.add("缺少WHERE条件，可能导致全表扫描");
            }
            if (upperSql.contains("SELECT *")) {
                tips.add("使用SELECT *，建议指定具体字段");
            }
            if (upperSql.contains("ORDER BY") && !upperSql.contains("LIMIT")) {
                tips.add("有ORDER BY但无LIMIT，大数据量时可能很慢");
            }
            if (upperSql.contains("NOT IN") || upperSql.contains("NOT EXISTS")) {
                tips.add("使用NOT IN/NOT EXISTS，考虑改用LEFT JOIN + IS NULL");
            }
            if (upperSql.contains("LIKE '%")) {
                tips.add("前缀模糊查询(LIKE '%...')无法使用索引");
            }
            if (tips.isEmpty()) {
                tips.add("建议通过EXPLAIN分析执行计划，检查索引使用情况");
            }

            suggestion.put("suggestions", tips);
            suggestions.add(suggestion);
        }

        return suggestions;
    }

    /**
     * 清理过期慢查询日志
     */
    public int cleanOldLogs(int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -days);
        return slowQueryLogMapper.deleteOlderThan(cal.getTime());
    }

    // ---- internal helpers ----

    /**
     * 计算 SQL 哈希（用于聚合相同模式的查询）
     */
    String computeSqlHash(String sql) {
        if (sql == null) return "";
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(sql.trim().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            // MD5 is always available
            return String.valueOf(sql.hashCode());
        }
    }

    /**
     * 截断过长的 SQL 文本
     */
    String truncateSql(String sql, int maxLength) {
        if (sql == null) return null;
        return sql.length() <= maxLength ? sql : sql.substring(0, maxLength);
    }
}

