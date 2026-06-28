package com.dataplatform.data.controller;

import com.dataplatform.common.Result;
import com.dataplatform.common.annotation.RequirePermission;
import com.dataplatform.data.entity.DataView;
import com.dataplatform.data.service.DataViewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dataview")
@RequirePermission("data:view")
public class DataViewController {

    @Autowired
    private DataViewService dataViewService;

    @GetMapping("/list")
    public Result<List<DataView>> list(@RequestParam(required = false) String keyword) {
        return Result.success(dataViewService.getList(keyword));
    }

    @GetMapping("/{id}")
    public Result<DataView> getById(@PathVariable Long id) {
        DataView dataView = dataViewService.getById(id);
        if (dataView == null) {
            return Result.error("数据视图不存在");
        }
        return Result.success(dataView);
    }

    @GetMapping("/code/{code}")
    public Result<DataView> getByCode(@PathVariable String code) {
        DataView dataView = dataViewService.getByCode(code);
        if (dataView == null) {
            return Result.error("数据视图不存在");
        }
        return Result.success(dataView);
    }

    @PostMapping
    @SuppressWarnings("unchecked")
    public Result<String> create(@RequestBody Map<String, Object> body) {
        try {
            DataView dataView = mapToDataView(body);
            List<Map<String, Object>> columns = (List<Map<String, Object>>) body.get("columns");
            dataViewService.create(dataView, columns);
            return Result.success("创建成功");
        } catch (Exception e) {
            return Result.error("创建失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @SuppressWarnings("unchecked")
    public Result<String> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            DataView dataView = mapToDataView(body);
            dataView.setId(id);
            List<Map<String, Object>> columns = (List<Map<String, Object>>) body.get("columns");
            dataViewService.update(dataView, columns);
            return Result.success("更新成功");
        } catch (Exception e) {
            return Result.error("更新失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        int rows = dataViewService.delete(id);
        return rows > 0 ? Result.success("删除成功") : Result.error("删除失败");
    }

    private DataView mapToDataView(Map<String, Object> body) {
        DataView dv = new DataView();
        dv.setName((String) body.get("name"));
        dv.setCode((String) body.get("code"));
        dv.setDataSourceId(toLong(body.get("dataSourceId")));
        dv.setTableName((String) body.get("tableName"));
        dv.setDescription((String) body.get("description"));
        dv.setStatus(toInt(body.get("status")));
        dv.setAllowQuery(toInt(body.get("allowQuery")));
        dv.setAllowInsert(toInt(body.get("allowInsert")));
        dv.setAllowUpdate(toInt(body.get("allowUpdate")));
        dv.setAllowDelete(toInt(body.get("allowDelete")));
        dv.setAllowImport(toInt(body.get("allowImport")));
        dv.setAllowExport(toInt(body.get("allowExport")));
        dv.setDefaultOrderBy((String) body.get("defaultOrderBy"));
        dv.setDefaultOrderDir((String) body.get("defaultOrderDir"));
        dv.setPageSize(toInt(body.get("pageSize")));
        dv.setGenerateMenu(toInt(body.get("generateMenu")));
        dv.setMenuName((String) body.get("menuName"));
        dv.setMenuParentId(toLong(body.get("menuParentId")));
        dv.setMenuIcon((String) body.get("menuIcon"));
        dv.setMenuSort(toInt(body.get("menuSort")));
        return dv;
    }

    private Integer toInt(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Integer) return (Integer) obj;
        if (obj instanceof Number) return ((Number) obj).intValue();
        return Integer.parseInt(obj.toString());
    }

    private Long toLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Long) return (Long) obj;
        if (obj instanceof Number) return ((Number) obj).longValue();
        return Long.parseLong(obj.toString());
    }
}
