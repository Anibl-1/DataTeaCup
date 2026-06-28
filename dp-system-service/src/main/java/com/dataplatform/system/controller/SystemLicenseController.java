package com.dataplatform.system.controller;

import com.dataplatform.common.Result;
import com.dataplatform.common.annotation.RequirePermission;
import com.dataplatform.data.service.LicenseLimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Runtime license status API.
 */
@RestController
@RequestMapping("/system/license")
@RequiredArgsConstructor
@RequirePermission(value = {"system:monitor", "ops:monitor"})
public class SystemLicenseController {

    private final LicenseLimitService licenseLimitService;

    /**
     * Returns license validation status and active limits. The raw key is never returned.
     */
    @GetMapping("/status")
    public Result<Map<String, Object>> getStatus() {
        return Result.success(licenseLimitService.getStatus());
    }
}
