package com.dataplatform.data.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 仪表盘分享实体
 * 
 * @author dataplatform
 */
@Data
@TableName("dashboard_share")
public class DashboardShare {
    
    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 仪表盘ID */
    private Long dashboardId;
    
    /** 分享token */
    private String shareToken;
    
    /** 过期时间（null表示永不过期） */
    private LocalDateTime expireTime;
    
    /** 创建人 */
    private Long createBy;
    
    /** 创建时间 */
    private LocalDateTime createTime;
    
    /** 状态: 1-有效 0-已撤销 */
    private Integer status;
}
