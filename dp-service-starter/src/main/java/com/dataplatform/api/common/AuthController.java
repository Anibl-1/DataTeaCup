package com.dataplatform.api.common;

import cn.dev33.satoken.stp.StpUtil;
import com.dataplatform.common.Result;
import com.dataplatform.common.annotation.OperationLog;
import com.dataplatform.common.annotation.RateLimit;
import com.dataplatform.common.captcha.CaptchaService;
import com.dataplatform.common.constants.Constants;
import com.dataplatform.common.exception.BusinessException;
import com.dataplatform.common.exception.ErrorCode;
import com.dataplatform.common.security.SecurityContext;
import com.dataplatform.common.util.JwtUtil;
import com.dataplatform.data.dto.LoginDTO;
import com.dataplatform.system.service.IUserService;
import com.dataplatform.system.service.LoginLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 * 处理用户登录、获取用户信息等认证相关接口
 * 
 * @author dataplatform
 */
@Tag(name = "认证管理", description = "用户登录、登出、获取用户信息等接口")
@RestController
@RequestMapping("/auth")
public class AuthController {
    @Autowired
    private IUserService userService;

    @Autowired
    private LoginLogService loginLogService;

    @Autowired
    private CaptchaService captchaService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 用户登录
     * 
     * @param loginDTO 登录信息（用户名、密码）
     * @return 包含token和用户信息的结果
     */
    @Operation(summary = "用户登录", description = "使用用户名和密码登录系统，返回JWT Token")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "登录成功"),
        @ApiResponse(responseCode = "400", description = "用户名或密码错误"),
        @ApiResponse(responseCode = "429", description = "登录请求过于频繁")
    })
    @PostMapping("/login")
    @RateLimit(limit = 10, period = 60, message = "登录请求过于频繁，请稍后再试")
    @OperationLog(module = "认证管理", type = OperationLog.OperationType.LOGIN, description = "用户登录", saveParams = false)
    public Result<Map<String, Object>> login(
            @Parameter(description = "登录信息", required = true)
            @Validated @RequestBody LoginDTO loginDTO,
            HttpServletRequest request) {
        // 验证码校验（启用时）
        if (captchaService.isEnabled()) {
            if (loginDTO.getCaptchaKey() == null || loginDTO.getCaptchaCode() == null
                    || loginDTO.getCaptchaKey().isBlank() || loginDTO.getCaptchaCode().isBlank()) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "请输入验证码");
            }
            if (!captchaService.verify(loginDTO.getCaptchaKey(), loginDTO.getCaptchaCode())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "验证码错误或已过期");
            }
        }

        String ip = getClientIp(request);
        String ua = request.getHeader("User-Agent");
        try {
            Map<String, Object> result = userService.login(loginDTO.getUsername(), loginDTO.getPassword());
            loginLogService.recordLogin(loginDTO.getUsername(), ip, ua, "success", "登录成功");
            return Result.success(result);
        } catch (Exception e) {
            loginLogService.recordLogin(loginDTO.getUsername(), ip, ua, "failure", e.getMessage());
            throw e;
        }
    }

    /**
     * 获取验证码
     *
     * @return captchaKey + base64图片
     */
    @Operation(summary = "获取验证码", description = "生成图形验证码，返回 captchaKey 和 base64 图片")
    @GetMapping("/captcha")
    @RateLimit(limit = 30, period = 60, message = "验证码请求过于频繁")
    public Result<Map<String, Object>> getCaptcha() {
        if (!captchaService.isEnabled()) {
            Map<String, Object> result = new HashMap<>();
            result.put("enabled", false);
            return Result.success(result);
        }
        CaptchaService.CaptchaResult captcha = captchaService.generate();
        Map<String, Object> result = new HashMap<>();
        result.put("enabled", true);
        result.put("captchaKey", captcha.captchaKey());
        result.put("captchaImage", captcha.captchaImage());
        return Result.success(result);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 用户登出
     * 清除 Sa-Token 会话，使后续权限校验立即失效
     */
    @Operation(summary = "用户登出", description = "登出当前账号，清除服务端会话")
    @ApiResponse(responseCode = "200", description = "登出成功")
    @PostMapping("/logout")
    @OperationLog(module = "认证管理", type = OperationLog.OperationType.LOGOUT, description = "用户登出")
    public Result<Void> logout(HttpServletRequest request) {
        String ip = getClientIp(request);
        String ua = request.getHeader("User-Agent");
        String username = null;
        try {
            // /auth/** 被 AuthInterceptor 排除，所以 Sa-Token 会话在此请求中可能没有建立
            // 从 JWT Token 手动提取 userId 并明确登出
            String authorization = request.getHeader("Authorization");
            if (authorization != null && authorization.startsWith(Constants.TOKEN_PREFIX)) {
                String token = authorization.substring(Constants.TOKEN_PREFIX.length());
                if (jwtUtil.validateToken(token)) {
                    username = jwtUtil.getUsernameFromToken(token);
                    Long userId = jwtUtil.getUserIdFromToken(token);
                    if (userId != null) {
                        // 明确按 userId 登出，无论当前请求是否建立了 Sa-Token 会话
                        StpUtil.logout(userId);
                    }
                }
            } else if (StpUtil.isLogin()) {
                // 如果已有活跃会话，直接登出
                username = SecurityContext.getCurrentUsername();
                StpUtil.logout();
            }
            if (username != null) {
                loginLogService.recordLogin(username, ip, ua, "logout", "正常登出");
            }
        } catch (Exception e) {
            // 即使出错也不影响前端，让前端清除本地 token
        }
        return Result.success();
    }

    /**
     * 获取当前用户信息（通过 Sa-Token 会话，无需重复解析 JWT）
     */
    @Operation(summary = "获取用户信息", description = "获取当前登录用户的详细信息，包括权限列表")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "401", description = "未授权或会话已过期")
    })
    @GetMapping("/userInfo")
    public Result<Map<String, Object>> getUserInfo() {
        String username = SecurityContext.getCurrentUsername();
        if (username == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录或会话已过期");
        }
        Map<String, Object> result = userService.getUserInfo(username);
        return Result.success(result);
    }

    /**
     * 修改密码（通过 Sa-Token 会话获取当前用户，无需传 Token 参数）
     */
    @Operation(summary = "修改密码", description = "用户修改自己的登录密码")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "修改成功"),
        @ApiResponse(responseCode = "400", description = "原密码错误"),
        @ApiResponse(responseCode = "401", description = "未授权")
    })
    @PostMapping("/changePassword")
    @OperationLog(module = "认证管理", type = OperationLog.OperationType.UPDATE, description = "修改密码", saveParams = false)
    public Result<Void> changePassword(
            @Parameter(description = "修改密码信息", required = true)
            @Validated @RequestBody com.dataplatform.data.dto.ChangePasswordDTO dto) {
        String username = SecurityContext.getCurrentUsername();
        if (username == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录或会话已过期");
        }
        userService.changePassword(username, dto.getOldPassword(), dto.getNewPassword());
        return Result.success();
    }
}
