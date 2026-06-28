package com.dataplatform.analytics.controller;

import com.dataplatform.common.Result;
import com.dataplatform.common.annotation.RequirePermission;
import com.dataplatform.data.entity.DashboardConfig;
import com.dataplatform.data.service.NLDashboardService;
import com.dataplatform.data.service.NLDashboardService.ChartRecommendation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 自然语言生成仪表盘控制器
 */
@Tag(name = "NL Dashboard", description = "自然语言生成仪表盘接口")
@RestController
@RequestMapping("/nl-dashboard")
@RequiredArgsConstructor
@RequirePermission("dashboard:read")
public class NLDashboardController {

    private final NLDashboardService nlDashboardService;

    @Operation(summary = "根据自然语言生成仪表盘")
    @PostMapping("/generate")
    public Result<DashboardConfig> generateDashboard(
            @RequestParam String description,
            @RequestParam Long dataSourceId,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            userId = 1L; // 默认用户
        }
        DashboardConfig dashboard = nlDashboardService.generateDashboard(description, dataSourceId, userId);
        return Result.success(dashboard);
    }

    @Operation(summary = "推荐图表配置")
    @PostMapping("/recommend-charts")
    public Result<List<ChartRecommendation>> recommendCharts(
            @RequestParam Long dataSourceId,
            @RequestParam String description) {
        List<ChartRecommendation> recommendations = nlDashboardService.recommendCharts(dataSourceId, description);
        return Result.success(recommendations);
    }

    @Operation(summary = "带进度的仪表盘生成")
    @PostMapping("/generate-with-progress")
    public Result<Map<String, Object>> generateWithProgress(
            @RequestParam String description,
            @RequestParam Long dataSourceId,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            userId = 1L;
        }
        Map<String, Object> result = nlDashboardService.generateDashboardWithProgress(description, dataSourceId, userId);
        return Result.success(result);
    }
}
