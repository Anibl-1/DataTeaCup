package com.dataplatform.data.controller;

import com.dataplatform.common.Result;
import com.dataplatform.common.annotation.RequirePermission;
import com.dataplatform.data.service.SqlAutoCompleteService;
import com.dataplatform.data.service.SqlAutoCompleteService.CompletionItem;
import com.dataplatform.data.service.SqlAutoCompleteService.ColumnInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * SQL 自动补全控制器
 * 
 * 需求: 2.3 - WHEN 用户在 SQL 编辑器中输入时，THE Report_Designer SHALL 提供表名和字段名的自动补全建议
 * 
 * @author dataplatform
 */
@Tag(name = "SQL自动补全", description = "SQL编辑器自动补全接口")
@RestController
@RequestMapping("/sql-autocomplete")
@RequiredArgsConstructor
@RequirePermission("data:query")
public class SqlAutoCompleteController {
    
    private final SqlAutoCompleteService sqlAutoCompleteService;
    
    /**
     * 获取自动补全建议
     * 
     * @param dataSourceId 数据源ID
     * @param prefix 输入前缀
     * @param context SQL上下文（可选）
     * @return 补全建议列表
     */
    @Operation(summary = "获取自动补全建议", description = "根据输入前缀和上下文返回SQL关键字、表名、字段名的补全建议")
    @GetMapping("/completions")
    public Result<List<CompletionItem>> getCompletions(
            @RequestParam(required = false) Long dataSourceId,
            @RequestParam(required = false, defaultValue = "") String prefix,
            @RequestParam(required = false) String context) {
        List<CompletionItem> completions = sqlAutoCompleteService.getCompletions(dataSourceId, prefix, context);
        return Result.success(completions);
    }
    
    /**
     * 获取数据源的表名列表
     * 
     * @param dataSourceId 数据源ID
     * @return 表名列表
     */
    @Operation(summary = "获取表名列表", description = "获取指定数据源的所有表名")
    @GetMapping("/tables")
    public Result<List<String>> getTableNames(@RequestParam Long dataSourceId) {
        List<String> tables = sqlAutoCompleteService.getTableNames(dataSourceId);
        return Result.success(tables);
    }
    
    /**
     * 获取表的字段列表
     * 
     * @param dataSourceId 数据源ID
     * @param tableName 表名
     * @return 字段列表
     */
    @Operation(summary = "获取字段列表", description = "获取指定表的所有字段信息")
    @GetMapping("/columns")
    public Result<List<ColumnInfo>> getTableColumns(
            @RequestParam Long dataSourceId,
            @RequestParam String tableName) {
        List<ColumnInfo> columns = sqlAutoCompleteService.getTableColumns(dataSourceId, tableName);
        return Result.success(columns);
    }
    
    /**
     * 获取SQL关键字列表
     * 
     * @return SQL关键字列表
     */
    @Operation(summary = "获取SQL关键字", description = "获取所有支持的SQL关键字列表")
    @GetMapping("/keywords")
    public Result<List<String>> getSqlKeywords() {
        List<String> keywords = sqlAutoCompleteService.getSqlKeywords();
        return Result.success(keywords);
    }
    
    /**
     * 刷新数据源元数据缓存
     * 
     * @param dataSourceId 数据源ID
     * @return 操作结果
     */
    @Operation(summary = "刷新元数据缓存", description = "刷新指定数据源的元数据缓存")
    @PostMapping("/refresh")
    public Result<Void> refreshMetadata(@RequestParam Long dataSourceId) {
        sqlAutoCompleteService.refreshMetadata(dataSourceId);
        return Result.success(null);
    }
}
