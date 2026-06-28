package com.dataplatform.data.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 公告实体
 */
@Data
public class Announcement {
    
    private Long id;
    
    /** 公告标题 */
    private String title;
    
    /** 公告内容 */
    private String content;
    
    /** 公告类型: info/success/warning/error */
    private String type;
    
    /** 优先级: 1-低 2-中 3-高 */
    private Integer priority;
    
    /** 状态: 0-禁用 1-启用 */
    private Integer status;
    
    /** 是否置顶: 0-否 1-是 */
    private Integer isTop;

    /** 发布范围: all-全员 dept-部门 role-角色 */
    private String targetType;

    /** 目标ID列表(JSON数组，部门或角色ID) */
    private String targetIds;

    /** 已读人数 */
    private Integer readCount;

    /** 附件JSON */
    private String attachments;
    
    /** 开始时间 */
    private LocalDateTime startTime;
    
    /** 结束时间 */
    private LocalDateTime endTime;
    
    /** 创建人 */
    private String createBy;
    
    /** 创建时间 */
    private LocalDateTime createTime;
    
    /** 更新时间 */
    private LocalDateTime updateTime;
}
