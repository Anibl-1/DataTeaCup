package com.dataplatform.data.mapper;

import com.dataplatform.data.service.comment.Annotation;
import com.dataplatform.data.service.comment.Comment;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CommentMapper {

    @Insert("INSERT INTO sys_comment (id, resource_type, resource_id, parent_id, user_id, content, mentions, resolved, created_at, updated_at) " +
            "VALUES (#{id}, #{resourceType}, #{resourceId}, #{parentId}, #{userId}, #{content}, #{mentionsJson}, #{resolved}, #{createdAt}, #{updatedAt})")
    int insertComment(Comment comment);

    @Select("SELECT * FROM sys_comment WHERE id = #{id}")
    Comment selectById(@Param("id") String id);

    @Select("SELECT * FROM sys_comment WHERE resource_type = #{resourceType} AND resource_id = #{resourceId} ORDER BY created_at ASC")
    List<Comment> selectByResource(@Param("resourceType") String resourceType, @Param("resourceId") String resourceId);

    @Select("SELECT * FROM sys_comment WHERE parent_id = #{parentId} ORDER BY created_at ASC")
    List<Comment> selectReplies(@Param("parentId") String parentId);

    @Update("UPDATE sys_comment SET content = #{content}, mentions = #{mentionsJson}, updated_at = #{updatedAt} WHERE id = #{id}")
    int updateComment(Comment comment);

    @Update("UPDATE sys_comment SET resolved = 1 WHERE id = #{id}")
    int resolveComment(@Param("id") String id);

    @Delete("DELETE FROM sys_comment WHERE id = #{id}")
    int deleteComment(@Param("id") String id);

    // ==================== 标注 ====================

    @Insert("INSERT INTO sys_annotation (id, chart_id, user_id, text, x, y, color, type, created_at) " +
            "VALUES (#{id}, #{chartId}, #{userId}, #{text}, #{x}, #{y}, #{color}, #{type}, #{createdAt})")
    int insertAnnotation(Annotation annotation);

    @Select("SELECT * FROM sys_annotation WHERE chart_id = #{chartId}")
    List<Annotation> selectAnnotationsByChart(@Param("chartId") String chartId);

    @Delete("DELETE FROM sys_annotation WHERE id = #{id}")
    int deleteAnnotation(@Param("id") String id);
}
