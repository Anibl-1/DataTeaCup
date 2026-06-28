package com.dataplatform.data.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 数据血缘关系实体
 */
@Data
public class DataLineage {

    private Long id;

    /** 源类型: table/view/report/chart */
    private String sourceType;

    /** 源对象ID */
    private Long sourceId;

    /** 源对象名称 */
    private String sourceName;

    /** 源数据库 */
    private String sourceDatabase;

    /** 源表名 */
    private String sourceTable;

    /** 源字段名 */
    private String sourceColumn;

    /** 目标类型: table/view/report/chart */
    private String targetType;

    /** 目标对象ID */
    private Long targetId;

    /** 目标对象名称 */
    private String targetName;

    /** 目标数据库 */
    private String targetDatabase;

    /** 目标表名 */
    private String targetTable;

    /** 目标字段名 */
    private String targetColumn;

    /** 血缘类型: direct/indirect */
    private String lineageType;

    /** 转换逻辑 */
    private String transformLogic;

    /** SQL内容 */
    private String sqlContent;

    /** 创建人 */
    private Long createBy;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
