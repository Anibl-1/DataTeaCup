package com.dataplatform.data.service.security;

import com.alibaba.fastjson2.JSON;
import com.dataplatform.common.exception.BusinessException;
import com.dataplatform.common.exception.ErrorCode;
import com.dataplatform.data.dto.PermissionMatrix;
import com.dataplatform.data.entity.ResourcePermission;
import com.dataplatform.system.entity.Role;
import com.dataplatform.data.mapper.ResourcePermissionMapper;
import com.dataplatform.system.mapper.RoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 资源权限服务实现
 * 实现细粒度的资源级权限控制，支持权限继承和权限组功能
 *
 * @author dataplatform
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResourcePermissionServiceImpl implements ResourcePermissionService {

    private final ResourcePermissionMapper resourcePermissionMapper;
    private final RoleMapper roleMapper;

    /** 支持的资源类型 */
    private static final Set<String> VALID_RESOURCE_TYPES = Set.of(
            "datasource", "report", "dashboard", "folder"
    );

    /** 支持的操作类型 */
    private static final Set<String> VALID_OPERATIONS = Set.of(
            "view", "edit", "delete", "export", "share"
    );

    @Override
    public boolean hasPermission(Long userId, String resourceType, Long resourceId, String operation) {
        if (userId == null || resourceType == null || resourceId == null || operation == null) {
            return false;
        }

        // 获取用户所有角色
        List<Role> roles = roleMapper.selectByUserId(userId);
        if (roles == null || roles.isEmpty()) {
            return false;
        }

        List<Long> roleIds = roles.stream()
                .map(Role::getId)
                .collect(Collectors.toList());

        // 查询用户角色对该资源的所有权限记录
        List<ResourcePermission> permissions = resourcePermissionMapper
                .selectByRoleIdsAndResource(roleIds, resourceType, resourceId);

        // 检查直接权限
        if (hasOperationInPermissions(permissions, operation)) {
            return true;
        }

        // 检查继承权限：如果资源类型不是folder，检查其所属folder的权限
        // folder类型的资源作为父资源，子资源默认继承父资源权限
        if (!"folder".equals(resourceType)) {
            List<ResourcePermission> folderPermissions = resourcePermissionMapper
                    .selectByRoleIdsAndResource(roleIds, "folder", resourceId);
            if (hasOperationInPermissions(folderPermissions, operation)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public List<Long> getAccessibleResources(Long userId, String resourceType, String operation) {
        if (userId == null || resourceType == null || operation == null) {
            return Collections.emptyList();
        }

        // 获取用户所有角色
        List<Role> roles = roleMapper.selectByUserId(userId);
        if (roles == null || roles.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> roleIds = roles.stream()
                .map(Role::getId)
                .collect(Collectors.toList());

        List<Long> resourceIds = resourcePermissionMapper
                .selectAccessibleResourceIds(roleIds, resourceType, operation);

        return resourceIds != null ? resourceIds : Collections.emptyList();
    }

    @Override
    @Transactional
    public void grantPermission(Long roleId, String resourceType, Long resourceId, List<String> operations) {
        // 参数校验
        validateGrantParams(roleId, resourceType, resourceId, operations);

        // 过滤无效操作
        List<String> validOps = operations.stream()
                .filter(VALID_OPERATIONS::contains)
                .distinct()
                .collect(Collectors.toList());

        if (validOps.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "操作类型无效");
        }

        // 查询是否已存在权限记录
        ResourcePermission existing = resourcePermissionMapper
                .selectByRoleAndResource(roleId, resourceType, resourceId);

        if (existing != null) {
            // 合并操作权限
            List<String> existingOps = parseOperations(existing.getOperations());
            Set<String> mergedOps = new LinkedHashSet<>(existingOps);
            mergedOps.addAll(validOps);

            existing.setOperations(JSON.toJSONString(mergedOps));
            existing.setUpdateTime(LocalDateTime.now());
            existing.setInherited(false);
            resourcePermissionMapper.updateById(existing);

            log.info("更新资源权限: roleId={}, resourceType={}, resourceId={}, operations={}",
                    roleId, resourceType, resourceId, mergedOps);
        } else {
            // 创建新权限记录
            ResourcePermission permission = new ResourcePermission();
            permission.setRoleId(roleId);
            permission.setResourceType(resourceType);
            permission.setResourceId(resourceId);
            permission.setOperations(JSON.toJSONString(validOps));
            permission.setInherited(false);
            permission.setCreateTime(LocalDateTime.now());
            permission.setUpdateTime(LocalDateTime.now());
            resourcePermissionMapper.insert(permission);

            log.info("授予资源权限: roleId={}, resourceType={}, resourceId={}, operations={}",
                    roleId, resourceType, resourceId, validOps);
        }
    }

    @Override
    @Transactional
    public void revokePermission(Long roleId, String resourceType, Long resourceId) {
        if (roleId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "角色ID不能为空");
        }
        if (resourceType == null || resourceType.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "资源类型不能为空");
        }
        if (resourceId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "资源ID不能为空");
        }

        int deleted = resourcePermissionMapper.deleteByRoleAndResource(roleId, resourceType, resourceId);
        log.info("撤销资源权限: roleId={}, resourceType={}, resourceId={}, 删除记录数={}",
                roleId, resourceType, resourceId, deleted);
    }

    @Override
    public PermissionMatrix getPermissionMatrix(String resourceType) {
        if (resourceType == null || resourceType.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "资源类型不能为空");
        }

        // 查询该资源类型下的所有权限记录
        List<ResourcePermission> allPermissions = resourcePermissionMapper
                .selectByResourceType(resourceType);

        // 按角色ID分组
        Map<Long, List<ResourcePermission>> permissionsByRole = allPermissions.stream()
                .collect(Collectors.groupingBy(ResourcePermission::getRoleId));

        // 构建权限矩阵行
        List<PermissionMatrix.RolePermissionRow> rows = new ArrayList<>();
        for (Map.Entry<Long, List<ResourcePermission>> entry : permissionsByRole.entrySet()) {
            Long roleId = entry.getKey();
            List<ResourcePermission> rolePermissions = entry.getValue();

            PermissionMatrix.RolePermissionRow row = new PermissionMatrix.RolePermissionRow();
            row.setRoleId(roleId);

            // 查询角色名称
            Role role = roleMapper.selectById(roleId);
            row.setRoleName(role != null ? role.getRoleName() : "未知角色");

            // 构建资源操作映射
            Map<Long, List<String>> resourceOperations = new HashMap<>();
            for (ResourcePermission perm : rolePermissions) {
                List<String> ops = parseOperations(perm.getOperations());
                resourceOperations.put(perm.getResourceId(), ops);
            }
            row.setResourceOperations(resourceOperations);

            rows.add(row);
        }

        PermissionMatrix matrix = new PermissionMatrix();
        matrix.setResourceType(resourceType);
        matrix.setRows(rows);

        return matrix;
    }

    /**
     * 检查权限列表中是否包含指定操作
     */
    private boolean hasOperationInPermissions(List<ResourcePermission> permissions, String operation) {
        if (permissions == null || permissions.isEmpty()) {
            return false;
        }
        for (ResourcePermission perm : permissions) {
            List<String> ops = parseOperations(perm.getOperations());
            if (ops.contains(operation)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 解析操作权限JSON字符串为列表
     */
    List<String> parseOperations(String operationsJson) {
        if (operationsJson == null || operationsJson.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            List<String> ops = JSON.parseArray(operationsJson, String.class);
            return ops != null ? ops : Collections.emptyList();
        } catch (Exception e) {
            log.warn("解析操作权限JSON失败: {}", operationsJson, e);
            return Collections.emptyList();
        }
    }

    /**
     * 校验授权参数
     */
    private void validateGrantParams(Long roleId, String resourceType, Long resourceId, List<String> operations) {
        if (roleId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "角色ID不能为空");
        }
        if (resourceType == null || resourceType.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "资源类型不能为空");
        }
        if (resourceId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "资源ID不能为空");
        }
        if (operations == null || operations.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "操作列表不能为空");
        }
    }
}
