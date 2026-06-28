package com.dataplatform.collaboration.controller;

import com.dataplatform.common.annotation.OperationLog;
import com.dataplatform.common.annotation.RequirePermission;
import com.dataplatform.common.Result;
import com.dataplatform.common.security.SecurityContext;
import com.dataplatform.data.entity.Notification;
import com.dataplatform.system.entity.User;
import com.dataplatform.system.mapper.UserMapper;
import com.dataplatform.data.service.NotificationService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通知控制器
 */
@RestController
@RequestMapping("/notification")
@RequirePermission("notification:read")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserMapper userMapper;

    /**
     * 分页查询当前用户通知
     */
    @GetMapping("/list")
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(required = false) String notificationType,
            @RequestParam(required = false) String keyword) {

        Long userId = SecurityContext.requireCurrentUserId();
        IPage<Notification> pageResult = notificationService.getPage(page, Math.min(pageSize, 200), userId, isRead, notificationType, keyword);

        Map<String, Object> result = new HashMap<>();
        result.put("list", pageResult.getRecords());
        result.put("total", pageResult.getTotal());

        return Result.success(result);
    }

    /**
     * 获取未读通知
     */
    @GetMapping("/unread")
    public Result<List<Notification>> getUnread() {
        Long userId = SecurityContext.requireCurrentUserId();
        return Result.success(notificationService.getUnread(userId));
    }

    /**
     * 获取未读数量
     */
    @GetMapping("/unread-count")
    public Result<Integer> getUnreadCount() {
        Long userId = SecurityContext.requireCurrentUserId();
        return Result.success(notificationService.getUnreadCount(userId));
    }

    /**
     * 标记已读
     */
    @PutMapping("/{id}/read")
    public Result<String> markAsRead(@PathVariable Long id) {
        int rows = notificationService.markAsRead(id);
        return rows > 0 ? Result.success("标记成功") : Result.error("标记失败");
    }

    /**
     * 全部已读
     */
    @PutMapping("/read-all")
    public Result<String> markAllAsRead() {
        Long userId = SecurityContext.requireCurrentUserId();
        notificationService.markAllAsRead(userId);
        return Result.success("全部已读");
    }

    /**
     * 发送通知（支持按用户/部门发送）
     * body: { title, content, notificationType, priority, targetType: "user"|"dept",
     *         targetUserIds: [1,2,3], deptId: 1, attachments, remark }
     */
    @OperationLog(module = "通知管理", type = OperationLog.OperationType.CREATE, description = "发送通知")
    @PostMapping("/send")
    public Result<String> sendNotification(@RequestBody Map<String, Object> body) {
        Long senderId = SecurityContext.requireCurrentUserId();
        User sender = userMapper.selectById(senderId);
        String senderName = sender != null ? (sender.getNickname() != null ? sender.getNickname() : sender.getUsername()) : "系统";

        Notification template = new Notification();
        template.setTitle((String) body.get("title"));
        template.setContent((String) body.get("content"));
        template.setNotificationType((String) body.getOrDefault("notificationType", "system"));
        template.setPriority((String) body.getOrDefault("priority", "normal"));
        template.setAttachments((String) body.get("attachments"));
        template.setRemark((String) body.get("remark"));

        String targetType = (String) body.getOrDefault("targetType", "user");
        int count;

        if ("dept".equals(targetType)) {
            Number deptIdNum = (Number) body.get("deptId");
            if (deptIdNum == null) {
                return Result.error("请选择目标部门");
            }
            // 如果指定了具体用户，则发给这些用户；否则发给部门全员
            List<Number> userIdNums = (List<Number>) body.get("targetUserIds");
            if (userIdNums != null && !userIdNums.isEmpty()) {
                template.setDeptId(deptIdNum.longValue());
                List<Long> userIds = userIdNums.stream().map(Number::longValue).toList();
                count = notificationService.sendToUsers(senderId, senderName, userIds, template);
            } else {
                count = notificationService.sendToDept(senderId, senderName, deptIdNum.longValue(), template);
            }
        } else {
            List<Number> userIdNums = (List<Number>) body.get("targetUserIds");
            if (userIdNums == null || userIdNums.isEmpty()) {
                // 单用户兼容
                Number targetUserId = (Number) body.get("targetUserId");
                if (targetUserId == null) {
                    return Result.error("请选择目标用户");
                }
                template.setTargetUserId(targetUserId.longValue());
                template.setSenderId(senderId);
                template.setSenderName(senderName);
                count = notificationService.send(template);
            } else {
                List<Long> userIds = userIdNums.stream().map(Number::longValue).toList();
                count = notificationService.sendToUsers(senderId, senderName, userIds, template);
            }
        }

        return Result.success("成功发送给 " + count + " 个用户");
    }

    /**
     * 发送通知（兼容旧接口）
     */
    @PostMapping
    public Result<String> send(@RequestBody Notification notification) {
        int rows = notificationService.send(notification);
        return rows > 0 ? Result.success("发送成功") : Result.error("发送失败");
    }

    /**
     * 删除通知
     */
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        int rows = notificationService.delete(id);
        return rows > 0 ? Result.success("删除成功") : Result.error("删除失败");
    }

    /**
     * 批量删除
     */
    @DeleteMapping("/batch")
    public Result<String> batchDelete(@RequestBody List<Long> ids) {
        int rows = notificationService.batchDelete(ids);
        return rows > 0 ? Result.success("删除成功") : Result.error("删除失败");
    }

}
