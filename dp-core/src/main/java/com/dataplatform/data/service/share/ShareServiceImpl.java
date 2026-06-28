package com.dataplatform.data.service.share;

import com.dataplatform.data.mapper.ShareMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 分享服务实现 - 数据库持久化
 * 需求: 21.1, 21.2, 21.3, 21.4, 21.5, 21.6, 21.7, 21.8
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShareServiceImpl implements ShareService {

    private final ShareMapper shareMapper;

    @Override
    public ShareLink createShare(ShareLink link) {
        if (link.getId() == null) {
            link.setId(UUID.randomUUID().toString().substring(0, 12));
        }
        if (link.getToken() == null) {
            link.setToken(UUID.randomUUID().toString().replace("-", ""));
        }
        link.setActive(true);
        link.setAccessCount(0);
        link.setCreatedAt(LocalDateTime.now());

        shareMapper.insertShare(link);
        log.info("[分享] 创建: id={}, type={}, resource={}", link.getId(), link.getAccessType(), link.getResourceId());
        return link;
    }

    @Override
    public ShareLink getShareByToken(String token) {
        return shareMapper.selectByToken(token);
    }

    @Override
    public ShareAccessResult access(String token, String password, String ip, String userAgent) {
        ShareLink link = shareMapper.selectByToken(token);

        if (link == null) {
            logAccess(null, ip, userAgent, false, "链接不存在");
            return ShareAccessResult.denied("链接不存在");
        }

        if (!link.isActive()) {
            logAccess(link.getId(), ip, userAgent, false, "链接已撤销");
            return ShareAccessResult.denied("链接已撤销");
        }

        // 检查过期
        if (link.getExpireAt() != null && LocalDateTime.now().isAfter(link.getExpireAt())) {
            logAccess(link.getId(), ip, userAgent, false, "链接已过期");
            return ShareAccessResult.denied("链接已过期");
        }

        // 检查访问次数
        if (link.getMaxAccessCount() > 0 && link.getAccessCount() >= link.getMaxAccessCount()) {
            logAccess(link.getId(), ip, userAgent, false, "超过最大访问次数");
            return ShareAccessResult.denied("超过最大访问次数");
        }

        // 检查密码
        if ("password".equals(link.getAccessType())) {
            if (link.getPassword() == null || !link.getPassword().equals(password)) {
                logAccess(link.getId(), ip, userAgent, false, "密码错误");
                return ShareAccessResult.denied("密码错误");
            }
        }

        // 访问成功
        shareMapper.incrementAccessCount(link.getId());
        link.setAccessCount(link.getAccessCount() + 1);
        logAccess(link.getId(), ip, userAgent, true, null);
        return ShareAccessResult.allowed(link);
    }

    @Override
    public void revokeShare(String shareId) {
        shareMapper.updateActive(shareId, false);
        log.info("[分享] 撤销: id={}", shareId);
    }

    @Override
    public List<ShareLink> listSharesByUser(String userId) {
        return shareMapper.selectByUser(userId);
    }

    @Override
    public List<ShareAccessLog> getAccessLogs(String shareId, int limit) {
        return shareMapper.selectAccessLogs(shareId, limit);
    }

    @Override
    public String generateEmbedCode(String shareId) {
        ShareLink link = shareMapper.selectById(shareId);
        if (link == null || !link.isEmbeddable()) {
            return null;
        }
        return "<iframe src=\"/share/" + link.getToken()
                + "\" width=\"100%\" height=\"600\" frameborder=\"0\"></iframe>";
    }

    private void logAccess(String shareId, String ip, String userAgent, boolean success, String reason) {
        if (shareId == null) return;
        ShareAccessLog accessLog = new ShareAccessLog();
        accessLog.setId(UUID.randomUUID().toString().substring(0, 12));
        accessLog.setShareId(shareId);
        accessLog.setAccessIp(ip);
        accessLog.setUserAgent(userAgent);
        accessLog.setSuccess(success);
        accessLog.setFailReason(reason);
        accessLog.setAccessTime(LocalDateTime.now());
        try {
            shareMapper.insertAccessLog(accessLog);
        } catch (Exception e) {
            log.warn("记录分享访问日志失败: {}", e.getMessage());
        }
    }
}
