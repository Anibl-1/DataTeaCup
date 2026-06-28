package com.dataplatform.analytics.controller;

import com.dataplatform.common.annotation.OperationLog;
import com.dataplatform.common.Result;
import com.dataplatform.common.annotation.RequirePermission;
import com.dataplatform.data.entity.ChartFolder;
import com.dataplatform.data.service.ChartFolderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 图表文件夹控制器
 */
@RestController
@RequestMapping("/chart-folder")
@RequirePermission("chart:read")
public class ChartFolderController {

    @Autowired
    private ChartFolderService chartFolderService;

    /**
     * 获取文件夹树
     */
    @GetMapping("/tree")
    public Result<List<ChartFolder>> getTree() {
        return Result.success(chartFolderService.getTree());
    }

    /**
     * 获取全部列表
     */
    @GetMapping("/list")
    public Result<List<ChartFolder>> getAll() {
        return Result.success(chartFolderService.getAll());
    }

    /**
     * 创建文件夹
     */
    @OperationLog(module = "图表文件夹", type = OperationLog.OperationType.CREATE, description = "创建图表文件夹")
    @PostMapping
    public Result<String> create(@RequestBody ChartFolder folder) {
        int rows = chartFolderService.create(folder);
        return rows > 0 ? Result.success("创建成功") : Result.error("创建失败");
    }

    /**
     * 更新文件夹
     */
    @OperationLog(module = "图表文件夹", type = OperationLog.OperationType.UPDATE, description = "更新图表文件夹")
    @PutMapping("/{id}")
    public Result<String> update(@PathVariable Long id, @RequestBody ChartFolder folder) {
        folder.setId(id);
        int rows = chartFolderService.update(folder);
        return rows > 0 ? Result.success("更新成功") : Result.error("更新失败");
    }

    /**
     * 删除文件夹
     */
    @OperationLog(module = "图表文件夹", type = OperationLog.OperationType.DELETE, description = "删除图表文件夹")
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        int rows = chartFolderService.delete(id);
        return rows > 0 ? Result.success("删除成功") : Result.error("删除失败");
    }
}
