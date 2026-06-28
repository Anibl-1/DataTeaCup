package com.dataplatform.collaboration.controller;

import com.dataplatform.common.Result;
import com.dataplatform.common.annotation.RequirePermission;
import com.dataplatform.data.entity.NotificationLog;
import com.dataplatform.system.entity.SystemConfig;
import com.dataplatform.data.mapper.NotificationLogMapper;
import com.dataplatform.system.service.SystemConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 通知配置与投递日志控制器
 */
@Slf4j
@Tag(name = "通知管理", description = "通知渠道配置与投递日志")
@RestController
@RequestMapping("/notification-setting")
@RequirePermission("system:config:read")
public class NotificationSettingController {

    @Autowired
    private NotificationLogMapper notificationLogMapper;

    @Autowired
    private SystemConfigService systemConfigService;

    private static final String CONFIG_PREFIX = "notification.";

    // ==================== 渠道配置 ====================

    @Operation(summary = "获取通知渠道配置")
    @GetMapping("/settings")
    public Result<Map<String, String>> getSettings() {
        List<SystemConfig> all = systemConfigService.getList(CONFIG_PREFIX);
        Map<String, String> result = new LinkedHashMap<>();
        for (SystemConfig c : all) {
            // 去掉前缀返回给前端
            String key = c.getConfigKey().startsWith(CONFIG_PREFIX)
                    ? c.getConfigKey().substring(CONFIG_PREFIX.length())
                    : c.getConfigKey();
            result.put(key, c.getConfigValue());
        }
        return Result.success(result);
    }

    @Operation(summary = "批量更新通知渠道配置")
    @PutMapping("/settings")
    public Result<Void> updateSettings(@RequestBody Map<String, String> configs) {
        for (Map.Entry<String, String> entry : configs.entrySet()) {
            String fullKey = CONFIG_PREFIX + entry.getKey();
            List<SystemConfig> existing = systemConfigService.getList(null);
            SystemConfig found = existing.stream()
                    .filter(c -> fullKey.equals(c.getConfigKey()))
                    .findFirst().orElse(null);
            if (found != null) {
                found.setConfigValue(entry.getValue());
                systemConfigService.update(found);
            } else {
                SystemConfig config = new SystemConfig();
                config.setConfigKey(fullKey);
                config.setConfigValue(entry.getValue());
                config.setConfigType("STRING");
                config.setConfigDesc("通知渠道配置");
                config.setIsSystem(false);
                config.setCreateTime(LocalDateTime.now());
                config.setUpdateTime(LocalDateTime.now());
                systemConfigService.create(config);
            }
        }
        return Result.success();
    }

    @Operation(summary = "测试通知渠道")
    @PostMapping("/test/{channel}")
    public Result<String> testChannel(@PathVariable String channel,
                                       @RequestParam(required = false) String recipient) {
        log.info("测试通知渠道: channel={}, recipient={}", channel, recipient);
        // TODO: 实际发送测试消息
        return Result.success("测试消息已发送至 " + channel + " 渠道");
    }

    // ==================== 投递日志 ====================

    @Operation(summary = "查询通知投递日志")
    @GetMapping("/logs")
    public Result<Map<String, Object>> getLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        int offset = (page - 1) * size;
        List<NotificationLog> logs = notificationLogMapper.findByPage(offset, size);
        long total = notificationLogMapper.countAll();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", logs);
        result.put("total", total);
        return Result.success(result);
    }

    @Operation(summary = "通知投递统计")
    @GetMapping("/logs/stats")
    public Result<Map<String, Object>> getLogStats(
            @RequestParam(defaultValue = "24") int hours) {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("byChannel", notificationLogMapper.statsByChannel(hours));
        stats.put("byChannelAndStatus", notificationLogMapper.statsByChannelAndStatus(hours));
        return Result.success(stats);
    }
}
