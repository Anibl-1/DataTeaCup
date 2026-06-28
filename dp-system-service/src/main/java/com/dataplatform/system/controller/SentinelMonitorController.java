package com.dataplatform.system.controller;

import com.alibaba.csp.sentinel.node.ClusterNode;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
import com.alibaba.csp.sentinel.slots.clusterbuilder.ClusterBuilderSlot;
import com.alibaba.csp.sentinel.slots.system.SystemRule;
import com.alibaba.csp.sentinel.slots.system.SystemRuleManager;
import com.dataplatform.common.Result;
import com.dataplatform.common.annotation.RequireRole;
import com.dataplatform.infra.sentinel.SentinelConfig;
import com.dataplatform.data.service.sentinel.SystemProtectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Sentinel监控控制器
 * 提供限流、熔断规则的查询和管理接口
 */
@Tag(name = "Sentinel监控", description = "限流熔断监控管理")
@RestController
@RequestMapping("/sentinel")
@RequiredArgsConstructor
@RequireRole("admin")
public class SentinelMonitorController {

    private final SentinelConfig sentinelConfig;
    private final SystemProtectionService systemProtectionService;

    @Operation(summary = "获取监控概览")
    @GetMapping("/overview")
    public Result<SentinelOverview> getOverview() {
        SentinelOverview overview = new SentinelOverview();
        overview.setFlowRuleCount(FlowRuleManager.getRules().size());
        overview.setDegradeRuleCount(DegradeRuleManager.getRules().size());
        overview.setParamFlowRuleCount(ParamFlowRuleManager.getRules().size());
        overview.setSystemRuleCount(SystemRuleManager.getRules().size());
        overview.setSystemStatus(systemProtectionService.getSystemStatus());
        overview.setSystemHealthy(systemProtectionService.isSystemHealthy());
        overview.setTimestamp(LocalDateTime.now());
        return Result.success(overview);
    }

    @Operation(summary = "获取所有限流规则")
    @GetMapping("/flow-rules")
    public Result<List<FlowRuleVO>> getFlowRules() {
        List<FlowRuleVO> rules = FlowRuleManager.getRules().stream()
            .map(this::toFlowRuleVO)
            .collect(Collectors.toList());
        return Result.success(rules);
    }

    @Operation(summary = "获取所有熔断规则")
    @GetMapping("/degrade-rules")
    public Result<List<DegradeRuleVO>> getDegradeRules() {
        List<DegradeRuleVO> rules = DegradeRuleManager.getRules().stream()
            .map(this::toDegradeRuleVO)
            .collect(Collectors.toList());
        return Result.success(rules);
    }

    @Operation(summary = "获取所有热点参数限流规则")
    @GetMapping("/param-flow-rules")
    public Result<List<ParamFlowRule>> getParamFlowRules() {
        return Result.success(ParamFlowRuleManager.getRules());
    }

    @Operation(summary = "获取所有系统保护规则")
    @GetMapping("/system-rules")
    public Result<List<SystemRule>> getSystemRules() {
        return Result.success(SystemRuleManager.getRules());
    }

    @Operation(summary = "获取资源实时统计")
    @GetMapping("/metrics/{resource}")
    public Result<ResourceMetrics> getResourceMetrics(@PathVariable String resource) {
        ClusterNode clusterNode = ClusterBuilderSlot.getClusterNode(resource);
        if (clusterNode == null) {
            return Result.error(404, "资源不存在或无统计数据");
        }
        
        ResourceMetrics metrics = new ResourceMetrics();
        metrics.setResource(resource);
        metrics.setPassQps(clusterNode.passQps());
        metrics.setBlockQps(clusterNode.blockQps());
        metrics.setTotalQps(clusterNode.totalQps());
        metrics.setExceptionQps(clusterNode.exceptionQps());
        metrics.setRt(clusterNode.avgRt());
        metrics.setCurThreadNum(clusterNode.curThreadNum());
        metrics.setSuccessQps(clusterNode.successQps());
        metrics.setTimestamp(LocalDateTime.now());
        
        return Result.success(metrics);
    }

    @Operation(summary = "获取所有资源统计")
    @GetMapping("/metrics")
    public Result<List<ResourceMetrics>> getAllResourceMetrics() {
        Map<com.alibaba.csp.sentinel.slotchain.ResourceWrapper, ClusterNode> clusterNodeMap = ClusterBuilderSlot.getClusterNodeMap();
        List<ResourceMetrics> metricsList = new ArrayList<>();
        
        for (Map.Entry<com.alibaba.csp.sentinel.slotchain.ResourceWrapper, ClusterNode> entry : clusterNodeMap.entrySet()) {
            ClusterNode node = entry.getValue();
            ResourceMetrics metrics = new ResourceMetrics();
            metrics.setResource(entry.getKey().getName());
            metrics.setPassQps(node.passQps());
            metrics.setBlockQps(node.blockQps());
            metrics.setTotalQps(node.totalQps());
            metrics.setExceptionQps(node.exceptionQps());
            metrics.setRt(node.avgRt());
            metrics.setCurThreadNum(node.curThreadNum());
            metrics.setSuccessQps(node.successQps());
            metrics.setTimestamp(LocalDateTime.now());
            metricsList.add(metrics);
        }
        
        return Result.success(metricsList);
    }

    @Operation(summary = "更新限流规则")
    @PutMapping("/flow-rules/{resource}")
    public Result<Void> updateFlowRule(
            @PathVariable String resource,
            @RequestParam int qps) {
        sentinelConfig.updateFlowRule(resource, qps);
        return Result.success();
    }

    @Operation(summary = "更新熔断规则")
    @PutMapping("/degrade-rules/{resource}")
    public Result<Void> updateDegradeRule(
            @PathVariable String resource,
            @RequestParam double errorRatio,
            @RequestParam int timeWindow) {
        sentinelConfig.updateDegradeRule(resource, errorRatio, timeWindow);
        return Result.success();
    }

    @Operation(summary = "更新系统QPS保护")
    @PutMapping("/system/qps")
    public Result<Void> updateSystemQps(@RequestParam double maxQps) {
        systemProtectionService.updateQpsProtection(maxQps);
        return Result.success();
    }

    @Operation(summary = "更新系统线程数保护")
    @PutMapping("/system/thread")
    public Result<Void> updateSystemThread(@RequestParam int maxThread) {
        systemProtectionService.updateThreadProtection(maxThread);
        return Result.success();
    }

    @Operation(summary = "获取系统状态")
    @GetMapping("/system/status")
    public Result<SystemProtectionService.SystemStatus> getSystemStatus() {
        return Result.success(systemProtectionService.getSystemStatus());
    }

    @Operation(summary = "触发自动调整保护")
    @PostMapping("/system/auto-adjust")
    public Result<Void> autoAdjustProtection() {
        systemProtectionService.autoAdjustProtection();
        return Result.success();
    }

    private FlowRuleVO toFlowRuleVO(FlowRule rule) {
        FlowRuleVO vo = new FlowRuleVO();
        vo.setResource(rule.getResource());
        vo.setGrade(rule.getGrade() == 0 ? "QPS" : "并发数");
        vo.setCount(rule.getCount());
        vo.setStrategy(getStrategyName(rule.getStrategy()));
        vo.setControlBehavior(getControlBehaviorName(rule.getControlBehavior()));
        vo.setWarmUpPeriodSec(rule.getWarmUpPeriodSec());
        vo.setMaxQueueingTimeMs(rule.getMaxQueueingTimeMs());
        
        // 获取实时统计
        ClusterNode node = ClusterBuilderSlot.getClusterNode(rule.getResource());
        if (node != null) {
            vo.setCurrentQps(node.passQps());
            vo.setBlockedQps(node.blockQps());
        }
        
        return vo;
    }

    private DegradeRuleVO toDegradeRuleVO(DegradeRule rule) {
        DegradeRuleVO vo = new DegradeRuleVO();
        vo.setResource(rule.getResource());
        vo.setGrade(getDegradeGradeName(rule.getGrade()));
        vo.setCount(rule.getCount());
        vo.setTimeWindow(rule.getTimeWindow());
        vo.setMinRequestAmount(rule.getMinRequestAmount());
        vo.setStatIntervalMs(rule.getStatIntervalMs());
        vo.setSlowRatioThreshold(rule.getSlowRatioThreshold());
        
        // 获取实时统计
        ClusterNode node = ClusterBuilderSlot.getClusterNode(rule.getResource());
        if (node != null) {
            vo.setCurrentErrorRatio(node.exceptionQps() / Math.max(node.totalQps(), 1));
            vo.setCurrentRt(node.avgRt());
        }
        
        return vo;
    }

    private String getStrategyName(int strategy) {
        return switch (strategy) {
            case 0 -> "直接";
            case 1 -> "关联";
            case 2 -> "链路";
            default -> "未知";
        };
    }

    private String getControlBehaviorName(int behavior) {
        return switch (behavior) {
            case 0 -> "快速失败";
            case 1 -> "Warm Up";
            case 2 -> "排队等待";
            case 3 -> "Warm Up + 排队等待";
            default -> "未知";
        };
    }

    private String getDegradeGradeName(int grade) {
        return switch (grade) {
            case 0 -> "慢调用比例";
            case 1 -> "异常比例";
            case 2 -> "异常数";
            default -> "未知";
        };
    }

    @Data
    public static class SentinelOverview {
        private int flowRuleCount;
        private int degradeRuleCount;
        private int paramFlowRuleCount;
        private int systemRuleCount;
        private SystemProtectionService.SystemStatus systemStatus;
        private boolean systemHealthy;
        private LocalDateTime timestamp;
    }

    @Data
    public static class FlowRuleVO {
        private String resource;
        private String grade;
        private double count;
        private String strategy;
        private String controlBehavior;
        private int warmUpPeriodSec;
        private int maxQueueingTimeMs;
        private double currentQps;
        private double blockedQps;
    }

    @Data
    public static class DegradeRuleVO {
        private String resource;
        private String grade;
        private double count;
        private int timeWindow;
        private int minRequestAmount;
        private int statIntervalMs;
        private double slowRatioThreshold;
        private double currentErrorRatio;
        private double currentRt;
    }

    @Data
    public static class ResourceMetrics {
        private String resource;
        private double passQps;
        private double blockQps;
        private double totalQps;
        private double exceptionQps;
        private double rt;
        private int curThreadNum;
        private double successQps;
        private LocalDateTime timestamp;
    }
}
