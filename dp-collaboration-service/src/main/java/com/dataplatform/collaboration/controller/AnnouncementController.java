package com.dataplatform.collaboration.controller;

import com.dataplatform.common.annotation.OperationLog;
import com.dataplatform.common.annotation.RequirePermission;
import com.dataplatform.common.Result;
import com.dataplatform.data.entity.Announcement;
import com.dataplatform.data.entity.AnnouncementRead;
import com.dataplatform.data.mapper.AnnouncementReadMapper;
import com.dataplatform.data.service.AnnouncementService;
import com.dataplatform.common.security.SecurityContext;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 公告控制器
 */
@RestController
@RequestMapping("/announcement")
@RequirePermission("announcement:read")
public class AnnouncementController {

    @Autowired
    private AnnouncementService announcementService;

    @Autowired
    private AnnouncementReadMapper announcementReadMapper;


    /**
     * 分页查询公告
     */
    @GetMapping("/list")
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        
        IPage<Announcement> pageResult = announcementService.getPage(page, Math.min(pageSize, 200), keyword, status);
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        
        return Result.success(result);
    }

    /**
     * 获取当前有效的公告（用于前台滚动展示）
     */
    @GetMapping("/active")
    public Result<List<Announcement>> getActiveAnnouncements() {
        return Result.success(announcementService.getActiveAnnouncements());
    }

    /**
     * 根据ID获取公告详情
     */
    @GetMapping("/{id}")
    public Result<Announcement> getById(@PathVariable Long id) {
        Announcement announcement = announcementService.getById(id);
        if (announcement == null) {
            return Result.error("公告不存在");
        }
        return Result.success(announcement);
    }

    /**
     * 创建公告
     */
    @RequirePermission("announcement:manage")
    @OperationLog(module = "公告管理", type = OperationLog.OperationType.CREATE, description = "创建公告")
    @PostMapping
    public Result<String> create(@RequestBody Announcement announcement) {
        int rows = announcementService.create(announcement);
        return rows > 0 ? Result.success("创建成功") : Result.error("创建失败");
    }

    /**
     * 更新公告
     */
    @RequirePermission("announcement:manage")
    @OperationLog(module = "公告管理", type = OperationLog.OperationType.UPDATE, description = "更新公告")
    @PutMapping("/{id}")
    public Result<String> update(@PathVariable Long id, @RequestBody Announcement announcement) {
        announcement.setId(id);
        int rows = announcementService.update(announcement);
        return rows > 0 ? Result.success("更新成功") : Result.error("更新失败");
    }

    /**
     * 删除公告
     */
    @RequirePermission("announcement:manage")
    @OperationLog(module = "公告管理", type = OperationLog.OperationType.DELETE, description = "删除公告")
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        int rows = announcementService.delete(id);
        return rows > 0 ? Result.success("删除成功") : Result.error("删除失败");
    }

    /**
     * 批量删除公告
     */
    @RequirePermission("announcement:manage")
    @OperationLog(module = "公告管理", type = OperationLog.OperationType.DELETE, description = "批量删除公告")
    @DeleteMapping("/batch")
    public Result<String> batchDelete(@RequestBody List<Long> ids) {
        int rows = announcementService.batchDelete(ids);
        return rows > 0 ? Result.success("删除成功") : Result.error("删除失败");
    }

    /**
     * 更新公告状态
     */
    @RequirePermission("announcement:manage")
    @OperationLog(module = "公告管理", type = OperationLog.OperationType.UPDATE, description = "更新公告状态")
    @PutMapping("/{id}/status")
    public Result<String> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        int rows = announcementService.updateStatus(id, status);
        return rows > 0 ? Result.success("状态更新成功") : Result.error("状态更新失败");
    }

    /**
     * 获取公告统计数据
     */
    @GetMapping("/stats")
    public Result<Map<String, Long>> getStats() {
        return Result.success(announcementService.getStats());
    }

    /**
     * 标记公告已读
     */
    @PutMapping("/{id}/read")
    public Result<String> markAsRead(@PathVariable Long id) {
        Long userId = SecurityContext.requireCurrentUserId();
        // 检查是否已读
        if (announcementReadMapper.countByAnnouncementAndUser(id, userId) > 0) {
            return Result.success("已读");
        }
        AnnouncementRead record = new AnnouncementRead();
        record.setAnnouncementId(id);
        record.setUserId(userId);
        record.setReadTime(LocalDateTime.now());
        announcementReadMapper.insert(record);
        // 更新公告已读计数
        Announcement ann = announcementService.getById(id);
        if (ann != null) {
            int count = announcementReadMapper.countByAnnouncement(id);
            ann.setReadCount(count);
            announcementService.update(ann);
        }
        return Result.success("已读");
    }

    /**
     * 获取公告已读统计
     */
    @GetMapping("/{id}/read-count")
    public Result<Map<String, Object>> getReadCount(@PathVariable Long id) {
        int count = announcementReadMapper.countByAnnouncement(id);
        List<Long> userIds = announcementReadMapper.selectUserIdsByAnnouncement(id);
        Map<String, Object> result = new HashMap<>();
        result.put("readCount", count);
        result.put("userIds", userIds);
        return Result.success(result);
    }

}
