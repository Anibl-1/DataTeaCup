package com.dataplatform.data.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dataplatform.data.dto.FilterConfig;
import com.dataplatform.data.dto.WidgetConfig;
import com.dataplatform.data.entity.DashboardConfig;
import com.dataplatform.data.entity.DashboardTemplate;
import com.dataplatform.data.mapper.DashboardConfigMapper;
import com.dataplatform.data.mapper.DashboardTemplateMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 仪表盘设计器服务
 * 管理仪表盘配置、布局、组件和模板
 * 
 * @author dataplatform
 */
@Slf4j
@Service
public class DashboardDesignerService {

    @Autowired
    private DashboardConfigMapper dashboardConfigMapper;

    @Autowired
    private DashboardTemplateMapper dashboardTemplateMapper;

    @Autowired
    private ObjectMapper objectMapper;

    // ==================== Dashboard CRUD Operations ====================

    /**
     * 创建新仪表盘
     *
     * @param config 仪表盘配置
     * @return 创建后的仪表盘配置（包含ID）
     */
    @Transactional
    public DashboardConfig createDashboard(DashboardConfig config) {
        log.info("Creating new dashboard: {}", config.getName());
        config.setCreateTime(LocalDateTime.now());
        config.setUpdateTime(LocalDateTime.now());
        if (config.getStatus() == null) {
            config.setStatus(1);
        }
        dashboardConfigMapper.insert(config);
        return config;
    }

    /**
     * 更新仪表盘
     *
     * @param id     仪表盘ID
     * @param config 更新的配置
     * @return 更新后的仪表盘配置
     */
    @Transactional
    public DashboardConfig updateDashboard(Long id, DashboardConfig config) {
        log.info("Updating dashboard: {}", id);
        DashboardConfig existing = dashboardConfigMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("Dashboard not found: " + id);
        }
        config.setId(id);
        config.setUpdateTime(LocalDateTime.now());
        config.setCreateTime(existing.getCreateTime());
        config.setCreateBy(existing.getCreateBy());
        dashboardConfigMapper.updateById(config);
        return config;
    }


    /**
     * 删除仪表盘
     *
     * @param id 仪表盘ID
     */
    @Transactional
    public void deleteDashboard(Long id) {
        log.info("Deleting dashboard: {}", id);
        DashboardConfig existing = dashboardConfigMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("Dashboard not found: " + id);
        }
        dashboardConfigMapper.deleteById(id);
    }

    /**
     * 获取仪表盘
     *
     * @param id 仪表盘ID
     * @return 仪表盘配置
     */
    public DashboardConfig getDashboard(Long id) {
        log.debug("Getting dashboard: {}", id);
        DashboardConfig config = dashboardConfigMapper.selectById(id);
        if (config == null) {
            throw new RuntimeException("Dashboard not found: " + id);
        }
        return config;
    }

    /**
     * 分页查询用户的仪表盘列表
     *
     * @param userId 用户ID
     * @param page   页码（从1开始）
     * @param size   每页大小
     * @return 分页结果
     */
    public Page<DashboardConfig> listDashboards(Long userId, int page, int size) {
        log.debug("Listing dashboards for user: {}, page: {}, size: {}", userId, page, size);
        Page<DashboardConfig> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<DashboardConfig> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(DashboardConfig::getCreateBy, userId);
        }
        wrapper.eq(DashboardConfig::getStatus, 1);
        wrapper.orderByDesc(DashboardConfig::getUpdateTime);
        return dashboardConfigMapper.selectPage(pageParam, wrapper);
    }

    // ==================== Layout Management ====================

    /**
     * 保存仪表盘布局
     *
     * @param dashboardId 仪表盘ID
     * @param layoutJson  布局配置JSON
     */
    @Transactional
    public void saveLayout(Long dashboardId, String layoutJson) {
        log.info("Saving layout for dashboard: {}", dashboardId);
        DashboardConfig config = dashboardConfigMapper.selectById(dashboardId);
        if (config == null) {
            throw new RuntimeException("Dashboard not found: " + dashboardId);
        }
        config.setLayoutJson(layoutJson);
        config.setUpdateTime(LocalDateTime.now());
        dashboardConfigMapper.updateById(config);
    }

    /**
     * 获取仪表盘布局
     *
     * @param dashboardId 仪表盘ID
     * @return 布局配置JSON
     */
    public String getLayout(Long dashboardId) {
        log.debug("Getting layout for dashboard: {}", dashboardId);
        DashboardConfig config = dashboardConfigMapper.selectById(dashboardId);
        if (config == null) {
            throw new RuntimeException("Dashboard not found: " + dashboardId);
        }
        return config.getLayoutJson();
    }

    // ==================== Widget Management ====================

    /**
     * 添加组件到仪表盘
     *
     * @param dashboardId 仪表盘ID
     * @param widget      组件配置
     * @return 添加后的组件配置（包含生成的ID）
     */
    @Transactional
    public WidgetConfig addWidget(Long dashboardId, WidgetConfig widget) {
        log.info("Adding widget to dashboard: {}", dashboardId);
        DashboardConfig config = dashboardConfigMapper.selectById(dashboardId);
        if (config == null) {
            throw new RuntimeException("Dashboard not found: " + dashboardId);
        }

        List<WidgetConfig> widgets = parseWidgets(config.getLayoutJson());
        
        // 生成唯一ID
        if (widget.getI() == null || widget.getI().isEmpty()) {
            widget.setI(UUID.randomUUID().toString().substring(0, 8));
        }
        
        widgets.add(widget);
        config.setLayoutJson(serializeWidgets(widgets));
        config.setUpdateTime(LocalDateTime.now());
        dashboardConfigMapper.updateById(config);
        
        return widget;
    }


    /**
     * 更新仪表盘中的组件
     *
     * @param dashboardId 仪表盘ID
     * @param widgetId    组件ID
     * @param widget      更新的组件配置
     * @return 更新后的组件配置
     */
    @Transactional
    public WidgetConfig updateWidget(Long dashboardId, String widgetId, WidgetConfig widget) {
        log.info("Updating widget {} in dashboard: {}", widgetId, dashboardId);
        DashboardConfig config = dashboardConfigMapper.selectById(dashboardId);
        if (config == null) {
            throw new RuntimeException("Dashboard not found: " + dashboardId);
        }

        List<WidgetConfig> widgets = parseWidgets(config.getLayoutJson());
        boolean found = false;
        
        for (int i = 0; i < widgets.size(); i++) {
            if (widgetId.equals(widgets.get(i).getI())) {
                widget.setI(widgetId);
                widgets.set(i, widget);
                found = true;
                break;
            }
        }
        
        if (!found) {
            throw new RuntimeException("Widget not found: " + widgetId);
        }
        
        config.setLayoutJson(serializeWidgets(widgets));
        config.setUpdateTime(LocalDateTime.now());
        dashboardConfigMapper.updateById(config);
        
        return widget;
    }

    /**
     * 从仪表盘移除组件
     *
     * @param dashboardId 仪表盘ID
     * @param widgetId    组件ID
     */
    @Transactional
    public void removeWidget(Long dashboardId, String widgetId) {
        log.info("Removing widget {} from dashboard: {}", widgetId, dashboardId);
        DashboardConfig config = dashboardConfigMapper.selectById(dashboardId);
        if (config == null) {
            throw new RuntimeException("Dashboard not found: " + dashboardId);
        }

        List<WidgetConfig> widgets = parseWidgets(config.getLayoutJson());
        boolean removed = widgets.removeIf(w -> widgetId.equals(w.getI()));
        
        if (!removed) {
            throw new RuntimeException("Widget not found: " + widgetId);
        }
        
        config.setLayoutJson(serializeWidgets(widgets));
        config.setUpdateTime(LocalDateTime.now());
        dashboardConfigMapper.updateById(config);
    }

    /**
     * 获取仪表盘的所有组件
     *
     * @param dashboardId 仪表盘ID
     * @return 组件列表
     */
    public List<WidgetConfig> getWidgets(Long dashboardId) {
        log.debug("Getting widgets for dashboard: {}", dashboardId);
        DashboardConfig config = dashboardConfigMapper.selectById(dashboardId);
        if (config == null) {
            throw new RuntimeException("Dashboard not found: " + dashboardId);
        }
        return parseWidgets(config.getLayoutJson());
    }

    // ==================== Template Operations ====================

    /**
     * 将仪表盘保存为模板
     *
     * @param dashboardId  仪表盘ID
     * @param templateName 模板名称
     * @return 创建的模板
     */
    @Transactional
    public DashboardTemplate saveAsTemplate(Long dashboardId, String templateName) {
        log.info("Saving dashboard {} as template: {}", dashboardId, templateName);
        DashboardConfig config = dashboardConfigMapper.selectById(dashboardId);
        if (config == null) {
            throw new RuntimeException("Dashboard not found: " + dashboardId);
        }

        DashboardTemplate template = new DashboardTemplate();
        template.setName(templateName);
        template.setLayoutJson(config.getLayoutJson());
        template.setDescription(config.getDescription());
        template.setCategory("自定义");
        
        dashboardTemplateMapper.insert(template);
        return template;
    }


    /**
     * 从模板创建仪表盘
     *
     * @param templateId 模板ID
     * @param name       仪表盘名称
     * @return 创建的仪表盘配置
     */
    @Transactional
    public DashboardConfig createFromTemplate(Long templateId, String name) {
        log.info("Creating dashboard from template: {}, name: {}", templateId, name);
        DashboardTemplate template = dashboardTemplateMapper.selectById(templateId);
        if (template == null) {
            throw new RuntimeException("Template not found: " + templateId);
        }

        DashboardConfig config = new DashboardConfig();
        config.setName(name);
        config.setDescription("基于模板 [" + template.getName() + "] 创建");
        config.setLayoutJson(template.getLayoutJson());
        config.setTemplateId(templateId);
        config.setStatus(1);
        config.setCreateTime(LocalDateTime.now());
        config.setUpdateTime(LocalDateTime.now());
        
        dashboardConfigMapper.insert(config);
        return config;
    }

    /**
     * 分页查询可用模板列表
     *
     * @param page 页码（从1开始）
     * @param size 每页大小
     * @return 分页结果
     */
    public Page<DashboardTemplate> listTemplates(int page, int size) {
        log.debug("Listing templates, page: {}, size: {}", page, size);
        Page<DashboardTemplate> pageParam = new Page<>(page, size);
        return dashboardTemplateMapper.selectPage(pageParam, null);
    }

    /**
     * 获取所有模板（不分页）
     *
     * @return 模板列表
     */
    public List<DashboardTemplate> getTemplates() {
        log.debug("Getting all templates");
        return dashboardTemplateMapper.selectList(null);
    }

    // ==================== Global Filter Management ====================

    /**
     * 设置仪表盘的全局筛选器
     *
     * @param dashboardId 仪表盘ID
     * @param filters     筛选器配置列表
     */
    @Transactional
    public void setGlobalFilters(Long dashboardId, List<FilterConfig> filters) {
        log.info("Setting global filters for dashboard: {}", dashboardId);
        DashboardConfig config = dashboardConfigMapper.selectById(dashboardId);
        if (config == null) {
            throw new RuntimeException("Dashboard not found: " + dashboardId);
        }

        try {
            String filtersJson = objectMapper.writeValueAsString(filters);
            config.setGlobalFiltersJson(filtersJson);
            config.setUpdateTime(LocalDateTime.now());
            dashboardConfigMapper.updateById(config);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize filters", e);
            throw new RuntimeException("Failed to serialize filters", e);
        }
    }

    /**
     * 获取仪表盘的全局筛选器
     *
     * @param dashboardId 仪表盘ID
     * @return 筛选器配置列表
     */
    public List<FilterConfig> getGlobalFilters(Long dashboardId) {
        log.debug("Getting global filters for dashboard: {}", dashboardId);
        DashboardConfig config = dashboardConfigMapper.selectById(dashboardId);
        if (config == null) {
            throw new RuntimeException("Dashboard not found: " + dashboardId);
        }

        String filtersJson = config.getGlobalFiltersJson();
        if (filtersJson == null || filtersJson.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            return objectMapper.readValue(filtersJson, new TypeReference<List<FilterConfig>>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to parse filters JSON", e);
            return new ArrayList<>();
        }
    }

    // ==================== Helper Methods ====================

    /**
     * 解析布局JSON为组件列表
     */
    private List<WidgetConfig> parseWidgets(String layoutJson) {
        if (layoutJson == null || layoutJson.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(layoutJson, new TypeReference<List<WidgetConfig>>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to parse layout JSON", e);
            return new ArrayList<>();
        }
    }

    /**
     * 序列化组件列表为JSON
     */
    private String serializeWidgets(List<WidgetConfig> widgets) {
        try {
            return objectMapper.writeValueAsString(widgets);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize widgets", e);
            throw new RuntimeException("Failed to serialize widgets", e);
        }
    }
}
