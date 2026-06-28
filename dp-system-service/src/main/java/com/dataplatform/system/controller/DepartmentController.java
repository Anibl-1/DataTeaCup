package com.dataplatform.system.controller;

import com.dataplatform.common.annotation.OperationLog;
import com.dataplatform.common.annotation.RequirePermission;
import com.dataplatform.common.Result;
import com.dataplatform.org.entity.Department;
import com.dataplatform.org.service.DepartmentService;
import com.dataplatform.system.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 部门管理控制器
 * 
 * @author dataplatform
 */
@RestController
@RequestMapping("/department")
@RequirePermission("department:read")
public class DepartmentController {

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private UserMapper userMapper;

    /**
     * 获取部门列表（平铺）
     */
    @GetMapping("/list")
    public Result<List<Department>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        List<Department> list = departmentService.getList(keyword, status);
        return Result.success(list);
    }

    /**
     * 获取部门树
     */
    @GetMapping("/tree")
    public Result<List<Department>> tree(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        List<Department> tree = departmentService.getTree(keyword, status);
        return Result.success(tree);
    }

    /**
     * 根据ID获取部门
     */
    @GetMapping("/{id}")
    public Result<Department> getById(@PathVariable Long id) {
        Department dept = departmentService.getById(id);
        return Result.success(dept);
    }

    /**
     * 创建部门
     */
    @RequirePermission("department:manage")
    @OperationLog(module = "部门管理", type = OperationLog.OperationType.CREATE, description = "创建部门")
    @PostMapping
    public Result<Department> create(@RequestBody Department department) {
        Department created = departmentService.create(department);
        return Result.success(created);
    }

    /**
     * 更新部门
     */
    @RequirePermission("department:manage")
    @OperationLog(module = "部门管理", type = OperationLog.OperationType.UPDATE, description = "更新部门")
    @PutMapping("/{id}")
    public Result<Department> update(@PathVariable Long id, @RequestBody Department department) {
        department.setId(id);
        Department updated = departmentService.update(department);
        return Result.success(updated);
    }

    /**
     * 删除部门
     */
    @RequirePermission("department:manage")
    @OperationLog(module = "部门管理", type = OperationLog.OperationType.DELETE, description = "删除部门")
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        departmentService.delete(id);
        return Result.success("删除成功");
    }

    /**
     * 更新部门状态
     */
    @RequirePermission("department:manage")
    @OperationLog(module = "部门管理", type = OperationLog.OperationType.UPDATE, description = "更新部门状态")
    @PutMapping("/{id}/status")
    public Result<String> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        departmentService.updateStatus(id, body.get("status"));
        return Result.success("状态更新成功");
    }

    /**
     * 批量更新部门排序
     */
    @RequirePermission("department:manage")
    @OperationLog(module = "部门管理", type = OperationLog.OperationType.UPDATE, description = "批量更新部门排序")
    @PostMapping("/batch-sort")
    public Result<String> batchUpdateSort(@RequestBody List<Map<String, Object>> items) {
        for (Map<String, Object> item : items) {
            Long id = Long.valueOf(item.get("id").toString());
            Integer sortOrder = Integer.valueOf(item.get("sortOrder").toString());
            departmentService.updateSortOrder(id, sortOrder);
        }
        return Result.success("排序更新成功");
    }

    /**
     * 查询部门下的用户列表（包含岗位信息）
     */
    @GetMapping("/{id}/users")
    public Result<List<com.dataplatform.system.entity.User>> getUsersByDeptId(@PathVariable Long id) {
        List<com.dataplatform.system.entity.User> users = userMapper.selectByDeptId(id);
        users.forEach(u -> u.setPassword(null));
        return Result.success(users);
    }
}
