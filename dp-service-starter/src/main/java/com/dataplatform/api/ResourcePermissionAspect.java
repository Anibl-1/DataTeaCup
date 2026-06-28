package com.dataplatform.api;

import cn.dev33.satoken.stp.StpUtil;
import com.dataplatform.common.annotation.RequireResourcePermission;
import com.dataplatform.common.exception.BusinessException;
import com.dataplatform.common.exception.ErrorCode;
import com.dataplatform.data.service.security.ResourcePermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * 资源级权限校验AOP切面
 * 拦截标注了 @RequireResourcePermission 的方法，校验当前用户是否拥有对指定资源的操作权限
 * 
 * 工作流程：
 * 1. 从 Sa-Token 获取当前登录用户ID
 * 2. 从方法参数中提取资源ID
 * 3. 调用 ResourcePermissionService 检查权限
 * 4. 权限不足时抛出 BusinessException
 * 
 * @author dataplatform
 * @see RequireResourcePermission
 * @see ResourcePermissionService
 */
@Slf4j
@Aspect
@Component
@Order(100) // 确保在登录校验之后执行
@RequiredArgsConstructor
public class ResourcePermissionAspect {

    private final ResourcePermissionService resourcePermissionService;

    /**
     * 环绕通知：拦截带有 @RequireResourcePermission 注解的方法
     */
    @Around("@annotation(com.dataplatform.common.annotation.RequireResourcePermission)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取方法签名和注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequireResourcePermission annotation = method.getAnnotation(RequireResourcePermission.class);

        // 获取当前用户ID（从Sa-Token获取）
        Long userId = getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录或登录已过期");
        }

        // 检查是否允许管理员跳过权限检查
        if (annotation.allowAdmin() && isAdmin(userId)) {
            log.debug("用户 {} 是管理员，跳过资源权限检查", userId);
            return joinPoint.proceed();
        }

        // 从方法参数中提取资源ID
        Long resourceId = extractResourceId(joinPoint, annotation.resourceIdParam());
        if (resourceId == null) {
            log.warn("无法从方法参数中提取资源ID，参数名: {}", annotation.resourceIdParam());
            throw new BusinessException(ErrorCode.PARAM_ERROR, "资源ID不能为空");
        }

        // 获取注解配置
        String resourceType = annotation.resourceType();
        String permission = annotation.permission();

        // 检查权限
        boolean hasPermission = resourcePermissionService.hasPermission(
                userId, resourceType, resourceId, permission);

        if (!hasPermission) {
            String errorMessage = annotation.message().isEmpty()
                    ? String.format("权限不足，无法对资源[%s:%d]执行[%s]操作", 
                            resourceType, resourceId, permission)
                    : annotation.message();
            
            log.warn("用户 {} 无权对资源 {}:{} 执行 {} 操作", 
                    userId, resourceType, resourceId, permission);
            throw new BusinessException(ErrorCode.FORBIDDEN, errorMessage);
        }

        log.debug("用户 {} 对资源 {}:{} 的 {} 权限校验通过", 
                userId, resourceType, resourceId, permission);
        return joinPoint.proceed();
    }

    /**
     * 获取当前登录用户ID
     * 从 Sa-Token 中获取当前会话的用户ID
     */
    private Long getCurrentUserId() {
        try {
            if (!StpUtil.isLogin()) {
                return null;
            }
            Object loginId = StpUtil.getLoginId();
            if (loginId instanceof Long) {
                return (Long) loginId;
            } else if (loginId instanceof Integer) {
                return ((Integer) loginId).longValue();
            } else if (loginId instanceof String) {
                return Long.parseLong((String) loginId);
            }
            return null;
        } catch (Exception e) {
            log.error("获取当前用户ID失败", e);
            return null;
        }
    }

    /**
     * 检查用户是否是管理员（通过 Sa-Token 缓存，零 DB 查询）
     */
    private boolean isAdmin(Long userId) {
        try {
            return StpUtil.hasRole("admin");
        } catch (Exception e) {
            log.error("检查用户管理员角色失败: userId={}", userId, e);
            return false;
        }
    }

    /**
     * 从方法参数中提取资源ID
     * 
     * @param joinPoint 切点
     * @param paramName 参数名称，支持简单参数名或嵌套属性（如 "dto.id"）
     * @return 资源ID，如果无法提取则返回 null
     */
    private Long extractResourceId(ProceedingJoinPoint joinPoint, String paramName) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] parameterNames = signature.getParameterNames();
            Object[] args = joinPoint.getArgs();

            if (parameterNames == null || args == null) {
                return null;
            }

            // 检查是否是嵌套属性（如 "dto.id"）
            if (paramName.contains(".")) {
                return extractNestedProperty(parameterNames, args, paramName);
            }

            // 简单参数名匹配
            for (int i = 0; i < parameterNames.length; i++) {
                if (parameterNames[i].equals(paramName) && args[i] != null) {
                    return convertToLong(args[i]);
                }
            }

            // 如果直接匹配失败，尝试从对象属性中获取
            for (Object arg : args) {
                if (arg != null) {
                    Long id = getPropertyValue(arg, paramName);
                    if (id != null) {
                        return id;
                    }
                }
            }

            return null;
        } catch (Exception e) {
            log.error("提取资源ID失败: paramName={}", paramName, e);
            return null;
        }
    }

    /**
     * 提取嵌套属性值（如 "dto.id"）
     */
    private Long extractNestedProperty(String[] parameterNames, Object[] args, String paramName) {
        String[] parts = paramName.split("\\.", 2);
        String objectName = parts[0];
        String propertyName = parts[1];

        for (int i = 0; i < parameterNames.length; i++) {
            if (parameterNames[i].equals(objectName) && args[i] != null) {
                return getPropertyValue(args[i], propertyName);
            }
        }
        return null;
    }

    /**
     * 通过反射获取对象属性值
     */
    private Long getPropertyValue(Object obj, String propertyName) {
        try {
            // 尝试通过 getter 方法获取
            String getterName = "get" + capitalize(propertyName);
            Method getter = obj.getClass().getMethod(getterName);
            Object value = getter.invoke(obj);
            return convertToLong(value);
        } catch (NoSuchMethodException e) {
            // 尝试直接访问字段
            try {
                java.lang.reflect.Field field = obj.getClass().getDeclaredField(propertyName);
                field.setAccessible(true);
                Object value = field.get(obj);
                return convertToLong(value);
            } catch (Exception ex) {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 将值转换为 Long 类型
     */
    private Long convertToLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }

    /**
     * 首字母大写
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}
