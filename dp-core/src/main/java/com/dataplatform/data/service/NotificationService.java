package com.dataplatform.data.service;

import com.dataplatform.data.entity.Notification;
import com.dataplatform.system.entity.User;
import com.dataplatform.data.mapper.NotificationMapper;
import com.dataplatform.system.mapper.UserMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知服务
 * 支持发送给指定用户、多个用户、部门
 */
@Slf4j
@Service
public class NotificationService {

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 分页查询用户通知
     */
    public IPage<Notification> getPage(int page, int pageSize, Long targetUserId, Boolean isRead, String notificationType) {
        return getPage(page, pageSize, targetUserId, isRead, notificationType, null);
    }

    public IPage<Notification> getPage(int page, int pageSize, Long targetUserId, Boolean isRead, String notificationType, String keyword) {
        Page<Notification> pageParam = new Page<>(page, pageSize);
        return notificationMapper.selectPage(pageParam, targetUserId, isRead, notificationType, keyword);
    }

    /**
     * 获取用户未读通知
     */
    public List<Notification> getUnread(Long userId) {
        return notificationMapper.selectUnreadByUserId(userId);
    }

    /**
     * 获取未读数量
     */
    public int getUnreadCount(Long userId) {
        return notificationMapper.countUnread(userId);
    }

    /**
     * 根据ID获取
     */
    public Notification getById(Long id) {
        return notificationMapper.selectById(id);
    }

    /**
     * 发送通知给单个用户
     */
    @Transactional
    public int send(Notification notification) {
        notification.setIsRead(false);
        notification.setCreateTime(LocalDateTime.now());
        if (notification.getPriority() == null) {
            notification.setPriority("normal");
        }
        if (notification.getNotificationType() == null) {
            notification.setNotificationType("system");
        }
        return notificationMapper.insert(notification);
    }

    /**
     * 发送通知给多个用户
     */
    @Transactional
    public int sendToUsers(Long senderId, String senderName, List<Long> targetUserIds, Notification template) {
        int count = 0;
        for (Long userId : targetUserIds) {
            Notification n = new Notification();
            n.setTitle(template.getTitle());
            n.setContent(template.getContent());
            n.setNotificationType(template.getNotificationType());
            n.setPriority(template.getPriority());
            n.setTargetUserId(userId);
            n.setSenderId(senderId);
            n.setSenderName(senderName);
            n.setDeptId(template.getDeptId());
            n.setAttachments(template.getAttachments());
            n.setRemark(template.getRemark());
            n.setRelatedType(template.getRelatedType());
            n.setRelatedId(template.getRelatedId());
            n.setIsRead(false);
            n.setCreateTime(LocalDateTime.now());
            if (n.getPriority() == null) n.setPriority("normal");
            if (n.getNotificationType() == null) n.setNotificationType("system");
            notificationMapper.insert(n);
            count++;
        }
        log.info("通知已发送给 {} 个用户, sender={}", count, senderName);
        return count;
    }

    /**
     * 发送通知给部门所有成员
     */
    @Transactional
    public int sendToDept(Long senderId, String senderName, Long deptId, Notification template) {
        List<User> deptUsers = userMapper.selectByDeptId(deptId);
        if (deptUsers == null || deptUsers.isEmpty()) {
            log.warn("部门 {} 下无用户，跳过发送", deptId);
            return 0;
        }
        template.setDeptId(deptId);
        List<Long> userIds = deptUsers.stream().map(User::getId).toList();
        return sendToUsers(senderId, senderName, userIds, template);
    }

    /**
     * 标记已读
     */
    @Transactional
    public int markAsRead(Long id) {
        return notificationMapper.markAsRead(id, LocalDateTime.now());
    }

    /**
     * 全部已读
     */
    @Transactional
    public int markAllAsRead(Long userId) {
        return notificationMapper.markAllAsRead(userId, LocalDateTime.now());
    }

    /**
     * 删除通知
     */
    @Transactional
    public int delete(Long id) {
        return notificationMapper.deleteById(id);
    }

    /**
     * 批量删除
     */
    @Transactional
    public int batchDelete(List<Long> ids) {
        return notificationMapper.batchDelete(ids);
    }

    /**
     * 发送系统通知
     * 
     * @param title 通知标题
     * @param content 通知内容
     * @param level 通知级别 (info, warning, error)
     */
    public void sendSystemNotification(String title, String content, String level) {
        log.info("Sending system notification: title={}, level={}", title, level);
        
        Notification notification = new Notification();
        notification.setTitle(title);
        notification.setContent(content);
        notification.setNotificationType("system");
        notification.setPriority(level != null ? level : "normal");
        notification.setIsRead(false);
        notification.setCreateTime(LocalDateTime.now());
        
        // 发送给所有管理员用户（简化实现：发送给系统）
        notification.setTargetUserId(null); // 系统级通知
        notification.setSenderName("系统");
        
        try {
            notificationMapper.insert(notification);
        } catch (Exception e) {
            log.error("Failed to send system notification", e);
        }
    }
}
