package com.dataplatform.system.mapper;

import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 角色菜单关联Mapper接口
 * 
 * @author dataplatform
 */
public interface RoleMenuMapper {
    int insertRoleMenus(@Param("roleId") Long roleId, @Param("menuIds") List<Long> menuIds);
    int deleteRoleMenus(Long roleId);
    List<Long> selectMenuIdsByRoleId(Long roleId);
    List<Long> selectMenuIdsByUserId(Long userId);
    List<String> selectPermissionCodesByUserId(Long userId);
}
