package com.dataplatform.system.controller;

import com.dataplatform.common.Result;
import com.dataplatform.common.annotation.RequirePermission;
import com.dataplatform.common.annotation.RequireRole;
import com.dataplatform.data.service.LogAlertService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 鏃ュ織鏌ヨ涓庡憡璀︾鐞咥PI
 * 闇€姹? 13.6, 13.7
 */
@RestController
@RequestMapping("/logs")
@RequiredArgsConstructor
@RequireRole("admin")
public class LogQueryController {

    private final LogAlertService logAlertService;

    /** 鏃ュ織鐩綍 */
    private static final String LOG_DIR = "../runtime/logs";

    /**
     * 鏌ヨ鏃ュ織鏂囦欢鍒楄〃
     */
    @GetMapping("/files")
    public Result<List<Map<String, Object>>> listLogFiles() {
        List<Map<String, Object>> files = new ArrayList<>();
        Path logPath = Paths.get(LOG_DIR);
        if (!Files.exists(logPath)) {
            return Result.success(files);
        }
        try (Stream<Path> stream = Files.list(logPath)) {
            stream.filter(p -> p.toString().endsWith(".log"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .forEach(p -> {
                        try {
                            Map<String, Object> info = new LinkedHashMap<>();
                            info.put("name", p.getFileName().toString());
                            info.put("size", Files.size(p));
                            info.put("lastModified", Files.getLastModifiedTime(p).toMillis());
                            files.add(info);
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException e) {
            return Result.error("璇诲彇鏃ュ織鐩綍澶辫触: " + e.getMessage());
        }
        return Result.success(files);
    }

    /**
     * 鏌ヨ鏃ュ織鍐呭锛堟敮鎸佸叧閿瓧鎼滅储銆佸垎椤碉級
     */
    @GetMapping("/search")
    public Result<LogSearchResult> searchLogs(
            @RequestParam(defaultValue = "data-platform.log") String file,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String level,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "100") int pageSize) {

        pageSize = Math.min(pageSize, 500);
        Path logFile = Paths.get(LOG_DIR, file);
        if (!Files.exists(logFile)) {
            return Result.error("鏃ュ織鏂囦欢涓嶅瓨鍦? " + file);
        }

        // 瀹夊叏妫€鏌ワ細闃叉璺緞閬嶅巻
        try {
            Path normalized = logFile.toRealPath();
            Path logDir = Paths.get(LOG_DIR).toRealPath();
            if (!normalized.startsWith(logDir)) {
                return Result.error("闈炴硶鏂囦欢璺緞");
            }
        } catch (IOException e) {
            return Result.error("鏂囦欢璺緞楠岃瘉澶辫触");
        }

        try {
            List<String> allLines = Files.readAllLines(logFile);
            List<String> filtered = allLines;

            // 鎸夊叧閿瓧杩囨护
            if (keyword != null && !keyword.isEmpty()) {
                String kw = keyword.toLowerCase();
                filtered = filtered.stream()
                        .filter(line -> line.toLowerCase().contains(kw))
                        .collect(Collectors.toList());
            }

            // 鎸夋棩蹇楃骇鍒繃婊?
            if (level != null && !level.isEmpty()) {
                String lvl = level.toUpperCase();
                filtered = filtered.stream()
                        .filter(line -> line.contains(lvl))
                        .collect(Collectors.toList());
            }

            int total = filtered.size();
            int start = (page - 1) * pageSize;
            int end = Math.min(start + pageSize, total);

            List<String> pageLines = start < total ? filtered.subList(start, end) : Collections.emptyList();

            LogSearchResult result = new LogSearchResult();
            result.setLines(pageLines);
            result.setTotal(total);
            result.setPage(page);
            result.setPageSize(pageSize);
            result.setFileName(file);

            return Result.success(result);
        } catch (IOException e) {
            return Result.error("璇诲彇鏃ュ織鏂囦欢澶辫触: " + e.getMessage());
        }
    }

    /**
     * 鑾峰彇鏃ュ織鏂囦欢灏鹃儴锛堝疄鏃舵煡鐪嬶級
     */
    @GetMapping("/tail")
    public Result<List<String>> tailLog(
            @RequestParam(defaultValue = "data-platform.log") String file,
            @RequestParam(defaultValue = "200") int lines) {

        lines = Math.min(lines, 1000);
        Path logFile = Paths.get(LOG_DIR, file);
        if (!Files.exists(logFile)) {
            return Result.error("鏃ュ織鏂囦欢涓嶅瓨鍦? " + file);
        }

        try {
            Path normalized = logFile.toRealPath();
            Path logDir = Paths.get(LOG_DIR).toRealPath();
            if (!normalized.startsWith(logDir)) {
                return Result.error("Illegal file path");
            }
        } catch (IOException e) {
            return Result.error("File path validation failed");
        }

        try {
            List<String> allLines = Files.readAllLines(logFile);
            int start = Math.max(0, allLines.size() - lines);
            return Result.success(allLines.subList(start, allLines.size()));
        } catch (IOException e) {
            return Result.error("璇诲彇鏃ュ織澶辫触: " + e.getMessage());
        }
    }

    // ========== 鍛婅绠＄悊 ==========

    /**
     * 鑾峰彇鍛婅鍘嗗彶
     */
    @GetMapping("/alerts")
    public Result<List<LogAlertService.LogAlertRecord>> getAlerts(
            @RequestParam(defaultValue = "100") int limit) {
        return Result.success(logAlertService.getAlertHistory(Math.min(limit, 500)));
    }

    /**
     * 鑾峰彇鏈鐞嗗憡璀?
     */
    @GetMapping("/alerts/open")
    public Result<List<LogAlertService.LogAlertRecord>> getOpenAlerts() {
        return Result.success(logAlertService.getOpenAlerts());
    }

    /**
     * 鑾峰彇鍛婅缁熻
     */
    @GetMapping("/alerts/stats")
    public Result<Map<String, Object>> getAlertStats() {
        return Result.success(logAlertService.getAlertStats());
    }

    /**
     * 纭鍛婅
     */
    @PutMapping("/alerts/{id}/acknowledge")
    public Result<Void> acknowledgeAlert(@PathVariable long id) {
        if (logAlertService.acknowledgeAlert(id)) {
            return Result.success();
        }
        return Result.error("鍛婅涓嶅瓨鍦? " + id);
    }

    /**
     * 鍏抽棴鍛婅
     */
    @PutMapping("/alerts/{id}/close")
    public Result<Void> closeAlert(@PathVariable long id) {
        if (logAlertService.closeAlert(id)) {
            return Result.success();
        }
        return Result.error("鍛婅涓嶅瓨鍦? " + id);
    }

    /**
     * 娣诲姞鍛婅闈欓粯瑙勫垯
     */
    @PostMapping("/alerts/silence")
    public Result<Void> addSilenceRule(@RequestBody SilenceRequest request) {
        logAlertService.addSilenceRule(request.getAlertKey(), request.getUntil());
        return Result.success();
    }

    @Data
    public static class LogSearchResult {
        private List<String> lines;
        private int total;
        private int page;
        private int pageSize;
        private String fileName;
    }

    @Data
    public static class SilenceRequest {
        private String alertKey;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        private LocalDateTime until;
    }
}
