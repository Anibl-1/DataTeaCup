package com.dataplatform.data.mapper;

import com.dataplatform.data.entity.AlertRecord;
import org.apache.ibatis.annotations.*;
import java.util.Date;
import java.util.List;

/**
 * 告警记录 Mapper
 */
@Mapper
public interface AlertRecordMapper {

    @Insert("INSERT INTO sys_alert_record (rule_id, rule_name, metric_type, metric_name, metric_value, " +
            "threshold_value, alert_level, alert_message, alert_time, is_notified, notification_time) " +
            "VALUES (#{ruleId}, #{ruleName}, #{metricType}, #{metricName}, #{metricValue}, " +
            "#{thresholdValue}, #{alertLevel}, #{alertMessage}, #{alertTime}, #{isNotified}, #{notificationTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(AlertRecord record);

    @Select("SELECT * FROM sys_alert_record ORDER BY alert_time DESC LIMIT #{limit} OFFSET #{offset}")
    List<AlertRecord> findByPage(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM sys_alert_record")
    long countAll();

    @Select("SELECT * FROM sys_alert_record WHERE is_resolved=0 ORDER BY alert_time DESC")
    List<AlertRecord> findUnresolved();

    @Select("SELECT COUNT(*) FROM sys_alert_record WHERE is_resolved=0")
    long countUnresolved();

    @Select("SELECT * FROM sys_alert_record WHERE alert_level=#{level} AND is_resolved=0 ORDER BY alert_time DESC")
    List<AlertRecord> findByLevel(@Param("level") String level);

    @Update("UPDATE sys_alert_record SET is_resolved=1, resolve_time=#{resolveTime}, resolve_by=#{resolveBy}, " +
            "resolve_note=#{resolveNote} WHERE id=#{id}")
    void resolve(@Param("id") Long id, @Param("resolveTime") Date resolveTime,
                 @Param("resolveBy") Long resolveBy, @Param("resolveNote") String resolveNote);

    @Delete("DELETE FROM sys_alert_record WHERE create_time < #{before}")
    int deleteOlderThan(@Param("before") Date before);

    @Select("SELECT alert_level, COUNT(*) AS cnt FROM sys_alert_record WHERE is_resolved=0 GROUP BY alert_level")
    List<java.util.Map<String, Object>> countByLevel();
}
