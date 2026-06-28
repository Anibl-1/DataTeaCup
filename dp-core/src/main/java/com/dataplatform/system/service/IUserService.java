package com.dataplatform.system.service;

import com.dataplatform.system.entity.User;

import java.util.List;
import java.util.Map;

/**
 * 用户服务接口
 * 
 * @author dataplatform
 */
public interface IUserService {
    Map<String, Object> login(String username, String password);
    Map<String, Object> getUserInfo(String username);
    List<User> getUserList(Integer page, Integer pageSize, String filters);
    long getUserCount(String filters);
    void createUser(User user);
    void updateUser(User user);
    void deleteUser(Long id);
    void assignRoles(Long userId, List<Long> roleIds);
    List<com.dataplatform.system.entity.Role> getUserRoles(Long userId);
    void changePassword(String username, String oldPassword, String newPassword);
    void resetPassword(Long userId);
    List<User> getUsersByPostId(Long postId);
}
