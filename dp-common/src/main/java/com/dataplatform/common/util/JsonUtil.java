package com.dataplatform.common.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.TypeReference;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * JSON工具类 - 基于FastJSON2
 * 
 * @author dataplatform
 */
@Slf4j
public class JsonUtil {

    private JsonUtil() {}

    public static String toJson(Object obj) {
        if (obj == null) return null;
        try { return JSON.toJSONString(obj); }
        catch (Exception e) { log.error("对象转JSON失败: {}", e.getMessage()); return null; }
    }

    public static String toJsonPretty(Object obj) {
        if (obj == null) return null;
        try { return JSON.toJSONString(obj, JSONWriter.Feature.PrettyFormat); }
        catch (Exception e) { log.error("对象转JSON失败: {}", e.getMessage()); return null; }
    }

    public static <T> T parseObject(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) return null;
        try { return JSON.parseObject(json, clazz); }
        catch (Exception e) { log.error("JSON转对象失败: {}", e.getMessage()); return null; }
    }

    public static <T> List<T> parseArray(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) return null;
        try { return JSON.parseArray(json, clazz); }
        catch (Exception e) { log.error("JSON转List失败: {}", e.getMessage()); return null; }
    }

    public static Map<String, Object> parseMap(String json) {
        if (json == null || json.isEmpty()) return null;
        try { return JSON.parseObject(json, new TypeReference<Map<String, Object>>() {}); }
        catch (Exception e) { log.error("JSON转Map失败: {}", e.getMessage()); return null; }
    }

    public static List<Map<String, Object>> parseMapList(String json) {
        if (json == null || json.isEmpty()) return null;
        try { return JSON.parseObject(json, new TypeReference<List<Map<String, Object>>>() {}); }
        catch (Exception e) { log.error("JSON转List<Map>失败: {}", e.getMessage()); return null; }
    }

    public static JSONObject parseJSONObject(String json) {
        if (json == null || json.isEmpty()) return null;
        try { return JSON.parseObject(json); }
        catch (Exception e) { log.error("JSON转JSONObject失败: {}", e.getMessage()); return null; }
    }

    public static JSONArray parseJSONArray(String json) {
        if (json == null || json.isEmpty()) return null;
        try { return JSON.parseArray(json); }
        catch (Exception e) { log.error("JSON转JSONArray失败: {}", e.getMessage()); return null; }
    }

    public static boolean isValidJson(String json) {
        if (json == null || json.isEmpty()) return false;
        try { JSON.parse(json); return true; }
        catch (Exception e) { return false; }
    }

    public static boolean isJsonObject(String json) {
        if (json == null || json.isEmpty()) return false;
        String trimmed = json.trim();
        return trimmed.startsWith("{") && trimmed.endsWith("}");
    }

    public static boolean isJsonArray(String json) {
        if (json == null || json.isEmpty()) return false;
        String trimmed = json.trim();
        return trimmed.startsWith("[") && trimmed.endsWith("]");
    }

    @SuppressWarnings("unchecked")
    public static <T> T deepCopy(T obj, Class<T> clazz) {
        if (obj == null) return null;
        return parseObject(toJson(obj), clazz);
    }
}
