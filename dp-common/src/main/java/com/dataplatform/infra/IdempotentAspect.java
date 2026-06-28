package com.dataplatform.infra;

import com.dataplatform.common.annotation.Idempotent;
import com.dataplatform.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * 幂等性切面
 * 拦截标注了 @Idempotent 的方法，防止重复提交
 */
@Slf4j
@Aspect
@Component
public class IdempotentAspect {

    private static final String IDEMPOTENT_PREFIX = "idempotent:";

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final ExpressionParser parser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        String idempotentKey = buildKey(joinPoint, idempotent);
        String redisKey = IDEMPOTENT_PREFIX + idempotentKey;

        Boolean success = redisTemplate.opsForValue()
            .setIfAbsent(redisKey, "1", idempotent.expireTime(), TimeUnit.SECONDS);

        if (Boolean.FALSE.equals(success)) {
            log.warn("重复提交被拦截: key={}", redisKey);
            return Result.error(idempotent.message());
        }

        try {
            return joinPoint.proceed();
        } catch (Throwable e) {
            redisTemplate.delete(redisKey);
            throw e;
        }
    }

    private String buildKey(ProceedingJoinPoint joinPoint, Idempotent idempotent) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String className = method.getDeclaringClass().getSimpleName();
        String methodName = method.getName();

        String keyExpression = idempotent.key();
        if (keyExpression != null && !keyExpression.isEmpty()) {
            return className + ":" + methodName + ":" + parseSpEL(keyExpression, joinPoint);
        }

        String params = Arrays.toString(joinPoint.getArgs());
        String userId = getCurrentUserId();
        return className + ":" + methodName + ":" + userId + ":" + md5(params);
    }

    private String parseSpEL(String expression, ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String[] paramNames = nameDiscoverer.getParameterNames(method);
        Object[] args = joinPoint.getArgs();

        EvaluationContext context = new StandardEvaluationContext();
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }

        try {
            Object value = parser.parseExpression(expression).getValue(context);
            return value != null ? value.toString() : "";
        } catch (Exception e) {
            log.warn("SpEL表达式解析失败: {}", expression, e);
            return md5(Arrays.toString(args));
        }
    }

    private String getCurrentUserId() {
        try {
            ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String token = request.getHeader("Authorization");
                return token != null ? md5(token) : "anonymous";
            }
        } catch (Exception e) {
            // ignore
        }
        return "anonymous";
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(input.hashCode());
        }
    }
}
