package com.dataplatform.data.mapper;

import com.dataplatform.data.service.subscription.PushLog;
import com.dataplatform.data.service.subscription.ReportSubscription;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface SubscriptionMapper {

    @Insert("INSERT INTO sys_report_subscription (id, user_id, resource_type, resource_id, resource_name, " +
            "cron_expression, push_channel, recipients, format, `condition`, active, created_at, last_push_at) " +
            "VALUES (#{id}, #{userId}, #{resourceType}, #{resourceId}, #{resourceName}, " +
            "#{cronExpression}, #{pushChannel}, #{recipientsJson}, #{format}, #{condition}, #{active}, #{createdAt}, #{lastPushAt})")
    int insertSubscription(ReportSubscription sub);

    @Select("SELECT * FROM sys_report_subscription WHERE id = #{id}")
    ReportSubscription selectById(@Param("id") String id);

    @Select("SELECT * FROM sys_report_subscription WHERE user_id = #{userId} AND active = 1 ORDER BY created_at DESC")
    List<ReportSubscription> selectByUser(@Param("userId") String userId);

    @Select("SELECT * FROM sys_report_subscription WHERE active = 1")
    List<ReportSubscription> selectActive();

    @Update("UPDATE sys_report_subscription SET active = 0 WHERE id = #{id}")
    int deactivate(@Param("id") String id);

    @Update("UPDATE sys_report_subscription SET last_push_at = #{lastPushAt} WHERE id = #{id}")
    int updateLastPushAt(@Param("id") String id, @Param("lastPushAt") java.time.LocalDateTime lastPushAt);

    // ==================== 推送日志 ====================

    @Insert("INSERT INTO sys_push_log (id, subscription_id, status, channel, error_message, push_time) " +
            "VALUES (#{id}, #{subscriptionId}, #{status}, #{channel}, #{errorMessage}, #{pushTime})")
    int insertPushLog(PushLog log);

    @Select("SELECT * FROM sys_push_log WHERE subscription_id = #{subscriptionId} ORDER BY push_time DESC LIMIT #{limit}")
    List<PushLog> selectPushLogs(@Param("subscriptionId") String subscriptionId, @Param("limit") int limit);
}
