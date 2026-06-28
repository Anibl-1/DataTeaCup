package com.dataplatform.data.service.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 慢查询缓存关联分析器实现
 * 
 * 实现慢查询与缓存未命中的关联分析，生成包含缓存关联信息的分析报告。
 * 
 * 需求引用：
 * - 需求 10.3: 关联分析缓存未命中的慢查询
 * 
 * 属性 32: 慢查询缓存关联
 * 对于任意缓存未命中的慢查询，应在分析报告中关联显示缓存未命中信息。
 * 
 * @see SlowQueryCacheAnalyzer 慢查询缓存关联分析器接口
 * @see CacheStatsService 缓存统计服务
 */
@Slf4j
@Service
public class SlowQueryCacheAnalyzerImpl implements SlowQueryCacheAnalyzer {
    
    /**
     * 默认慢查询阈值（毫秒）
     */
    private static final long DEFAULT_SLOW_QUERY_THRESHOLD_MS = 3000L;
    
    /**
     * 内存缓存队列最大大小
     */
    private static final int MAX_QUEUE_SIZE = 10000;
    
    /**
     * 批量持久化大小
     */
    private static final int BATCH_PERSIST_SIZE = 100;
    
    /**
     * JDBC 模板
     */
    private final JdbcTemplate jdbcTemplate;
    
    /**
     * 内存中的缓存状态记录队列（用于批量持久化）
     */
    private final ConcurrentLinkedQueue<QueryCacheStatusRecord> recordQueue = new ConcurrentLinkedQueue<>();
    
    /**
     * SQL 哈希缓存（避免重复计算）
     */
    private final ConcurrentHashMap<String, String> sqlHashCache = new ConcurrentHashMap<>();
    
    @Autowired
    public SlowQueryCacheAnalyzerImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        log.info("慢查询缓存关联分析器初始化完成");
    }
    
    @Override
    public void recordQueryCacheStatus(String cacheKey, String sql, CacheHitStatus cacheStatus,
                                        long executionTime, Long dataSourceId) {
        if (sql == null || sql.trim().isEmpty()) {
            return;
        }
        
        boolean isSlowQuery = executionTime >= DEFAULT_SLOW_QUERY_THRESHOLD_MS;
        String sqlHash = computeSqlHash(sql);
        
        QueryCacheStatusRecord record = QueryCacheStatusRecord.builder()
                .cacheKey(cacheKey)
                .sql(truncateSql(sql, 2000))
                .sqlHash(sqlHash)
                .cacheStatus(cacheStatus)
                .executionTime(executionTime)
                .dataSourceId(dataSourceId)
                .slowQuery(isSlowQuery)
                .recordTime(LocalDateTime.now())
                .build();
        
        // 添加到队列
        if (recordQueue.size() < MAX_QUEUE_SIZE) {
            recordQueue.offer(record);
        } else {
            log.warn("缓存状态记录队列已满，丢弃记录: cacheKey={}", cacheKey);
        }
        
        // 如果是慢查询且缓存未命中，记录日志
        if (isSlowQuery && cacheStatus == CacheHitStatus.MISS) {
            log.info("检测到缓存未命中的慢查询: executionTime={}ms, sqlHash={}", 
                    executionTime, sqlHash);
        }
    }
    
    @Override
    public List<SlowQueryCacheReport> getSlowQueryCacheReports(LocalDateTime startTime,
                                                                LocalDateTime endTime,
                                                                long slowQueryThresholdMs) {
        // 先持久化内存中的记录
        persistRecords();
        
        String sql = """
            SELECT 
                qcs.id,
                qcs.sql_text,
                qcs.sql_hash,
                qcs.execution_time,
                qcs.cache_status,
                qcs.cache_key,
                qcs.data_source_id,
                qcs.record_time,
                ds.name as data_source_name,
                COUNT(*) OVER (PARTITION BY qcs.sql_hash) as occurrence_count,
                AVG(qcs.execution_time) OVER (PARTITION BY qcs.sql_hash) as avg_execution_time,
                MAX(qcs.execution_time) OVER (PARTITION BY qcs.sql_hash) as max_execution_time,
                SUM(CASE WHEN qcs.cache_status = 'MISS' THEN 1 ELSE 0 END) OVER (PARTITION BY qcs.sql_hash) as cache_miss_count,
                SUM(CASE WHEN qcs.cache_status != 'MISS' THEN 1 ELSE 0 END) OVER (PARTITION BY qcs.sql_hash) as cache_hit_count
            FROM query_cache_status qcs
            LEFT JOIN sys_data_source ds ON qcs.data_source_id = ds.id
            WHERE qcs.record_time BETWEEN ? AND ?
              AND qcs.execution_time >= ?
            ORDER BY qcs.execution_time DESC
            """;
        
        try {
            List<SlowQueryCacheReport> reports = jdbcTemplate.query(sql,
                    new Object[]{startTime, endTime, slowQueryThresholdMs},
                    (rs, rowNum) -> {
                        SlowQueryCacheReport report = SlowQueryCacheReport.builder()
                                .id(rs.getLong("id"))
                                .sql(rs.getString("sql_text"))
                                .sqlHash(rs.getString("sql_hash"))
                                .executionTime(rs.getLong("execution_time"))
                                .cacheStatus(CacheHitStatus.fromCode(rs.getString("cache_status")))
                                .cacheKey(rs.getString("cache_key"))
                                .dataSourceId(rs.getLong("data_source_id"))
                                .dataSourceName(rs.getString("data_source_name"))
                                .queryTime(rs.getTimestamp("record_time").toLocalDateTime())
                                .occurrenceCount(rs.getInt("occurrence_count"))
                                .avgExecutionTime(rs.getDouble("avg_execution_time"))
                                .maxExecutionTime(rs.getLong("max_execution_time"))
                                .cacheMissCount(rs.getInt("cache_miss_count"))
                                .cacheHitCount(rs.getInt("cache_hit_count"))
                                .isSlowQuery(true)
                                .build();
                        
                        // 生成优化建议
                        generateSuggestions(report);
                        return report;
                    });
            
            return reports;
        } catch (Exception e) {
            log.error("查询慢查询缓存关联报告失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<SlowQueryCacheReport> getCacheMissSlowQueries(LocalDateTime startTime,
                                                               LocalDateTime endTime,
                                                               long slowQueryThresholdMs) {
        // 先持久化内存中的记录
        persistRecords();
        
        String sql = """
            SELECT 
                qcs.id,
                qcs.sql_text,
                qcs.sql_hash,
                qcs.execution_time,
                qcs.cache_status,
                qcs.cache_key,
                qcs.data_source_id,
                qcs.record_time,
                ds.name as data_source_name,
                COUNT(*) OVER (PARTITION BY qcs.sql_hash) as occurrence_count,
                AVG(qcs.execution_time) OVER (PARTITION BY qcs.sql_hash) as avg_execution_time,
                MAX(qcs.execution_time) OVER (PARTITION BY qcs.sql_hash) as max_execution_time
            FROM query_cache_status qcs
            LEFT JOIN sys_data_source ds ON qcs.data_source_id = ds.id
            WHERE qcs.record_time BETWEEN ? AND ?
              AND qcs.execution_time >= ?
              AND qcs.cache_status = 'MISS'
            ORDER BY qcs.execution_time DESC
            """;
        
        try {
            List<SlowQueryCacheReport> reports = jdbcTemplate.query(sql,
                    new Object[]{startTime, endTime, slowQueryThresholdMs},
                    (rs, rowNum) -> {
                        SlowQueryCacheReport report = SlowQueryCacheReport.builder()
                                .id(rs.getLong("id"))
                                .sql(rs.getString("sql_text"))
                                .sqlHash(rs.getString("sql_hash"))
                                .executionTime(rs.getLong("execution_time"))
                                .cacheStatus(CacheHitStatus.MISS)
                                .cacheKey(rs.getString("cache_key"))
                                .dataSourceId(rs.getLong("data_source_id"))
                                .dataSourceName(rs.getString("data_source_name"))
                                .queryTime(rs.getTimestamp("record_time").toLocalDateTime())
                                .occurrenceCount(rs.getInt("occurrence_count"))
                                .avgExecutionTime(rs.getDouble("avg_execution_time"))
                                .maxExecutionTime(rs.getLong("max_execution_time"))
                                .cacheMissCount(rs.getInt("occurrence_count"))
                                .cacheHitCount(0)
                                .isSlowQuery(true)
                                .build();
                        
                        // 生成优化建议
                        generateSuggestions(report);
                        return report;
                    });
            
            return reports;
        } catch (Exception e) {
            log.error("查询缓存未命中慢查询失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public SlowQueryCacheSummary getSummary(LocalDateTime startTime, LocalDateTime endTime) {
        // 先持久化内存中的记录
        persistRecords();
        
        String sql = """
            SELECT 
                COUNT(*) as total_count,
                SUM(CASE WHEN cache_status = 'MISS' THEN 1 ELSE 0 END) as miss_count,
                SUM(CASE WHEN cache_status = 'L1_HIT' THEN 1 ELSE 0 END) as l1_hit_count,
                SUM(CASE WHEN cache_status = 'L2_HIT' THEN 1 ELSE 0 END) as l2_hit_count,
                AVG(execution_time) as avg_time,
                AVG(CASE WHEN cache_status = 'MISS' THEN execution_time ELSE NULL END) as avg_miss_time,
                AVG(CASE WHEN cache_status != 'MISS' THEN execution_time ELSE NULL END) as avg_hit_time,
                MAX(execution_time) as max_time,
                COUNT(DISTINCT sql_hash) as unique_sql_count
            FROM query_cache_status
            WHERE record_time BETWEEN ? AND ?
              AND is_slow_query = true
            """;
        
        try {
            return jdbcTemplate.queryForObject(sql, new Object[]{startTime, endTime},
                    (rs, rowNum) -> {
                        long totalCount = rs.getLong("total_count");
                        long missCount = rs.getLong("miss_count");
                        
                        return SlowQueryCacheSummary.builder()
                                .startTime(startTime)
                                .endTime(endTime)
                                .totalSlowQueries(totalCount)
                                .cacheMissSlowQueries(missCount)
                                .l1HitSlowQueries(rs.getLong("l1_hit_count"))
                                .l2HitSlowQueries(rs.getLong("l2_hit_count"))
                                .cacheMissRate(totalCount > 0 ? (double) missCount / totalCount : 0.0)
                                .avgExecutionTime(rs.getDouble("avg_time"))
                                .avgCacheMissExecutionTime(rs.getDouble("avg_miss_time"))
                                .avgCacheHitExecutionTime(rs.getDouble("avg_hit_time"))
                                .maxExecutionTime(rs.getLong("max_time"))
                                .uniqueSqlCount(rs.getLong("unique_sql_count"))
                                .suggestedWarmUpCount(calculateSuggestedWarmUpCount(startTime, endTime))
                                .build();
                    });
        } catch (Exception e) {
            log.error("获取慢查询缓存统计摘要失败: {}", e.getMessage(), e);
            return SlowQueryCacheSummary.builder()
                    .startTime(startTime)
                    .endTime(endTime)
                    .build();
        }
    }
    
    @Override
    public List<QueryCacheStatusRecord> getCacheStatusHistory(String cacheKey, int limit) {
        String sql = """
            SELECT id, cache_key, sql_text, sql_hash, cache_status, 
                   execution_time, data_source_id, is_slow_query, record_time
            FROM query_cache_status
            WHERE cache_key = ?
            ORDER BY record_time DESC
            LIMIT ?
            """;
        
        try {
            return jdbcTemplate.query(sql, new Object[]{cacheKey, limit},
                    (rs, rowNum) -> QueryCacheStatusRecord.builder()
                            .id(rs.getLong("id"))
                            .cacheKey(rs.getString("cache_key"))
                            .sql(rs.getString("sql_text"))
                            .sqlHash(rs.getString("sql_hash"))
                            .cacheStatus(CacheHitStatus.fromCode(rs.getString("cache_status")))
                            .executionTime(rs.getLong("execution_time"))
                            .dataSourceId(rs.getLong("data_source_id"))
                            .slowQuery(rs.getBoolean("is_slow_query"))
                            .recordTime(rs.getTimestamp("record_time").toLocalDateTime())
                            .build());
        } catch (Exception e) {
            log.error("获取缓存状态历史失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public int cleanExpiredRecords(int retentionDays) {
        String sql = "DELETE FROM query_cache_status WHERE record_time < ?";
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(retentionDays);
        
        try {
            int deleted = jdbcTemplate.update(sql, cutoffTime);
            log.info("清理过期缓存状态记录: {} 条", deleted);
            return deleted;
        } catch (Exception e) {
            log.error("清理过期记录失败: {}", e.getMessage(), e);
            return 0;
        }
    }
    
    /**
     * 定时持久化内存中的记录
     */
    @Scheduled(fixedRate = 30000) // 每 30 秒执行一次
    public void scheduledPersist() {
        persistRecords();
    }
    
    /**
     * 持久化内存中的记录到数据库
     */
    private void persistRecords() {
        if (recordQueue.isEmpty()) {
            return;
        }
        
        List<QueryCacheStatusRecord> batch = new ArrayList<>();
        QueryCacheStatusRecord record;
        
        while ((record = recordQueue.poll()) != null && batch.size() < BATCH_PERSIST_SIZE) {
            batch.add(record);
        }
        
        if (batch.isEmpty()) {
            return;
        }
        
        String sql = """
            INSERT INTO query_cache_status 
            (cache_key, sql_text, sql_hash, cache_status, execution_time, data_source_id, is_slow_query, record_time)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        try {
            jdbcTemplate.batchUpdate(sql, batch, batch.size(),
                    (ps, r) -> {
                        ps.setString(1, r.getCacheKey());
                        ps.setString(2, r.getSql());
                        ps.setString(3, r.getSqlHash());
                        ps.setString(4, r.getCacheStatusCode());
                        ps.setLong(5, r.getExecutionTime());
                        ps.setObject(6, r.getDataSourceId());
                        ps.setBoolean(7, r.isSlowQuery());
                        ps.setObject(8, r.getRecordTime());
                    });
            
            log.debug("持久化缓存状态记录: {} 条", batch.size());
        } catch (Exception e) {
            log.error("持久化缓存状态记录失败: {}", e.getMessage(), e);
            // 失败时将记录放回队列
            for (QueryCacheStatusRecord r : batch) {
                if (recordQueue.size() < MAX_QUEUE_SIZE) {
                    recordQueue.offer(r);
                }
            }
        }
    }
    
    /**
     * 生成优化建议
     */
    private void generateSuggestions(SlowQueryCacheReport report) {
        List<String> suggestions = new ArrayList<>();
        
        // 缓存未命中建议
        if (report.isCacheMiss()) {
            suggestions.add("建议预热此查询到缓存");
            
            if (report.getOccurrenceCount() > 5) {
                suggestions.add("此查询频繁执行且缓存未命中，强烈建议加入缓存预热列表");
            }
        }
        
        // 执行时间建议
        if (report.getExecutionTime() > 10000) {
            suggestions.add("查询执行时间超过10秒，建议优化SQL或添加索引");
        } else if (report.getExecutionTime() > 5000) {
            suggestions.add("查询执行时间较长，建议检查执行计划");
        }
        
        // SQL 模式建议
        String sql = report.getSql();
        if (sql != null) {
            String upperSql = sql.toUpperCase();
            
            if (upperSql.contains("SELECT *")) {
                suggestions.add("使用 SELECT *，建议指定具体字段以减少数据传输");
            }
            
            if (!upperSql.contains("WHERE") && 
                (upperSql.startsWith("SELECT") || upperSql.startsWith("UPDATE") || upperSql.startsWith("DELETE"))) {
                suggestions.add("缺少 WHERE 条件，可能导致全表扫描");
            }
            
            if (upperSql.contains("LIKE '%")) {
                suggestions.add("前缀模糊查询无法使用索引，考虑使用全文索引");
            }
        }
        
        // 缓存命中率建议
        int totalCount = report.getCacheMissCount() + report.getCacheHitCount();
        if (totalCount > 0) {
            double hitRate = (double) report.getCacheHitCount() / totalCount;
            if (hitRate < 0.5) {
                suggestions.add("此查询缓存命中率低于50%，建议调整缓存TTL或预热策略");
            }
        }
        
        report.setSuggestions(suggestions);
    }
    
    /**
     * 计算建议预热的查询数量
     */
    private long calculateSuggestedWarmUpCount(LocalDateTime startTime, LocalDateTime endTime) {
        String sql = """
            SELECT COUNT(DISTINCT sql_hash) 
            FROM query_cache_status
            WHERE record_time BETWEEN ? AND ?
              AND is_slow_query = true
              AND cache_status = 'MISS'
            GROUP BY sql_hash
            HAVING COUNT(*) >= 3
            """;
        
        try {
            List<Long> counts = jdbcTemplate.queryForList(sql, Long.class, startTime, endTime);
            return counts.size();
        } catch (Exception e) {
            log.debug("计算建议预热数量失败: {}", e.getMessage());
            return 0;
        }
    }
    
    /**
     * 计算 SQL 哈希值
     */
    private String computeSqlHash(String sql) {
        if (sql == null) {
            return "";
        }
        
        // 标准化 SQL（去除多余空格、转小写）
        String normalizedSql = sql.trim().replaceAll("\\s+", " ").toLowerCase();
        
        return sqlHashCache.computeIfAbsent(normalizedSql, s -> {
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] digest = md.digest(s.getBytes());
                StringBuilder sb = new StringBuilder();
                for (byte b : digest) {
                    sb.append(String.format("%02x", b));
                }
                return sb.toString();
            } catch (NoSuchAlgorithmException e) {
                return String.valueOf(s.hashCode());
            }
        });
    }
    
    /**
     * 截断 SQL 语句
     */
    private String truncateSql(String sql, int maxLength) {
        if (sql == null) {
            return null;
        }
        if (sql.length() <= maxLength) {
            return sql;
        }
        return sql.substring(0, maxLength);
    }
}
