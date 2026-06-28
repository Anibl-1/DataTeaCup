package com.dataplatform.api.common;

import com.dataplatform.common.Result;
import com.dataplatform.data.service.health.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 健康检查控制器
 */
@Tag(name = "健康检查", description = "系统健康状态监控")
@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
public class HealthController {

    private final HealthCheckService healthCheckService;
    private final AutoRecoveryService autoRecoveryService;
    private final ServiceWarmer serviceWarmer;

    @Operation(summary = "获取健康状态概览")
    @GetMapping
    public Result<HealthStatus> getHealthStatus() {
        return Result.success(healthCheckService.checkHealth());
    }

    @Operation(summary = "获取简单健康状态")
    @GetMapping("/simple")
    public Result<SimpleHealthStatus> getSimpleHealthStatus() {
        HealthStatus status = healthCheckService.checkHealth();
        SimpleHealthStatus simple = new SimpleHealthStatus();
        simple.setStatus(status.getStatus());
        simple.setHealthy(HealthStatus.STATUS_UP.equals(status.getStatus()));
        simple.setTimestamp(status.getCheckTime());
        return Result.success(simple);
    }

    @Operation(summary = "检查特定组件")
    @GetMapping("/component/{name}")
    public Result<ComponentHealth> checkComponent(@PathVariable String name) {
        return Result.success(healthCheckService.checkComponent(name));
    }

    @Operation(summary = "获取所有检查器")
    @GetMapping("/checkers")
    public Result<Map<String, String>> getCheckers() {
        Map<String, HealthChecker> checkers = healthCheckService.getCheckers();
        Map<String, String> result = new java.util.HashMap<>();
        for (Map.Entry<String, HealthChecker> entry : checkers.entrySet()) {
            result.put(entry.getKey(), entry.getValue().isCritical() ? "关键" : "非关键");
        }
        return Result.success(result);
    }

    @Operation(summary = "获取组件健康历史")
    @GetMapping("/history/{componentName}")
    public Result<List<HealthRecord>> getHealthHistory(
            @PathVariable String componentName,
            @RequestParam(defaultValue = "24") int hours) {
        return Result.success(healthCheckService.getHealthHistory(componentName, hours));
    }

    @Operation(summary = "手动触发恢复")
    @PostMapping("/recover/{componentName}")
    public Result<RecoveryResult> triggerRecovery(@PathVariable String componentName) {
        boolean success = autoRecoveryService.attemptRecovery(componentName);
        RecoveryResult result = new RecoveryResult();
        result.setComponentName(componentName);
        result.setSuccess(success);
        result.setRetryCount(autoRecoveryService.getRetryCount(componentName));
        result.setTimestamp(LocalDateTime.now());
        return Result.success(result);
    }

    @Operation(summary = "获取恢复状态")
    @GetMapping("/recovery/{componentName}")
    public Result<RecoveryStatus> getRecoveryStatus(@PathVariable String componentName) {
        RecoveryStatus status = new RecoveryStatus();
        status.setComponentName(componentName);
        status.setInProgress(autoRecoveryService.isRecoveryInProgress(componentName));
        status.setRetryCount(autoRecoveryService.getRetryCount(componentName));
        return Result.success(status);
    }

    @Operation(summary = "获取预热状态")
    @GetMapping("/warmup")
    public Result<WarmupStatus> getWarmupStatus() {
        WarmupStatus status = new WarmupStatus();
        status.setComplete(serviceWarmer.isWarmupComplete());
        status.setErrors(serviceWarmer.getWarmupErrors());
        return Result.success(status);
    }

    @Operation(summary = "存活探针")
    @GetMapping("/liveness")
    public Result<String> liveness() {
        return Result.success("OK");
    }

    @Operation(summary = "就绪探针")
    @GetMapping("/readiness")
    public Result<ReadinessStatus> readiness() {
        ReadinessStatus status = new ReadinessStatus();
        status.setReady(healthCheckService.isHealthy() && serviceWarmer.isWarmupComplete());
        status.setHealthy(healthCheckService.isHealthy());
        status.setWarmupComplete(serviceWarmer.isWarmupComplete());
        return Result.success(status);
    }

    @Data
    public static class SimpleHealthStatus {
        private String status;
        private boolean healthy;
        private LocalDateTime timestamp;
    }

    @Data
    public static class RecoveryResult {
        private String componentName;
        private boolean success;
        private int retryCount;
        private LocalDateTime timestamp;
    }

    @Data
    public static class RecoveryStatus {
        private String componentName;
        private boolean inProgress;
        private int retryCount;
    }

    @Data
    public static class WarmupStatus {
        private boolean complete;
        private List<String> errors;
    }

    @Data
    public static class ReadinessStatus {
        private boolean ready;
        private boolean healthy;
        private boolean warmupComplete;
    }
}
