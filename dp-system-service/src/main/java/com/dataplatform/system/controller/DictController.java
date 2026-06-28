package com.dataplatform.system.controller;

import com.dataplatform.common.annotation.OperationLog;
import com.dataplatform.common.annotation.RequirePermission;
import com.dataplatform.common.Result;
import com.dataplatform.system.entity.DictData;
import com.dataplatform.system.entity.DictType;
import com.dataplatform.system.service.DictDataService;
import com.dataplatform.system.service.DictTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/dict")
@RequirePermission("dict:read")
public class DictController {

    @Autowired
    private DictTypeService dictTypeService;

    @Autowired
    private DictDataService dictDataService;

    // ==================== 字典类型接口 ====================

    @GetMapping("/types")
    public Result<List<DictType>> listTypes() {
        return Result.success(dictTypeService.list());
    }

    @RequirePermission("dict:manage")
    @OperationLog(module = "数据字典", type = OperationLog.OperationType.CREATE, description = "创建字典类型")
    @PostMapping("/types")
    public Result<DictType> createType(@RequestBody DictType dictType) {
        return Result.success(dictTypeService.create(dictType));
    }

    @RequirePermission("dict:manage")
    @OperationLog(module = "数据字典", type = OperationLog.OperationType.UPDATE, description = "更新字典类型")
    @PutMapping("/types/{id}")
    public Result<DictType> updateType(@PathVariable Long id, @RequestBody DictType dictType) {
        dictType.setId(id);
        return Result.success(dictTypeService.update(dictType));
    }

    @RequirePermission("dict:manage")
    @OperationLog(module = "数据字典", type = OperationLog.OperationType.DELETE, description = "删除字典类型")
    @DeleteMapping("/types/{id}")
    public Result<String> deleteType(@PathVariable Long id) {
        dictTypeService.delete(id);
        return Result.success("删除成功");
    }

    // ==================== 字典数据接口 ====================

    @GetMapping("/data")
    public Result<List<DictData>> listData(@RequestParam String dictCode) {
        return Result.success(dictDataService.listEnabledByDictCode(dictCode));
    }

    @GetMapping("/data/batch")
    public Result<Map<String, List<DictData>>> listDataBatch(@RequestParam String codes) {
        List<String> codeList = Arrays.stream(codes.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        return Result.success(dictDataService.listBatch(codeList));
    }

    @RequirePermission("dict:manage")
    @OperationLog(module = "数据字典", type = OperationLog.OperationType.CREATE, description = "创建字典数据项")
    @PostMapping("/data")
    public Result<DictData> createData(@RequestBody DictData dictData) {
        return Result.success(dictDataService.create(dictData));
    }

    @RequirePermission("dict:manage")
    @OperationLog(module = "数据字典", type = OperationLog.OperationType.UPDATE, description = "更新字典数据项")
    @PutMapping("/data/{id}")
    public Result<DictData> updateData(@PathVariable Long id, @RequestBody DictData dictData) {
        dictData.setId(id);
        return Result.success(dictDataService.update(dictData));
    }

    @RequirePermission("dict:manage")
    @OperationLog(module = "数据字典", type = OperationLog.OperationType.DELETE, description = "删除字典数据项")
    @DeleteMapping("/data/{id}")
    public Result<String> deleteData(@PathVariable Long id) {
        dictDataService.delete(id);
        return Result.success("删除成功");
    }
}
