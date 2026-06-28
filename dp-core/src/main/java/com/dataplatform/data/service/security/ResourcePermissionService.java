package com.dataplatform.data.service.security;

import com.dataplatform.data.dto.PermissionMatrix;

import java.util.List;

/**
 * 资源权限服务接口
 * 提供细粒度的资源级权限控制，支持权限检查、授权、撤销和权限矩阵查询
 *
 * @author dataplatform
 */
public interface ResourcePermissionService {

    /**
     * 检查用户对资源的操作权限
     * 验证用户是否具有该资源的对应操作权限，支持权限继承机制
     *
     * @param userId       用户ID
     * @param resourceType 资源类型 (datasource, report, dashboard, folder)
     * @param resourceId   资源ID
     * @param operation    操作类型 (view, edit, delete, export, share)
     * @return true 表示有权限，false 表示无权限
     */
    boolean hasPermission(Long userId, String resourceType, Long resourceId, String operation);

    /**
     * 获取用户可访问的资源ID列表
     *
     * @param userId       用户ID
     * @param resourceType 资源类型
     * @param operation    操作类型
     * @return 可访问的资源ID列表
     */
    List<Long> getAccessibleResources(Long userId, String resourceType, String operation);

    /**
     * 授予角色对资源的操作权限
     * 支持权限组功能，允许批量授权多个操作
     *
     * @param roleId       角色ID
     * @param resourceType 资源类型
     * @param resourceId   资源ID
     * @param operations   操作列表 (如 ["view","edit","delete"])
     */
    void grantPermission(Long roleId, String resourceType, Long resourceId, List<String> operations);

    /**
     * 撤销角色对资源的权限
     *
     * @param roleId       角色ID
     * @param resourceType 资源类型
     * @param resourceId   资源ID
     */
    void revokePermission(Long roleId, String resourceType, Long resourceId);

    /**
     * 获取权限矩阵
     * 展示指定资源类型下所有角色与资源的权限关系
     *
     * @param resourceType 资源类型
     * @return 权限矩阵
     */
    PermissionMatrix getPermissionMatrix(String resourceType);
}
