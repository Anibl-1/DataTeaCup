package com.dataplatform.system.entity;

import lombok.Data;

@Data
public class RolePermission {
    private Long id;
    private Long roleId;
    private Long permissionId;
}
