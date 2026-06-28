package com.dataplatform.data.mapper;

import com.dataplatform.data.service.share.ShareAccessLog;
import com.dataplatform.data.service.share.ShareLink;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ShareMapper {

    @Insert("INSERT INTO sys_share_link (id, token, resource_type, resource_id, resource_name, created_by, " +
            "access_type, password, expire_at, max_access_count, access_count, watermark_enabled, embeddable, active, created_at) " +
            "VALUES (#{id}, #{token}, #{resourceType}, #{resourceId}, #{resourceName}, #{createdBy}, " +
            "#{accessType}, #{password}, #{expireAt}, #{maxAccessCount}, #{accessCount}, #{watermarkEnabled}, #{embeddable}, #{active}, #{createdAt})")
    int insertShare(ShareLink link);

    @Select("SELECT * FROM sys_share_link WHERE id = #{id}")
    ShareLink selectById(@Param("id") String id);

    @Select("SELECT * FROM sys_share_link WHERE token = #{token}")
    ShareLink selectByToken(@Param("token") String token);

    @Select("SELECT * FROM sys_share_link WHERE created_by = #{userId} ORDER BY created_at DESC")
    List<ShareLink> selectByUser(@Param("userId") String userId);

    @Update("UPDATE sys_share_link SET active = #{active} WHERE id = #{id}")
    int updateActive(@Param("id") String id, @Param("active") boolean active);

    @Update("UPDATE sys_share_link SET access_count = access_count + 1 WHERE id = #{id}")
    int incrementAccessCount(@Param("id") String id);

    // ==================== 访问日志 ====================

    @Insert("INSERT INTO sys_share_access_log (id, share_id, access_ip, user_agent, success, fail_reason, access_time) " +
            "VALUES (#{id}, #{shareId}, #{accessIp}, #{userAgent}, #{success}, #{failReason}, #{accessTime})")
    int insertAccessLog(ShareAccessLog log);

    @Select("SELECT * FROM sys_share_access_log WHERE share_id = #{shareId} ORDER BY access_time DESC LIMIT #{limit}")
    List<ShareAccessLog> selectAccessLogs(@Param("shareId") String shareId, @Param("limit") int limit);
}
