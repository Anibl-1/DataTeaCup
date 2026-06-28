package com.dataplatform.serviceapi.system.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Set;

/**
 * 跨服务角色 DTO
 */
@Data
public class RoleDTO implements Serializable {
    private Long id;
    private String roleCode;
    private String roleName;
    private Set<String> permissions;
}
