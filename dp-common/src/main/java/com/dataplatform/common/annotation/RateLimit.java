package com.dataplatform.common.annotation;

import java.lang.annotation.*;

/**
 * 接口限流注解
 * 使用方式: @RateLimit(limit = 10, period = 60) 表示60秒内最多10次请求
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    int limit() default 100;
    int period() default 60;
    String prefix() default "rate_limit:";
    LimitType limitType() default LimitType.IP;
    String message() default "请求过于频繁，请稍后再试";

    enum LimitType {
        IP, USER, GLOBAL
    }
}
