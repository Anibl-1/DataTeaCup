package com.dataplatform.data.controller;

import com.dataplatform.common.Result;
import com.dataplatform.common.annotation.RequirePermission;
import com.dataplatform.data.entity.RlsRule;
import com.dataplatform.data.service.RlsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 行级权限控制器
 */
@Tag(name = "RLS", description = "行级权限接口")
@RestController
@RequestMapping("/rls")
@RequiredArgsConstructor
@RequirePermission("rls:manage")
public class RlsController {

    private final RlsService rlsService;

    @Operation(summary = "保存 RLS 规则")
    @PostMapping("/rule")
    public Result<RlsRule> saveRule(@RequestBody RlsRule rule) {
        RlsRule saved = rlsService.saveRule(rule);
        return Result.success(saved);
    }

    @Operation(summary = "根据角色获取规则")
    @GetMapping("/rules/role/{roleId}")
    public Result<List<RlsRule>> getRulesByRole(@PathVariable Long roleId) {
        List<RlsRule> rules = rlsService.getRulesByRole(roleId);
        return Result.success(rules);
    }

    @Operation(summary = "获取所有规则")
    @GetMapping("/rules")
    public Result<List<RlsRule>> getAllRules() {
        List<RlsRule> rules = rlsService.getAllRules();
        return Result.success(rules);
    }

    @Operation(summary = "根据数据源获取规则")
    @GetMapping("/rules/datasource/{dataSourceId}")
    public Result<List<RlsRule>> getRulesByDataSource(@PathVariable Long dataSourceId) {
        List<RlsRule> rules = rlsService.getRulesByDataSource(dataSourceId);
        return Result.success(rules);
    }

    @Operation(summary = "删除规则")
    @DeleteMapping("/rule/{id}")
    public Result<Void> deleteRule(@PathVariable Long id) {
        rlsService.deleteRule(id);
        return Result.success(null);
    }

    @Operation(summary = "测试 SQL 注入")
    @PostMapping("/test-inject")
    public Result<String> testInject(
            @RequestBody String sql,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String injectedSql = rlsService.injectRlsFilter(sql, userId);
        return Result.success(injectedSql);
    }
}
