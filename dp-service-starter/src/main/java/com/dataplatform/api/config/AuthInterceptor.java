package com.dataplatform.api.config;

import com.dataplatform.common.constants.Constants;
import com.dataplatform.common.util.JwtUtil;
import com.dataplatform.system.entity.User;
import com.dataplatform.system.mapper.UserMapper;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.session.SaSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 认证拦截器（专业级）
 * 
 * <p>职责链：JWT Token 验证 → Sa-Token 会话同步 → Token 自动续期</p>
 * 
 * <h3>认证流程：</h3>
 * <ol>
 *   <li>从 Authorization Header 或 URL 参数提取 JWT Token</li>
 *   <li>验证 JWT Token 有效性</li>
 *   <li>提取 userId/username，同步到 Sa-Token 会话（含 Session 存储用户名）</li>
 *   <li>Sa-Token 会话建立后，后续权限校验通过 {@code StpUtil.getPermissionList()} 走缓存</li>
 *   <li>Token 即将过期时自动刷新，通过响应头返回新 Token</li>
 * </ol>
 * 
 * <p>与 {@code RequirePermissionAspect} 协作：
 * 本拦截器保证 Sa-Token 会话存在，切面再通过 Sa-Token 做权限校验。</p>
 *
 * @author dataplatform
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(AuthInterceptor.class);

    @Autowired(required = false)
    private JwtUtil jwtUtil;

    @Autowired(required = false)
    private UserMapper userMapper;

    private JwtUtil getJwtUtil(HttpServletRequest request) {
        if (jwtUtil != null) return jwtUtil;
        WebApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(request.getServletContext());
        if (ctx != null) jwtUtil = ctx.getBean(JwtUtil.class);
        return jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();
        logger.debug("[Auth] {} {}", request.getMethod(), requestURI);

        // CORS 预检放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // ========================= 1. 提取 Token =========================
        String authorization = request.getHeader("Authorization");

        // SSE（EventSource）不支持自定义 headers，允许通过 URL 参数传递 token
        if (!StringUtils.hasText(authorization)) {
            String tokenParam = request.getParameter("token");
            if (StringUtils.hasText(tokenParam)) {
                authorization = Constants.TOKEN_PREFIX + tokenParam;
            }
        }

        if (!StringUtils.hasText(authorization)) {
            logger.warn("[Auth] 缺少Authorization: {}", requestURI);
            sendUnauthorizedResponse(response, "未授权，请先登录");
            return false;
        }

        if (!authorization.startsWith(Constants.TOKEN_PREFIX)) {
            logger.warn("[Auth] Token格式错误: {}", requestURI);
            sendUnauthorizedResponse(response, "Token格式错误");
            return false;
        }

        String token = authorization.substring(Constants.TOKEN_PREFIX.length());

        // ========================= 2. 验证 JWT =========================
        JwtUtil jwt = getJwtUtil(request);
        if (jwt == null) {
            logger.error("[Auth] JwtUtil未初始化");
            sendUnauthorizedResponse(response, "服务器配置错误");
            return false;
        }

        try {
            if (!jwt.validateToken(token)) {
                logger.warn("[Auth] Token已过期: {}", requestURI);
                sendUnauthorizedResponse(response, "Token已过期，请重新登录");
                return false;
            }

            String username = jwt.getUsernameFromToken(token);
            Long userId = jwt.getUserIdFromToken(token);

            // JWT 中无 userId 时，从数据库补全（仅发生在旧版 Token）
            if (userId == null) {
                userId = resolveUserId(username, request);
            }

            // 向下兼容：设置 request attribute（部分旧代码仍依赖）
            request.setAttribute("currentUsername", username);
            if (userId != null) {
                request.setAttribute("userId", userId);
            }

            // ========================= 3. 同步 Sa-Token 会话 =========================
            if (userId != null) {
                syncSaTokenSession(userId, username);
            }

            // ========================= 4. Token 自动续期 =========================
            if (jwt.shouldRefreshToken(token)) {
                String newToken = jwt.refreshToken(token);
                if (newToken != null) {
                    response.setHeader("X-New-Token", newToken);
                    response.setHeader("Access-Control-Expose-Headers", "X-New-Token");
                    logger.debug("[Auth] Token已续期: user={}", username);
                }
            }

            logger.debug("[Auth] 认证通过: user={}, path={}", username, requestURI);
            return true;

        } catch (Exception e) {
            logger.error("[Auth] Token验证异常: {} - {}", requestURI, e.getMessage());
            sendUnauthorizedResponse(response, "Token无效，请重新登录");
            return false;
        }
    }

    /**
     * 同步 JWT 认证信息到 Sa-Token 会话
     * Sa-Token 会话建立后，后续的 StpUtil.getPermissionList()、StpUtil.hasRole()
     * 等调用将通过 StpInterfaceImpl（带 Caffeine L1 缓存）获取权限数据
     */
    private void syncSaTokenSession(Long userId, String username) {
        try {
            if (!StpUtil.isLogin()) {
                StpUtil.login(userId);
                logger.debug("[Auth] Sa-Token会话已建立: userId={}", userId);
            }

            // 将 username 存入 Sa-Token Session，供 SecurityContext 使用
            SaSession session = StpUtil.getSession();
            if (session.get("username") == null) {
                session.set("username", username);
            }
        } catch (Exception e) {
            logger.warn("[Auth] Sa-Token会话同步失败: userId={}, error={}", userId, e.getMessage());
        }
    }

    /**
     * 从数据库解析 userId（兜底逻辑，仅旧版 JWT Token 会走到此处）
     */
    private Long resolveUserId(String username, HttpServletRequest request) {
        try {
            UserMapper mapper = getUserMapper(request);
            if (mapper != null) {
                User user = mapper.selectByUsername(username);
                if (user != null) {
                    return user.getId();
                }
            }
        } catch (Exception e) {
            logger.warn("[Auth] 解析userId失败: {}", e.getMessage());
        }
        return null;
    }

    private UserMapper getUserMapper(HttpServletRequest request) {
        if (userMapper != null) return userMapper;
        WebApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(request.getServletContext());
        if (ctx != null) userMapper = ctx.getBean(UserMapper.class);
        return userMapper;
    }

    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"msg\":\"" + message + "\",\"data\":null,\"timestamp\":" + System.currentTimeMillis() + "}");
    }
}
