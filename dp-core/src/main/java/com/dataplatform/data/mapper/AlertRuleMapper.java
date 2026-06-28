package com.dataplatform.data.mapper;

import com.dataplatform.data.entity.AlertRule;
import org.apache.ibatis.annotations.*;
import java.util.List;

/**
 * 告警规则 Mapper
 */
@Mapper
public interface AlertRuleMapper {

    @Insert("INSERT INTO sys_alert_rule (rule_name, rule_code, metric_type, metric_name, threshold_type, " +
            "threshold_value, duration_seconds, alert_level, alert_message, notification_channels, " +
            "notification_users, is_enabled, create_by) VALUES (#{ruleName}, #{ruleCode}, #{metricType}, " +
            "#{metricName}, #{thresholdType}, #{thresholdValue}, #{durationSeconds}, #{alertLevel}, " +
            "#{alertMessage}, #{notificationChannels}, #{notificationUsers}, #{isEnabled}, #{createBy})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(AlertRule rule);

    @Update("UPDATE sys_alert_rule SET rule_name=#{ruleName}, metric_type=#{metricType}, metric_name=#{metricName}, " +
            "threshold_type=#{thresholdType}, threshold_value=#{thresholdValue}, duration_seconds=#{durationSeconds}, " +
            "alert_level=#{alertLevel}, alert_message=#{alertMessage}, notification_channels=#{notificationChannels}, " +
            "notification_users=#{notificationUsers}, is_enabled=#{isEnabled} WHERE id=#{id}")
    void update(AlertRule rule);

    @Delete("DELETE FROM sys_alert_rule WHERE id=#{id}")
    void deleteById(@Param("id") Long id);

    @Select("SELECT * FROM sys_alert_rule WHERE id=#{id}")
    AlertRule findById(@Param("id") Long id);

    @Select("SELECT * FROM sys_alert_rule ORDER BY create_time DESC")
    List<AlertRule> findAll();

    @Select("SELECT * FROM sys_alert_rule WHERE is_enabled=1 ORDER BY create_time DESC")
    List<AlertRule> findEnabled();

    @Select("SELECT * FROM sys_alert_rule WHERE metric_type=#{metricType} AND is_enabled=1")
    List<AlertRule> findByMetricType(@Param("metricType") String metricType);

    @Update("UPDATE sys_alert_rule SET is_enabled=#{enabled} WHERE id=#{id}")
    void toggleEnabled(@Param("id") Long id, @Param("enabled") Integer enabled);
}
