package com.dataplatform.infra.security;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 权限认证配置类（专业级）
 * 
 * <p>三层防御架构：</p>
 * <ol>
 *   <li><b>路由级</b>: 本类 — 登录校验 + 关键路由权限校验</li>
 *   <li><b>方法级</b>: {@code @RequirePermission} / {@code @RequireRole} — AOP切面校验</li>
 *   <li><b>资源级</b>: {@code @RequireResourcePermission} — 细粒度资源权限校验</li>
 * </ol>
 * 
 * <p>排除列表与 {@code AuthInterceptor} / {@code WebConfig} 保持同步。</p>
 *
 * @author dataplatform
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(SaTokenConfig.class);

    /** 公开路径（无需登录） */
    private static final String[] PUBLIC_PATHS = {
        "/auth/**",
        "/public/**",
        "/static/**",
        "/favicon.ico",
        "/error",
        "/swagger-ui.html",
        "/swagger-ui/**",
        "/v3/api-docs",
        "/v3/api-docs/**",
        "/swagger-resources/**",
        "/webjars/**",
        "/health",
        "/health/**",
        "/druid/**",
        "/ws",
        "/ws/**",
        "/chart-definition/*/embed",
        "/file/download/**",
        "/diag/**",
        "/actuator/**"
    };

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {

            // ========================= 第一层：登录校验 =========================
            SaRouter.match("/**")
                .notMatch(PUBLIC_PATHS)
                .check(r -> StpUtil.checkLogin());

            // ========================= 第二层：路由级角色/权限校验 =========================
            // 用户管理 API — 仅 admin 角色（/user/list 允许普通用户读取用户列表用于聊天等）
            SaRouter.match("/user/**")
                .notMatch("/user/current", "/user/profile", "/user/list")
                .check(r -> StpUtil.checkRole("admin"));

            // 角色管理 API — 仅 admin 角色
            SaRouter.match("/role/**")
                .check(r -> StpUtil.checkRole("admin"));

            // 菜单管理 API — 仅 admin 角色（读取类接口对普通用户开放）
            SaRouter.match("/menu/**")
                .notMatch("/menu/tree", "/menu/list", "/menu/visible")
                .check(r -> StpUtil.checkRole("admin"));

            // 系统配置写操作由 Controller 方法级 @RequirePermission("system:config") 控制
            // 读操作对所有登录用户开放

        })).addPathPatterns("/**").order(0);

        log.info("[SaTokenConfig] 三层权限防御已启用: 路由级 → 方法级 → 资源级");
    }
}
