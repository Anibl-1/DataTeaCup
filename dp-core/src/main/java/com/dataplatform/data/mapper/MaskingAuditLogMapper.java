package com.dataplatform.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dataplatform.data.entity.MaskingAuditLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 脱敏审计日志 Mapper 接口
 *
 * **Validates: Requirements 6.4**
 *
 * @author dataplatform
 */
@Mapper
public interface MaskingAuditLogMapper extends BaseMapper<MaskingAuditLog> {

    /**
     * 根据用户ID和时间范围查询审计日志
     */
    @Select("SELECT * FROM masking_audit_log WHERE user_id = #{userId} " +
            "AND create_time BETWEEN #{startTime} AND #{endTime} " +
            "ORDER BY create_time DESC")
    List<MaskingAuditLog> findByUserAndTimeRange(
            @Param("userId") Long userId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 根据数据源ID和时间范围查询审计日志
     */
    @Select("SELECT * FROM masking_audit_log WHERE data_source_id = #{dataSourceId} " +
            "AND create_time BETWEEN #{startTime} AND #{endTime} " +
            "ORDER BY create_time DESC")
    List<MaskingAuditLog> findByDataSourceAndTimeRange(
            @Param("dataSourceId") Long dataSourceId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}
