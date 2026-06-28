package com.dataplatform.system.mapper;

import com.dataplatform.system.entity.User;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 用户Mapper接口
 * 
 * NOTE: 此接口引用 com.dataplatform.common.dto.FilterCondition，
 * 该类当前位于 backend 模块。待 dp-data 模块创建后将迁移到合适的位置。
 * 
 * @author dataplatform
 */
public interface UserMapper {
    User selectById(Long id);
    User selectByUsername(String username);
    List<User> selectList(@Param("offset") Integer offset, @Param("pageSize") Integer pageSize, 
                         @Param("filters") java.util.List<com.dataplatform.common.dto.FilterCondition> filters);
    long count(@Param("filters") java.util.List<com.dataplatform.common.dto.FilterCondition> filters);
    int insert(User user);
    int update(User user);
    int delete(Long id);
    int insertUserRoles(@Param("userId") Long userId, @Param("roleIds") List<Long> roleIds);
    int deleteUserRoles(Long userId);
    List<Long> selectRoleIdsByUserId(Long userId);
    List<User> selectByDeptId(@Param("deptId") Long deptId);
    List<User> selectByPostId(@Param("postId") Long postId);
}
