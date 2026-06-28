package com.dataplatform.common.annotation;

import java.lang.annotation.*;

/**
 * 资源级权限校验注解
 * 标记在Controller方法上，用于细粒度的资源级权限控制
 * 
 * @author dataplatform
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireResourcePermission {
    String resourceType();
    String permission();
    String resourceIdParam() default "id";
    boolean allowAdmin() default true;
    String message() default "";
}
