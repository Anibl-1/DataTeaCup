package com.dataplatform.data.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据质量报告实体
 * 
 * @author dataplatform
 */
@Data
@TableName("data_quality_report")
public class DataQualityReport {
    
    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 规则ID */
    private Long ruleId;
    
    /** 数据源ID */
    private Long dataSourceId;
    
    /** 表名 */
    private String tableName;
    
    /** 质量评分（0-100） */
    private Integer score;
    
    /** 检测详情JSON */
    private String detailJson;
    
    /** 检测时间 */
    private LocalDateTime createTime;
}
