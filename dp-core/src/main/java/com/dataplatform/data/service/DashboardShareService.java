package com.dataplatform.data.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dataplatform.data.entity.DashboardConfig;
import com.dataplatform.data.entity.DashboardShare;
import com.dataplatform.data.mapper.DashboardShareMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 仪表盘分享服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardShareService {

    private final DashboardShareMapper shareMapper;
    private final DashboardDesignerService dashboardDesignerService;

    /**
     * 创建分享链接
     */
    @Transactional
    public DashboardShare createShareLink(Long dashboardId, Long expireHours, Long userId) {
        log.info("Creating share link for dashboard: {}", dashboardId);
        
        // 检查仪表盘是否存在
        DashboardConfig dashboard = dashboardDesignerService.getDashboard(dashboardId);
        if (dashboard == null) {
            throw new RuntimeException("仪表盘不存在");
        }
        
        // 创建分享记录
        DashboardShare share = new DashboardShare();
        share.setDashboardId(dashboardId);
        share.setShareToken(generateToken());
        share.setCreateBy(userId);
        share.setCreateTime(LocalDateTime.now());
        share.setStatus(1);
        
        if (expireHours != null && expireHours > 0) {
            share.setExpireTime(LocalDateTime.now().plusHours(expireHours));
        }
        
        shareMapper.insert(share);
        return share;
    }

    /**
     * 获取分享的仪表盘
     */
    public DashboardConfig getSharedDashboard(String token) {
        DashboardShare share = getValidShare(token);
        if (share == null) {
            throw new RuntimeException("分享链接无效或已过期");
        }
        
        return dashboardDesignerService.getDashboard(share.getDashboardId());
    }

    /**
     * 生成嵌入代码
     */
    public String generateEmbedCode(Long dashboardId, String baseUrl) {
        // 获取或创建分享链接
        DashboardShare share = getOrCreateShare(dashboardId);
        
        String embedUrl = baseUrl + "/embed/dashboard/" + share.getShareToken();
        
        return String.format(
            "<iframe src=\"%s\" width=\"100%%\" height=\"600\" frameborder=\"0\" allowfullscreen></iframe>",
            embedUrl
        );
    }

    /**
     * 撤销分享
     */
    @Transactional
    public void revokeShare(Long dashboardId) {
        LambdaQueryWrapper<DashboardShare> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DashboardShare::getDashboardId, dashboardId)
               .eq(DashboardShare::getStatus, 1);
        
        DashboardShare share = new DashboardShare();
        share.setStatus(0);
        
        shareMapper.update(share, wrapper);
    }

    /**
     * 获取仪表盘的分享列表
     */
    public List<DashboardShare> getShareList(Long dashboardId) {
        LambdaQueryWrapper<DashboardShare> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DashboardShare::getDashboardId, dashboardId)
               .orderByDesc(DashboardShare::getCreateTime);
        
        return shareMapper.selectList(wrapper);
    }

    /**
     * 验证分享 token
     */
    public boolean validateToken(String token) {
        return getValidShare(token) != null;
    }

    /**
     * 删除分享
     */
    @Transactional
    public void deleteShare(Long shareId) {
        shareMapper.deleteById(shareId);
    }

    private DashboardShare getValidShare(String token) {
        LambdaQueryWrapper<DashboardShare> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DashboardShare::getShareToken, token)
               .eq(DashboardShare::getStatus, 1);
        
        DashboardShare share = shareMapper.selectOne(wrapper);
        
        if (share == null) {
            return null;
        }
        
        // 检查是否过期
        if (share.getExpireTime() != null && share.getExpireTime().isBefore(LocalDateTime.now())) {
            return null;
        }
        
        return share;
    }

    private DashboardShare getOrCreateShare(Long dashboardId) {
        LambdaQueryWrapper<DashboardShare> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DashboardShare::getDashboardId, dashboardId)
               .eq(DashboardShare::getStatus, 1)
               .isNull(DashboardShare::getExpireTime)
               .last("LIMIT 1");
        
        DashboardShare share = shareMapper.selectOne(wrapper);
        
        if (share == null) {
            share = createShareLink(dashboardId, null, null);
        }
        
        return share;
    }

    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
