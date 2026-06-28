package com.dataplatform.system.controller;

import com.dataplatform.common.Result;
import com.dataplatform.common.annotation.RequirePermission;
import com.dataplatform.data.service.OpsAuditService;
import com.dataplatform.data.service.upgrade.UpgradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 升级管理控制器
 * 需求: 31.1-31.8
 */
@Tag(name = "升级管理", description = "系统升级与补丁管理")
@RestController
@RequestMapping("/upgrade")
@RequiredArgsConstructor
@RequirePermission("upgrade:manage")
public class UpgradeController {

    private final UpgradeService upgradeService;
    private final OpsAuditService opsAuditService;

    @Value("${upgrade.upload-dir:runtime/upgrade-packages}")
    private String uploadDir;

    @RequirePermission("upgrade:manage")
    @Operation(summary = "检查新版本")
    @GetMapping("/check")
    public Result<UpgradeService.VersionInfo> checkForUpdate() {
        return Result.success(upgradeService.checkForUpdate());
    }

    @Operation(summary = "获取当前版本")
    @GetMapping("/version")
    public Result<String> getCurrentVersion() {
        return Result.success(upgradeService.getCurrentVersion());
    }

    @RequirePermission("upgrade:manage")
    @Operation(summary = "执行升级")
    @PostMapping("/perform")
    public Result<UpgradeService.UpgradeResult> performUpgrade(@RequestBody UpgradeRequest request) {
        UpgradeService.UpgradeResult result = upgradeService.performUpgrade(request.getTargetVersion());
        opsAuditService.recordOperation(
                "system",
                "upgrade_perform",
                request.getTargetVersion(),
                result.getMessage(),
                result.isSuccess());
        return Result.success(result);
    }

    @RequirePermission("upgrade:manage")
    @Operation(summary = "回滚版本")
    @PostMapping("/rollback")
    public Result<UpgradeService.UpgradeResult> rollback(@RequestBody UpgradeRequest request) {
        UpgradeService.UpgradeResult result = upgradeService.rollback(request.getTargetVersion());
        opsAuditService.recordOperation(
                "system",
                "upgrade_rollback",
                request.getTargetVersion(),
                result.getMessage(),
                result.isSuccess());
        return Result.success(result);
    }

    @RequirePermission("upgrade:manage")
    @Operation(summary = "应用热补丁")
    @PostMapping("/hotfix")
    public Result<UpgradeService.UpgradeResult> applyHotfix(@RequestBody HotfixRequest request) {
        UpgradeService.UpgradeResult result = upgradeService.applyHotfix(request.getHotfixId());
        opsAuditService.recordOperation(
                "system",
                "upgrade_hotfix",
                request.getHotfixId(),
                result.getMessage(),
                result.isSuccess());
        return Result.success(result);
    }

    @RequirePermission("upgrade:manage")
    @Operation(summary = "获取升级历史")
    @GetMapping("/history")
    public Result<List<UpgradeService.UpgradeRecord>> getUpgradeHistory() {
        return Result.success(upgradeService.getUpgradeHistory());
    }

    @RequirePermission("upgrade:manage")
    @Operation(summary = "创建备份")
    @PostMapping("/backup")
    public Result<UpgradeService.BackupInfo> createBackup() {
        UpgradeService.BackupInfo backupInfo = upgradeService.createBackup();
        opsAuditService.recordOperation(
                "system",
                "upgrade_backup",
                backupInfo.getBackupId(),
                backupInfo.getPath(),
                true);
        return Result.success(backupInfo);
    }

    @RequirePermission("upgrade:manage")
    @Operation(summary = "上传升级包")
    @PostMapping("/upload")
    public Result<Map<String, Object>> uploadPackage(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.error(400, "升级包不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        String safeFilename = sanitizeFilename(originalFilename);
        String storedFilename = LocalDateTime.now().toString().replace(":", "").replace(".", "")
                + "-" + UUID.randomUUID().toString().substring(0, 8) + "-" + safeFilename;

        try {
            Path targetDir = Path.of(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(targetDir);
            Path targetFile = targetDir.resolve(storedFilename).normalize();
            if (!targetFile.startsWith(targetDir)) {
                return Result.error(400, "升级包文件名非法");
            }

            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("fileName", safeFilename);
            data.put("storedFileName", storedFilename);
            data.put("path", targetFile.toString());
            data.put("size", file.getSize());
            data.put("version", extractVersion(safeFilename));
            data.put("uploadedAt", LocalDateTime.now());

            opsAuditService.recordOperation(
                    "system",
                    "upgrade_upload",
                    safeFilename,
                    targetFile.toString(),
                    true);
            return Result.success(data);
        } catch (Exception e) {
            opsAuditService.recordOperation(
                    "system",
                    "upgrade_upload",
                    safeFilename,
                    e.getMessage(),
                    false);
            return Result.error(500, "上传升级包失败: " + e.getMessage());
        }
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "upgrade-package.bin";
        }
        return Path.of(filename).getFileName().toString().replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private String extractVersion(String filename) {
        String name = filename == null ? "" : filename;
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("(\\d+\\.\\d+(?:\\.\\d+)?(?:[-_][A-Za-z0-9]+)?)")
                .matcher(name);
        return matcher.find() ? matcher.group(1).replace('_', '-') : upgradeService.getCurrentVersion();
    }

    @Data
    public static class UpgradeRequest {
        private String targetVersion;
    }

    @Data
    public static class HotfixRequest {
        private String hotfixId;
    }
}
