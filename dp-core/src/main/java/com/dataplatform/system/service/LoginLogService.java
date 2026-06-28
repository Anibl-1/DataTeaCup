package com.dataplatform.system.service;

import com.dataplatform.system.entity.LoginLog;
import com.dataplatform.system.mapper.LoginLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginLogService {

    private final LoginLogMapper loginLogMapper;

    public void recordLogin(String username, String ipAddress, String userAgent,
                            String status, String message) {
        LoginLog loginLog = new LoginLog();
        loginLog.setUsername(username);
        loginLog.setIpAddress(ipAddress);
        loginLog.setUserAgent(userAgent);
        loginLog.setBrowser(parseBrowser(userAgent));
        loginLog.setOs(parseOs(userAgent));
        loginLog.setStatus(status);
        loginLog.setMessage(message);
        loginLog.setLoginTime(LocalDateTime.now());
        try {
            loginLogMapper.insert(loginLog);
        } catch (Exception e) {
            log.error("记录登录日志失败: {}", e.getMessage());
        }
    }

    public List<LoginLog> getPage(int page, int pageSize, String username, String status, String ipAddress) {
        int offset = (page - 1) * pageSize;
        return loginLogMapper.selectPage(username, status, ipAddress, offset, pageSize);
    }

    public long getCount(String username, String status, String ipAddress) {
        return loginLogMapper.count(username, status, ipAddress);
    }

    public int cleanBefore(int days) {
        LocalDateTime beforeTime = LocalDateTime.now().minusDays(days);
        return loginLogMapper.deleteBeforeTime(beforeTime);
    }

    private String parseBrowser(String ua) {
        if (ua == null) return "Unknown";
        if (ua.contains("Edg/")) return "Edge";
        if (ua.contains("Chrome/")) return "Chrome";
        if (ua.contains("Firefox/")) return "Firefox";
        if (ua.contains("Safari/") && !ua.contains("Chrome")) return "Safari";
        if (ua.contains("MSIE") || ua.contains("Trident")) return "IE";
        return "Other";
    }

    private String parseOs(String ua) {
        if (ua == null) return "Unknown";
        if (ua.contains("Windows")) return "Windows";
        if (ua.contains("Mac OS")) return "macOS";
        if (ua.contains("Linux")) return "Linux";
        if (ua.contains("Android")) return "Android";
        if (ua.contains("iPhone") || ua.contains("iPad")) return "iOS";
        return "Other";
    }
}
