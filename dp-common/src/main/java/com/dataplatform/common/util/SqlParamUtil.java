package com.dataplatform.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL 自定义参数 ${} 工具类
 */
public final class SqlParamUtil {

    private static final Logger log = LoggerFactory.getLogger(SqlParamUtil.class);
    public static final Pattern CUSTOM_PARAM_PATTERN = Pattern.compile("\\$\\{([\\w\\u4e00-\\u9fa5]+)\\}");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private SqlParamUtil() {}

    @SuppressWarnings("unchecked")
    public static Map<String, String> parseParamsJson(String paramsJson) {
        Map<String, String> result = new LinkedHashMap<>();
        if (paramsJson == null || paramsJson.isBlank()) return result;
        try {
            Map<String, Object> map = MAPPER.readValue(paramsJson, Map.class);
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (entry.getValue() != null) result.put(entry.getKey(), entry.getValue().toString());
            }
        } catch (Exception e) { log.warn("解析自定义参数失败: {}", e.getMessage()); }
        return result;
    }

    public static Object[] replaceCustomParams(String sql, Map<String, String> paramValues) {
        List<Object> params = new ArrayList<>();
        Matcher matcher = CUSTOM_PARAM_PATTERN.matcher(sql);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String paramName = matcher.group(1);
            matcher.appendReplacement(sb, "?");
            params.add(paramValues.get(paramName));
        }
        matcher.appendTail(sb);
        return new Object[]{sb.toString(), params};
    }

    public static String stripCustomParamsForTest(String sql) {
        return CUSTOM_PARAM_PATTERN.matcher(sql).replaceAll("NULL");
    }

    public static boolean hasCustomParams(String sql) {
        return sql != null && CUSTOM_PARAM_PATTERN.matcher(sql).find();
    }
}
