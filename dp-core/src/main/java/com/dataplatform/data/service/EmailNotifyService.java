package com.dataplatform.data.service;

import com.dataplatform.common.exception.BusinessException;
import com.dataplatform.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 邮件通知服务
 * 支持 HTML/纯文本正文、收件人/抄送、模板变量替换
 * SMTP 配置从 SystemConfigService 读取
 */
@Slf4j
@Service
public class EmailNotifyService {

    @Autowired(required = false)
    private JavaMailSender defaultMailSender;

    @Autowired
    private com.dataplatform.common.service.SystemConfigProvider systemConfigService;
    
    @Autowired(required = false)
    private MessageChannelService messageChannelService;

    /**
     * 发送邮件通知
     *
     * @param config  节点配置 (toList, ccList, subject, content, contentType, sendCondition)
     * @param context 流程上下文变量
     */
    public void send(Map<String, Object> config, Map<String, String> context) {
        List<String> toList = (List<String>) config.get("toList");
        List<String> ccList = (List<String>) config.get("ccList");
        String subject = (String) config.getOrDefault("subject", "流程通知");
        String content = (String) config.getOrDefault("content", "");
        String contentType = (String) config.getOrDefault("contentType", "text");
        
        // 获取通道配置ID
        Long channelId = null;
        Object channelIdObj = config.get("channelId");
        if (channelIdObj != null) {
            channelId = channelIdObj instanceof Number ? ((Number) channelIdObj).longValue() : Long.parseLong(channelIdObj.toString());
        }

        if (toList == null || toList.isEmpty()) {
            log.warn("邮件收件人列表为空，跳过发送");
            return;
        }

        // 变量替换
        subject = replaceVariables(subject, context);
        content = replaceVariables(content, context);
        
        String toStr = String.join(",", toList);
        String ccStr = ccList != null && !ccList.isEmpty() ? String.join(",", ccList) : null;
        
        // 使用统一的sendWithChannel方法
        sendWithChannel(channelId, toStr, ccStr, subject, content, "html".equals(contentType));
    }

    /**
     * 获取 JavaMailSender，优先从系统配置动态构建
     */
    private JavaMailSender getMailSender() {
        String host = systemConfigService.getValueByKey("email.smtp.host");
        String port = systemConfigService.getValueByKey("email.smtp.port");
        String username = systemConfigService.getValueByKey("email.smtp.username");
        String password = systemConfigService.getValueByKey("email.smtp.password");

        if (host != null && !host.isEmpty()) {
            JavaMailSenderImpl sender = new JavaMailSenderImpl();
            sender.setHost(host);
            sender.setPort(port != null ? Integer.parseInt(port) : 587);
            sender.setUsername(username);
            sender.setPassword(password);

            Properties props = sender.getJavaMailProperties();
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp.auth", "true");

            String ssl = systemConfigService.getValueByKey("email.smtp.ssl");
            if ("true".equals(ssl)) {
                props.put("mail.smtp.ssl.enable", "true");
            } else {
                props.put("mail.smtp.starttls.enable", "true");
            }
            props.put("mail.smtp.timeout", "10000");
            props.put("mail.smtp.connectiontimeout", "10000");

            return sender;
        }

        // 回退到 Spring 默认的 JavaMailSender
        return defaultMailSender;
    }

    /**
     * 直接发送邮件（简化接口，供报表推送等场景使用）
     *
     * @param to      收件人（多个用逗号分隔）
     * @param cc      抄送人（多个用逗号分隔，可为null）
     * @param subject 主题
     * @param content 内容
     * @param isHtml  是否HTML格式
     */
    public void sendWithContent(String to, String cc, String subject, String content, boolean isHtml) {
        if (to == null || to.isEmpty()) {
            log.warn("邮件收件人为空，跳过发送");
            return;
        }
        try {
            JavaMailSender mailSender = getMailSender();
            if (mailSender == null) {
                log.warn("邮件服务未配置，跳过发送");
                return;
            }

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            String from = systemConfigService.getValueByKey("email.smtp.from");
            if (from == null || from.isEmpty()) {
                from = systemConfigService.getValueByKey("email.smtp.username");
            }
            if (from != null && !from.isEmpty()) {
                String fromName = systemConfigService.getValueByKey("email.smtp.fromName");
                if (fromName != null && !fromName.isEmpty()) {
                    helper.setFrom(from, fromName);
                } else {
                    helper.setFrom(from);
                }
            }

            helper.setTo(to.split(","));
            if (cc != null && !cc.isEmpty()) {
                helper.setCc(cc.split(","));
            }
            helper.setSubject(subject);
            helper.setText(content, isHtml);

            mailSender.send(mimeMessage);
            log.info("邮件发送成功: to={}, subject={}", to, subject);
        } catch (Exception e) {
            log.error("邮件发送失败: {}", e.getMessage());
            throw new BusinessException(ErrorCode.NOTIFICATION_SEND_FAILED, "邮件发送失败: " + e.getMessage());
        }
    }

    private String replaceVariables(String template, Map<String, String> context) {
        if (template == null || context == null) return template;
        for (Map.Entry<String, String> entry : context.entrySet()) {
            template = template.replace("${" + entry.getKey() + "}", entry.getValue() != null ? entry.getValue() : "");
        }
        return template;
    }
    
    /**
     * 使用指定通道配置发送邮件
     * @param channelId 通道配置ID（null则使用默认配置）
     */
    public void sendWithChannel(Long channelId, String to, String cc, String subject, String content, boolean isHtml) {
        if (to == null || to.isEmpty()) {
            log.warn("邮件收件人为空，跳过发送");
            return;
        }
        try {
            JavaMailSender mailSender = getMailSenderByChannel(channelId);
            if (mailSender == null) {
                log.warn("邮件服务未配置，跳过发送");
                return;
            }
            
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            // 获取发件人配置
            String from = null;
            String fromName = null;
            if (messageChannelService != null && channelId != null) {
                var channel = messageChannelService.findById(channelId);
                if (channel != null) {
                    var config = messageChannelService.parseConfig(channel);
                    from = (String) config.get("username");
                    fromName = (String) config.get("fromName");
                }
            }
            if (from == null || from.isEmpty()) {
                from = systemConfigService.getValueByKey("email.smtp.username");
                fromName = systemConfigService.getValueByKey("email.smtp.fromName");
            }
            
            if (from != null && !from.isEmpty()) {
                if (fromName != null && !fromName.isEmpty()) {
                    helper.setFrom(from, fromName);
                } else {
                    helper.setFrom(from);
                }
            }
            
            helper.setTo(to.split(","));
            if (cc != null && !cc.isEmpty()) {
                helper.setCc(cc.split(","));
            }
            helper.setSubject(subject);
            helper.setText(content, isHtml);
            
            mailSender.send(mimeMessage);
            log.info("邮件发送成功(channelId={}): to={}, subject={}", channelId, to, subject);
        } catch (Exception e) {
            log.error("邮件发送失败: {}", e.getMessage());
            throw new BusinessException(ErrorCode.NOTIFICATION_SEND_FAILED, "邮件发送失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据通道配置获取JavaMailSender
     */
    private JavaMailSender getMailSenderByChannel(Long channelId) {
        // 优先使用指定的通道配置
        if (messageChannelService != null && channelId != null) {
            var channel = messageChannelService.findById(channelId);
            if (channel != null && channel.getStatus() == 1) {
                var config = messageChannelService.parseConfig(channel);
                String host = (String) config.get("host");
                if (host != null && !host.isEmpty()) {
                    return buildMailSender(config);
                }
            }
        }
        // 尝试使用默认邮件通道
        if (messageChannelService != null) {
            var defaultChannel = messageChannelService.getDefaultChannel("email");
            if (defaultChannel != null) {
                var config = messageChannelService.parseConfig(defaultChannel);
                String host = (String) config.get("host");
                if (host != null && !host.isEmpty()) {
                    return buildMailSender(config);
                }
            }
        }
        // 回退到系统配置
        return getMailSender();
    }
    
    private JavaMailSender buildMailSender(Map<String, Object> config) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost((String) config.get("host"));
        Object portObj = config.get("port");
        sender.setPort(portObj != null ? ((Number) portObj).intValue() : 587);
        sender.setUsername((String) config.get("username"));
        sender.setPassword((String) config.get("password"));
        
        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        
        Object sslObj = config.get("ssl");
        if (Boolean.TRUE.equals(sslObj) || "true".equals(sslObj)) {
            props.put("mail.smtp.ssl.enable", "true");
        } else {
            props.put("mail.smtp.starttls.enable", "true");
        }
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.connectiontimeout", "10000");
        
        return sender;
    }
}
