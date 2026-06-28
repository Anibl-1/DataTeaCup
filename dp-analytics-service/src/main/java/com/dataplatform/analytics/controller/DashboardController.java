package com.dataplatform.analytics.controller;

import com.dataplatform.common.Result;
import com.dataplatform.common.annotation.RequirePermission;
import com.dataplatform.data.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 浠〃鐩樻帶鍒跺櫒
 * 
 * @author dataplatform
 */
@RestController
@RequestMapping("/dashboard")
@RequirePermission("dashboard:read")
public class DashboardController {
    
    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        return Result.success(dashboardService.getStats());
    }

    @GetMapping("/datasource-distribution")
    public Result<List<Map<String, Object>>> getDataSourceDistribution() {
        return Result.success(dashboardService.getDataSourceDistribution());
    }

    @GetMapping("/collect-trend")
    public Result<Map<String, Object>> getCollectTrend() {
        return Result.success(dashboardService.getCollectTrend());
    }

    @GetMapping("/announcements")
    public Result<List<com.dataplatform.data.entity.Announcement>> getAnnouncements() {
        return Result.success(dashboardService.getDashboardAnnouncements());
    }
    
    /**
     * 涓汉宸ヤ綔鍙?
     */
    @GetMapping("/workspace")
    public Result<Map<String, Object>> getWorkspace(jakarta.servlet.http.HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(dashboardService.getWorkspace(userId));
    }
}

