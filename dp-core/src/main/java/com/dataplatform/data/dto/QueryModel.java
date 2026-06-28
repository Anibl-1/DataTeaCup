package com.dataplatform.data.dto;

import lombok.Data;
import java.util.List;

/**
 * 可视化查询模型
 */
@Data
public class QueryModel {
    private List<TableRef> tables;
    private List<JoinConfig> joins;
    private List<SelectField> selectFields;
    private List<WhereCondition> conditions;
    private List<String> groupBy;
    private List<OrderByField> orderBy;
    private Integer limit;

    @Data
    public static class TableRef {
        private String name;
        private String alias;
    }

    @Data
    public static class JoinConfig {
        private String type; // INNER, LEFT, RIGHT
        private String leftTable;
        private String leftField;
        private String rightTable;
        private String rightField;
    }

    @Data
    public static class SelectField {
        private String table;
        private String field;
        private String alias;
        private String aggregate; // COUNT, SUM, AVG, MAX, MIN
    }

    @Data
    public static class WhereCondition {
        private String field;
        private String operator; // =, !=, >, >=, <, <=, LIKE, IN, BETWEEN
        private Object value;
        private String logic; // AND, OR
    }

    @Data
    public static class OrderByField {
        private String field;
        private String direction; // ASC, DESC
    }
}
