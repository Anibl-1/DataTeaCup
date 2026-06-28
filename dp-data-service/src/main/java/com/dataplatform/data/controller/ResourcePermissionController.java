package com.dataplatform.data.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.dataplatform.common.annotation.OperationLog;
import com.dataplatform.common.Result;
import com.dataplatform.data.dto.GrantPermissionDTO;
import com.dataplatform.data.dto.PermissionMatrix;
import com.dataplatform.data.dto.RevokePermissionDTO;
import com.dataplatform.data.entity.ResourcePermission;
import com.dataplatform.data.mapper.ResourcePermissionMapper;
import com.dataplatform.data.service.security.ResourcePermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 资源权限控制器
 * 提供权限矩阵查询、授权、撤销等API接口
 *
 * @author dataplatform
 */
@Slf4j
@Tag(name = "资源权限管理", description = "资源级权限控制接口，支持权限矩阵查询、授权、撤销")
@RestController
@RequestMapping("/permissions")
@RequiredArgsConstructor
@SaCheckLogin
public class ResourcePermissionController {

    private final ResourcePermissionService resourcePermissionService;
    private final ResourcePermissionMapper resourcePermissionMapper;

    /**
     * 获取权限矩阵
     * 展示指定资源类型下所有角色与资源的权限关系
     *
     * @param resourceType 资源类型 (datasource, report, dashboard, folder)
     * @return 权限矩阵
     */
    @Operation(summary = "获取权限矩阵", description = "按资源类型获取角色与资源的权限关系矩阵")
    @GetMapping("/matrix")
    @SaCheckPermission("permission:view")
    public Result<PermissionMatrix> getPermissionMatrix(
            @Parameter(description = "资源类型: datasource, report, dashboard, folder")
            @RequestParam String resourceType) {
        log.info("获取权限矩阵: resourceType={}", resourceType);
        PermissionMatrix matrix = resourcePermissionService.getPermissionMatrix(resourceType);
        return Result.success(matrix);
    }

    /**
     * 获取指定资源的权限列表
     *
     * @param resourceType 资源类型
     * @param resourceId   资源ID
     * @return 权限列表
     */
    @Operation(summary = "获取资源权限列表", description = "获取指定资源的所有权限配置")
    @GetMapping("/resource/{resourceType}/{resourceId}")
    @SaCheckPermission("permission:view")
    public Result<List<ResourcePermission>> getResourcePermissions(
            @Parameter(description = "资源类型") @PathVariable String resourceType,
            @Parameter(description = "资源ID") @PathVariable Long resourceId) {
        log.info("获取资源权限: resourceType={}, resourceId={}", resourceType, resourceId);
        List<ResourcePermission> permissions = resourcePermissionMapper
                .selectByResourceTypeAndId(resourceType, resourceId);
        return Result.success(permissions);
    }

    /**
     * 授予权限
     * 为角色授予对指定资源的操作权限
     *
     * @param dto 授权请求
     * @return 操作结果
     */
    @Operation(summary = "授予权限", description = "为角色授予对指定资源的操作权限")
    @PostMapping("/grant")
    @SaCheckPermission("permission:manage")
    @OperationLog(module = "权限管理", type = OperationLog.OperationType.CREATE, description = "授予资源权限")
    public Result<Void> grantPermission(@Validated @RequestBody GrantPermissionDTO dto) {
        log.info("授予权限: roleId={}, resourceType={}, resourceId={}, operations={}",
                dto.getRoleId(), dto.getResourceType(), dto.getResourceId(), dto.getOperations());
        resourcePermissionService.grantPermission(
                dto.getRoleId(),
                dto.getResourceType(),
                dto.getResourceId(),
                dto.getOperations()
        );
        return Result.success();
    }

    /**
     * 撤销权限
     * 撤销角色对指定资源的所有权限
     *
     * @param dto 撤销请求
     * @return 操作结果
     */
    @Operation(summary = "撤销权限", description = "撤销角色对指定资源的所有权限")
    @PostMapping("/revoke")
    @SaCheckPermission("permission:manage")
    @OperationLog(module = "权限管理", type = OperationLog.OperationType.DELETE, description = "撤销资源权限")
    public Result<Void> revokePermission(@Validated @RequestBody RevokePermissionDTO dto) {
        log.info("撤销权限: roleId={}, resourceType={}, resourceId={}",
                dto.getRoleId(), dto.getResourceType(), dto.getResourceId());
        resourcePermissionService.revokePermission(
                dto.getRoleId(),
                dto.getResourceType(),
                dto.getResourceId()
        );
        return Result.success();
    }

    /**
     * 获取用户的所有权限
     * 查询指定用户可访问的所有资源权限
     *
     * @param userId 用户ID
     * @return 用户权限列表
     */
    @Operation(summary = "获取用户权限", description = "获取指定用户的所有资源权限")
    @GetMapping("/user/{userId}")
    @SaCheckPermission("permission:view")
    public Result<List<ResourcePermission>> getUserPermissions(
            @Parameter(description = "用户ID") @PathVariable Long userId) {
        log.info("获取用户权限: userId={}", userId);
        List<ResourcePermission> permissions = resourcePermissionMapper.selectByUserId(userId);
        return Result.success(permissions);
    }

    /**
     * 检查当前用户对资源的权限
     *
     * @param resourceType 资源类型
     * @param resourceId   资源ID
     * @param operation    操作类型
     * @return 是否有权限
     */
    @Operation(summary = "检查权限", description = "检查当前登录用户对指定资源的操作权限")
    @GetMapping("/check")
    public Result<Boolean> checkPermission(
            @Parameter(description = "资源类型") @RequestParam String resourceType,
            @Parameter(description = "资源ID") @RequestParam Long resourceId,
            @Parameter(description = "操作类型: view, edit, delete, export, share") @RequestParam String operation) {
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        boolean hasPermission = resourcePermissionService.hasPermission(userId, resourceType, resourceId, operation);
        return Result.success(hasPermission);
    }

    /**
     * 获取当前用户可访问的资源列表
     *
     * @param resourceType 资源类型
     * @param operation    操作类型
     * @return 可访问的资源ID列表
     */
    @Operation(summary = "获取可访问资源", description = "获取当前用户可执行指定操作的资源ID列表")
    @GetMapping("/accessible")
    public Result<List<Long>> getAccessibleResources(
            @Parameter(description = "资源类型") @RequestParam String resourceType,
            @Parameter(description = "操作类型") @RequestParam String operation) {
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        List<Long> resourceIds = resourcePermissionService.getAccessibleResources(userId, resourceType, operation);
        return Result.success(resourceIds);
    }
}
