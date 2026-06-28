package com.dataplatform.data.service.alert;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 邮件告警渠道
 * 需求: 15.1
 */
@Slf4j
@Component
public class EmailAlertChannel implements AlertChannel {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${alert.email.from:noreply@datateacup.local}")
    private String fromAddress;

    @Value("${alert.email.enabled:false}")
    private boolean enabled;

    @Override
    public void send(String title, String content, String level, List<String> receivers) {
        if (mailSender == null || !enabled || receivers.isEmpty()) {
            log.debug("邮件告警渠道未启用或无接收人");
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(receivers.toArray(new String[0]));
            message.setSubject("[" + level.toUpperCase() + "] " + title);
            message.setText(content);
            mailSender.send(message);
            log.info("邮件告警发送成功: title={}, receivers={}", title, receivers.size());
        } catch (Exception e) {
            log.error("邮件告警发送失败: {}", e.getMessage());
        }
    }

    @Override
    public String getChannelType() {
        return "email";
    }

    @Override
    public boolean isAvailable() {
        return enabled && mailSender != null;
    }
}
