package com.dataplatform.system.service;

import com.dataplatform.common.constants.Constants;
import com.dataplatform.common.exception.BusinessException;
import com.dataplatform.common.exception.ErrorCode;
import com.dataplatform.system.entity.User;
import com.dataplatform.system.entity.Role;
import com.dataplatform.system.entity.Menu;
import com.dataplatform.system.mapper.UserMapper;
import com.dataplatform.system.mapper.RoleMapper;
import com.dataplatform.system.mapper.RoleMenuMapper;
import com.dataplatform.system.mapper.MenuMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户服务实现
 * 
 * NOTE: 此类引用了以下跨模块类，待后续模块创建后解决：
 * - com.dataplatform.common.dto.FilterCondition (dp-data)
 * - com.dataplatform.common.util.FilterUtil (dp-data)
 * - com.dataplatform.common.util.JwtUtil (dp-infra)
 * - com.dataplatform.common.util.MD5Util (dp-common/util)
 * - com.dataplatform.data.service.security.LoginSecurityService (dp-infra)
 */
@Slf4j
@Service
public class UserService implements IUserService {
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private RoleMapper roleMapper;
    
    @Autowired
    private RoleMenuMapper roleMenuMapper;
    
    @Autowired
    private MenuMapper menuMapper;
    
    @Autowired
    private com.dataplatform.common.util.JwtUtil jwtUtil;
    
    @Autowired
    private com.dataplatform.common.security.LoginSecurityService loginSecurityService;

    @Autowired
    private org.springframework.context.ApplicationEventPublisher eventPublisher;
    
    @Value("${user.defaultPassword:admin123}")
    private String defaultPassword;

    public Map<String, Object> login(String username, String password) {
        if (!StringUtils.hasText(username)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户名不能为空");
        }
        if (!StringUtils.hasText(password)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "密码不能为空");
        }
        
        long lockedSeconds = loginSecurityService.checkLocked(username);
        if (lockedSeconds > 0) {
            long minutes = (lockedSeconds + 59) / 60;
            throw new BusinessException(ErrorCode.FORBIDDEN, 
                    "账户已被锁定，请" + minutes + "分钟后再试");
        }
        
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            loginSecurityService.recordFailure(username);
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户名或密码错误");
        }
        if (!user.getPassword().equals(com.dataplatform.common.util.MD5Util.encrypt(password))) {
            long lockTime = loginSecurityService.recordFailure(username);
            if (lockTime > 0) {
                throw new BusinessException(ErrorCode.FORBIDDEN, 
                        "登录失败次数过多，账户已被锁定" + (lockTime / 60) + "分钟");
            }
            int remaining = loginSecurityService.getRemainingAttempts(username);
            throw new BusinessException(ErrorCode.USER_PASSWORD_ERROR, 
                    "用户名或密码错误，还剩" + remaining + "次重试机会");
        }
        if (user.getStatus() != Constants.USER_STATUS_ENABLED) {
            throw new BusinessException(ErrorCode.USER_DISABLED, "用户已被禁用");
        }
        
        loginSecurityService.clearFailures(username);

        String token = jwtUtil.generateToken(username, user.getId());
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        
        Map<String, Object> userInfo = buildUserInfo(user);
        result.put("userInfo", userInfo);
        
        result.put("mustChangePassword", 
                user.getMustChangePassword() != null && user.getMustChangePassword() == 1);
        
        return result;
    }

    public Map<String, Object> getUserInfo(String username) {
        if (!StringUtils.hasText(username)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户名不能为空");
        }
        
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }
        
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> userInfo = buildUserInfo(user);
        result.put("userInfo", userInfo);
        
        List<Role> roles = roleMapper.selectByUserId(user.getId());
        boolean isAdmin = roles.stream().anyMatch(r -> "admin".equals(r.getRoleCode()));
        
        List<String> permissionCodes;
        
        if (isAdmin) {
            permissionCodes = new ArrayList<>();
            permissionCodes.add("*");
        } else {
            List<Long> userMenuIds = roleMenuMapper.selectMenuIdsByUserId(user.getId());
            
            if (userMenuIds == null || userMenuIds.isEmpty()) {
                permissionCodes = new ArrayList<>();
            } else {
                List<Menu> allMenus = menuMapper.selectAll();
                Set<String> permissionCodeSet = new HashSet<>();
                
                for (Long menuId : userMenuIds) {
                    Menu menu = allMenus.stream()
                        .filter(m -> m.getId() != null && m.getId().equals(menuId))
                        .findFirst()
                        .orElse(null);
                    
                    if (menu != null && StringUtils.hasText(menu.getPermissionCode())) {
                        permissionCodeSet.add(menu.getPermissionCode());
                    }
                }
                
                permissionCodes = new ArrayList<>(permissionCodeSet);
            }
        }
        result.put("permissions", permissionCodes);
        
        List<String> roleCodes = roles.stream()
            .map(Role::getRoleCode)
            .collect(Collectors.toList());
        result.put("roles", roleCodes);
        
        return result;
    }

    public List<User> getUserList(Integer page, Integer pageSize, String filters) {
        if (page == null || page < 1) { page = Constants.DEFAULT_PAGE; }
        if (pageSize == null || pageSize < 1) { pageSize = Constants.DEFAULT_PAGE_SIZE; }
        int offset = (page - 1) * pageSize;
        List<com.dataplatform.common.dto.FilterCondition> filterList = com.dataplatform.common.util.FilterUtil.parseFilters(filters);
        return userMapper.selectList(offset, pageSize, filterList);
    }

    public long getUserCount(String filters) {
        List<com.dataplatform.common.dto.FilterCondition> filterList = com.dataplatform.common.util.FilterUtil.parseFilters(filters);
        return userMapper.count(filterList);
    }

    @Transactional
    public void createUser(User user) {
        if (user == null) { throw new BusinessException(ErrorCode.PARAM_ERROR, "用户信息不能为空"); }
        if (!StringUtils.hasText(user.getUsername())) { throw new BusinessException(ErrorCode.PARAM_ERROR, "用户名不能为空"); }
        if (!StringUtils.hasText(user.getPassword())) { throw new BusinessException(ErrorCode.PARAM_ERROR, "密码不能为空"); }
        User existingUser = userMapper.selectByUsername(user.getUsername());
        if (existingUser != null) { throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "用户名已存在"); }
        user.setPassword(com.dataplatform.common.util.MD5Util.encrypt(user.getPassword()));
        if (user.getStatus() == null) { user.setStatus(Constants.USER_STATUS_ENABLED); }
        if (user.getMustChangePassword() == null) { user.setMustChangePassword(1); }
        userMapper.insert(user);
    }

    @Transactional
    public void updateUser(User user) {
        if (user == null) { throw new BusinessException(ErrorCode.PARAM_ERROR, "用户信息不能为空"); }
        if (user.getId() == null) { throw new BusinessException(ErrorCode.PARAM_ERROR, "用户ID不能为空"); }
        User existingUser = userMapper.selectById(user.getId());
        if (existingUser == null) { throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"); }
        userMapper.update(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        if (id == null) { throw new BusinessException(ErrorCode.PARAM_ERROR, "用户ID不能为空"); }
        User user = userMapper.selectById(id);
        if (user == null) { throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"); }
        if (Constants.ADMIN_USERNAME.equals(user.getUsername())) { throw new BusinessException(ErrorCode.FORBIDDEN, "不能删除管理员用户"); }
        userMapper.deleteUserRoles(id);
        userMapper.delete(id);
    }
    
    @Transactional
    public void assignRoles(Long userId, List<Long> roleIds) {
        if (userId == null) { throw new BusinessException(ErrorCode.PARAM_ERROR, "用户ID不能为空"); }
        User user = userMapper.selectById(userId);
        if (user == null) { throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"); }
        userMapper.deleteUserRoles(userId);
        if (roleIds != null && !roleIds.isEmpty()) { userMapper.insertUserRoles(userId, roleIds); }
        // 发布缓存失效事件，清除该用户的权限缓存，使角色变更立即生效
        eventPublisher.publishEvent(new com.dataplatform.common.event.PermissionCacheInvalidateEvent(this, userId));
    }
    
    public List<Role> getUserRoles(Long userId) {
        if (userId == null) { throw new BusinessException(ErrorCode.PARAM_ERROR, "用户ID不能为空"); }
        return roleMapper.selectByUserId(userId);
    }
    
    private Map<String, Object> buildUserInfo(User user) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("nickname", user.getNickname());
        userInfo.put("email", user.getEmail());
        userInfo.put("avatar", user.getAvatar());
        List<Role> roles = roleMapper.selectByUserId(user.getId());
        List<String> roleCodes = roles.stream().map(Role::getRoleCode).collect(Collectors.toList());
        userInfo.put("roles", roleCodes);
        return userInfo;
    }
    
    @Transactional
    public void changePassword(String username, String oldPassword, String newPassword) {
        if (!StringUtils.hasText(username)) { throw new BusinessException(ErrorCode.PARAM_ERROR, "用户名不能为空"); }
        if (!StringUtils.hasText(oldPassword)) { throw new BusinessException(ErrorCode.PARAM_ERROR, "旧密码不能为空"); }
        if (!StringUtils.hasText(newPassword)) { throw new BusinessException(ErrorCode.PARAM_ERROR, "新密码不能为空"); }
        validatePasswordStrength(newPassword);
        User user = userMapper.selectByUsername(username);
        if (user == null) { throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"); }
        if (!user.getPassword().equals(com.dataplatform.common.util.MD5Util.encrypt(oldPassword))) { throw new BusinessException(ErrorCode.USER_PASSWORD_ERROR, "旧密码错误"); }
        if (oldPassword.equals(newPassword)) { throw new BusinessException(ErrorCode.PARAM_ERROR, "新密码不能与旧密码相同"); }
        user.setPassword(com.dataplatform.common.util.MD5Util.encrypt(newPassword));
        user.setMustChangePassword(0);
        userMapper.update(user);
    }
    
    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < 8) { throw new BusinessException(ErrorCode.PARAM_ERROR, "密码长度至少8位"); }
        boolean hasLowerCase = password.matches(".*[a-z].*");
        boolean hasUpperCase = password.matches(".*[A-Z].*");
        boolean hasDigit = password.matches(".*[0-9].*");
        boolean hasSpecialChar = password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*");
        int typeCount = 0;
        if (hasLowerCase) typeCount++;
        if (hasUpperCase) typeCount++;
        if (hasDigit) typeCount++;
        if (hasSpecialChar) typeCount++;
        if (typeCount < 2) { throw new BusinessException(ErrorCode.PARAM_ERROR, "密码强度太弱，请使用包含大小写字母、数字和特殊字符的组合"); }
    }
    
    @Transactional
    public void resetPassword(Long userId) {
        if (userId == null) { throw new BusinessException(ErrorCode.PARAM_ERROR, "用户ID不能为空"); }
        User user = userMapper.selectById(userId);
        if (user == null) { throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"); }
        if (Constants.ADMIN_USERNAME.equals(user.getUsername())) { throw new BusinessException(ErrorCode.FORBIDDEN, "管理员用户不允许重置密码"); }
        user.setPassword(com.dataplatform.common.util.MD5Util.encrypt(defaultPassword));
        user.setMustChangePassword(1);
        userMapper.update(user);
    }

    /**
     * 根据岗位ID查询用户列表
     */
    public List<User> getUsersByPostId(Long postId) {
        if (postId == null) { throw new BusinessException(ErrorCode.PARAM_ERROR, "岗位ID不能为空"); }
        return userMapper.selectByPostId(postId);
    }
}
