package com.dataplatform.data.mapper;

import org.apache.ibatis.annotations.*;

import java.util.Map;

/**
 * AI配置Mapper
 */
@Mapper
public interface AiConfigMapper {

    @Select("SELECT config_key, config_value FROM sys_ai_config")
    @MapKey("config_key")
    Map<String, Map<String, Object>> getAllConfigs();

    @Select("SELECT config_value FROM sys_ai_config WHERE config_key = #{key}")
    String getConfigValue(@Param("key") String key);

    @Update("UPDATE sys_ai_config SET config_value = #{value} WHERE config_key = #{key}")
    int updateConfig(@Param("key") String key, @Param("value") String value);

    @Insert("INSERT INTO sys_ai_config (config_key, config_value, description) VALUES (#{key}, #{value}, #{description}) " +
            "ON DUPLICATE KEY UPDATE config_value = #{value}")
    int upsertConfig(@Param("key") String key, @Param("value") String value, @Param("description") String description);
}
