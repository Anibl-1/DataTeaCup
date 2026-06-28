package com.dataplatform.common.annotation;

import java.lang.annotation.*;

/**
 * 角色校验注解
 * 标记在 Controller 类或方法上，用于后端接口级别的角色控制
 * 
 * <p>使用方式：</p>
 * <ul>
 *   <li>单角色: {@code @RequireRole("admin")} — 要求 admin 角色</li>
 *   <li>多角色: {@code @RequireRole(value={"admin","manager"}, logical=Logical.OR)} — 任一角色即可</li>
 *   <li>支持类级别和方法级别，方法级优先</li>
 * </ul>
 * 
 * <p>底层委托 Sa-Token 鉴权引擎。</p>
 * 
 * @author dataplatform
 * @see com.dataplatform.common.annotation.RequirePermission
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {

    /**
     * 逻辑组合方式
     */
    enum Logical {
        /** 需要满足所有角色 */
        AND,
        /** 满足任一角色即可（默认） */
        OR
    }

    /**
     * 需要校验的角色编码列表
     */
    String[] value();

    /**
     * 多角色的逻辑组合方式，默认 OR
     */
    Logical logical() default Logical.OR;
}
