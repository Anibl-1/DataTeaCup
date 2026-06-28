package com.dataplatform.common.constants;

/**
 * 系统常量类
 * 统一管理系统中使用的常量值
 * 
 * @author dataplatform
 */
public class Constants {
    // ==================== 用户状态 ====================
    public static final int USER_STATUS_ENABLED = 1;
    public static final int USER_STATUS_DISABLED = 0;

    // ==================== 任务状态 ====================
    public static final String TASK_STATUS_RUNNING = "running";
    public static final String TASK_STATUS_STOPPED = "stopped";
    public static final String TASK_STATUS_ERROR = "error";

    // ==================== 分页默认值 ====================
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int DEFAULT_PAGE = 1;

    // ==================== Token相关 ====================
    public static final String TOKEN_PREFIX = "Bearer ";

    // ==================== 系统用户 ====================
    public static final String ADMIN_USERNAME = "admin";

    // ==================== 通用状态 ====================
    public static final int STATUS_ENABLED = 1;
    public static final int STATUS_DISABLED = 0;
}
