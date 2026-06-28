package com.dataplatform.data.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 角色创建DTO
 * 
 * @author dataplatform
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RoleCreateDTO {
    
    /**
     * 角色名称
     */
    @NotBlank(message = "角色名称不能为空")
    @Size(max = 50, message = "角色名称长度不能超过50个字符")
    private String roleName;
    
    /**
     * 角色编码
     */
    @NotBlank(message = "角色编码不能为空")
    @Size(max = 50, message = "角色编码长度不能超过50个字符")
    private String roleCode;
    
    /**
     * 描述
     */
    @Size(max = 255, message = "描述长度不能超过255个字符")
    private String description;
}


