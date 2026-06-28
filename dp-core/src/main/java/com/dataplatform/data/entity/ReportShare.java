package com.dataplatform.data.entity;

import lombok.Data;
import java.util.Date;

/**
 * 报表分享实体
 */
@Data
public class ReportShare {
    /** 分享ID */
    private Long id;
    
    /** 报表ID */
    private Long reportId;
    
    /** 分享Token（唯一标识） */
    private String shareToken;
    
    /** 分享类型：report-报表分享，chart-图表分享 */
    private String shareType;
    
    /** 是否启用密码保护 */
    private Boolean passwordProtected;
    
    /** 访问密码（可选） */
    private String accessPassword;
    
    /** 过期时间（null表示永不过期） */
    private Date expireTime;
    
    /** 最大访问次数（0表示不限制） */
    private Integer maxAccessCount;
    
    /** 已访问次数 */
    private Integer accessCount;
    
    /** 状态：1-启用，0-禁用 */
    private Integer status;
    
    /** 创建人ID */
    private Long createBy;
    
    /** 创建时间 */
    private Date createTime;
}
