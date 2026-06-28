package com.dataplatform.analytics.controller;

import com.dataplatform.common.Result;
import com.dataplatform.common.annotation.RequirePermission;
import com.dataplatform.data.service.share.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 分享API
 * 需求: 21.6
 */
@RestController
@RequestMapping("/share")
@RequiredArgsConstructor
@RequirePermission("report:read")
public class ShareController {

    private final ShareService shareService;

    @PostMapping
    public Result<ShareLink> createShare(@RequestBody ShareLink link) {
        return Result.success(shareService.createShare(link));
    }

    @PostMapping("/access/{token}")
    public Result<ShareAccessResult> accessShare(
            @PathVariable String token,
            @RequestBody(required = false) AccessRequest request,
            HttpServletRequest httpRequest) {
        String password = request != null ? request.getPassword() : null;
        String ip = httpRequest.getRemoteAddr();
        String ua = httpRequest.getHeader("User-Agent");
        return Result.success(shareService.access(token, password, ip, ua));
    }

    @DeleteMapping("/{shareId}")
    public Result<Void> revokeShare(@PathVariable String shareId) {
        shareService.revokeShare(shareId);
        return Result.success();
    }

    @GetMapping("/my")
    public Result<List<ShareLink>> myShares(@RequestParam String userId) {
        return Result.success(shareService.listSharesByUser(userId));
    }

    @GetMapping("/{shareId}/logs")
    public Result<List<ShareAccessLog>> getAccessLogs(
            @PathVariable String shareId,
            @RequestParam(defaultValue = "50") int limit) {
        return Result.success(shareService.getAccessLogs(shareId, limit));
    }

    @GetMapping("/{shareId}/embed")
    public Result<String> getEmbedCode(@PathVariable String shareId) {
        String code = shareService.generateEmbedCode(shareId);
        return code != null ? Result.success(code) : Result.error("不支持嵌入或分享不存在");
    }

    @Data
    public static class AccessRequest {
        private String password;
    }
}
