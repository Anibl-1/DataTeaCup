package com.dataplatform.system.controller;

import com.dataplatform.common.annotation.OperationLog;
import com.dataplatform.common.annotation.RequirePermission;
import com.dataplatform.common.Result;
import com.dataplatform.system.entity.SystemConfig;
import com.dataplatform.system.service.SystemConfigService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/system-config")
public class SystemConfigController {

    @Autowired
    private SystemConfigService systemConfigService;

    @GetMapping("/list")
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword) {
        IPage<SystemConfig> pageResult = systemConfigService.getPage(page, pageSize, keyword);
        Map<String, Object> result = new HashMap<>();
        result.put("list", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        return Result.success(result);
    }

    @GetMapping("/all")
    public Result<List<SystemConfig>> getAll(@RequestParam(required = false) String keyword) {
        return Result.success(systemConfigService.getList(keyword));
    }

    @GetMapping("/groups")
    public Result<List<String>> listGroups() {
        return Result.success(systemConfigService.listConfigGroups());
    }

    @GetMapping("/by-group")
    public Result<List<SystemConfig>> listByGroup(@RequestParam(required = false) String group) {
        return Result.success(systemConfigService.listByGroup(group));
    }

    @GetMapping("/key/{configKey}")
    public Result<String> getByKey(@PathVariable String configKey) {
        String value = systemConfigService.getValueByKey(configKey);
        if (value == null) { return Result.success(null); }
        return Result.success(value);
    }

    @GetMapping("/{id}")
    public Result<SystemConfig> getById(@PathVariable Long id) {
        SystemConfig config = systemConfigService.getById(id);
        if (config == null) { return Result.error("配置不存在"); }
        return Result.success(config);
    }

    @RequirePermission("system:config")
    @OperationLog(module = "系统配置", type = OperationLog.OperationType.CREATE, description = "创建系统配置")
    @PostMapping
    public Result<String> create(@RequestBody SystemConfig config) {
        int rows = systemConfigService.create(config);
        return rows > 0 ? Result.success("创建成功") : Result.error("创建失败");
    }

    @RequirePermission("system:config")
    @OperationLog(module = "系统配置", type = OperationLog.OperationType.UPDATE, description = "更新系统配置")
    @PutMapping("/{id}")
    public Result<String> update(@PathVariable Long id, @RequestBody SystemConfig config) {
        config.setId(id);
        int rows = systemConfigService.update(config);
        return rows > 0 ? Result.success("更新成功") : Result.error("更新失败");
    }

    @RequirePermission("system:config")
    @OperationLog(module = "系统配置", type = OperationLog.OperationType.DELETE, description = "删除系统配置")
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        int rows = systemConfigService.delete(id);
        return rows > 0 ? Result.success("删除成功") : Result.error("删除失败");
    }
}
