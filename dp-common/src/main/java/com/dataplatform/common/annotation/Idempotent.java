package com.dataplatform.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 幂等性注解
 * 标注在Controller方法上，防止重复提交
 * 基于Redis实现幂等键的存储和过期
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {
    String key() default "";
    long expireTime() default 300;
    String message() default "请勿重复提交";
}
