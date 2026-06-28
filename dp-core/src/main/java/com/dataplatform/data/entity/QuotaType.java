package com.dataplatform.data.entity;

/**
 * 配额类型枚举
 * 
 * 需求 6.1-6.4: 定义各类配额类型
 *
 * @author dataplatform
 */
public enum QuotaType {

    /** 用户数量 */
    USER_COUNT("USER_COUNT", "用户数量", "个"),
    /** 数据源数量 */
    DATASOURCE_COUNT("DATASOURCE_COUNT", "数据源数量", "个"),
    /** 仪表盘数量 */
    DASHBOARD_COUNT("DASHBOARD_COUNT", "仪表盘数量", "个"),
    /** 报表数量 */
    REPORT_COUNT("REPORT_COUNT", "报表数量", "个"),
    /** 存储空间 */
    STORAGE_SIZE("STORAGE_SIZE", "存储空间", "MB"),
    /** API调用次数（每日） */
    API_CALLS_DAILY("API_CALLS_DAILY", "每日API调用次数", "次"),
    /** 查询并发数 */
    QUERY_CONCURRENCY("QUERY_CONCURRENCY", "查询并发数", "个"),
    /** 导出次数（每日） */
    EXPORT_DAILY("EXPORT_DAILY", "每日导出次数", "次"),
    /** 数据行数上限 */
    DATA_ROWS("DATA_ROWS", "数据行数上限", "行");

    private final String code;
    private final String desc;
    private final String unit;

    QuotaType(String code, String desc, String unit) {
        this.code = code;
        this.desc = desc;
        this.unit = unit;
    }

    public String getCode() { return code; }
    public String getDesc() { return desc; }
    public String getUnit() { return unit; }

    public static QuotaType fromCode(String code) {
        for (QuotaType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
