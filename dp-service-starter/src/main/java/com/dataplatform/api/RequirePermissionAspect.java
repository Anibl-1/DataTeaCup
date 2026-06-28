package com.dataplatform.api;

import cn.dev33.satoken.stp.StpUtil;
import com.dataplatform.common.annotation.RequirePermission;
import com.dataplatform.common.annotation.RequireRole;
import com.dataplatform.common.exception.BusinessException;
import com.dataplatform.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * 统一权限校验AOP切面（专业级）
 * 
 * <p>拦截 {@link RequirePermission} 和 {@link RequireRole} 注解，
 * 委托 Sa-Token 鉴权引擎进行权限校验。</p>
 * 
 * <h3>核心改进（相比旧版）：</h3>
 * <ul>
 *   <li>零 DB 查询：权限数据通过 Sa-Token → StpInterfaceImpl（Caffeine L1 + Redis L2）获取</li>
 *   <li>支持类级别注解：类上的注解对所有方法生效，方法级注解优先覆盖</li>
 *   <li>支持角色校验：{@code @RequireRole("admin")} 直接校验角色</li>
 *   <li>admin 角色自动放行：管理员拥有所有权限（通过 Sa-Token hasRole 判断）</li>
 * </ul>
 * 
 * @author dataplatform
 */
@Slf4j
@Aspect
@Component
@Order(50)
public class RequirePermissionAspect {

    /**
     * 所有登录用户默认拥有的基础权限（无需数据库分配）
     * 
     * <p>数据平台核心理念：登录即可使用基础功能，管理功能需额外授权。</p>
     * <p>仅管理类操作（用户/角色管理、系统监控、升级等）需要显式权限分配。</p>
     */
    private static final Set<String> DEFAULT_USER_PERMISSIONS = Set.of(
        // ==================== 数据查询与管理 ====================
        "tabledata:read", "tabledata:manage",
        "data:query", "data:export",
        "data:source:read",
        "data:view",
        // ==================== 报表 & 图表 & 页面 & 仪表盘（读写） ====================
        "report:read", "report:manage",
        "chart:read", "chart:manage",
        "page:read", "page:manage",
        "dashboard:read", "dashboard:manage",
        // ==================== AI 助手 & 聊天 ====================
        "ai:use", "chat:read",
        // ==================== 样式 & 通知 & 版本 ====================
        "style:read", "style:manage",
        "notification:read",
        "version:read",
        // ==================== 系统配置（只读） ====================
        "system:config:read",
        // ==================== 协作功能 ====================
        "team:read", "team:manage",
        "ticket:read", "ticket:manage",
        "data:collect:read", "data:collect:manage",
        "comment:read", "comment:manage",
        "share:read", "share:manage",
        // ==================== 数据治理（只读） ====================
        "data:dictionary", "data:lineage", "data:quality",
        // ==================== 告警（只读） ====================
        "alert:read",
        // ==================== 其他基础功能 ====================
        "pipeline:read",
        "dynamic:parameter:read", "dynamic:parameter:manage",
        "user:read",
        "menu:read",
        "announcement:read"
    );

    // ========================= Pointcut 定义 =========================

    @Pointcut("@annotation(com.dataplatform.common.annotation.RequirePermission) || " +
              "@within(com.dataplatform.common.annotation.RequirePermission)")
    public void requirePermissionPointcut() {}

    @Pointcut("@annotation(com.dataplatform.common.annotation.RequireRole) || " +
              "@within(com.dataplatform.common.annotation.RequireRole)")
    public void requireRolePointcut() {}

    // ========================= 权限校验 =========================

    @Around("requirePermissionPointcut()")
    public Object checkPermission(ProceedingJoinPoint point) throws Throwable {
        // 确保已登录（Sa-Token 会话）
        ensureLogin();

        // admin 角色直接放行
        if (StpUtil.hasRole("admin")) {
            return point.proceed();
        }

        // 获取注解（方法级优先，其次类级）
        RequirePermission annotation = getPermissionAnnotation(point);
        if (annotation == null) {
            return point.proceed();
        }

        String[] requiredPermissions = annotation.value();
        RequirePermission.Logical logical = annotation.logical();

        // 快速路径：如果所需权限全部在默认基础权限集中，直接放行
        boolean allDefault = Arrays.stream(requiredPermissions).allMatch(this::isDefaultPermission);
        if (allDefault) {
            return point.proceed();
        }

        // 委托 Sa-Token 鉴权（底层走缓存，零 DB 查询）
        boolean hasPermission = checkPermissions(requiredPermissions, logical);

        if (!hasPermission) {
            MethodSignature signature = (MethodSignature) point.getSignature();
            log.warn("[权限拒绝] userId={}, target={}.{}, 需要权限: {} ({})",
                    getLoginId(), point.getTarget().getClass().getSimpleName(),
                    signature.getMethod().getName(),
                    Arrays.toString(requiredPermissions), logical);
            throw new BusinessException(ErrorCode.FORBIDDEN, "权限不足，无法执行此操作");
        }

        return point.proceed();
    }

    // ========================= 角色校验 =========================

    @Around("requireRolePointcut()")
    public Object checkRole(ProceedingJoinPoint point) throws Throwable {
        ensureLogin();

        // 获取注解（方法级优先，其次类级）
        RequireRole annotation = getRoleAnnotation(point);
        if (annotation == null) {
            return point.proceed();
        }

        String[] requiredRoles = annotation.value();
        RequireRole.Logical logical = annotation.logical();

        boolean hasRole = checkRoles(requiredRoles, logical);

        if (!hasRole) {
            MethodSignature signature = (MethodSignature) point.getSignature();
            log.warn("[角色拒绝] userId={}, target={}.{}, 需要角色: {} ({})",
                    getLoginId(), point.getTarget().getClass().getSimpleName(),
                    signature.getMethod().getName(),
                    Arrays.toString(requiredRoles), logical);
            throw new BusinessException(ErrorCode.FORBIDDEN, "角色权限不足，无法执行此操作");
        }

        return point.proceed();
    }

    // ========================= 内部方法 =========================

    /**
     * 确保 Sa-Token 会话已登录
     */
    private void ensureLogin() {
        if (!StpUtil.isLogin()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录或登录已过期");
        }
    }

    /**
     * 获取 @RequirePermission 注解（方法级优先于类级）
     */
    private RequirePermission getPermissionAnnotation(ProceedingJoinPoint point) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();

        // 方法级注解优先
        RequirePermission annotation = method.getAnnotation(RequirePermission.class);
        if (annotation != null) {
            return annotation;
        }

        // 类级注解作为兜底
        return point.getTarget().getClass().getAnnotation(RequirePermission.class);
    }

    /**
     * 获取 @RequireRole 注解（方法级优先于类级）
     */
    private RequireRole getRoleAnnotation(ProceedingJoinPoint point) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();

        RequireRole annotation = method.getAnnotation(RequireRole.class);
        if (annotation != null) {
            return annotation;
        }

        return point.getTarget().getClass().getAnnotation(RequireRole.class);
    }

    /**
     * 通过 Sa-Token 校验权限（支持通配符）
     * 
     * <p>匹配规则（优先级由高到低）：</p>
     * <ol>
     *   <li>{@code "*"} — 超级权限，直接放行所有检查</li>
     *   <li>精确匹配 — {@code "system:config"} 匹配 {@code "system:config"}</li>
     *   <li>命名空间通配符 — {@code "system:*"} 匹配所有 {@code "system:xxx"}</li>
     * </ol>
     */
    private boolean checkPermissions(String[] requiredPermissions, RequirePermission.Logical logical) {
        List<String> userPermissions = StpUtil.getPermissionList();

        // 超级权限直接放行
        if (userPermissions.contains("*")) {
            return true;
        }

        if (logical == RequirePermission.Logical.AND) {
            return Arrays.stream(requiredPermissions).allMatch(p -> hasPermissionMatch(userPermissions, p));
        } else {
            return Arrays.stream(requiredPermissions).anyMatch(p -> hasPermissionMatch(userPermissions, p));
        }
    }

    /**
     * 检查权限是否在默认基础权限集中（所有登录用户自动拥有）
     */
    private boolean isDefaultPermission(String permission) {
        if (DEFAULT_USER_PERMISSIONS.contains(permission)) {
            return true;
        }
        // 命名空间匹配：如 "report:read" 在默认集中，则 "report:read:xxx" 也算
        int colonIdx = permission.lastIndexOf(':');
        if (colonIdx > 0) {
            String parent = permission.substring(0, colonIdx);
            return DEFAULT_USER_PERMISSIONS.contains(parent);
        }
        return false;
    }

    /**
     * 单个权限码匹配：精确匹配 或 命名空间通配符匹配
     */
    private boolean hasPermissionMatch(List<String> userPermissions, String required) {
        if (userPermissions.contains(required)) {
            return true;
        }
        // 命名空间通配符：如用户有 "system:*"，则 required="system:config" 也匹配
        int colonIdx = required.lastIndexOf(':');
        if (colonIdx > 0) {
            String namespaceWildcard = required.substring(0, colonIdx + 1) + "*";
            return userPermissions.contains(namespaceWildcard);
        }
        return false;
    }

    /**
     * 通过 Sa-Token 校验角色
     */
    private boolean checkRoles(String[] requiredRoles, RequireRole.Logical logical) {
        List<String> userRoles = StpUtil.getRoleList();

        if (logical == RequireRole.Logical.AND) {
            return Arrays.stream(requiredRoles).allMatch(userRoles::contains);
        } else {
            return Arrays.stream(requiredRoles).anyMatch(userRoles::contains);
        }
    }

    private Object getLoginId() {
        try {
            return StpUtil.getLoginId();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
