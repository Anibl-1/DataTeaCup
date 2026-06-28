package com.dataplatform.common.util;

import com.dataplatform.common.constants.Constants;
import com.dataplatform.common.exception.BusinessException;
import com.dataplatform.common.exception.ErrorCode;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 用户上下文工具类
 * 
 * @author dataplatform
 */
public class UserContextUtil {

    private static final String USER_ID_ATTRIBUTE = "currentUserId";

    public static Long getCurrentUserId() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) return 1L;

        HttpServletRequest request = attributes.getRequest();
        Object userId = request.getAttribute(USER_ID_ATTRIBUTE);

        if (userId == null) return 1L;
        if (userId instanceof Long) return (Long) userId;

        try { return Long.parseLong(userId.toString()); }
        catch (NumberFormatException e) { throw new BusinessException(ErrorCode.UNAUTHORIZED, "无效的用户ID"); }
    }

    public static void setCurrentUserId(Long userId) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) attributes.getRequest().setAttribute(USER_ID_ATTRIBUTE, userId);
    }

    public static String extractToken(String authorization) {
        if (!StringUtils.hasText(authorization)) return null;
        if (!authorization.startsWith(Constants.TOKEN_PREFIX)) return null;
        return authorization.substring(Constants.TOKEN_PREFIX.length());
    }

    public static String getUsernameFromToken(String token, JwtUtil jwtUtil) {
        if (!StringUtils.hasText(token)) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未授权");
        try {
            if (!jwtUtil.validateToken(token)) throw new BusinessException(ErrorCode.TOKEN_EXPIRED, "Token已过期");
            return jwtUtil.getUsernameFromToken(token);
        } catch (BusinessException e) { throw e; }
        catch (Exception e) { throw new BusinessException(ErrorCode.INVALID_TOKEN, "Token无效"); }
    }

    public static String getUsernameFromAuthorization(String authorization, JwtUtil jwtUtil) {
        String token = extractToken(authorization);
        if (token == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未授权");
        return getUsernameFromToken(token, jwtUtil);
    }
}
