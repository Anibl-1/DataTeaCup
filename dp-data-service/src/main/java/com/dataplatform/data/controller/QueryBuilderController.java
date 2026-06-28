package com.dataplatform.data.controller;

import com.dataplatform.common.Result;
import com.dataplatform.common.annotation.RequirePermission;
import com.dataplatform.data.dto.JoinRecommendation;
import com.dataplatform.data.dto.QueryModel;
import com.dataplatform.data.service.JoinRecommendService;
import com.dataplatform.data.service.QueryBuildService;
import com.dataplatform.data.service.QueryBuildService.TableMeta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 可视化查询构建器控制器
 */
@Tag(name = "Query Builder", description = "可视化查询构建器接口")
@RestController
@RequestMapping("/query-builder")
@RequiredArgsConstructor
@RequirePermission("data:query")
public class QueryBuilderController {

    private final QueryBuildService queryBuildService;
    private final JoinRecommendService joinRecommendService;

    @Operation(summary = "生成 SQL")
    @PostMapping("/generate-sql")
    public Result<String> generateSql(@RequestBody QueryModel model) {
        String sql = queryBuildService.generateSql(model);
        return Result.success(sql);
    }

    @Operation(summary = "解析 SQL")
    @PostMapping("/parse-sql")
    public Result<QueryModel> parseSql(@RequestBody String sql) {
        QueryModel model = queryBuildService.parseSql(sql);
        return Result.success(model);
    }

    @Operation(summary = "预览查询结果")
    @PostMapping("/preview")
    public Result<List<Map<String, Object>>> previewQuery(
            @RequestParam Long dataSourceId,
            @RequestBody QueryModel model,
            @RequestParam(defaultValue = "100") int limit) {
        limit = Math.min(limit, 1000);
        List<Map<String, Object>> data = queryBuildService.previewQuery(dataSourceId, model, limit);
        return Result.success(data);
    }

    @Operation(summary = "执行 SQL 查询")
    @PostMapping("/execute")
    public Result<List<Map<String, Object>>> executeQuery(
            @RequestParam Long dataSourceId,
            @RequestBody String sql) {
        List<Map<String, Object>> data = queryBuildService.executeQuery(dataSourceId, sql);
        return Result.success(data);
    }

    @Operation(summary = "获取表元数据（全量，含列信息）")
    @GetMapping("/table-meta")
    public Result<List<TableMeta>> getTableMeta(@RequestParam Long dataSourceId) {
        List<TableMeta> tables = queryBuildService.getTableMeta(dataSourceId);
        return Result.success(tables);
    }

    @Operation(summary = "获取表名列表（轻量，不含列信息）")
    @GetMapping("/table-names")
    public Result<List<String>> getTableNames(@RequestParam Long dataSourceId) {
        List<String> names = queryBuildService.getTableNames(dataSourceId);
        return Result.success(names);
    }

    @Operation(summary = "获取单个表的列元数据（懒加载）")
    @GetMapping("/column-meta")
    public Result<TableMeta> getColumnMeta(
            @RequestParam Long dataSourceId,
            @RequestParam String tableName) {
        if (tableName == null || !tableName.matches("^[a-zA-Z0-9_.]+$")) {
            return Result.error("无效的表名");
        }
        TableMeta meta = queryBuildService.getSingleTableMeta(dataSourceId, tableName);
        return Result.success(meta);
    }

    @Operation(summary = "推荐两个表之间的关联关系")
    @GetMapping("/recommend-joins")
    public Result<List<JoinRecommendation>> recommendJoins(
            @RequestParam Long dataSourceId,
            @RequestParam String leftTable,
            @RequestParam String rightTable) {
        List<JoinRecommendation> recommendations = joinRecommendService.recommendJoins(
                dataSourceId, leftTable, rightTable);
        return Result.success(recommendations);
    }

    @Operation(summary = "推荐多个表之间的所有可能关联关系")
    @PostMapping("/recommend-joins-for-tables")
    public Result<List<JoinRecommendation>> recommendJoinsForTables(
            @RequestParam Long dataSourceId,
            @RequestBody List<String> tableNames) {
        List<JoinRecommendation> recommendations = joinRecommendService.recommendJoinsForTables(
                dataSourceId, tableNames);
        return Result.success(recommendations);
    }
}
