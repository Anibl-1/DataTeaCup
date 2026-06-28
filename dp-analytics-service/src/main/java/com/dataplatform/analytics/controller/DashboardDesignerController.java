package com.dataplatform.analytics.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dataplatform.common.Result;
import com.dataplatform.common.annotation.RequirePermission;

import com.dataplatform.data.dto.FilterConfig;
import com.dataplatform.data.dto.WidgetConfig;
import com.dataplatform.data.entity.DashboardConfig;
import com.dataplatform.data.entity.DashboardTemplate;
import com.dataplatform.data.service.DashboardDesignerService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 浠〃鐩樿璁″櫒鎺у埗鍣?
 * 鎻愪緵浠〃鐩樼殑鍒涘缓銆佺紪杈戙€佸竷灞€绠＄悊銆佺粍浠剁鐞嗗拰妯℃澘鍔熻兘
 *
 * @author dataplatform
 */
@Slf4j
@RestController
@RequestMapping("/dashboard/designer")
@RequirePermission("dashboard:read")
public class DashboardDesignerController {

    @Autowired
    private DashboardDesignerService dashboardDesignerService;

    // ==================== Dashboard CRUD ====================

    /**
     * 鍒涘缓浠〃鐩?
     */
    @PostMapping
    public Result<DashboardConfig> createDashboard(@RequestBody DashboardConfig config, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        config.setCreateBy(userId);
        DashboardConfig created = dashboardDesignerService.createDashboard(config);
        return Result.success(created);
    }

    /**
     * 鏇存柊浠〃鐩?
     */
    @PutMapping("/{id}")
    public Result<DashboardConfig> updateDashboard(@PathVariable Long id, @RequestBody DashboardConfig config) {
        DashboardConfig updated = dashboardDesignerService.updateDashboard(id, config);
        return Result.success(updated);
    }

    /**
     * 鑾峰彇浠〃鐩樿鎯?
     */
    @GetMapping("/{id}")
    public Result<DashboardConfig> getDashboard(@PathVariable Long id) {
        DashboardConfig config = dashboardDesignerService.getDashboard(id);
        return Result.success(config);
    }

    /**
     * 鍒犻櫎浠〃鐩?
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteDashboard(@PathVariable Long id) {
        dashboardDesignerService.deleteDashboard(id);
        return Result.success();
    }

    /**
     * 鍒嗛〉鏌ヨ浠〃鐩樺垪琛?
     */
    @GetMapping("/list")
    public Result<Page<DashboardConfig>> listDashboards(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Page<DashboardConfig> result = dashboardDesignerService.listDashboards(userId, page, size);
        return Result.success(result);
    }

    // ==================== Layout Management ====================

    /**
     * 淇濆瓨浠〃鐩樺竷灞€
     */
    @PostMapping("/{id}/layout")
    public Result<Void> saveLayout(@PathVariable Long id, @RequestBody String layoutJson) {
        dashboardDesignerService.saveLayout(id, layoutJson);
        return Result.success();
    }

    /**
     * 鑾峰彇浠〃鐩樺竷灞€
     */
    @GetMapping("/{id}/layout")
    public Result<String> getLayout(@PathVariable Long id) {
        String layout = dashboardDesignerService.getLayout(id);
        return Result.success(layout);
    }

    // ==================== Widget Management ====================

    /**
     * 娣诲姞缁勪欢鍒颁华琛ㄧ洏
     */
    @PostMapping("/{id}/widget")
    public Result<WidgetConfig> addWidget(@PathVariable Long id, @RequestBody WidgetConfig widget) {
        WidgetConfig added = dashboardDesignerService.addWidget(id, widget);
        return Result.success(added);
    }

    /**
     * 鏇存柊浠〃鐩樼粍浠?
     */
    @PutMapping("/{dashboardId}/widget/{widgetId}")
    public Result<WidgetConfig> updateWidget(
            @PathVariable Long dashboardId,
            @PathVariable String widgetId,
            @RequestBody WidgetConfig widget) {
        WidgetConfig updated = dashboardDesignerService.updateWidget(dashboardId, widgetId, widget);
        return Result.success(updated);
    }

    /**
     * 鍒犻櫎浠〃鐩樼粍浠?
     */
    @DeleteMapping("/{dashboardId}/widget/{widgetId}")
    public Result<Void> removeWidget(@PathVariable Long dashboardId, @PathVariable String widgetId) {
        dashboardDesignerService.removeWidget(dashboardId, widgetId);
        return Result.success();
    }

    /**
     * 鑾峰彇浠〃鐩樼殑鎵€鏈夌粍浠?
     */
    @GetMapping("/{id}/widgets")
    public Result<List<WidgetConfig>> getWidgets(@PathVariable Long id) {
        List<WidgetConfig> widgets = dashboardDesignerService.getWidgets(id);
        return Result.success(widgets);
    }

    // ==================== Template Operations ====================

    /**
     * 鑾峰彇鎵€鏈夋ā鏉?
     */
    @GetMapping("/templates")
    public Result<List<DashboardTemplate>> getTemplates() {
        List<DashboardTemplate> templates = dashboardDesignerService.getTemplates();
        return Result.success(templates);
    }

    /**
     * 鍒嗛〉鏌ヨ妯℃澘鍒楄〃
     */
    @GetMapping("/templates/list")
    public Result<Page<DashboardTemplate>> listTemplates(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<DashboardTemplate> result = dashboardDesignerService.listTemplates(page, size);
        return Result.success(result);
    }

    /**
     * 浠庢ā鏉垮垱寤轰华琛ㄧ洏
     */
    @PostMapping("/from-template")
    public Result<DashboardConfig> createFromTemplate(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Long templateId = Long.valueOf(params.get("templateId").toString());
        String name = (String) params.get("name");
        
        DashboardConfig config = dashboardDesignerService.createFromTemplate(templateId, name);
        
        // 璁剧疆鍒涘缓浜?
        Long userId = (Long) request.getAttribute("userId");
        config.setCreateBy(userId);
        
        return Result.success(config);
    }

    /**
     * 灏嗕华琛ㄧ洏淇濆瓨涓烘ā鏉?
     */
    @PostMapping("/{id}/save-as-template")
    public Result<DashboardTemplate> saveAsTemplate(@PathVariable Long id, @RequestBody Map<String, String> params) {
        String templateName = params.get("name");
        DashboardTemplate template = dashboardDesignerService.saveAsTemplate(id, templateName);
        return Result.success(template);
    }

    // ==================== Global Filter Management ====================

    /**
     * 璁剧疆鍏ㄥ眬绛涢€夊櫒
     */
    @PostMapping("/{id}/filters")
    public Result<Void> setGlobalFilters(@PathVariable Long id, @RequestBody List<FilterConfig> filters) {
        dashboardDesignerService.setGlobalFilters(id, filters);
        return Result.success();
    }

    /**
     * 鑾峰彇鍏ㄥ眬绛涢€夊櫒
     */
    @GetMapping("/{id}/filters")
    public Result<List<FilterConfig>> getGlobalFilters(@PathVariable Long id) {
        List<FilterConfig> filters = dashboardDesignerService.getGlobalFilters(id);
        return Result.success(filters);
    }

    // ==================== Additional Operations ====================

    /**
     * 澶嶅埗浠〃鐩?
     */
    @PostMapping("/{id}/duplicate")
    public Result<DashboardConfig> duplicateDashboard(@PathVariable Long id, HttpServletRequest request) {
        DashboardConfig original = dashboardDesignerService.getDashboard(id);
        
        DashboardConfig copy = new DashboardConfig();
        copy.setName(original.getName() + " (鍓湰)");
        copy.setDescription(original.getDescription());
        copy.setLayoutJson(original.getLayoutJson());
        copy.setGlobalFiltersJson(original.getGlobalFiltersJson());
        copy.setLinkConfigJson(original.getLinkConfigJson());
        copy.setStatus(1);
        
        Long userId = (Long) request.getAttribute("userId");
        copy.setCreateBy(userId);
        
        DashboardConfig created = dashboardDesignerService.createDashboard(copy);
        return Result.success(created);
    }
}
