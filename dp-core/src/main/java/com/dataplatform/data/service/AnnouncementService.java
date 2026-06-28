package com.dataplatform.data.service;

import com.dataplatform.data.entity.Announcement;
import com.dataplatform.data.mapper.AnnouncementMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 公告服务
 */
@Service
public class AnnouncementService {

    @Autowired
    private AnnouncementMapper announcementMapper;

    /**
     * 分页查询公告
     */
    public IPage<Announcement> getPage(int page, int pageSize, String keyword, Integer status) {
        Page<Announcement> pageParam = new Page<>(page, pageSize);
        return announcementMapper.selectPage(pageParam, keyword, status);
    }

    /**
     * 获取当前有效的公告列表（用于前台展示）
     */
    public List<Announcement> getActiveAnnouncements() {
        return announcementMapper.selectActiveList();
    }

    /**
     * 根据ID获取公告
     */
    public Announcement getById(Long id) {
        return announcementMapper.selectById(id);
    }

    /**
     * 创建公告
     */
    public int create(Announcement announcement) {
        announcement.setCreateTime(LocalDateTime.now());
        announcement.setUpdateTime(LocalDateTime.now());
        if (announcement.getStatus() == null) {
            announcement.setStatus(1);
        }
        if (announcement.getPriority() == null) {
            announcement.setPriority(2);
        }
        if (announcement.getIsTop() == null) {
            announcement.setIsTop(0);
        }
        if (announcement.getType() == null) {
            announcement.setType("info");
        }
        return announcementMapper.insert(announcement);
    }

    /**
     * 更新公告
     */
    public int update(Announcement announcement) {
        announcement.setUpdateTime(LocalDateTime.now());
        return announcementMapper.update(announcement);
    }

    /**
     * 删除公告
     */
    public int delete(Long id) {
        return announcementMapper.deleteById(id);
    }

    /**
     * 批量删除公告
     */
    public int batchDelete(List<Long> ids) {
        return announcementMapper.batchDelete(ids);
    }

    /**
     * 更新公告状态
     */
    public int updateStatus(Long id, Integer status) {
        return announcementMapper.updateStatus(id, status, LocalDateTime.now());
    }

    /**
     * 获取公告统计数据
     */
    public java.util.Map<String, Long> getStats() {
        java.util.Map<String, Long> stats = new java.util.HashMap<>();
        stats.put("total", announcementMapper.countTotal());
        stats.put("enabled", announcementMapper.countEnabled());
        stats.put("top", announcementMapper.countTop());
        return stats;
    }
}
