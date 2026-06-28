package com.dataplatform.data.service;

import com.dataplatform.data.entity.ReportShare;
import com.dataplatform.data.mapper.ReportShareMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 报表/图表分享服务
 */
@Slf4j
@Service
public class ReportShareService {

    @Autowired
    private ReportShareMapper reportShareMapper;

    /**
     * 创建分享链接
     *
     * @param reportId       报表/图表ID
     * @param shareType      分享类型：report/chart
     * @param password       访问密码（可选）
     * @param expireHours    过期小时数（0表示永不过期）
     * @param maxAccessCount 最大访问次数（0表示不限制）
     * @param createBy       创建人ID
     * @return 分享记录
     */
    public ReportShare createShare(Long reportId, String shareType, String password,
                                    int expireHours, int maxAccessCount, Long createBy) {
        ReportShare share = new ReportShare();
        share.setReportId(reportId);
        share.setShareType(shareType);
        share.setShareToken(UUID.randomUUID().toString().replace("-", ""));
        share.setPasswordProtected(password != null && !password.isEmpty());
        share.setAccessPassword(password);
        share.setMaxAccessCount(maxAccessCount);
        share.setAccessCount(0);
        share.setStatus(1);
        share.setCreateBy(createBy);

        if (expireHours > 0) {
            share.setExpireTime(new Date(System.currentTimeMillis() + expireHours * 3600000L));
        }

        reportShareMapper.insert(share);
        log.info("创建分享链接: type={}, reportId={}, token={}", shareType, reportId, share.getShareToken());
        return share;
    }

    /**
     * 验证分享Token并返回分享记录
     *
     * @return 有效的分享记录，无效则返回null
     */
    public ReportShare validateShare(String shareToken, String password) {
        ReportShare share = reportShareMapper.selectByToken(shareToken);
        if (share == null) {
            return null;
        }

        // 检查状态
        if (share.getStatus() == null || share.getStatus() != 1) {
            return null;
        }

        // 检查过期时间
        if (share.getExpireTime() != null && share.getExpireTime().before(new Date())) {
            return null;
        }

        // 检查访问次数
        if (share.getMaxAccessCount() != null && share.getMaxAccessCount() > 0
                && share.getAccessCount() >= share.getMaxAccessCount()) {
            return null;
        }

        // 检查密码
        if (Boolean.TRUE.equals(share.getPasswordProtected())) {
            if (password == null || !password.equals(share.getAccessPassword())) {
                return null;
            }
        }

        // 增加访问计数
        reportShareMapper.incrementAccessCount(share.getId());
        return share;
    }

    /**
     * 获取报表/图表的分享列表
     */
    public List<ReportShare> getShareList(Long reportId, String shareType) {
        return reportShareMapper.selectByReportId(reportId, shareType);
    }

    /**
     * 停用分享链接
     */
    public void disableShare(Long id) {
        reportShareMapper.updateStatus(id, 0);
    }

    /**
     * 删除分享链接
     */
    public void deleteShare(Long id) {
        reportShareMapper.deleteById(id);
    }
}
