package com.dataplatform.data.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dataplatform.data.entity.DataDictionary;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据字典Mapper
 */
@Mapper
public interface DataDictionaryMapper {

    @Select("<script>" +
            "SELECT * FROM data_dictionary " +
            "<where>" +
            "  <if test='dictType != null and dictType != \"\"'>" +
            "    AND dict_type = #{dictType}" +
            "  </if>" +
            "  <if test='keyword != null and keyword != \"\"'>" +
            "    AND (dict_label LIKE CONCAT('%', #{keyword}, '%') OR dict_code LIKE CONCAT('%', #{keyword}, '%'))" +
            "  </if>" +
            "  <if test='status != null'>" +
            "    AND status = #{status}" +
            "  </if>" +
            "</where>" +
            " ORDER BY dict_type, sort_order" +
            "</script>")
    IPage<DataDictionary> selectPage(Page<DataDictionary> page,
                                     @Param("dictType") String dictType,
                                     @Param("keyword") String keyword,
                                     @Param("status") Integer status);

    @Select("<script>" +
            "SELECT * FROM data_dictionary " +
            "<where>" +
            "  <if test='dictType != null and dictType != \"\"'>" +
            "    AND dict_type = #{dictType}" +
            "  </if>" +
            "  <if test='keyword != null and keyword != \"\"'>" +
            "    AND (dict_label LIKE CONCAT('%', #{keyword}, '%') OR dict_code LIKE CONCAT('%', #{keyword}, '%'))" +
            "  </if>" +
            "  <if test='status != null'>" +
            "    AND status = #{status}" +
            "  </if>" +
            "</where>" +
            " ORDER BY dict_type, sort_order" +
            "</script>")
    List<DataDictionary> selectList(@Param("dictType") String dictType,
                                   @Param("keyword") String keyword,
                                   @Param("status") Integer status);

    /** 按字典类型查询启用的项（前端下拉用） */
    @Select("SELECT * FROM data_dictionary WHERE dict_type = #{dictType} AND status = 1 ORDER BY sort_order")
    List<DataDictionary> selectByType(@Param("dictType") String dictType);

    /** 查询所有不重复的字典类型 */
    @Select("SELECT DISTINCT dict_type FROM data_dictionary ORDER BY dict_type")
    List<String> selectDistinctTypes();

    @Select("SELECT * FROM data_dictionary WHERE id = #{id}")
    DataDictionary selectById(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM data_dictionary WHERE dict_type = #{dictType} AND dict_code = #{dictCode}")
    int countByTypeAndCode(@Param("dictType") String dictType, @Param("dictCode") String dictCode);

    @Insert("INSERT INTO data_dictionary (dict_type, dict_code, dict_label, dict_value, sort_order, is_default, status, remark, create_time, update_time) " +
            "VALUES (#{dictType}, #{dictCode}, #{dictLabel}, #{dictValue}, #{sortOrder}, #{isDefault}, #{status}, #{remark}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DataDictionary dict);

    @Update("<script>" +
            "UPDATE data_dictionary " +
            "<set>" +
            "  <if test='dictType != null'>dict_type = #{dictType},</if>" +
            "  <if test='dictCode != null'>dict_code = #{dictCode},</if>" +
            "  <if test='dictLabel != null'>dict_label = #{dictLabel},</if>" +
            "  <if test='dictValue != null'>dict_value = #{dictValue},</if>" +
            "  <if test='sortOrder != null'>sort_order = #{sortOrder},</if>" +
            "  <if test='isDefault != null'>is_default = #{isDefault},</if>" +
            "  <if test='status != null'>status = #{status},</if>" +
            "  remark = #{remark}," +
            "  update_time = #{updateTime}" +
            "</set>" +
            "WHERE id = #{id}" +
            "</script>")
    int update(DataDictionary dict);

    @Delete("DELETE FROM data_dictionary WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    @Delete("<script>" +
            "DELETE FROM data_dictionary WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "  #{id}" +
            "</foreach>" +
            "</script>")
    int batchDelete(@Param("ids") List<Long> ids);
}
