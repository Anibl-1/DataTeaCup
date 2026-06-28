package com.dataplatform.data.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 角色更新DTO
 * 
 * @author dataplatform
 */
@Data
public class RoleUpdateDTO {
    
    /**
     * 角色ID
     */
    @NotNull(message = "角色ID不能为空")
    private Long id;
    
    /**
     * 角色名称
     */
    @Size(max = 50, message = "角色名称长度不能超过50个字符")
    private String roleName;
    
    /**
     * 角色编码
     */
    @Size(max = 50, message = "角色编码长度不能超过50个字符")
    private String roleCode;
    
    /**
     * 描述
     */
    @Size(max = 255, message = "描述长度不能超过255个字符")
    private String description;
}


