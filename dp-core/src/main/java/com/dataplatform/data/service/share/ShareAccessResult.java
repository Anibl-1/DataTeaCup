package com.dataplatform.data.service.share;

import lombok.Data;

/**
 * 分享访问结果
 */
@Data
public class ShareAccessResult {
    private boolean allowed;
    private String reason;
    private ShareLink shareLink;

    public static ShareAccessResult allowed(ShareLink link) {
        ShareAccessResult r = new ShareAccessResult();
        r.setAllowed(true);
        r.setShareLink(link);
        return r;
    }

    public static ShareAccessResult denied(String reason) {
        ShareAccessResult r = new ShareAccessResult();
        r.setAllowed(false);
        r.setReason(reason);
        return r;
    }
}
