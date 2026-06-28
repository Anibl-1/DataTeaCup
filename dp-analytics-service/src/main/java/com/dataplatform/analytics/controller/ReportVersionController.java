package com.dataplatform.analytics.controller;

import com.dataplatform.common.Result;
import com.dataplatform.common.annotation.RequirePermission;
import com.dataplatform.data.entity.ReportVersion;
import com.dataplatform.data.service.ReportVersionService;
import com.dataplatform.data.service.ReportVersionService.VersionCompareResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 报表版本控制器
 */
@Tag(name = "Report Version", description = "报表版本管理接口")
@RestController
@RequestMapping("/report-version")
@RequiredArgsConstructor
@RequirePermission("report:read")
public class ReportVersionController {

    private final ReportVersionService reportVersionService;

    @Operation(summary = "创建版本")
    @PostMapping("/create")
    public Result<ReportVersion> createVersion(
            @RequestParam Long reportId,
            @RequestParam(required = false) String summary) {
        // TODO: 从 SecurityContext 获取真实用户ID
        Long userId = 1L;
        ReportVersion version = reportVersionService.createVersion(reportId, summary, userId);
        return Result.success(version);
    }

    @Operation(summary = "获取版本历史")
    @GetMapping("/history/{reportId}")
    public Result<List<ReportVersion>> getVersionHistory(@PathVariable Long reportId) {
        List<ReportVersion> versions = reportVersionService.getVersionHistory(reportId);
        return Result.success(versions);
    }

    @Operation(summary = "获取版本详情")
    @GetMapping("/{versionId}")
    public Result<ReportVersion> getVersion(@PathVariable Long versionId) {
        ReportVersion version = reportVersionService.getVersion(versionId);
        return Result.success(version);
    }

    @Operation(summary = "比较两个版本")
    @GetMapping("/compare")
    public Result<VersionCompareResult> compareVersions(
            @RequestParam Long versionId1,
            @RequestParam Long versionId2) {
        VersionCompareResult result = reportVersionService.compareVersions(versionId1, versionId2);
        return Result.success(result);
    }

    @Operation(summary = "回滚到指定版本")
    @PostMapping("/rollback")
    public Result<Void> rollbackToVersion(
            @RequestParam Long reportId,
            @RequestParam Long versionId) {
        reportVersionService.rollbackToVersion(reportId, versionId);
        return Result.success(null);
    }

    @Operation(summary = "删除版本")
    @DeleteMapping("/{versionId}")
    public Result<Void> deleteVersion(@PathVariable Long versionId) {
        reportVersionService.deleteVersion(versionId);
        return Result.success(null);
    }
}
