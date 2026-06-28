package com.dataplatform.data.mapper;

import com.dataplatform.data.entity.ReportShare;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 报表分享Mapper
 */
@Mapper
public interface ReportShareMapper {

    @Insert("INSERT INTO report_share (report_id, share_token, share_type, password_protected, " +
            "access_password, expire_time, max_access_count, access_count, status, create_by, create_time) " +
            "VALUES (#{reportId}, #{shareToken}, #{shareType}, #{passwordProtected}, " +
            "#{accessPassword}, #{expireTime}, #{maxAccessCount}, #{accessCount}, #{status}, #{createBy}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ReportShare share);

    @Select("SELECT * FROM report_share WHERE share_token = #{shareToken}")
    ReportShare selectByToken(@Param("shareToken") String shareToken);

    @Select("SELECT * FROM report_share WHERE report_id = #{reportId} AND share_type = #{shareType} " +
            "ORDER BY create_time DESC")
    List<ReportShare> selectByReportId(@Param("reportId") Long reportId, 
                                       @Param("shareType") String shareType);

    @Update("UPDATE report_share SET access_count = access_count + 1 WHERE id = #{id}")
    int incrementAccessCount(@Param("id") Long id);

    @Update("UPDATE report_share SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    @Delete("DELETE FROM report_share WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
}
