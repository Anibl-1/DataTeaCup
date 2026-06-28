package com.dataplatform.common.aspect;

import com.dataplatform.common.annotation.OperationLog;
import com.dataplatform.common.service.OperationLogProvider;
import com.dataplatform.common.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 操作日志AOP切面
 * 支持捕获操作前后的数据快照，序列化为 JSON 存储
 */
@Aspect
@Component
public class OperationLogAspect {

    @Autowired(required = false)
    private OperationLogProvider operationLogProvider;

    @Autowired(required = false)
    private JwtUtil jwtUtil;

    private ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 模块名称映射：将中文模块名映射到领域模块标识
     */
    private static final Map<String, String> MODULE_MAPPING = new HashMap<>();
    static {
        // system 模块
        MODULE_MAPPING.put("用户管理", "system");
        MODULE_MAPPING.put("角色管理", "system");
        MODULE_MAPPING.put("菜单管理", "system");
        MODULE_MAPPING.put("系统配置", "system");
        MODULE_MAPPING.put("数据字典", "system");
        MODULE_MAPPING.put("认证管理", "system");
        // org 模块
        MODULE_MAPPING.put("部门管理", "org");
        MODULE_MAPPING.put("岗位管理", "org");
        // data 模块
        MODULE_MAPPING.put("图表管理", "data");
        MODULE_MAPPING.put("数据源管理", "data");
        MODULE_MAPPING.put("仪表盘管理", "data");
        MODULE_MAPPING.put("报表管理", "data");
        MODULE_MAPPING.put("数据管道", "data");
        MODULE_MAPPING.put("数据质量", "data");
        MODULE_MAPPING.put("数据同步", "data");
    }

    @Around("@annotation(com.dataplatform.common.annotation.OperationLog)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 获取注解
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        OperationLog annotation = method.getAnnotation(OperationLog.class);

        // 获取请求信息
        HttpServletRequest request = getRequest();

        // 构建日志信息
        String moduleName = annotation.module();
        String operationType = annotation.type().name();

        // 读取前端传递的当前菜单名称，拼入操作描述
        String menuName = null;
        if (request != null) {
            String rawMenuName = request.getHeader("X-Menu-Name");
            if (rawMenuName != null && !rawMenuName.isEmpty()) {
                try {
                    menuName = java.net.URLDecoder.decode(rawMenuName, "UTF-8");
                } catch (Exception e) {
                    menuName = rawMenuName;
                }
            }
        }
        String operationDesc;
        if (menuName != null && !menuName.isEmpty() && !menuName.equals(annotation.module())) {
            operationDesc = "\u3010" + menuName + "\u3011" + annotation.description();
        } else {
            operationDesc = annotation.description();
        }

        String requestMethodStr = null;
        String requestUrl = null;
        String ipAddress = null;
        String username = null;

        if (request != null) {
            requestMethodStr = request.getMethod();
            requestUrl = request.getRequestURI();
            ipAddress = getIpAddress(request);

            // 获取用户信息
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ") && jwtUtil != null) {
                try {
                    token = token.substring(7);
                    username = jwtUtil.getUsernameFromToken(token);
                } catch (Exception e) {
                    // 忽略token解析错误
                }
            }
        }

        // 记录请求参数
        String requestParams = null;
        if (annotation.saveParams()) {
            try {
                Object[] args = point.getArgs();
                if (args != null && args.length > 0) {
                    requestParams = objectMapper.writeValueAsString(args);
                    requestParams = desensitize(requestParams);
                }
            } catch (Exception e) {
                requestParams = "参数序列化失败";
            }
        }

        // 捕获操作前数据快照（@Before 阶段）
        String beforeData = captureBeforeData(annotation, point);

        Object result = null;
        boolean success = true;
        String responseResult = null;
        String afterData = null;
        try {
            // 执行方法
            result = point.proceed();

            // 记录响应结果
            if (annotation.saveResult() && result != null) {
                try {
                    responseResult = objectMapper.writeValueAsString(result);
                    if (responseResult.length() > 1000) {
                        responseResult = responseResult.substring(0, 1000) + "...";
                    }
                } catch (Exception e) {
                    responseResult = "结果序列化失败";
                }
            }

            // 捕获操作后数据快照（@AfterReturning 阶段）
            afterData = captureAfterData(annotation, result);

        } catch (Throwable e) {
            success = false;
            throw e;
        } finally {
            // 记录耗时
            long duration = System.currentTimeMillis() - startTime;

            // 异步保存日志（含变更前后数据）
            if (operationLogProvider != null) {
                operationLogProvider.saveLogAsync(moduleName, operationType, operationDesc,
                        signature.getDeclaringTypeName() + "." + method.getName(),
                        requestUrl, requestMethodStr, requestParams, responseResult,
                        username, ipAddress, duration, success,
                        beforeData, afterData);
            }
        }

        return result;
    }

    /**
     * 捕获操作前数据快照
     * 对于 UPDATE 和 DELETE 操作，序列化方法参数作为变更前数据
     */
    private String captureBeforeData(OperationLog annotation, ProceedingJoinPoint point) {
        OperationLog.OperationType type = annotation.type();
        if (type != OperationLog.OperationType.UPDATE
                && type != OperationLog.OperationType.DELETE
                && type != OperationLog.OperationType.CREATE) {
            return null;
        }
        try {
            Object[] args = point.getArgs();
            if (args == null || args.length == 0) {
                return null;
            }
            // 过滤掉 HttpServletRequest/HttpServletResponse 等不可序列化的参数
            Object[] serializableArgs = filterSerializableArgs(args);
            if (serializableArgs.length == 0) {
                return null;
            }
            String json = objectMapper.writeValueAsString(
                    serializableArgs.length == 1 ? serializableArgs[0] : serializableArgs);
            return truncate(desensitize(json), 2000);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 捕获操作后数据快照
     * 序列化方法返回值作为变更后数据
     */
    private String captureAfterData(OperationLog annotation, Object result) {
        OperationLog.OperationType type = annotation.type();
        if (type != OperationLog.OperationType.UPDATE
                && type != OperationLog.OperationType.DELETE
                && type != OperationLog.OperationType.CREATE) {
            return null;
        }
        if (result == null) {
            return null;
        }
        try {
            String json = objectMapper.writeValueAsString(result);
            return truncate(desensitize(json), 2000);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 过滤可序列化的参数，排除 HttpServletRequest/Response 等
     */
    private Object[] filterSerializableArgs(Object[] args) {
        return java.util.Arrays.stream(args)
                .filter(arg -> arg != null)
                .filter(arg -> !(arg instanceof jakarta.servlet.ServletRequest))
                .filter(arg -> !(arg instanceof jakarta.servlet.ServletResponse))
                .filter(arg -> !(arg instanceof org.springframework.web.multipart.MultipartFile))
                .toArray();
    }

    /**
     * 截断字符串到指定长度
     */
    private String truncate(String str, int maxLength) {
        if (str == null || str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength) + "...";
    }

    /**
     * 获取HttpServletRequest
     */
    private HttpServletRequest getRequest() {
        try {
            ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取IP地址
     */
    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    /**
     * 敏感信息脱敏
     */
    private String desensitize(String content) {
        if (content == null) {
            return null;
        }
        // 密码脱敏
        content = content.replaceAll("\"password\"\\s*:\\s*\"[^\"]*\"", "\"password\":\"******\"");
        // 手机号脱敏
        content = content.replaceAll("\"phone\"\\s*:\\s*\"(\\d{3})\\d{4}(\\d{4})\"", "\"phone\":\"$1****$2\"");
        // 身份证脱敏
        content = content.replaceAll("\"idCard\"\\s*:\\s*\"(\\d{6})\\d{8}(\\d{4})\"", "\"idCard\":\"$1********$2\"");
        return content;
    }
}
