package com.dataplatform.system.mapper;

import com.dataplatform.system.entity.Role;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 角色Mapper接口
 * 
 * NOTE: 此接口引用 com.dataplatform.common.dto.FilterCondition，
 * 该类当前位于 backend 模块。待 dp-data 模块创建后将迁移到合适的位置。
 * 
 * @author dataplatform
 */
public interface RoleMapper {
    Role selectById(Long id);
    Role selectByRoleCode(String roleCode);
    List<Role> selectList(@Param("offset") Integer offset, @Param("pageSize") Integer pageSize, 
                         @Param("filters") java.util.List<com.dataplatform.common.dto.FilterCondition> filters);
    long count(@Param("filters") java.util.List<com.dataplatform.common.dto.FilterCondition> filters);
    int insert(Role role);
    int update(Role role);
    int delete(Long id);
    List<Role> selectByUserId(Long userId);
    int insertRolePermissions(@Param("roleId") Long roleId, @Param("permissionIds") List<Long> permissionIds);
    int deleteRolePermissions(Long roleId);
    List<Long> selectPermissionIdsByRoleId(Long roleId);
}
