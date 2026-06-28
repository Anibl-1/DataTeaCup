package com.dataplatform.data.service.security;

import cn.dev33.satoken.stp.StpInterface;
import com.dataplatform.common.event.PermissionCacheInvalidateEvent;
import com.dataplatform.system.entity.Permission;
import com.dataplatform.system.entity.Role;
import com.dataplatform.system.mapper.PermissionMapper;
import com.dataplatform.system.mapper.RoleMapper;
import com.dataplatform.system.mapper.RoleMenuMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Sa-Token 权限数据加载接口实现（专业级）
 * 
 * <p>核心职责：为 Sa-Token 框架提供用户的权限和角色数据。</p>
 * 
 * <h3>权限数据源合并策略：</h3>
 * <ul>
 *   <li>数据源 A: {@code sys_role_permission → sys_permission.permission_code}（权限表）</li>
 *   <li>数据源 B: {@code sys_role_menu → sys_menu.permission_code}（菜单表）</li>
 *   <li>最终权限 = A ∪ B（取并集），确保两种配置方式均生效</li>
 * </ul>
 * 
 * <h3>缓存策略（L1 Caffeine 本地缓存）：</h3>
 * <ul>
 *   <li>权限列表缓存: 5 分钟 TTL，最多 2000 用户</li>
 *   <li>角色列表缓存: 5 分钟 TTL，最多 2000 用户</li>
 *   <li>角色/权限变更时通过 {@link #invalidateUser}/{@link #invalidateAll} 清除缓存</li>
 * </ul>
 * 
 * <p>注：Sa-Token 自身通过 Redis 进行二级缓存（sa-token-redis-jackson），
 * 本类的 Caffeine 缓存作为 L1 进一步减少 Redis 访问。</p>
 *
 * @author dataplatform
 */
@Slf4j
@Component
public class StpInterfaceImpl implements StpInterface {

    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final RoleMenuMapper roleMenuMapper;

    /** L1 权限缓存: userId → permissionCodes */
    private final Cache<Long, List<String>> permissionCache;

    /** L1 角色缓存: userId → roleCodes */
    private final Cache<Long, List<String>> roleCache;

    public StpInterfaceImpl(RoleMapper roleMapper,
                            PermissionMapper permissionMapper,
                            RoleMenuMapper roleMenuMapper) {
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.roleMenuMapper = roleMenuMapper;

        // 初始化 Caffeine L1 缓存
        this.permissionCache = Caffeine.newBuilder()
                .maximumSize(2000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats()
                .build();

        this.roleCache = Caffeine.newBuilder()
                .maximumSize(2000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }

    /**
     * 获取用户的权限编码列表（带缓存）
     * 合并 sys_permission 和 sys_menu.permission_code 两个数据源
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        Long userId = parseUserId(loginId);
        if (userId == null) {
            return Collections.emptyList();
        }

        return permissionCache.get(userId, this::loadPermissions);
    }

    /**
     * 获取用户的角色编码列表（带缓存）
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Long userId = parseUserId(loginId);
        if (userId == null) {
            return Collections.emptyList();
        }

        return roleCache.get(userId, this::loadRoles);
    }

    // ========================= 缓存失效（事件驱动）=========================

    /**
     * 监听权限缓存失效事件（异步执行，不阻塞业务事务）
     * 由 RoleService/UserService 在角色/权限/菜单变更后发布
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPermissionCacheInvalidate(PermissionCacheInvalidateEvent event) {
        if (event.isInvalidateAll()) {
            invalidateAll();
        } else {
            invalidateUser(event.getUserId());
        }
    }

    /**
     * 清除指定用户的权限和角色缓存
     */
    public void invalidateUser(Long userId) {
        if (userId == null) return;
        permissionCache.invalidate(userId);
        roleCache.invalidate(userId);
        log.info("已清除用户 {} 的权限缓存", userId);
    }

    /**
     * 清除所有用户的权限和角色缓存
     */
    public void invalidateAll() {
        permissionCache.invalidateAll();
        roleCache.invalidateAll();
        log.info("已清除所有用户的权限缓存");
    }

    /**
     * 获取缓存统计信息（用于监控）
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("permissionCache.size", permissionCache.estimatedSize());
        stats.put("permissionCache.hitRate", permissionCache.stats().hitRate());
        stats.put("permissionCache.hitCount", permissionCache.stats().hitCount());
        stats.put("permissionCache.missCount", permissionCache.stats().missCount());
        stats.put("roleCache.size", roleCache.estimatedSize());
        stats.put("roleCache.hitRate", roleCache.stats().hitRate());
        stats.put("roleCache.hitCount", roleCache.stats().hitCount());
        stats.put("roleCache.missCount", roleCache.stats().missCount());
        return stats;
    }

    // ========================= 内部加载方法 =========================

    /**
     * 从数据库加载用户权限（合并双数据源）
     */
    private List<String> loadPermissions(Long userId) {
        try {
            Set<String> permissionSet = new LinkedHashSet<>();

            // 数据源 A: sys_role_permission → sys_permission
            List<Permission> permissions = permissionMapper.selectByUserId(userId);
            if (permissions != null) {
                for (Permission p : permissions) {
                    if (p.getPermissionCode() != null && !p.getPermissionCode().isEmpty()) {
                        permissionSet.add(p.getPermissionCode());
                    }
                }
            }

            // 数据源 B: sys_role_menu → sys_menu.permission_code
            List<String> menuPermissions = roleMenuMapper.selectPermissionCodesByUserId(userId);
            if (menuPermissions != null) {
                for (String code : menuPermissions) {
                    if (code != null && !code.isEmpty()) {
                        permissionSet.add(code);
                    }
                }
            }

            List<String> result = new ArrayList<>(permissionSet);
            log.debug("加载用户 {} 权限: {} 条 (permission表: {}, menu表: {})",
                    userId, result.size(),
                    permissions != null ? permissions.size() : 0,
                    menuPermissions != null ? menuPermissions.size() : 0);
            return result;
        } catch (Exception e) {
            log.error("加载用户权限失败, userId={}", userId, e);
            return Collections.emptyList();
        }
    }

    /**
     * 从数据库加载用户角色
     */
    private List<String> loadRoles(Long userId) {
        try {
            List<Role> roles = roleMapper.selectByUserId(userId);
            if (roles == null || roles.isEmpty()) {
                return Collections.emptyList();
            }

            List<String> result = roles.stream()
                    .map(Role::getRoleCode)
                    .filter(code -> code != null && !code.isEmpty())
                    .collect(Collectors.toList());

            log.debug("加载用户 {} 角色: {}", userId, result);
            return result;
        } catch (Exception e) {
            log.error("加载用户角色失败, userId={}", userId, e);
            return Collections.emptyList();
        }
    }

    private Long parseUserId(Object loginId) {
        if (loginId == null) return null;
        if (loginId instanceof Long) return (Long) loginId;
        if (loginId instanceof Integer) return ((Integer) loginId).longValue();
        if (loginId instanceof Number) return ((Number) loginId).longValue();
        try {
            return Long.parseLong(loginId.toString());
        } catch (NumberFormatException e) {
            log.warn("无法解析登录ID为用户ID: {}", loginId);
            return null;
        }
    }
}
