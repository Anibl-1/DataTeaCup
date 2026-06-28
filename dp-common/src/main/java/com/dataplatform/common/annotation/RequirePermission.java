package com.dataplatform.common.annotation;

import java.lang.annotation.*;

/**
 * 权限校验注解
 * 标记在 Controller 类或方法上，用于后端接口级别的权限控制
 * 
 * <p>使用方式：</p>
 * <ul>
 *   <li>方法级别: {@code @RequirePermission("user:manage")} — 仅该方法生效</li>
 *   <li>类级别: {@code @RequirePermission("data:source")} — 类下所有方法生效</li>
 *   <li>方法级覆盖类级: 方法上的注解优先级高于类上的注解</li>
 *   <li>多权限: {@code @RequirePermission(value={"a","b"}, logical=Logical.AND)} — 要求同时拥有</li>
 * </ul>
 * 
 * <p>底层委托 Sa-Token 鉴权引擎，权限数据通过 Redis 缓存，避免每次查库。</p>
 * 
 * @author dataplatform
 * @see com.dataplatform.common.annotation.RequireRole
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {

    /**
     * 逻辑组合方式
     */
    enum Logical {
        /** 需要满足所有权限 */
        AND,
        /** 满足任一权限即可（默认） */
        OR
    }

    /**
     * 需要校验的权限编码列表
     */
    String[] value();

    /**
     * 多权限的逻辑组合方式，默认 OR
     */
    Logical logical() default Logical.OR;
}
