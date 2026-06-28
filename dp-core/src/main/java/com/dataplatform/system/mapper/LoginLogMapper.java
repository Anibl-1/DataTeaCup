package com.dataplatform.system.mapper;

import com.dataplatform.system.entity.LoginLog;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 登录日志Mapper
 */
@Mapper
public interface LoginLogMapper {

    @Insert("INSERT INTO sys_login_log (username, ip_address, user_agent, browser, os, status, message, login_time) " +
            "VALUES (#{username}, #{ipAddress}, #{userAgent}, #{browser}, #{os}, #{status}, #{message}, #{loginTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(LoginLog log);

    @Select("<script>" +
            "SELECT * FROM sys_login_log WHERE 1=1 " +
            "<if test='username != null and username != \"\"'> AND username LIKE CONCAT('%', #{username}, '%') </if>" +
            "<if test='status != null and status != \"\"'> AND status = #{status} </if>" +
            "<if test='ipAddress != null and ipAddress != \"\"'> AND ip_address LIKE CONCAT('%', #{ipAddress}, '%') </if>" +
            "ORDER BY login_time DESC LIMIT #{offset}, #{pageSize}" +
            "</script>")
    List<LoginLog> selectPage(@Param("username") String username,
                              @Param("status") String status,
                              @Param("ipAddress") String ipAddress,
                              @Param("offset") int offset,
                              @Param("pageSize") int pageSize);

    @Select("<script>" +
            "SELECT COUNT(*) FROM sys_login_log WHERE 1=1 " +
            "<if test='username != null and username != \"\"'> AND username LIKE CONCAT('%', #{username}, '%') </if>" +
            "<if test='status != null and status != \"\"'> AND status = #{status} </if>" +
            "<if test='ipAddress != null and ipAddress != \"\"'> AND ip_address LIKE CONCAT('%', #{ipAddress}, '%') </if>" +
            "</script>")
    long count(@Param("username") String username,
               @Param("status") String status,
               @Param("ipAddress") String ipAddress);

    @Delete("DELETE FROM sys_login_log WHERE login_time < #{beforeTime}")
    int deleteBeforeTime(@Param("beforeTime") java.time.LocalDateTime beforeTime);
}
