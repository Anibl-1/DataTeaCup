package com.dataplatform.data.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 角色权限分配DTO
 * 
 * @author dataplatform
 */
@Data
public class RolePermissionDTO {
    
    /**
     * 角色ID
     */
    @NotNull(message = "角色ID不能为空")
    private Long roleId;
    
    /**
     * 权限ID列表
     */
    private List<Long> permissionIds;
}


