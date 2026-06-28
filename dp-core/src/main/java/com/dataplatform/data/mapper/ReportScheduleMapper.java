package com.dataplatform.data.mapper;

import com.dataplatform.data.entity.ReportSchedule;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 报表定时推送Mapper
 */
@Mapper
public interface ReportScheduleMapper {

    @Select("SELECT * FROM sys_report_schedule ORDER BY create_time DESC")
    List<ReportSchedule> findAll();

    @Select("SELECT * FROM sys_report_schedule WHERE id = #{id}")
    ReportSchedule findById(@Param("id") Long id);

    @Select("SELECT * FROM sys_report_schedule WHERE report_id = #{reportId}")
    List<ReportSchedule> findByReportId(@Param("reportId") Long reportId);

    @Select("SELECT * FROM sys_report_schedule WHERE is_enabled = 1")
    List<ReportSchedule> findEnabled();

    @Insert("INSERT INTO sys_report_schedule (report_id, schedule_name, cron_expression, recipients, " +
            "channels, email_channel_id, wecom_channel_id, dingtalk_channel_id, attach_excel, attach_format, filter_params, date_params, is_enabled, create_by, create_time, update_time) VALUES " +
            "(#{reportId}, #{scheduleName}, #{cronExpression}, #{recipients}, " +
            "#{channels}, #{emailChannelId}, #{wecomChannelId}, #{dingtalkChannelId}, #{attachExcel}, #{attachFormat}, #{filterParams}, #{dateParams}, #{isEnabled}, #{createBy}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ReportSchedule schedule);

    @Update("UPDATE sys_report_schedule SET schedule_name=#{scheduleName}, cron_expression=#{cronExpression}, " +
            "recipients=#{recipients}, channels=#{channels}, email_channel_id=#{emailChannelId}, wecom_channel_id=#{wecomChannelId}, " +
            "dingtalk_channel_id=#{dingtalkChannelId}, attach_excel=#{attachExcel}, attach_format=#{attachFormat}, " +
            "filter_params=#{filterParams}, date_params=#{dateParams}, is_enabled=#{isEnabled}, update_time=NOW() WHERE id=#{id}")
    int update(ReportSchedule schedule);

    @Update("UPDATE sys_report_schedule SET last_run_time=NOW(), last_run_status=#{status} WHERE id=#{id}")
    int updateRunStatus(@Param("id") Long id, @Param("status") String status);

    @Delete("DELETE FROM sys_report_schedule WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
}
