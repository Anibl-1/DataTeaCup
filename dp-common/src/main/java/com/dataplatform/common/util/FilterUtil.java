package com.dataplatform.common.util;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.dataplatform.common.dto.FilterCondition;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 筛选工具类
 */
@Slf4j
public class FilterUtil {
    
    private FilterUtil() {}
    
    public static List<FilterCondition> parseFilters(String filtersJson) {
        List<FilterCondition> filters = new ArrayList<>();
        if (StrUtil.isBlank(filtersJson)) return filters;
        try {
            filters = JSON.parseObject(filtersJson, new TypeReference<List<FilterCondition>>() {});
            if (filters == null) filters = new ArrayList<>();
        } catch (Exception e) {
            log.warn("解析筛选条件失败: {}", e.getMessage());
        }
        return filters;
    }
    
    public static String buildWhereClause(List<FilterCondition> filters, java.util.Map<String, String> columnMapping) {
        if (filters == null || filters.isEmpty()) return "";
        StringBuilder whereClause = new StringBuilder();
        boolean first = true;
        for (FilterCondition filter : filters) {
            if (StrUtil.isBlank(filter.getField()) || StrUtil.isBlank(filter.getOperator())) continue;
            String columnName = columnMapping.getOrDefault(filter.getField(), filter.getField());
            if (!first) whereClause.append(" AND ");
            first = false;
            switch (filter.getOperator()) {
                case "eq": whereClause.append(columnName).append(" = #{filters.").append(filter.getField()).append("}"); break;
                case "ne": whereClause.append(columnName).append(" != #{filters.").append(filter.getField()).append("}"); break;
                case "contains": whereClause.append(columnName).append(" LIKE CONCAT('%', #{filters.").append(filter.getField()).append("}, '%')"); break;
                case "notContains": whereClause.append(columnName).append(" NOT LIKE CONCAT('%', #{filters.").append(filter.getField()).append("}, '%')"); break;
                case "startsWith": whereClause.append(columnName).append(" LIKE CONCAT(#{filters.").append(filter.getField()).append("}, '%')"); break;
                case "endsWith": whereClause.append(columnName).append(" LIKE CONCAT('%', #{filters.").append(filter.getField()).append("})"); break;
                case "gt": whereClause.append(columnName).append(" > #{filters.").append(filter.getField()).append("}"); break;
                case "gte": whereClause.append(columnName).append(" >= #{filters.").append(filter.getField()).append("}"); break;
                case "lt": whereClause.append(columnName).append(" < #{filters.").append(filter.getField()).append("}"); break;
                case "lte": whereClause.append(columnName).append(" <= #{filters.").append(filter.getField()).append("}"); break;
                case "isNull": whereClause.append(columnName).append(" IS NULL"); break;
                case "isNotNull": whereClause.append(columnName).append(" IS NOT NULL"); break;
                default: continue;
            }
        }
        return whereClause.toString();
    }

    public static FilterWhereClause buildDynamicWhereClause(List<FilterCondition> filters) {
        if (filters == null || filters.isEmpty()) return new FilterWhereClause("", new ArrayList<>());
        List<String> conditions = new ArrayList<>();
        List<Object> parameters = new ArrayList<>();
        for (FilterCondition filter : filters) {
            if (StrUtil.isBlank(filter.getField()) || StrUtil.isBlank(filter.getOperator())) continue;
            String fieldName = filter.getField();
            if (!fieldName.matches("^[a-zA-Z0-9_]+$")) continue;
            Object value = filter.getValue();
            switch (filter.getOperator()) {
                case "eq": if (value != null) { conditions.add(fieldName + " = ?"); parameters.add(value); } break;
                case "ne": if (value != null) { conditions.add(fieldName + " != ?"); parameters.add(value); } break;
                case "contains": if (value != null) { conditions.add(fieldName + " LIKE ?"); parameters.add("%" + value + "%"); } break;
                case "notContains": if (value != null) { conditions.add(fieldName + " NOT LIKE ?"); parameters.add("%" + value + "%"); } break;
                case "startsWith": if (value != null) { conditions.add(fieldName + " LIKE ?"); parameters.add(value + "%"); } break;
                case "endsWith": if (value != null) { conditions.add(fieldName + " LIKE ?"); parameters.add("%" + value); } break;
                case "gt": if (value != null) { conditions.add(fieldName + " > ?"); parameters.add(value); } break;
                case "gte": if (value != null) { conditions.add(fieldName + " >= ?"); parameters.add(value); } break;
                case "lt": if (value != null) { conditions.add(fieldName + " < ?"); parameters.add(value); } break;
                case "lte": if (value != null) { conditions.add(fieldName + " <= ?"); parameters.add(value); } break;
                case "isNull": conditions.add(fieldName + " IS NULL"); break;
                case "isNotNull": conditions.add(fieldName + " IS NOT NULL"); break;
                default: break;
            }
        }
        String whereSql = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        return new FilterWhereClause(whereSql, parameters);
    }
    
    public static class FilterWhereClause {
        private final String whereClause;
        private final List<Object> parameters;
        public FilterWhereClause(String whereClause, List<Object> parameters) {
            this.whereClause = whereClause;
            this.parameters = parameters;
        }
        public String getWhereClause() { return whereClause; }
        public List<Object> getParameters() { return parameters; }
    }
}
