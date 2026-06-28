package com.dataplatform.data.controller;

import com.dataplatform.common.PageResult;
import com.dataplatform.common.Result;
import com.dataplatform.common.annotation.OperationLog;
import com.dataplatform.common.annotation.RequirePermission;
import com.dataplatform.common.util.LogUtil;
import com.dataplatform.data.dto.DataSourceCreateDTO;
import com.dataplatform.data.dto.DataSourceUpdateDTO;
import com.dataplatform.data.entity.DataSource;
import com.dataplatform.data.service.DataSourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 数据源控制器
 * 
 * @author dataplatform
 */
@Slf4j
@Tag(name = "数据源管理", description = "数据源配置及连接测试接口")
@RestController
@RequestMapping("/data-source")
@RequirePermission("data:source:read")
public class DataSourceController {
    
    private static final String MODULE = "DataSource";
    
    @Autowired
    private DataSourceService dataSourceService;
    
    /**
     * 获取数据源列表（分页）
     */
    @Operation(summary = "获取数据源列表", description = "分页获取数据源列表")
    @ApiResponse(responseCode = "200", description = "成功")
    @RequirePermission("data:source:read")
    @GetMapping("/list")
    public Result<PageResult<DataSource>> getDataSourceList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String filters) {
        log.debug("[{}] 查询数据源列表 | page={}, pageSize={}", MODULE, page, pageSize);
        List<DataSource> list = dataSourceService.getDataSourceList(page, pageSize, filters);
        long total = dataSourceService.getDataSourceCount(filters);
        log.debug("[{}] 查询数据源列表完成 | total={}", MODULE, total);
        return Result.success(new PageResult<>(list, total));
    }
    
    /**
     * 根据ID获取数据源
     */
    @GetMapping("/{id}")
    public Result<DataSource> getDataSourceById(@PathVariable Long id) {
        log.debug("[{}] 查询数据源详情 | id={}", MODULE, id);
        DataSource dataSource = dataSourceService.getDataSourceById(id);
        return Result.success(dataSource);
    }
    
    /**
     * 获取数据源的表列表（委托给Service层）
     */
    @GetMapping("/{id}/tables")
    public Result<List<Map<String, String>>> getTables(@PathVariable Long id) {
        log.debug("[{}] 获取表列表 | dataSourceId={}", MODULE, id);
        List<Map<String, String>> tables = dataSourceService.getTables(id);
        log.debug("[{}] 获取表列表完成 | dataSourceId={}, tableCount={}", MODULE, id, tables != null ? tables.size() : 0);
        return Result.success(tables);
    }
    
    /**
     * 获取表的字段列表（委托给Service层）
     */
    @GetMapping("/{id}/tables/{tableName}/columns")
    public Result<List<Map<String, Object>>> getTableColumns(
            @PathVariable Long id,
            @PathVariable String tableName) {
        if (tableName == null || !tableName.matches("^[a-zA-Z0-9_.]+$")) {
            log.warn("[{}] 获取字段列表失败 | 无效表名: {}", MODULE, tableName);
            return Result.error("无效的表名");
        }
        log.debug("[{}] 获取字段列表 | dataSourceId={}, table={}", MODULE, id, tableName);
        List<Map<String, Object>> columns = dataSourceService.getTableColumns(id, tableName);
        return Result.success(columns);
    }
    
    /**
     * 创建数据源
     */
    @RequirePermission("data:source")
    @OperationLog(module = "数据源管理", type = OperationLog.OperationType.CREATE, description = "创建数据源")
    @PostMapping("/create")
    public Result<Void> createDataSource(@Valid @RequestBody DataSourceCreateDTO dto) {
        log.info("[{}] 创建数据源 | name={}, type={}", MODULE, dto.getName(), dto.getDbType());
        DataSource dataSource = new DataSource();
        BeanUtils.copyProperties(dto, dataSource);
        dataSourceService.createDataSource(dataSource);
        log.info("[{}] 创建数据源成功 | name={}", MODULE, dto.getName());
        return Result.success(null);
    }
    
    /**
     * 更新数据源
     */
    @RequirePermission("data:source")
    @OperationLog(module = "数据源管理", type = OperationLog.OperationType.UPDATE, description = "更新数据源")
    @PostMapping("/update")
    public Result<Void> updateDataSource(@Valid @RequestBody DataSourceUpdateDTO dto) {
        log.info("[{}] 更新数据源 | id={}", MODULE, dto.getId());
        DataSource dataSource = new DataSource();
        BeanUtils.copyProperties(dto, dataSource);
        dataSourceService.updateDataSource(dataSource);
        log.info("[{}] 更新数据源成功 | id={}", MODULE, dto.getId());
        return Result.success(null);
    }
    
    /**
     * 删除数据源
     */
    @RequirePermission("data:source")
    @OperationLog(module = "数据源管理", type = OperationLog.OperationType.DELETE, description = "删除数据源")
    @DeleteMapping("/{id}")
    public Result<Void> deleteDataSource(@PathVariable Long id) {
        log.info("[{}] 删除数据源 | id={}", MODULE, id);
        dataSourceService.deleteDataSource(id);
        log.info("[{}] 删除数据源成功 | id={}", MODULE, id);
        return Result.success(null);
    }
    
    /**
     * 获取所有分组名称
     */
    @GetMapping("/groups")
    public Result<List<String>> getGroups() {
        log.debug("[{}] 获取数据源分组", MODULE);
        List<String> groups = dataSourceService.getGroups();
        return Result.success(groups);
    }

    /**
     * 测试数据源连接
     */
    @Operation(summary = "测试连接", description = "测试数据源连接是否正常")
    @ApiResponse(responseCode = "200", description = "连接成功")
    @RequirePermission("data:source")
    @PostMapping("/test")
    public Result<Void> testConnection(@RequestBody DataSourceCreateDTO dto) {
        log.info("[{}] 测试连接 | name={}, host={}", MODULE, dto.getName(), dto.getHost());
        DataSource dataSource = new DataSource();
        BeanUtils.copyProperties(dto, dataSource);
        dataSourceService.testConnection(dataSource);
        log.info("[{}] 测试连接成功 | name={}", MODULE, dto.getName());
        return Result.success(null);
    }

    /**
     * 批量测试数据源连接
     */
    @RequirePermission("data:source")
    @PostMapping("/batch-test")
    public Result<List<Map<String, Object>>> batchTestConnection(@RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body.get("ids");
        if (ids == null || ids.isEmpty()) {
            log.warn("[{}] 批量测试失败 | 未选择数据源", MODULE);
            return Result.error("请选择要测试的数据源");
        }
        log.info("[{}] 批量测试连接 | count={}", MODULE, ids.size());
        List<Map<String, Object>> results = dataSourceService.batchTestConnection(ids);
        log.info("[{}] 批量测试完成 | count={}", MODULE, ids.size());
        return Result.success(results);
    }

    /**
     * 获取数据源详情（含表列表）
     */
    @GetMapping("/{id}/detail")
    public Result<Map<String, Object>> getDataSourceDetail(@PathVariable Long id) {
        log.debug("[{}] 获取数据源详情 | id={}", MODULE, id);
        Map<String, Object> detail = dataSourceService.getDataSourceDetail(id);
        return Result.success(detail);
    }
}
