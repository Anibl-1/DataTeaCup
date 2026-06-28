package com.dataplatform.common.security;

import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 安全上下文工具类
 * 提供线程安全的当前用户信息获取方法，统一从 Sa-Token 会话中读取
 * 
 * <p>所有 Controller/Service 需要获取当前用户信息时，应使用此工具类，
 * 避免直接从 HttpServletRequest 或 Sa-Token API 取值。</p>
 * 
 * @author dataplatform
 */
@Slf4j
public final class SecurityContext {

    private SecurityContext() {
        // 工具类禁止实例化
    }

    /**
     * 获取当前登录用户ID
     * 
     * @return 用户ID，未登录返回 null
     */
    public static Long getCurrentUserId() {
        try {
            if (!StpUtil.isLogin()) {
                return null;
            }
            Object loginId = StpUtil.getLoginId();
            return parseUserId(loginId);
        } catch (Exception e) {
            log.debug("获取当前用户ID失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取当前登录用户ID（必须已登录，否则抛异常）
     * 
     * @return 用户ID
     * @throws cn.dev33.satoken.exception.NotLoginException 未登录时抛出
     */
    public static Long requireCurrentUserId() {
        Object loginId = StpUtil.getLoginId();
        return parseUserId(loginId);
    }

    /**
     * 获取当前登录用户名（存储在 Sa-Token Session 中）
     * 
     * @return 用户名，未登录返回 null
     */
    public static String getCurrentUsername() {
        try {
            if (!StpUtil.isLogin()) {
                return null;
            }
            return (String) StpUtil.getSession().get("username");
        } catch (Exception e) {
            log.debug("获取当前用户名失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 判断当前用户是否已登录
     */
    public static boolean isAuthenticated() {
        try {
            return StpUtil.isLogin();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 判断当前用户是否具有指定角色
     */
    public static boolean hasRole(String role) {
        try {
            return StpUtil.hasRole(role);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 判断当前用户是否具有指定权限
     */
    public static boolean hasPermission(String permission) {
        try {
            return StpUtil.hasPermission(permission);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 判断当前用户是否是管理员
     */
    public static boolean isAdmin() {
        return hasRole("admin");
    }

    private static Long parseUserId(Object loginId) {
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
