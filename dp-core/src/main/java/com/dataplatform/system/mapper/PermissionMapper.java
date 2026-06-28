package com.dataplatform.system.mapper;

import com.dataplatform.system.entity.Permission;
import java.util.List;

/**
 * 权限Mapper接口
 * 
 * @author dataplatform
 */
public interface PermissionMapper {
    Permission selectById(Long id);
    Permission selectByPermissionCode(String permissionCode);
    List<Permission> selectAll();
    List<Permission> selectByRoleId(Long roleId);
    List<Permission> selectByUserId(Long userId);
}
