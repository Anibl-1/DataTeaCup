package com.dataplatform.data.controller;

import com.dataplatform.common.annotation.OperationLog;
import com.dataplatform.common.annotation.RequirePermission;
import com.dataplatform.common.Result;
import com.dataplatform.data.entity.DataDictionary;
import com.dataplatform.data.service.DataDictionaryService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据字典控制器
 */
@RestController
@RequestMapping("/data-dictionary")
@RequirePermission("data:dictionary")
public class DataDictionaryController {

    @Autowired
    private DataDictionaryService dataDictionaryService;

    /**
     * 分页查询
     */
    @GetMapping("/list")
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String dictType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {

        IPage<DataDictionary> pageResult = dataDictionaryService.getPage(page, Math.min(pageSize, 200), dictType, keyword, status);

        Map<String, Object> result = new HashMap<>();
        result.put("list", pageResult.getRecords());
        result.put("total", pageResult.getTotal());

        return Result.success(result);
    }

    /**
     * 按字典类型查询（前端下拉用）
     */
    @GetMapping("/type/{dictType}")
    public Result<List<DataDictionary>> getByType(@PathVariable String dictType) {
        return Result.success(dataDictionaryService.getByType(dictType));
    }

    /**
     * 获取所有字典类型
     */
    @GetMapping("/types")
    public Result<List<String>> getTypes() {
        return Result.success(dataDictionaryService.getDistinctTypes());
    }

    /**
     * 根据ID获取详情
     */
    @GetMapping("/{id}")
    public Result<DataDictionary> getById(@PathVariable Long id) {
        DataDictionary dict = dataDictionaryService.getById(id);
        if (dict == null) {
            return Result.error("字典项不存在");
        }
        return Result.success(dict);
    }

    /**
     * 创建字典项
     */
    @OperationLog(module = "数据字典", type = OperationLog.OperationType.CREATE, description = "创建字典项")
    @PostMapping
    public Result<String> create(@RequestBody DataDictionary dict) {
        int rows = dataDictionaryService.create(dict);
        return rows > 0 ? Result.success("创建成功") : Result.error("创建失败");
    }

    /**
     * 更新字典项
     */
    @OperationLog(module = "数据字典", type = OperationLog.OperationType.UPDATE, description = "更新字典项")
    @PutMapping("/{id}")
    public Result<String> update(@PathVariable Long id, @RequestBody DataDictionary dict) {
        dict.setId(id);
        int rows = dataDictionaryService.update(dict);
        return rows > 0 ? Result.success("更新成功") : Result.error("更新失败");
    }

    /**
     * 删除字典项
     */
    @OperationLog(module = "数据字典", type = OperationLog.OperationType.DELETE, description = "删除字典项")
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        int rows = dataDictionaryService.delete(id);
        return rows > 0 ? Result.success("删除成功") : Result.error("删除失败");
    }

    /**
     * 批量删除
     */
    @OperationLog(module = "数据字典", type = OperationLog.OperationType.DELETE, description = "批量删除字典项")
    @DeleteMapping("/batch")
    public Result<String> batchDelete(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.error("请选择要删除的项");
        }
        if (ids.size() > 100) {
            return Result.error("批量删除最多支持100个");
        }
        int rows = dataDictionaryService.batchDelete(ids);
        return rows > 0 ? Result.success("删除成功") : Result.error("删除失败");
    }

    /**
     * 批量获取多个字典类型的映射（报表字段值翻译用）
     * @param types 逗号分隔的字典类型列表
     * @return Map<dictType, Map<dictValue, dictLabel>>
     */
    @GetMapping("/mappings")
    public Result<Map<String, Map<String, String>>> getMappings(@RequestParam String types) {
        Map<String, Map<String, String>> result = new HashMap<>();
        if (types != null && !types.isEmpty()) {
            for (String type : types.split(",")) {
                type = type.trim();
                if (type.isEmpty()) continue;
                List<DataDictionary> items = dataDictionaryService.getByType(type);
                Map<String, String> mapping = new HashMap<>();
                for (DataDictionary item : items) {
                    if (item.getStatus() != null && item.getStatus() == 1) {
                        mapping.put(item.getDictValue(), item.getDictLabel());
                    }
                }
                result.put(type, mapping);
            }
        }
        return Result.success(result);
    }
}
