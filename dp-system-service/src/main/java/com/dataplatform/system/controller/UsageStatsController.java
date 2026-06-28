package com.dataplatform.system.controller;

import com.dataplatform.common.Result;
import com.dataplatform.common.annotation.RequirePermission;
import com.dataplatform.data.service.usage.UsageStats;
import com.dataplatform.data.service.usage.UsageStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 使用统计控制器
 * 需求: 30.6, 30.7
 */
@Tag(name = "使用统计", description = "功能使用统计与分析")
@RestController
@RequestMapping("/usage-stats")
@RequiredArgsConstructor
@RequirePermission(value = {"usage:stats", "system:monitor"})
public class UsageStatsController {

    private final UsageStatsService usageStatsService;

    @Operation(summary = "获取功能使用频率")
    @GetMapping("/feature-usage")
    public Result<Map<String, Long>> getFeatureUsage(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.success(usageStatsService.getFeatureUsageCount(startDate, endDate));
    }

    @Operation(summary = "获取日活跃用户数")
    @GetMapping("/dau")
    public Result<Long> getDau(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.success(usageStatsService.getDailyActiveUsers(date));
    }

    @Operation(summary = "获取周活跃用户数")
    @GetMapping("/wau")
    public Result<Long> getWau(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        return Result.success(usageStatsService.getWeeklyActiveUsers(weekStart));
    }

    @Operation(summary = "获取月活跃用户数")
    @GetMapping("/mau")
    public Result<Long> getMau(@RequestParam int year, @RequestParam int month) {
        return Result.success(usageStatsService.getMonthlyActiveUsers(year, month));
    }

    @Operation(summary = "获取资源访问排行")
    @GetMapping("/resource-rank")
    public Result<List<UsageStatsService.ResourceAccessRank>> getResourceRank(
            @RequestParam String resourceType,
            @RequestParam(defaultValue = "10") int topN,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.success(usageStatsService.getResourceAccessRank(resourceType, topN, startDate, endDate));
    }

    @Operation(summary = "获取查询性能分布")
    @GetMapping("/query-performance")
    public Result<Map<String, Long>> getQueryPerformance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.success(usageStatsService.getQueryPerformanceDistribution(startDate, endDate));
    }

    @Operation(summary = "获取错误率统计")
    @GetMapping("/error-rate")
    public Result<Map<String, Long>> getErrorRate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.success(usageStatsService.getErrorRateStats(startDate, endDate));
    }

    @Operation(summary = "导出统计数据(CSV)")
    @GetMapping("/export/csv")
    public void exportCsv(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "10000") int limit,
            HttpServletResponse response) throws Exception {
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename=usage_stats.csv");

        List<UsageStats> records = usageStatsService.getUsageRecords(startDate, endDate, limit);
        PrintWriter writer = response.getWriter();
        writer.println("时间,用户ID,功能,操作,资源类型,资源ID,耗时(ms),结果数,错误码,客户端");
        for (UsageStats s : records) {
            writer.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                    s.getCreateTime(), nvl(s.getUserId()), nvl(s.getFeatureCode()),
                    nvl(s.getAction()), nvl(s.getResourceType()), nvl(s.getResourceId()),
                    s.getDuration() != null ? s.getDuration() : "",
                    s.getResultCount() != null ? s.getResultCount() : "",
                    nvl(s.getErrorCode()), nvl(s.getClientType()));
        }
        writer.flush();
    }

    private String nvl(String s) { return s != null ? s : ""; }
}
