package com.dataplatform.data.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dataplatform.data.entity.Announcement;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 公告Mapper
 */
@Mapper
public interface AnnouncementMapper {

    @Select("<script>" +
            "SELECT * FROM sys_announcement " +
            "<where>" +
            "  <if test='keyword != null and keyword != \"\"'>" +
            "    AND (title LIKE CONCAT('%', #{keyword}, '%') OR content LIKE CONCAT('%', #{keyword}, '%'))" +
            "  </if>" +
            "  <if test='status != null'>" +
            "    AND status = #{status}" +
            "  </if>" +
            "</where>" +
            " ORDER BY is_top DESC, priority DESC, create_time DESC" +
            "</script>")
    List<Announcement> selectList(@Param("keyword") String keyword, @Param("status") Integer status);

    @Select("<script>" +
            "SELECT * FROM sys_announcement " +
            "<where>" +
            "  <if test='keyword != null and keyword != \"\"'>" +
            "    AND (title LIKE CONCAT('%', #{keyword}, '%') OR content LIKE CONCAT('%', #{keyword}, '%'))" +
            "  </if>" +
            "  <if test='status != null'>" +
            "    AND status = #{status}" +
            "  </if>" +
            "</where>" +
            " ORDER BY is_top DESC, priority DESC, create_time DESC" +
            "</script>")
    IPage<Announcement> selectPage(Page<Announcement> page, @Param("keyword") String keyword, @Param("status") Integer status);

    @Select("SELECT * FROM sys_announcement " +
            "WHERE status = 1 " +
            "AND (start_time IS NULL OR start_time <= NOW()) " +
            "AND (end_time IS NULL OR end_time >= NOW()) " +
            "ORDER BY is_top DESC, priority DESC, create_time DESC")
    List<Announcement> selectActiveList();

    @Select("SELECT * FROM sys_announcement WHERE id = #{id}")
    Announcement selectById(@Param("id") Long id);

    @Insert("INSERT INTO sys_announcement (title, content, type, priority, status, is_top, " +
            "target_type, target_ids, read_count, attachments, " +
            "start_time, end_time, create_by, create_time, update_time) " +
            "VALUES (#{title}, #{content}, #{type}, #{priority}, #{status}, #{isTop}, " +
            "#{targetType}, #{targetIds}, #{readCount}, #{attachments}, " +
            "#{startTime}, #{endTime}, #{createBy}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Announcement announcement);

    @Update("<script>" +
            "UPDATE sys_announcement " +
            "<set>" +
            "  <if test='title != null'>title = #{title},</if>" +
            "  <if test='content != null'>content = #{content},</if>" +
            "  <if test='type != null'>type = #{type},</if>" +
            "  <if test='priority != null'>priority = #{priority},</if>" +
            "  <if test='status != null'>status = #{status},</if>" +
            "  <if test='isTop != null'>is_top = #{isTop},</if>" +
            "  <if test='targetType != null'>target_type = #{targetType},</if>" +
            "  <if test='targetIds != null'>target_ids = #{targetIds},</if>" +
            "  <if test='attachments != null'>attachments = #{attachments},</if>" +
            "  start_time = #{startTime}," +
            "  end_time = #{endTime}," +
            "  update_time = #{updateTime}" +
            "</set>" +
            "WHERE id = #{id}" +
            "</script>")
    int update(Announcement announcement);

    @Delete("DELETE FROM sys_announcement WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    @Delete("<script>" +
            "DELETE FROM sys_announcement WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "  #{id}" +
            "</foreach>" +
            "</script>")
    int batchDelete(@Param("ids") List<Long> ids);

    @Update("UPDATE sys_announcement SET status = #{status}, update_time = #{updateTime} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status, @Param("updateTime") LocalDateTime updateTime);

    /**
     * 统计公告总数
     */
    @Select("SELECT COUNT(*) FROM sys_announcement")
    long countTotal();

    /**
     * 统计启用的公告数
     */
    @Select("SELECT COUNT(*) FROM sys_announcement WHERE status = 1")
    long countEnabled();

    /**
     * 统计置顶的公告数
     */
    @Select("SELECT COUNT(*) FROM sys_announcement WHERE is_top = 1")
    long countTop();
}
