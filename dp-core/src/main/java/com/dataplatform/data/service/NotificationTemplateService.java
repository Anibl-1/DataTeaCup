package com.dataplatform.data.service;

import com.dataplatform.data.entity.NotificationTemplate;
import com.dataplatform.data.mapper.NotificationTemplateMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 通知模板服务
 * 支持 CRUD + 变量替换渲染
 */
@Slf4j
@Service
public class NotificationTemplateService {

    private static final Pattern VAR_PATTERN = Pattern.compile("\\$\\{(\\w+(?:\\.\\w+)*)}");

    private final NotificationTemplateMapper templateMapper;

    public NotificationTemplateService(NotificationTemplateMapper templateMapper) {
        this.templateMapper = templateMapper;
    }

    public List<NotificationTemplate> findAll() {
        return templateMapper.findAll();
    }

    public NotificationTemplate findById(Long id) {
        return templateMapper.findById(id);
    }

    public NotificationTemplate findByCode(String code) {
        return templateMapper.findByCode(code);
    }

    public List<NotificationTemplate> search(String channel, String notificationType) {
        return templateMapper.search(channel, notificationType);
    }

    public NotificationTemplate create(NotificationTemplate template) {
        if (template.getIsEnabled() == null) {
            template.setIsEnabled(1);
        }
        templateMapper.insert(template);
        return template;
    }

    public NotificationTemplate update(NotificationTemplate template) {
        templateMapper.update(template);
        return templateMapper.findById(template.getId());
    }

    public void delete(Long id) {
        templateMapper.deleteById(id);
    }

    /**
     * 渲染模板：将 ${variable} 替换为实际值
     * @param templateCode 模板编码
     * @param variables    变量键值对
     * @return [subject, content] 渲染后的标题和内容；模板不存在则返回null
     */
    public String[] render(String templateCode, Map<String, String> variables) {
        NotificationTemplate tpl = templateMapper.findByCode(templateCode);
        if (tpl == null) {
            log.warn("通知模板不存在或未启用: {}", templateCode);
            return null;
        }
        String subject = replaceVariables(tpl.getSubject(), variables);
        String content = replaceVariables(tpl.getContent(), variables);
        return new String[]{subject, content};
    }

    /**
     * 渲染指定模板对象
     */
    public String[] render(NotificationTemplate tpl, Map<String, String> variables) {
        if (tpl == null) return null;
        String subject = replaceVariables(tpl.getSubject(), variables);
        String content = replaceVariables(tpl.getContent(), variables);
        return new String[]{subject, content};
    }

    /**
     * 变量替换：${key} → value
     */
    public String replaceVariables(String text, Map<String, String> variables) {
        if (text == null || text.isEmpty() || variables == null || variables.isEmpty()) {
            return text;
        }
        Matcher matcher = VAR_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = variables.getOrDefault(key, matcher.group(0));
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
