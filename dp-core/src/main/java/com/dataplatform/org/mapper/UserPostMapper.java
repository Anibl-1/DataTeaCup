package com.dataplatform.org.mapper;

import com.dataplatform.org.entity.UserPost;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 用户岗位关联Mapper
 */
@Mapper
public interface UserPostMapper {

    @Select("SELECT * FROM sys_user_post WHERE post_id = #{postId}")
    List<UserPost> selectByPostId(@Param("postId") Long postId);

    @Select("SELECT * FROM sys_user_post WHERE user_id = #{userId}")
    List<UserPost> selectByUserId(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM sys_user_post WHERE post_id = #{postId}")
    int countByPostId(@Param("postId") Long postId);

    @Insert("INSERT INTO sys_user_post (user_id, post_id) VALUES (#{userId}, #{postId})")
    int insert(UserPost userPost);

    @Delete("DELETE FROM sys_user_post WHERE user_id = #{userId} AND post_id = #{postId}")
    int delete(@Param("userId") Long userId, @Param("postId") Long postId);

    @Delete("DELETE FROM sys_user_post WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);

    @Delete("DELETE FROM sys_user_post WHERE post_id = #{postId}")
    int deleteByPostId(@Param("postId") Long postId);
}
