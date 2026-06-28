package com.dataplatform.data.vo;

import java.util.*;

/**
 * 血缘分析结果VO
 */
public class LineageAnalysisVO {
    
    /**
     * 全链路追溯结果
     */
    public static class FullChainResult {
        private String tableName;
        private List<ChainNode> upstreamChain = new ArrayList<>();   // 上游链路
        private List<ChainNode> downstreamChain = new ArrayList<>(); // 下游链路
        private int totalUpstreamLayers;    // 上游层数
        private int totalDownstreamLayers;  // 下游层数
        private int totalNodes;             // 总节点数
        private List<String> criticalPath;  // 关键路径
        
        // Getters and Setters
        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }
        public List<ChainNode> getUpstreamChain() { return upstreamChain; }
        public void setUpstreamChain(List<ChainNode> upstreamChain) { this.upstreamChain = upstreamChain; }
        public List<ChainNode> getDownstreamChain() { return downstreamChain; }
        public void setDownstreamChain(List<ChainNode> downstreamChain) { this.downstreamChain = downstreamChain; }
        public int getTotalUpstreamLayers() { return totalUpstreamLayers; }
        public void setTotalUpstreamLayers(int totalUpstreamLayers) { this.totalUpstreamLayers = totalUpstreamLayers; }
        public int getTotalDownstreamLayers() { return totalDownstreamLayers; }
        public void setTotalDownstreamLayers(int totalDownstreamLayers) { this.totalDownstreamLayers = totalDownstreamLayers; }
        public int getTotalNodes() { return totalNodes; }
        public void setTotalNodes(int totalNodes) { this.totalNodes = totalNodes; }
        public List<String> getCriticalPath() { return criticalPath; }
        public void setCriticalPath(List<String> criticalPath) { this.criticalPath = criticalPath; }
    }
    
    /**
     * 链路节点
     */
    public static class ChainNode {
        private String tableName;
        private String tableType;       // table, view, report
        private int layer;              // 层级（距离中心表的距离）
        private String lineageType;     // etl, sql, collect, report
        private String transformLogic;  // 转换逻辑
        private List<String> columns;   // 涉及的字段
        private String parentTable;     // 父节点表名
        
        public ChainNode() {}
        
        public ChainNode(String tableName, int layer, String lineageType) {
            this.tableName = tableName;
            this.layer = layer;
            this.lineageType = lineageType;
        }
        
        // Getters and Setters
        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }
        public String getTableType() { return tableType; }
        public void setTableType(String tableType) { this.tableType = tableType; }
        public int getLayer() { return layer; }
        public void setLayer(int layer) { this.layer = layer; }
        public String getLineageType() { return lineageType; }
        public void setLineageType(String lineageType) { this.lineageType = lineageType; }
        public String getTransformLogic() { return transformLogic; }
        public void setTransformLogic(String transformLogic) { this.transformLogic = transformLogic; }
        public List<String> getColumns() { return columns; }
        public void setColumns(List<String> columns) { this.columns = columns; }
        public String getParentTable() { return parentTable; }
        public void setParentTable(String parentTable) { this.parentTable = parentTable; }
    }
    
    /**
     * 热点表分析结果
     */
    public static class HotspotAnalysis {
        private List<TableHotspot> hotspots = new ArrayList<>();
        private int totalTables;
        private double averageDependencies;
        
        // Getters and Setters
        public List<TableHotspot> getHotspots() { return hotspots; }
        public void setHotspots(List<TableHotspot> hotspots) { this.hotspots = hotspots; }
        public int getTotalTables() { return totalTables; }
        public void setTotalTables(int totalTables) { this.totalTables = totalTables; }
        public double getAverageDependencies() { return averageDependencies; }
        public void setAverageDependencies(double averageDependencies) { this.averageDependencies = averageDependencies; }
    }
    
    /**
     * 表热度信息
     */
    public static class TableHotspot {
        private String tableName;
        private int upstreamCount;      // 上游依赖数
        private int downstreamCount;    // 下游被依赖数
        private int totalConnections;   // 总连接数
        private double hotScore;        // 热度分数
        private String riskLevel;       // 风险等级: high, medium, low
        private String suggestion;      // 建议
        
        // Getters and Setters
        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }
        public int getUpstreamCount() { return upstreamCount; }
        public void setUpstreamCount(int upstreamCount) { this.upstreamCount = upstreamCount; }
        public int getDownstreamCount() { return downstreamCount; }
        public void setDownstreamCount(int downstreamCount) { this.downstreamCount = downstreamCount; }
        public int getTotalConnections() { return totalConnections; }
        public void setTotalConnections(int totalConnections) { this.totalConnections = totalConnections; }
        public double getHotScore() { return hotScore; }
        public void setHotScore(double hotScore) { this.hotScore = hotScore; }
        public String getRiskLevel() { return riskLevel; }
        public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
        public String getSuggestion() { return suggestion; }
        public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
    }
    
    /**
     * 孤岛检测结果
     */
    public static class OrphanAnalysis {
        private List<String> orphanTables = new ArrayList<>();     // 孤岛表（无血缘关系）
        private List<String> deadEndTables = new ArrayList<>();    // 终端表（只有上游无下游）
        private List<String> sourceTables = new ArrayList<>();     // 源头表（只有下游无上游）
        private List<List<String>> isolatedClusters = new ArrayList<>(); // 孤立的血缘簇
        private int totalOrphans;
        
        // Getters and Setters
        public List<String> getOrphanTables() { return orphanTables; }
        public void setOrphanTables(List<String> orphanTables) { this.orphanTables = orphanTables; }
        public List<String> getDeadEndTables() { return deadEndTables; }
        public void setDeadEndTables(List<String> deadEndTables) { this.deadEndTables = deadEndTables; }
        public List<String> getSourceTables() { return sourceTables; }
        public void setSourceTables(List<String> sourceTables) { this.sourceTables = sourceTables; }
        public List<List<String>> getIsolatedClusters() { return isolatedClusters; }
        public void setIsolatedClusters(List<List<String>> isolatedClusters) { this.isolatedClusters = isolatedClusters; }
        public int getTotalOrphans() { return totalOrphans; }
        public void setTotalOrphans(int totalOrphans) { this.totalOrphans = totalOrphans; }
    }
    
    /**
     * 血缘健康度报告
     */
    public static class HealthReport {
        private int totalLineages;
        private int totalTables;
        private double coverage;            // 覆盖率
        private int healthScore;            // 健康分数 0-100
        private String healthLevel;         // 健康等级
        private List<HealthIssue> issues = new ArrayList<>();
        private List<String> recommendations = new ArrayList<>();
        private Map<String, Integer> lineageTypeDistribution = new LinkedHashMap<>();
        
        // Getters and Setters
        public int getTotalLineages() { return totalLineages; }
        public void setTotalLineages(int totalLineages) { this.totalLineages = totalLineages; }
        public int getTotalTables() { return totalTables; }
        public void setTotalTables(int totalTables) { this.totalTables = totalTables; }
        public double getCoverage() { return coverage; }
        public void setCoverage(double coverage) { this.coverage = coverage; }
        public int getHealthScore() { return healthScore; }
        public void setHealthScore(int healthScore) { this.healthScore = healthScore; }
        public String getHealthLevel() { return healthLevel; }
        public void setHealthLevel(String healthLevel) { this.healthLevel = healthLevel; }
        public List<HealthIssue> getIssues() { return issues; }
        public void setIssues(List<HealthIssue> issues) { this.issues = issues; }
        public List<String> getRecommendations() { return recommendations; }
        public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
        public Map<String, Integer> getLineageTypeDistribution() { return lineageTypeDistribution; }
        public void setLineageTypeDistribution(Map<String, Integer> dist) { this.lineageTypeDistribution = dist; }
    }
    
    /**
     * 健康问题
     */
    public static class HealthIssue {
        private String type;        // orphan, circular, broken, stale
        private String severity;    // critical, warning, info
        private String description;
        private String tableName;
        private String suggestion;
        
        public HealthIssue() {}
        
        public HealthIssue(String type, String severity, String description) {
            this.type = type;
            this.severity = severity;
            this.description = description;
        }
        
        // Getters and Setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }
        public String getSuggestion() { return suggestion; }
        public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
    }
    
    /**
     * 列级血缘详情
     */
    public static class ColumnLineageDetail {
        private String sourceTable;
        private String sourceColumn;
        private String targetTable;
        private String targetColumn;
        private String transformType;   // direct, computed, aggregated
        private String expression;
        private List<String> intermediateSteps = new ArrayList<>();
        
        // Getters and Setters
        public String getSourceTable() { return sourceTable; }
        public void setSourceTable(String sourceTable) { this.sourceTable = sourceTable; }
        public String getSourceColumn() { return sourceColumn; }
        public void setSourceColumn(String sourceColumn) { this.sourceColumn = sourceColumn; }
        public String getTargetTable() { return targetTable; }
        public void setTargetTable(String targetTable) { this.targetTable = targetTable; }
        public String getTargetColumn() { return targetColumn; }
        public void setTargetColumn(String targetColumn) { this.targetColumn = targetColumn; }
        public String getTransformType() { return transformType; }
        public void setTransformType(String transformType) { this.transformType = transformType; }
        public String getExpression() { return expression; }
        public void setExpression(String expression) { this.expression = expression; }
        public List<String> getIntermediateSteps() { return intermediateSteps; }
        public void setIntermediateSteps(List<String> intermediateSteps) { this.intermediateSteps = intermediateSteps; }
    }
    
    /**
     * 血缘比较结果（用于检测变更）
     */
    public static class LineageComparison {
        private List<String> addedTables = new ArrayList<>();
        private List<String> removedTables = new ArrayList<>();
        private List<String> addedEdges = new ArrayList<>();
        private List<String> removedEdges = new ArrayList<>();
        private boolean hasChanges;
        private String summary;
        
        // Getters and Setters
        public List<String> getAddedTables() { return addedTables; }
        public void setAddedTables(List<String> addedTables) { this.addedTables = addedTables; }
        public List<String> getRemovedTables() { return removedTables; }
        public void setRemovedTables(List<String> removedTables) { this.removedTables = removedTables; }
        public List<String> getAddedEdges() { return addedEdges; }
        public void setAddedEdges(List<String> addedEdges) { this.addedEdges = addedEdges; }
        public List<String> getRemovedEdges() { return removedEdges; }
        public void setRemovedEdges(List<String> removedEdges) { this.removedEdges = removedEdges; }
        public boolean isHasChanges() { return hasChanges; }
        public void setHasChanges(boolean hasChanges) { this.hasChanges = hasChanges; }
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
    }
}
