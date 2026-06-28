package com.dataplatform.data.service;

import com.dataplatform.data.entity.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 慢查询分析器
 * 解析 EXPLAIN 执行计划，推荐索引，检测低效 SQL 模式，生成优化建议。
 *
 * 需求 16.2: 解析并展示查询的执行计划（EXPLAIN 结果）
 * 需求 16.3: 基于查询模式推荐索引优化建议
 * 需求 16.4: 检测并提示低效的 SQL 模式
 * 需求 16.5: 提供慢查询分析报告
 */
@Slf4j
@Service
public class SlowQueryAnalyzer {

    @Autowired
    private DataSourceService dataSourceService;

    // ========================================================================
    // 执行计划分析 (需求 16.2)
    // ========================================================================

    /**
     * 对指定数据源执行 EXPLAIN 并解析结果。
     *
     * @param sql          要分析的 SQL
     * @param dataSourceId 数据源 ID
     * @return 解析后的执行计划
     */
    public ExecutionPlan analyzeExplain(String sql, Long dataSourceId) {
        if (sql == null || sql.trim().isEmpty()) {
            throw new IllegalArgumentException("SQL 不能为空");
        }
        if (dataSourceId == null) {
            throw new IllegalArgumentException("数据源 ID 不能为空");
        }

        ExecutionPlan plan = new ExecutionPlan();
        plan.setSql(sql);

        javax.sql.DataSource ds = dataSourceService.getDataSource(dataSourceId);
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("EXPLAIN " + sql.trim())) {

            long totalRows = 0;
            while (rs.next()) {
                PlanNode node = parsePlanNode(rs);
                plan.getNodes().add(node);
                if (node.getRows() != null) {
                    totalRows += node.getRows();
                }
            }
            plan.setEstimatedRows(totalRows);
            plan.setEstimatedCost(totalRows); // 简化：以扫描行数作为代价估算

            // 生成警告
            generatePlanWarnings(plan);

        } catch (SQLException e) {
            log.error("执行 EXPLAIN 失败: dataSourceId={}, sql={}", dataSourceId, sql, e);
            plan.getWarnings().add("执行 EXPLAIN 失败: " + e.getMessage());
        }

        return plan;
    }

    /**
     * 解析 EXPLAIN 结果集的一行为 PlanNode。
     * 此方法为包级可见，便于单元测试。
     */
    PlanNode parsePlanNode(ResultSet rs) throws SQLException {
        PlanNode node = new PlanNode();
        node.setId(getIntOrNull(rs, "id"));
        node.setSelectType(rs.getString("select_type"));
        node.setTable(rs.getString("table"));
        node.setPartitions(getStringOrNull(rs, "partitions"));
        node.setType(rs.getString("type"));
        node.setPossibleKeys(rs.getString("possible_keys"));
        node.setKey(rs.getString("key"));
        node.setKeyLen(getStringOrNull(rs, "key_len"));
        node.setRef(rs.getString("ref"));
        node.setRows(getLongOrNull(rs, "rows"));
        node.setFiltered(getDoubleOrNull(rs, "filtered"));
        node.setExtra(rs.getString("Extra"));
        return node;
    }

    /**
     * 从已解析的 PlanNode 列表构建 ExecutionPlan（不需要数据库连接）。
     * 用于测试和从已有数据重建执行计划。
     */
    public ExecutionPlan buildExecutionPlan(String sql, List<PlanNode> nodes) {
        ExecutionPlan plan = new ExecutionPlan();
        plan.setSql(sql);
        plan.setNodes(nodes != null ? nodes : new ArrayList<>());

        long totalRows = 0;
        for (PlanNode node : plan.getNodes()) {
            if (node.getRows() != null) {
                totalRows += node.getRows();
            }
        }
        plan.setEstimatedRows(totalRows);
        plan.setEstimatedCost(totalRows);

        generatePlanWarnings(plan);
        return plan;
    }

    /**
     * 根据执行计划节点生成警告信息。
     */
    void generatePlanWarnings(ExecutionPlan plan) {
        for (PlanNode node : plan.getNodes()) {
            if ("ALL".equalsIgnoreCase(node.getType())) {
                plan.getWarnings().add(
                        String.format("表 '%s' 执行了全表扫描 (type=ALL)，预估扫描 %d 行",
                                node.getTable(), node.getRows() != null ? node.getRows() : 0));
            }
            if (node.getType() != null && "index".equalsIgnoreCase(node.getType())) {
                plan.getWarnings().add(
                        String.format("表 '%s' 执行了全索引扫描 (type=index)，可能需要优化",
                                node.getTable()));
            }
            if (node.getExtra() != null) {
                if (node.getExtra().contains("Using filesort")) {
                    plan.getWarnings().add(
                            String.format("表 '%s' 使用了文件排序 (Using filesort)，考虑添加排序索引",
                                    node.getTable()));
                }
                if (node.getExtra().contains("Using temporary")) {
                    plan.getWarnings().add(
                            String.format("表 '%s' 使用了临时表 (Using temporary)，考虑优化查询",
                                    node.getTable()));
                }
            }
        }
    }

    // ========================================================================
    // 索引推荐 (需求 16.3)
    // ========================================================================

    /**
     * 基于执行计划推荐索引。
     */
    public List<IndexRecommendation> recommendIndexes(String sql, Long dataSourceId) {
        ExecutionPlan plan = analyzeExplain(sql, dataSourceId);
        return recommendIndexesFromPlan(plan, sql);
    }

    /**
     * 基于已有的执行计划和 SQL 推荐索引（不需要数据库连接）。
     * 
     * 推荐策略：
     * 1. WHERE 条件列 - 用于过滤数据
     * 2. JOIN 条件列 - 用于表连接
     * 3. ORDER BY 列 - 用于排序优化
     * 4. GROUP BY 列 - 用于分组优化
     */
    public List<IndexRecommendation> recommendIndexesFromPlan(ExecutionPlan plan, String sql) {
        List<IndexRecommendation> recommendations = new ArrayList<>();
        Set<String> recommendedIndexKeys = new HashSet<>(); // 避免重复推荐

        for (PlanNode node : plan.getNodes()) {
            String tableName = node.getTable();
            if (tableName == null || tableName.isEmpty()) {
                continue;
            }

            boolean isFullScan = "ALL".equalsIgnoreCase(node.getType());
            boolean hasFilesort = node.getExtra() != null && node.getExtra().contains("Using filesort");
            boolean hasTemporary = node.getExtra() != null && node.getExtra().contains("Using temporary");
            boolean noIndexUsed = node.getKey() == null || node.getKey().isEmpty();

            // 1. WHERE 条件列索引推荐（全表扫描时）
            if (isFullScan) {
                List<String> whereColumns = extractWhereColumns(sql, tableName);
                if (!whereColumns.isEmpty()) {
                    String indexKey = tableName + "_where_" + String.join("_", whereColumns);
                    if (recommendedIndexKeys.add(indexKey)) {
                        IndexRecommendation rec = new IndexRecommendation();
                        rec.setTableName(tableName);
                        rec.setColumns(whereColumns);
                        rec.setIndexType("BTREE");
                        rec.setReason(String.format("表 '%s' 全表扫描，预估扫描 %d 行，建议为 WHERE 条件列创建索引",
                                tableName, node.getRows() != null ? node.getRows() : 0));
                        rec.setCreateStatement(buildCreateIndexStatement(tableName, whereColumns, "where"));
                        rec.setEstimatedImprovement(estimateImprovement(node));
                        recommendations.add(rec);
                    }
                }
            }

            // 2. JOIN 条件列索引推荐
            if (isFullScan || noIndexUsed) {
                List<String> joinColumns = extractJoinColumns(sql, tableName);
                if (!joinColumns.isEmpty()) {
                    String indexKey = tableName + "_join_" + String.join("_", joinColumns);
                    if (recommendedIndexKeys.add(indexKey)) {
                        IndexRecommendation rec = new IndexRecommendation();
                        rec.setTableName(tableName);
                        rec.setColumns(joinColumns);
                        rec.setIndexType("BTREE");
                        rec.setReason(String.format("表 '%s' 的 JOIN 条件列 %s 未使用索引，建议创建索引以优化连接性能",
                                tableName, joinColumns));
                        rec.setCreateStatement(buildCreateIndexStatement(tableName, joinColumns, "join"));
                        rec.setEstimatedImprovement(estimateJoinImprovement(node));
                        recommendations.add(rec);
                    }
                }
            }

            // 3. ORDER BY 列索引推荐（有文件排序时）
            if (hasFilesort) {
                List<String> orderByColumns = extractOrderByColumns(sql, tableName);
                if (!orderByColumns.isEmpty()) {
                    String indexKey = tableName + "_order_" + String.join("_", orderByColumns);
                    if (recommendedIndexKeys.add(indexKey)) {
                        IndexRecommendation rec = new IndexRecommendation();
                        rec.setTableName(tableName);
                        rec.setColumns(orderByColumns);
                        rec.setIndexType("BTREE");
                        rec.setReason(String.format("表 '%s' 使用了文件排序 (Using filesort)，建议为 ORDER BY 列 %s 创建索引",
                                tableName, orderByColumns));
                        rec.setCreateStatement(buildCreateIndexStatement(tableName, orderByColumns, "order"));
                        rec.setEstimatedImprovement(estimateSortImprovement(node));
                        recommendations.add(rec);
                    }
                }
            }

            // 4. GROUP BY 列索引推荐（有临时表时）
            if (hasTemporary) {
                List<String> groupByColumns = extractGroupByColumns(sql, tableName);
                if (!groupByColumns.isEmpty()) {
                    String indexKey = tableName + "_group_" + String.join("_", groupByColumns);
                    if (recommendedIndexKeys.add(indexKey)) {
                        IndexRecommendation rec = new IndexRecommendation();
                        rec.setTableName(tableName);
                        rec.setColumns(groupByColumns);
                        rec.setIndexType("BTREE");
                        rec.setReason(String.format("表 '%s' 使用了临时表 (Using temporary)，建议为 GROUP BY 列 %s 创建索引",
                                tableName, groupByColumns));
                        rec.setCreateStatement(buildCreateIndexStatement(tableName, groupByColumns, "group"));
                        rec.setEstimatedImprovement(estimateGroupImprovement(node));
                        recommendations.add(rec);
                    }
                }
            }
        }

        return recommendations;
    }

    /**
     * 基于查询模式推荐索引（不依赖执行计划）。
     * 纯 SQL 分析，适用于无法获取执行计划的场景。
     */
    public List<IndexRecommendation> recommendIndexesFromSql(String sql) {
        List<IndexRecommendation> recommendations = new ArrayList<>();
        if (sql == null || sql.trim().isEmpty()) {
            return recommendations;
        }

        Set<String> recommendedIndexKeys = new HashSet<>();
        Set<String> tables = extractTables(sql);

        for (String tableName : tables) {
            // WHERE 条件列
            List<String> whereColumns = extractWhereColumns(sql, tableName);
            if (!whereColumns.isEmpty()) {
                String indexKey = tableName + "_where_" + String.join("_", whereColumns);
                if (recommendedIndexKeys.add(indexKey)) {
                    IndexRecommendation rec = new IndexRecommendation();
                    rec.setTableName(tableName);
                    rec.setColumns(whereColumns);
                    rec.setIndexType("BTREE");
                    rec.setReason(String.format("建议为表 '%s' 的 WHERE 条件列创建索引", tableName));
                    rec.setCreateStatement(buildCreateIndexStatement(tableName, whereColumns, "where"));
                    rec.setEstimatedImprovement(50.0);
                    recommendations.add(rec);
                }
            }

            // JOIN 条件列
            List<String> joinColumns = extractJoinColumns(sql, tableName);
            if (!joinColumns.isEmpty()) {
                String indexKey = tableName + "_join_" + String.join("_", joinColumns);
                if (recommendedIndexKeys.add(indexKey)) {
                    IndexRecommendation rec = new IndexRecommendation();
                    rec.setTableName(tableName);
                    rec.setColumns(joinColumns);
                    rec.setIndexType("BTREE");
                    rec.setReason(String.format("建议为表 '%s' 的 JOIN 条件列创建索引", tableName));
                    rec.setCreateStatement(buildCreateIndexStatement(tableName, joinColumns, "join"));
                    rec.setEstimatedImprovement(60.0);
                    recommendations.add(rec);
                }
            }

            // ORDER BY 列
            List<String> orderByColumns = extractOrderByColumns(sql, tableName);
            if (!orderByColumns.isEmpty()) {
                String indexKey = tableName + "_order_" + String.join("_", orderByColumns);
                if (recommendedIndexKeys.add(indexKey)) {
                    IndexRecommendation rec = new IndexRecommendation();
                    rec.setTableName(tableName);
                    rec.setColumns(orderByColumns);
                    rec.setIndexType("BTREE");
                    rec.setReason(String.format("建议为表 '%s' 的 ORDER BY 列创建索引以避免文件排序", tableName));
                    rec.setCreateStatement(buildCreateIndexStatement(tableName, orderByColumns, "order"));
                    rec.setEstimatedImprovement(40.0);
                    recommendations.add(rec);
                }
            }

            // GROUP BY 列
            List<String> groupByColumns = extractGroupByColumns(sql, tableName);
            if (!groupByColumns.isEmpty()) {
                String indexKey = tableName + "_group_" + String.join("_", groupByColumns);
                if (recommendedIndexKeys.add(indexKey)) {
                    IndexRecommendation rec = new IndexRecommendation();
                    rec.setTableName(tableName);
                    rec.setColumns(groupByColumns);
                    rec.setIndexType("BTREE");
                    rec.setReason(String.format("建议为表 '%s' 的 GROUP BY 列创建索引以避免临时表", tableName));
                    rec.setCreateStatement(buildCreateIndexStatement(tableName, groupByColumns, "group"));
                    rec.setEstimatedImprovement(45.0);
                    recommendations.add(rec);
                }
            }
        }

        return recommendations;
    }

    // ========================================================================
    // 低效 SQL 模式检测 (需求 16.4)
    // ========================================================================

    /**
     * 检测 SQL 中的低效模式（反模式）。
     * 纯 SQL 文本分析，不需要数据库连接。
     */
    public List<SqlAntiPattern> detectAntiPatterns(String sql) {
        List<SqlAntiPattern> patterns = new ArrayList<>();
        if (sql == null || sql.trim().isEmpty()) {
            return patterns;
        }

        String upperSql = sql.toUpperCase().trim();

        // SELECT *
        if (upperSql.contains("SELECT *") || upperSql.contains("SELECT  *")) {
            patterns.add(new SqlAntiPattern(
                    "SELECT_STAR",
                    "使用了 SELECT *，查询所有列",
                    "MEDIUM",
                    "建议指定具体需要的字段，减少数据传输量和内存占用"
            ));
        }

        // 缺少 WHERE 条件
        if (isQueryWithoutWhere(upperSql)) {
            patterns.add(new SqlAntiPattern(
                    "MISSING_WHERE",
                    "查询缺少 WHERE 条件，可能导致全表扫描",
                    "HIGH",
                    "添加 WHERE 条件限制查询范围，避免全表扫描"
            ));
        }

        // 前缀模糊查询
        if (LEADING_WILDCARD_PATTERN.matcher(upperSql).find()) {
            patterns.add(new SqlAntiPattern(
                    "LEADING_WILDCARD",
                    "使用了前缀模糊查询 (LIKE '%...')，无法使用索引",
                    "HIGH",
                    "考虑使用全文索引或调整查询方式，避免前缀通配符"
            ));
        }

        // NOT IN / NOT EXISTS
        if (upperSql.contains("NOT IN") || upperSql.contains("NOT EXISTS")) {
            patterns.add(new SqlAntiPattern(
                    "NOT_IN_EXISTS",
                    "使用了 NOT IN 或 NOT EXISTS，性能可能较差",
                    "MEDIUM",
                    "考虑改用 LEFT JOIN + IS NULL 替代"
            ));
        }

        // ORDER BY 无 LIMIT
        if (upperSql.contains("ORDER BY") && !upperSql.contains("LIMIT")) {
            patterns.add(new SqlAntiPattern(
                    "ORDER_WITHOUT_LIMIT",
                    "有 ORDER BY 但无 LIMIT，大数据量时排序开销大",
                    "LOW",
                    "添加 LIMIT 限制返回行数，减少排序开销"
            ));
        }

        // 隐式类型转换风险：函数作用于索引列
        if (FUNCTION_ON_COLUMN_PATTERN.matcher(upperSql).find()) {
            patterns.add(new SqlAntiPattern(
                    "FUNCTION_ON_COLUMN",
                    "在 WHERE 条件中对列使用了函数，可能导致索引失效",
                    "MEDIUM",
                    "避免在 WHERE 条件的列上使用函数，改为对常量值做转换"
            ));
        }

        return patterns;
    }

    // ========================================================================
    // 综合优化建议 (需求 16.5)
    // ========================================================================

    /**
     * 为慢查询日志生成综合优化建议。
     */
    public OptimizationSuggestion generateSuggestions(SlowQueryLog queryLog) {
        OptimizationSuggestion suggestion = new OptimizationSuggestion();
        suggestion.setSqlHash(queryLog.getSqlHash());
        suggestion.setSqlSummary(truncate(queryLog.getSqlText(), 200));

        // 检测反模式
        List<SqlAntiPattern> antiPatterns = detectAntiPatterns(queryLog.getSqlText());
        suggestion.setAntiPatterns(antiPatterns);

        // 汇总建议文本
        for (SqlAntiPattern ap : antiPatterns) {
            suggestion.getTips().add("[" + ap.getSeverity() + "] " + ap.getDescription() + " → " + ap.getSuggestion());
        }

        if (suggestion.getTips().isEmpty()) {
            suggestion.getTips().add("建议通过 EXPLAIN 分析执行计划，检查索引使用情况");
        }

        return suggestion;
    }

    /**
     * 为慢查询日志生成综合优化建议（含执行计划分析）。
     */
    public OptimizationSuggestion generateSuggestionsWithPlan(SlowQueryLog queryLog, Long dataSourceId) {
        OptimizationSuggestion suggestion = generateSuggestions(queryLog);

        if (dataSourceId != null && queryLog.getSqlText() != null) {
            try {
                ExecutionPlan plan = analyzeExplain(queryLog.getSqlText(), dataSourceId);
                suggestion.setExecutionPlan(plan);

                List<IndexRecommendation> indexRecs = recommendIndexesFromPlan(plan, queryLog.getSqlText());
                suggestion.setIndexRecommendations(indexRecs);

                for (String warning : plan.getWarnings()) {
                    suggestion.getTips().add("[PLAN] " + warning);
                }
                for (IndexRecommendation rec : indexRecs) {
                    suggestion.getTips().add("[INDEX] " + rec.getReason());
                }
            } catch (Exception e) {
                log.warn("执行计划分析失败: {}", e.getMessage());
                suggestion.getTips().add("执行计划分析失败: " + e.getMessage());
            }
        }

        return suggestion;
    }

    // ========================================================================
    // 慢查询分析报告 (需求 16.5)
    // ========================================================================

    /**
     * 生成慢查询分析报告。
     * 
     * 报告包含：
     * - 查询频率统计
     * - 执行时间分析（平均、最大、P90、P95、P99）
     * - 反模式检测结果汇总
     * - 索引建议
     * - 综合优化建议
     * 
     * @param slowQueryLogs 慢查询日志列表
     * @return 分析报告
     * 
     * **Validates: Requirements 16.5**
     */
    public SlowQueryAnalysisReport generateAnalysisReport(List<SlowQueryLog> slowQueryLogs) {
        SlowQueryAnalysisReport report = SlowQueryAnalysisReport.builder()
                .generatedAt(java.time.LocalDateTime.now())
                .frequencyStats(new ArrayList<>())
                .slowestQueries(new ArrayList<>())
                .antiPatternSummaries(new ArrayList<>())
                .indexRecommendations(new ArrayList<>())
                .overallSuggestions(new ArrayList<>())
                .dataSourceStats(new ArrayList<>())
                .build();

        if (slowQueryLogs == null || slowQueryLogs.isEmpty()) {
            report.setTotalSlowQueries(0);
            report.setUniqueSqlPatterns(0);
            report.setQueriesWithAntiPatterns(0);
            report.setTablesNeedingIndexes(0);
            report.addOverallSuggestion("没有慢查询记录，系统运行良好");
            return report;
        }

        // 设置分析时间范围
        setAnalysisTimeRange(report, slowQueryLogs);

        // 基础统计
        report.setTotalSlowQueries(slowQueryLogs.size());

        // 按 SQL 哈希分组统计频率
        Map<String, List<SlowQueryLog>> groupedBySqlHash = groupBySqlHash(slowQueryLogs);
        report.setUniqueSqlPatterns(groupedBySqlHash.size());

        // 生成频率统计
        List<SlowQueryAnalysisReport.QueryFrequencyStats> frequencyStatsList = 
                generateFrequencyStats(groupedBySqlHash, slowQueryLogs.size());
        report.setFrequencyStats(frequencyStatsList);

        // 生成执行时间统计
        SlowQueryAnalysisReport.ExecutionTimeStats executionTimeStats = 
                calculateExecutionTimeStats(slowQueryLogs);
        report.setExecutionTimeStats(executionTimeStats);

        // 生成最慢查询列表（Top 10）
        List<SlowQueryAnalysisReport.QueryExecutionDetail> slowestQueries = 
                generateSlowestQueries(slowQueryLogs, 10);
        report.setSlowestQueries(slowestQueries);

        // 汇总反模式检测结果
        Map<String, SlowQueryAnalysisReport.AntiPatternSummary> antiPatternMap = new LinkedHashMap<>();
        int queriesWithAntiPatterns = 0;
        Set<String> tablesNeedingIndexes = new HashSet<>();

        for (SlowQueryAnalysisReport.QueryFrequencyStats stats : frequencyStatsList) {
            if (!stats.getAntiPatterns().isEmpty()) {
                queriesWithAntiPatterns += stats.getOccurrenceCount();
                for (SqlAntiPattern ap : stats.getAntiPatterns()) {
                    antiPatternMap.compute(ap.getPatternType(), (k, v) -> {
                        if (v == null) {
                            return SlowQueryAnalysisReport.AntiPatternSummary.builder()
                                    .patternType(ap.getPatternType())
                                    .description(ap.getDescription())
                                    .severity(ap.getSeverity())
                                    .occurrenceCount(stats.getOccurrenceCount())
                                    .affectedQueries(1)
                                    .suggestion(ap.getSuggestion())
                                    .build();
                        } else {
                            v.setOccurrenceCount(v.getOccurrenceCount() + stats.getOccurrenceCount());
                            v.setAffectedQueries(v.getAffectedQueries() + 1);
                            return v;
                        }
                    });
                }
            }

            // 收集索引建议
            List<IndexRecommendation> indexRecs = recommendIndexesFromSql(stats.getFullSql());
            for (IndexRecommendation rec : indexRecs) {
                tablesNeedingIndexes.add(rec.getTableName());
                // 避免重复添加相同的索引建议
                boolean exists = report.getIndexRecommendations().stream()
                        .anyMatch(r -> r.getCreateStatement().equals(rec.getCreateStatement()));
                if (!exists) {
                    report.addIndexRecommendation(rec);
                }
            }
        }

        report.setQueriesWithAntiPatterns(queriesWithAntiPatterns);
        report.setAntiPatternSummaries(new ArrayList<>(antiPatternMap.values()));
        report.setTablesNeedingIndexes(tablesNeedingIndexes.size());

        // 按数据源分组统计
        List<SlowQueryAnalysisReport.DataSourceStats> dataSourceStatsList = 
                generateDataSourceStats(slowQueryLogs);
        report.setDataSourceStats(dataSourceStatsList);

        // 生成综合优化建议
        generateOverallSuggestions(report);

        return report;
    }

    /**
     * 设置分析时间范围
     */
    private void setAnalysisTimeRange(SlowQueryAnalysisReport report, List<SlowQueryLog> logs) {
        java.time.LocalDateTime earliest = null;
        java.time.LocalDateTime latest = null;

        for (SlowQueryLog log : logs) {
            if (log.getQueryTime() != null) {
                java.time.LocalDateTime queryTime = log.getQueryTime().toInstant()
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDateTime();
                if (earliest == null || queryTime.isBefore(earliest)) {
                    earliest = queryTime;
                }
                if (latest == null || queryTime.isAfter(latest)) {
                    latest = queryTime;
                }
            }
        }

        report.setAnalysisStartTime(earliest);
        report.setAnalysisEndTime(latest);
    }

    /**
     * 按 SQL 哈希分组
     */
    private Map<String, List<SlowQueryLog>> groupBySqlHash(List<SlowQueryLog> logs) {
        Map<String, List<SlowQueryLog>> grouped = new LinkedHashMap<>();
        for (SlowQueryLog log : logs) {
            String hash = log.getSqlHash() != null ? log.getSqlHash() : "unknown";
            grouped.computeIfAbsent(hash, k -> new ArrayList<>()).add(log);
        }
        return grouped;
    }

    /**
     * 生成频率统计
     */
    private List<SlowQueryAnalysisReport.QueryFrequencyStats> generateFrequencyStats(
            Map<String, List<SlowQueryLog>> groupedBySqlHash, int totalQueries) {
        
        List<SlowQueryAnalysisReport.QueryFrequencyStats> statsList = new ArrayList<>();

        for (Map.Entry<String, List<SlowQueryLog>> entry : groupedBySqlHash.entrySet()) {
            String sqlHash = entry.getKey();
            List<SlowQueryLog> logs = entry.getValue();

            if (logs.isEmpty()) continue;

            SlowQueryLog firstLog = logs.get(0);
            String fullSql = firstLog.getSqlText();
            String sqlSummary = truncate(fullSql, 200);

            // 计算执行时间统计
            long totalTime = 0;
            long maxTime = Long.MIN_VALUE;
            long minTime = Long.MAX_VALUE;

            for (SlowQueryLog log : logs) {
                long execTime = log.getExecutionTime() != null ? log.getExecutionTime() : 0;
                totalTime += execTime;
                maxTime = Math.max(maxTime, execTime);
                minTime = Math.min(minTime, execTime);
            }

            double avgTime = logs.size() > 0 ? (double) totalTime / logs.size() : 0;
            double percentage = totalQueries > 0 ? (double) logs.size() / totalQueries * 100 : 0;

            // 检测反模式
            List<SqlAntiPattern> antiPatterns = detectAntiPatterns(fullSql);

            // 生成建议
            List<String> suggestions = new ArrayList<>();
            for (SqlAntiPattern ap : antiPatterns) {
                suggestions.add("[" + ap.getSeverity() + "] " + ap.getSuggestion());
            }
            if (suggestions.isEmpty()) {
                suggestions.add("建议通过 EXPLAIN 分析执行计划");
            }

            SlowQueryAnalysisReport.QueryFrequencyStats stats = SlowQueryAnalysisReport.QueryFrequencyStats.builder()
                    .sqlHash(sqlHash)
                    .sqlSummary(sqlSummary)
                    .fullSql(fullSql)
                    .occurrenceCount(logs.size())
                    .percentageOfTotal(percentage)
                    .avgExecutionTime(avgTime)
                    .maxExecutionTime(maxTime == Long.MIN_VALUE ? 0 : maxTime)
                    .minExecutionTime(minTime == Long.MAX_VALUE ? 0 : minTime)
                    .totalExecutionTime(totalTime)
                    .dataSourceName(firstLog.getDataSourceName())
                    .antiPatterns(antiPatterns)
                    .suggestions(suggestions)
                    .build();

            statsList.add(stats);
        }

        // 按出现次数降序排序
        statsList.sort((a, b) -> Integer.compare(b.getOccurrenceCount(), a.getOccurrenceCount()));

        return statsList;
    }

    /**
     * 计算执行时间统计
     */
    private SlowQueryAnalysisReport.ExecutionTimeStats calculateExecutionTimeStats(List<SlowQueryLog> logs) {
        if (logs.isEmpty()) {
            return SlowQueryAnalysisReport.ExecutionTimeStats.builder()
                    .avgExecutionTime(0)
                    .maxExecutionTime(0)
                    .minExecutionTime(0)
                    .medianExecutionTime(0)
                    .p90ExecutionTime(0)
                    .p95ExecutionTime(0)
                    .p99ExecutionTime(0)
                    .totalExecutionTime(0)
                    .standardDeviation(0)
                    .build();
        }

        // 收集所有执行时间
        List<Long> executionTimes = new ArrayList<>();
        long totalTime = 0;
        long maxTime = Long.MIN_VALUE;
        long minTime = Long.MAX_VALUE;

        for (SlowQueryLog log : logs) {
            long execTime = log.getExecutionTime() != null ? log.getExecutionTime() : 0;
            executionTimes.add(execTime);
            totalTime += execTime;
            maxTime = Math.max(maxTime, execTime);
            minTime = Math.min(minTime, execTime);
        }

        // 排序用于计算百分位数
        Collections.sort(executionTimes);

        double avgTime = (double) totalTime / logs.size();
        double medianTime = calculatePercentile(executionTimes, 50);
        double p90Time = calculatePercentile(executionTimes, 90);
        double p95Time = calculatePercentile(executionTimes, 95);
        double p99Time = calculatePercentile(executionTimes, 99);

        // 计算标准差
        double variance = 0;
        for (Long time : executionTimes) {
            variance += Math.pow(time - avgTime, 2);
        }
        double stdDev = Math.sqrt(variance / logs.size());

        return SlowQueryAnalysisReport.ExecutionTimeStats.builder()
                .avgExecutionTime(avgTime)
                .maxExecutionTime(maxTime == Long.MIN_VALUE ? 0 : maxTime)
                .minExecutionTime(minTime == Long.MAX_VALUE ? 0 : minTime)
                .medianExecutionTime(medianTime)
                .p90ExecutionTime(p90Time)
                .p95ExecutionTime(p95Time)
                .p99ExecutionTime(p99Time)
                .totalExecutionTime(totalTime)
                .standardDeviation(stdDev)
                .build();
    }

    /**
     * 计算百分位数
     */
    private double calculatePercentile(List<Long> sortedValues, int percentile) {
        if (sortedValues.isEmpty()) return 0;
        if (percentile <= 0) return sortedValues.get(0);
        if (percentile >= 100) return sortedValues.get(sortedValues.size() - 1);

        double index = (percentile / 100.0) * (sortedValues.size() - 1);
        int lower = (int) Math.floor(index);
        int upper = (int) Math.ceil(index);

        if (lower == upper) {
            return sortedValues.get(lower);
        }

        double fraction = index - lower;
        return sortedValues.get(lower) * (1 - fraction) + sortedValues.get(upper) * fraction;
    }

    /**
     * 生成最慢查询列表
     */
    private List<SlowQueryAnalysisReport.QueryExecutionDetail> generateSlowestQueries(
            List<SlowQueryLog> logs, int limit) {
        
        // 按执行时间降序排序
        List<SlowQueryLog> sorted = new ArrayList<>(logs);
        sorted.sort((a, b) -> {
            long timeA = a.getExecutionTime() != null ? a.getExecutionTime() : 0;
            long timeB = b.getExecutionTime() != null ? b.getExecutionTime() : 0;
            return Long.compare(timeB, timeA);
        });

        List<SlowQueryAnalysisReport.QueryExecutionDetail> details = new ArrayList<>();
        int count = 0;

        for (SlowQueryLog log : sorted) {
            if (count >= limit) break;

            List<SqlAntiPattern> antiPatterns = detectAntiPatterns(log.getSqlText());
            List<IndexRecommendation> indexRecs = recommendIndexesFromSql(log.getSqlText());

            java.time.LocalDateTime queryTime = null;
            if (log.getQueryTime() != null) {
                queryTime = log.getQueryTime().toInstant()
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDateTime();
            }

            SlowQueryAnalysisReport.QueryExecutionDetail detail = SlowQueryAnalysisReport.QueryExecutionDetail.builder()
                    .sqlHash(log.getSqlHash())
                    .sqlSummary(truncate(log.getSqlText(), 200))
                    .executionTime(log.getExecutionTime() != null ? log.getExecutionTime() : 0)
                    .rowsExamined(log.getRowsExamined())
                    .rowsReturned(log.getRowsReturned())
                    .queryTime(queryTime)
                    .dataSourceName(log.getDataSourceName())
                    .userName(log.getUserName())
                    .antiPatterns(antiPatterns)
                    .indexRecommendations(indexRecs)
                    .build();

            details.add(detail);
            count++;
        }

        return details;
    }

    /**
     * 生成数据源统计
     */
    private List<SlowQueryAnalysisReport.DataSourceStats> generateDataSourceStats(List<SlowQueryLog> logs) {
        Map<Long, List<SlowQueryLog>> groupedByDataSource = new LinkedHashMap<>();

        for (SlowQueryLog log : logs) {
            Long dsId = log.getDataSourceId() != null ? log.getDataSourceId() : -1L;
            groupedByDataSource.computeIfAbsent(dsId, k -> new ArrayList<>()).add(log);
        }

        List<SlowQueryAnalysisReport.DataSourceStats> statsList = new ArrayList<>();
        int totalQueries = logs.size();

        for (Map.Entry<Long, List<SlowQueryLog>> entry : groupedByDataSource.entrySet()) {
            Long dsId = entry.getKey();
            List<SlowQueryLog> dsLogs = entry.getValue();

            if (dsLogs.isEmpty()) continue;

            String dsName = dsLogs.get(0).getDataSourceName();
            long totalTime = 0;
            long maxTime = 0;
            long totalRowsExamined = 0;

            for (SlowQueryLog log : dsLogs) {
                long execTime = log.getExecutionTime() != null ? log.getExecutionTime() : 0;
                totalTime += execTime;
                maxTime = Math.max(maxTime, execTime);
                if (log.getRowsExamined() != null) {
                    totalRowsExamined += log.getRowsExamined();
                }
            }

            double avgTime = dsLogs.size() > 0 ? (double) totalTime / dsLogs.size() : 0;
            double percentage = totalQueries > 0 ? (double) dsLogs.size() / totalQueries * 100 : 0;

            SlowQueryAnalysisReport.DataSourceStats stats = SlowQueryAnalysisReport.DataSourceStats.builder()
                    .dataSourceId(dsId == -1L ? null : dsId)
                    .dataSourceName(dsName)
                    .slowQueryCount(dsLogs.size())
                    .avgExecutionTime(avgTime)
                    .maxExecutionTime(maxTime)
                    .totalRowsExamined(totalRowsExamined)
                    .percentageOfTotal(percentage)
                    .build();

            statsList.add(stats);
        }

        // 按慢查询数量降序排序
        statsList.sort((a, b) -> Integer.compare(b.getSlowQueryCount(), a.getSlowQueryCount()));

        return statsList;
    }

    /**
     * 生成综合优化建议
     */
    private void generateOverallSuggestions(SlowQueryAnalysisReport report) {
        // 基于反模式生成建议
        for (SlowQueryAnalysisReport.AntiPatternSummary summary : report.getAntiPatternSummaries()) {
            if ("HIGH".equals(summary.getSeverity())) {
                report.addOverallSuggestion(String.format(
                        "[高优先级] %s - 影响 %d 个查询，共出现 %d 次。建议: %s",
                        summary.getDescription(),
                        summary.getAffectedQueries(),
                        summary.getOccurrenceCount(),
                        summary.getSuggestion()));
            }
        }

        // 基于索引建议生成建议
        if (!report.getIndexRecommendations().isEmpty()) {
            report.addOverallSuggestion(String.format(
                    "[索引优化] 发现 %d 个表需要添加索引，预计可显著提升查询性能",
                    report.getTablesNeedingIndexes()));
        }

        // 基于执行时间统计生成建议
        SlowQueryAnalysisReport.ExecutionTimeStats timeStats = report.getExecutionTimeStats();
        if (timeStats != null) {
            if (timeStats.getP95ExecutionTime() > 10000) {
                report.addOverallSuggestion(String.format(
                        "[性能警告] P95 执行时间为 %.2f ms，超过 10 秒，建议重点优化最慢的查询",
                        timeStats.getP95ExecutionTime()));
            }
            if (timeStats.getStandardDeviation() > timeStats.getAvgExecutionTime()) {
                report.addOverallSuggestion(
                        "[性能波动] 查询执行时间波动较大，建议检查是否存在锁竞争或资源争用");
            }
        }

        // 基于频率统计生成建议
        if (!report.getFrequencyStats().isEmpty()) {
            SlowQueryAnalysisReport.QueryFrequencyStats topQuery = report.getFrequencyStats().get(0);
            if (topQuery.getPercentageOfTotal() > 30) {
                report.addOverallSuggestion(String.format(
                        "[热点查询] SQL '%s' 占总慢查询的 %.1f%%，建议优先优化此查询",
                        topQuery.getSqlSummary(),
                        topQuery.getPercentageOfTotal()));
            }
        }

        // 基于数据源统计生成建议
        for (SlowQueryAnalysisReport.DataSourceStats dsStats : report.getDataSourceStats()) {
            if (dsStats.getPercentageOfTotal() > 50) {
                report.addOverallSuggestion(String.format(
                        "[数据源热点] 数据源 '%s' 产生了 %.1f%% 的慢查询，建议检查该数据源的性能",
                        dsStats.getDataSourceName(),
                        dsStats.getPercentageOfTotal()));
            }
        }

        // 如果没有任何建议，添加默认建议
        if (report.getOverallSuggestions().isEmpty()) {
            report.addOverallSuggestion("系统整体运行良好，建议定期监控慢查询趋势");
        }
    }

    // ========================================================================
    // 内部辅助方法
    // ========================================================================

    /** 前缀模糊查询模式 */
    private static final Pattern LEADING_WILDCARD_PATTERN =
            Pattern.compile("LIKE\\s+'%", Pattern.CASE_INSENSITIVE);

    /** WHERE 条件中对列使用函数的模式 */
    private static final Pattern FUNCTION_ON_COLUMN_PATTERN =
            Pattern.compile("WHERE\\s+.*\\b(DATE|YEAR|MONTH|DAY|UPPER|LOWER|TRIM|SUBSTR|SUBSTRING|CONCAT|CAST|CONVERT)\\s*\\(",
                    Pattern.CASE_INSENSITIVE);

    /** 从 SQL 中提取所有表名 */
    Set<String> extractTables(String sql) {
        Set<String> tables = new LinkedHashSet<>();
        if (sql == null) return tables;

        String upperSql = sql.toUpperCase();
        
        // 提取 FROM 子句中的表
        Pattern fromPattern = Pattern.compile(
                "\\bFROM\\s+([\\w]+)(?:\\s+(?:AS\\s+)?([\\w]+))?",
                Pattern.CASE_INSENSITIVE);
        Matcher fromMatcher = fromPattern.matcher(sql);
        while (fromMatcher.find()) {
            String tableName = fromMatcher.group(1);
            if (!isSqlKeyword(tableName)) {
                tables.add(tableName);
            }
        }

        // 提取 JOIN 子句中的表
        Pattern joinPattern = Pattern.compile(
                "\\bJOIN\\s+([\\w]+)(?:\\s+(?:AS\\s+)?([\\w]+))?",
                Pattern.CASE_INSENSITIVE);
        Matcher joinMatcher = joinPattern.matcher(sql);
        while (joinMatcher.find()) {
            String tableName = joinMatcher.group(1);
            if (!isSqlKeyword(tableName)) {
                tables.add(tableName);
            }
        }

        return tables;
    }

    /** 从 SQL 中提取 WHERE 条件涉及的列名（简化解析） */
    List<String> extractWhereColumns(String sql, String tableName) {
        List<String> columns = new ArrayList<>();
        if (sql == null) return columns;

        String upperSql = sql.toUpperCase();
        int whereIdx = upperSql.indexOf("WHERE");
        if (whereIdx < 0) return columns;

        String whereClause = sql.substring(whereIdx + 5);
        // 截断到 GROUP BY / ORDER BY / LIMIT / HAVING
        for (String keyword : new String[]{"GROUP BY", "ORDER BY", "LIMIT", "HAVING", "UNION"}) {
            int idx = whereClause.toUpperCase().indexOf(keyword);
            if (idx >= 0) {
                whereClause = whereClause.substring(0, idx);
            }
        }

        // 简单提取 column = / column > / column < / column IN / column LIKE 等模式中的列名
        Pattern colPattern = Pattern.compile(
                "(?:(?:\\w+\\.)?)(\\w+)\\s*(?:=|!=|<>|>=?|<=?|\\bIN\\b|\\bLIKE\\b|\\bBETWEEN\\b|\\bIS\\b)",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = colPattern.matcher(whereClause);
        Set<String> seen = new LinkedHashSet<>();
        while (matcher.find()) {
            String col = matcher.group(1);
            // 排除 SQL 关键字
            if (!isSqlKeyword(col) && seen.add(col.toLowerCase())) {
                columns.add(col);
            }
        }

        return columns;
    }

    /** 从 SQL 中提取 JOIN 条件涉及的列名 */
    List<String> extractJoinColumns(String sql, String tableName) {
        List<String> columns = new ArrayList<>();
        if (sql == null || tableName == null) return columns;

        // 匹配 JOIN ... ON 条件
        // 支持格式: table.column = other.column 或 alias.column = other.column
        Pattern joinOnPattern = Pattern.compile(
                "\\bJOIN\\s+(?:[\\w]+)(?:\\s+(?:AS\\s+)?([\\w]+))?\\s+ON\\s+([^\\s]+)\\s*=\\s*([^\\s,;)]+)",
                Pattern.CASE_INSENSITIVE);
        Matcher joinMatcher = joinOnPattern.matcher(sql);
        
        Set<String> seen = new LinkedHashSet<>();
        while (joinMatcher.find()) {
            String leftSide = joinMatcher.group(2);
            String rightSide = joinMatcher.group(3);
            
            // 提取列名（可能带表名前缀）
            extractColumnForTable(leftSide, tableName, seen, columns);
            extractColumnForTable(rightSide, tableName, seen, columns);
        }

        // 也检查简单的 ON 条件（不带 JOIN 关键字的情况）
        Pattern onPattern = Pattern.compile(
                "\\bON\\s+([\\w.]+)\\s*=\\s*([\\w.]+)",
                Pattern.CASE_INSENSITIVE);
        Matcher onMatcher = onPattern.matcher(sql);
        while (onMatcher.find()) {
            String leftSide = onMatcher.group(1);
            String rightSide = onMatcher.group(2);
            
            extractColumnForTable(leftSide, tableName, seen, columns);
            extractColumnForTable(rightSide, tableName, seen, columns);
        }

        return columns;
    }

    /** 从 SQL 中提取 ORDER BY 涉及的列名 */
    List<String> extractOrderByColumns(String sql, String tableName) {
        List<String> columns = new ArrayList<>();
        if (sql == null) return columns;

        String upperSql = sql.toUpperCase();
        int orderByIdx = upperSql.indexOf("ORDER BY");
        if (orderByIdx < 0) return columns;

        String orderByClause = sql.substring(orderByIdx + 8);
        // 截断到 LIMIT / UNION / 分号
        for (String keyword : new String[]{"LIMIT", "UNION", ";"}) {
            int idx = orderByClause.toUpperCase().indexOf(keyword);
            if (idx >= 0) {
                orderByClause = orderByClause.substring(0, idx);
            }
        }

        // 提取列名（可能带 ASC/DESC）
        Pattern colPattern = Pattern.compile(
                "(?:([\\w]+)\\.)?([\\w]+)(?:\\s+(?:ASC|DESC))?",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = colPattern.matcher(orderByClause);
        Set<String> seen = new LinkedHashSet<>();
        while (matcher.find()) {
            String tablePrefix = matcher.group(1);
            String col = matcher.group(2);
            
            // 排除 SQL 关键字和 ASC/DESC
            if (!isSqlKeyword(col) && !"ASC".equalsIgnoreCase(col) && !"DESC".equalsIgnoreCase(col)) {
                // 如果有表前缀，检查是否匹配目标表
                if (tablePrefix == null || tablePrefix.equalsIgnoreCase(tableName) || isAliasForTable(sql, tablePrefix, tableName)) {
                    if (seen.add(col.toLowerCase())) {
                        columns.add(col);
                    }
                }
            }
        }

        return columns;
    }

    /** 从 SQL 中提取 GROUP BY 涉及的列名 */
    List<String> extractGroupByColumns(String sql, String tableName) {
        List<String> columns = new ArrayList<>();
        if (sql == null) return columns;

        String upperSql = sql.toUpperCase();
        int groupByIdx = upperSql.indexOf("GROUP BY");
        if (groupByIdx < 0) return columns;

        String groupByClause = sql.substring(groupByIdx + 8);
        // 截断到 HAVING / ORDER BY / LIMIT / UNION
        for (String keyword : new String[]{"HAVING", "ORDER BY", "LIMIT", "UNION", ";"}) {
            int idx = groupByClause.toUpperCase().indexOf(keyword);
            if (idx >= 0) {
                groupByClause = groupByClause.substring(0, idx);
            }
        }

        // 提取列名
        Pattern colPattern = Pattern.compile(
                "(?:([\\w]+)\\.)?([\\w]+)",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = colPattern.matcher(groupByClause);
        Set<String> seen = new LinkedHashSet<>();
        while (matcher.find()) {
            String tablePrefix = matcher.group(1);
            String col = matcher.group(2);
            
            // 排除 SQL 关键字
            if (!isSqlKeyword(col)) {
                // 如果有表前缀，检查是否匹配目标表
                if (tablePrefix == null || tablePrefix.equalsIgnoreCase(tableName) || isAliasForTable(sql, tablePrefix, tableName)) {
                    if (seen.add(col.toLowerCase())) {
                        columns.add(col);
                    }
                }
            }
        }

        return columns;
    }

    /** 辅助方法：从带表前缀的列引用中提取列名 */
    private void extractColumnForTable(String columnRef, String tableName, Set<String> seen, List<String> columns) {
        if (columnRef == null || columnRef.isEmpty()) return;
        
        String[] parts = columnRef.split("\\.");
        if (parts.length == 2) {
            String tableOrAlias = parts[0].trim();
            String col = parts[1].trim();
            // 检查表名或别名是否匹配
            if (tableOrAlias.equalsIgnoreCase(tableName) && !isSqlKeyword(col)) {
                if (seen.add(col.toLowerCase())) {
                    columns.add(col);
                }
            }
        } else if (parts.length == 1) {
            // 无表前缀的列，假设属于目标表
            String col = parts[0].trim();
            if (!isSqlKeyword(col) && seen.add(col.toLowerCase())) {
                columns.add(col);
            }
        }
    }

    /** 检查别名是否对应目标表 */
    private boolean isAliasForTable(String sql, String alias, String tableName) {
        if (sql == null || alias == null || tableName == null) return false;
        
        // 匹配 tableName alias 或 tableName AS alias
        Pattern aliasPattern = Pattern.compile(
                "\\b" + Pattern.quote(tableName) + "\\s+(?:AS\\s+)?" + Pattern.quote(alias) + "\\b",
                Pattern.CASE_INSENSITIVE);
        return aliasPattern.matcher(sql).find();
    }

    /** 判断是否为 SQL 关键字 */
    private boolean isSqlKeyword(String word) {
        Set<String> keywords = Set.of(
                "AND", "OR", "NOT", "NULL", "TRUE", "FALSE", "IS", "IN",
                "LIKE", "BETWEEN", "EXISTS", "SELECT", "FROM", "WHERE",
                "GROUP", "ORDER", "BY", "HAVING", "LIMIT", "OFFSET",
                "JOIN", "ON", "AS", "SET", "VALUES", "INTO", "UPDATE", "DELETE",
                "ASC", "DESC", "INNER", "LEFT", "RIGHT", "OUTER", "FULL", "CROSS"
        );
        return keywords.contains(word.toUpperCase());
    }

    /** 判断是否为缺少 WHERE 的查询 */
    boolean isQueryWithoutWhere(String upperSql) {
        if (upperSql.startsWith("SELECT") || upperSql.startsWith("UPDATE") || upperSql.startsWith("DELETE")) {
            // 排除子查询中有 WHERE 的情况：只检查最外层
            // 简化：只要整个 SQL 不包含 WHERE 就认为缺少
            return !upperSql.contains("WHERE");
        }
        return false;
    }

    /** 构建 CREATE INDEX 语句 */
    String buildCreateIndexStatement(String tableName, List<String> columns) {
        return buildCreateIndexStatement(tableName, columns, "idx");
    }

    /** 构建 CREATE INDEX 语句（带类型前缀） */
    String buildCreateIndexStatement(String tableName, List<String> columns, String indexTypePrefix) {
        String indexName = "idx_" + tableName + "_" + indexTypePrefix + "_" + String.join("_", columns);
        // 限制索引名长度（MySQL 限制 64 字符）
        if (indexName.length() > 64) {
            indexName = indexName.substring(0, 60) + "_" + Math.abs(indexName.hashCode() % 1000);
        }
        return String.format("CREATE INDEX %s ON %s (%s)",
                indexName, tableName, String.join(", ", columns));
    }

    /** 估算索引优化后的性能提升（WHERE 条件） */
    double estimateImprovement(PlanNode node) {
        if (node.getRows() == null || node.getRows() <= 100) {
            return 10.0;
        } else if (node.getRows() <= 10000) {
            return 50.0;
        } else {
            return 80.0;
        }
    }

    /** 估算 JOIN 索引优化后的性能提升 */
    double estimateJoinImprovement(PlanNode node) {
        if (node.getRows() == null || node.getRows() <= 100) {
            return 20.0;
        } else if (node.getRows() <= 10000) {
            return 60.0;
        } else {
            return 85.0;
        }
    }

    /** 估算排序索引优化后的性能提升 */
    double estimateSortImprovement(PlanNode node) {
        if (node.getRows() == null || node.getRows() <= 1000) {
            return 15.0;
        } else if (node.getRows() <= 100000) {
            return 40.0;
        } else {
            return 70.0;
        }
    }

    /** 估算分组索引优化后的性能提升 */
    double estimateGroupImprovement(PlanNode node) {
        if (node.getRows() == null || node.getRows() <= 1000) {
            return 20.0;
        } else if (node.getRows() <= 100000) {
            return 45.0;
        } else {
            return 75.0;
        }
    }

    /** 安全获取 Integer 值 */
    private Integer getIntOrNull(ResultSet rs, String column) throws SQLException {
        int val = rs.getInt(column);
        return rs.wasNull() ? null : val;
    }

    /** 安全获取 Long 值 */
    private Long getLongOrNull(ResultSet rs, String column) throws SQLException {
        long val = rs.getLong(column);
        return rs.wasNull() ? null : val;
    }

    /** 安全获取 Double 值 */
    private Double getDoubleOrNull(ResultSet rs, String column) throws SQLException {
        double val = rs.getDouble(column);
        return rs.wasNull() ? null : val;
    }

    /** 安全获取 String 值（某些 EXPLAIN 列可能不存在） */
    private String getStringOrNull(ResultSet rs, String column) {
        try {
            return rs.getString(column);
        } catch (SQLException e) {
            return null;
        }
    }

    /** 截断字符串 */
    private String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }
}
