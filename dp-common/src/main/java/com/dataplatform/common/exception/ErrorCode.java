package com.dataplatform.common.exception;

/**
 * 错误码常量类
 * 统一管理系统中所有的错误码
 * 
 * @author dataplatform
 */
public class ErrorCode {
    // ==================== 通用错误码 ====================
    public static final int SUCCESS = 200;
    public static final int ERROR = 500;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int BAD_REQUEST = 400;

    // ==================== 业务错误码 - 用户相关 (1001-1099) ====================
    public static final int USER_NOT_FOUND = 1001;
    public static final int USER_PASSWORD_ERROR = 1002;
    public static final int USER_DISABLED = 1003;
    public static final int USER_ALREADY_EXISTS = 1004;
    public static final int INVALID_TOKEN = 1005;
    public static final int TOKEN_EXPIRED = 1006;
    public static final int PARAM_ERROR = 1007;
    public static final int RESOURCE_NOT_FOUND = 1008;
    public static final int RESOURCE_DISABLED = 1009;

    // ==================== 数据源相关 (1101-1199) ====================
    public static final int DATA_SOURCE_NOT_FOUND = 1101;
    public static final int DATA_SOURCE_CONNECTION_FAILED = 1102;
    public static final int DATA_SOURCE_UNSUPPORTED_TYPE = 1103;
    public static final int DATA_SOURCE_ALREADY_EXISTS = 1104;

    // ==================== 采集任务相关 (1201-1299) ====================
    public static final int COLLECT_TASK_NOT_FOUND = 1201;
    public static final int COLLECT_TASK_ALREADY_RUNNING = 1202;
    public static final int COLLECT_TASK_NAME_EMPTY = 1203;
    public static final int COLLECT_TASK_TABLE_NAME_EMPTY = 1204;
    public static final int COLLECT_TASK_DATA_SOURCE_EMPTY = 1205;

    // ==================== 流程相关 (1301-1399) ====================
    public static final int PIPELINE_NOT_FOUND = 1301;
    public static final int PIPELINE_EXECUTION_FAILED = 1302;
    public static final int PIPELINE_NODE_CONFIG_ERROR = 1303;
    public static final int PIPELINE_ALREADY_RUNNING = 1304;

    // ==================== DataX任务相关 (1401-1499) ====================
    public static final int DATAX_JOB_NOT_FOUND = 1401;
    public static final int DATAX_JOB_EXECUTION_FAILED = 1402;

    // ==================== 报表相关 (1501-1599) ====================
    public static final int REPORT_NOT_FOUND = 1501;
    public static final int REPORT_QUERY_FAILED = 1502;

    // ==================== 数据操作相关 (1601-1699) ====================
    public static final int DATA_OPERATION_FAILED = 1601;
    public static final int TABLE_NOT_FOUND = 1602;
    public static final int SQL_EXECUTION_FAILED = 1603;
    public static final int DATA_IMPORT_FAILED = 1604;
    public static final int DATA_EXPORT_FAILED = 1605;

    // ==================== 系统相关 (1701-1799) ====================
    public static final int SYSTEM_CONFIG_ERROR = 1701;
    public static final int NOTIFICATION_SEND_FAILED = 1702;
}
