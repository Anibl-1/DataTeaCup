package com.dataplatform.data.service;

import com.dataplatform.data.entity.DataLineage;
import com.dataplatform.data.mapper.DataLineageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 数据血缘关系服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataLineageService {
    
    private final DataLineageMapper dataLineageMapper;
    private final JdbcTemplate jdbcTemplate;
    
    public DataLineage create(DataLineage lineage) {
        // 设置必填字段默认值，避免 NOT NULL 约束导致插入失败
        if (lineage.getSourceType() == null || lineage.getSourceType().isEmpty()) {
            lineage.setSourceType("table");
        }
        if (lineage.getSourceName() == null || lineage.getSourceName().isEmpty()) {
            // 使用 sourceTable 作为默认 sourceName
            lineage.setSourceName(lineage.getSourceTable() != null ? lineage.getSourceTable() : "unknown");
        }
        if (lineage.getTargetType() == null || lineage.getTargetType().isEmpty()) {
            lineage.setTargetType("table");
        }
        if (lineage.getTargetName() == null || lineage.getTargetName().isEmpty()) {
            // 使用 targetTable 作为默认 targetName
            lineage.setTargetName(lineage.getTargetTable() != null ? lineage.getTargetTable() : "unknown");
        }
        if (lineage.getLineageType() == null || lineage.getLineageType().isEmpty()) {
            lineage.setLineageType("manual");
        }
        if (lineage.getCreateTime() == null) {
            lineage.setCreateTime(LocalDateTime.now());
        }
        
        dataLineageMapper.insert(lineage);
        return lineage;
    }
    
    public DataLineage update(DataLineage lineage) {
        // 确保更新时必填字段有值
        if (lineage.getSourceType() == null || lineage.getSourceType().isEmpty()) {
            lineage.setSourceType("table");
        }
        if (lineage.getSourceName() == null || lineage.getSourceName().isEmpty()) {
            lineage.setSourceName(lineage.getSourceTable() != null ? lineage.getSourceTable() : "unknown");
        }
        if (lineage.getTargetType() == null || lineage.getTargetType().isEmpty()) {
            lineage.setTargetType("table");
        }
        if (lineage.getTargetName() == null || lineage.getTargetName().isEmpty()) {
            lineage.setTargetName(lineage.getTargetTable() != null ? lineage.getTargetTable() : "unknown");
        }
        if (lineage.getLineageType() == null || lineage.getLineageType().isEmpty()) {
            lineage.setLineageType("manual");
        }
        dataLineageMapper.update(lineage);
        return lineage;
    }
    
    public void delete(Long id) {
        dataLineageMapper.deleteById(id);
    }
    
    public DataLineage getById(Long id) {
        return dataLineageMapper.findById(id);
    }
    
    public List<DataLineage> getAll() {
        return dataLineageMapper.findAll();
    }
    
    public List<DataLineage> getByTableName(String tableName) {
        return dataLineageMapper.findByTableName(tableName);
    }
    
    public List<DataLineage> getUpstream(String tableName) {
        return dataLineageMapper.findUpstream(tableName);
    }
    
    public List<DataLineage> getDownstream(String tableName) {
        return dataLineageMapper.findDownstream(tableName);
    }
    
    /**
     * 获取血缘图谱数据（用于可视化）
     */
    public Map<String, Object> getLineageGraph(String tableName, int depth) {
        Map<String, Object> result = new HashMap<>();
        Set<String> visitedTables = new HashSet<>();
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();
        
        // 添加中心节点
        addNode(nodes, tableName, "center", visitedTables);
        
        // 递归获取上游
        collectUpstream(tableName, depth, nodes, edges, visitedTables);
        
        // 递归获取下游
        collectDownstream(tableName, depth, nodes, edges, visitedTables);
        
        result.put("nodes", nodes);
        result.put("edges", edges);
        result.put("centerTable", tableName);
        
        return result;
    }
    
    private void collectUpstream(String tableName, int depth, List<Map<String, Object>> nodes, 
                                  List<Map<String, Object>> edges, Set<String> visited) {
        if (depth <= 0) return;
        
        List<DataLineage> upstreams = dataLineageMapper.findUpstream(tableName);
        for (DataLineage lineage : upstreams) {
            String sourceTable = lineage.getSourceTable();
            if (sourceTable != null && !sourceTable.isEmpty()) {
                // 添加边
                Map<String, Object> edge = new HashMap<>();
                edge.put("source", sourceTable);
                edge.put("target", tableName);
                edge.put("lineageType", lineage.getLineageType());
                edge.put("transformLogic", lineage.getTransformLogic());
                edges.add(edge);
                
                // 添加节点并递归
                if (addNode(nodes, sourceTable, "upstream", visited)) {
                    collectUpstream(sourceTable, depth - 1, nodes, edges, visited);
                }
            }
        }
    }
    
    private void collectDownstream(String tableName, int depth, List<Map<String, Object>> nodes,
                                    List<Map<String, Object>> edges, Set<String> visited) {
        if (depth <= 0) return;
        
        List<DataLineage> downstreams = dataLineageMapper.findDownstream(tableName);
        for (DataLineage lineage : downstreams) {
            String targetTable = lineage.getTargetTable();
            if (targetTable != null && !targetTable.isEmpty()) {
                // 添加边
                Map<String, Object> edge = new HashMap<>();
                edge.put("source", tableName);
                edge.put("target", targetTable);
                edge.put("lineageType", lineage.getLineageType());
                edge.put("transformLogic", lineage.getTransformLogic());
                edges.add(edge);
                
                // 添加节点并递归
                if (addNode(nodes, targetTable, "downstream", visited)) {
                    collectDownstream(targetTable, depth - 1, nodes, edges, visited);
                }
            }
        }
    }
    
    private boolean addNode(List<Map<String, Object>> nodes, String tableName, String type, Set<String> visited) {
        if (visited.contains(tableName)) return false;
        visited.add(tableName);
        
        Map<String, Object> node = new HashMap<>();
        node.put("id", tableName);
        node.put("label", tableName);
        node.put("type", type);
        nodes.add(node);
        return true;
    }
    
    /**
     * 从SQL解析血缘关系
     */
    public List<DataLineage> parseFromSql(String sql, String targetTable, String targetDatabase) {
        List<DataLineage> lineages = new ArrayList<>();
        
        if (sql == null || sql.isEmpty()) return lineages;
        
        // 提取FROM和JOIN中的表名
        Set<String> sourceTables = extractTablesFromSql(sql);
        
        for (String sourceTable : sourceTables) {
            if (!sourceTable.equalsIgnoreCase(targetTable)) {
                DataLineage lineage = new DataLineage();
                lineage.setSourceType("table");
                lineage.setSourceName(sourceTable);  // 设置必填字段
                lineage.setSourceTable(sourceTable);
                lineage.setTargetType("table");
                lineage.setTargetName(targetTable);  // 设置必填字段
                lineage.setTargetTable(targetTable);
                lineage.setTargetDatabase(targetDatabase);
                lineage.setLineageType("sql");
                lineage.setSqlContent(sql);
                lineage.setCreateTime(LocalDateTime.now());
                lineages.add(lineage);
            }
        }
        
        return lineages;
    }
    
    /**
     * 从SQL中提取表名
     */
    private Set<String> extractTablesFromSql(String sql) {
        Set<String> tables = new HashSet<>();
        String upperSql = sql.toUpperCase();
        
        // FROM table
        Pattern fromPattern = Pattern.compile("FROM\\s+([`\\w]+\\.)?([`\\w]+)", Pattern.CASE_INSENSITIVE);
        Matcher fromMatcher = fromPattern.matcher(sql);
        while (fromMatcher.find()) {
            String table = fromMatcher.group(2).replace("`", "");
            tables.add(table);
        }
        
        // JOIN table
        Pattern joinPattern = Pattern.compile("JOIN\\s+([`\\w]+\\.)?([`\\w]+)", Pattern.CASE_INSENSITIVE);
        Matcher joinMatcher = joinPattern.matcher(sql);
        while (joinMatcher.find()) {
            String table = joinMatcher.group(2).replace("`", "");
            tables.add(table);
        }
        
        return tables;
    }
    
    /**
     * 从ETL任务自动采集血缘
     */
    public DataLineage createFromEtl(Long sourceDataSourceId, String sourceTable, 
                                      Long targetDataSourceId, String targetTable,
                                      String jobName) {
        DataLineage lineage = new DataLineage();
        lineage.setSourceType("table");
        lineage.setSourceId(sourceDataSourceId);
        lineage.setSourceName(sourceTable);  // 设置必填字段
        lineage.setSourceTable(sourceTable);
        lineage.setTargetType("table");
        lineage.setTargetId(targetDataSourceId);
        lineage.setTargetName(targetTable);  // 设置必填字段
        lineage.setTargetTable(targetTable);
        lineage.setLineageType("etl");
        lineage.setTransformLogic("ETL任务: " + jobName);
        lineage.setCreateTime(LocalDateTime.now());
        
        dataLineageMapper.insert(lineage);
        return lineage;
    }
    
    /**
     * 从报表定义自动采集血缘
     */
    public List<DataLineage> createFromReport(Long dataSourceId, String reportName, String sql) {
        List<DataLineage> lineages = parseFromSql(sql, "report:" + reportName, null);
        
        for (DataLineage lineage : lineages) {
            lineage.setSourceId(dataSourceId);
            lineage.setTargetType("report");
            lineage.setTargetName(reportName);
            lineage.setLineageType("report");
            dataLineageMapper.insert(lineage);
        }
        
        return lineages;
    }
    
    /**
     * 从数据采集任务自动记录血缘
     */
    public DataLineage createFromCollect(Long sourceDataSourceId, String sourceTable,
                                          Long targetDataSourceId, String targetTable,
                                          String taskName, String collectMode) {
        // 检查是否已存在相同的血缘记录，避免重复
        List<DataLineage> existing = dataLineageMapper.findByTableName(targetTable);
        for (DataLineage l : existing) {
            if ("collect".equals(l.getLineageType()) 
                    && sourceTable.equalsIgnoreCase(l.getSourceTable())
                    && targetTable.equalsIgnoreCase(l.getTargetTable())) {
                // 已存在，更新时间
                l.setUpdateTime(LocalDateTime.now());
                l.setTransformLogic("采集任务: " + taskName + " (模式: " + collectMode + ")");
                dataLineageMapper.update(l);
                return l;
            }
        }
        
        DataLineage lineage = new DataLineage();
        lineage.setSourceType("table");
        lineage.setSourceId(sourceDataSourceId);
        lineage.setSourceName(sourceTable);  // 设置必填字段
        lineage.setSourceTable(sourceTable);
        lineage.setTargetType("table");
        lineage.setTargetId(targetDataSourceId);
        lineage.setTargetName(targetTable);  // 设置必填字段
        lineage.setTargetTable(targetTable);
        lineage.setLineageType("collect");
        lineage.setTransformLogic("采集任务: " + taskName + " (模式: " + collectMode + ")");
        lineage.setCreateTime(LocalDateTime.now());
        
        dataLineageMapper.insert(lineage);
        return lineage;
    }

    /**
     * 自动发现血缘关系
     */
    public Map<String, Object> autoDiscover() {
        int discoveredCount = 0;
        
        try {
            // 1. 从采集任务中发现血缘
            discoveredCount += discoverFromCollectTasks();
        } catch (Exception e) {
            log.warn("从采集任务发现血缘失败: {}", e.getMessage());
        }
        
        try {
            // 2. 从DataX任务中发现血缘
            discoveredCount += discoverFromDataxJobs();
        } catch (Exception e) {
            log.warn("从DataX任务发现血缘失败: {}", e.getMessage());
        }
        
        try {
            // 3. 从报表定义中发现血缘
            discoveredCount += discoverFromReports();
        } catch (Exception e) {
            log.warn("从报表发现血缘失败: {}", e.getMessage());
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("discoveredCount", discoveredCount);
        result.put("message", "发现 " + discoveredCount + " 条新血缘关系");
        return result;
    }
    
    private int discoverFromCollectTasks() {
        int count = 0;
        try {
            String sql = "SELECT ct.id, ct.task_name, ct.table_name, ct.collect_mode, " +
                         "ds.id as ds_id, ds.name as ds_name " +
                         "FROM collect_task ct LEFT JOIN data_source ds ON ct.data_source_id = ds.id " +
                         "WHERE ct.table_name IS NOT NULL AND ct.table_name != ''";
            List<Map<String, Object>> tasks = jdbcTemplate.queryForList(sql);
            
            for (Map<String, Object> task : tasks) {
                String tableName = (String) task.get("table_name");
                String taskName = (String) task.get("task_name");
                String mode = (String) task.get("collect_mode");
                Number dsId = (Number) task.get("ds_id");
                String dsName = (String) task.get("ds_name");
                
                if (tableName == null || tableName.isEmpty()) continue;
                
                // 检查是否已存在
                if (!lineageExists(tableName, "local_" + tableName, "collect")) {
                    DataLineage lineage = new DataLineage();
                    lineage.setSourceType("datasource");
                    lineage.setSourceId(dsId != null ? dsId.longValue() : null);
                    lineage.setSourceName(dsName != null ? dsName : tableName);  // 确保不为空
                    lineage.setSourceTable(tableName);
                    lineage.setTargetType("table");
                    lineage.setTargetName("local_" + tableName);  // 设置必填字段
                    lineage.setTargetTable("local_" + tableName);
                    lineage.setLineageType("collect");
                    lineage.setTransformLogic("采集任务: " + taskName + " (模式: " + (mode != null ? mode : "full") + ")");
                    lineage.setCreateTime(LocalDateTime.now());
                    dataLineageMapper.insert(lineage);
                    count++;
                }
            }
        } catch (Exception e) {
            log.debug("采集任务表可能不存在: {}", e.getMessage());
        }
        return count;
    }
    
    private int discoverFromDataxJobs() {
        int count = 0;
        try {
            String sql = "SELECT j.id, j.job_name, j.source_table, j.target_table, " +
                         "j.source_data_source_id, j.target_data_source_id " +
                         "FROM datax_job j " +
                         "WHERE j.source_table IS NOT NULL AND j.target_table IS NOT NULL AND j.del_flag = 0";
            List<Map<String, Object>> jobs = jdbcTemplate.queryForList(sql);
            
            for (Map<String, Object> job : jobs) {
                String sourceTable = (String) job.get("source_table");
                String targetTable = (String) job.get("target_table");
                String jobName = (String) job.get("job_name");
                
                if (sourceTable == null || targetTable == null) continue;
                
                if (!lineageExists(sourceTable, targetTable, "etl")) {
                    DataLineage lineage = new DataLineage();
                    lineage.setSourceType("table");
                    lineage.setSourceName(sourceTable);  // 设置必填字段
                    lineage.setSourceTable(sourceTable);
                    lineage.setTargetType("table");
                    lineage.setTargetName(targetTable);  // 设置必填字段
                    lineage.setTargetTable(targetTable);
                    lineage.setLineageType("etl");
                    lineage.setTransformLogic("DataX任务: " + jobName);
                    lineage.setCreateTime(LocalDateTime.now());
                    dataLineageMapper.insert(lineage);
                    count++;
                }
            }
        } catch (Exception e) {
            log.debug("DataX任务表可能不存在: {}", e.getMessage());
        }
        return count;
    }
    
    private int discoverFromReports() {
        int count = 0;
        try {
            String sql = "SELECT id, report_name, report_code, sql_content, data_source_id FROM report_definition WHERE sql_content IS NOT NULL AND sql_content != ''";
            List<Map<String, Object>> reports = jdbcTemplate.queryForList(sql);
            
            for (Map<String, Object> report : reports) {
                String reportName = (String) report.get("report_name");
                String querySql = (String) report.get("sql_content");
                
                if (querySql == null || querySql.isEmpty()) continue;
                
                Set<String> tables = extractTablesFromSql(querySql);
                for (String table : tables) {
                    String targetName = "report:" + reportName;
                    if (!lineageExists(table, targetName, "report")) {
                        DataLineage lineage = new DataLineage();
                        lineage.setSourceType("table");
                        lineage.setSourceName(table);  // 设置必填字段
                        lineage.setSourceTable(table);
                        lineage.setTargetType("report");
                        lineage.setTargetTable(targetName);
                        lineage.setTargetName(reportName);
                        lineage.setLineageType("report");
                        lineage.setSqlContent(querySql.length() > 2000 ? querySql.substring(0, 2000) : querySql);
                        lineage.setTransformLogic("报表: " + reportName);
                        lineage.setCreateTime(LocalDateTime.now());
                        dataLineageMapper.insert(lineage);
                        count++;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("报表表可能不存在: {}", e.getMessage());
        }
        return count;
    }
    
    private boolean lineageExists(String sourceTable, String targetTable, String type) {
        List<DataLineage> existing = dataLineageMapper.findByTableName(targetTable);
        for (DataLineage l : existing) {
            if (type.equals(l.getLineageType())
                    && sourceTable.equalsIgnoreCase(l.getSourceTable())
                    && targetTable.equalsIgnoreCase(l.getTargetTable())) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 影响分析：查找某表变更会影响的所有下游表/报表
     */
    public Map<String, Object> getImpactAnalysis(String tableName, int depth) {
        Set<String> impacted = new LinkedHashSet<>();
        List<Map<String, Object>> impactChain = new ArrayList<>();
        collectImpact(tableName, depth, impacted, impactChain, 0);
        
        Map<String, Object> result = new HashMap<>();
        result.put("tableName", tableName);
        result.put("impactedCount", impacted.size());
        result.put("impactedTables", new ArrayList<>(impacted));
        result.put("impactChain", impactChain);
        return result;
    }
    
    private void collectImpact(String tableName, int depth, Set<String> impacted, 
                                List<Map<String, Object>> chain, int currentDepth) {
        if (depth <= 0 || impacted.contains(tableName)) return;
        
        List<DataLineage> downstreams = dataLineageMapper.findDownstream(tableName);
        for (DataLineage l : downstreams) {
            String target = l.getTargetTable();
            if (target != null && !target.isEmpty() && !impacted.contains(target)) {
                impacted.add(target);
                Map<String, Object> item = new HashMap<>();
                item.put("source", tableName);
                item.put("target", target);
                item.put("type", l.getLineageType());
                item.put("depth", currentDepth + 1);
                item.put("logic", l.getTransformLogic());
                chain.add(item);
                collectImpact(target, depth - 1, impacted, chain, currentDepth + 1);
            }
        }
    }

    /**
     * 获取血缘统计信息
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        List<DataLineage> all = dataLineageMapper.findAll();
        stats.put("totalCount", all.size());
        
        // 按类型统计
        Map<String, Long> byType = all.stream()
            .filter(l -> l.getLineageType() != null)
            .collect(Collectors.groupingBy(DataLineage::getLineageType, Collectors.counting()));
        stats.put("byType", byType);
        
        // 统计表数量
        Set<String> tables = new HashSet<>();
        for (DataLineage l : all) {
            if (l.getSourceTable() != null) tables.add(l.getSourceTable());
            if (l.getTargetTable() != null) tables.add(l.getTargetTable());
        }
        stats.put("tableCount", tables.size());
        
        return stats;
    }
    
    // ==================== 智能分析功能 ====================
    
    /**
     * 全链路追溯：获取某表的完整上下游链路
     */
    public Map<String, Object> getFullChainAnalysis(String tableName, int maxDepth) {
        Map<String, Object> result = new HashMap<>();
        result.put("tableName", tableName);
        
        // 上游链路
        Set<String> visitedUp = new HashSet<>();
        List<Map<String, Object>> upstreamChain = new ArrayList<>();
        int upstreamLayers = collectFullChain(tableName, maxDepth, visitedUp, upstreamChain, true, 0);
        
        // 下游链路
        Set<String> visitedDown = new HashSet<>();
        List<Map<String, Object>> downstreamChain = new ArrayList<>();
        int downstreamLayers = collectFullChain(tableName, maxDepth, visitedDown, downstreamChain, false, 0);
        
        result.put("upstreamChain", upstreamChain);
        result.put("downstreamChain", downstreamChain);
        result.put("totalUpstreamLayers", upstreamLayers);
        result.put("totalDownstreamLayers", downstreamLayers);
        result.put("totalNodes", visitedUp.size() + visitedDown.size());
        
        // 找关键路径（最长路径）
        List<String> criticalPath = findCriticalPath(tableName, upstreamChain, downstreamChain);
        result.put("criticalPath", criticalPath);
        
        return result;
    }
    
    private int collectFullChain(String tableName, int depth, Set<String> visited, 
                                  List<Map<String, Object>> chain, boolean upstream, int currentLayer) {
        if (depth <= 0 || visited.contains(tableName)) return currentLayer;
        visited.add(tableName);
        
        int maxLayer = currentLayer;
        List<DataLineage> lineages = upstream ? 
            dataLineageMapper.findUpstream(tableName) : 
            dataLineageMapper.findDownstream(tableName);
        
        for (DataLineage l : lineages) {
            String nextTable = upstream ? l.getSourceTable() : l.getTargetTable();
            if (nextTable != null && !nextTable.isEmpty() && !visited.contains(nextTable)) {
                Map<String, Object> node = new HashMap<>();
                node.put("tableName", nextTable);
                node.put("layer", currentLayer + 1);
                node.put("lineageType", l.getLineageType());
                node.put("transformLogic", l.getTransformLogic());
                node.put("parentTable", tableName);
                node.put("direction", upstream ? "upstream" : "downstream");
                
                // 提取涉及的字段
                List<String> columns = new ArrayList<>();
                if (l.getSourceColumn() != null) columns.add(l.getSourceColumn());
                if (l.getTargetColumn() != null) columns.add(l.getTargetColumn());
                node.put("columns", columns);
                
                chain.add(node);
                
                int subLayer = collectFullChain(nextTable, depth - 1, visited, chain, upstream, currentLayer + 1);
                maxLayer = Math.max(maxLayer, subLayer);
            }
        }
        
        return maxLayer;
    }
    
    private List<String> findCriticalPath(String center, List<Map<String, Object>> upstream, List<Map<String, Object>> downstream) {
        List<String> path = new ArrayList<>();
        
        // 从上游找最深的路径
        List<String> upPath = new ArrayList<>();
        int maxUpLayer = 0;
        for (Map<String, Object> node : upstream) {
            int layer = (int) node.get("layer");
            if (layer > maxUpLayer) {
                maxUpLayer = layer;
            }
        }
        // 回溯构建上游路径
        String current = null;
        for (int layer = maxUpLayer; layer >= 1; layer--) {
            for (Map<String, Object> node : upstream) {
                if ((int) node.get("layer") == layer) {
                    if (current == null || current.equals(node.get("parentTable"))) {
                        upPath.add(0, (String) node.get("tableName"));
                        current = (String) node.get("tableName");
                        break;
                    }
                }
            }
        }
        
        path.addAll(upPath);
        path.add(center);
        
        // 从下游找最深的路径
        current = center;
        for (int layer = 1; layer <= 10; layer++) {
            boolean found = false;
            for (Map<String, Object> node : downstream) {
                if ((int) node.get("layer") == layer && current.equals(node.get("parentTable"))) {
                    path.add((String) node.get("tableName"));
                    current = (String) node.get("tableName");
                    found = true;
                    break;
                }
            }
            if (!found) break;
        }
        
        return path;
    }
    
    /**
     * 热点表分析：找出依赖最多/被依赖最多的表
     */
    public Map<String, Object> getHotspotAnalysis(int topN) {
        List<DataLineage> all = dataLineageMapper.findAll();
        
        // 统计每个表的上下游数量
        Map<String, int[]> tableStats = new HashMap<>(); // [upstream, downstream]
        
        for (DataLineage l : all) {
            String source = l.getSourceTable();
            String target = l.getTargetTable();
            
            if (source != null && !source.isEmpty()) {
                tableStats.computeIfAbsent(source, k -> new int[2]);
                tableStats.get(source)[1]++; // 作为源表，说明有下游
            }
            if (target != null && !target.isEmpty()) {
                tableStats.computeIfAbsent(target, k -> new int[2]);
                tableStats.get(target)[0]++; // 作为目标表，说明有上游
            }
        }
        
        // 计算热度并排序
        List<Map<String, Object>> hotspots = new ArrayList<>();
        double totalConnections = 0;
        
        for (Map.Entry<String, int[]> entry : tableStats.entrySet()) {
            String table = entry.getKey();
            int[] counts = entry.getValue();
            int upCount = counts[0];
            int downCount = counts[1];
            int total = upCount + downCount;
            totalConnections += total;
            
            // 热度分数：下游影响权重更高
            double hotScore = upCount + downCount * 1.5;
            
            Map<String, Object> hotspot = new HashMap<>();
            hotspot.put("tableName", table);
            hotspot.put("upstreamCount", upCount);
            hotspot.put("downstreamCount", downCount);
            hotspot.put("totalConnections", total);
            hotspot.put("hotScore", hotScore);
            
            // 风险等级
            String riskLevel;
            String suggestion;
            if (downCount >= 10) {
                riskLevel = "high";
                suggestion = "该表被大量下游依赖，变更需谨慎，建议进行充分的影响分析和回归测试";
            } else if (downCount >= 5 || total >= 8) {
                riskLevel = "medium";
                suggestion = "该表有较多依赖关系，变更前建议评估影响范围";
            } else {
                riskLevel = "low";
                suggestion = "该表依赖关系较少，变更风险较低";
            }
            hotspot.put("riskLevel", riskLevel);
            hotspot.put("suggestion", suggestion);
            
            hotspots.add(hotspot);
        }
        
        // 按热度排序
        hotspots.sort((a, b) -> Double.compare((double) b.get("hotScore"), (double) a.get("hotScore")));
        
        Map<String, Object> result = new HashMap<>();
        result.put("hotspots", hotspots.subList(0, Math.min(topN, hotspots.size())));
        result.put("totalTables", tableStats.size());
        result.put("averageDependencies", tableStats.isEmpty() ? 0 : totalConnections / tableStats.size());
        
        return result;
    }
    
    /**
     * 孤岛检测：找出没有血缘关系的表、终端表、源头表
     */
    public Map<String, Object> getOrphanAnalysis() {
        List<DataLineage> all = dataLineageMapper.findAll();
        
        Set<String> hasUpstream = new HashSet<>();   // 有上游的表
        Set<String> hasDownstream = new HashSet<>(); // 有下游的表
        Set<String> allTables = new HashSet<>();
        
        for (DataLineage l : all) {
            String source = l.getSourceTable();
            String target = l.getTargetTable();
            
            if (source != null && !source.isEmpty()) {
                hasDownstream.add(source);
                allTables.add(source);
            }
            if (target != null && !target.isEmpty()) {
                hasUpstream.add(target);
                allTables.add(target);
            }
        }
        
        // 源头表：只有下游，没有上游
        List<String> sourceTables = new ArrayList<>();
        for (String t : hasDownstream) {
            if (!hasUpstream.contains(t)) {
                sourceTables.add(t);
            }
        }
        
        // 终端表：只有上游，没有下游
        List<String> deadEndTables = new ArrayList<>();
        for (String t : hasUpstream) {
            if (!hasDownstream.contains(t)) {
                deadEndTables.add(t);
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("sourceTables", sourceTables);
        result.put("deadEndTables", deadEndTables);
        result.put("totalTablesInLineage", allTables.size());
        result.put("sourceTableCount", sourceTables.size());
        result.put("deadEndTableCount", deadEndTables.size());
        
        return result;
    }
    
    /**
     * 血缘健康度报告
     */
    public Map<String, Object> getHealthReport() {
        List<DataLineage> all = dataLineageMapper.findAll();
        
        Map<String, Object> report = new HashMap<>();
        report.put("totalLineages", all.size());
        
        // 收集所有表
        Set<String> allTables = new HashSet<>();
        for (DataLineage l : all) {
            if (l.getSourceTable() != null) allTables.add(l.getSourceTable());
            if (l.getTargetTable() != null) allTables.add(l.getTargetTable());
        }
        report.put("totalTables", allTables.size());
        
        // 类型分布
        Map<String, Long> typeDistribution = all.stream()
            .filter(l -> l.getLineageType() != null)
            .collect(Collectors.groupingBy(DataLineage::getLineageType, Collectors.counting()));
        report.put("lineageTypeDistribution", typeDistribution);
        
        // 检测问题
        List<Map<String, Object>> issues = new ArrayList<>();
        
        // 1. 检查孤岛
        Map<String, Object> orphanAnalysis = getOrphanAnalysis();
        List<String> sourceTables = (List<String>) orphanAnalysis.get("sourceTables");
        List<String> deadEndTables = (List<String>) orphanAnalysis.get("deadEndTables");
        
        if (sourceTables.size() > 10) {
            Map<String, Object> issue = new HashMap<>();
            issue.put("type", "too_many_sources");
            issue.put("severity", "warning");
            issue.put("description", "存在 " + sourceTables.size() + " 个源头表，建议检查是否有遗漏的血缘关系");
            issues.add(issue);
        }
        
        // 2. 检查热点表
        Map<String, Object> hotspotAnalysis = getHotspotAnalysis(5);
        List<Map<String, Object>> hotspots = (List<Map<String, Object>>) hotspotAnalysis.get("hotspots");
        for (Map<String, Object> h : hotspots) {
            if ("high".equals(h.get("riskLevel"))) {
                Map<String, Object> issue = new HashMap<>();
                issue.put("type", "high_risk_table");
                issue.put("severity", "critical");
                issue.put("tableName", h.get("tableName"));
                issue.put("description", "表 " + h.get("tableName") + " 被 " + h.get("downstreamCount") + " 个下游依赖，属于高风险核心表");
                issue.put("suggestion", h.get("suggestion"));
                issues.add(issue);
            }
        }
        
        report.put("issues", issues);
        
        // 计算健康分数
        int healthScore = 100;
        for (Map<String, Object> issue : issues) {
            if ("critical".equals(issue.get("severity"))) healthScore -= 15;
            else if ("warning".equals(issue.get("severity"))) healthScore -= 5;
        }
        healthScore = Math.max(0, healthScore);
        
        String healthLevel;
        if (healthScore >= 90) healthLevel = "优秀";
        else if (healthScore >= 70) healthLevel = "良好";
        else if (healthScore >= 50) healthLevel = "一般";
        else healthLevel = "需改进";
        
        report.put("healthScore", healthScore);
        report.put("healthLevel", healthLevel);
        
        // 建议
        List<String> recommendations = new ArrayList<>();
        if (all.size() < 10) {
            recommendations.add("血缘记录较少，建议使用'自动发现'功能扫描系统中的任务和报表");
        }
        if (typeDistribution.getOrDefault("manual", 0L) > all.size() * 0.5) {
            recommendations.add("手动录入的血缘占比较高，建议从SQL或ETL任务自动解析血缘");
        }
        recommendations.add("定期执行自动发现以保持血缘数据的时效性");
        report.put("recommendations", recommendations);
        
        return report;
    }
    
    /**
     * 智能SQL解析并创建血缘（使用增强的解析器）
     */
    public Map<String, Object> parseAndCreateLineage(String sql, String targetTable, String targetDatabase) {
        Map<String, Object> result = new HashMap<>();
        
        // 使用简单正则解析（增强版）
        Set<String> sourceTables = extractTablesFromSqlEnhanced(sql);
        
        List<DataLineage> created = new ArrayList<>();
        for (String sourceTable : sourceTables) {
            if (!sourceTable.equalsIgnoreCase(targetTable)) {
                DataLineage lineage = new DataLineage();
                lineage.setSourceType("table");
                lineage.setSourceName(sourceTable);
                lineage.setSourceTable(sourceTable);
                lineage.setTargetType("table");
                lineage.setTargetName(targetTable);
                lineage.setTargetTable(targetTable);
                lineage.setTargetDatabase(targetDatabase);
                lineage.setLineageType("sql");
                lineage.setSqlContent(sql.length() > 2000 ? sql.substring(0, 2000) : sql);
                lineage.setTransformLogic("SQL解析自动生成");
                lineage.setCreateTime(LocalDateTime.now());
                
                create(lineage);
                created.add(lineage);
            }
        }
        
        result.put("success", true);
        result.put("parsedTables", sourceTables);
        result.put("createdCount", created.size());
        result.put("lineages", created);
        
        // 分析SQL复杂度
        result.put("sqlAnalysis", analyzeSqlComplexity(sql));
        
        return result;
    }
    
    /**
     * 增强的SQL表提取
     */
    private Set<String> extractTablesFromSqlEnhanced(String sql) {
        Set<String> tables = new LinkedHashSet<>();
        if (sql == null || sql.isEmpty()) return tables;
        
        // 预处理
        sql = sql.replaceAll("--.*", " ").replaceAll("/\\*.*?\\*/", " ").replaceAll("\\s+", " ");
        
        // FROM table
        java.util.regex.Pattern fromPattern = java.util.regex.Pattern.compile(
            "\\bFROM\\s+([`\"\\[\\]\\w_.]+)", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher fromMatcher = fromPattern.matcher(sql);
        while (fromMatcher.find()) {
            String table = fromMatcher.group(1).replaceAll("[`\"\\[\\]]", "");
            if (!table.toUpperCase().startsWith("SELECT")) {
                tables.add(table);
            }
        }
        
        // JOIN table
        java.util.regex.Pattern joinPattern = java.util.regex.Pattern.compile(
            "\\bJOIN\\s+([`\"\\[\\]\\w_.]+)", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher joinMatcher = joinPattern.matcher(sql);
        while (joinMatcher.find()) {
            String table = joinMatcher.group(1).replaceAll("[`\"\\[\\]]", "");
            tables.add(table);
        }
        
        // 子查询中的表（简化处理）
        int idx = 0;
        while ((idx = sql.indexOf("(", idx)) != -1) {
            int end = findMatchingParen(sql, idx);
            if (end > idx) {
                String inner = sql.substring(idx + 1, end);
                if (inner.trim().toUpperCase().startsWith("SELECT")) {
                    tables.addAll(extractTablesFromSqlEnhanced(inner));
                }
            }
            idx++;
        }
        
        // 移除DUAL等伪表
        tables.remove("DUAL");
        tables.remove("dual");
        
        return tables;
    }
    
    private int findMatchingParen(String sql, int start) {
        int depth = 0;
        for (int i = start; i < sql.length(); i++) {
            if (sql.charAt(i) == '(') depth++;
            else if (sql.charAt(i) == ')') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }
    
    /**
     * 分析SQL复杂度
     */
    private Map<String, Object> analyzeSqlComplexity(String sql) {
        Map<String, Object> analysis = new LinkedHashMap<>();
        String upper = sql.toUpperCase();
        
        analysis.put("hasSubquery", upper.indexOf("SELECT", upper.indexOf("SELECT") + 1) > 0);
        analysis.put("hasUnion", upper.contains(" UNION "));
        analysis.put("hasJoin", upper.contains(" JOIN "));
        analysis.put("hasCTE", upper.startsWith("WITH "));
        analysis.put("hasGroupBy", upper.contains(" GROUP BY "));
        analysis.put("hasWindow", upper.contains(" OVER(") || upper.contains(" OVER ("));
        
        int complexity = 1;
        if ((Boolean) analysis.get("hasSubquery")) complexity += 3;
        if ((Boolean) analysis.get("hasUnion")) complexity += 2;
        if ((Boolean) analysis.get("hasJoin")) complexity += 1;
        if ((Boolean) analysis.get("hasCTE")) complexity += 2;
        if ((Boolean) analysis.get("hasWindow")) complexity += 2;
        
        String level;
        if (complexity <= 2) level = "简单";
        else if (complexity <= 5) level = "中等";
        else if (complexity <= 8) level = "复杂";
        else level = "非常复杂";
        
        analysis.put("complexityScore", complexity);
        analysis.put("complexityLevel", level);
        
        return analysis;
    }
    
    /**
     * 获取表的依赖摘要
     */
    public Map<String, Object> getTableDependencySummary(String tableName) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("tableName", tableName);
        
        List<DataLineage> upstream = dataLineageMapper.findUpstream(tableName);
        List<DataLineage> downstream = dataLineageMapper.findDownstream(tableName);
        
        summary.put("upstreamCount", upstream.size());
        summary.put("downstreamCount", downstream.size());
        
        // 上游表列表
        List<String> upstreamTables = upstream.stream()
            .map(DataLineage::getSourceTable)
            .filter(t -> t != null && !t.isEmpty())
            .distinct()
            .collect(Collectors.toList());
        summary.put("upstreamTables", upstreamTables);
        
        // 下游表列表
        List<String> downstreamTables = downstream.stream()
            .map(DataLineage::getTargetTable)
            .filter(t -> t != null && !t.isEmpty())
            .distinct()
            .collect(Collectors.toList());
        summary.put("downstreamTables", downstreamTables);
        
        // 血缘类型分布
        Map<String, Long> typeDistribution = new HashMap<>();
        for (DataLineage l : upstream) {
            String type = l.getLineageType();
            if (type != null) {
                typeDistribution.merge(type, 1L, Long::sum);
            }
        }
        for (DataLineage l : downstream) {
            String type = l.getLineageType();
            if (type != null) {
                typeDistribution.merge(type, 1L, Long::sum);
            }
        }
        summary.put("lineageTypes", typeDistribution);
        
        // 风险评估
        String riskLevel;
        if (downstream.size() >= 10) riskLevel = "高";
        else if (downstream.size() >= 5) riskLevel = "中";
        else riskLevel = "低";
        summary.put("riskLevel", riskLevel);
        
        return summary;
    }
}
