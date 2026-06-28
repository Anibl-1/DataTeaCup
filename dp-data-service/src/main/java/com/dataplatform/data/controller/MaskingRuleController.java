package com.dataplatform.data.controller;

import com.dataplatform.common.annotation.OperationLog;
import com.dataplatform.common.annotation.RequirePermission;
import com.dataplatform.common.PageResult;
import com.dataplatform.common.Result;
import com.dataplatform.data.entity.MaskingRuleEntity;
import com.dataplatform.data.entity.MaskingRuleRole;
import com.dataplatform.data.service.masking.MaskingEngine;
import com.dataplatform.data.service.masking.MaskingRule;
import com.dataplatform.data.service.masking.MaskingRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 脱敏规则管理控制器
 * 提供脱敏规则配置界面和预览功能（需求 5.9）
 * 
 * @author dataplatform
 */
@Tag(name = "脱敏规则管理", description = "脱敏规则CRUD、角色配置及预览接口")
@RestController
@RequestMapping("/masking/rules")
@RequiredArgsConstructor
@RequirePermission("masking:rule:read")
public class MaskingRuleController {
    
    private final MaskingRuleService maskingRuleService;
    private final MaskingEngine maskingEngine;
    
    // ==================== CRUD 操作 ====================
    
    /**
     * 创建脱敏规则
     * 
     * @param rule 脱敏规则实体
     * @return 创建后的规则
     */
    @Operation(summary = "创建脱敏规则", description = "创建新的脱敏规则")
    @ApiResponse(responseCode = "200", description = "创建成功")
    @RequirePermission("masking:rule:manage")
    @OperationLog(module = "脱敏规则管理", type = OperationLog.OperationType.CREATE, description = "创建脱敏规则")
    @PostMapping
    public Result<MaskingRuleEntity> createRule(@Validated @RequestBody MaskingRuleEntity rule) {
        MaskingRuleEntity created = maskingRuleService.createRule(rule);
        return Result.success(created);
    }
    
    /**
     * 更新脱敏规则
     * 
     * @param id 规则ID
     * @param rule 脱敏规则实体
     * @return 更新后的规则
     */
    @Operation(summary = "更新脱敏规则", description = "根据ID更新脱敏规则")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @RequirePermission("masking:rule:manage")
    @OperationLog(module = "脱敏规则管理", type = OperationLog.OperationType.UPDATE, description = "更新脱敏规则")
    @PutMapping("/{id}")
    public Result<MaskingRuleEntity> updateRule(
            @Parameter(description = "规则ID") @PathVariable Long id,
            @Validated @RequestBody MaskingRuleEntity rule) {
        rule.setId(id);
        MaskingRuleEntity updated = maskingRuleService.updateRule(rule);
        return Result.success(updated);
    }

    /**
     * 删除脱敏规则
     * 
     * @param id 规则ID
     * @return 操作结果
     */
    @Operation(summary = "删除脱敏规则", description = "根据ID删除脱敏规则")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @RequirePermission("masking:rule:manage")
    @OperationLog(module = "脱敏规则管理", type = OperationLog.OperationType.DELETE, description = "删除脱敏规则")
    @DeleteMapping("/{id}")
    public Result<Void> deleteRule(@Parameter(description = "规则ID") @PathVariable Long id) {
        maskingRuleService.deleteRule(id);
        return Result.success();
    }
    
    /**
     * 根据ID获取脱敏规则
     * 
     * @param id 规则ID
     * @return 脱敏规则
     */
    @Operation(summary = "获取脱敏规则详情", description = "根据ID获取脱敏规则详情")
    @ApiResponse(responseCode = "200", description = "成功")
    @GetMapping("/{id}")
    public Result<MaskingRuleEntity> getRuleById(@Parameter(description = "规则ID") @PathVariable Long id) {
        MaskingRuleEntity rule = maskingRuleService.getRuleById(id);
        return Result.success(rule);
    }
    
    /**
     * 获取脱敏规则列表（分页）
     * 
     * @param page 页码
     * @param pageSize 每页大小
     * @return 脱敏规则列表
     */
    @Operation(summary = "获取脱敏规则列表", description = "分页获取脱敏规则列表")
    @ApiResponse(responseCode = "200", description = "成功")
    @GetMapping
    public Result<PageResult<MaskingRuleEntity>> getRuleList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer pageSize) {
        List<MaskingRuleEntity> rules = maskingRuleService.getRulesByPage(page, Math.min(pageSize, 200));
        long total = maskingRuleService.getRuleCount();
        return Result.success(new PageResult<>(rules, total));
    }
    
    /**
     * 启用/禁用脱敏规则
     * 
     * @param id 规则ID
     * @param enabled 是否启用
     * @return 操作结果
     */
    @Operation(summary = "启用/禁用脱敏规则", description = "切换脱敏规则的启用状态")
    @ApiResponse(responseCode = "200", description = "成功")
    @RequirePermission("masking:rule:manage")
    @OperationLog(module = "脱敏规则管理", type = OperationLog.OperationType.UPDATE, description = "切换脱敏规则状态")
    @PutMapping("/{id}/toggle")
    public Result<Void> toggleRuleEnabled(
            @Parameter(description = "规则ID") @PathVariable Long id,
            @Parameter(description = "是否启用") @RequestParam Boolean enabled) {
        maskingRuleService.toggleRuleEnabled(id, enabled);
        return Result.success();
    }
    
    // ==================== 查询接口 ====================
    
    /**
     * 根据字段名查询脱敏规则
     * 
     * @param fieldName 字段名
     * @return 脱敏规则列表
     */
    @Operation(summary = "根据字段名查询规则", description = "根据字段名查询匹配的脱敏规则")
    @ApiResponse(responseCode = "200", description = "成功")
    @GetMapping("/by-field/{fieldName}")
    public Result<List<MaskingRuleEntity>> findByFieldName(
            @Parameter(description = "字段名") @PathVariable String fieldName) {
        List<MaskingRuleEntity> rules = maskingRuleService.findByFieldName(fieldName);
        return Result.success(rules);
    }
    
    /**
     * 根据数据源ID查询脱敏规则
     * 
     * @param dataSourceId 数据源ID
     * @return 脱敏规则列表
     */
    @Operation(summary = "根据数据源查询规则", description = "根据数据源ID查询脱敏规则")
    @ApiResponse(responseCode = "200", description = "成功")
    @GetMapping("/by-datasource/{dataSourceId}")
    public Result<List<MaskingRuleEntity>> findByDataSource(
            @Parameter(description = "数据源ID") @PathVariable Long dataSourceId) {
        List<MaskingRuleEntity> rules = maskingRuleService.findByDataSource(dataSourceId);
        return Result.success(rules);
    }
    
    /**
     * 根据敏感类型查询脱敏规则
     * 
     * @param sensitiveType 敏感类型
     * @return 脱敏规则列表
     */
    @Operation(summary = "根据敏感类型查询规则", description = "根据敏感字段类型查询脱敏规则")
    @ApiResponse(responseCode = "200", description = "成功")
    @GetMapping("/by-type/{sensitiveType}")
    public Result<List<MaskingRuleEntity>> findBySensitiveType(
            @Parameter(description = "敏感类型") @PathVariable String sensitiveType) {
        List<MaskingRuleEntity> rules = maskingRuleService.findBySensitiveType(sensitiveType);
        return Result.success(rules);
    }

    // ==================== 角色管理接口 ====================
    
    /**
     * 为规则分配角色
     * 
     * @param ruleId 规则ID
     * @param roleId 角色ID
     * @param maskingLevel 脱敏级别（1-完全脱敏，2-部分脱敏，3-不脱敏）
     * @return 操作结果
     */
    @Operation(summary = "为规则分配角色", description = "为脱敏规则分配角色并设置脱敏级别")
    @ApiResponse(responseCode = "200", description = "成功")
    @RequirePermission("masking:rule:manage")
    @OperationLog(module = "脱敏规则管理", type = OperationLog.OperationType.UPDATE, description = "为规则分配角色")
    @PostMapping("/{ruleId}/roles/{roleId}")
    public Result<Void> assignRoleToRule(
            @Parameter(description = "规则ID") @PathVariable Long ruleId,
            @Parameter(description = "角色ID") @PathVariable Long roleId,
            @Parameter(description = "脱敏级别") @RequestParam(defaultValue = "1") Integer maskingLevel) {
        maskingRuleService.assignRoleToRule(ruleId, roleId, maskingLevel);
        return Result.success();
    }
    
    /**
     * 从规则移除角色
     * 
     * @param ruleId 规则ID
     * @param roleId 角色ID
     * @return 操作结果
     */
    @Operation(summary = "从规则移除角色", description = "从脱敏规则中移除角色关联")
    @ApiResponse(responseCode = "200", description = "成功")
    @RequirePermission("masking:rule:manage")
    @OperationLog(module = "脱敏规则管理", type = OperationLog.OperationType.DELETE, description = "从规则移除角色")
    @DeleteMapping("/{ruleId}/roles/{roleId}")
    public Result<Void> removeRoleFromRule(
            @Parameter(description = "规则ID") @PathVariable Long ruleId,
            @Parameter(description = "角色ID") @PathVariable Long roleId) {
        maskingRuleService.removeRoleFromRule(ruleId, roleId);
        return Result.success();
    }
    
    /**
     * 获取规则关联的角色列表
     * 
     * @param ruleId 规则ID
     * @return 角色关联列表
     */
    @Operation(summary = "获取规则的角色列表", description = "获取脱敏规则关联的所有角色配置")
    @ApiResponse(responseCode = "200", description = "成功")
    @GetMapping("/{ruleId}/roles")
    public Result<List<MaskingRuleRole>> getRuleRoles(
            @Parameter(description = "规则ID") @PathVariable Long ruleId) {
        List<MaskingRuleRole> roles = maskingRuleService.getRuleRoles(ruleId);
        return Result.success(roles);
    }
    
    /**
     * 根据角色查询脱敏规则
     * 
     * @param roleId 角色ID
     * @return 脱敏规则列表
     */
    @Operation(summary = "根据角色查询规则", description = "获取指定角色关联的所有脱敏规则")
    @ApiResponse(responseCode = "200", description = "成功")
    @GetMapping("/by-role/{roleId}")
    public Result<List<MaskingRuleEntity>> getRulesByRole(
            @Parameter(description = "角色ID") @PathVariable Long roleId) {
        List<MaskingRuleEntity> rules = maskingRuleService.getRulesByRole(roleId);
        return Result.success(rules);
    }
    
    /**
     * 更新角色的脱敏级别
     * 
     * @param ruleId 规则ID
     * @param roleId 角色ID
     * @param maskingLevel 脱敏级别
     * @return 操作结果
     */
    @Operation(summary = "更新角色脱敏级别", description = "更新角色在指定规则中的脱敏级别")
    @ApiResponse(responseCode = "200", description = "成功")
    @RequirePermission("masking:rule:manage")
    @OperationLog(module = "脱敏规则管理", type = OperationLog.OperationType.UPDATE, description = "更新角色脱敏级别")
    @PutMapping("/{ruleId}/roles/{roleId}/level")
    public Result<Void> updateRoleMaskingLevel(
            @Parameter(description = "规则ID") @PathVariable Long ruleId,
            @Parameter(description = "角色ID") @PathVariable Long roleId,
            @Parameter(description = "脱敏级别") @RequestParam Integer maskingLevel) {
        maskingRuleService.updateRoleMaskingLevel(ruleId, roleId, maskingLevel);
        return Result.success();
    }
    
    // ==================== 预览接口 ====================
    
    /**
     * 预览脱敏效果
     * 
     * @param request 预览请求
     * @return 脱敏后的结果
     */
    @Operation(summary = "预览脱敏效果", description = "预览脱敏规则对样本数据的处理效果")
    @ApiResponse(responseCode = "200", description = "成功")
    @PostMapping("/preview")
    public Result<MaskingPreviewResponse> previewMasking(@RequestBody MaskingPreviewRequest request) {
        // 构建脱敏规则
        MaskingRule rule = MaskingRule.builder()
                .fieldName(request.getFieldName())
                .strategyType(request.getStrategyType())
                .strategyConfig(request.getStrategyConfig())
                .enabled(true)
                .build();
        
        // 验证规则配置
        String validationError = maskingEngine.validateRule(rule);
        if (validationError != null) {
            return Result.error(validationError);
        }
        
        // 执行预览
        Object maskedValue = maskingEngine.previewMasking(request.getSampleValue(), rule);
        
        MaskingPreviewResponse response = new MaskingPreviewResponse();
        response.setOriginalValue(request.getSampleValue());
        response.setMaskedValue(maskedValue);
        response.setStrategyType(request.getStrategyType());
        
        return Result.success(response);
    }
    
    /**
     * 获取可用的脱敏策略类型
     * 
     * @return 策略类型列表
     */
    @Operation(summary = "获取脱敏策略类型", description = "获取所有可用的脱敏策略类型")
    @ApiResponse(responseCode = "200", description = "成功")
    @GetMapping("/strategy-types")
    public Result<List<String>> getStrategyTypes() {
        List<String> types = maskingEngine.getAvailableStrategyTypes();
        return Result.success(types);
    }
    
    // ==================== 内部类 ====================
    
    /**
     * 脱敏预览请求
     */
    @lombok.Data
    public static class MaskingPreviewRequest {
        /**
         * 样本值
         */
        private Object sampleValue;
        
        /**
         * 字段名
         */
        private String fieldName;
        
        /**
         * 脱敏策略类型
         */
        private String strategyType;
        
        /**
         * 策略配置
         */
        private Map<String, Object> strategyConfig;
    }
    
    /**
     * 脱敏预览响应
     */
    @lombok.Data
    public static class MaskingPreviewResponse {
        /**
         * 原始值
         */
        private Object originalValue;
        
        /**
         * 脱敏后的值
         */
        private Object maskedValue;
        
        /**
         * 使用的策略类型
         */
        private String strategyType;
    }
}
