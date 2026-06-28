package com.dataplatform.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 日期函数工具类
 */
public final class DateFunctionUtil {

    private static final Logger log = LoggerFactory.getLogger(DateFunctionUtil.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DateFunctionUtil() {}

    public static String resolve(String expression) {
        if (expression == null || !expression.startsWith("$")) return expression;
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        return switch (expression) {
            case "$today" -> today.format(DATE_FMT);
            case "$yesterday" -> today.minusDays(1).format(DATE_FMT);
            case "$tomorrow" -> today.plusDays(1).format(DATE_FMT);
            case "$thisMonthStart" -> today.with(TemporalAdjusters.firstDayOfMonth()).format(DATE_FMT);
            case "$thisMonthEnd" -> today.with(TemporalAdjusters.lastDayOfMonth()).format(DATE_FMT);
            case "$lastMonthStart" -> today.minusMonths(1).with(TemporalAdjusters.firstDayOfMonth()).format(DATE_FMT);
            case "$lastMonthEnd" -> today.minusMonths(1).with(TemporalAdjusters.lastDayOfMonth()).format(DATE_FMT);
            case "$thisYearStart" -> today.with(TemporalAdjusters.firstDayOfYear()).format(DATE_FMT);
            case "$lastYearStart" -> today.minusYears(1).with(TemporalAdjusters.firstDayOfYear()).format(DATE_FMT);
            case "$lastYearEnd" -> today.minusYears(1).with(TemporalAdjusters.lastDayOfYear()).format(DATE_FMT);
            case "$last7days" -> today.minusDays(7).format(DATE_FMT);
            case "$last30days" -> today.minusDays(30).format(DATE_FMT);
            case "$lastWeekStart" -> today.minusWeeks(1).with(java.time.DayOfWeek.MONDAY).format(DATE_FMT);
            case "$lastWeekEnd" -> today.minusWeeks(1).with(java.time.DayOfWeek.SUNDAY).format(DATE_FMT);
            case "$now" -> now.format(DATETIME_FMT);
            default -> expression;
        };
    }

    @SuppressWarnings("unchecked")
    public static String mergeWithFilterParams(String dateParamsJson, String filterParamsJson) {
        Map<String, String> merged = new LinkedHashMap<>();
        if (filterParamsJson != null && !filterParamsJson.isBlank()) {
            try {
                Map<String, Object> existing = MAPPER.readValue(filterParamsJson, Map.class);
                existing.forEach((k, v) -> { if (v != null) merged.put(k, v.toString()); });
            } catch (Exception e) { log.warn("解析filterParams失败: {}", e.getMessage()); }
        }
        if (dateParamsJson != null && !dateParamsJson.isBlank()) {
            try {
                Map<String, Object> dateParams = MAPPER.readValue(dateParamsJson, Map.class);
                dateParams.forEach((k, v) -> { if (v != null) merged.put(k, resolve(v.toString())); });
            } catch (Exception e) { log.warn("解析dateParams失败: {}", e.getMessage()); }
        }
        if (merged.isEmpty()) return filterParamsJson;
        try { return MAPPER.writeValueAsString(merged); }
        catch (Exception e) { log.warn("序列化合并参数失败: {}", e.getMessage()); return filterParamsJson; }
    }
}
