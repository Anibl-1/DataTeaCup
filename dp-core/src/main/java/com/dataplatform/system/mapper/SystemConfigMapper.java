package com.dataplatform.system.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dataplatform.system.entity.SystemConfig;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 系统配置Mapper
 */
@Mapper
public interface SystemConfigMapper {

    @Select("<script>" +
            "SELECT * FROM sys_config " +
            "<where>" +
            "  <if test='keyword != null and keyword != \"\"'>" +
            "    AND (config_key LIKE CONCAT('%', #{keyword}, '%') OR config_desc LIKE CONCAT('%', #{keyword}, '%'))" +
            "  </if>" +
            "</where>" +
            " ORDER BY id" +
            "</script>")
    IPage<SystemConfig> selectPage(Page<SystemConfig> page, @Param("keyword") String keyword);

    @Select("<script>" +
            "SELECT * FROM sys_config " +
            "<where>" +
            "  <if test='keyword != null and keyword != \"\"'>" +
            "    AND (config_key LIKE CONCAT('%', #{keyword}, '%') OR config_desc LIKE CONCAT('%', #{keyword}, '%'))" +
            "  </if>" +
            "</where>" +
            " ORDER BY id" +
            "</script>")
    List<SystemConfig> selectList(@Param("keyword") String keyword);

    @Select("SELECT * FROM sys_config WHERE config_key = #{configKey}")
    SystemConfig selectByKey(@Param("configKey") String configKey);

    @Select("SELECT * FROM sys_config WHERE id = #{id}")
    SystemConfig selectById(@Param("id") Long id);

    @Select("SELECT DISTINCT config_group FROM sys_config ORDER BY config_group")
    List<String> selectConfigGroups();

    @Select("<script>" +
            "SELECT * FROM sys_config " +
            "<where>" +
            "  <if test='configGroup != null and configGroup != \"\"'>" +
            "    AND config_group = #{configGroup}" +
            "  </if>" +
            "</where>" +
            " ORDER BY id" +
            "</script>")
    List<SystemConfig> selectByGroup(@Param("configGroup") String configGroup);

    @Insert("INSERT INTO sys_config (config_key, config_value, config_type, config_desc, config_group, is_system, create_time, update_time) " +
            "VALUES (#{configKey}, #{configValue}, #{configType}, #{configDesc}, #{configGroup}, #{isSystem}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SystemConfig config);

    @Update("<script>" +
            "UPDATE sys_config " +
            "<set>" +
            "  <if test='configValue != null'>config_value = #{configValue},</if>" +
            "  <if test='configType != null'>config_type = #{configType},</if>" +
            "  <if test='configGroup != null'>config_group = #{configGroup},</if>" +
            "  config_desc = #{configDesc}," +
            "  update_time = #{updateTime}" +
            "</set>" +
            "WHERE id = #{id}" +
            "</script>")
    int update(SystemConfig config);

    @Delete("DELETE FROM sys_config WHERE id = #{id} AND is_system = false")
    int deleteById(@Param("id") Long id);
}
