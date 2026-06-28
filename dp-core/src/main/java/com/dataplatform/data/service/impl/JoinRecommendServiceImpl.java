package com.dataplatform.data.service.impl;

import com.dataplatform.data.dto.JoinRecommendation;
import com.dataplatform.data.dto.JoinRecommendation.RecommendationType;
import com.dataplatform.data.service.DataSourceService;
import com.dataplatform.data.service.JoinRecommendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 表关联推荐服务实现
 * 基于外键关系和字段名匹配自动推荐表之间的关联关系
 * 
 * 验证需求: 12.2 - THE Query_Builder SHALL 自动推荐表之间的关联关系（基于外键或字段名匹配）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JoinRecommendServiceImpl implements JoinRecommendService {
    
    private final DataSourceService dataSourceService;
    
    // 常见的ID字段后缀模式
    private static final List<String> ID_SUFFIXES = Arrays.asList("_id", "id", "_key", "_fk");
    
    // 常见的主键字段名
    private static final Set<String> PRIMARY_KEY_NAMES = Set.of("id", "pk", "key", "code");
    
    // 字段名匹配模式：table_name_id 匹配 table_name.id
    private static final Pattern TABLE_ID_PATTERN = Pattern.compile("^(.+?)_?(id|key|code)$", Pattern.CASE_INSENSITIVE);
    
    @Override
    public List<JoinRecommendation> recommendJoins(Long dataSourceId, String leftTable, String rightTable) {
        List<JoinRecommendation> recommendations = new ArrayList<>();
        
        // 1. 首先检测外键关系（置信度最高）
        recommendations.addAll(detectForeignKeyJoins(dataSourceId, leftTable, rightTable));
        
        // 2. 检测字段名匹配（置信度次之）
        recommendations.addAll(detectFieldNameJoins(dataSourceId, leftTable, rightTable));
        
        // 去重并按置信度排序
        return deduplicateAndSort(recommendations);
    }
    
    @Override
    public List<JoinRecommendation> recommendJoinsForTables(Long dataSourceId, List<String> tableNames) {
        List<JoinRecommendation> allRecommendations = new ArrayList<>();
        
        // 对每对表进行关联推荐
        for (int i = 0; i < tableNames.size(); i++) {
            for (int j = i + 1; j < tableNames.size(); j++) {
                String leftTable = tableNames.get(i);
                String rightTable = tableNames.get(j);
                
                // 双向检测
                allRecommendations.addAll(recommendJoins(dataSourceId, leftTable, rightTable));
                allRecommendations.addAll(recommendJoins(dataSourceId, rightTable, leftTable));
            }
        }
        
        return deduplicateAndSort(allRecommendations);
    }
    
    @Override
    public List<JoinRecommendation> detectForeignKeyJoins(Long dataSourceId, String leftTable, String rightTable) {
        List<JoinRecommendation> recommendations = new ArrayList<>();
        
        try {
            DataSource ds = dataSourceService.getDataSource(dataSourceId);
            try (Connection conn = ds.getConnection()) {
                DatabaseMetaData metaData = conn.getMetaData();
                
                // 检查 leftTable 的外键是否引用 rightTable
                recommendations.addAll(getForeignKeyRecommendations(metaData, leftTable, rightTable, conn));
                
                // 检查 rightTable 的外键是否引用 leftTable
                recommendations.addAll(getForeignKeyRecommendations(metaData, rightTable, leftTable, conn));
            }
        } catch (Exception e) {
            log.warn("检测外键关联失败: dataSourceId={}, leftTable={}, rightTable={}, error={}", 
                    dataSourceId, leftTable, rightTable, e.getMessage());
        }
        
        return recommendations;
    }
    
    @Override
    public List<JoinRecommendation> detectFieldNameJoins(Long dataSourceId, String leftTable, String rightTable) {
        List<JoinRecommendation> recommendations = new ArrayList<>();
        
        try {
            DataSource ds = dataSourceService.getDataSource(dataSourceId);
            try (Connection conn = ds.getConnection()) {
                DatabaseMetaData metaData = conn.getMetaData();
                
                // 获取两个表的列信息
                List<ColumnInfo> leftColumns = getTableColumns(metaData, leftTable, conn);
                List<ColumnInfo> rightColumns = getTableColumns(metaData, rightTable, conn);
                
                // 1. 检测 table_name_id 模式匹配
                recommendations.addAll(detectTableNameIdPattern(leftTable, leftColumns, rightTable, rightColumns));
                recommendations.addAll(detectTableNameIdPattern(rightTable, rightColumns, leftTable, leftColumns));
                
                // 2. 检测主键与外键字段名匹配
                recommendations.addAll(detectPrimaryKeyMatch(leftTable, leftColumns, rightTable, rightColumns));
                recommendations.addAll(detectPrimaryKeyMatch(rightTable, rightColumns, leftTable, leftColumns));
                
                // 3. 检测相同字段名匹配
                recommendations.addAll(detectSameFieldNameMatch(leftTable, leftColumns, rightTable, rightColumns));
            }
        } catch (Exception e) {
            log.warn("检测字段名关联失败: dataSourceId={}, leftTable={}, rightTable={}, error={}", 
                    dataSourceId, leftTable, rightTable, e.getMessage());
        }
        
        return recommendations;
    }
    
    /**
     * 获取外键推荐
     */
    private List<JoinRecommendation> getForeignKeyRecommendations(
            DatabaseMetaData metaData, String fkTable, String pkTable, Connection conn) throws SQLException {
        List<JoinRecommendation> recommendations = new ArrayList<>();
        
        String catalog = conn.getCatalog();
        
        try (ResultSet rs = metaData.getImportedKeys(catalog, null, fkTable)) {
            while (rs.next()) {
                String pkTableName = rs.getString("PKTABLE_NAME");
                if (pkTableName.equalsIgnoreCase(pkTable)) {
                    String fkColumnName = rs.getString("FKCOLUMN_NAME");
                    String pkColumnName = rs.getString("PKCOLUMN_NAME");
                    
                    recommendations.add(JoinRecommendation.builder()
                            .leftTable(fkTable)
                            .leftField(fkColumnName)
                            .rightTable(pkTable)
                            .rightField(pkColumnName)
                            .joinType("LEFT")
                            .recommendationType(RecommendationType.FOREIGN_KEY)
                            .confidence(1.0)
                            .reason(String.format("外键约束: %s.%s -> %s.%s", 
                                    fkTable, fkColumnName, pkTable, pkColumnName))
                            .build());
                }
            }
        }
        
        return recommendations;
    }
    
    /**
     * 检测 table_name_id 模式匹配
     * 例如: orders.user_id 匹配 users.id
     */
    private List<JoinRecommendation> detectTableNameIdPattern(
            String leftTable, List<ColumnInfo> leftColumns,
            String rightTable, List<ColumnInfo> rightColumns) {
        List<JoinRecommendation> recommendations = new ArrayList<>();
        
        // 获取右表的可能名称变体
        Set<String> rightTableVariants = getTableNameVariants(rightTable);
        
        for (ColumnInfo leftCol : leftColumns) {
            String colName = leftCol.name.toLowerCase();
            
            // 检查是否匹配 table_name_id 模式
            for (String suffix : ID_SUFFIXES) {
                if (colName.endsWith(suffix)) {
                    String prefix = colName.substring(0, colName.length() - suffix.length());
                    if (prefix.endsWith("_")) {
                        prefix = prefix.substring(0, prefix.length() - 1);
                    }
                    
                    // 检查前缀是否匹配右表名
                    if (rightTableVariants.contains(prefix.toLowerCase())) {
                        // 查找右表的主键或id字段
                        for (ColumnInfo rightCol : rightColumns) {
                            if (rightCol.isPrimaryKey || 
                                PRIMARY_KEY_NAMES.contains(rightCol.name.toLowerCase())) {
                                recommendations.add(JoinRecommendation.builder()
                                        .leftTable(leftTable)
                                        .leftField(leftCol.name)
                                        .rightTable(rightTable)
                                        .rightField(rightCol.name)
                                        .joinType("LEFT")
                                        .recommendationType(RecommendationType.FIELD_NAME_PATTERN)
                                        .confidence(0.85)
                                        .reason(String.format("字段名模式匹配: %s.%s 可能关联 %s.%s", 
                                                leftTable, leftCol.name, rightTable, rightCol.name))
                                        .build());
                                break;
                            }
                        }
                    }
                }
            }
        }
        
        return recommendations;
    }
    
    /**
     * 检测主键匹配
     * 例如: orders.customer_id 匹配 customers.id (customers 的主键)
     */
    private List<JoinRecommendation> detectPrimaryKeyMatch(
            String leftTable, List<ColumnInfo> leftColumns,
            String rightTable, List<ColumnInfo> rightColumns) {
        List<JoinRecommendation> recommendations = new ArrayList<>();
        
        // 找到右表的主键
        List<ColumnInfo> rightPrimaryKeys = rightColumns.stream()
                .filter(c -> c.isPrimaryKey)
                .collect(Collectors.toList());
        
        if (rightPrimaryKeys.isEmpty()) {
            return recommendations;
        }
        
        for (ColumnInfo leftCol : leftColumns) {
            // 跳过左表的主键
            if (leftCol.isPrimaryKey) {
                continue;
            }
            
            String leftColLower = leftCol.name.toLowerCase();
            
            for (ColumnInfo rightPk : rightPrimaryKeys) {
                String rightPkLower = rightPk.name.toLowerCase();
                
                // 检查是否是 table_pk 格式
                String expectedFkName = rightTable.toLowerCase() + "_" + rightPkLower;
                String expectedFkName2 = getSingularForm(rightTable.toLowerCase()) + "_" + rightPkLower;
                
                if (leftColLower.equals(expectedFkName) || leftColLower.equals(expectedFkName2)) {
                    recommendations.add(JoinRecommendation.builder()
                            .leftTable(leftTable)
                            .leftField(leftCol.name)
                            .rightTable(rightTable)
                            .rightField(rightPk.name)
                            .joinType("LEFT")
                            .recommendationType(RecommendationType.PRIMARY_KEY_MATCH)
                            .confidence(0.9)
                            .reason(String.format("主键匹配: %s.%s 关联 %s.%s (主键)", 
                                    leftTable, leftCol.name, rightTable, rightPk.name))
                            .build());
                }
            }
        }
        
        return recommendations;
    }
    
    /**
     * 检测相同字段名匹配
     * 例如: orders.product_code 匹配 products.product_code
     */
    private List<JoinRecommendation> detectSameFieldNameMatch(
            String leftTable, List<ColumnInfo> leftColumns,
            String rightTable, List<ColumnInfo> rightColumns) {
        List<JoinRecommendation> recommendations = new ArrayList<>();
        
        for (ColumnInfo leftCol : leftColumns) {
            for (ColumnInfo rightCol : rightColumns) {
                // 字段名完全相同
                if (leftCol.name.equalsIgnoreCase(rightCol.name)) {
                    // 跳过常见的非关联字段
                    if (isCommonNonJoinField(leftCol.name)) {
                        continue;
                    }
                    
                    // 如果是ID类字段，置信度更高
                    double confidence = isIdField(leftCol.name) ? 0.75 : 0.5;
                    
                    recommendations.add(JoinRecommendation.builder()
                            .leftTable(leftTable)
                            .leftField(leftCol.name)
                            .rightTable(rightTable)
                            .rightField(rightCol.name)
                            .joinType("INNER")
                            .recommendationType(RecommendationType.FIELD_NAME_EXACT)
                            .confidence(confidence)
                            .reason(String.format("字段名完全匹配: %s.%s = %s.%s", 
                                    leftTable, leftCol.name, rightTable, rightCol.name))
                            .build());
                }
            }
        }
        
        return recommendations;
    }
    
    /**
     * 获取表的列信息
     */
    private List<ColumnInfo> getTableColumns(DatabaseMetaData metaData, String tableName, Connection conn) 
            throws SQLException {
        List<ColumnInfo> columns = new ArrayList<>();
        Set<String> primaryKeys = new HashSet<>();
        
        String catalog = conn.getCatalog();
        
        // 获取主键
        try (ResultSet pkRs = metaData.getPrimaryKeys(catalog, null, tableName)) {
            while (pkRs.next()) {
                primaryKeys.add(pkRs.getString("COLUMN_NAME"));
            }
        }
        
        // 获取列信息
        try (ResultSet rs = metaData.getColumns(catalog, null, tableName, "%")) {
            while (rs.next()) {
                ColumnInfo col = new ColumnInfo();
                col.name = rs.getString("COLUMN_NAME");
                col.type = rs.getString("TYPE_NAME");
                col.isPrimaryKey = primaryKeys.contains(col.name);
                columns.add(col);
            }
        }
        
        return columns;
    }
    
    /**
     * 获取表名的各种变体形式
     */
    private Set<String> getTableNameVariants(String tableName) {
        Set<String> variants = new HashSet<>();
        String lower = tableName.toLowerCase();
        
        variants.add(lower);
        variants.add(getSingularForm(lower));
        
        // 处理下划线分隔的表名
        if (lower.contains("_")) {
            String[] parts = lower.split("_");
            if (parts.length > 1) {
                // 取最后一部分
                variants.add(parts[parts.length - 1]);
                variants.add(getSingularForm(parts[parts.length - 1]));
            }
        }
        
        return variants;
    }
    
    /**
     * 获取单数形式（简单实现）
     */
    private String getSingularForm(String word) {
        if (word.endsWith("ies")) {
            return word.substring(0, word.length() - 3) + "y";
        } else if (word.endsWith("es")) {
            return word.substring(0, word.length() - 2);
        } else if (word.endsWith("s") && !word.endsWith("ss")) {
            return word.substring(0, word.length() - 1);
        }
        return word;
    }
    
    /**
     * 判断是否是ID类字段
     */
    private boolean isIdField(String fieldName) {
        String lower = fieldName.toLowerCase();
        return lower.endsWith("_id") || lower.endsWith("id") || 
               lower.endsWith("_key") || lower.endsWith("_code") ||
               lower.equals("id") || lower.equals("key") || lower.equals("code");
    }
    
    /**
     * 判断是否是常见的非关联字段
     */
    private boolean isCommonNonJoinField(String fieldName) {
        String lower = fieldName.toLowerCase();
        return Set.of(
                "created_at", "updated_at", "create_time", "update_time",
                "created_by", "updated_by", "create_by", "update_by",
                "status", "state", "type", "name", "description", "remark",
                "is_deleted", "deleted", "enabled", "active",
                "version", "sort", "order", "seq"
        ).contains(lower);
    }
    
    /**
     * 去重并按置信度排序
     */
    private List<JoinRecommendation> deduplicateAndSort(List<JoinRecommendation> recommendations) {
        // 使用 Map 去重，保留置信度最高的
        Map<String, JoinRecommendation> uniqueMap = new LinkedHashMap<>();
        
        for (JoinRecommendation rec : recommendations) {
            String key = generateKey(rec);
            JoinRecommendation existing = uniqueMap.get(key);
            
            if (existing == null || rec.getConfidence() > existing.getConfidence()) {
                uniqueMap.put(key, rec);
            }
        }
        
        // 按置信度降序排序
        return uniqueMap.values().stream()
                .sorted((a, b) -> Double.compare(b.getConfidence(), a.getConfidence()))
                .collect(Collectors.toList());
    }
    
    /**
     * 生成去重键
     */
    private String generateKey(JoinRecommendation rec) {
        // 标准化键：确保 (A.x, B.y) 和 (B.y, A.x) 被视为相同
        String left = rec.getLeftTable() + "." + rec.getLeftField();
        String right = rec.getRightTable() + "." + rec.getRightField();
        
        if (left.compareTo(right) > 0) {
            return right + "|" + left;
        }
        return left + "|" + right;
    }
    
    /**
     * 列信息内部类
     */
    private static class ColumnInfo {
        String name;
        String type;
        boolean isPrimaryKey;
    }
}
