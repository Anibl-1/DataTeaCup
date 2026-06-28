package com.dataplatform.data.service;

import com.dataplatform.data.entity.DataSource;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * SQL 自动补全服务
 * 提供表名、字段名和 SQL 关键字的自动补全建议
 * 
 * 需求: 2.3 - WHEN 用户在 SQL 编辑器中输入时，THE Report_Designer SHALL 提供表名和字段名的自动补全建议
 * 
 * @author dataplatform
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SqlAutoCompleteService {
    
    private final DataSourceService dataSourceService;
    
    /** 元数据缓存：dataSourceId -> 完整元数据，TTL 10分钟 */
    private Cache<Long, DataSourceMetadata> metadataCache;
    
    /** SQL 关键字列表 */
    private static final List<String> SQL_KEYWORDS = Arrays.asList(
        // DQL
        "SELECT", "FROM", "WHERE", "AND", "OR", "NOT", "IN", "BETWEEN", "LIKE", "IS", "NULL",
        "ORDER", "BY", "ASC", "DESC", "GROUP", "HAVING", "LIMIT", "OFFSET", "DISTINCT",
        // JOIN
        "JOIN", "INNER", "LEFT", "RIGHT", "FULL", "OUTER", "CROSS", "ON", "USING",
        // Aggregate
        "COUNT", "SUM", "AVG", "MAX", "MIN", "COALESCE", "IFNULL", "NULLIF",
        // DML
        "INSERT", "INTO", "VALUES", "UPDATE", "SET", "DELETE",
        // DDL
        "CREATE", "TABLE", "ALTER", "DROP", "INDEX", "VIEW", "DATABASE",
        // Clauses
        "AS", "CASE", "WHEN", "THEN", "ELSE", "END", "UNION", "ALL", "EXISTS",
        // Data types
        "INT", "INTEGER", "BIGINT", "SMALLINT", "TINYINT", "DECIMAL", "NUMERIC", "FLOAT", "DOUBLE",
        "VARCHAR", "CHAR", "TEXT", "BLOB", "DATE", "TIME", "DATETIME", "TIMESTAMP", "BOOLEAN",
        // Constraints
        "PRIMARY", "KEY", "FOREIGN", "REFERENCES", "UNIQUE", "CHECK", "DEFAULT", "AUTO_INCREMENT",
        // Functions
        "CONCAT", "SUBSTRING", "TRIM", "UPPER", "LOWER", "LENGTH", "REPLACE", "CAST", "CONVERT",
        "NOW", "CURDATE", "CURTIME", "DATE_FORMAT", "DATEDIFF", "DATE_ADD", "DATE_SUB",
        "ROUND", "FLOOR", "CEIL", "ABS", "MOD", "POWER", "SQRT",
        "IF", "GREATEST", "LEAST"
    );
    
    /** SQL 关键字（小写映射，用于快速查找） */
    private static final Set<String> SQL_KEYWORDS_LOWER = SQL_KEYWORDS.stream()
            .map(String::toLowerCase)
            .collect(Collectors.toSet());
    
    @PostConstruct
    public void init() {
        metadataCache = Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(100)
                .build();
        log.info("SQL 自动补全服务初始化完成");
    }
    
    /**
     * 获取自动补全建议
     * 
     * @param dataSourceId 数据源ID
     * @param prefix 输入前缀
     * @param context 上下文（可选，用于智能推断）
     * @return 补全建议列表
     */
    public List<CompletionItem> getCompletions(Long dataSourceId, String prefix, String context) {
        List<CompletionItem> completions = new ArrayList<>();
        String lowerPrefix = prefix != null ? prefix.toLowerCase() : "";
        
        // 1. 添加 SQL 关键字建议
        completions.addAll(getKeywordCompletions(lowerPrefix));
        
        // 2. 如果有数据源，添加表名和字段名建议
        if (dataSourceId != null) {
            DataSourceMetadata metadata = getMetadata(dataSourceId);
            if (metadata != null) {
                // 添加表名建议
                completions.addAll(getTableCompletions(metadata, lowerPrefix));
                
                // 根据上下文添加字段名建议
                completions.addAll(getColumnCompletions(metadata, lowerPrefix, context));
            }
        }
        
        // 按相关性排序：完全匹配 > 前缀匹配 > 包含匹配
        completions.sort((a, b) -> {
            int scoreA = calculateMatchScore(a.getLabel().toLowerCase(), lowerPrefix);
            int scoreB = calculateMatchScore(b.getLabel().toLowerCase(), lowerPrefix);
            if (scoreA != scoreB) return scoreB - scoreA;
            // 同分时按类型排序：关键字 > 表名 > 字段名
            return getTypeOrder(a.getType()) - getTypeOrder(b.getType());
        });
        
        // 限制返回数量
        return completions.stream().limit(50).collect(Collectors.toList());
    }
    
    /**
     * 获取数据源的表名列表
     */
    public List<String> getTableNames(Long dataSourceId) {
        DataSourceMetadata metadata = getMetadata(dataSourceId);
        if (metadata == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(metadata.getTables().keySet());
    }
    
    /**
     * 获取表的字段名列表
     */
    public List<ColumnInfo> getTableColumns(Long dataSourceId, String tableName) {
        DataSourceMetadata metadata = getMetadata(dataSourceId);
        if (metadata == null || tableName == null) {
            return Collections.emptyList();
        }
        TableInfo tableInfo = metadata.getTables().get(tableName.toLowerCase());
        if (tableInfo == null) {
            // 尝试原始大小写
            tableInfo = metadata.getTables().get(tableName);
        }
        return tableInfo != null ? tableInfo.getColumns() : Collections.emptyList();
    }
    
    /**
     * 获取 SQL 关键字列表
     */
    public List<String> getSqlKeywords() {
        return new ArrayList<>(SQL_KEYWORDS);
    }
    
    /**
     * 刷新数据源元数据缓存
     */
    public void refreshMetadata(Long dataSourceId) {
        metadataCache.invalidate(dataSourceId);
        log.info("已刷新数据源 {} 的元数据缓存", dataSourceId);
    }
    
    /**
     * 获取数据源元数据（带缓存）
     */
    private DataSourceMetadata getMetadata(Long dataSourceId) {
        if (dataSourceId == null) {
            return null;
        }
        
        return metadataCache.get(dataSourceId, id -> {
            try {
                return loadMetadata(id);
            } catch (Exception e) {
                log.warn("加载数据源 {} 元数据失败: {}", id, e.getMessage());
                return null;
            }
        });
    }
    
    /**
     * 从数据库加载元数据
     */
    private DataSourceMetadata loadMetadata(Long dataSourceId) {
        DataSourceMetadata metadata = new DataSourceMetadata();
        metadata.setDataSourceId(dataSourceId);
        metadata.setTables(new HashMap<>());
        
        // 获取表列表
        List<Map<String, String>> tables = dataSourceService.getTables(dataSourceId);
        for (Map<String, String> table : tables) {
            String tableName = table.get("tableName");
            String tableType = table.get("tableType");
            
            TableInfo tableInfo = new TableInfo();
            tableInfo.setName(tableName);
            tableInfo.setType(tableType);
            tableInfo.setColumns(new ArrayList<>());
            
            // 获取字段列表
            List<Map<String, Object>> columns = dataSourceService.getTableColumns(dataSourceId, tableName);
            for (Map<String, Object> col : columns) {
                ColumnInfo columnInfo = new ColumnInfo();
                columnInfo.setName((String) col.get("columnName"));
                columnInfo.setDataType((String) col.get("dataType"));
                columnInfo.setNullable("YES".equalsIgnoreCase((String) col.get("nullable")));
                columnInfo.setRemarks((String) col.get("remarks"));
                Object columnSize = col.get("columnSize");
                if (columnSize instanceof Number) {
                    columnInfo.setColumnSize(((Number) columnSize).intValue());
                }
                tableInfo.getColumns().add(columnInfo);
            }
            
            metadata.getTables().put(tableName.toLowerCase(), tableInfo);
        }
        
        metadata.setLoadTime(System.currentTimeMillis());
        log.debug("已加载数据源 {} 的元数据，共 {} 张表", dataSourceId, metadata.getTables().size());
        return metadata;
    }
    
    /**
     * 获取关键字补全建议
     */
    private List<CompletionItem> getKeywordCompletions(String prefix) {
        return SQL_KEYWORDS.stream()
                .filter(kw -> prefix.isEmpty() || kw.toLowerCase().contains(prefix))
                .map(kw -> {
                    CompletionItem item = new CompletionItem();
                    item.setLabel(kw);
                    item.setType("keyword");
                    item.setDetail("SQL 关键字");
                    return item;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 获取表名补全建议
     */
    private List<CompletionItem> getTableCompletions(DataSourceMetadata metadata, String prefix) {
        return metadata.getTables().values().stream()
                .filter(t -> prefix.isEmpty() || t.getName().toLowerCase().contains(prefix))
                .map(t -> {
                    CompletionItem item = new CompletionItem();
                    item.setLabel(t.getName());
                    item.setType("table");
                    item.setDetail("VIEW".equalsIgnoreCase(t.getType()) ? "视图" : "表");
                    item.setInfo(String.format("%d 个字段", t.getColumns().size()));
                    return item;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 获取字段名补全建议
     */
    private List<CompletionItem> getColumnCompletions(DataSourceMetadata metadata, String prefix, String context) {
        List<CompletionItem> completions = new ArrayList<>();
        
        // 解析上下文中的表名
        Set<String> contextTables = parseTablesFromContext(context);
        
        for (TableInfo table : metadata.getTables().values()) {
            // 如果有上下文表名，只显示相关表的字段
            if (!contextTables.isEmpty() && !contextTables.contains(table.getName().toLowerCase())) {
                continue;
            }
            
            for (ColumnInfo col : table.getColumns()) {
                if (prefix.isEmpty() || col.getName().toLowerCase().contains(prefix)) {
                    CompletionItem item = new CompletionItem();
                    item.setLabel(col.getName());
                    item.setType("column");
                    item.setDetail(col.getDataType());
                    item.setInfo(table.getName());
                    if (col.getRemarks() != null && !col.getRemarks().isEmpty()) {
                        item.setDocumentation(col.getRemarks());
                    }
                    completions.add(item);
                }
            }
        }
        
        return completions;
    }
    
    /**
     * 从 SQL 上下文中解析表名
     */
    private Set<String> parseTablesFromContext(String context) {
        Set<String> tables = new HashSet<>();
        if (context == null || context.isEmpty()) {
            return tables;
        }
        
        String lowerContext = context.toLowerCase();
        
        // 简单解析 FROM 和 JOIN 后的表名
        String[] patterns = {"from\\s+(\\w+)", "join\\s+(\\w+)"};
        for (String pattern : patterns) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher m = p.matcher(context);
            while (m.find()) {
                tables.add(m.group(1).toLowerCase());
            }
        }
        
        return tables;
    }
    
    /**
     * 计算匹配分数
     */
    private int calculateMatchScore(String label, String prefix) {
        if (prefix.isEmpty()) return 0;
        if (label.equals(prefix)) return 100;
        if (label.startsWith(prefix)) return 80;
        if (label.contains(prefix)) return 50;
        return 0;
    }
    
    /**
     * 获取类型排序权重
     */
    private int getTypeOrder(String type) {
        switch (type) {
            case "keyword": return 1;
            case "table": return 2;
            case "column": return 3;
            default: return 4;
        }
    }
    
    // ========== 内部数据类 ==========
    
    @Data
    public static class DataSourceMetadata {
        private Long dataSourceId;
        private Map<String, TableInfo> tables;
        private long loadTime;
    }
    
    @Data
    public static class TableInfo {
        private String name;
        private String type;
        private List<ColumnInfo> columns;
    }
    
    @Data
    public static class ColumnInfo {
        private String name;
        private String dataType;
        private boolean nullable;
        private String remarks;
        private int columnSize;
    }
    
    @Data
    public static class CompletionItem {
        private String label;      // 显示文本
        private String type;       // 类型：keyword, table, column
        private String detail;     // 详细信息（如数据类型）
        private String info;       // 附加信息（如所属表名）
        private String documentation; // 文档/注释
    }
}
