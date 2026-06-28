package com.dataplatform.data.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 慢查询分析报告
 * 
 * 包含查询频率、执行时间、反模式检测结果、索引建议和优化建议的综合报告。
 * 
 * 需求引用：
 * - 需求 16.5: 提供慢查询分析报告，包括查询频率、平均执行时间和优化建议
 * 
 * **Validates: Requirements 16.5**
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlowQueryAnalysisReport {
    
    // ========================================================================
    // 报告元数据
    // ========================================================================
    
    /**
     * 报告生成时间
     */
    private LocalDateTime generatedAt;
    
    /**
     * 分析时间范围开始
     */
    private LocalDateTime analysisStartTime;
    
    /**
     * 分析时间范围结束
     */
    private LocalDateTime analysisEndTime;
    
    /**
     * 分析的慢查询总数
     */
    private int totalSlowQueries;
    
    /**
     * 唯一 SQL 模式数量
     */
    private int uniqueSqlPatterns;
    
    // ========================================================================
    // 频率统计
    // ========================================================================
    
    /**
     * 按频率排序的查询统计列表
     */
    @Builder.Default
    private List<QueryFrequencyStats> frequencyStats = new ArrayList<>();
    
    // ========================================================================
    // 执行时间分析
    // ========================================================================
    
    /**
     * 执行时间统计
     */
    private ExecutionTimeStats executionTimeStats;
    
    /**
     * 按执行时间排序的最慢查询列表
     */
    @Builder.Default
    private List<QueryExecutionDetail> slowestQueries = new ArrayList<>();
    
    // ========================================================================
    // 反模式检测结果
    // ========================================================================
    
    /**
     * 检测到的反模式汇总
     */
    @Builder.Default
    private List<AntiPatternSummary> antiPatternSummaries = new ArrayList<>();
    
    /**
     * 包含反模式的查询数量
     */
    private int queriesWithAntiPatterns;
    
    // ========================================================================
    // 索引建议
    // ========================================================================
    
    /**
     * 索引建议列表
     */
    @Builder.Default
    private List<IndexRecommendation> indexRecommendations = new ArrayList<>();
    
    /**
     * 需要索引优化的表数量
     */
    private int tablesNeedingIndexes;
    
    // ========================================================================
    // 综合优化建议
    // ========================================================================
    
    /**
     * 综合优化建议列表
     */
    @Builder.Default
    private List<String> overallSuggestions = new ArrayList<>();
    
    /**
     * 按数据源分组的统计
     */
    @Builder.Default
    private List<DataSourceStats> dataSourceStats = new ArrayList<>();
    
    // ========================================================================
    // 内部类定义
    // ========================================================================
    
    /**
     * 查询频率统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QueryFrequencyStats {
        /** SQL 哈希 */
        private String sqlHash;
        /** SQL 摘要（截断后的 SQL） */
        private String sqlSummary;
        /** 完整 SQL */
        private String fullSql;
        /** 出现次数 */
        private int occurrenceCount;
        /** 占总查询的百分比 */
        private double percentageOfTotal;
        /** 平均执行时间（毫秒） */
        private double avgExecutionTime;
        /** 最大执行时间（毫秒） */
        private long maxExecutionTime;
        /** 最小执行时间（毫秒） */
        private long minExecutionTime;
        /** 总执行时间（毫秒） */
        private long totalExecutionTime;
        /** 数据源名称 */
        private String dataSourceName;
        /** 检测到的反模式 */
        @Builder.Default
        private List<SqlAntiPattern> antiPatterns = new ArrayList<>();
        /** 优化建议 */
        @Builder.Default
        private List<String> suggestions = new ArrayList<>();
    }
    
    /**
     * 执行时间统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExecutionTimeStats {
        /** 平均执行时间（毫秒） */
        private double avgExecutionTime;
        /** 最大执行时间（毫秒） */
        private long maxExecutionTime;
        /** 最小执行时间（毫秒） */
        private long minExecutionTime;
        /** 中位数执行时间（毫秒） */
        private double medianExecutionTime;
        /** P90 执行时间（毫秒） */
        private double p90ExecutionTime;
        /** P95 执行时间（毫秒） */
        private double p95ExecutionTime;
        /** P99 执行时间（毫秒） */
        private double p99ExecutionTime;
        /** 总执行时间（毫秒） */
        private long totalExecutionTime;
        /** 标准差 */
        private double standardDeviation;
    }
    
    /**
     * 查询执行详情
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QueryExecutionDetail {
        /** SQL 哈希 */
        private String sqlHash;
        /** SQL 摘要 */
        private String sqlSummary;
        /** 执行时间（毫秒） */
        private long executionTime;
        /** 扫描行数 */
        private Long rowsExamined;
        /** 返回行数 */
        private Long rowsReturned;
        /** 查询时间 */
        private LocalDateTime queryTime;
        /** 数据源名称 */
        private String dataSourceName;
        /** 用户名 */
        private String userName;
        /** 检测到的反模式 */
        @Builder.Default
        private List<SqlAntiPattern> antiPatterns = new ArrayList<>();
        /** 索引建议 */
        @Builder.Default
        private List<IndexRecommendation> indexRecommendations = new ArrayList<>();
    }
    
    /**
     * 反模式汇总
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AntiPatternSummary {
        /** 反模式类型 */
        private String patternType;
        /** 反模式描述 */
        private String description;
        /** 严重程度 */
        private String severity;
        /** 出现次数 */
        private int occurrenceCount;
        /** 影响的查询数量 */
        private int affectedQueries;
        /** 建议 */
        private String suggestion;
    }
    
    /**
     * 数据源统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataSourceStats {
        /** 数据源 ID */
        private Long dataSourceId;
        /** 数据源名称 */
        private String dataSourceName;
        /** 慢查询数量 */
        private int slowQueryCount;
        /** 平均执行时间（毫秒） */
        private double avgExecutionTime;
        /** 最大执行时间（毫秒） */
        private long maxExecutionTime;
        /** 总扫描行数 */
        private long totalRowsExamined;
        /** 占总慢查询的百分比 */
        private double percentageOfTotal;
    }
    
    // ========================================================================
    // 便捷方法
    // ========================================================================
    
    /**
     * 添加频率统计
     */
    public void addFrequencyStats(QueryFrequencyStats stats) {
        if (frequencyStats == null) {
            frequencyStats = new ArrayList<>();
        }
        frequencyStats.add(stats);
    }
    
    /**
     * 添加最慢查询
     */
    public void addSlowestQuery(QueryExecutionDetail detail) {
        if (slowestQueries == null) {
            slowestQueries = new ArrayList<>();
        }
        slowestQueries.add(detail);
    }
    
    /**
     * 添加反模式汇总
     */
    public void addAntiPatternSummary(AntiPatternSummary summary) {
        if (antiPatternSummaries == null) {
            antiPatternSummaries = new ArrayList<>();
        }
        antiPatternSummaries.add(summary);
    }
    
    /**
     * 添加索引建议
     */
    public void addIndexRecommendation(IndexRecommendation recommendation) {
        if (indexRecommendations == null) {
            indexRecommendations = new ArrayList<>();
        }
        indexRecommendations.add(recommendation);
    }
    
    /**
     * 添加综合建议
     */
    public void addOverallSuggestion(String suggestion) {
        if (overallSuggestions == null) {
            overallSuggestions = new ArrayList<>();
        }
        if (!overallSuggestions.contains(suggestion)) {
            overallSuggestions.add(suggestion);
        }
    }
    
    /**
     * 添加数据源统计
     */
    public void addDataSourceStats(DataSourceStats stats) {
        if (dataSourceStats == null) {
            dataSourceStats = new ArrayList<>();
        }
        dataSourceStats.add(stats);
    }
    
    /**
     * 获取报告摘要
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("慢查询分析报告 (%s 至 %s)\n", 
                analysisStartTime, analysisEndTime));
        sb.append(String.format("- 总慢查询数: %d\n", totalSlowQueries));
        sb.append(String.format("- 唯一 SQL 模式: %d\n", uniqueSqlPatterns));
        sb.append(String.format("- 包含反模式的查询: %d\n", queriesWithAntiPatterns));
        sb.append(String.format("- 需要索引优化的表: %d\n", tablesNeedingIndexes));
        if (executionTimeStats != null) {
            sb.append(String.format("- 平均执行时间: %.2f ms\n", executionTimeStats.getAvgExecutionTime()));
            sb.append(String.format("- P95 执行时间: %.2f ms\n", executionTimeStats.getP95ExecutionTime()));
        }
        return sb.toString();
    }
    
    /**
     * 判断是否有严重问题
     */
    public boolean hasCriticalIssues() {
        // 有 HIGH 严重程度的反模式
        if (antiPatternSummaries != null) {
            for (AntiPatternSummary summary : antiPatternSummaries) {
                if ("HIGH".equals(summary.getSeverity()) && summary.getOccurrenceCount() > 0) {
                    return true;
                }
            }
        }
        // 平均执行时间超过 10 秒
        if (executionTimeStats != null && executionTimeStats.getAvgExecutionTime() > 10000) {
            return true;
        }
        return false;
    }
}
