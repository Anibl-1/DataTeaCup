package com.dataplatform.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dataplatform.data.entity.ResourcePermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 资源权限Mapper接口
 *
 * @author dataplatform
 */
@Mapper
public interface ResourcePermissionMapper extends BaseMapper<ResourcePermission> {

    /**
     * 根据角色ID和资源查询权限
     *
     * @param roleId 角色ID
     * @param resourceType 资源类型
     * @param resourceId 资源ID
     * @return 资源权限
     */
    ResourcePermission selectByRoleAndResource(@Param("roleId") Long roleId,
                                                @Param("resourceType") String resourceType,
                                                @Param("resourceId") Long resourceId);

    /**
     * 根据角色ID列表和资源查询权限
     *
     * @param roleIds 角色ID列表
     * @param resourceType 资源类型
     * @param resourceId 资源ID
     * @return 资源权限列表
     */
    List<ResourcePermission> selectByRoleIdsAndResource(@Param("roleIds") List<Long> roleIds,
                                                         @Param("resourceType") String resourceType,
                                                         @Param("resourceId") Long resourceId);

    /**
     * 根据角色ID列表和资源类型查询可访问的资源ID列表
     *
     * @param roleIds 角色ID列表
     * @param resourceType 资源类型
     * @param operation 操作类型
     * @return 资源ID列表
     */
    List<Long> selectAccessibleResourceIds(@Param("roleIds") List<Long> roleIds,
                                            @Param("resourceType") String resourceType,
                                            @Param("operation") String operation);

    /**
     * 根据资源类型和资源ID删除权限
     *
     * @param resourceType 资源类型
     * @param resourceId 资源ID
     * @return 影响行数
     */
    int deleteByResource(@Param("resourceType") String resourceType,
                          @Param("resourceId") Long resourceId);

    /**
     * 根据角色ID删除所有资源权限
     *
     * @param roleId 角色ID
     * @return 影响行数
     */
    int deleteByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据资源类型查询所有权限记录
     *
     * @param resourceType 资源类型
     * @return 资源权限列表
     */
    List<ResourcePermission> selectByResourceType(@Param("resourceType") String resourceType);

    /**
     * 根据角色ID和资源类型及资源ID删除权限
     *
     * @param roleId 角色ID
     * @param resourceType 资源类型
     * @param resourceId 资源ID
     * @return 影响行数
     */
    int deleteByRoleAndResource(@Param("roleId") Long roleId,
                                 @Param("resourceType") String resourceType,
                                 @Param("resourceId") Long resourceId);

    /**
     * 根据资源类型和资源ID查询权限列表
     *
     * @param resourceType 资源类型
     * @param resourceId 资源ID
     * @return 资源权限列表
     */
    List<ResourcePermission> selectByResourceTypeAndId(@Param("resourceType") String resourceType,
                                                        @Param("resourceId") Long resourceId);

    /**
     * 根据用户ID查询所有资源权限
     * 通过用户关联的角色查询权限
     *
     * @param userId 用户ID
     * @return 资源权限列表
     */
    List<ResourcePermission> selectByUserId(@Param("userId") Long userId);

}
