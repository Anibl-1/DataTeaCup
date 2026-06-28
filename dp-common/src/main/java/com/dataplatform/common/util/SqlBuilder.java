package com.dataplatform.common.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * SQL语句动态构建工具类
 */
public class SqlBuilder {

    private StringBuilder sql;
    private List<Object> parameters;

    public SqlBuilder() { this.sql = new StringBuilder(); this.parameters = new ArrayList<>(); }

    public SqlBuilder select(String... columns) {
        sql.append("SELECT ");
        sql.append(columns == null || columns.length == 0 ? "* " : String.join(", ", columns) + " ");
        return this;
    }

    public SqlBuilder from(String table) { sql.append("FROM ").append(sanitizeTableName(table)).append(" "); return this; }
    public SqlBuilder where(String condition) { sql.append("WHERE ").append(condition).append(" "); return this; }
    public SqlBuilder and(String condition) { sql.append("AND ").append(condition).append(" "); return this; }
    public SqlBuilder or(String condition) { sql.append("OR ").append(condition).append(" "); return this; }
    public SqlBuilder orderBy(String column, String direction) { sql.append("ORDER BY ").append(sanitizeColumnName(column)).append(" ").append(direction).append(" "); return this; }
    public SqlBuilder limit(int offset, int pageSize) { sql.append("LIMIT ").append(offset).append(", ").append(pageSize).append(" "); return this; }

    public SqlBuilder insertInto(String table, Map<String, Object> data) {
        sql.append("INSERT INTO ").append(sanitizeTableName(table)).append(" (");
        List<String> columns = new ArrayList<>(data.keySet());
        sql.append(String.join(", ", columns)).append(") VALUES (");
        List<String> placeholders = new ArrayList<>();
        for (String column : columns) { placeholders.add("?"); parameters.add(data.get(column)); }
        sql.append(String.join(", ", placeholders)).append(") ");
        return this;
    }

    public SqlBuilder update(String table) { sql.append("UPDATE ").append(sanitizeTableName(table)).append(" SET "); return this; }

    public SqlBuilder set(Map<String, Object> data) {
        List<String> setPairs = new ArrayList<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) { setPairs.add(sanitizeColumnName(entry.getKey()) + " = ?"); parameters.add(entry.getValue()); }
        sql.append(String.join(", ", setPairs)).append(" ");
        return this;
    }

    public SqlBuilder deleteFrom(String table) { sql.append("DELETE FROM ").append(sanitizeTableName(table)).append(" "); return this; }
    public SqlBuilder join(String table, String condition) { sql.append("INNER JOIN ").append(sanitizeTableName(table)).append(" ON ").append(condition).append(" "); return this; }
    public SqlBuilder leftJoin(String table, String condition) { sql.append("LEFT JOIN ").append(sanitizeTableName(table)).append(" ON ").append(condition).append(" "); return this; }
    public SqlBuilder groupBy(String... columns) { sql.append("GROUP BY ").append(String.join(", ", columns)).append(" "); return this; }
    public SqlBuilder having(String condition) { sql.append("HAVING ").append(condition).append(" "); return this; }
    public SqlBuilder addParameter(Object parameter) { parameters.add(parameter); return this; }
    public String build() { return sql.toString().trim(); }
    public List<Object> getParameters() { return parameters; }
    public SqlBuilder clear() { sql = new StringBuilder(); parameters.clear(); return this; }

    private String sanitizeTableName(String tableName) {
        if (tableName == null || tableName.trim().isEmpty()) throw new IllegalArgumentException("表名不能为空");
        if (!tableName.matches("^[a-zA-Z0-9_]+$")) throw new IllegalArgumentException("非法的表名: " + tableName);
        return tableName;
    }

    private String sanitizeColumnName(String columnName) {
        if (columnName == null || columnName.trim().isEmpty()) throw new IllegalArgumentException("列名不能为空");
        if (!columnName.matches("^[a-zA-Z0-9_.]+$")) throw new IllegalArgumentException("非法的列名: " + columnName);
        return columnName;
    }

    public static String buildPageSql(String baseSql, int page, int pageSize) {
        return baseSql + " LIMIT " + ((page - 1) * pageSize) + ", " + pageSize;
    }

    public static String buildCountSql(String baseSql) {
        String countSql = baseSql.replaceAll("(?i)ORDER\\s+BY\\s+[^;]+", "");
        return "SELECT COUNT(*) FROM (" + countSql + ") AS count_table";
    }

    public static class WhereBuilder {
        private List<String> conditions = new ArrayList<>();
        private List<Object> parameters = new ArrayList<>();

        public WhereBuilder eq(String column, Object value) { if (value != null) { conditions.add(column + " = ?"); parameters.add(value); } return this; }
        public WhereBuilder ne(String column, Object value) { if (value != null) { conditions.add(column + " != ?"); parameters.add(value); } return this; }
        public WhereBuilder like(String column, String value) { if (value != null && !value.trim().isEmpty()) { conditions.add(column + " LIKE ?"); parameters.add("%" + value + "%"); } return this; }
        public WhereBuilder in(String column, List<?> values) { if (values != null && !values.isEmpty()) { List<String> p = new ArrayList<>(); for (Object v : values) { p.add("?"); parameters.add(v); } conditions.add(column + " IN (" + String.join(", ", p) + ")"); } return this; }
        public WhereBuilder between(String column, Object start, Object end) { if (start != null && end != null) { conditions.add(column + " BETWEEN ? AND ?"); parameters.add(start); parameters.add(end); } return this; }
        public WhereBuilder isNull(String column) { conditions.add(column + " IS NULL"); return this; }
        public WhereBuilder isNotNull(String column) { conditions.add(column + " IS NOT NULL"); return this; }
        public String buildWhere() { return conditions.isEmpty() ? "" : "WHERE " + String.join(" AND ", conditions); }
        public List<Object> getParameters() { return parameters; }
    }
}
