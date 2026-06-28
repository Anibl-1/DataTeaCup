package com.dataplatform.common.config;

import com.dataplatform.common.util.LogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;

/**
 * 请求日志拦截器
 * 
 * @author dataplatform
 */
@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingInterceptor.class);
    private static final String START_TIME_ATTR = "requestStartTime";

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());

        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        String remoteAddr = getClientIp(request);

        String requestId = UUID.randomUUID().toString().substring(0, 8);

        MDC.put(LogUtil.MDC_REQUEST_ID, requestId);
        MDC.put(LogUtil.MDC_IP, remoteAddr);

        response.setHeader("X-Request-Id", requestId);

        if (logger.isDebugEnabled()) {
            logger.debug("[{}] >>> {} {}{} - IP: {}",
                requestId, method, uri,
                queryString != null ? "?" + queryString : "",
                remoteAddr);
        }

        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
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

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                @NonNull Object handler, @Nullable Exception ex) {
        try {
            Long startTime = (Long) request.getAttribute(START_TIME_ATTR);
            long duration = startTime != null ? System.currentTimeMillis() - startTime : 0;

            String requestId = MDC.get(LogUtil.MDC_REQUEST_ID);
            int status = response.getStatus();

            if (ex != null) {
                if (isClientAbortException(ex)) {
                    logger.debug("[{}] 客户端断开连接: {} {}", requestId, request.getMethod(), request.getRequestURI());
                    return;
                }
                logger.error("[{}] <<< {} {} - {} {}ms | Error: {}",
                    requestId, request.getMethod(), request.getRequestURI(),
                    status, duration, ex.getMessage());
            } else {
                if (duration > 3000) {
                    logger.warn("[{}] <<< {} {} - {} {}ms [SLOW]",
                        requestId, request.getMethod(), request.getRequestURI(), status, duration);
                } else if (logger.isDebugEnabled()) {
                    logger.debug("[{}] <<< {} {} - {} {}ms",
                        requestId, request.getMethod(), request.getRequestURI(), status, duration);
                }
            }
        } finally {
            LogUtil.clearMDC();
        }
    }

    private boolean isClientAbortException(Throwable e) {
        if (e == null) return false;
        String className = e.getClass().getName();
        if (className.contains("ClientAbortException") || className.contains("ClosedChannelException")) return true;
        String message = e.getMessage();
        if (message != null && (message.contains("Broken pipe") ||
            message.contains("Connection reset") ||
            message.contains("ClosedChannelException"))) return true;
        return isClientAbortException(e.getCause());
    }
}
