package com.dataplatform.system.service;

import com.dataplatform.common.event.PermissionCacheInvalidateEvent;
import com.dataplatform.common.exception.BusinessException;
import com.dataplatform.common.exception.ErrorCode;
import com.dataplatform.system.entity.Role;
import com.dataplatform.system.mapper.RoleMapper;
import com.dataplatform.system.mapper.PermissionMapper;
import com.dataplatform.system.mapper.RoleMenuMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 角色服务
 * 
 * NOTE: 此类引用了以下跨模块类，待后续模块创建后解决：
 * - com.dataplatform.common.dto.FilterCondition (dp-data)
 * - com.dataplatform.common.util.FilterUtil (dp-data)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {
    
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final ApplicationEventPublisher eventPublisher;
    
    public List<Role> getRoleList(Integer page, Integer pageSize, String filters) {
        if (page == null || page < 1) { page = 1; }
        if (pageSize == null || pageSize < 1) { pageSize = 10; }
        int offset = (page - 1) * pageSize;
        List<com.dataplatform.common.dto.FilterCondition> filterList = com.dataplatform.common.util.FilterUtil.parseFilters(filters);
        return roleMapper.selectList(offset, pageSize, filterList);
    }
    
    public long getRoleCount(String filters) {
        List<com.dataplatform.common.dto.FilterCondition> filterList = com.dataplatform.common.util.FilterUtil.parseFilters(filters);
        return roleMapper.count(filterList);
    }
    
    public Role getRoleById(Long id) {
        if (id == null) { throw new BusinessException(ErrorCode.PARAM_ERROR, "角色ID不能为空"); }
        Role role = roleMapper.selectById(id);
        if (role == null) { throw new BusinessException(ErrorCode.USER_NOT_FOUND, "角色不存在"); }
        return role;
    }
    
    @Transactional
    public Role createRole(Role role) {
        if (role == null) { throw new BusinessException(ErrorCode.PARAM_ERROR, "角色信息不能为空"); }
        if (!StringUtils.hasText(role.getRoleName())) { throw new BusinessException(ErrorCode.PARAM_ERROR, "角色名称不能为空"); }
        if (!StringUtils.hasText(role.getRoleCode())) { throw new BusinessException(ErrorCode.PARAM_ERROR, "角色编码不能为空"); }
        Role existingRole = roleMapper.selectByRoleCode(role.getRoleCode());
        if (existingRole != null) { throw new BusinessException(ErrorCode.PARAM_ERROR, "角色编码已存在"); }
        roleMapper.insert(role);
        return role;
    }
    
    @Transactional
    public Role updateRole(Role role) {
        if (role == null || role.getId() == null) { throw new BusinessException(ErrorCode.PARAM_ERROR, "角色信息或ID不能为空"); }
        Role existingRole = roleMapper.selectById(role.getId());
        if (existingRole == null) { throw new BusinessException(ErrorCode.USER_NOT_FOUND, "角色不存在"); }
        if (StringUtils.hasText(role.getRoleCode()) && !role.getRoleCode().equals(existingRole.getRoleCode())) {
            Role roleWithSameCode = roleMapper.selectByRoleCode(role.getRoleCode());
            if (roleWithSameCode != null && !roleWithSameCode.getId().equals(role.getId())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "角色编码已存在");
            }
        }
        roleMapper.update(role);
        return roleMapper.selectById(role.getId());
    }
    
    @Transactional
    public void deleteRole(Long id) {
        if (id == null) { throw new BusinessException(ErrorCode.PARAM_ERROR, "角色ID不能为空"); }
        Role role = roleMapper.selectById(id);
        if (role == null) { throw new BusinessException(ErrorCode.USER_NOT_FOUND, "角色不存在"); }
        if ("admin".equals(role.getRoleCode())) { throw new BusinessException(ErrorCode.PARAM_ERROR, "系统默认角色不能删除"); }
        roleMapper.deleteRolePermissions(id);
        roleMapper.delete(id);
        invalidatePermissionCache();
    }
    
    @Transactional
    public void assignPermissions(Long roleId, List<Long> permissionIds) {
        if (roleId == null) { throw new BusinessException(ErrorCode.PARAM_ERROR, "角色ID不能为空"); }
        Role role = roleMapper.selectById(roleId);
        if (role == null) { throw new BusinessException(ErrorCode.USER_NOT_FOUND, "角色不存在"); }
        roleMapper.deleteRolePermissions(roleId);
        if (permissionIds != null && !permissionIds.isEmpty()) { roleMapper.insertRolePermissions(roleId, permissionIds); }
        invalidatePermissionCache();
    }
    
    public List<com.dataplatform.system.entity.Permission> getRolePermissions(Long roleId) {
        if (roleId == null) { throw new BusinessException(ErrorCode.PARAM_ERROR, "角色ID不能为空"); }
        return permissionMapper.selectByRoleId(roleId);
    }
    
    public List<com.dataplatform.system.entity.Permission> getAllPermissions() {
        return permissionMapper.selectAll();
    }
    
    public List<Role> getUserRoles(Long userId) {
        if (userId == null) { throw new BusinessException(ErrorCode.PARAM_ERROR, "用户ID不能为空"); }
        return roleMapper.selectByUserId(userId);
    }
    
    @Transactional
    public void assignMenus(Long roleId, List<Long> menuIds) {
        if (roleId == null) { throw new BusinessException(ErrorCode.PARAM_ERROR, "角色ID不能为空"); }
        Role role = roleMapper.selectById(roleId);
        if (role == null) { throw new BusinessException(ErrorCode.USER_NOT_FOUND, "角色不存在"); }
        roleMenuMapper.deleteRoleMenus(roleId);
        if (menuIds != null && !menuIds.isEmpty()) { roleMenuMapper.insertRoleMenus(roleId, menuIds); }
        invalidatePermissionCache();
    }
    
    public List<Long> getRoleMenus(Long roleId) {
        if (roleId == null) { throw new BusinessException(ErrorCode.PARAM_ERROR, "角色ID不能为空"); }
        return roleMenuMapper.selectMenuIdsByRoleId(roleId);
    }

    /**
     * 发布权限缓存失效事件（事务提交后执行，确保DB已写入）
     * 角色权限/菜单变更影响面广，清除全部用户缓存
     */
    private void invalidatePermissionCache() {
        eventPublisher.publishEvent(new PermissionCacheInvalidateEvent(this));
        log.info("已发布全局权限缓存失效事件");
    }
}
