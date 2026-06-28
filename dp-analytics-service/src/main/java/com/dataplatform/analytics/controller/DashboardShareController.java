package com.dataplatform.analytics.controller;

import com.dataplatform.common.Result;
import com.dataplatform.common.annotation.RequirePermission;
import com.dataplatform.data.entity.DashboardConfig;
import com.dataplatform.data.entity.DashboardShare;
import com.dataplatform.data.service.DashboardShareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 仪表盘分享控制器
 */
@Tag(name = "Dashboard Share", description = "仪表盘分享与嵌入接口")
@RestController
@RequestMapping("/dashboard-share")
@RequiredArgsConstructor
@RequirePermission("dashboard:read")
public class DashboardShareController {

    private final DashboardShareService shareService;

    @Operation(summary = "创建分享链接")
    @PostMapping("/create")
    public Result<DashboardShare> createShareLink(
            @RequestParam Long dashboardId,
            @RequestParam(required = false) Long expireHours,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        DashboardShare share = shareService.createShareLink(dashboardId, expireHours, userId);
        return Result.success(share);
    }

    @Operation(summary = "获取分享的仪表盘")
    @GetMapping("/view/{token}")
    public Result<DashboardConfig> getSharedDashboard(@PathVariable String token) {
        DashboardConfig dashboard = shareService.getSharedDashboard(token);
        return Result.success(dashboard);
    }

    @Operation(summary = "生成嵌入代码")
    @GetMapping("/embed-code")
    public Result<Map<String, String>> generateEmbedCode(
            @RequestParam Long dashboardId,
            HttpServletRequest request) {
        String baseUrl = request.getScheme() + "://" + request.getServerName();
        if (request.getServerPort() != 80 && request.getServerPort() != 443) {
            baseUrl += ":" + request.getServerPort();
        }
        
        String embedCode = shareService.generateEmbedCode(dashboardId, baseUrl);
        
        Map<String, String> result = new HashMap<>();
        result.put("embedCode", embedCode);
        return Result.success(result);
    }

    @Operation(summary = "撤销分享")
    @PostMapping("/revoke")
    public Result<Void> revokeShare(@RequestParam Long dashboardId) {
        shareService.revokeShare(dashboardId);
        return Result.success(null);
    }

    @Operation(summary = "获取分享列表")
    @GetMapping("/list")
    public Result<List<DashboardShare>> getShareList(@RequestParam Long dashboardId) {
        List<DashboardShare> shares = shareService.getShareList(dashboardId);
        return Result.success(shares);
    }

    @Operation(summary = "验证分享 token")
    @GetMapping("/validate/{token}")
    public Result<Boolean> validateToken(@PathVariable String token) {
        boolean valid = shareService.validateToken(token);
        return Result.success(valid);
    }

    @Operation(summary = "删除分享")
    @DeleteMapping("/{id}")
    public Result<Void> deleteShare(@PathVariable Long id) {
        shareService.deleteShare(id);
        return Result.success(null);
    }
}
