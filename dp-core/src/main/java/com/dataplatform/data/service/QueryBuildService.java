package com.dataplatform.data.service;

import com.dataplatform.data.dto.QueryModel;
import com.dataplatform.data.dto.QueryModel.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 可视化查询构建服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueryBuildService {

    private final DataSourceService dataSourceService;

    /**
     * 根据查询模型生成 SQL
     */
    public String generateSql(QueryModel model) {
        StringBuilder sql = new StringBuilder();
        
        // SELECT
        sql.append("SELECT ");
        if (model.getSelectFields() == null || model.getSelectFields().isEmpty()) {
            sql.append("*");
        } else {
            String selectClause = model.getSelectFields().stream()
                .map(this::buildSelectField)
                .collect(Collectors.joining(", "));
            sql.append(selectClause);
        }
        
        // FROM
        sql.append("\nFROM ");
        if (model.getTables() != null && !model.getTables().isEmpty()) {
            TableRef mainTable = model.getTables().get(0);
            sql.append(mainTable.getName());
            if (mainTable.getAlias() != null && !mainTable.getAlias().isEmpty()) {
                sql.append(" AS ").append(mainTable.getAlias());
            }
        }
        
        // JOIN
        if (model.getJoins() != null && !model.getJoins().isEmpty()) {
            for (JoinConfig join : model.getJoins()) {
                sql.append("\n").append(join.getType()).append(" JOIN ");
                sql.append(join.getRightTable());
                sql.append(" ON ").append(join.getLeftTable()).append(".").append(join.getLeftField());
                sql.append(" = ").append(join.getRightTable()).append(".").append(join.getRightField());
            }
        }
        
        // WHERE
        if (model.getConditions() != null && !model.getConditions().isEmpty()) {
            sql.append("\nWHERE ");
            for (int i = 0; i < model.getConditions().size(); i++) {
                WhereCondition cond = model.getConditions().get(i);
                if (i > 0) {
                    sql.append(" ").append(cond.getLogic() != null ? cond.getLogic() : "AND").append(" ");
                }
                sql.append(buildWhereCondition(cond));
            }
        }
        
        // GROUP BY
        if (model.getGroupBy() != null && !model.getGroupBy().isEmpty()) {
            sql.append("\nGROUP BY ").append(String.join(", ", model.getGroupBy()));
        }
        
        // ORDER BY
        if (model.getOrderBy() != null && !model.getOrderBy().isEmpty()) {
            String orderClause = model.getOrderBy().stream()
                .map(o -> o.getField() + " " + o.getDirection())
                .collect(Collectors.joining(", "));
            sql.append("\nORDER BY ").append(orderClause);
        }
        
        // LIMIT
        if (model.getLimit() != null && model.getLimit() > 0) {
            sql.append("\nLIMIT ").append(model.getLimit());
        }
        
        return sql.toString();
    }

    /**
     * 解析 SQL 为查询模型（简化实现）
     */
    public QueryModel parseSql(String sql) {
        QueryModel model = new QueryModel();
        model.setTables(new ArrayList<>());
        model.setJoins(new ArrayList<>());
        model.setSelectFields(new ArrayList<>());
        model.setConditions(new ArrayList<>());
        model.setGroupBy(new ArrayList<>());
        model.setOrderBy(new ArrayList<>());
        
        // 简化解析：提取表名
        String upperSql = sql.toUpperCase();
        int fromIndex = upperSql.indexOf("FROM");
        if (fromIndex > 0) {
            int whereIndex = upperSql.indexOf("WHERE");
            int joinIndex = upperSql.indexOf("JOIN");
            int endIndex = whereIndex > 0 ? whereIndex : (joinIndex > 0 ? joinIndex : sql.length());
            
            String tablesPart = sql.substring(fromIndex + 4, endIndex).trim();
            String[] tables = tablesPart.split(",");
            for (String table : tables) {
                TableRef ref = new TableRef();
                String[] parts = table.trim().split("\\s+");
                ref.setName(parts[0].trim());
                if (parts.length > 1 && !"AS".equalsIgnoreCase(parts[1])) {
                    ref.setAlias(parts[1].trim());
                } else if (parts.length > 2) {
                    ref.setAlias(parts[2].trim());
                }
                model.getTables().add(ref);
            }
        }
        
        return model;
    }

    /**
     * 预览查询结果
     */
    public List<Map<String, Object>> previewQuery(Long dataSourceId, QueryModel model, int limit) {
        String sql = generateSql(model);
        
        // 确保有 LIMIT
        if (model.getLimit() == null || model.getLimit() <= 0) {
            sql += "\nLIMIT " + Math.min(limit, 100);
        }
        
        return executeQuery(dataSourceId, sql);
    }

    /**
     * 执行 SQL 查询（仅允许 SELECT 语句）
     */
    public List<Map<String, Object>> executeQuery(Long dataSourceId, String sql) {
        // 安全校验：去掉注释后检查
        String cleanSql = sql.replaceAll("--[^\r\n]*", "")
                             .replaceAll("/\\*[\\s\\S]*?\\*/", "")
                             .trim();
        String upperSql = cleanSql.toUpperCase();
        if (!upperSql.startsWith("SELECT")) {
            throw new RuntimeException("安全限制：仅支持 SELECT 查询语句");
        }
        // 去掉字符串内容后检查危险关键字
        String sqlWithoutStrings = upperSql.replaceAll("'[^']*'", "''").replaceAll("\"[^\"]*\"", "\"\"");
        String[] dangerousKeywords = {"DROP", "DELETE", "UPDATE", "INSERT", "TRUNCATE", "ALTER",
            "CREATE", "GRANT", "REVOKE", "EXEC", "EXECUTE", "INTO OUTFILE", "INTO DUMPFILE",
            "LOAD_FILE", "BENCHMARK", "SLEEP", "WAITFOR"};
        for (String keyword : dangerousKeywords) {
            String pattern = "(?<![A-Z0-9_])" + keyword + "(?![A-Z0-9_])";
            if (java.util.regex.Pattern.compile(pattern).matcher(sqlWithoutStrings).find()) {
                throw new RuntimeException("安全限制：禁止执行危险操作（" + keyword + "）");
            }
        }
        try {
            DataSource ds = dataSourceService.getDataSource(dataSourceId);
            JdbcTemplate jdbcTemplate = new JdbcTemplate(ds);
            return jdbcTemplate.queryForList(cleanSql);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Query execution failed", e);
            throw new RuntimeException("查询执行失败: " + e.getMessage());
        }
    }

    /**
     * 获取表元数据
     */
    public List<TableMeta> getTableMeta(Long dataSourceId) {
        List<TableMeta> tables = new ArrayList<>();
        
        try {
            DataSource ds = dataSourceService.getDataSource(dataSourceId);
            try (Connection conn = ds.getConnection()) {
                DatabaseMetaData metaData = conn.getMetaData();
                String catalog = conn.getCatalog();
                
                // 获取所有表
                try (ResultSet rs = metaData.getTables(catalog, null, "%", new String[]{"TABLE", "VIEW"})) {
                    while (rs.next()) {
                        String tableName = rs.getString("TABLE_NAME");
                        TableMeta table = new TableMeta();
                        table.setTableName(tableName);
                        table.setColumns(getColumnMeta(metaData, catalog, tableName));
                        tables.add(table);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to get table meta", e);
        }
        
        return tables;
    }

    /**
     * 仅获取表名列表（轻量接口，不查列信息）
     */
    public List<String> getTableNames(Long dataSourceId) {
        List<String> tableNames = new ArrayList<>();
        try {
            DataSource ds = dataSourceService.getDataSource(dataSourceId);
            try (Connection conn = ds.getConnection()) {
                DatabaseMetaData metaData = conn.getMetaData();
                String catalog = conn.getCatalog();
                try (ResultSet rs = metaData.getTables(catalog, null, "%", new String[]{"TABLE", "VIEW"})) {
                    while (rs.next()) {
                        tableNames.add(rs.getString("TABLE_NAME"));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to get table names", e);
        }
        return tableNames;
    }

    /**
     * 获取单个表的列元数据（懒加载用）
     */
    public TableMeta getSingleTableMeta(Long dataSourceId, String tableName) {
        TableMeta table = new TableMeta();
        table.setTableName(tableName);
        try {
            DataSource ds = dataSourceService.getDataSource(dataSourceId);
            try (Connection conn = ds.getConnection()) {
                DatabaseMetaData metaData = conn.getMetaData();
                String catalog = conn.getCatalog();
                table.setColumns(getColumnMeta(metaData, catalog, tableName));
            }
        } catch (Exception e) {
            log.error("Failed to get column meta for table: {}", tableName, e);
            table.setColumns(new ArrayList<>());
        }
        return table;
    }


    private List<ColumnMeta> getColumnMeta(DatabaseMetaData metaData, String catalog, String tableName) throws SQLException {
        List<ColumnMeta> columns = new ArrayList<>();
        Set<String> primaryKeys = new HashSet<>();
        
        // 获取主键
        try (ResultSet pkRs = metaData.getPrimaryKeys(catalog, null, tableName)) {
            while (pkRs.next()) {
                primaryKeys.add(pkRs.getString("COLUMN_NAME"));
            }
        }
        
        // 获取列信息
        try (ResultSet rs = metaData.getColumns(catalog, null, tableName, "%")) {
            while (rs.next()) {
                ColumnMeta col = new ColumnMeta();
                col.setName(rs.getString("COLUMN_NAME"));
                col.setType(rs.getString("TYPE_NAME"));
                col.setComment(rs.getString("REMARKS"));
                col.setNullable("YES".equals(rs.getString("IS_NULLABLE")));
                col.setPrimaryKey(primaryKeys.contains(col.getName()));
                columns.add(col);
            }
        }
        
        return columns;
    }

    private String buildSelectField(SelectField field) {
        StringBuilder sb = new StringBuilder();
        
        if (field.getAggregate() != null) {
            sb.append(field.getAggregate()).append("(");
        }
        
        if (field.getTable() != null && !field.getTable().isEmpty()) {
            sb.append(field.getTable()).append(".");
        }
        sb.append(field.getField());
        
        if (field.getAggregate() != null) {
            sb.append(")");
        }
        
        if (field.getAlias() != null && !field.getAlias().isEmpty()) {
            sb.append(" AS ").append(field.getAlias());
        }
        
        return sb.toString();
    }

    private String buildWhereCondition(WhereCondition cond) {
        String field = cond.getField();
        String op = cond.getOperator();
        Object value = cond.getValue();
        
        switch (op) {
            case "LIKE":
                return field + " LIKE '%" + value + "%'";
            case "IN":
                if (value instanceof List) {
                    String inValues = ((List<?>) value).stream()
                        .map(v -> "'" + v + "'")
                        .collect(Collectors.joining(", "));
                    return field + " IN (" + inValues + ")";
                }
                return field + " IN (" + value + ")";
            case "BETWEEN":
                if (value instanceof List && ((List<?>) value).size() >= 2) {
                    List<?> range = (List<?>) value;
                    return field + " BETWEEN '" + range.get(0) + "' AND '" + range.get(1) + "'";
                }
                return field + " = '" + value + "'";
            default:
                if (value instanceof Number) {
                    return field + " " + op + " " + value;
                }
                return field + " " + op + " '" + value + "'";
        }
    }

    // 内部类
    public static class TableMeta {
        private String tableName;
        private List<ColumnMeta> columns;
        
        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }
        public List<ColumnMeta> getColumns() { return columns; }
        public void setColumns(List<ColumnMeta> columns) { this.columns = columns; }
    }

    public static class ColumnMeta {
        private String name;
        private String type;
        private String comment;
        private boolean nullable;
        private boolean primaryKey;
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
        public boolean isNullable() { return nullable; }
        public void setNullable(boolean nullable) { this.nullable = nullable; }
        public boolean isPrimaryKey() { return primaryKey; }
        public void setPrimaryKey(boolean primaryKey) { this.primaryKey = primaryKey; }
    }
}
