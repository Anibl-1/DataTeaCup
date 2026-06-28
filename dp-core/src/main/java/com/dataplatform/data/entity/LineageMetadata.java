package com.dataplatform.data.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 血缘元数据实体
 * 
 * @author dataplatform
 */
@Data
@TableName("lineage_metadata")
public class LineageMetadata {
    
    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 源数据源ID */
    private Long sourceDsId;
    
    /** 源表名 */
    private String sourceTable;
    
    /** 目标数据源ID */
    private Long targetDsId;
    
    /** 目标表名 */
    private String targetTable;
    
    /** 转换类型（etl/collect/sql） */
    private String transformType;
    
    /** 关联的任务ID */
    private Long transformId;
    
    /** 创建时间 */
    private LocalDateTime createTime;
    
    /** 更新时间 */
    private LocalDateTime updateTime;
}
