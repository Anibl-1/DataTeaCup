package com.dataplatform.data.controller;

import com.dataplatform.common.annotation.OperationLog;
import com.dataplatform.common.annotation.RequirePermission;
import com.dataplatform.common.Result;
import com.dataplatform.data.entity.DataLineage;
import com.dataplatform.data.service.DataLineageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 数据血缘关系控制器
 */
@Slf4j
@RestController
@RequestMapping("/data-lineage")
@RequirePermission("data:lineage")
public class DataLineageController {
    
    @Autowired
    private DataLineageService dataLineageService;
    
    /**
     * 创建血缘关系
     */
    @OperationLog(module = "数据血缘", type = OperationLog.OperationType.CREATE, description = "创建血缘关系")
    @PostMapping
    public Result<DataLineage> create(@RequestBody DataLineage lineage) {
        log.info("创建血缘关系: {} -> {}", lineage.getSourceTable(), lineage.getTargetTable());
        DataLineage created = dataLineageService.create(lineage);
        return Result.success(created);
    }
    
    /**
     * 更新血缘关系
     */
    @OperationLog(module = "数据血缘", type = OperationLog.OperationType.UPDATE, description = "更新血缘关系")
    @PutMapping("/{id}")
    public Result<DataLineage> update(@PathVariable Long id, @RequestBody DataLineage lineage) {
        log.info("更新血缘关系: id={}", id);
        lineage.setId(id);
        DataLineage updated = dataLineageService.update(lineage);
        return Result.success(updated);
    }
    
    /**
     * 删除血缘关系
     */
    @OperationLog(module = "数据血缘", type = OperationLog.OperationType.DELETE, description = "删除血缘关系")
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        log.info("删除血缘关系: id={}", id);
        dataLineageService.delete(id);
        return Result.success("删除成功");
    }
    
    /**
     * 获取血缘关系详情
     */
    @GetMapping("/{id}")
    public Result<DataLineage> getById(@PathVariable Long id) {
        DataLineage lineage = dataLineageService.getById(id);
        return Result.success(lineage);
    }
    
    /**
     * 获取所有血缘关系
     */
    @GetMapping("/list")
    public Result<List<DataLineage>> getAll() {
        List<DataLineage> list = dataLineageService.getAll();
        return Result.success(list);
    }
    
    /**
     * 根据表名查询血缘关系
     */
    @GetMapping("/table/{tableName}")
    public Result<List<DataLineage>> getByTableName(@PathVariable String tableName) {
        log.info("查询表血缘: {}", tableName);
        List<DataLineage> list = dataLineageService.getByTableName(tableName);
        return Result.success(list);
    }
    
    /**
     * 获取上游血缘
     */
    @GetMapping("/upstream/{tableName}")
    public Result<List<DataLineage>> getUpstream(@PathVariable String tableName) {
        List<DataLineage> list = dataLineageService.getUpstream(tableName);
        return Result.success(list);
    }
    
    /**
     * 获取下游血缘
     */
    @GetMapping("/downstream/{tableName}")
    public Result<List<DataLineage>> getDownstream(@PathVariable String tableName) {
        List<DataLineage> list = dataLineageService.getDownstream(tableName);
        return Result.success(list);
    }
    
    /**
     * 获取血缘图谱（用于可视化）
     */
    @GetMapping("/graph/{tableName}")
    public Result<Map<String, Object>> getLineageGraph(
            @PathVariable String tableName,
            @RequestParam(defaultValue = "3") int depth) {
        depth = Math.min(depth, 10);
        log.info("获取血缘图谱: table={}, depth={}", tableName, depth);
        Map<String, Object> graph = dataLineageService.getLineageGraph(tableName, depth);
        return Result.success(graph);
    }
    
    /**
     * 从SQL解析血缘关系
     */
    @PostMapping("/parse-sql")
    public Result<List<DataLineage>> parseFromSql(@RequestBody Map<String, String> request) {
        String sql = request.get("sql");
        String targetTable = request.get("targetTable");
        String targetDatabase = request.get("targetDatabase");
        
        log.info("从SQL解析血缘: targetTable={}", targetTable);
        List<DataLineage> lineages = dataLineageService.parseFromSql(sql, targetTable, targetDatabase);
        return Result.success(lineages);
    }
    
    /**
     * 批量保存解析的血缘关系
     */
    @OperationLog(module = "数据血缘", type = OperationLog.OperationType.CREATE, description = "批量保存血缘关系")
    @PostMapping("/batch-save")
    public Result<String> batchSave(@RequestBody List<DataLineage> lineages) {
        if (lineages.size() > 100) {
            return Result.error("批量保存最多支持100条");
        }
        log.info("批量保存血缘关系: count={}", lineages.size());
        for (DataLineage lineage : lineages) {
            dataLineageService.create(lineage);
        }
        return Result.success("保存成功，共 " + lineages.size() + " 条");
    }
    
    /**
     * 获取血缘统计信息
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = dataLineageService.getStatistics();
        return Result.success(stats);
    }
    
    /**
     * 自动发现血缘关系（从采集任务、DataX任务、报表中提取）
     */
    @OperationLog(module = "数据血缘", type = OperationLog.OperationType.CREATE, description = "自动发现血缘关系")
    @PostMapping("/discover")
    public Result<Map<String, Object>> autoDiscover() {
        log.info("开始自动发现血缘关系");
        Map<String, Object> result = dataLineageService.autoDiscover();
        return Result.success(result);
    }
    
    /**
     * 影响分析：查找某表变更影响的所有下游
     */
    @GetMapping("/impact/{tableName}")
    public Result<Map<String, Object>> getImpactAnalysis(
            @PathVariable String tableName,
            @RequestParam(defaultValue = "5") int depth) {
        depth = Math.min(depth, 10);
        log.info("影响分析: table={}, depth={}", tableName, depth);
        Map<String, Object> analysis = dataLineageService.getImpactAnalysis(tableName, depth);
        return Result.success(analysis);
    }
    
    // ==================== 智能分析接口 ====================
    
    /**
     * 全链路追溯：获取某表的完整上下游链路
     */
    @GetMapping("/full-chain/{tableName}")
    public Result<Map<String, Object>> getFullChainAnalysis(
            @PathVariable String tableName,
            @RequestParam(defaultValue = "5") int depth) {
        depth = Math.min(depth, 10);
        log.info("全链路追溯: table={}, depth={}", tableName, depth);
        Map<String, Object> result = dataLineageService.getFullChainAnalysis(tableName, depth);
        return Result.success(result);
    }
    
    /**
     * 热点表分析：找出依赖最多/被依赖最多的表
     */
    @GetMapping("/hotspot")
    public Result<Map<String, Object>> getHotspotAnalysis(
            @RequestParam(defaultValue = "10") int topN) {
        topN = Math.min(topN, 50);
        log.info("热点表分析: topN={}", topN);
        Map<String, Object> result = dataLineageService.getHotspotAnalysis(topN);
        return Result.success(result);
    }
    
    /**
     * 孤岛检测：找出源头表、终端表等
     */
    @GetMapping("/orphan")
    public Result<Map<String, Object>> getOrphanAnalysis() {
        log.info("孤岛检测分析");
        Map<String, Object> result = dataLineageService.getOrphanAnalysis();
        return Result.success(result);
    }
    
    /**
     * 血缘健康度报告
     */
    @GetMapping("/health")
    public Result<Map<String, Object>> getHealthReport() {
        log.info("获取血缘健康报告");
        Map<String, Object> report = dataLineageService.getHealthReport();
        return Result.success(report);
    }
    
    /**
     * 智能SQL解析并创建血缘
     */
    @OperationLog(module = "数据血缘", type = OperationLog.OperationType.CREATE, description = "智能SQL解析血缘")
    @PostMapping("/smart-parse")
    public Result<Map<String, Object>> smartParseSql(@RequestBody Map<String, String> request) {
        String sql = request.get("sql");
        String targetTable = request.get("targetTable");
        String targetDatabase = request.get("targetDatabase");
        
        log.info("智能SQL解析并创建血缘: targetTable={}", targetTable);
        Map<String, Object> result = dataLineageService.parseAndCreateLineage(sql, targetTable, targetDatabase);
        return Result.success(result);
    }
    
    /**
     * 获取表的依赖摘要
     */
    @GetMapping("/summary/{tableName}")
    public Result<Map<String, Object>> getTableDependencySummary(@PathVariable String tableName) {
        log.info("获取表依赖摘要: {}", tableName);
        Map<String, Object> summary = dataLineageService.getTableDependencySummary(tableName);
        return Result.success(summary);
    }
}
