package com.dataplatform.common.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解
 * 用于标记需要记录操作日志的方法
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    enum OperationType {
        CREATE, UPDATE, DELETE, QUERY, EXPORT, IMPORT, LOGIN, LOGOUT
    }

    String module();
    OperationType type();
    String description() default "";
    boolean saveParams() default true;
    boolean saveResult() default false;
}
