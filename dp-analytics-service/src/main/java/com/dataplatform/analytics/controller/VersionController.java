package com.dataplatform.analytics.controller;

import com.dataplatform.common.Result;
import com.dataplatform.common.annotation.RequirePermission;
import com.dataplatform.data.service.version.ResourceVersion;
import com.dataplatform.data.service.version.VersionService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 鐗堟湰绠＄悊API
 * 闇€姹? 24.1, 24.2, 24.3
 */
@RestController
@RequestMapping("/versions")
@RequiredArgsConstructor
@RequirePermission("version:read")
public class VersionController {

    private final VersionService versionService;

    @PostMapping
    public Result<ResourceVersion> saveVersion(@RequestBody SaveVersionRequest req) {
        return Result.success(versionService.saveVersion(
                req.getResourceType(), req.getResourceId(),
                req.getContent(), req.getDescription(), req.getUserId()));
    }

    @GetMapping("/{resourceType}/{resourceId}")
    public Result<List<ResourceVersion>> getHistory(
            @PathVariable String resourceType, @PathVariable String resourceId) {
        return Result.success(versionService.getVersionHistory(resourceType, resourceId));
    }

    @PostMapping("/{resourceType}/{resourceId}/rollback/{versionNumber}")
    public Result<String> rollback(
            @PathVariable String resourceType, @PathVariable String resourceId,
            @PathVariable int versionNumber) {
        return Result.success(versionService.rollback(resourceType, resourceId, versionNumber));
    }

    @GetMapping("/{resourceType}/{resourceId}/compare")
    public Result<VersionService.VersionDiff> compare(
            @PathVariable String resourceType, @PathVariable String resourceId,
            @RequestParam int v1, @RequestParam int v2) {
        return Result.success(versionService.compareVersions(resourceType, resourceId, v1, v2));
    }

    @Data
    public static class SaveVersionRequest {
        private String resourceType;
        private String resourceId;
        private String content;
        private String description;
        private String userId;
    }
}
