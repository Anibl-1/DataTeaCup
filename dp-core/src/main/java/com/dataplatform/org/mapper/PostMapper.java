package com.dataplatform.org.mapper;

import com.dataplatform.org.entity.Post;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 岗位Mapper
 */
@Mapper
public interface PostMapper {

    @Select("SELECT * FROM sys_post ORDER BY sort_order ASC, id ASC")
    List<Post> selectList();

    @Select("<script>" +
            "SELECT * FROM sys_post WHERE 1=1" +
            "<if test='postName != null and postName != \"\"'>" +
            "  AND post_name LIKE CONCAT('%', #{postName}, '%')" +
            "</if>" +
            "<if test='status != null'> AND status = #{status}</if>" +
            " ORDER BY sort_order ASC, id ASC" +
            "</script>")
    List<Post> selectByCondition(@Param("postName") String postName, @Param("status") Integer status);

    @Select("SELECT * FROM sys_post WHERE id = #{id}")
    Post selectById(@Param("id") Long id);

    @Select("SELECT * FROM sys_post WHERE post_code = #{postCode}")
    Post selectByPostCode(@Param("postCode") String postCode);

    @Insert("INSERT INTO sys_post (post_code, post_name, sort_order, status, remark, create_time, update_time) " +
            "VALUES (#{postCode}, #{postName}, #{sortOrder}, #{status}, #{remark}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Post post);

    @Update("UPDATE sys_post SET post_name = #{postName}, sort_order = #{sortOrder}, " +
            "status = #{status}, remark = #{remark}, update_time = #{updateTime} WHERE id = #{id}")
    int update(Post post);

    @Delete("DELETE FROM sys_post WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
}
