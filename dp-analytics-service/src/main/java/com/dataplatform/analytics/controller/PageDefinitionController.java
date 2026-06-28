package com.dataplatform.analytics.controller;

import com.dataplatform.common.annotation.OperationLog;
import com.dataplatform.common.annotation.RequirePermission;
import com.dataplatform.common.PageResult;
import com.dataplatform.common.Result;
import com.dataplatform.data.entity.PageChart;
import com.dataplatform.data.entity.PageDefinition;
import com.dataplatform.data.service.PageDefinitionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 页面定义控制器
 * 
 * @author dataplatform
 */
@RestController
@RequestMapping("/page-definition")
@RequirePermission("page:read")
public class PageDefinitionController {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private PageDefinitionService pageDefinitionService;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @PostConstruct
    private void initSchema() {
        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS sys_page_version (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, page_id BIGINT NOT NULL, " +
                "layout_config TEXT, charts_json TEXT, parameter_panel TEXT, " +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP, remark VARCHAR(200))");
        } catch (Exception e) {
            // 表已存在或数据库不支持该语法时忽略，但记录日志以便排查
            org.slf4j.LoggerFactory.getLogger(getClass()).debug("初始化sys_page_version表: {}", e.getMessage());
        }
    }
    
    /**
     * 获取页面定义列表（分页，按布局模式过滤）
     */
    @GetMapping("/list")
    public Result<PageResult<PageDefinition>> getPageDefinitionList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String layoutMode) {
        page = Math.max(1, page);
        pageSize = Math.max(1, Math.min(200, pageSize));
        List<PageDefinition> list = pageDefinitionService.getPageDefinitionList(page, pageSize, keyword, layoutMode);
        long total = pageDefinitionService.getPageDefinitionCount(keyword, layoutMode);
        
        PageResult<PageDefinition> pageResult = new PageResult<>(list, total);
        return Result.success(pageResult);
    }
    
    /**
     * 获取各布局模式的页面数量（用于Tab计数）
     */
    @GetMapping("/counts")
    public Result<Map<String, Long>> getLayoutModeCounts() {
        Map<String, Long> counts = new java.util.LinkedHashMap<>();
        counts.put("desktop", pageDefinitionService.countByLayoutMode(null));
        counts.put("mobile", pageDefinitionService.countByLayoutMode("mobile"));
        counts.put("bigscreen", pageDefinitionService.countByLayoutMode("bigscreen"));
        return Result.success(counts);
    }
    
    /**
     * 根据ID获取页面定义
     */
    @GetMapping("/{id}")
    public Result<PageDefinition> getPageDefinitionById(@PathVariable Long id) {
        PageDefinition page = pageDefinitionService.getPageDefinitionById(id);
        return Result.success(page);
    }
    
    /**
     * 根据编码获取页面定义
     */
    @GetMapping("/code/{code}")
    public Result<PageDefinition> getPageDefinitionByCode(@PathVariable String code) {
        PageDefinition page = pageDefinitionService.getPageDefinitionByCode(code);
        return Result.success(page);
    }
    
    /**
     * 创建页面定义
     */
    @RequirePermission("page:manage")
    @OperationLog(module = "页面管理", type = OperationLog.OperationType.CREATE, description = "创建页面")
    @PostMapping
    public Result<PageDefinition> createPageDefinition(@RequestBody Map<String, Object> params) {
        PageDefinition page = new PageDefinition();
        page.setPageName((String) params.get("pageName"));
        page.setPageCode((String) params.get("pageCode"));
        setJsonField(page::setLayoutConfig, params.get("layoutConfig"));
        page.setDescription((String) params.get("description"));
        page.setTheme((String) params.get("theme"));
        setJsonField(page::setThemeConfig, params.get("themeConfig"));
        
        // 处理参数面板配置
        Object parameterPanelObj = params.get("parameterPanel");
        if (parameterPanelObj != null) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                String parameterPanelJson = mapper.writeValueAsString(parameterPanelObj);
                page.setParameterPanel(parameterPanelJson);
            } catch (Exception e) {
                // 如果已经是字符串，直接使用
                if (parameterPanelObj instanceof String) {
                    page.setParameterPanel((String) parameterPanelObj);
                }
            }
        }
        
        if (params.get("status") != null) {
            page.setStatus(((Number) params.get("status")).intValue());
        }
        
        // 布局模式相关字段
        page.setLayoutMode((String) params.getOrDefault("layoutMode", "desktop"));
        setJsonField(page::setBigscreenConfig, params.get("bigscreenConfig"));
        setJsonField(page::setMobileLayoutConfig, params.get("mobileLayoutConfig"));
        if (params.get("projectId") != null) {
            page.setProjectId(((Number) params.get("projectId")).longValue());
        }
        
        List<PageChart> charts = parseChartsData(params);
        
        PageDefinition created = pageDefinitionService.createPageDefinition(page, charts);
        return Result.success(created);
    }
    
    /**
     * 更新页面定义
     */
    @RequirePermission("page:design")
    @OperationLog(module = "页面管理", type = OperationLog.OperationType.UPDATE, description = "更新页面")
    @PutMapping("/{id}")
    public Result<PageDefinition> updatePageDefinition(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        PageDefinition page = new PageDefinition();
        page.setId(id);
        page.setPageName((String) params.get("pageName"));
        page.setPageCode((String) params.get("pageCode"));
        setJsonField(page::setLayoutConfig, params.get("layoutConfig"));
        page.setDescription((String) params.get("description"));
        page.setTheme((String) params.get("theme"));
        setJsonField(page::setThemeConfig, params.get("themeConfig"));
        
        // 处理参数面板配置
        Object parameterPanelObj = params.get("parameterPanel");
        if (parameterPanelObj != null) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                String parameterPanelJson = mapper.writeValueAsString(parameterPanelObj);
                page.setParameterPanel(parameterPanelJson);
            } catch (Exception e) {
                if (parameterPanelObj instanceof String) {
                    page.setParameterPanel((String) parameterPanelObj);
                }
            }
        } else {
            page.setParameterPanel(null);
        }
        
        if (params.get("status") != null) {
            page.setStatus(((Number) params.get("status")).intValue());
        }
        
        // 布局模式相关字段
        if (params.containsKey("layoutMode")) {
            page.setLayoutMode((String) params.get("layoutMode"));
        }
        setJsonField(page::setBigscreenConfig, params.get("bigscreenConfig"));
        setJsonField(page::setMobileLayoutConfig, params.get("mobileLayoutConfig"));
        if (params.get("projectId") != null) {
            page.setProjectId(((Number) params.get("projectId")).longValue());
        }
        
        List<PageChart> charts = parseChartsData(params);
        
        // 保存版本快照
        try {
            PageDefinition existing = pageDefinitionService.getPageDefinitionById(id);
            if (existing != null) {
                jdbcTemplate.update("INSERT INTO sys_page_version (page_id, layout_config, parameter_panel, remark) VALUES (?, ?, ?, ?)",
                    id, existing.getLayoutConfig(), existing.getParameterPanel(), "自动保存");
            }
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("保存页面版本快照失败: {}", e.getMessage());
        }
        
        PageDefinition updated = pageDefinitionService.updatePageDefinition(page, charts);
        return Result.success(updated);
    }
    
    /**
     * 删除页面定义
     */
    @RequirePermission("page:manage")
    @OperationLog(module = "页面管理", type = OperationLog.OperationType.DELETE, description = "删除页面")
    @DeleteMapping("/{id}")
    public Result<Void> deletePageDefinition(@PathVariable Long id) {
        pageDefinitionService.deletePageDefinition(id);
        return Result.success(null);
    }
    
    /**
     * 获取移动端已发布页面列表（mobileEnabled=1）
     */
    @GetMapping("/mobile-enabled")
    public Result<List<Map<String, Object>>> getMobileEnabledPages() {
        List<Map<String, Object>> pages = jdbcTemplate.queryForList(
            "SELECT id, page_name, page_code, description, layout_mode FROM page_definition " +
            "WHERE mobile_enabled = 1 AND status = 1 ORDER BY update_time DESC LIMIT 100");
        return Result.success(pages);
    }

    /**
     * 更新页面移动端配置
     */
    @RequirePermission("page:design")
    @PutMapping("/{id}/mobile")
    public Result<Void> updatePageMobileEnabled(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        Integer mobileEnabled = params.get("mobileEnabled") != null ? ((Number) params.get("mobileEnabled")).intValue() : 0;
        jdbcTemplate.update("UPDATE page_definition SET mobile_enabled = ? WHERE id = ?", mobileEnabled, id);
        return Result.success(null);
    }
    
    /**
     * 获取页面模板列表（系统预设模板）
     */
    @GetMapping("/templates")
    public Result<List<Map<String, Object>>> getPageTemplates() {
        List<Map<String, Object>> templates = new java.util.ArrayList<>();
        
        // ====== 网格系统：padding=16, gap=16, 可用宽度=1168 ======
        // 4列: w=280, x=[16, 312, 608, 904]
        // 3列: w=378, x=[16, 410, 804]
        // 2列: w=576, x=[16, 608]
        // 全宽: w=1168, x=16
        
        // 模板1: 数据统计看板 — 4 KPI + 2图表 + 1全宽图表
        Map<String, Object> t1 = new java.util.LinkedHashMap<>();
        t1.put("id", "stats-dashboard");
        t1.put("name", "数据统计看板");
        t1.put("description", "顶部KPI卡片 + 下方柱状图和饼图的经典看板布局");
        t1.put("thumbnail", "stats");
        t1.put("layoutConfig", "{\"width\":1200,\"height\":960}");
        t1.put("charts", List.of(
            Map.of("mode", "inline", "left", 16, "top", 16, "width", 280, "height", 130, "inlineConfig", "{\"chartType\":\"kpi\",\"chartName\":\"KPI卡片1\",\"title\":\"总销售额\"}"),
            Map.of("mode", "inline", "left", 312, "top", 16, "width", 280, "height", 130, "inlineConfig", "{\"chartType\":\"kpi\",\"chartName\":\"KPI卡片2\",\"title\":\"订单数量\"}"),
            Map.of("mode", "inline", "left", 608, "top", 16, "width", 280, "height", 130, "inlineConfig", "{\"chartType\":\"kpi\",\"chartName\":\"KPI卡片3\",\"title\":\"用户数\"}"),
            Map.of("mode", "inline", "left", 904, "top", 16, "width", 280, "height", 130, "inlineConfig", "{\"chartType\":\"kpi\",\"chartName\":\"KPI卡片4\",\"title\":\"转化率\"}"),
            Map.of("mode", "inline", "left", 16, "top", 162, "width", 576, "height", 380, "inlineConfig", "{\"chartType\":\"bar\",\"chartName\":\"柱状图\",\"title\":\"月度销售趋势\"}"),
            Map.of("mode", "inline", "left", 608, "top", 162, "width", 576, "height", 380, "inlineConfig", "{\"chartType\":\"pie\",\"chartName\":\"饼图\",\"title\":\"销售分类占比\"}"),
            Map.of("mode", "inline", "left", 16, "top", 558, "width", 1168, "height", 380, "inlineConfig", "{\"chartType\":\"line\",\"chartName\":\"折线图\",\"title\":\"每日趋势分析\"}")
        ));
        templates.add(t1);
        
        // 模板2: 报表展示页 — 全宽表格
        Map<String, Object> t2 = new java.util.LinkedHashMap<>();
        t2.put("id", "report-view");
        t2.put("name", "报表展示页");
        t2.put("description", "带查询条件的报表表格布局，适合数据查询展示");
        t2.put("thumbnail", "report");
        t2.put("layoutConfig", "{\"width\":1200,\"height\":680}");
        t2.put("charts", List.of(
            Map.of("mode", "inline", "left", 16, "top", 16, "width", 1168, "height", 640, "inlineConfig", "{\"chartType\":\"table\",\"chartName\":\"数据表格\",\"title\":\"数据明细表\"}")
        ));
        templates.add(t2);
        
        // 模板3: 图表三分屏 — 3列等宽
        Map<String, Object> t3 = new java.util.LinkedHashMap<>();
        t3.put("id", "triple-chart");
        t3.put("name", "图表三分屏");
        t3.put("description", "三个等宽图表并排布局，适合对比分析");
        t3.put("thumbnail", "chart3");
        t3.put("layoutConfig", "{\"width\":1200,\"height\":460}");
        t3.put("charts", List.of(
            Map.of("mode", "inline", "left", 16, "top", 16, "width", 378, "height", 420, "inlineConfig", "{\"chartType\":\"bar\",\"chartName\":\"柱状图\",\"title\":\"分类对比\"}"),
            Map.of("mode", "inline", "left", 410, "top", 16, "width", 378, "height", 420, "inlineConfig", "{\"chartType\":\"pie\",\"chartName\":\"饼图\",\"title\":\"占比分析\"}"),
            Map.of("mode", "inline", "left", 804, "top", 16, "width", 380, "height", 420, "inlineConfig", "{\"chartType\":\"line\",\"chartName\":\"折线图\",\"title\":\"趋势变化\"}")
        ));
        templates.add(t3);
        
        // 模板4: 左右分栏 — 左1/3 + 右2/3
        Map<String, Object> t4 = new java.util.LinkedHashMap<>();
        t4.put("id", "left-right-split");
        t4.put("name", "左右分栏");
        t4.put("description", "左侧饼图/指标 + 右侧表格，适合概览+明细场景");
        t4.put("thumbnail", "split");
        t4.put("layoutConfig", "{\"width\":1200,\"height\":700}");
        t4.put("charts", List.of(
            Map.of("mode", "inline", "left", 16, "top", 16, "width", 378, "height", 326, "inlineConfig", "{\"chartType\":\"pie\",\"chartName\":\"饼图\",\"title\":\"数据分布\"}"),
            Map.of("mode", "inline", "left", 16, "top", 358, "width", 378, "height", 326, "inlineConfig", "{\"chartType\":\"bar\",\"chartName\":\"柱状图\",\"title\":\"排名TOP10\"}"),
            Map.of("mode", "inline", "left", 410, "top", 16, "width", 774, "height", 668, "inlineConfig", "{\"chartType\":\"table\",\"chartName\":\"数据表格\",\"title\":\"数据明细\"}")
        ));
        templates.add(t4);
        
        // 模板5: 四宫格 — 2x2等分
        Map<String, Object> t5 = new java.util.LinkedHashMap<>();
        t5.put("id", "quad-grid");
        t5.put("name", "四宫格看板");
        t5.put("description", "2x2等分四个图表区域，适合多维度监控");
        t5.put("thumbnail", "grid4");
        t5.put("layoutConfig", "{\"width\":1200,\"height\":780}");
        t5.put("charts", List.of(
            Map.of("mode", "inline", "left", 16, "top", 16, "width", 576, "height", 360, "inlineConfig", "{\"chartType\":\"line\",\"chartName\":\"折线图\",\"title\":\"访问量趋势\"}"),
            Map.of("mode", "inline", "left", 608, "top", 16, "width", 576, "height", 360, "inlineConfig", "{\"chartType\":\"bar\",\"chartName\":\"柱状图\",\"title\":\"部门业绩\"}"),
            Map.of("mode", "inline", "left", 16, "top", 392, "width", 576, "height", 360, "inlineConfig", "{\"chartType\":\"pie\",\"chartName\":\"饼图\",\"title\":\"来源分布\"}"),
            Map.of("mode", "inline", "left", 608, "top", 392, "width", 576, "height", 360, "inlineConfig", "{\"chartType\":\"scatter\",\"chartName\":\"散点图\",\"title\":\"相关性分析\"}")
        ));
        templates.add(t5);
        
        // 模板6: 大屏监控 — 1920设计宽度, 6 KPI + 大图 + 底部3图
        Map<String, Object> t6 = new java.util.LinkedHashMap<>();
        t6.put("id", "monitor-screen");
        t6.put("name", "大屏监控");
        t6.put("description", "顶部指标行 + 中间大图 + 底部小图，适合数据大屏展示");
        t6.put("thumbnail", "monitor");
        t6.put("layoutConfig", "{\"width\":1920,\"height\":1080}");
        t6.put("charts", List.of(
            Map.of("mode", "inline", "left", 20, "top", 20, "width", 296, "height", 130, "inlineConfig", "{\"chartType\":\"kpi\",\"chartName\":\"指标1\",\"title\":\"实时在线\"}"),
            Map.of("mode", "inline", "left", 336, "top", 20, "width", 296, "height", 130, "inlineConfig", "{\"chartType\":\"kpi\",\"chartName\":\"指标2\",\"title\":\"今日访问\"}"),
            Map.of("mode", "inline", "left", 652, "top", 20, "width", 296, "height", 130, "inlineConfig", "{\"chartType\":\"kpi\",\"chartName\":\"指标3\",\"title\":\"处理任务\"}"),
            Map.of("mode", "inline", "left", 968, "top", 20, "width", 296, "height", 130, "inlineConfig", "{\"chartType\":\"kpi\",\"chartName\":\"指标4\",\"title\":\"告警数量\"}"),
            Map.of("mode", "inline", "left", 1284, "top", 20, "width", 296, "height", 130, "inlineConfig", "{\"chartType\":\"kpi\",\"chartName\":\"指标5\",\"title\":\"系统负载\"}"),
            Map.of("mode", "inline", "left", 1600, "top", 20, "width", 300, "height", 130, "inlineConfig", "{\"chartType\":\"kpi\",\"chartName\":\"指标6\",\"title\":\"存储使用\"}"),
            Map.of("mode", "inline", "left", 20, "top", 170, "width", 1240, "height", 500, "inlineConfig", "{\"chartType\":\"line\",\"chartName\":\"主图表\",\"title\":\"实时数据监控\"}"),
            Map.of("mode", "inline", "left", 1280, "top", 170, "width", 620, "height", 500, "inlineConfig", "{\"chartType\":\"pie\",\"chartName\":\"饼图\",\"title\":\"资源分配\"}"),
            Map.of("mode", "inline", "left", 20, "top", 690, "width", 616, "height", 370, "inlineConfig", "{\"chartType\":\"bar\",\"chartName\":\"柱状图\",\"title\":\"服务响应时间\"}"),
            Map.of("mode", "inline", "left", 656, "top", 690, "width", 616, "height", 370, "inlineConfig", "{\"chartType\":\"table\",\"chartName\":\"表格\",\"title\":\"最近告警记录\"}"),
            Map.of("mode", "inline", "left", 1292, "top", 690, "width", 608, "height", 370, "inlineConfig", "{\"chartType\":\"bar\",\"chartName\":\"横向柱状图\",\"title\":\"TOP10慢查询\"}")
        ));
        templates.add(t6);
        
        // 模板7: 图表+表格混合 — 2/3+1/3上 + 全宽表格下
        Map<String, Object> t7 = new java.util.LinkedHashMap<>();
        t7.put("id", "chart-table-mix");
        t7.put("name", "图表+表格混合");
        t7.put("description", "上方图表区 + 下方表格区，适合分析报告页面");
        t7.put("thumbnail", "mix");
        t7.put("layoutConfig", "{\"width\":1200,\"height\":800}");
        t7.put("charts", List.of(
            Map.of("mode", "inline", "left", 16, "top", 16, "width", 774, "height", 360, "inlineConfig", "{\"chartType\":\"line\",\"chartName\":\"折线图\",\"title\":\"趋势分析\"}"),
            Map.of("mode", "inline", "left", 806, "top", 16, "width", 378, "height", 360, "inlineConfig", "{\"chartType\":\"pie\",\"chartName\":\"饼图\",\"title\":\"分类占比\"}"),
            Map.of("mode", "inline", "left", 16, "top", 392, "width", 1168, "height", 380, "inlineConfig", "{\"chartType\":\"table\",\"chartName\":\"数据表格\",\"title\":\"详细数据\"}")
        ));
        templates.add(t7);
        
        // 模板8: KPI指标墙 — 4x2网格
        Map<String, Object> t8 = new java.util.LinkedHashMap<>();
        t8.put("id", "kpi-wall");
        t8.put("name", "KPI指标墙");
        t8.put("description", "多个KPI指标卡片排列，适合关键指标展示");
        t8.put("thumbnail", "kpi");
        t8.put("layoutConfig", "{\"width\":1200,\"height\":480}");
        t8.put("charts", List.of(
            Map.of("mode", "inline", "left", 16, "top", 16, "width", 280, "height", 210, "inlineConfig", "{\"chartType\":\"kpi\",\"chartName\":\"指标1\",\"title\":\"总收入\"}"),
            Map.of("mode", "inline", "left", 312, "top", 16, "width", 280, "height", 210, "inlineConfig", "{\"chartType\":\"kpi\",\"chartName\":\"指标2\",\"title\":\"新增用户\"}"),
            Map.of("mode", "inline", "left", 608, "top", 16, "width", 280, "height", 210, "inlineConfig", "{\"chartType\":\"kpi\",\"chartName\":\"指标3\",\"title\":\"活跃率\"}"),
            Map.of("mode", "inline", "left", 904, "top", 16, "width", 280, "height", 210, "inlineConfig", "{\"chartType\":\"kpi\",\"chartName\":\"指标4\",\"title\":\"留存率\"}"),
            Map.of("mode", "inline", "left", 16, "top", 242, "width", 280, "height", 210, "inlineConfig", "{\"chartType\":\"kpi\",\"chartName\":\"指标5\",\"title\":\"客单价\"}"),
            Map.of("mode", "inline", "left", 312, "top", 242, "width", 280, "height", 210, "inlineConfig", "{\"chartType\":\"kpi\",\"chartName\":\"指标6\",\"title\":\"退款率\"}"),
            Map.of("mode", "inline", "left", 608, "top", 242, "width", 280, "height", 210, "inlineConfig", "{\"chartType\":\"kpi\",\"chartName\":\"指标7\",\"title\":\"响应时间\"}"),
            Map.of("mode", "inline", "left", 904, "top", 242, "width", 280, "height", 210, "inlineConfig", "{\"chartType\":\"kpi\",\"chartName\":\"指标8\",\"title\":\"满意度\"}")
        ));
        templates.add(t8);
        
        // 模板9: 双图表对比 — 2列等宽
        Map<String, Object> t9 = new java.util.LinkedHashMap<>();
        t9.put("id", "dual-compare");
        t9.put("name", "双图表对比");
        t9.put("description", "左右两个大图表并排，适合A/B对比分析");
        t9.put("thumbnail", "compare");
        t9.put("layoutConfig", "{\"width\":1200,\"height\":560}");
        t9.put("charts", List.of(
            Map.of("mode", "inline", "left", 16, "top", 16, "width", 576, "height", 520, "inlineConfig", "{\"chartType\":\"bar\",\"chartName\":\"图表A\",\"title\":\"方案A数据\"}"),
            Map.of("mode", "inline", "left", 608, "top", 16, "width", 576, "height", 520, "inlineConfig", "{\"chartType\":\"bar\",\"chartName\":\"图表B\",\"title\":\"方案B数据\"}")
        ));
        templates.add(t9);
        
        // 模板10: 综合运营看板 — 3 KPI + 2/3+1/3图表 + 2列图表 + 全宽表格
        Map<String, Object> t10 = new java.util.LinkedHashMap<>();
        t10.put("id", "ops-dashboard");
        t10.put("name", "综合运营看板");
        t10.put("description", "KPI + 多图表 + 表格的综合运营分析页面");
        t10.put("thumbnail", "ops");
        t10.put("layoutConfig", "{\"width\":1200,\"height\":950}");
        t10.put("charts", List.of(
            Map.of("mode", "inline", "left", 16, "top", 16, "width", 378, "height", 130, "inlineConfig", "{\"chartType\":\"kpi\",\"chartName\":\"KPI1\",\"title\":\"日活用户\"}"),
            Map.of("mode", "inline", "left", 410, "top", 16, "width", 378, "height", 130, "inlineConfig", "{\"chartType\":\"kpi\",\"chartName\":\"KPI2\",\"title\":\"交易金额\"}"),
            Map.of("mode", "inline", "left", 804, "top", 16, "width", 380, "height", 130, "inlineConfig", "{\"chartType\":\"kpi\",\"chartName\":\"KPI3\",\"title\":\"转化率\"}"),
            Map.of("mode", "inline", "left", 16, "top", 162, "width", 774, "height", 360, "inlineConfig", "{\"chartType\":\"line\",\"chartName\":\"折线图\",\"title\":\"用户增长趋势\"}"),
            Map.of("mode", "inline", "left", 806, "top", 162, "width", 378, "height", 360, "inlineConfig", "{\"chartType\":\"pie\",\"chartName\":\"饼图\",\"title\":\"渠道来源\"}"),
            Map.of("mode", "inline", "left", 16, "top", 538, "width", 576, "height", 360, "inlineConfig", "{\"chartType\":\"bar\",\"chartName\":\"柱状图\",\"title\":\"各区域销售额\"}"),
            Map.of("mode", "inline", "left", 608, "top", 538, "width", 576, "height", 360, "inlineConfig", "{\"chartType\":\"scatter\",\"chartName\":\"散点图\",\"title\":\"价格-销量关系\"}")
        ));
        templates.add(t10);
        
        return Result.success(templates);
    }
    
    /**
     * 从模板创建页面
     */
    @PostMapping("/from-template")
    public Result<PageDefinition> createFromTemplate(@RequestBody Map<String, Object> params) {
        String templateId = (String) params.get("templateId");
        String pageName = (String) params.get("pageName");
        if (templateId == null || pageName == null) {
            return Result.error("请指定模板ID和页面名称");
        }
        // 复用创建接口逻辑，将模板配置传入
        params.put("pageCode", "tpl_" + System.currentTimeMillis());
        return createPageDefinition(params);
    }
    
    /**
     * 获取页面版本历史
     */
    @GetMapping("/{id}/versions")
    public Result<List<Map<String, Object>>> getPageVersions(@PathVariable Long id) {
        List<Map<String, Object>> versions = jdbcTemplate.queryForList(
            "SELECT id, page_id, create_time, remark, LENGTH(layout_config) as config_size FROM sys_page_version WHERE page_id = ? ORDER BY create_time DESC LIMIT 50", id);
        return Result.success(versions);
    }
    
    /**
     * 恢复页面版本
     */
    @RequirePermission("page:design")
    @PostMapping("/{id}/versions/{versionId}/restore")
    public Result<Void> restorePageVersion(@PathVariable Long id, @PathVariable Long versionId) {
        List<Map<String, Object>> versions = jdbcTemplate.queryForList(
            "SELECT layout_config, charts_json, parameter_panel FROM sys_page_version WHERE id = ? AND page_id = ?", versionId, id);
        if (versions.isEmpty()) return Result.error("版本不存在");
        
        Map<String, Object> ver = versions.get(0);
        jdbcTemplate.update("UPDATE page_definition SET layout_config = ?, parameter_panel = ?, update_time = NOW() WHERE id = ?",
            ver.get("layout_config"), ver.get("parameter_panel"), id);
        return Result.success();
    }
    
    /**
     * 解析图表数据列表（提取公共方法，消除创建/更新方法中的重复代码）
     */
    @SuppressWarnings("unchecked")
    private List<PageChart> parseChartsData(Map<String, Object> params) {
        List<Map<String, Object>> chartsData = (List<Map<String, Object>>) params.get("charts");
        if (chartsData == null || chartsData.isEmpty()) {
            return null;
        }
        
        return chartsData.stream().map(data -> {
            PageChart chart = new PageChart();
            
            // 解析图表模式：inline / static / referenced
            String mode = (String) data.getOrDefault("mode", "referenced");
            chart.setMode(mode);
            
            if ("inline".equals(mode) || "static".equals(mode)) {
                // inline 和 static 模式都将配置存储在 inlineConfig 字段
                Object inlineConfigObj = data.get("inlineConfig");
                if (inlineConfigObj != null) {
                    try {
                        if (inlineConfigObj instanceof String) {
                            chart.setInlineConfig((String) inlineConfigObj);
                        } else {
                            chart.setInlineConfig(OBJECT_MAPPER.writeValueAsString(inlineConfigObj));
                        }
                    } catch (Exception e) {
                        if (inlineConfigObj instanceof String) {
                            chart.setInlineConfig((String) inlineConfigObj);
                        }
                    }
                }
            } else {
                if (data.get("chartId") == null) {
                    throw new IllegalArgumentException("引用模式下图表ID不能为空");
                }
                chart.setChartId(((Number) data.get("chartId")).longValue());
            }
            
            // 像素布局
            chart.setLeft(data.get("left") != null ? ((Number) data.get("left")).intValue() : 0);
            chart.setTop(data.get("top") != null ? ((Number) data.get("top")).intValue() : 0);
            chart.setWidth(data.get("width") != null ? ((Number) data.get("width")).intValue() : 300);
            chart.setHeight(data.get("height") != null ? ((Number) data.get("height")).intValue() : 200);
            
            // 兼容旧格式（网格布局）
            if (data.get("x") != null) chart.setX(((Number) data.get("x")).intValue());
            if (data.get("y") != null) chart.setY(((Number) data.get("y")).intValue());
            if (data.get("w") != null) chart.setW(((Number) data.get("w")).intValue());
            if (data.get("h") != null) chart.setH(((Number) data.get("h")).intValue());
            
            chart.setSortOrder(data.get("sortOrder") != null ? ((Number) data.get("sortOrder")).intValue() : 0);
            return chart;
        }).collect(Collectors.toList());
    }
    
    /**
     * 将Object转为JSON字符串并设置到实体字段（辅助方法）
     */
    private void setJsonField(java.util.function.Consumer<String> setter, Object value) {
        if (value == null) {
            setter.accept(null);
            return;
        }
        if (value instanceof String) {
            setter.accept((String) value);
            return;
        }
        try {
            setter.accept(OBJECT_MAPPER.writeValueAsString(value));
        } catch (Exception e) {
            setter.accept(null);
        }
    }
}

