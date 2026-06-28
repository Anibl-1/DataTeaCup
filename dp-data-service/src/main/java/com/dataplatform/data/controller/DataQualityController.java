package com.dataplatform.data.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dataplatform.common.PageResult;
import com.dataplatform.common.annotation.RequirePermission;
import com.dataplatform.common.Result;
import com.dataplatform.data.entity.DataQualityReport;
import com.dataplatform.data.entity.DataQualityRule;
import com.dataplatform.data.service.DataQualityService;
import com.dataplatform.data.service.DataQualityService.QualityReport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据质量监控控制器
 */
@Tag(name = "Data Quality", description = "数据质量监控接口")
@RestController
@RequestMapping("/data-quality")
@RequiredArgsConstructor
@RequirePermission("data:quality")
public class DataQualityController {

    private final DataQualityService dataQualityService;

    @Operation(summary = "检查数据质量")
    @PostMapping("/check")
    public Result<QualityReport> checkQuality(
            @RequestParam Long dataSourceId,
            @RequestParam String tableName) {
        QualityReport report = dataQualityService.checkQuality(dataSourceId, tableName);
        return Result.success(report);
    }

    @Operation(summary = "保存质量规则")
    @PostMapping("/rule")
    public Result<DataQualityRule> saveRule(@RequestBody DataQualityRule rule) {
        DataQualityRule saved = dataQualityService.saveQualityRule(rule);
        return Result.success(saved);
    }

    @Operation(summary = "获取质量规则列表")
    @GetMapping("/rules")
    public Result<List<DataQualityRule>> getRules(
            @RequestParam(required = false) Long dataSourceId) {
        List<DataQualityRule> rules = dataQualityService.getQualityRules(dataSourceId);
        return Result.success(rules);
    }

    @Operation(summary = "删除质量规则")
    @DeleteMapping("/rule/{id}")
    public Result<Void> deleteRule(@PathVariable Long id) {
        dataQualityService.deleteRule(id);
        return Result.success(null);
    }

    @Operation(summary = "检查并告警")
    @PostMapping("/check-and-alert")
    public Result<Void> checkAndAlert(
            @RequestParam Long dataSourceId,
            @RequestParam String tableName) {
        dataQualityService.checkAndAlert(dataSourceId, tableName);
        return Result.success(null);
    }

    @Operation(summary = "获取质量报告历史")
    @GetMapping("/reports")
    public Result<PageResult<DataQualityReport>> getReportHistory(
            @RequestParam(required = false) Long dataSourceId,
            @RequestParam(required = false) String tableName,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        size = Math.min(size, 200);
        Page<DataQualityReport> pageResult = dataQualityService.getReportHistory(dataSourceId, tableName, page, size);
        return Result.success(PageResult.of(pageResult.getRecords(), pageResult.getTotal()));
    }

    @Operation(summary = "获取最新报告")
    @GetMapping("/latest-report")
    public Result<DataQualityReport> getLatestReport(
            @RequestParam Long dataSourceId,
            @RequestParam String tableName) {
        DataQualityReport report = dataQualityService.getLatestReport(dataSourceId, tableName);
        return Result.success(report);
    }
}
