package com.dataplatform.analytics.controller;

import com.dataplatform.common.Result;
import com.dataplatform.common.annotation.RequirePermission;
import com.dataplatform.data.service.DynamicParameterService;
import com.dataplatform.data.service.DynamicParameterService.ParameterOption;
import com.dataplatform.data.service.ParameterRecommendService;
import com.dataplatform.data.service.ParameterRecommendService.RecommendRequest;
import com.dataplatform.data.service.ParameterRecommendService.RecommendResult;
import com.dataplatform.data.service.ParameterRecommendService.RecommendStrategy;
import com.dataplatform.data.service.ParameterValidationService;
import com.dataplatform.data.service.ParameterValidationService.BatchValidationResult;
import com.dataplatform.data.service.ParameterValidationService.ParameterValidationConfig;
import com.dataplatform.data.service.ParameterValidationService.ValidationResult;
import com.dataplatform.common.security.SecurityContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 动态参数控制器
 * 提供从数据库动态获取参数选项的API
 * 
 * @validates 需求 13.2 - 从数据库动态获取参数选项值
 * @validates 需求 13.4 - 实现前后端双重参数校验
 */
@Tag(name = "Dynamic Parameter", description = "动态参数选项接口")
@RestController
@RequestMapping("/dynamic-parameter")
@RequiredArgsConstructor
@RequirePermission("data:query")
public class DynamicParameterController {

    private final DynamicParameterService dynamicParameterService;
    private final ParameterRecommendService parameterRecommendService;
    private final ParameterValidationService parameterValidationService;

    /**
     * 获取参数选项
     */
    @Operation(summary = "获取参数选项", description = "执行SQL查询获取参数选项，支持参数替换")
    @PostMapping("/options")
    public Result<List<ParameterOption>> getParameterOptions(@RequestBody ParameterOptionsRequest request) {
        List<ParameterOption> options = dynamicParameterService.getParameterOptions(
                request.getDataSourceId(),
                request.getSql(),
                request.getDependencies(),
                request.getLabelField(),
                request.getValueField(),
                request.getUseCache() != null ? request.getUseCache() : true
        );
        return Result.success(options);
    }

    /**
     * 预览SQL参数替换结果
     */
    @Operation(summary = "预览SQL参数替换", description = "预览SQL参数替换后的结果，用于调试")
    @PostMapping("/preview-sql")
    public Result<String> previewSql(@RequestBody ParameterOptionsRequest request) {
        String processedSql = dynamicParameterService.substituteParameters(
                request.getSql(),
                request.getDependencies()
        );
        return Result.success(processedSql);
    }

    /**
     * 清除指定数据源的缓存
     */
    @Operation(summary = "清除数据源缓存", description = "清除指定数据源的参数选项缓存")
    @DeleteMapping("/cache/{dataSourceId}")
    public Result<Void> clearCache(@PathVariable Long dataSourceId) {
        dynamicParameterService.clearCache(dataSourceId);
        return Result.success();
    }

    /**
     * 清除所有缓存
     */
    @Operation(summary = "清除所有缓存", description = "清除所有参数选项缓存")
    @DeleteMapping("/cache")
    public Result<Void> clearAllCache() {
        dynamicParameterService.clearAllCache();
        return Result.success();
    }

    /**
     * 获取缓存统计信息
     */
    @Operation(summary = "获取缓存统计", description = "获取参数选项缓存的统计信息")
    @GetMapping("/cache/stats")
    public Result<Map<String, Object>> getCacheStats() {
        Map<String, Object> stats = dynamicParameterService.getCacheStats();
        return Result.success(stats);
    }

    // ========================================================================
    // 参数默认值推荐接口
    // @validates 需求 13.3 - 基于用户历史使用记录智能推荐参数默认值
    // ========================================================================

    /**
     * 获取参数默认值推荐
     */
    @Operation(summary = "获取参数默认值推荐", description = "基于用户历史使用记录推荐参数默认值")
    @PostMapping("/recommend")
    public Result<List<RecommendResult>> getRecommendations(@RequestBody RecommendRequestDTO request) {
        Long userId = SecurityContext.getCurrentUserId();
        
        RecommendRequest recommendRequest = new RecommendRequest();
        recommendRequest.setUserId(userId);
        recommendRequest.setParamName(request.getParamName());
        recommendRequest.setReportId(request.getReportId());
        recommendRequest.setChartId(request.getChartId());
        recommendRequest.setLimit(request.getLimit());
        recommendRequest.setIncludeGlobal(request.getIncludeGlobal());
        
        if (request.getStrategy() != null) {
            recommendRequest.setStrategy(RecommendStrategy.valueOf(request.getStrategy().toUpperCase()));
        }
        
        List<RecommendResult> recommendations = parameterRecommendService.getRecommendations(recommendRequest);
        return Result.success(recommendations);
    }

    /**
     * 获取单个参数的推荐默认值
     */
    @Operation(summary = "获取参数默认值", description = "获取单个参数的推荐默认值")
    @GetMapping("/recommend/default")
    public Result<Object> getDefaultValue(
            @RequestParam String paramName,
            @RequestParam(required = false) Long reportId,
            @RequestParam(required = false) Long chartId) {
        Long userId = SecurityContext.getCurrentUserId();
        Object defaultValue = parameterRecommendService.getDefaultValue(userId, paramName, reportId, chartId);
        return Result.success(defaultValue);
    }

    /**
     * 批量获取参数默认值推荐
     */
    @Operation(summary = "批量获取参数默认值", description = "批量获取多个参数的推荐默认值")
    @PostMapping("/recommend/defaults")
    public Result<Map<String, Object>> getDefaultValues(@RequestBody BatchDefaultsRequestDTO request) {
        Long userId = SecurityContext.getCurrentUserId();
        Map<String, Object> defaults = parameterRecommendService.getDefaultValues(
                userId, request.getParamNames(), request.getReportId(), request.getChartId());
        return Result.success(defaults);
    }

    /**
     * 记录参数使用
     */
    @Operation(summary = "记录参数使用", description = "记录用户的参数使用历史，用于后续推荐")
    @PostMapping("/usage/record")
    public Result<Void> recordUsage(@RequestBody RecordUsageRequestDTO request) {
        Long userId = SecurityContext.getCurrentUserId();
        
        if (request.getParamValues() != null && !request.getParamValues().isEmpty()) {
            // 批量记录
            parameterRecommendService.recordUsageBatch(
                    userId, request.getParamValues(), request.getReportId(), request.getChartId());
        } else if (request.getParamName() != null && request.getParamValue() != null) {
            // 单个记录
            parameterRecommendService.recordUsage(
                    userId, request.getParamName(), request.getParamValue(), 
                    request.getReportId(), request.getChartId());
        }
        
        return Result.success();
    }

    /**
     * 清除参数使用历史
     */
    @Operation(summary = "清除参数使用历史", description = "清除当前用户的参数使用历史")
    @DeleteMapping("/usage/history")
    public Result<Void> clearHistory(@RequestParam(required = false) String paramName) {
        Long userId = SecurityContext.getCurrentUserId();
        parameterRecommendService.clearHistory(userId, paramName);
        return Result.success();
    }

    // ========================================================================
    // 参数校验接口
    // @validates 需求 13.4 - 实现前后端双重参数校验
    // ========================================================================

    /**
     * 校验单个参数
     */
    @Operation(summary = "校验单个参数", description = "根据校验规则校验单个参数值")
    @PostMapping("/validate")
    public Result<ValidationResult> validateParameter(@RequestBody ValidateParameterRequest request) {
        ValidationResult result = parameterValidationService.validateParameter(
                request.getValue(), 
                request.getConfig()
        );
        return Result.success(result);
    }

    /**
     * 批量校验参数
     */
    @Operation(summary = "批量校验参数", description = "根据校验规则批量校验多个参数值")
    @PostMapping("/validate/batch")
    public Result<BatchValidationResult> validateParameters(@RequestBody ValidateParametersRequest request) {
        BatchValidationResult result = parameterValidationService.validateParameters(
                request.getValues(), 
                request.getConfigs()
        );
        return Result.success(result);
    }

    /**
     * 获取内置校验器列表
     */
    @Operation(summary = "获取内置校验器列表", description = "获取所有可用的内置自定义校验器名称")
    @GetMapping("/validate/validators")
    public Result<Set<String>> getBuiltInValidators() {
        Set<String> validators = parameterValidationService.getBuiltInValidatorNames();
        return Result.success(validators);
    }

    /**
     * 校验单个参数请求DTO
     */
    @Data
    public static class ValidateParameterRequest {
        /** 参数值 */
        private Object value;
        /** 校验配置 */
        private ParameterValidationConfig config;
    }

    /**
     * 批量校验参数请求DTO
     */
    @Data
    public static class ValidateParametersRequest {
        /** 参数值映射 */
        private Map<String, Object> values;
        /** 校验配置列表 */
        private List<ParameterValidationConfig> configs;
    }

    /**
     * 推荐请求DTO
     */
    @Data
    public static class RecommendRequestDTO {
        /** 参数名称 */
        private String paramName;
        /** 报表ID（可选） */
        private Long reportId;
        /** 图表ID（可选） */
        private Long chartId;
        /** 推荐策略：FREQUENCY, RECENT, PREFERENCE */
        private String strategy;
        /** 推荐数量 */
        private Integer limit;
        /** 是否包含全局推荐 */
        private Boolean includeGlobal;
    }

    /**
     * 批量获取默认值请求DTO
     */
    @Data
    public static class BatchDefaultsRequestDTO {
        /** 参数名称列表 */
        private List<String> paramNames;
        /** 报表ID（可选） */
        private Long reportId;
        /** 图表ID（可选） */
        private Long chartId;
    }

    /**
     * 记录使用请求DTO
     */
    @Data
    public static class RecordUsageRequestDTO {
        /** 参数名称（单个记录时使用） */
        private String paramName;
        /** 参数值（单个记录时使用） */
        private Object paramValue;
        /** 参数值映射（批量记录时使用） */
        private Map<String, Object> paramValues;
        /** 报表ID（可选） */
        private Long reportId;
        /** 图表ID（可选） */
        private Long chartId;
    }

    /**
     * 参数选项请求
     */
    @Data
    public static class ParameterOptionsRequest {
        /** 数据源ID */
        private Long dataSourceId;
        
        /** SQL查询语句，支持 ${paramName} 占位符 */
        private String sql;
        
        /** 依赖参数值映射 */
        private Map<String, Object> dependencies;
        
        /** 标签字段名（可选，默认自动检测） */
        private String labelField;
        
        /** 值字段名（可选，默认自动检测） */
        private String valueField;
        
        /** 是否使用缓存（默认true） */
        private Boolean useCache;
    }
}