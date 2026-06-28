package com.dataplatform.data.entity;

import lombok.Data;
import java.util.Date;

/**
 * 报表定时推送实体
 * 对应表 sys_report_schedule
 */
@Data
public class ReportSchedule {
    private Long id;
    /** 关联报表定义ID */
    private Long reportId;
    /** 推送名称 */
    private String scheduleName;
    /** Cron表达式 */
    private String cronExpression;
    /** 收件人列表(逗号分隔邮箱) */
    private String recipients;
    /** 推送渠道: email/wecom/dingtalk */
    private String channels;
    /** 邮件通道配置ID（使用消息通道管理中的配置） */
    private Long emailChannelId;
    /** 企业微信通道配置ID */
    private Long wecomChannelId;
    /** 钉钉通道配置ID */
    private Long dingtalkChannelId;
    /** 是否附带Excel附件 */
    private Integer attachExcel;
    /** 附件格式: excel/pdf */
    private String attachFormat;
    /** 报表过滤参数JSON */
    private String filterParams;
    /** 日期函数参数JSON，如 {"start_date":"$yesterday","end_date":"$today"} */
    private String dateParams;
    /** 是否启用 */
    private Integer isEnabled;
    /** 上次执行时间 */
    private Date lastRunTime;
    /** 上次执行状态: success/failed */
    private String lastRunStatus;
    /** 创建人 */
    private Long createBy;
    private Date createTime;
    private Date updateTime;
}
