package com.dataplatform.system.mapper;

import com.dataplatform.system.entity.DictData;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 字典数据Mapper
 */
@Mapper
public interface DictDataMapper {

    @Select("SELECT * FROM sys_dict_data WHERE dict_code = #{dictCode} ORDER BY sort_order ASC")
    List<DictData> selectByDictCode(@Param("dictCode") String dictCode);

    @Select("SELECT * FROM sys_dict_data WHERE dict_code = #{dictCode} AND status = 1 ORDER BY sort_order ASC")
    List<DictData> selectEnabledByDictCode(@Param("dictCode") String dictCode);

    @Select("SELECT * FROM sys_dict_data WHERE id = #{id}")
    DictData selectById(@Param("id") Long id);

    @Insert("INSERT INTO sys_dict_data (dict_code, label, value, sort_order, css_class, status, remark, create_time, update_time) " +
            "VALUES (#{dictCode}, #{label}, #{value}, #{sortOrder}, #{cssClass}, #{status}, #{remark}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DictData dictData);

    @Update("UPDATE sys_dict_data SET label = #{label}, value = #{value}, sort_order = #{sortOrder}, " +
            "css_class = #{cssClass}, status = #{status}, remark = #{remark}, update_time = #{updateTime} WHERE id = #{id}")
    int update(DictData dictData);

    @Delete("DELETE FROM sys_dict_data WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    @Delete("DELETE FROM sys_dict_data WHERE dict_code = #{dictCode}")
    int deleteByDictCode(@Param("dictCode") String dictCode);
}
