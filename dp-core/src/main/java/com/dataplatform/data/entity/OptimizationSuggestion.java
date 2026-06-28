package com.dataplatform.data.entity;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 查询优化建议（综合）
 */
@Data
public class OptimizationSuggestion {
    /** SQL 哈希 */
    private String sqlHash;
    /** SQL 摘要 */
    private String sqlSummary;
    /** 执行计划分析 */
    private ExecutionPlan executionPlan;
    /** 索引建议 */
    private List<IndexRecommendation> indexRecommendations = new ArrayList<>();
    /** 检测到的反模式 */
    private List<SqlAntiPattern> antiPatterns = new ArrayList<>();
    /** 综合优化建议文本 */
    private List<String> tips = new ArrayList<>();
}
