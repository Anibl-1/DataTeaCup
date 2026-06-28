package com.dataplatform.analytics.controller;

import com.dataplatform.common.Result;
import jakarta.annotation.PostConstruct;
import com.dataplatform.infra.ai.AiConfig;
import com.dataplatform.data.dto.*;
import com.dataplatform.data.service.AiService;
import com.dataplatform.data.service.AiChartService;
import com.dataplatform.data.service.FileParseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.concurrent.Executor;

/**
 * AI接口控制器
 * 提供智能SQL生成、数据分析、SQL优化、智能对话等功能
 */
@Slf4j
@RestController
@RequestMapping("/ai")
public class AiController {

    @Autowired
    private AiService aiService;

    @Autowired
    @Qualifier("taskExecutor")
    private Executor taskExecutor;
    
    @Autowired
    private AiChartService aiChartService;
    
    @Autowired
    private AiConfig aiConfig;
    
    @Autowired
    private com.dataplatform.data.service.AiConfigManager aiConfigManager;
    
    @Autowired
    private com.dataplatform.system.service.MenuService menuService;
    
    @Autowired
    private FileParseService fileParseService;
    
    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    
    /**
     * 统一处理AI服务返回结果
     */
    private Result<Map<String, Object>> handleAiResult(Map<String, Object> result) {
        if (Boolean.TRUE.equals(result.get("success"))) {
            return Result.success(result);
        } else {
            return Result.error((String) result.get("error"));
        }
    }

    /**
     * 获取AI服务状态
     */
    @GetMapping("/status")
    public Result<Map<String, Object>> getStatus() {
        return Result.success(aiService.getStatus());
    }

    /**
     * 智能SQL生成
     * @param request 请求体 {query: "自然语言描述", tableSchema: "表结构（可选）", dbType: "数据库类型", dataSourceId: 数据源ID}
     */
    @PostMapping("/generate-sql")
    public Result<Map<String, Object>> generateSql(@RequestBody AiSqlRequest request) {
        if (request.getQuery() == null || request.getQuery().trim().isEmpty()) {
            return Result.error("请输入查询描述");
        }
        
        String dbType = aiService.resolveDbType(request.getDataSourceId(), request.getDbType());
        log.info("AI生成SQL请求: dbType={}, query={}", dbType, request.getQuery());
        return handleAiResult(aiService.generateSql(request.getQuery(), request.getTableSchema(), dbType));
    }

    /**
     * 数据分析
     * @param request 请求体 {question: "问题", dataContext: "数据上下文"}
     */
    @PostMapping("/analyze")
    public Result<Map<String, Object>> analyzeData(@RequestBody Map<String, String> request) {
        String question = request.get("question");
        String dataContext = request.get("dataContext");
        
        if (question == null || question.trim().isEmpty()) {
            return Result.error("请输入分析问题");
        }
        
        log.info("AI数据分析请求: {}", question);
        return handleAiResult(aiService.analyzeData(question, dataContext));
    }

    /**
     * SQL优化
     * @param request 请求体 {sql: "SQL语句", dbType: "数据库类型"}
     */
    @PostMapping("/optimize-sql")
    public Result<Map<String, Object>> optimizeSql(@RequestBody AiSqlRequest request) {
        if (request.getSql() == null || request.getSql().trim().isEmpty()) {
            return Result.error("请输入SQL语句");
        }
        
        log.info("AI优化SQL请求: {}", request.getSql().substring(0, Math.min(100, request.getSql().length())));
        String dbType = aiService.resolveDbType(request.getDataSourceId(), request.getDbType());
        return handleAiResult(aiService.optimizeSql(request.getSql(), dbType));
    }
    
    /**
     * SQL解释
     * @param request 请求体 {sql: "SQL语句"}
     */
    @PostMapping("/explain-sql")
    public Result<Map<String, Object>> explainSql(@RequestBody AiSqlRequest request) {
        if (request.getSql() == null || request.getSql().trim().isEmpty()) {
            return Result.error("请输入SQL语句");
        }
        
        String dbType = aiService.resolveDbType(request.getDataSourceId(), request.getDbType());
        log.info("AI解释Sql请求: dbType={}", dbType);
        return handleAiResult(aiService.explainSql(request.getSql(), dbType));
    }

    /**
     * AI对话（支持会话历史）
     * @param request 请求体 {message: "用户消息", context: "上下文（可选）", sessionId: "会话ID（可选）"}
     */
    @PostMapping("/chat")
    public Result<Map<String, Object>> chat(@RequestBody AiChatRequest request) {
        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            return Result.error("请输入消息");
        }
        
        log.info("AI对话请求: sessionId={}, message={}", request.getSessionId(), 
            request.getMessage().substring(0, Math.min(80, request.getMessage().length())));
        return handleAiResult(aiService.chatWithContext(request.getMessage(), request.getContext(), request.getSessionId()));
    }
    
    /**
     * 创建新会话
     */
    @PostMapping("/session/create")
    public Result<Map<String, String>> createSession() {
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        return Result.success(Map.of("sessionId", sessionId));
    }
    
    /**
     * 清除会话历史
     */
    @PostMapping("/session/clear")
    public Result<Void> clearSession(@RequestBody Map<String, String> request) {
        String sessionId = request.get("sessionId");
        if (sessionId != null && !sessionId.isEmpty()) {
            aiService.clearConversation(sessionId);
        }
        return Result.success(null);
    }
    
    /**
     * 快捷指令
     * @param request 请求体 {command: "指令名称", params: {参数对象}}
     */
    @PostMapping("/quick-command")
    public Result<Map<String, Object>> quickCommand(@RequestBody Map<String, Object> request) {
        String command = (String) request.get("command");
        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) request.get("params");
        
        if (command == null || command.trim().isEmpty()) {
            return Result.error("请指定指令名称");
        }
        
        log.info("AI快捷指令: {}", command);
        return handleAiResult(aiService.quickCommand(command, params));
    }
    
    /**
     * 获取AI配置
     */
    @GetMapping("/config")
    public Result<Map<String, Object>> getConfig() {
        return Result.success(aiService.getConfig());
    }
    
    /**
     * 获取AI系统上下文（包含数据源和表结构信息）
     */
    @GetMapping("/system-context")
    public Result<Map<String, Object>> getSystemContext(
            @RequestParam(required = false) Long dataSourceId) {
        log.info("获取AI系统上下文, dataSourceId={}", dataSourceId);
        Map<String, Object> context = aiService.getSystemContext(dataSourceId);
        return Result.success(context);
    }
    
    /**
     * AI执行SQL查询（仅支持SELECT语句，安全限制）
     */
    @PostMapping("/execute-sql")
    public Result<Map<String, Object>> executeSql(@RequestBody Map<String, Object> request) {
        if (request.get("dataSourceId") == null) {
            return Result.error("数据源ID不能为空");
        }
        if (request.get("sql") == null || request.get("sql").toString().trim().isEmpty()) {
            return Result.error("SQL语句不能为空");
        }
        
        Long dataSourceId = Long.valueOf(request.get("dataSourceId").toString());
        String sql = request.get("sql").toString().trim();
        Integer limit = request.get("limit") != null ? Math.min(Integer.valueOf(request.get("limit").toString()), 1000) : 100;
        
        log.info("AI执行SQL: dataSourceId={}, sql={}", dataSourceId, sql.substring(0, Math.min(200, sql.length())));
        Map<String, Object> result = aiService.executeSql(dataSourceId, sql, limit);
        return Result.success(result);
    }
    
    /**
     * 获取系统概览信息（ETL任务、报表等）
     */
    @GetMapping("/system-overview")
    public Result<Map<String, Object>> getSystemOverview() {
        log.info("获取系统概览信息");
        Map<String, Object> overview = aiService.getSystemOverview();
        return Result.success(overview);
    }
    
    /**
     * AI生成并创建报表
     */
    @PostMapping("/create-report")
    public Result<Map<String, Object>> createReport(@RequestBody Map<String, Object> request,
                                                     jakarta.servlet.http.HttpServletRequest httpRequest) {
        if (request.get("reportName") == null || request.get("reportName").toString().trim().isEmpty()) {
            return Result.error("报表名称不能为空");
        }
        if (request.get("dataSourceId") == null) {
            return Result.error("数据源ID不能为空");
        }
        if (request.get("sql") == null || request.get("sql").toString().trim().isEmpty()) {
            return Result.error("SQL语句不能为空");
        }
        
        String reportName = request.get("reportName").toString().trim();
        String description = request.get("description") != null ? request.get("description").toString() : "";
        Long dataSourceId = Long.valueOf(request.get("dataSourceId").toString());
        String sql = request.get("sql").toString().trim();
        Long userId = (Long) httpRequest.getAttribute("userId");
        
        log.info("AI创建报表: name={}, dataSourceId={}", reportName, dataSourceId);
        Map<String, Object> result = aiService.createReport(reportName, description, dataSourceId, sql, userId);
        return Result.success(result);
    }
    
    /**
     * AI生成并创建菜单
     */
    @PostMapping("/create-menu")
    public Result<Map<String, Object>> createMenu(@RequestBody Map<String, Object> request) {
        String menuName = (String) request.get("menuName");
        if (menuName == null || menuName.trim().isEmpty()) {
            return Result.error("菜单名称不能为空");
        }
        Long parentId = request.get("parentId") != null ? Long.valueOf(request.get("parentId").toString()) : 0L;
        String icon = (String) request.get("icon");
        Long reportId = request.get("reportId") != null ? Long.valueOf(request.get("reportId").toString()) : null;
        
        log.info("AI创建菜单: name={}, parentId={}, reportId={}", menuName, parentId, reportId);
        Map<String, Object> result = aiService.createMenu(menuName, parentId, icon, reportId);
        return Result.success(result);
    }
    
    /**
     * AI一键创建报表和菜单（增强版，支持查询参数）
     */
    @PostMapping("/create-report-with-menu")
    public Result<Map<String, Object>> createReportWithMenu(@RequestBody Map<String, Object> request,
                                                             jakarta.servlet.http.HttpServletRequest httpRequest) {
        String reportName = (String) request.get("reportName");
        if (reportName == null || reportName.trim().isEmpty()) {
            return Result.error("报表名称不能为空");
        }
        if (request.get("dataSourceId") == null) {
            return Result.error("数据源ID不能为空");
        }
        String description = (String) request.get("description");
        Long dataSourceId = Long.valueOf(request.get("dataSourceId").toString());
        // 支持 sql 和 sqlContent 两个字段名
        String sql = request.get("sqlContent") != null ? (String) request.get("sqlContent") : (String) request.get("sql");
        if (sql == null || sql.trim().isEmpty()) {
            return Result.error("SQL语句不能为空");
        }
        Long parentMenuId = request.get("parentMenuId") != null ? Long.valueOf(request.get("parentMenuId").toString()) : 0L;
        String icon = (String) request.get("icon");
        
        // 提取查询参数配置
        @SuppressWarnings("unchecked")
        java.util.List<java.util.Map<String, Object>> queryParams = 
            (java.util.List<java.util.Map<String, Object>>) request.get("queryParams");
        
        // 提取导出权限配置
        Boolean allowExportExcel = request.get("allowExportExcel") != null ? 
            Boolean.valueOf(request.get("allowExportExcel").toString()) : true;
        Boolean allowExportPdf = request.get("allowExportPdf") != null ? 
            Boolean.valueOf(request.get("allowExportPdf").toString()) : true;
        String pdfWatermark = request.get("pdfWatermark") != null ? 
            request.get("pdfWatermark").toString() : null;
        
        Long userId = (Long) httpRequest.getAttribute("userId");
        
        log.info("AI一键创建报表和菜单: name={}, queryParams={}", reportName, queryParams != null ? queryParams.size() : 0);
        Map<String, Object> result = aiService.createReportWithMenu(reportName, description, dataSourceId, sql, parentMenuId, icon, queryParams, allowExportExcel, allowExportPdf, pdfWatermark, userId);
        return Result.success(result);
    }
    
    /**
     * AI创建ETL数据同步任务
     */
    @PostMapping("/create-etl-job")
    public Result<Map<String, Object>> createEtlJob(@RequestBody Map<String, Object> request,
                                                     jakarta.servlet.http.HttpServletRequest httpRequest) {
        if (request.get("jobName") == null || request.get("jobName").toString().trim().isEmpty()) {
            return Result.error("任务名称不能为空");
        }
        if (request.get("sourceDataSourceId") == null || request.get("targetDataSourceId") == null) {
            return Result.error("源数据源和目标数据源不能为空");
        }
        if (request.get("sourceTable") == null || request.get("targetTable") == null) {
            return Result.error("源表和目标表不能为空");
        }
        
        String jobName = request.get("jobName").toString().trim();
        String jobDesc = request.get("jobDesc") != null ? request.get("jobDesc").toString() : "";
        Long sourceDataSourceId = Long.valueOf(request.get("sourceDataSourceId").toString());
        String sourceTable = request.get("sourceTable").toString().trim();
        Long targetDataSourceId = Long.valueOf(request.get("targetDataSourceId").toString());
        String targetTable = request.get("targetTable").toString().trim();
        String writeMode = request.get("writeMode") != null ? request.get("writeMode").toString() : "insert";
        
        Long userId = (Long) httpRequest.getAttribute("userId");
        
        log.info("AI创建ETL任务: name={}", jobName);
        Map<String, Object> result = aiService.createEtlJob(jobName, jobDesc, sourceDataSourceId, sourceTable, targetDataSourceId, targetTable, writeMode, userId);
        return Result.success(result);
    }
    
    /**
     * 获取系统诊断信息
     */
    @GetMapping("/system-diagnosis")
    public Result<Map<String, Object>> getSystemDiagnosis() {
        log.info("获取系统诊断信息");
        Map<String, Object> diagnosis = aiService.getSystemDiagnosis();
        return Result.success(diagnosis);
    }
    
    /**
     * 获取AI可执行的操作列表
     */
    @GetMapping("/available-actions")
    public Result<Map<String, Object>> getAvailableActions() {
        Map<String, Object> actions = new HashMap<>();
        actions.put("actions", List.of(
            Map.of("name", "createReport", "label", "创建报表", "description", "根据SQL创建数据报表"),
            Map.of("name", "createMenu", "label", "创建菜单", "description", "创建系统菜单项"),
            Map.of("name", "createEtlJob", "label", "创建ETL任务", "description", "创建数据同步任务"),
            Map.of("name", "executeSql", "label", "执行SQL", "description", "执行SQL查询并返回结果"),
            Map.of("name", "analyzeTable", "label", "分析表结构", "description", "分析数据库表结构"),
            Map.of("name", "optimizeSql", "label", "优化SQL", "description", "分析并优化SQL性能")
        ));
        return Result.success(actions);
    }
    
    /**
     * 获取现有报表列表
     */
    @GetMapping("/reports")
    public Result<Map<String, Object>> getReports() {
        log.info("AI获取报表列表");
        Map<String, Object> result = aiService.getReportsList();
        return Result.success(result);
    }
    
    /**
     * 生成数据字典
     */
    @GetMapping("/data-dictionary/{dataSourceId}")
    public Result<Map<String, Object>> generateDataDictionary(@PathVariable Long dataSourceId) {
        log.info("AI生成数据字典: dataSourceId={}", dataSourceId);
        Map<String, Object> result = aiService.generateDataDictionary(dataSourceId);
        return Result.success(result);
    }
    
    /**
     * AI推荐可视化方案
     */
    @PostMapping("/recommend-chart")
    public Result<Map<String, Object>> recommendChart(@RequestBody Map<String, Object> request) {
        String sql = (String) request.get("sql");
        if (sql == null || sql.trim().isEmpty()) {
            return Result.error("SQL语句不能为空");
        }
        if (request.get("dataSourceId") == null) {
            return Result.error("数据源ID不能为空");
        }
        Long dataSourceId = Long.valueOf(request.get("dataSourceId").toString());
        
        log.info("AI推荐可视化方案");
        Map<String, Object> result = aiService.recommendChart(dataSourceId, sql);
        return Result.success(result);
    }
    
    /**
     * 获取ETL任务列表
     */
    @GetMapping("/etl-jobs")
    public Result<Map<String, Object>> getEtlJobs() {
        log.info("AI获取ETL任务列表");
        Map<String, Object> result = aiService.getEtlJobsList();
        return Result.success(result);
    }
    
    /**
     * 数据质量分析
     */
    @GetMapping("/data-quality/{dataSourceId}/{tableName}")
    public Result<Map<String, Object>> analyzeDataQuality(
            @PathVariable Long dataSourceId, 
            @PathVariable String tableName) {
        log.info("AI数据质量分析: dataSourceId={}, table={}", dataSourceId, tableName);
        Map<String, Object> result = aiService.analyzeDataQuality(dataSourceId, tableName);
        return Result.success(result);
    }
    
    /**
     * AI智能建议
     */
    @GetMapping("/suggestions/{dataSourceId}/{tableName}")
    public Result<Map<String, Object>> getSmartSuggestions(
            @PathVariable Long dataSourceId,
            @PathVariable String tableName) {
        log.info("AI智能建议: dataSourceId={}, table={}", dataSourceId, tableName);
        Map<String, Object> result = aiService.getSmartSuggestions(dataSourceId, tableName);
        return Result.success(result);
    }
    
    /**
     * 保存对话消息
     */
    @PostMapping("/chat/save")
    public Result<String> saveChatMessage(@RequestBody Map<String, Object> request) {
        String sessionId = (String) request.get("sessionId");
        Long userId = request.get("userId") != null ? Long.valueOf(request.get("userId").toString()) : null;
        String role = (String) request.get("role");
        // 限制role只能是合法值
        if (role == null || (!"user".equals(role) && !"assistant".equals(role) && !"system".equals(role))) {
            return Result.error("无效的消息角色");
        }
        String content = (String) request.get("content");
        Long dataSourceId = request.get("dataSourceId") != null ? Long.valueOf(request.get("dataSourceId").toString()) : null;
        String messageType = (String) request.getOrDefault("messageType", "text");
        
        aiService.saveChatMessage(sessionId, userId, role, content, dataSourceId, messageType);
        return Result.success("保存成功");
    }
    
    /**
     * 获取对话历史
     */
    @GetMapping("/chat/history/{sessionId}")
    public Result<Map<String, Object>> getChatHistory(
            @PathVariable String sessionId,
            @RequestParam(defaultValue = "50") Integer limit) {
        log.info("获取对话历史: sessionId={}", sessionId);
        Map<String, Object> result = aiService.getChatHistory(sessionId, limit);
        return Result.success(result);
    }
    
    /**
     * 获取用户的会话列表
     */
    @GetMapping("/chat/sessions")
    public Result<List<Map<String, Object>>> getUserSessions(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "20") Integer limit,
            jakarta.servlet.http.HttpServletRequest httpRequest) {
        // 优先使用认证用户ID，防止越权查看他人会话
        Long authenticatedUserId = (Long) httpRequest.getAttribute("userId");
        Long actualUserId = authenticatedUserId != null ? authenticatedUserId : userId;
        if (actualUserId == null) {
            return Result.error("用户ID不能为空");
        }
        log.info("获取用户会话列表: userId={}", actualUserId);
        List<Map<String, Object>> sessions = aiService.getUserSessions(actualUserId, Math.min(limit, 50));
        return Result.success(sessions);
    }
    
    /**
     * AI分析数据血缘
     */
    @PostMapping("/analyze-lineage")
    public Result<Map<String, Object>> analyzeLineage(@RequestBody Map<String, Object> request) {
        String tableName = (String) request.get("tableName");
        Long dataSourceId = request.get("dataSourceId") != null ? 
            Long.valueOf(request.get("dataSourceId").toString()) : null;
        
        if (tableName == null || tableName.trim().isEmpty()) {
            return Result.error("表名不能为空");
        }
        // 表名安全校验：仅允许字母、数字、下划线、点号
        if (!tableName.matches("^[a-zA-Z0-9_.]+$")) {
            return Result.error("表名包含不安全字符");
        }
        
        log.info("AI分析血缘: tableName={}, dataSourceId={}", tableName, dataSourceId);
        return handleAiResult(aiService.analyzeDataLineage(dataSourceId, tableName));
    }
    
    /**
     * AI生成ETL流程配置
     */
    @PostMapping("/generate-etl")
    public Result<Map<String, Object>> generateEtl(@RequestBody Map<String, Object> request) {
        String requirement = (String) request.get("requirement");
        Long dataSourceId = request.get("dataSourceId") != null ?
            Long.valueOf(request.get("dataSourceId").toString()) : null;
        @SuppressWarnings("unchecked")
        Map<String, Object> context = (Map<String, Object>) request.get("context");
        if (context == null) {
            context = new HashMap<>();
        }
        if (!context.containsKey("dbType")) {
            String dbType = request.get("dbType") != null ? request.get("dbType").toString() : null;
            dbType = aiService.resolveDbType(dataSourceId, dbType);
            if (dbType != null && !dbType.isBlank()) {
                context.put("dbType", dbType);
            }
        }
        
        if (requirement == null || requirement.trim().isEmpty()) {
            return Result.error("请描述ETL需求");
        }
        
        log.info("AI生成ETL流程: requirement={}", requirement.substring(0, Math.min(100, requirement.length())));
        return handleAiResult(aiService.generateEtlFlow(requirement, context));
    }
    
    /**
     * AI生成图表配置
     * 根据用户需求自动生成SQL、图表类型和ECharts配置
     */
    @PostMapping("/generate-chart")
    public Result<Map<String, Object>> generateChart(@RequestBody Map<String, Object> request) {
        String requirement = (String) request.get("requirement");
        Long dataSourceId = request.get("dataSourceId") != null ? 
            Long.valueOf(request.get("dataSourceId").toString()) : null;
        @SuppressWarnings("unchecked")
        Map<String, Object> context = (Map<String, Object>) request.get("context");
        if (context == null) {
            context = new HashMap<>();
        }
        if (!context.containsKey("dbType")) {
            String dbType = request.get("dbType") != null ? request.get("dbType").toString() : null;
            dbType = aiService.resolveDbType(dataSourceId, dbType);
            if (dbType != null && !dbType.isBlank()) {
                context.put("dbType", dbType);
            }
        }
        
        if (requirement == null || requirement.trim().isEmpty()) {
            return Result.error("请描述图表需求");
        }
        
        log.info("AI生成图表配置: requirement={}, dataSourceId={}", 
            requirement.substring(0, Math.min(100, requirement.length())), dataSourceId);
        return handleAiResult(aiChartService.generateChart(requirement, dataSourceId, context));
    }
    
    /**
     * AI创建图表
     * 根据配置直接创建图表定义
     */
    @PostMapping("/create-chart")
    public Result<Map<String, Object>> createChart(@RequestBody Map<String, Object> request) {
        String chartName = (String) request.get("chartName");
        String chartCode = (String) request.get("chartCode");
        String chartType = (String) request.get("chartType");
        String description = (String) request.get("description");
        Long dataSourceId = request.get("dataSourceId") != null ? 
            Long.valueOf(request.get("dataSourceId").toString()) : null;
        String sql = (String) request.get("sql");
        String chartConfigJson = (String) request.get("chartConfig");
        
        // 如果chartConfig是对象，转换为JSON字符串
        if (chartConfigJson == null && request.get("chartConfig") != null) {
            try {
                chartConfigJson = objectMapper.writeValueAsString(request.get("chartConfig"));
            } catch (Exception e) {
                log.warn("转换chartConfig失败: {}", e.getMessage());
            }
        }
        
        // 🆕 提取查询参数
        @SuppressWarnings("unchecked")
        java.util.List<java.util.Map<String, Object>> queryParams = 
            (java.util.List<java.util.Map<String, Object>>) request.get("queryParams");
        
        // 提取导出配置和水印
        Boolean allowExportExcel = request.get("allowExportExcel") != null ? 
            Boolean.valueOf(request.get("allowExportExcel").toString()) : true;
        Boolean allowExportPdf = request.get("allowExportPdf") != null ? 
            Boolean.valueOf(request.get("allowExportPdf").toString()) : true;
        String watermarkType = request.get("watermarkType") != null ?
            request.get("watermarkType").toString() : "none";
        String pdfWatermark = request.get("pdfWatermark") != null ? 
            request.get("pdfWatermark").toString() : null;
        
        if (chartName == null || chartName.trim().isEmpty()) {
            return Result.error("图表名称不能为空");
        }
        if (dataSourceId == null) {
            return Result.error("数据源ID不能为空");
        }
        if (sql == null || sql.trim().isEmpty()) {
            return Result.error("SQL语句不能为空");
        }
        
        log.info("AI创建图表: name={}, type={}, dataSourceId={}, queryParams={}", 
            chartName, chartType, dataSourceId, queryParams != null ? queryParams.size() : 0);
        Map<String, Object> result = aiChartService.createChart(chartName, chartCode, chartType, 
            description, dataSourceId, sql, chartConfigJson, queryParams, allowExportExcel, allowExportPdf, watermarkType, pdfWatermark);
        
        return Result.success(result);
    }
    
    /**
     * AI创建图表并同时创建菜单
     * 从AI助手对话中快速创建图表和对应菜单
     */
    @PostMapping("/create-chart-with-menu")
    public Result<Map<String, Object>> createChartWithMenu(@RequestBody Map<String, Object> request) {
        String chartName = (String) request.get("chartName");
        String chartType = (String) request.get("chartType");
        String description = (String) request.get("description");
        Long dataSourceId = request.get("dataSourceId") != null ? 
            Long.valueOf(request.get("dataSourceId").toString()) : null;
        String sqlContent = (String) request.get("sqlContent");
        Long parentMenuId = request.get("parentMenuId") != null ? 
            Long.valueOf(request.get("parentMenuId").toString()) : 0L;
        String icon = (String) request.get("icon");
        
        if (chartName == null || chartName.trim().isEmpty()) {
            return Result.error("图表名称不能为空");
        }
        if (dataSourceId == null) {
            return Result.error("数据源ID不能为空");
        }
        if (sqlContent == null || sqlContent.trim().isEmpty()) {
            return Result.error("SQL语句不能为空");
        }
        
        // 提取样式配置
        @SuppressWarnings("unchecked")
        Map<String, Object> styleConfig = (Map<String, Object>) request.get("styleConfig");
        
        // 提取导出配置
        Boolean allowExportExcel = request.get("allowExportExcel") != null ? 
            Boolean.valueOf(request.get("allowExportExcel").toString()) : true;
        Boolean allowExportPdf = request.get("allowExportPdf") != null ? 
            Boolean.valueOf(request.get("allowExportPdf").toString()) : true;
        String watermarkType = request.get("watermarkType") != null ?
            request.get("watermarkType").toString() : "none";
        String pdfWatermark = request.get("pdfWatermark") != null ? 
            request.get("pdfWatermark").toString() : null;
        
        // 提取字段映射和参数配置
        @SuppressWarnings("unchecked")
        Map<String, Object> fieldMapping = (Map<String, Object>) request.get("fieldMapping");
        @SuppressWarnings("unchecked")
        java.util.List<java.util.Map<String, Object>> queryParams = 
            (java.util.List<java.util.Map<String, Object>>) request.get("queryParams");
        
        log.info("AI创建图表和菜单: name={}, type={}, dataSourceId={}, parentMenuId={}", 
            chartName, chartType, dataSourceId, parentMenuId);
        
        Map<String, Object> result = new java.util.HashMap<>();
        try {
            // 1. 创建图表
            String chartCode = "ai_chart_" + System.currentTimeMillis();
            Map<String, Object> chartResult = aiChartService.createChartWithConfig(
                chartName, chartCode, chartType != null ? chartType : "bar", 
                description, dataSourceId, sqlContent, fieldMapping, styleConfig,
                queryParams, allowExportExcel, allowExportPdf, watermarkType, pdfWatermark);
            
            if (chartResult.get("chartId") == null) {
                return Result.error("创建图表失败: " + chartResult.get("error"));
            }
            
            Long chartId = Long.valueOf(chartResult.get("chartId").toString());
            result.put("chartId", chartId);
            result.put("chartName", chartName);
            
            // 2. 创建菜单
            String menuName2 = chartName;
            String menuCode2 = "ai_chart_" + chartId;
            String menuType2 = "menu";
            String routePath2 = "/chart-view/" + chartId;
            String componentPath2 = "DynamicChart";
            String icon2 = icon != null ? icon : "BarChartOutline";
            
            Long menuId = menuService.createMenu(
                menuName2, menuCode2, parentMenuId, menuType2,
                icon2, 1, 99, routePath2, componentPath2, null,
                chartId, null, null, "tab", null
            );
            result.put("menuId", menuId);
            result.put("success", true);
            result.put("message", "图表和菜单创建成功");
            
            log.info("图表和菜单创建成功: chartId={}, menuId={}", chartId, menuId);
        } catch (Exception e) {
            log.error("创建图表和菜单失败", e);
            return Result.error("创建失败: " + e.getMessage());
        }
        
        return Result.success(result);
    }
    
    /**
     * AI一键生成并创建图表
     * 结合生成和创建，一步完成
     */
    @PostMapping("/generate-and-create-chart")
    public Result<Map<String, Object>> generateAndCreateChart(@RequestBody Map<String, Object> request) {
        String requirement = (String) request.get("requirement");
        Long dataSourceId = request.get("dataSourceId") != null ? 
            Long.valueOf(request.get("dataSourceId").toString()) : null;
        @SuppressWarnings("unchecked")
        Map<String, Object> options = (Map<String, Object>) request.get("options");
        if (options == null) {
            options = new HashMap<>();
        }
        if (!options.containsKey("dbType")) {
            String dbType = request.get("dbType") != null ? request.get("dbType").toString() : null;
            dbType = aiService.resolveDbType(dataSourceId, dbType);
            if (dbType != null && !dbType.isBlank()) {
                options.put("dbType", dbType);
            }
        }
        
        if (requirement == null || requirement.trim().isEmpty()) {
            return Result.error("请描述图表需求");
        }
        if (dataSourceId == null) {
            return Result.error("请选择数据源");
        }
        
        log.info("AI一键生成图表: requirement={}, dataSourceId={}", 
            requirement.substring(0, Math.min(100, requirement.length())), dataSourceId);
        Map<String, Object> result = aiChartService.generateAndCreateChart(requirement, dataSourceId, options);
        
        return Result.success(result);
    }
    
    /**
     * AI优化图表样式
     */
    @PostMapping("/optimize-chart-style")
    public Result<Map<String, Object>> optimizeChartStyle(@RequestBody Map<String, Object> request) {
        Long chartId = request.get("chartId") != null ? 
            Long.valueOf(request.get("chartId").toString()) : null;
        String stylePreference = (String) request.get("stylePreference");
        
        if (chartId == null) {
            return Result.error("图表ID不能为空");
        }
        
        log.info("AI优化图表样式: chartId={}, preference={}", chartId, stylePreference);
        return handleAiResult(aiChartService.optimizeChartStyle(chartId, stylePreference));
    }
    
    /**
     * 测试AI连接
     */
    @PostMapping("/test-connection")
    public Result<Map<String, Object>> testConnection(@RequestBody Map<String, String> request) {
        String provider = request.get("provider");
        String apiKey = request.get("apiKey");
        String baseUrl = request.get("baseUrl");
        String model = request.get("model");
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("测试AI连接: provider={}, model={}", provider, model);
            
            // 简单测试：发送一个简短请求
            String testResponse = aiService.testProviderConnection(provider, apiKey, baseUrl, model);
            
            if (testResponse != null && !testResponse.isEmpty()) {
                result.put("success", true);
                result.put("message", "连接成功");
                result.put("response", testResponse.length() > 100 ? testResponse.substring(0, 100) + "..." : testResponse);
            } else {
                result.put("success", false);
                result.put("error", "未收到有效响应");
            }
        } catch (Exception e) {
            log.error("测试连接失败: {}", e.getMessage());
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return Result.success(result);
    }
    
    /**
     * 保存AI配置
     */
    @PostMapping("/config")
    public Result<Void> saveConfig(@RequestBody AiConfigRequest request) {
        if (request.getProvider() == null || request.getProvider().isEmpty()) {
            return Result.error("请选择服务提供商");
        }
        
        log.info("保存AI配置: provider={}", request.getProvider());
        
        try {
            // 构建配置Map，委托AiConfigManager持久化到数据库
            Map<String, Object> configMap = new HashMap<>();
            configMap.put("provider", request.getProvider());
            configMap.put("apiKey", request.getApiKey());
            configMap.put("baseUrl", request.getBaseUrl());
            configMap.put("model", request.getModel());
            if (request.getMaxHistory() != null) {
                configMap.put("maxHistory", request.getMaxHistory());
            }
            aiConfigManager.saveConfig(configMap);
            if (Boolean.TRUE.equals(request.getSetActive())) {
                aiConfig.setProvider(request.getProvider());
                aiConfig.setEnabled(true);
            }
            return Result.success();
        } catch (Exception e) {
            log.error("保存AI配置失败", e);
            return Result.error("保存配置失败: " + e.getMessage());
        }
    }
    
    /**
     * 文件分析（支持Excel、CSV）
     * 委托FileParseService解析，遵循单一职责原则
     */
    @PostMapping("/analyze-file")
    public Result<Map<String, Object>> analyzeFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("请上传文件");
        }
        
        log.info("文件分析请求: {}", file.getOriginalFilename());
        Map<String, Object> result = new HashMap<>();
        
        try {
            FileParseService.ParseResult parseResult = fileParseService.parse(file);
            List<String> columns = parseResult.getColumns();
            List<Map<String, Object>> data = parseResult.getData();
            
            // AI分析数据
            String analysis = "";
            if (!data.isEmpty() && aiConfig.isEnabled()) {
                analysis = analyzeDataWithAi(columns, data);
            }
            
            result.put("success", true);
            result.put("columns", columns);
            result.put("data", data.size() > 100 ? data.subList(0, 100) : data);
            result.put("totalRows", data.size());
            result.put("analysis", analysis);
            
            return Result.success(result);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("文件解析失败", e);
            return Result.error("文件解析失败: " + e.getMessage());
        }
    }
    
    /**
     * AI流式对话（已废弃，重定向到同步接口）
     * 保留端点以兼容旧客户端
     */
    @PostMapping("/chat/stream")
    public Result<Map<String, Object>> chatStream(@RequestBody AiChatRequest request) {
        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            return Result.error("请输入消息");
        }
        return handleAiResult(aiService.chatWithContext(
            request.getMessage(), request.getContext(), request.getSessionId()
        ));
    }
    
    /**
     * 查询AI使用量/配额
     */
    @GetMapping("/usage")
    public Result<Map<String, Object>> getAiUsage() {
        return Result.success(aiService.getAiUsage());
    }
    
    /**
     * 获取AI使用统计
     */
    @GetMapping("/usage/stats")
    public Result<Map<String, Object>> getAiUsageStats() {
        return Result.success(aiService.getAiUsageStats());
    }
    
    // ==================== Prompt 模板管理 ====================

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private static final String PROMPT_TABLE_DDL =
        "CREATE TABLE IF NOT EXISTS sys_ai_prompt_template (" +
        "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
        "name VARCHAR(200) NOT NULL, " +
        "category VARCHAR(100) DEFAULT '通用', " +
        "content TEXT NOT NULL, " +
        "create_time DATETIME DEFAULT CURRENT_TIMESTAMP)";

    @PostConstruct
    private void initPromptSchema() {
        try {
            jdbcTemplate.execute(PROMPT_TABLE_DDL);
        } catch (Exception e) {
            log.debug("初始化sys_ai_prompt_template表: {}", e.getMessage());
        }
    }

    /**
     * 获取Prompt模板列表
     */
    @GetMapping("/prompt-templates")
    public Result<List<Map<String, Object>>> getPromptTemplates(
            @RequestParam(required = false) String category) {
        List<Map<String, Object>> templates;
        if (category != null && !category.isEmpty()) {
            templates = jdbcTemplate.queryForList(
                "SELECT id, name, category, content, create_time FROM sys_ai_prompt_template WHERE category = ? ORDER BY create_time DESC", category);
        } else {
            templates = jdbcTemplate.queryForList(
                "SELECT id, name, category, content, create_time FROM sys_ai_prompt_template ORDER BY create_time DESC");
        }
        return Result.success(templates);
    }
    
    /**
     * 创建Prompt模板
     */
    @PostMapping("/prompt-templates")
    public Result<Map<String, Object>> createPromptTemplate(@RequestBody Map<String, String> request) {
        String name = request.get("name");
        String category = request.getOrDefault("category", "通用");
        String content = request.get("content");
        if (name == null || name.isBlank()) return Result.error("名称不能为空");
        if (content == null || content.isBlank()) return Result.error("内容不能为空");
        jdbcTemplate.update("INSERT INTO sys_ai_prompt_template (name, category, content) VALUES (?, ?, ?)",
            name, category, content);
        return Result.success(Map.of("message", "创建成功"));
    }
    
    /**
     * 更新Prompt模板
     */
    @PutMapping("/prompt-templates/{id}")
    public Result<Void> updatePromptTemplate(@PathVariable Long id, @RequestBody Map<String, String> request) {
        String name = request.get("name");
        String content = request.get("content");
        if (name == null || name.isBlank()) return Result.error("名称不能为空");
        if (content == null || content.isBlank()) return Result.error("内容不能为空");
        String category = request.getOrDefault("category", "通用");
        jdbcTemplate.update("UPDATE sys_ai_prompt_template SET name=?, category=?, content=? WHERE id=?",
            name, category, content, id);
        return Result.success();
    }
    
    /**
     * 删除Prompt模板
     */
    @DeleteMapping("/prompt-templates/{id}")
    public Result<Void> deletePromptTemplate(@PathVariable Long id) {
        jdbcTemplate.update("DELETE FROM sys_ai_prompt_template WHERE id=?", id);
        return Result.success();
    }
    
    /**
     * 使用AI分析数据
     */
    private String analyzeDataWithAi(List<String> columns, List<Map<String, Object>> data) {
        try {
            StringBuilder context = new StringBuilder();
            context.append("数据包含 ").append(data.size()).append(" 行，");
            context.append("列名：").append(String.join(", ", columns)).append("\n");
            context.append("前5行数据样本：\n");
            
            for (int i = 0; i < Math.min(5, data.size()); i++) {
                context.append(data.get(i).toString()).append("\n");
            }
            
            Map<String, Object> aiResult = aiService.analyzeData(
                "请简要分析这份数据的特征、可能的用途和建议的可视化方式",
                context.toString()
            );
            
            if (Boolean.TRUE.equals(aiResult.get("success"))) {
                return (String) aiResult.get("content");
            }
        } catch (Exception e) {
            log.warn("AI分析数据失败: {}", e.getMessage());
        }
        return "";
    }
}
