package com.dataplatform.data.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dataplatform.data.entity.RlsRule;
import com.dataplatform.system.entity.User;
import com.dataplatform.data.mapper.RlsRuleMapper;
import com.dataplatform.system.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 行级权限服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RlsService {

    private final RlsRuleMapper rlsRuleMapper;
    private final UserMapper userMapper;

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    /**
     * 保存 RLS 规则
     */
    @Transactional
    public RlsRule saveRule(RlsRule rule) {
        if (rule.getId() == null) {
            rule.setCreateTime(LocalDateTime.now());
            rlsRuleMapper.insert(rule);
        } else {
            rlsRuleMapper.updateById(rule);
        }
        return rule;
    }

    /**
     * 根据角色获取规则
     */
    public List<RlsRule> getRulesByRole(Long roleId) {
        LambdaQueryWrapper<RlsRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RlsRule::getRoleId, roleId)
               .orderByDesc(RlsRule::getCreateTime);
        return rlsRuleMapper.selectList(wrapper);
    }

    /**
     * 获取所有规则
     */
    public List<RlsRule> getAllRules() {
        LambdaQueryWrapper<RlsRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(RlsRule::getCreateTime);
        return rlsRuleMapper.selectList(wrapper);
    }

    /**
     * 根据数据源获取规则
     */
    public List<RlsRule> getRulesByDataSource(Long dataSourceId) {
        LambdaQueryWrapper<RlsRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RlsRule::getDataSourceId, dataSourceId)
               .orderByDesc(RlsRule::getCreateTime);
        return rlsRuleMapper.selectList(wrapper);
    }

    /**
     * 删除规则
     */
    @Transactional
    public void deleteRule(Long ruleId) {
        rlsRuleMapper.deleteById(ruleId);
    }

    /**
     * 注入 RLS 过滤条件到 SQL
     */
    public String injectRlsFilter(String originalSql, Long userId) {
        if (userId == null) {
            return originalSql;
        }

        // 获取用户信息
        User user = userMapper.selectById(userId);
        if (user == null) {
            return originalSql;
        }

        // 获取用户角色对应的 RLS 规则
        List<RlsRule> rules = getRulesForUser(userId);
        if (rules.isEmpty()) {
            return originalSql;
        }

        // 构建过滤条件
        Map<String, List<String>> tableFilters = new HashMap<>();
        
        for (RlsRule rule : rules) {
            String filter = buildFilterCondition(rule, user);
            if (filter != null) {
                tableFilters.computeIfAbsent(rule.getTableName().toLowerCase(), k -> new ArrayList<>())
                           .add(filter);
            }
        }

        if (tableFilters.isEmpty()) {
            return originalSql;
        }

        // 注入过滤条件
        return injectFiltersToSql(originalSql, tableFilters);
    }

    /**
     * 获取用户的 RLS 规则
     */
    private List<RlsRule> getRulesForUser(Long userId) {
        // 简化实现：获取用户角色对应的所有规则
        User user = userMapper.selectById(userId);
        if (user == null || user.getRoleId() == null) {
            return Collections.emptyList();
        }
        
        return getRulesByRole(user.getRoleId());
    }

    /**
     * 构建过滤条件
     */
    private String buildFilterCondition(RlsRule rule, User user) {
        String value = resolveVariables(rule.getFilterValue(), user);
        String field = rule.getFilterField();
        String operator = rule.getFilterOperator();

        switch (operator.toUpperCase()) {
            case "=":
            case "!=":
            case ">":
            case "<":
            case ">=":
            case "<=":
                if (isNumeric(value)) {
                    return field + " " + operator + " " + value;
                }
                return field + " " + operator + " '" + escapeValue(value) + "'";
            
            case "IN":
                String[] values = value.split(",");
                StringBuilder inClause = new StringBuilder(field + " IN (");
                for (int i = 0; i < values.length; i++) {
                    if (i > 0) inClause.append(", ");
                    String v = values[i].trim();
                    if (isNumeric(v)) {
                        inClause.append(v);
                    } else {
                        inClause.append("'").append(escapeValue(v)).append("'");
                    }
                }
                inClause.append(")");
                return inClause.toString();
            
            case "LIKE":
                return field + " LIKE '%" + escapeValue(value) + "%'";
            
            default:
                return field + " = '" + escapeValue(value) + "'";
        }
    }

    /**
     * 解析变量
     */
    private String resolveVariables(String value, User user) {
        Matcher matcher = VARIABLE_PATTERN.matcher(value);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String variable = matcher.group(1);
            String replacement = getVariableValue(variable, user);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        
        return result.toString();
    }

    /**
     * 获取变量值
     */
    private String getVariableValue(String variable, User user) {
        switch (variable.toLowerCase()) {
            case "user.id":
                return String.valueOf(user.getId());
            case "user.username":
                return user.getUsername();
            case "user.deptid":
            case "user.departmentid":
                return user.getDeptId() != null ? String.valueOf(user.getDeptId()) : "";
            case "user.roleid":
                return user.getRoleId() != null ? String.valueOf(user.getRoleId()) : "";
            default:
                return variable;
        }
    }

    /**
     * 注入过滤条件到 SQL
     */
    private String injectFiltersToSql(String sql, Map<String, List<String>> tableFilters) {
        String upperSql = sql.toUpperCase();
        
        // 查找 WHERE 子句位置
        int whereIndex = upperSql.indexOf(" WHERE ");
        int groupByIndex = upperSql.indexOf(" GROUP BY ");
        int orderByIndex = upperSql.indexOf(" ORDER BY ");
        int limitIndex = upperSql.indexOf(" LIMIT ");
        
        // 构建过滤条件
        StringBuilder filterClause = new StringBuilder();
        for (Map.Entry<String, List<String>> entry : tableFilters.entrySet()) {
            for (String filter : entry.getValue()) {
                if (filterClause.length() > 0) {
                    filterClause.append(" AND ");
                }
                filterClause.append("(").append(filter).append(")");
            }
        }
        
        if (filterClause.length() == 0) {
            return sql;
        }
        
        // 注入过滤条件
        if (whereIndex > 0) {
            // 已有 WHERE，添加 AND
            int insertPos = whereIndex + 7;
            return sql.substring(0, insertPos) + "(" + filterClause + ") AND " + sql.substring(insertPos);
        } else {
            // 没有 WHERE，添加 WHERE
            int insertPos = sql.length();
            if (groupByIndex > 0) insertPos = Math.min(insertPos, groupByIndex);
            if (orderByIndex > 0) insertPos = Math.min(insertPos, orderByIndex);
            if (limitIndex > 0) insertPos = Math.min(insertPos, limitIndex);
            
            return sql.substring(0, insertPos) + " WHERE " + filterClause + sql.substring(insertPos);
        }
    }

    private boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String escapeValue(String value) {
        return value.replace("'", "''");
    }
}
