package com.dataplatform.data.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 授予权限请求DTO
 *
 * @author dataplatform
 */
@Data
public class GrantPermissionDTO {

    /** 角色ID */
    @NotNull(message = "角色ID不能为空")
    private Long roleId;

    /** 资源类型: datasource, report, dashboard, folder */
    @NotEmpty(message = "资源类型不能为空")
    private String resourceType;

    /** 资源ID */
    @NotNull(message = "资源ID不能为空")
    private Long resourceId;

    /** 操作权限列表: view, edit, delete, export, share */
    @NotEmpty(message = "操作权限列表不能为空")
    private List<String> operations;
}
