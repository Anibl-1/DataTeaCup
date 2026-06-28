package com.dataplatform.common.util;

import com.dataplatform.common.exception.BusinessException;
import com.dataplatform.common.exception.ErrorCode;
import org.springframework.util.StringUtils;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

/**
 * SQL安全工具类
 * 
 * @author dataplatform
 */
public class SqlSecurityUtil {

    private static final Set<String> FORBIDDEN_KEYWORDS = new HashSet<>(Arrays.asList(
        "DROP", "DELETE", "TRUNCATE", "ALTER", "CREATE", "INSERT", "UPDATE",
        "GRANT", "REVOKE", "EXEC", "EXECUTE", "CALL", "MERGE", "REPLACE"
    ));

    private static final Pattern COMMENT_PATTERN = Pattern.compile("(?m)(--.*$|/\\*[\\s\\S]*?\\*/|#.*$)");

    public static void validateSql(String sql) {
        if (!StringUtils.hasText(sql)) throw new BusinessException(ErrorCode.PARAM_ERROR, "SQL语句不能为空");
        sql = sql.trim();
        String sqlWithoutComments = COMMENT_PATTERN.matcher(sql).replaceAll(" ");
        String upperSql = sqlWithoutComments.toUpperCase().trim();
        if (!upperSql.startsWith("SELECT")) throw new BusinessException(ErrorCode.PARAM_ERROR, "只允许执行SELECT查询语句");
        if (sql.contains(";")) throw new BusinessException(ErrorCode.PARAM_ERROR, "SQL语句中不能包含分号");
        for (String forbidden : FORBIDDEN_KEYWORDS) {
            String regex = "\\b" + Pattern.quote(forbidden) + "\\b";
            if (Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(sql).find())
                throw new BusinessException(ErrorCode.PARAM_ERROR, "SQL语句中不允许使用关键字: " + forbidden);
        }
        if (!upperSql.contains("FROM")) throw new BusinessException(ErrorCode.PARAM_ERROR, "SELECT语句必须包含FROM子句");
    }

    public static String sanitizeSql(String sql) {
        if (!StringUtils.hasText(sql)) return "";
        sql = sql.trim();
        sql = COMMENT_PATTERN.matcher(sql).replaceAll(" ");
        sql = sql.replaceAll("\\s+", " ");
        return sql.trim();
    }

    public static String sanitizeTableName(String tableName) {
        if (!StringUtils.hasText(tableName)) throw new BusinessException(ErrorCode.PARAM_ERROR, "表名不能为空");
        String sanitized = tableName.trim().replaceAll("[^a-zA-Z0-9_.]", "");
        if (sanitized.isEmpty()) throw new BusinessException(ErrorCode.PARAM_ERROR, "表名格式不正确");
        return sanitized;
    }

    public static String sanitizeColumnName(String columnName) {
        if (!StringUtils.hasText(columnName)) throw new BusinessException(ErrorCode.PARAM_ERROR, "列名不能为空");
        String sanitized = columnName.trim().replaceAll("[^a-zA-Z0-9_.]", "");
        if (sanitized.isEmpty()) throw new BusinessException(ErrorCode.PARAM_ERROR, "列名格式不正确");
        return sanitized;
    }

    public static boolean detectSqlInjection(String input) {
        if (!StringUtils.hasText(input)) return false;
        String normalized = input;
        try { normalized = URLDecoder.decode(input, StandardCharsets.UTF_8.name()); } catch (Exception e) {}
        normalized = normalized.replaceAll("\\s+", " ");
        Pattern[] patterns = {
            Pattern.compile("(?i)\\bUNION\\b.*\\bSELECT\\b"),
            Pattern.compile("(?i)(--|#|/\\*|\\*/)"),
            Pattern.compile("'\\s*(OR|AND)\\s*'", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)\\b(SLEEP|BENCHMARK|WAITFOR|DELAY)\\b\\s*\\("),
            Pattern.compile(";\\s*(?i)(SELECT|INSERT|UPDATE|DELETE|DROP)\\b"),
        };
        for (Pattern p : patterns) { if (p.matcher(normalized).find()) return true; }
        return false;
    }

    public static String filterDangerousChars(String input) {
        if (!StringUtils.hasText(input)) return input;
        Set<Character> dangerous = new HashSet<>(Arrays.asList('\'', '"', ';', '-', '#', '/', '*', '\\', '\0'));
        StringBuilder result = new StringBuilder(input.length());
        for (char c : input.toCharArray()) { if (!dangerous.contains(c)) result.append(c); }
        return result.toString();
    }

    public static String escapeDangerousChars(String input) {
        if (!StringUtils.hasText(input)) return input;
        StringBuilder result = new StringBuilder(input.length() * 2);
        for (char c : input.toCharArray()) {
            switch (c) {
                case '\'': result.append("''"); break;
                case '"': result.append("\\\""); break;
                case '\\': result.append("\\\\"); break;
                case '\0': break;
                default: result.append(c);
            }
        }
        return result.toString();
    }

    public static String formatParameter(Object param) {
        if (param == null) return "NULL";
        if (param instanceof Number) return param.toString();
        if (param instanceof Boolean) return (Boolean) param ? "TRUE" : "FALSE";
        return "'" + escapeDangerousChars(param.toString()) + "'";
    }
}
