package com.dataplatform.system.mapper;

import com.dataplatform.system.entity.DictType;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 字典类型Mapper
 */
@Mapper
public interface DictTypeMapper {

    @Select("SELECT * FROM sys_dict_type ORDER BY id")
    List<DictType> selectList();

    @Select("SELECT * FROM sys_dict_type WHERE id = #{id}")
    DictType selectById(@Param("id") Long id);

    @Select("SELECT * FROM sys_dict_type WHERE dict_code = #{dictCode}")
    DictType selectByDictCode(@Param("dictCode") String dictCode);

    @Insert("INSERT INTO sys_dict_type (dict_code, dict_name, status, remark, create_time, update_time) " +
            "VALUES (#{dictCode}, #{dictName}, #{status}, #{remark}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DictType dictType);

    @Update("UPDATE sys_dict_type SET dict_name = #{dictName}, status = #{status}, " +
            "remark = #{remark}, update_time = #{updateTime} WHERE id = #{id}")
    int update(DictType dictType);

    @Delete("DELETE FROM sys_dict_type WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
}
