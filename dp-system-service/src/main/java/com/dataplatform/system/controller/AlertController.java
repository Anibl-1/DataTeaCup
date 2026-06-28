package com.dataplatform.system.controller;

import com.dataplatform.common.Result;
import com.dataplatform.common.annotation.RequirePermission;
import com.dataplatform.data.entity.AlertRule;
import com.dataplatform.data.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@Tag(name = "告警管理", description = "告警规则CRUD、告警记录查询与处理")
@RestController
@RequestMapping("/alert")
@RequirePermission("alert:read")
public class AlertController {

    @Autowired
    private AlertService alertService;

    @Operation(summary = "获取告警规则列表")
    @GetMapping("/rules")
    public Result<List<AlertRule>> listRules() {
        return Result.success(alertService.listRules());
    }

    @Operation(summary = "获取单个告警规则")
    @GetMapping("/rule/{id}")
    public Result<AlertRule> getRule(@PathVariable Long id) {
        AlertRule rule = alertService.getRuleById(id);
        if (rule == null) {
            return Result.error("规则不存在");
        }
        return Result.success(rule);
    }

    @Operation(summary = "创建告警规则")
    @RequirePermission("alert:manage")
    @PostMapping("/rule")
    public Result<Void> createRule(@Validated @RequestBody AlertRule rule, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        rule.setCreateBy(userId);
        alertService.createRule(rule);
        return Result.success();
    }

    @Operation(summary = "更新告警规则")
    @PutMapping("/rule/{id}")
    public Result<Void> updateRule(@PathVariable Long id, @Validated @RequestBody AlertRule rule) {
        rule.setId(id);
        alertService.updateRule(rule);
        return Result.success();
    }

    @Operation(summary = "删除告警规则")
    @RequirePermission("alert:manage")
    @DeleteMapping("/rule/{id}")
    public Result<Void> deleteRule(@PathVariable Long id) {
        alertService.deleteRule(id);
        return Result.success();
    }

    @Operation(summary = "启用/禁用告警规则")
    @PutMapping("/rule/{id}/toggle")
    public Result<Void> toggleRule(@PathVariable Long id) {
        alertService.toggleRule(id);
        return Result.success();
    }

    @Operation(summary = "获取告警记录列表")
    @GetMapping("/records")
    public Result<Map<String, Object>> listRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(alertService.listRecords(page, Math.min(size, 200)));
    }

    @Operation(summary = "解决告警")
    @RequirePermission("alert:manage")
    @PostMapping("/record/{id}/resolve")
    public Result<Void> resolveRecord(
            @PathVariable Long id,
            @RequestParam(required = false) String note,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        alertService.resolveRecord(id, userId, note);
        return Result.success();
    }

    @Operation(summary = "获取告警统计")
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        return Result.success(alertService.getStats());
    }
}
