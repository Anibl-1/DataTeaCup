package com.dataplatform.system.controller;

import com.dataplatform.common.annotation.RequirePermission;
import com.dataplatform.common.Result;
import com.dataplatform.data.service.OpsAuditService;
import com.dataplatform.data.service.PerformanceBaselineService;
import com.dataplatform.data.service.alert.AlertAggregationService;
import com.dataplatform.data.service.alert.AlertEscalationService;
import com.dataplatform.data.service.alert.AlertSilenceService;
import com.dataplatform.data.service.alert.OnCallScheduleService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 杩愮淮绠＄悊鍚庡彴API
 * 闇€姹? 16.1, 16.3, 16.5, 16.8
 */
@RestController
@RequestMapping("/ops")
@RequiredArgsConstructor
@RequirePermission(value = {"ops:manage", "ops:monitor"})
public class OpsManagementController {

    private final PerformanceBaselineService baselineService;
    private final AlertAggregationService aggregationService;
    private final AlertEscalationService escalationService;
    private final AlertSilenceService silenceService;
    private final OnCallScheduleService onCallService;
    private final OpsAuditService opsAuditService;

    @Value("${app.version:2.0.0}")
    private String appVersion;

    /**
     * 绯荤粺姒傝
     */
    @GetMapping("/overview")
    public Result<Map<String, Object>> getSystemOverview() {
        Map<String, Object> overview = new LinkedHashMap<>();

        // 鍩烘湰淇℃伅
        overview.put("version", appVersion);
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        overview.put("uptime", runtime.getUptime());
        overview.put("startTime", runtime.getStartTime());
        overview.put("javaVersion", System.getProperty("java.version"));
        overview.put("osName", System.getProperty("os.name"));

        // JVM鍐呭瓨
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        Map<String, Object> jvm = new LinkedHashMap<>();
        jvm.put("heapUsed", memory.getHeapMemoryUsage().getUsed());
        jvm.put("heapMax", memory.getHeapMemoryUsage().getMax());
        jvm.put("nonHeapUsed", memory.getNonHeapMemoryUsage().getUsed());
        jvm.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        jvm.put("threadCount", ManagementFactory.getThreadMXBean().getThreadCount());
        overview.put("jvm", jvm);

        // 鍛婅姒傝
        Map<String, Object> alerts = new LinkedHashMap<>();
        alerts.put("pendingEscalations", escalationService.getPendingCount());
        alerts.put("silenceRules", silenceService.listSilenceRules().size());
        alerts.put("onCallPersons", onCallService.getCurrentOnCallPersons());
        overview.put("alerts", alerts);

        // 鎬ц兘鍩虹嚎
        overview.put("baselines", baselineService.getAllBaselines());

        return Result.success(overview);
    }

    /**
     * 鎬ц兘寮傚父璁板綍
     */
    @GetMapping("/anomalies")
    public Result<List<PerformanceBaselineService.AnomalyRecord>> getAnomalies(
            @RequestParam(defaultValue = "100") int limit) {
        return Result.success(baselineService.getAnomalies(Math.min(limit, 500)));
    }

    /**
     * 鍛婅鑱氬悎缁熻
     */
    @GetMapping("/alert-aggregation")
    public Result<Map<String, Object>> getAlertAggregation() {
        return Result.success(aggregationService.getAggregationStats());
    }

    /**
     * 鍛婅闈欓粯瑙勫垯鍒楄〃
     */
    @GetMapping("/silence-rules")
    public Result<List<AlertSilenceService.SilenceRule>> listSilenceRules() {
        return Result.success(silenceService.listSilenceRules());
    }

    /**
     * 娣诲姞鍛婅闈欓粯瑙勫垯
     */
    @RequirePermission("ops:manage")
    @PostMapping("/silence-rules")
    public Result<String> addSilenceRule(@RequestBody SilenceRuleRequest request) {
        String id = silenceService.addSilenceRule(
                request.getName(), request.getMatchPattern(),
                request.getStartTime(), request.getEndTime(), request.getCreatedBy());
        opsAuditService.recordOperation(
                request.getCreatedBy() != null ? request.getCreatedBy() : "system",
                "silence_rule_create",
                id,
                request.getName(),
                true);
        return Result.success(id);
    }

    /**
     * 鍒犻櫎鍛婅闈欓粯瑙勫垯
     */
    @RequirePermission("ops:manage")
    @DeleteMapping("/silence-rules/{id}")
    public Result<Void> deleteSilenceRule(@PathVariable String id) {
        boolean removed = silenceService.removeSilenceRule(id);
        opsAuditService.recordOperation(
                "system",
                "silence_rule_delete",
                id,
                removed ? "Remove silence rule" : "Silence rule not found",
                removed);
        return Result.success();
    }

    /**
     * 鍛婅鍗囩骇绛栫暐鍒楄〃
     */
    @GetMapping("/escalation-policies")
    public Result<List<AlertEscalationService.EscalationPolicy>> listEscalationPolicies() {
        return Result.success(escalationService.listPolicies());
    }

    /**
     * 鍊肩彮琛ㄥ垪琛?
     */
    @GetMapping("/oncall-schedules")
    public Result<List<OnCallScheduleService.OnCallSchedule>> listOnCallSchedules() {
        return Result.success(onCallService.listSchedules());
    }

    /**
     * 褰撳墠鍊肩彮浜哄憳
     */
    @GetMapping("/oncall/current")
    public Result<List<String>> getCurrentOnCall() {
        return Result.success(onCallService.getCurrentOnCallPersons());
    }

    @Data
    public static class SilenceRuleRequest {
        private String name;
        private String matchPattern;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String createdBy;
    }

    // ========== 杩愮淮瀹¤ ==========

    /**
     * 鏌ヨ杩愮淮瀹¤璁板綍
     */
    @GetMapping("/audit/records")
    public Result<List<OpsAuditService.OpsAuditRecord>> queryAuditRecords(
            @RequestParam(required = false) String operationType,
            @RequestParam(defaultValue = "100") int limit) {
        return Result.success(opsAuditService.queryRecords(operationType, Math.min(limit, 500)));
    }

    /**
     * 杩愮淮瀹¤缁熻
     */
    @GetMapping("/audit/stats")
    public Result<Map<String, Object>> getAuditStats() {
        return Result.success(opsAuditService.getStats());
    }
}
