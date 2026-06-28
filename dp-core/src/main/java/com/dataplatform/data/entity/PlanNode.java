package com.dataplatform.data.entity;

import lombok.Data;

/**
 * EXPLAIN 执行计划中的单个节点
 * 对应 MySQL EXPLAIN 输出的一行
 */
@Data
public class PlanNode {
    /** 查询中 SELECT 的标识符 */
    private Integer id;
    /** 查询类型：SIMPLE, PRIMARY, SUBQUERY, DERIVED, UNION 等 */
    private String selectType;
    /** 访问的表名 */
    private String table;
    /** 分区信息 */
    private String partitions;
    /** 访问类型：ALL, index, range, ref, eq_ref, const, system 等 */
    private String type;
    /** 可能使用的索引 */
    private String possibleKeys;
    /** 实际使用的索引 */
    private String key;
    /** 使用的索引长度 */
    private String keyLen;
    /** 与索引比较的列 */
    private String ref;
    /** 预估扫描行数 */
    private Long rows;
    /** 按表条件过滤后的行百分比 */
    private Double filtered;
    /** 额外信息 */
    private String extra;
}
