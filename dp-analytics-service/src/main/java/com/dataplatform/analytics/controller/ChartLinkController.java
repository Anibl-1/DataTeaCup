package com.dataplatform.analytics.controller;

import com.dataplatform.common.Result;
import com.dataplatform.common.annotation.RequirePermission;

import com.dataplatform.data.dto.ChartLinkConfig;
import com.dataplatform.data.service.ChartLinkService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 鍥捐〃鑱斿姩鎺у埗鍣?
 * 鎻愪緵鍥捐〃鑱斿姩閰嶇疆绠＄悊鍜岃仈鍔ㄦ暟鎹煡璇㈠姛鑳?
 *
 * @author dataplatform
 */
@Slf4j
@RestController
@RequestMapping("/chart-link")
@RequirePermission("chart:read")
public class ChartLinkController {

    @Autowired
    private ChartLinkService chartLinkService;

    // ==================== 鑱斿姩閰嶇疆绠＄悊 ====================

    /**
     * 淇濆瓨鍥捐〃鑱斿姩閰嶇疆
     *
     * @param dashboardId 浠〃鐩業D
     * @param configs     鑱斿姩閰嶇疆鍒楄〃
     * @return 鎿嶄綔缁撴灉
     */
    @PostMapping("/config/{dashboardId}")
    public Result<Void> saveLinkConfig(
            @PathVariable Long dashboardId,
            @RequestBody List<ChartLinkConfig> configs) {
        log.info("Saving link config for dashboard: {}, configs count: {}", 
                dashboardId, configs != null ? configs.size() : 0);
        chartLinkService.saveLinkConfig(dashboardId, configs);
        return Result.success();
    }

    /**
     * 鑾峰彇鍥捐〃鑱斿姩閰嶇疆
     *
     * @param dashboardId 浠〃鐩業D
     * @return 鑱斿姩閰嶇疆鍒楄〃
     */
    @GetMapping("/config/{dashboardId}")
    public Result<List<ChartLinkConfig>> getLinkConfig(@PathVariable Long dashboardId) {
        log.debug("Getting link config for dashboard: {}", dashboardId);
        List<ChartLinkConfig> configs = chartLinkService.getLinkConfig(dashboardId);
        return Result.success(configs);
    }

    /**
     * 鍒犻櫎鎸囧畾鐨勮仈鍔ㄩ厤缃?
     *
     * @param dashboardId 浠〃鐩業D
     * @param linkId      鑱斿姩閰嶇疆ID
     * @return 鎿嶄綔缁撴灉
     */
    @DeleteMapping("/config/{dashboardId}/{linkId}")
    public Result<Void> deleteLinkConfig(
            @PathVariable Long dashboardId,
            @PathVariable String linkId) {
        log.info("Deleting link config: {} from dashboard: {}", linkId, dashboardId);
        chartLinkService.deleteLinkConfig(dashboardId, linkId);
        return Result.success();
    }

    // ==================== 鑱斿姩鏁版嵁鏌ヨ ====================

    /**
     * 鑾峰彇鑱斿姩鍥捐〃鏁版嵁
     * 鏍规嵁婧愬浘琛ㄧ殑閫変腑缁村害鍊硷紝鏌ヨ鐩爣鍥捐〃鐨勮繃婊ゆ暟鎹?
     *
     * @param request 璇锋眰鍙傛暟锛屽寘鍚?sourceChartId, dimensionValue, targetChartIds
     * @return 鐩爣鍥捐〃ID鍒版暟鎹殑鏄犲皠
     */
    @PostMapping("/linked-data")
    public Result<Map<Long, List<Map<String, Object>>>> getLinkedChartData(
            @RequestBody Map<String, Object> request) {
        Long sourceChartId = Long.valueOf(request.get("sourceChartId").toString());
        String dimensionValue = String.valueOf(request.get("dimensionValue"));
        
        @SuppressWarnings("unchecked")
        List<Number> targetChartIdNumbers = (List<Number>) request.get("targetChartIds");
        List<Long> targetChartIds = targetChartIdNumbers.stream()
                .map(Number::longValue)
                .toList();

        log.info("Getting linked chart data: sourceChartId={}, dimensionValue={}, targetChartIds={}", 
                sourceChartId, dimensionValue, targetChartIds);

        Map<Long, List<Map<String, Object>>> data = chartLinkService.getLinkedChartData(
                sourceChartId, dimensionValue, targetChartIds);
        
        return Result.success(data);
    }
}
