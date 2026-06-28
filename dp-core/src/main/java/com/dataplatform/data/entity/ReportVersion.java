package com.dataplatform.data.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 报表版本实体
 * 
 * @author dataplatform
 */
@Data
@TableName("report_version")
public class ReportVersion {
    
    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 报表ID */
    private Long reportId;
    
    /** 版本号 */
    private Integer versionNo;
    
    /** 配置快照JSON */
    private String configSnapshot;
    
    /** SQL快照 */
    private String sqlSnapshot;
    
    /** 修改摘要 */
    private String summary;
    
    /** 创建人 */
    private Long createBy;
    
    /** 创建时间 */
    private LocalDateTime createTime;
}
