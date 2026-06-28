package com.dataplatform.data.entity;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * SQL 执行计划
 * 封装 EXPLAIN 结果的完整解析
 */
@Data
public class ExecutionPlan {
    /** 原始 SQL */
    private String sql;
    /** 执行计划节点列表 */
    private List<PlanNode> nodes = new ArrayList<>();
    /** 预估总扫描行数 */
    private long estimatedRows;
    /** 预估总代价 */
    private long estimatedCost;
    /** 警告信息 */
    private List<String> warnings = new ArrayList<>();

    /**
     * 是否包含全表扫描
     */
    public boolean hasFullTableScan() {
        return nodes.stream().anyMatch(n -> "ALL".equalsIgnoreCase(n.getType()));
    }

    /**
     * 是否使用了索引
     */
    public boolean usesIndex() {
        return nodes.stream().anyMatch(n -> n.getKey() != null && !n.getKey().isEmpty());
    }
}
