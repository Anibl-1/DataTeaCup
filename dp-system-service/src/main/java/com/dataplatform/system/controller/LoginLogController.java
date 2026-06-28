package com.dataplatform.system.controller;

import com.dataplatform.common.annotation.RequirePermission;
import com.dataplatform.common.PageResult;
import com.dataplatform.common.Result;
import com.dataplatform.system.entity.LoginLog;
import com.dataplatform.system.service.LoginLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/login-log")
@RequirePermission("log:login")
@RequiredArgsConstructor
public class LoginLogController {

    private final LoginLogService loginLogService;

    @GetMapping("/list")
    public Result<PageResult<LoginLog>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String ipAddress) {
        List<LoginLog> list = loginLogService.getPage(page, pageSize, username, status, ipAddress);
        long total = loginLogService.getCount(username, status, ipAddress);
        return Result.success(new PageResult<>(list, total));
    }

    @RequirePermission("system:config")
    @DeleteMapping("/clean")
    public Result<Integer> clean(@RequestBody Map<String, Object> params) {
        int days = params.get("days") != null ? ((Number) params.get("days")).intValue() : 90;
        int count = loginLogService.cleanBefore(days);
        return Result.success(count);
    }
}
