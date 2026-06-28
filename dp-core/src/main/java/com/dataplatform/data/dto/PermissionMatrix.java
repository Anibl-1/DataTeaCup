package com.dataplatform.data.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 权限矩阵DTO
 * 展示角色与资源的权限关系
 *
 * @author dataplatform
 */
@Data
public class PermissionMatrix {

    /** 资源类型 */
    private String resourceType;

    /** 角色权限行列表，每行代表一个角色对各资源的权限 */
    private List<RolePermissionRow> rows;

    /**
     * 角色权限行
     */
    @Data
    public static class RolePermissionRow {
        /** 角色ID */
        private Long roleId;

        /** 角色名称 */
        private String roleName;

        /** 资源权限映射: resourceId -> 操作列表 */
        private Map<Long, List<String>> resourceOperations;
    }
}
