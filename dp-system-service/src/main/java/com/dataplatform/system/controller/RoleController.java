package com.dataplatform.system.controller;

import com.dataplatform.common.PageResult;
import com.dataplatform.common.Result;
import com.dataplatform.common.annotation.OperationLog;
import com.dataplatform.common.annotation.RequirePermission;
import com.dataplatform.common.util.LogUtil;
import com.dataplatform.data.dto.RoleCreateDTO;
import com.dataplatform.data.dto.RoleUpdateDTO;
import com.dataplatform.data.dto.RolePermissionDTO;
import com.dataplatform.system.entity.Role;
import com.dataplatform.system.entity.Permission;
import com.dataplatform.system.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "角色管理", description = "角色CRUD及权限分配接口")
@RestController
@RequestMapping("/role")
@RequirePermission("role:read")
public class RoleController {
    
    private static final String MODULE = LogUtil.ROLE;
    
    @Autowired
    private RoleService roleService;
    
    @Operation(summary = "获取角色列表", description = "分页获取角色列表")
    @ApiResponse(responseCode = "200", description = "成功")
    @RequirePermission("role:read")
    @GetMapping("/list")
    public Result<PageResult<Role>> getRoleList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String filters) {
        log.debug("[{}] 查询角色列表 | page={}, pageSize={}", MODULE, page, pageSize);
        List<Role> roles = roleService.getRoleList(page, pageSize, filters);
        long total = roleService.getRoleCount(filters);
        return Result.success(new PageResult<>(roles, total));
    }
    
    @RequirePermission("role:read")
    @GetMapping("/{id}")
    public Result<Role> getRoleById(@PathVariable Long id) {
        log.debug("[{}] 查询角色详情 | id={}", MODULE, id);
        Role role = roleService.getRoleById(id);
        return Result.success(role);
    }
    
    @Operation(summary = "创建角色", description = "创建新角色")
    @ApiResponse(responseCode = "200", description = "创建成功")
    @RequirePermission("role:manage")
    @OperationLog(module = "角色管理", type = OperationLog.OperationType.CREATE, description = "创建角色")
    @PostMapping("/create")
    public Result<Role> createRole(@Validated @RequestBody RoleCreateDTO dto) {
        if (dto == null) { throw new IllegalArgumentException("角色信息不能为空"); }
        log.info("[{}] 创建角色 | name={}, code={}", MODULE, dto.getRoleName(), dto.getRoleCode());
        Role role = new Role();
        BeanUtils.copyProperties(dto, role);
        Role createdRole = roleService.createRole(role);
        log.info("[{}] 创建角色成功 | id={}, name={}", MODULE, createdRole.getId(), createdRole.getRoleName());
        return Result.success(createdRole);
    }
    
    @RequirePermission("role:manage")
    @OperationLog(module = "角色管理", type = OperationLog.OperationType.UPDATE, description = "更新角色")
    @PutMapping("/update")
    public Result<Role> updateRole(@Validated @RequestBody RoleUpdateDTO dto) {
        if (dto == null) { throw new IllegalArgumentException("角色信息不能为空"); }
        log.info("[{}] 更新角色 | id={}", MODULE, dto.getId());
        Role role = new Role();
        BeanUtils.copyProperties(dto, role);
        Role updatedRole = roleService.updateRole(role);
        log.info("[{}] 更新角色成功 | id={}", MODULE, updatedRole.getId());
        return Result.success(updatedRole);
    }
    
    @Operation(summary = "删除角色", description = "根据ID删除角色")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @RequirePermission("role:manage")
    @OperationLog(module = "角色管理", type = OperationLog.OperationType.DELETE, description = "删除角色")
    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteRole(@Parameter(description = "角色ID") @PathVariable Long id) {
        log.info("[{}] 删除角色 | id={}", MODULE, id);
        roleService.deleteRole(id);
        log.info("[{}] 删除角色成功 | id={}", MODULE, id);
        return Result.success();
    }
    
    @RequirePermission("role:manage")
    @OperationLog(module = "角色管理", type = OperationLog.OperationType.UPDATE, description = "分配角色权限")
    @PostMapping("/assignPermissions")
    public Result<Void> assignPermissions(@Validated @RequestBody RolePermissionDTO dto) {
        log.info("[{}] 分配权限 | roleId={}, permissionCount={}", MODULE, dto.getRoleId(), 
            dto.getPermissionIds() != null ? dto.getPermissionIds().size() : 0);
        roleService.assignPermissions(dto.getRoleId(), dto.getPermissionIds());
        log.info("[{}] 分配权限成功 | roleId={}", MODULE, dto.getRoleId());
        return Result.success();
    }
    
    @RequirePermission("role:read")
    @GetMapping("/{roleId}/permissions")
    public Result<List<Permission>> getRolePermissions(@PathVariable Long roleId) {
        log.debug("[{}] 查询角色权限 | roleId={}", MODULE, roleId);
        List<Permission> permissions = roleService.getRolePermissions(roleId);
        return Result.success(permissions);
    }
    
    @RequirePermission("role:read")
    @GetMapping("/permissions")
    public Result<List<Permission>> getAllPermissions() {
        log.debug("[{}] 查询所有权限", MODULE);
        List<Permission> permissions = roleService.getAllPermissions();
        return Result.success(permissions);
    }
    
    @RequirePermission("user:read")
    @GetMapping("/user/{userId}")
    public Result<List<Role>> getUserRoles(@PathVariable Long userId) {
        log.debug("[{}] 查询用户角色 | userId={}", MODULE, userId);
        List<Role> roles = roleService.getUserRoles(userId);
        return Result.success(roles);
    }
    
    @RequirePermission("role:manage")
    @OperationLog(module = "角色管理", type = OperationLog.OperationType.UPDATE, description = "分配角色菜单")
    @PostMapping("/{roleId}/assignMenus")
    public Result<Void> assignMenus(@PathVariable Long roleId, @RequestBody List<Long> menuIds) {
        log.info("[{}] 分配菜单 | roleId={}, menuCount={}", MODULE, roleId, menuIds != null ? menuIds.size() : 0);
        roleService.assignMenus(roleId, menuIds);
        log.info("[{}] 分配菜单成功 | roleId={}", MODULE, roleId);
        return Result.success();
    }
    
    @RequirePermission("role:read")
    @GetMapping("/{roleId}/menus")
    public Result<List<Long>> getRoleMenus(@PathVariable Long roleId) {
        log.debug("[{}] 查询角色菜单 | roleId={}", MODULE, roleId);
        List<Long> menuIds = roleService.getRoleMenus(roleId);
        return Result.success(menuIds);
    }
}
