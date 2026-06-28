package com.dataplatform.data.service;

import com.dataplatform.data.entity.MessageChannel;
import com.dataplatform.data.mapper.MessageChannelMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 消息通道配置服务
 * 提供通道配置的CRUD和获取配置项的方法
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageChannelService {

    private final MessageChannelMapper channelMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<MessageChannel> findAll() {
        return channelMapper.findAll();
    }

    public MessageChannel findById(Long id) {
        return channelMapper.findById(id);
    }

    public List<MessageChannel> findByType(String channelType) {
        return channelMapper.findByType(channelType);
    }

    public List<MessageChannel> findEnabled() {
        return channelMapper.findEnabled();
    }

    /**
     * 获取指定类型的默认通道配置
     */
    public MessageChannel getDefaultChannel(String channelType) {
        return channelMapper.findDefaultByType(channelType);
    }

    /**
     * 获取通道配置（优先指定ID，否则使用默认）
     */
    public MessageChannel getChannel(Long channelId, String channelType) {
        if (channelId != null) {
            MessageChannel channel = channelMapper.findById(channelId);
            if (channel != null && channel.getStatus() == 1) {
                return channel;
            }
        }
        return getDefaultChannel(channelType);
    }

    /**
     * 解析通道配置为Map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> parseConfig(MessageChannel channel) {
        if (channel == null || channel.getConfig() == null) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(channel.getConfig(), Map.class);
        } catch (Exception e) {
            log.warn("解析通道配置失败: {}", e.getMessage());
            return Map.of();
        }
    }

    /**
     * 获取指定通道的配置项值
     */
    public String getConfigValue(Long channelId, String channelType, String key) {
        MessageChannel channel = getChannel(channelId, channelType);
        Map<String, Object> config = parseConfig(channel);
        Object value = config.get(key);
        return value != null ? value.toString() : null;
    }
    
    /**
     * 直接获取通道配置Map（通过channelId）
     */
    public Map<String, Object> getChannelConfig(Long channelId) {
        if (channelId == null) {
            return null;
        }
        MessageChannel channel = channelMapper.findById(channelId);
        if (channel == null || channel.getStatus() != 1) {
            return null;
        }
        return parseConfig(channel);
    }

    @Transactional
    public MessageChannel create(MessageChannel channel) {
        if (channel.getStatus() == null) channel.setStatus(1);
        if (channel.getIsDefault() == null) channel.setIsDefault(0);
        
        // 如果是第一个该类型的配置，自动设为默认
        if (channelMapper.countByType(channel.getChannelType()) == 0) {
            channel.setIsDefault(1);
        }
        
        // 如果设为默认，清除其他默认
        if (channel.getIsDefault() == 1) {
            channelMapper.clearDefaultByType(channel.getChannelType(), -1L);
        }
        
        channelMapper.insert(channel);
        return channel;
    }

    @Transactional
    public MessageChannel update(MessageChannel channel) {
        MessageChannel existing = channelMapper.findById(channel.getId());
        if (existing == null) {
            throw new RuntimeException("通道配置不存在");
        }
        
        // 如果设为默认，清除其他默认
        if (channel.getIsDefault() != null && channel.getIsDefault() == 1) {
            channelMapper.clearDefaultByType(existing.getChannelType(), channel.getId());
        }
        
        channelMapper.update(channel);
        return channelMapper.findById(channel.getId());
    }

    @Transactional
    public void delete(Long id) {
        MessageChannel channel = channelMapper.findById(id);
        if (channel == null) return;
        
        channelMapper.deleteById(id);
        
        // 如果删除的是默认配置，自动设置另一个为默认
        if (channel.getIsDefault() == 1) {
            List<MessageChannel> remaining = channelMapper.findByType(channel.getChannelType());
            if (!remaining.isEmpty()) {
                MessageChannel first = remaining.get(0);
                first.setIsDefault(1);
                channelMapper.update(first);
            }
        }
    }

    /**
     * 设置为默认通道
     */
    @Transactional
    public void setDefault(Long id) {
        MessageChannel channel = channelMapper.findById(id);
        if (channel == null) {
            throw new RuntimeException("通道配置不存在");
        }
        channelMapper.clearDefaultByType(channel.getChannelType(), id);
        channel.setIsDefault(1);
        channelMapper.update(channel);
    }

    @Lazy
    @Autowired(required = false)
    private EmailNotifyService emailNotifyService;
    
    @Lazy
    @Autowired(required = false)
    private WecomNotifyService wecomNotifyService;
    
    @Lazy
    @Autowired(required = false)
    private DingtalkNotifyService dingtalkNotifyService;

    /**
     * 测试通道配置（发送测试消息）
     */
    public boolean testChannel(Long id, String testRecipient, String content) {
        MessageChannel channel = channelMapper.findById(id);
        if (channel == null) {
            throw new RuntimeException("通道配置不存在");
        }
        
        Map<String, Object> config = parseConfig(channel);
        String testSubject = "消息通道测试";
        String testContent = (content != null && !content.isBlank()) ? content
                : "这是一条测试消息，用于验证通道配置是否正确。时间: " + java.time.LocalDateTime.now();
        
        log.info("测试通道: id={}, type={}, recipient={}", id, channel.getChannelType(), testRecipient);
        
        switch (channel.getChannelType()) {
            case "email":
                if (emailNotifyService == null) {
                    throw new RuntimeException("邮件服务未初始化");
                }
                emailNotifyService.sendWithChannel(id, testRecipient, null, testSubject, testContent, false);
                return true;
            case "wecom":
                if (wecomNotifyService == null) {
                    throw new RuntimeException("企业微信服务未初始化");
                }
                java.util.Map<String, Object> wecomConfig = new java.util.HashMap<>(config);
                wecomConfig.put("content", testContent);
                wecomConfig.put("channelId", id);
                wecomNotifyService.send(wecomConfig, Map.of());
                return true;
            case "dingtalk":
                if (dingtalkNotifyService == null) {
                    throw new RuntimeException("钉钉服务未初始化");
                }
                java.util.Map<String, Object> dtConfig = new java.util.HashMap<>(config);
                dtConfig.put("content", testContent);
                dtConfig.put("channelId", id);
                dingtalkNotifyService.send(dtConfig, Map.of());
                return true;
            case "sms":
                log.warn("短信通道测试暂未实现");
                throw new RuntimeException("短信通道测试暂未实现");
            default:
                throw new RuntimeException("不支持的通道类型: " + channel.getChannelType());
        }
    }
}
