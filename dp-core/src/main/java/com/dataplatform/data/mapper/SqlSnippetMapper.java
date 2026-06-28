package com.dataplatform.data.mapper;

import com.dataplatform.data.entity.SqlSnippet;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface SqlSnippetMapper {

    @Insert("INSERT INTO sql_snippet (name, sql_content, description, db_type) " +
            "VALUES (#{name}, #{sqlContent}, #{description}, #{dbType})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SqlSnippet snippet);

    @Update("UPDATE sql_snippet SET name = #{name}, sql_content = #{sqlContent}, " +
            "description = #{description}, db_type = #{dbType} WHERE id = #{id}")
    int update(SqlSnippet snippet);

    @Delete("DELETE FROM sql_snippet WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    @Select("SELECT * FROM sql_snippet WHERE id = #{id}")
    SqlSnippet selectById(@Param("id") Long id);

    @Select("<script>" +
            "SELECT * FROM sql_snippet " +
            "<where>" +
            "  <if test='keyword != null and keyword != \"\"'>AND (name LIKE CONCAT('%', #{keyword}, '%') OR sql_content LIKE CONCAT('%', #{keyword}, '%'))</if>" +
            "  <if test='dbType != null and dbType != \"\"'>AND db_type = #{dbType}</if>" +
            "</where>" +
            "ORDER BY update_time DESC LIMIT #{offset}, #{pageSize}" +
            "</script>")
    List<SqlSnippet> selectList(@Param("keyword") String keyword,
                                @Param("dbType") String dbType,
                                @Param("offset") int offset,
                                @Param("pageSize") int pageSize);

    @Select("<script>" +
            "SELECT COUNT(*) FROM sql_snippet " +
            "<where>" +
            "  <if test='keyword != null and keyword != \"\"'>AND (name LIKE CONCAT('%', #{keyword}, '%') OR sql_content LIKE CONCAT('%', #{keyword}, '%'))</if>" +
            "  <if test='dbType != null and dbType != \"\"'>AND db_type = #{dbType}</if>" +
            "</where>" +
            "</script>")
    long count(@Param("keyword") String keyword, @Param("dbType") String dbType);
}
