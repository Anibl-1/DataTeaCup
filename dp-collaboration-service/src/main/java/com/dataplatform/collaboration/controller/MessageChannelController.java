package com.dataplatform.collaboration.controller;

import com.dataplatform.common.annotation.RequirePermission;
import com.dataplatform.common.Result;
import com.dataplatform.data.entity.MessageChannel;
import com.dataplatform.data.service.MessageChannelService;
import com.dataplatform.common.annotation.OperationLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 消息通道配置Controller
 * 统一管理企业微信、钉钉、邮件等消息通道
 */
@Slf4j
@RestController
@RequestMapping("/message-channel")
@RequiredArgsConstructor
@RequirePermission("system:config:read")
public class MessageChannelController {

    private final MessageChannelService channelService;

    /**
     * 获取所有通道配置
     */
    @GetMapping("/list")
    public Result<List<MessageChannel>> list() {
        List<MessageChannel> channels = channelService.findAll();
        // 隐藏敏感配置信息（密码等）
        channels.forEach(this::maskSensitiveConfig);
        return Result.success(channels);
    }

    /**
     * 按类型获取通道配置（用于下拉选择）
     */
    @GetMapping("/by-type/{type}")
    public Result<List<MessageChannel>> listByType(@PathVariable String type) {
        List<MessageChannel> channels = channelService.findByType(type);
        channels.forEach(this::maskSensitiveConfig);
        return Result.success(channels);
    }

    /**
     * 获取所有启用的通道（按类型分组，用于选择器）
     */
    @GetMapping("/enabled")
    public Result<Map<String, List<MessageChannel>>> listEnabled() {
        List<MessageChannel> channels = channelService.findEnabled();
        channels.forEach(this::maskSensitiveConfig);
        Map<String, List<MessageChannel>> grouped = channels.stream()
                .collect(Collectors.groupingBy(MessageChannel::getChannelType));
        return Result.success(grouped);
    }

    /**
     * 获取单个通道配置详情
     */
    @GetMapping("/{id}")
    public Result<MessageChannel> getById(@PathVariable Long id) {
        MessageChannel channel = channelService.findById(id);
        if (channel == null) {
            return Result.error(404, "通道配置不存在");
        }
        // 编辑时不隐藏配置
        return Result.success(channel);
    }

    /**
     * 创建通道配置
     */
    @RequirePermission("system:config")
    @OperationLog(module = "消息通道", type = OperationLog.OperationType.CREATE, description = "创建消息通道配置")
    @PostMapping
    public Result<MessageChannel> create(@RequestBody MessageChannel channel) {
        if (channel.getChannelName() == null || channel.getChannelName().isBlank()) {
            return Result.error(400, "通道名称不能为空");
        }
        if (channel.getChannelType() == null || channel.getChannelType().isBlank()) {
            return Result.error(400, "通道类型不能为空");
        }
        if (!isValidChannelType(channel.getChannelType())) {
            return Result.error(400, "不支持的通道类型: " + channel.getChannelType());
        }
        MessageChannel created = channelService.create(channel);
        return Result.success(created);
    }

    /**
     * 更新通道配置
     */
    @RequirePermission("system:config")
    @OperationLog(module = "消息通道", type = OperationLog.OperationType.UPDATE, description = "更新消息通道配置")
    @PutMapping("/{id}")
    public Result<MessageChannel> update(@PathVariable Long id, @RequestBody MessageChannel channel) {
        channel.setId(id);
        MessageChannel updated = channelService.update(channel);
        return Result.success(updated);
    }

    /**
     * 删除通道配置
     */
    @RequirePermission("system:config")
    @OperationLog(module = "消息通道", type = OperationLog.OperationType.DELETE, description = "删除消息通道配置")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        channelService.delete(id);
        return Result.success(null);
    }

    /**
     * 设置为默认通道
     */
    @RequirePermission("system:config")
    @OperationLog(module = "消息通道", type = OperationLog.OperationType.UPDATE, description = "设置默认通道")
    @PostMapping("/{id}/set-default")
    public Result<Void> setDefault(@PathVariable Long id) {
        channelService.setDefault(id);
        return Result.success(null);
    }

    /**
     * 测试通道配置
     */
    @PostMapping("/{id}/test")
    public Result<String> testChannel(@PathVariable Long id, @RequestBody Map<String, String> params) {
        String recipient = params.get("recipient");
        String content = params.get("content");
        if (recipient == null || recipient.isBlank()) {
            return Result.error(400, "请提供测试接收人");
        }
        try {
            boolean success = channelService.testChannel(id, recipient, content);
            return success ? Result.success("测试消息发送成功") : Result.error(500, "测试消息发送失败");
        } catch (Exception e) {
            return Result.error(500, "测试失败: " + e.getMessage());
        }
    }

    // ==================== 工具方法 ====================

    private boolean isValidChannelType(String type) {
        return "email".equals(type) || "wecom".equals(type) || 
               "dingtalk".equals(type) || "sms".equals(type);
    }

    private static final java.util.Set<String> SENSITIVE_KEYS = java.util.Set.of(
            "password", "secret", "appSecret", "secretKey", "apiSecret", "token", "accessKey"
    );

    private void maskSensitiveConfig(MessageChannel channel) {
        if (channel.getConfig() == null || channel.getConfig().isBlank()) return;
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> map = om.readValue(channel.getConfig(), java.util.Map.class);
            for (String key : map.keySet()) {
                if (SENSITIVE_KEYS.contains(key) && map.get(key) instanceof String) {
                    String val = (String) map.get(key);
                    if (val.length() > 4) {
                        map.put(key, val.substring(0, 2) + "****" + val.substring(val.length() - 2));
                    } else {
                        map.put(key, "****");
                    }
                }
            }
            channel.setConfig(om.writeValueAsString(map));
        } catch (Exception e) {
            // JSON解析失败时不暴露原始配置
            channel.setConfig("{}");
        }
    }
}
