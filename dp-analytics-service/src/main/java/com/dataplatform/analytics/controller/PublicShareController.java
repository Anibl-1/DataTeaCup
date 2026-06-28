package com.dataplatform.analytics.controller;

import com.dataplatform.common.Result;
import com.dataplatform.data.entity.ReportShare;
import com.dataplatform.data.service.ChartDefinitionService;
import com.dataplatform.data.service.ReportDefinitionService;
import com.dataplatform.data.service.ReportShareService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 公开分享接口控制器
 * 路径以 /public/ 开头，不需要认证
 */
@Slf4j
@RestController
@RequestMapping("/public/share")
public class PublicShareController {

    @Autowired
    private ReportShareService reportShareService;

    @Autowired
    private ReportDefinitionService reportDefinitionService;

    @Autowired
    private ChartDefinitionService chartDefinitionService;

    /**
     * 获取分享信息（验证Token是否有效，是否需要密码）
     */
    @GetMapping("/{token}/info")
    public Result<Map<String, Object>> getShareInfo(@PathVariable String token) {
        try {
            ReportShare share = reportShareService.validateShare(token, null);
            Map<String, Object> info = new HashMap<>();
            
            if (share == null) {
                // 可能是需要密码，重新获取不校验密码
                // 仅返回是否需要密码的信息
                info.put("valid", false);
                info.put("needPassword", true);
                return Result.success(info);
            }
            
            info.put("valid", true);
            info.put("needPassword", Boolean.TRUE.equals(share.getPasswordProtected()));
            info.put("shareType", share.getShareType());
            info.put("reportId", share.getReportId());
            return Result.success(info);
        } catch (Exception e) {
            return Result.error("获取分享信息失败: " + e.getMessage());
        }
    }

    /**
     * 通过分享Token访问报表数据
     */
    @GetMapping("/report/{token}")
    public Result<Map<String, Object>> getSharedReport(
            @PathVariable String token,
            @RequestParam(required = false) String password,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String filters) {
        try {
            ReportShare share = reportShareService.validateShare(token, password);
            if (share == null) {
                return Result.error("分享链接无效或已过期");
            }
            if (!"report".equals(share.getShareType())) {
                return Result.error("分享类型不匹配");
            }

            com.dataplatform.common.PageResult<Map<String, Object>> data =
                    reportDefinitionService.executeReportQueryWithPagination(
                            share.getReportId(), page, pageSize, filters, null);
            
            Map<String, Object> result = new HashMap<>();
            result.put("data", data);
            
            // 获取报表基本信息
            var report = reportDefinitionService.getReportDefinitionById(share.getReportId());
            if (report != null) {
                result.put("reportName", report.getReportName());
                result.put("description", report.getDescription());
            }
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("访问分享报表失败", e);
            return Result.error("访问失败: " + e.getMessage());
        }
    }

    /**
     * 通过分享Token访问图表数据
     */
    @GetMapping("/chart/{token}")
    public Result<Map<String, Object>> getSharedChart(
            @PathVariable String token,
            @RequestParam(required = false) String password,
            @RequestParam(required = false) String filters,
            @RequestParam(required = false, defaultValue = "10000") Integer limit) {
        try {
            ReportShare share = reportShareService.validateShare(token, password);
            if (share == null) {
                return Result.error("分享链接无效或已过期");
            }
            if (!"chart".equals(share.getShareType())) {
                return Result.error("分享类型不匹配");
            }

            List<Map<String, Object>> data = chartDefinitionService.executeChartQuery(
                    share.getReportId(), filters, limit);

            Map<String, Object> result = new HashMap<>();
            result.put("data", data);

            // 获取图表基本信息和配置
            var chart = chartDefinitionService.getChartDefinitionById(share.getReportId());
            if (chart != null) {
                result.put("chartName", chart.getChartName());
                result.put("chartType", chart.getChartType());
                result.put("chartConfig", chart.getChartConfig());
                result.put("description", chart.getDescription());
            }

            return Result.success(result);
        } catch (Exception e) {
            log.error("访问分享图表失败", e);
            return Result.error("访问失败: " + e.getMessage());
        }
    }
}
