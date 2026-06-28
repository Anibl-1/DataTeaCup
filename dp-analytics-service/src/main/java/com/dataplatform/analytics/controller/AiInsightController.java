package com.dataplatform.analytics.controller;

import com.dataplatform.common.Result;
import com.dataplatform.common.annotation.RequirePermission;
import com.dataplatform.data.service.AiInsightService;
import com.dataplatform.data.service.AiInsightService.AnomalyRecord;
import com.dataplatform.data.service.AiInsightService.InsightReport;
import com.dataplatform.data.service.AiInsightService.TrendAnalysis;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI 数据洞察控制器
 */
@Slf4j
@RestController
@RequestMapping("/ai/insight")
@Tag(name = "AI数据洞察", description = "AI自动数据分析与洞察")
@RequirePermission("ai:insight")
public class AiInsightController {

    @Autowired
    private AiInsightService aiInsightService;
    
    @PostMapping("/analyze")
    @Operation(summary = "分析数据集", description = "对指定数据集进行AI分析，返回洞察报告")
    public Result<InsightReport> analyzeDatasetRest(
            @RequestParam Long dataSourceId,
            @RequestParam(required = false) String tableName,
            @RequestParam String sql) {
        InsightReport report = aiInsightService.analyzeDatasetSync(dataSourceId, tableName, sql);
        if (report.getError() != null) {
            return Result.error(report.getError());
        }
        return Result.success(report);
    }
    
    @PostMapping("/report")
    @Operation(summary = "生成洞察报告", description = "生成完整的数据洞察报告")
    public Result<InsightReport> generateReport(
            @RequestParam Long dataSourceId,
            @RequestParam String sql) {
        InsightReport report = aiInsightService.generateInsightReport(dataSourceId, sql);
        if (report.getError() != null) {
            return Result.error(report.getError());
        }
        return Result.success(report);
    }
    
    @PostMapping("/anomalies")
    @Operation(summary = "检测异常值", description = "检测数据中的异常值")
    public Result<List<AnomalyRecord>> detectAnomalies(
            @RequestParam Long dataSourceId,
            @RequestParam String sql,
            @RequestParam(defaultValue = "3.0") Double threshold) {
        List<AnomalyRecord> anomalies = aiInsightService.detectAnomalies(dataSourceId, sql, threshold);
        return Result.success(anomalies);
    }
    
    @PostMapping("/trend")
    @Operation(summary = "分析趋势", description = "分析数据趋势")
    public Result<TrendAnalysis> analyzeTrend(
            @RequestParam Long dataSourceId,
            @RequestParam String sql,
            @RequestParam(required = false) String timeField) {
        TrendAnalysis trend = aiInsightService.analyzeTrend(dataSourceId, sql, timeField);
        return Result.success(trend);
    }
    
    @PostMapping("/detect-and-alert")
    @Operation(summary = "检测并告警", description = "检测数据异常并发送告警")
    public Result<Void> detectAndAlert(
            @RequestParam Long dataSourceId,
            @RequestParam String tableName,
            @RequestParam String sql,
            @RequestParam(defaultValue = "3.0") Double threshold) {
        aiInsightService.detectAndAlert(dataSourceId, tableName, sql, threshold);
        return Result.success(null);
    }
}
