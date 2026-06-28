package com.dataplatform.data.service.share;

import java.util.List;

/**
 * 分享服务接口
 * 需求: 21.1, 21.2, 21.3, 21.4, 21.5, 21.6
 */
public interface ShareService {

    ShareLink createShare(ShareLink link);

    ShareLink getShareByToken(String token);

    /**
     * 访问分享链接，返回是否允许访问
     */
    ShareAccessResult access(String token, String password, String ip, String userAgent);

    void revokeShare(String shareId);

    List<ShareLink> listSharesByUser(String userId);

    List<ShareAccessLog> getAccessLogs(String shareId, int limit);

    /**
     * 生成嵌入代码
     */
    String generateEmbedCode(String shareId);
}
