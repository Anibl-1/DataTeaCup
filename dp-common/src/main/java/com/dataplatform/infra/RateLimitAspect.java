package com.dataplatform.infra;

import com.dataplatform.common.annotation.RateLimit;
import com.dataplatform.common.exception.BusinessException;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 接口限流切面
 * 使用滑动窗口计数实现限流
 */
@Aspect
@Component
public class RateLimitAspect {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitAspect.class);

    private final LoadingCache<String, AtomicInteger> requestCountCache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build(new CacheLoader<String, AtomicInteger>() {
                @Override
                public AtomicInteger load(String key) {
                    return new AtomicInteger(0);
                }
            });

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint point, RateLimit rateLimit) throws Throwable {
        String key = generateKey(point, rateLimit);

        try {
            AtomicInteger count = requestCountCache.get(key);
            int currentCount = count.incrementAndGet();

            if (currentCount > rateLimit.limit()) {
                logger.warn("接口限流触发: key={}, count={}, limit={}/{}",
                    key, currentCount, rateLimit.limit(), rateLimit.period());
                throw new BusinessException(429, rateLimit.message());
            }

            return point.proceed();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.error("限流检查异常", e);
            return point.proceed();
        }
    }

    private String generateKey(ProceedingJoinPoint point, RateLimit rateLimit) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        String methodName = method.getDeclaringClass().getName() + "." + method.getName();

        StringBuilder key = new StringBuilder(rateLimit.prefix());
        key.append(methodName);

        switch (rateLimit.limitType()) {
            case IP:
                key.append(":").append(getClientIp());
                break;
            case USER:
                key.append(":").append(getCurrentUserId());
                break;
            case GLOBAL:
                break;
        }

        return key.toString();
    }

    private String getClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) return "unknown";
        HttpServletRequest request = attributes.getRequest();
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

    private String getCurrentUserId() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) return "anonymous";
        HttpServletRequest request = attributes.getRequest();
        Object userId = request.getAttribute("userId");
        return userId != null ? userId.toString() : "anonymous";
    }
}
