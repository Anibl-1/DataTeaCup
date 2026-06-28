package com.dataplatform.common.util;

import java.util.regex.Pattern;

/**
 * 数据验证工具类
 */
public class DataValidator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{4,20}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d@$!%*?&]{8,20}$");
    private static final Pattern IP_PATTERN = Pattern.compile("^((25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)$");
    private static final Pattern TABLE_NAME_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]{0,63}$");
    private static final Pattern COLUMN_NAME_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]{0,63}$");

    public static boolean isValidEmail(String email) { return email != null && !email.trim().isEmpty() && EMAIL_PATTERN.matcher(email.trim()).matches(); }
    public static boolean isValidPhone(String phone) { return phone != null && !phone.trim().isEmpty() && PHONE_PATTERN.matcher(phone.trim()).matches(); }
    public static boolean isValidUsername(String username) { return username != null && !username.trim().isEmpty() && USERNAME_PATTERN.matcher(username.trim()).matches(); }
    public static boolean isValidPassword(String password) { return password != null && !password.trim().isEmpty() && PASSWORD_PATTERN.matcher(password).matches(); }
    public static boolean isValidIP(String ip) { return ip != null && !ip.trim().isEmpty() && IP_PATTERN.matcher(ip.trim()).matches(); }
    public static boolean isValidPort(Integer port) { return port != null && port > 0 && port <= 65535; }
    public static boolean isValidTableName(String tableName) { return tableName != null && !tableName.trim().isEmpty() && TABLE_NAME_PATTERN.matcher(tableName.trim()).matches(); }
    public static boolean isValidColumnName(String columnName) { return columnName != null && !columnName.trim().isEmpty() && COLUMN_NAME_PATTERN.matcher(columnName.trim()).matches(); }
    public static boolean isNotEmpty(String str) { return str != null && !str.trim().isEmpty(); }
    public static boolean isValidLength(String str, int minLength, int maxLength) { return str != null && str.length() >= minLength && str.length() <= maxLength; }
    public static boolean isInRange(Number value, Number min, Number max) { return value != null && value.doubleValue() >= min.doubleValue() && value.doubleValue() <= max.doubleValue(); }
    public static boolean isAlphanumeric(String str) { return str != null && !str.isEmpty() && str.matches("^[a-zA-Z0-9]+$"); }
    public static boolean containsChinese(String str) { return str != null && !str.isEmpty() && str.matches(".*[\u4e00-\u9fa5].*"); }
    public static boolean isValidUrl(String url) { return url != null && !url.trim().isEmpty() && url.matches("^(http|https)://[a-zA-Z0-9.-]+.*$"); }
    public static boolean isValidHost(String host) { return host != null && !host.trim().isEmpty() && (isValidIP(host) || host.matches("^[a-zA-Z0-9.-]+$")); }
    public static String sanitizeSqlInput(String input) { return input == null ? null : input.replaceAll("[';\\-\\-\\/\\*]", ""); }

    public static boolean isValidJson(String json) {
        if (json == null || json.trim().isEmpty()) return true;
        json = json.trim();
        return (json.startsWith("{") && json.endsWith("}")) || (json.startsWith("[") && json.endsWith("]"));
    }

    public static int getPasswordStrength(String password) {
        if (password == null || password.isEmpty()) return 0;
        int strength = 0;
        if (password.length() >= 8) strength++;
        if (password.length() >= 12) strength++;
        if (password.matches(".*[a-z].*")) strength++;
        if (password.matches(".*[A-Z].*")) strength++;
        if (password.matches(".*\\d.*")) strength++;
        if (password.matches(".*[@$!%*?&].*")) strength++;
        if (strength <= 2) return 0;
        if (strength <= 4) return 1;
        return 2;
    }
}
