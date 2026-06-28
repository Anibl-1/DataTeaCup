package com.dataplatform.data.service;

import com.dataplatform.infra.ai.AiConfig;
import com.dataplatform.common.exception.BusinessException;
import com.dataplatform.common.exception.ErrorCode;
import com.dataplatform.common.security.SecurityContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import com.dataplatform.common.PageResult;
import com.dataplatform.common.service.MenuProvider;
import com.dataplatform.data.dto.ReportDefinitionCreateDTO;
import com.dataplatform.data.entity.AiChatHistory;
import com.dataplatform.data.entity.DataSource;
import com.dataplatform.data.entity.DataxJob;
import com.dataplatform.data.entity.ReportDefinition;
import com.dataplatform.data.mapper.AiChatHistoryMapper;
import com.dataplatform.data.mapper.DataxJobMapper;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * AI服务 - 核心协调器
 * 委托 AiProviderClient 处理API调用，AiConfigManager 处理配置管理
 * 自身保留：对话逻辑、意图识别、SQL业务、缓存管理
 */
@Slf4j
@Service
public class AiService {

    @Autowired
    private AiConfig aiConfig;
    
    @Autowired
    private AiProviderClient providerClient;
    
    @Autowired
    private AiConfigManager configManager;
    
    @Autowired(required = false)
    private AiChatOptimizer chatOptimizer;
    
    @Autowired(required = false)
    private DataSourceService dataSourceService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;
    
    @Autowired(required = false)
    private TableDataService tableDataService;
    
    @Autowired(required = false)
    private DataxJobMapper dataxJobMapper;
    
    @Autowired(required = false)
    private AiChatHistoryMapper aiChatHistoryMapper;
    
    @Autowired
    private ReportDefinitionService reportDefinitionService;
    
    @Autowired
    private MenuProvider menuProvider;
    
    @Autowired
    private DataxJobService dataxJobService;

    @Autowired
    private LicenseLimitService licenseLimitService;
    
    // 会话历史管理（优先Redis，降级到内存）
    private final Map<String, List<Map<String, String>>> conversationHistory = new ConcurrentHashMap<>();
    private final Map<String, Long> conversationLastAccess = new ConcurrentHashMap<>();
    private static final int MAX_SESSIONS = 200;
    private static final long SESSION_TTL_MS = 2 * 60 * 60 * 1000; // 2小时过期
    private volatile long lastCleanupTime = 0;
    private static final long CLEANUP_INTERVAL_MS = 5 * 60 * 1000; // 每5分钟清理一次
    private static final String REDIS_CHAT_PREFIX = "ai:chat:session:";
    private static final long REDIS_SESSION_TTL_SECONDS = 2 * 60 * 60; // 2小时
    
    // ============ 意图分类枚举 ============
    public enum IntentType {
        GREETING,       // 问候语
        HELP,           // 帮助请求
        CAPABILITY,     // 能力询问
        SQL_GENERATE,   // SQL生成
        SQL_EXPLAIN,    // SQL解释
        SQL_OPTIMIZE,   // SQL优化
        DATA_ANALYSIS,  // 数据分析
        ETL_FLOW,       // ETL流程
        CHART_GENERATE, // 图表生成
        GENERAL         // 通用对话
    }
    
    // 快速响应映射（不需要调用AI的简单问题）
    private static final Map<String, String> QUICK_RESPONSES = new HashMap<>();
    
    // 意图关键词映射（用于快速意图识别）
    private static final Map<IntentType, List<String>> INTENT_KEYWORDS = new HashMap<>();
    
    // 预编译的表名校验正则（仅允许字母、数字、下划线、点号）
    private static final Pattern VALID_TABLE_NAME = Pattern.compile("^[a-zA-Z0-9_.]+$");
    
    // 预编译的危险SQL关键字正则（用于executeSql安全检查）
    private static final Map<String, Pattern> DANGEROUS_SQL_PATTERNS;
    // 预编译的参数占位符正则（用于removeUnreplacedParams）
    private static final Pattern PARAM_IN_PARENS = Pattern.compile("\\([^()]*\\$\\{[^}]+\\}[^()]*\\)");
    private static final Pattern PARAM_AND_TAIL = Pattern.compile("(?i)\\s+AND\\s+[^AND|OR]*\\$\\{[^}]+\\}[^AND|OR]*(?=\\s+AND|\\s+OR|\\s+ORDER|\\s+GROUP|\\s+LIMIT|\\s*$)");
    private static final Pattern PARAM_OR_TAIL = Pattern.compile("(?i)\\s+OR\\s+[^AND|OR]*\\$\\{[^}]+\\}[^AND|OR]*(?=\\s+AND|\\s+OR|\\s+ORDER|\\s+GROUP|\\s+LIMIT|\\s*$)");
    static {
        String[] keywords = {"DROP", "DELETE", "UPDATE", "INSERT", "TRUNCATE", "ALTER",
            "CREATE", "GRANT", "REVOKE", "EXEC", "EXECUTE", "XP_", "SP_", "INTO OUTFILE",
            "INTO DUMPFILE", "LOAD_FILE", "BENCHMARK", "SLEEP", "WAITFOR"};
        Map<String, Pattern> map = new LinkedHashMap<>();
        for (String kw : keywords) {
            map.put(kw, Pattern.compile("(?<![A-Z0-9_])" + Pattern.quote(kw) + "(?![A-Z0-9_])"));
        }
        DANGEROUS_SQL_PATTERNS = Collections.unmodifiableMap(map);
    }
    
    // 响应缓存（相似问题缓存，减少重复AI调用）
    private final Map<String, CachedResponse> responseCache = new ConcurrentHashMap<>();
    private static final int CACHE_MAX_SIZE = 500;
    private static final long CACHE_TTL_MS = 60 * 60 * 1000; // 60分钟
    
    // 缓存响应类
    private static class CachedResponse {
        String response;
        long timestamp;
        final AtomicInteger hitCount;
        volatile long lastAccessTime;
        
        CachedResponse(String response) {
            this.response = response;
            this.timestamp = System.currentTimeMillis();
            this.lastAccessTime = this.timestamp;
            this.hitCount = new AtomicInteger(0);
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }
    
    static {
        // ============ 快速响应模板 ============
        
        // 问候语（多种变体）
        String greetingResponse = "你好！我是AI助手，可以帮您：\n• SQL生成与优化\n• 数据分析\n• 图表设计\n• ETL流程\n\n请问有什么可以帮您的？";
        QUICK_RESPONSES.put("你好", greetingResponse);
        QUICK_RESPONSES.put("您好", greetingResponse);
        QUICK_RESPONSES.put("hi", greetingResponse);
        QUICK_RESPONSES.put("hello", greetingResponse);
        QUICK_RESPONSES.put("hey", greetingResponse);
        QUICK_RESPONSES.put("在吗", "在的，请问有什么可以帮您？");
        QUICK_RESPONSES.put("嗨", "你好！请问需要什么帮助？");
        QUICK_RESPONSES.put("在不在", "在的，随时为您服务！");
        QUICK_RESPONSES.put("你好呀", "你好！请问需要什么帮助？");
        QUICK_RESPONSES.put("哈喽", "你好！有什么可以帮您的吗？");
        
        // 能力询问
        String capabilityResponse = "我可以帮您：\n\n📝 **SQL相关**\n• 自然语言生成SQL\n• SQL性能优化\n• SQL语句解释\n\n📊 **数据分析**\n• 数据洞察分析\n• 趋势预测建议\n\n📈 **可视化**\n• 智能图表生成\n• 报表自动设计\n\n🔄 **数据处理**\n• ETL流程生成\n• 数据同步配置\n\n直接描述您的需求即可！";
        QUICK_RESPONSES.put("你能做什么", capabilityResponse);
        QUICK_RESPONSES.put("你会什么", capabilityResponse);
        QUICK_RESPONSES.put("能做什么", capabilityResponse);
        QUICK_RESPONSES.put("有什么功能", capabilityResponse);
        QUICK_RESPONSES.put("功能介绍", capabilityResponse);
        QUICK_RESPONSES.put("介绍一下", capabilityResponse);
        QUICK_RESPONSES.put("你是谁", "我是数据平台AI助手，专注于SQL生成、数据分析、图表设计等任务。有什么可以帮您的？");
        QUICK_RESPONSES.put("who are you", "I'm an AI assistant for this data platform. I can help with SQL, data analysis, and chart design.");
        
        // 帮助
        String helpResponse = "**🤖 AI助手使用指南**\n\n**SQL生成**\n```\n帮我写一个查询用户订单的SQL\n```\n\n**SQL优化**\n```\n优化这个SQL: SELECT * FROM orders\n```\n\n**图表生成**\n```\n生成一个销售趋势折线图\n```\n\n**数据分析**\n```\n分析最近7天的用户活跃度\n```\n\n**ETL流程**\n```\n创建一个数据同步流程\n```\n\n💡 直接用自然语言描述需求即可！";
        QUICK_RESPONSES.put("帮助", helpResponse);
        QUICK_RESPONSES.put("help", helpResponse);
        QUICK_RESPONSES.put("怎么用", helpResponse);
        QUICK_RESPONSES.put("使用说明", helpResponse);
        QUICK_RESPONSES.put("怎么使用", helpResponse);
        QUICK_RESPONSES.put("如何使用", helpResponse);
        
        // 感谢
        QUICK_RESPONSES.put("谢谢", "不客气！如有其他问题随时问我 😊");
        QUICK_RESPONSES.put("感谢", "很高兴能帮到您！还有什么需要帮忙的吗？");
        QUICK_RESPONSES.put("谢谢你", "不用谢！随时为您服务。");
        QUICK_RESPONSES.put("多谢", "客气了，有问题随时找我！");
        QUICK_RESPONSES.put("thanks", "You're welcome! Let me know if you need anything else.");
        QUICK_RESPONSES.put("thank you", "You're welcome!");
        QUICK_RESPONSES.put("thx", "You're welcome!");
        
        // 再见
        QUICK_RESPONSES.put("再见", "再见！有问题随时找我 👋");
        QUICK_RESPONSES.put("拜拜", "拜拜！祝您工作顺利！");
        QUICK_RESPONSES.put("bye", "Goodbye! Feel free to ask if you need help.");
        QUICK_RESPONSES.put("88", "拜拜！下次见～");
        QUICK_RESPONSES.put("886", "拜拜！有问题随时来找我～");
        
        // 确认/肯定
        QUICK_RESPONSES.put("好的", "好的，请继续告诉我您的需求。");
        QUICK_RESPONSES.put("ok", "OK, please continue with your request.");
        QUICK_RESPONSES.put("好", "好的，请问还有什么需要？");
        QUICK_RESPONSES.put("明白了", "好的，如有其他问题请随时提问。");
        QUICK_RESPONSES.put("知道了", "好的，还有什么可以帮您的吗？");
        
        // 否定/取消
        QUICK_RESPONSES.put("没事了", "好的，如有需要随时找我！");
        QUICK_RESPONSES.put("算了", "好的，有其他问题随时问我。");
        QUICK_RESPONSES.put("不用了", "好的，如有需要随时找我！");
        
        // ============ 意图关键词映射 ============
        
        INTENT_KEYWORDS.put(IntentType.SQL_GENERATE, Arrays.asList(
            "写sql", "生成sql", "sql语句", "帮我查", "查询", "写个查询", 
            "select", "insert", "update", "delete", "创建表", "建表"
        ));
        
        INTENT_KEYWORDS.put(IntentType.SQL_OPTIMIZE, Arrays.asList(
            "优化sql", "sql优化", "性能优化", "慢查询", "执行计划", "索引优化"
        ));
        
        INTENT_KEYWORDS.put(IntentType.SQL_EXPLAIN, Arrays.asList(
            "解释sql", "sql解释", "什么意思", "这个sql", "分析sql"
        ));
        
        INTENT_KEYWORDS.put(IntentType.CHART_GENERATE, Arrays.asList(
            "图表", "柱状图", "折线图", "饼图", "散点图", "可视化", 
            "生成图", "做个图", "画个图", "chart"
        ));
        
        INTENT_KEYWORDS.put(IntentType.ETL_FLOW, Arrays.asList(
            "etl", "流程", "数据同步", "数据迁移", "数据抽取", "pipeline"
        ));
        
        INTENT_KEYWORDS.put(IntentType.DATA_ANALYSIS, Arrays.asList(
            "分析", "统计", "趋势", "对比", "汇总", "报表", "洞察"
        ));
    }
    
    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        // 应用完全就绪后再从数据库加载AI配置，避免DB未ready导致加载失败
        log.info("应用就绪，开始从数据库加载AI配置...");
        configManager.loadConfigFromDb();
        log.info("AI配置加载完成: provider={}, enabled={}", aiConfig.getProvider(), aiConfig.isEnabled());
    }
    

    /**
     * 智能SQL生成
     * @param naturalLanguage 自然语言描述
     * @param tableSchema 表结构信息（可选）
     * @param dbType 数据库类型（mysql/postgresql/oracle/sqlserver 等）
     * @return 生成的SQL
     */
    public Map<String, Object> generateSql(String naturalLanguage, String tableSchema) {
        return generateSql(naturalLanguage, tableSchema, null);
    }

    public Map<String, Object> generateSql(String naturalLanguage, String tableSchema, String dbType) {
        if (!aiConfig.isEnabled() || !aiConfig.getFeatures().isSqlGeneration()) {
            return errorResult("AI功能未启用或SQL生成功能已关闭");
        }
        
        String systemPrompt = buildSqlSystemPrompt(tableSchema, dbType);
        String userPrompt = "请根据以下需求生成SQL语句：\n" + naturalLanguage;
        
        try {
            String response = callAiProvider(systemPrompt, userPrompt);
            return successResult(response, "sql");
        } catch (Exception e) {
            return handleAiException(e, "SQL生成");
        }
    }

    /**
     * 数据分析助手
     * @param question 用户问题
     * @param dataContext 数据上下文（表结构、示例数据等）
     * @return 分析结果
     */
    public Map<String, Object> analyzeData(String question, String dataContext) {
        if (!aiConfig.isEnabled() || !aiConfig.getFeatures().isDataAnalysis()) {
            return errorResult("AI功能未启用或数据分析功能已关闭");
        }
        
        String systemPrompt = """
            你是数据分析专家。基于数据上下文回答问题，输出格式：
            1. **数据洞察**：关键发现（用数据说话）
            2. **可视化建议**：推荐图表类型及维度/指标
            3. **深入方向**：值得进一步探索的分析角度
            如需SQL辅助分析，放在```sql代码块中。
            """;
        
        String userPrompt = "数据上下文：\n" + dataContext + "\n\n问题：" + question;
        
        try {
            String response = callAiProvider(systemPrompt, userPrompt);
            return successResult(response, "analysis");
        } catch (Exception e) {
            return handleAiException(e, "数据分析");
        }
    }

    /**
     * SQL优化建议
     * @param sql 原始SQL
     * @param dbType 数据库类型
     * @return 优化建议
     */
    public Map<String, Object> optimizeSql(String sql, String dbType) {
        if (!aiConfig.isEnabled() || !aiConfig.getFeatures().isCodeOptimization()) {
            return errorResult("AI功能未启用或代码优化功能已关闭");
        }
        
        String dialect = normalizeDbDialect(dbType);
        String systemPrompt = """
            你是%s数据库性能优化专家。分析SQL并给出优化方案：
            1. **问题诊断**：指出性能瓶颈（全表扫描、笛卡尔积、子查询等）
            2. **优化SQL**：给出优化后的SQL（```sql代码块）
            3. **索引建议**：推荐创建的索引及原因
            4. 优化后的SQL必须使用%s方言，不能混用其他数据库语法
            直接给出最优方案，不需要列举多种选项。
            """.formatted(dialect, dialect);
        
        try {
            String response = callAiProvider(systemPrompt, "请优化以下SQL：\n" + sql);
            return successResult(response, "optimization");
        } catch (Exception e) {
            return handleAiException(e, "SQL优化");
        }
    }

    /**
     * 意图分类 - 快速识别用户意图
     * 优先级：业务意图关键词 > 问候/帮助等简单意图
     * 解决"你好，帮我写个SQL"被错误识别为GREETING的问题
     */
    public IntentType classifyIntent(String message) {
        if (message == null || message.trim().isEmpty()) {
            return IntentType.GENERAL;
        }
        
        String msg = message.trim().toLowerCase();
        
        // 1. 优先检查业务意图关键词（避免"你好，帮我写个SQL"被误判为GREETING）
        IntentType[] priorityOrder = {
            IntentType.SQL_OPTIMIZE,   // 优先匹配优化（比生成更具体）
            IntentType.SQL_EXPLAIN,    // 解释
            IntentType.SQL_GENERATE,   // SQL生成
            IntentType.CHART_GENERATE, // 图表
            IntentType.ETL_FLOW,       // ETL
            IntentType.DATA_ANALYSIS   // 数据分析
        };
        
        for (IntentType intentType : priorityOrder) {
            List<String> keywords = INTENT_KEYWORDS.get(intentType);
            if (keywords != null) {
                for (String keyword : keywords) {
                    if (msg.contains(keyword)) {
                        return intentType;
                    }
                }
            }
        }
        
        // 2. 没有业务意图时，再判断简单意图（问候/帮助/能力询问）
        if (QUICK_RESPONSES.containsKey(msg) || QUICK_RESPONSES.containsKey(message.trim())) {
            if (msg.contains("帮助") || msg.equals("help") || msg.contains("怎么用")) return IntentType.HELP;
            if (msg.contains("你能") || msg.contains("你会") || msg.contains("功能")) return IntentType.CAPABILITY;
            return IntentType.GREETING;
        }
        
        return IntentType.GENERAL;
    }
    
    /**
     * 生成问题的缓存键（标准化处理）
     */
    private String generateCacheKey(String message, String context) {
        // 标准化：去除多余空格，转小写，提取核心词
        String normalized = message.trim().toLowerCase()
            .replaceAll("\\s+", " ")
            .replaceAll("[，。！？,.!?]", "");
        
        // 结合上下文生成唯一键
        if (context != null && !context.isEmpty()) {
            return normalized + "::" + context.hashCode();
        }
        return normalized;
    }
    
    /**
     * 尝试从缓存获取响应
     */
    private String tryGetFromCache(String cacheKey) {
        CachedResponse cached = responseCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            cached.hitCount.incrementAndGet();
            cached.lastAccessTime = System.currentTimeMillis();
            log.debug("缓存命中: key={}, hitCount={}", cacheKey.substring(0, Math.min(20, cacheKey.length())), cached.hitCount.get());
            return cached.response;
        }
        if (cached != null && cached.isExpired()) {
            responseCache.remove(cacheKey);
        }
        return null;
    }
    
    /**
     * 保存响应到缓存
     */
    private void saveToCache(String cacheKey, String response) {
        // 缓存大小限制：先清理过期条目
        if (responseCache.size() >= CACHE_MAX_SIZE) {
            responseCache.entrySet().removeIf(e -> e.getValue().isExpired());
        }
        // 仍然满则按LRU策略移除最久未访问的条目
        while (responseCache.size() >= CACHE_MAX_SIZE) {
            String lruKey = null;
            long lruTime = Long.MAX_VALUE;
            for (Map.Entry<String, CachedResponse> entry : responseCache.entrySet()) {
                if (entry.getValue().lastAccessTime < lruTime) {
                    lruTime = entry.getValue().lastAccessTime;
                    lruKey = entry.getKey();
                }
            }
            if (lruKey != null) {
                responseCache.remove(lruKey);
            } else {
                break;
            }
        }
        responseCache.put(cacheKey, new CachedResponse(response));
    }
    
    // 业务动作关键词 - 包含这些词说明用户有明确业务需求，不应匹配快速响应
    private static final List<String> BUSINESS_ACTION_KEYWORDS = Arrays.asList(
        "帮我", "帮忙", "写", "生成", "创建", "查询", "分析", "优化", "设计", "统计",
        "sql", "select", "insert", "update", "delete", "表", "图表", "报表",
        "数据", "导入", "导出", "同步", "etl", "流程", "请"
    );
    
    /**
     * 检查消息是否包含业务动作关键词
     */
    private boolean containsBusinessKeyword(String lowerMessage) {
        for (String keyword : BUSINESS_ACTION_KEYWORDS) {
            if (lowerMessage.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 尝试获取快速响应（不调用AI）
     * 仅对纯粹的简单消息返回快速响应，包含业务意图的消息交给AI处理
     */
    private String tryQuickResponse(String message) {
        if (message == null) return null;
        String trimmed = message.trim();
        String lower = trimmed.toLowerCase();
        
        // 0. 如果包含业务关键词，说明有明确需求，不走快速响应
        if (containsBusinessKeyword(lower)) {
            return null;
        }
        
        // 1. 精确匹配
        if (QUICK_RESPONSES.containsKey(trimmed)) {
            return QUICK_RESPONSES.get(trimmed);
        }
        if (QUICK_RESPONSES.containsKey(lower)) {
            return QUICK_RESPONSES.get(lower);
        }
        
        // 2. 去除标点后匹配
        String noPunct = lower.replaceAll("[，。！？,.!?~～…]", "").trim();
        if (QUICK_RESPONSES.containsKey(noPunct)) {
            return QUICK_RESPONSES.get(noPunct);
        }
        
        // 3. 短消息近似匹配（仅当消息几乎等于关键词时才匹配）
        if (noPunct.length() <= 8) {
            for (Map.Entry<String, String> entry : QUICK_RESPONSES.entrySet()) {
                String key = entry.getKey();
                // 只允许消息包含关键词且长度不超过关键词的1.5倍（如"你好呀"匹配"你好"）
                if (noPunct.contains(key) && noPunct.length() <= key.length() * 1.5 + 2) {
                    return entry.getValue();
                }
            }
        }
        
        return null;
    }
    
    /**
     * 获取缓存统计信息
     */
    public Map<String, Object> getCacheStats() {
        int total = responseCache.size();
        int expired = 0;
        int totalHits = 0;
        for (CachedResponse cached : responseCache.values()) {
            if (cached.isExpired()) expired++;
            totalHits += cached.hitCount.get();
        }
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalEntries", total);
        stats.put("expiredEntries", expired);
        stats.put("activeEntries", total - expired);
        stats.put("totalHits", totalHits);
        stats.put("maxSize", CACHE_MAX_SIZE);
        stats.put("ttlMinutes", CACHE_TTL_MS / 60000);
        return stats;
    }
    
    /**
     * 清除响应缓存
     */
    public void clearResponseCache() {
        responseCache.clear();
        log.info("AI响应缓存已清除");
    }
    
    /**
     * 通用对话接口（支持会话历史）
     * 优化：先尝试快速响应，再调用AI
     * @param message 用户消息
     * @param context 上下文（可选）
     * @param sessionId 会话ID（可选，用于保持对话上下文）
     * @return AI回复
     */
    public Map<String, Object> chatWithContext(String message, String context, String sessionId) {
        long startTime = System.currentTimeMillis();
        
        if (!aiConfig.isEnabled()) {
            return errorResult("AI功能未启用");
        }
        
        // ★ 第1层：快速响应路径（简单问题，毫秒级）
        String quickResponse = tryQuickResponse(message);
        if (quickResponse != null) {
            log.debug("快速响应命中: {} ({}ms)", message, System.currentTimeMillis() - startTime);
            consumeAiUsage(quickResponse);
            Map<String, Object> result = successResult(quickResponse, "quick");
            result.put("intent", classifyIntent(message).name());
            result.put("fastPath", true);
            result.put("responseTime", System.currentTimeMillis() - startTime);
            // 添加智能建议
            if (chatOptimizer != null) {
                result.put("suggestions", chatOptimizer.generateSuggestions("GENERAL", null));
            }
            return result;
        }
        
        // ★ 第1.5层：语义意图识别（使用向量相似度）
        IntentType intent = classifyIntent(message);
        double intentConfidence = 0.0;
        // 仅对较长消息做HanLP语义分析（短消息关键词匹配已足够，省去NLP开销）
        if (chatOptimizer != null && message.trim().length() > 4) {
            try {
                AiChatOptimizer.IntentResult semanticIntent = chatOptimizer.classifyIntentSemantic(message);
                if (semanticIntent.isConfident()) {
                    try {
                        intent = IntentType.valueOf(semanticIntent.intent);
                        intentConfidence = semanticIntent.confidence;
                        log.debug("语义意图识别: {} (置信度: {})", intent, String.format("%.2f", intentConfidence));
                    } catch (IllegalArgumentException e) {
                        log.debug("语义意图枚举匹配失败: {}", semanticIntent.intent);
                    }
                }
            } catch (Exception e) {
                log.debug("语义意图识别降级: {}", e.getMessage());
            }
        }
        log.debug("最终意图: {} -> {}", message.substring(0, Math.min(20, message.length())), intent);
        
        // ★ 第2层：缓存响应路径（相似问题，毫秒级）
        String cacheKey = null;
        if (sessionId == null || sessionId.isEmpty() || intent == IntentType.GENERAL) {
            cacheKey = generateCacheKey(message, context);
            String cachedResponse = tryGetFromCache(cacheKey);
            if (cachedResponse != null) {
                log.debug("缓存响应命中: {} ({}ms)", cacheKey.substring(0, Math.min(20, cacheKey.length())), System.currentTimeMillis() - startTime);
                consumeAiUsage(cachedResponse);
                Map<String, Object> result = successResult(cachedResponse, "cached");
                result.put("intent", intent.name());
                result.put("fastPath", true);
                result.put("cached", true);
                result.put("responseTime", System.currentTimeMillis() - startTime);
                return result;
            }
        }
        
        // ★ 第3层：AI调用路径（复杂问题）
        // 使用优化器增强上下文
        String enhancedContext = context;
        if (chatOptimizer != null && sessionId != null) {
            try {
                enhancedContext = chatOptimizer.enhanceContext(message, sessionId, context);
                chatOptimizer.updateSessionState(sessionId, intent.name(), null);
            } catch (Exception e) {
                log.debug("上下文增强失败: {}", e.getMessage());
            }
        }
        
        String systemPrompt = buildSystemPrompt(enhancedContext);
        
        try {
            // 获取会话历史（限制最多发送最近3轮=6条消息，减少token开销）
            List<Map<String, String>> history = null;
            if (sessionId != null && !sessionId.isEmpty()) {
                history = getSessionHistory(sessionId);
                if (history != null && history.size() > 6) {
                    history = history.subList(history.size() - 6, history.size());
                }
            }
            
            AiUsageReservation usageReservation = reserveAiUsage();
            String response;
            try {
                response = providerClient.callAiProviderWithHistory(systemPrompt, message, history);
                recordAiResponseTokens(usageReservation, response);
            } catch (Exception e) {
                rollbackAiUsage(usageReservation);
                throw e;
            }
            
            // 保存到会话历史
            if (sessionId != null && !sessionId.isEmpty()) {
                saveToHistory(sessionId, message, response);
            }
            
            // 保存到缓存（仅缓存通用问题的响应）
            if (cacheKey != null && response != null && !response.isEmpty()) {
                saveToCache(cacheKey, response);
            }
            
            long duration = System.currentTimeMillis() - startTime;
            log.debug("AI响应完成: intent={}, duration={}ms", intent, duration);
            
            Map<String, Object> result = successResult(response, "chat");
            result.put("intent", intent.name());
            result.put("fastPath", false);
            result.put("responseTime", duration);
            
            // 添加智能建议（基于意图）
            if (chatOptimizer != null) {
                AiChatOptimizer.SessionState state = sessionId != null ? 
                    chatOptimizer.getSessionState(sessionId) : null;
                result.put("suggestions", chatOptimizer.generateSuggestions(intent.name(), state));
                
                // 提取关键词
                List<String> keywords = chatOptimizer.extractKeywords(message);
                if (!keywords.isEmpty()) {
                    result.put("keywords", keywords);
                }
            }
            
            return result;
        } catch (Exception e) {
            return handleAiException(e, "AI对话");
        }
    }
    
    /**
     * 简化的对话接口（无会话历史）
     */
    public Map<String, Object> chatWithContext(String message, String context) {
        return chatWithContext(message, context, null);
    }
    
    /**
     * 获取会话历史（优先Redis，降级到内存）
     */
    private List<Map<String, String>> getSessionHistory(String sessionId) {
        if (redisTemplate != null) {
            try {
                String key = REDIS_CHAT_PREFIX + sessionId;
                String json = redisTemplate.opsForValue().get(key);
                if (json != null && !json.isEmpty()) {
                    return objectMapper.readValue(json, new TypeReference<List<Map<String, String>>>() {});
                }
                return null;
            } catch (Exception e) {
                log.debug("从Redis获取会话历史失败，降级到内存: {}", e.getMessage());
            }
        }
        return conversationHistory.get(sessionId);
    }

    /**
     * 保存会话历史（优先Redis，降级到内存）
     */
    private void saveToHistory(String sessionId, String userMessage, String assistantResponse) {
        List<Map<String, String>> history = getSessionHistory(sessionId);
        if (history == null) {
            history = new ArrayList<>();
        }
        
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        history.add(userMsg);
        
        Map<String, String> assistantMsg = new HashMap<>();
        assistantMsg.put("role", "assistant");
        assistantMsg.put("content", assistantResponse);
        history.add(assistantMsg);
        
        // 限制历史长度
        int maxSize = configManager.getMaxHistorySize() * 2;
        if (history.size() > maxSize) {
            int excess = history.size() - maxSize;
            excess = (excess + 1) / 2 * 2;
            history.subList(0, Math.min(excess, history.size())).clear();
        }
        
        // 优先存Redis
        if (redisTemplate != null) {
            try {
                String key = REDIS_CHAT_PREFIX + sessionId;
                String json = objectMapper.writeValueAsString(history);
                redisTemplate.opsForValue().set(key, json, REDIS_SESSION_TTL_SECONDS, TimeUnit.SECONDS);
                return;
            } catch (Exception e) {
                log.debug("保存会话历史到Redis失败，降级到内存: {}", e.getMessage());
            }
        }
        
        // 降级到内存存储
        conversationHistory.put(sessionId, Collections.synchronizedList(history));
        conversationLastAccess.put(sessionId, System.currentTimeMillis());
        
        // 节流清理内存会话
        long now = System.currentTimeMillis();
        if (now - lastCleanupTime > CLEANUP_INTERVAL_MS || conversationHistory.size() > MAX_SESSIONS) {
            lastCleanupTime = now;
            cleanExpiredSessions();
        }
    }
    
    /**
     * 清理过期会话（仅内存模式时需要，Redis模式依赖TTL自动过期）
     */
    private void cleanExpiredSessions() {
        long now = System.currentTimeMillis();
        conversationLastAccess.entrySet().removeIf(entry -> {
            if (now - entry.getValue() > SESSION_TTL_MS) {
                conversationHistory.remove(entry.getKey());
                return true;
            }
            return false;
        });
        while (conversationHistory.size() > MAX_SESSIONS) {
            String oldestKey = conversationLastAccess.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
            if (oldestKey != null) {
                conversationHistory.remove(oldestKey);
                conversationLastAccess.remove(oldestKey);
            } else {
                break;
            }
        }
    }
    
    /**
     * 清除会话历史
     */
    public void clearConversation(String sessionId) {
        conversationHistory.remove(sessionId);
        conversationLastAccess.remove(sessionId);
        if (redisTemplate != null) {
            try {
                redisTemplate.delete(REDIS_CHAT_PREFIX + sessionId);
            } catch (Exception e) {
                log.debug("从Redis删除会话失败: {}", e.getMessage());
            }
        }
    }
    
    /**
     * 根据表结构智能生成SQL
     */
    public Map<String, Object> generateSqlWithSchema(String naturalLanguage, Long dataSourceId, String tableName) {
        if (!aiConfig.isEnabled() || !aiConfig.getFeatures().isSqlGeneration()) {
            return errorResult("AI功能未启用或SQL生成功能已关闭");
        }
        
        // 从数据源获取表结构信息
        String tableSchema = getTableSchemaInfo(dataSourceId, tableName);
        
        return generateSql(naturalLanguage, tableSchema, resolveDbType(dataSourceId, null));
    }
    
    /**
     * 获取表结构信息用于AI生成SQL
     */
    private String getTableSchemaInfo(Long dataSourceId, String tableName) {
        StringBuilder schema = new StringBuilder();
        schema.append("表名: ").append(tableName).append("\n");
        
        if (dataSourceService == null || dataSourceId == null) {
            return schema.toString();
        }
        
        try {
            // 获取表字段信息
            List<Map<String, Object>> columns = dataSourceService.getTableColumns(dataSourceId, tableName);
            if (columns != null && !columns.isEmpty()) {
                schema.append("字段列表:\n");
                for (Map<String, Object> col : columns) {
                    String colName = String.valueOf(col.getOrDefault("columnName", col.getOrDefault("COLUMN_NAME", "")));
                    String colType = String.valueOf(col.getOrDefault("dataType", col.getOrDefault("DATA_TYPE", "")));
                    String colComment = String.valueOf(col.getOrDefault("remarks", col.getOrDefault("REMARKS", "")));
                    String nullable = String.valueOf(col.getOrDefault("nullable", col.getOrDefault("IS_NULLABLE", "")));
                    
                    schema.append("  - ").append(colName)
                          .append(" (").append(colType).append(")");
                    if (colComment != null && !colComment.isEmpty() && !"null".equals(colComment)) {
                        schema.append(" -- ").append(colComment);
                    }
                    if ("NO".equalsIgnoreCase(nullable) || "0".equals(nullable)) {
                        schema.append(" [NOT NULL]");
                    }
                    schema.append("\n");
                }
            }
        } catch (Exception e) {
            log.warn("获取表结构信息失败: dataSourceId={}, tableName={}, error={}", dataSourceId, tableName, e.getMessage());
        }
        
        return schema.toString();
    }
    
    /**
     * 解释SQL语句
     */
    public Map<String, Object> explainSql(String sql) {
        return explainSql(sql, null);
    }

    public Map<String, Object> explainSql(String sql, String dbType) {
        if (!aiConfig.isEnabled()) {
            return errorResult("AI功能未启用");
        }
        
        String dialect = normalizeDbDialect(dbType);
        String systemPrompt = """
            你是%s数据库SQL专家。请按%s方言解释以下SQL语句的含义和执行逻辑。
            
            解释应包括：
            1. SQL的整体功能描述
            2. 各个子句的作用
            3. 涉及的表和字段
            4. 可能的性能影响
            5. 潜在的问题或改进建议
            
            请使用中文，条理清晰地回答。
            """.formatted(dialect, dialect);
        
        try {
            String response = callAiProvider(systemPrompt, "请解释这个SQL语句：\n" + sql);
            return successResult(response, "explain");
        } catch (Exception e) {
            return handleAiException(e, "SQL解释");
        }
    }
    
    /**
     * 智能问答 - 快捷指令
     */
    public Map<String, Object> quickCommand(String command, Map<String, Object> params) {
        if (!aiConfig.isEnabled()) {
            return errorResult("AI功能未启用");
        }
        if (command == null || command.isBlank()) {
            return errorResult("快捷指令不能为空");
        }
        if (params == null) {
            params = Collections.emptyMap();
        }
        
        String systemPrompt;
        String userPrompt;
        
        switch (command.toLowerCase()) {
            case "create_table":
                systemPrompt = "你是数据库设计专家。请根据用户描述生成CREATE TABLE语句。";
                userPrompt = "请根据以下需求生成建表SQL：" + params.get("description");
                break;
            case "index_suggestion":
                systemPrompt = "你是数据库性能优化专家。请根据表结构和查询模式给出索引建议。";
                userPrompt = "表结构：" + params.get("tableSchema") + "\n查询SQL：" + params.get("querySql");
                break;
            case "data_clean":
                systemPrompt = "你是数据清洗专家。请给出数据清洗的SQL语句和建议。";
                userPrompt = "数据问题：" + params.get("issue") + "\n表信息：" + params.get("tableInfo");
                break;
            case "data_compare":
                systemPrompt = "你是数据对比分析专家。请生成两个表或数据集之间的对比SQL，找出差异数据。";
                userPrompt = "源表：" + params.get("sourceTable") + "\n目标表：" + params.get("targetTable")
                    + (params.get("compareFields") != null ? "\n对比字段：" + params.get("compareFields") : "");
                break;
            case "schema_design":
                systemPrompt = "你是数据库建模专家。请根据业务需求设计合理的数据库表结构，遵循第三范式，并给出ER关系说明。";
                userPrompt = "业务需求：" + params.get("requirement");
                break;
            case "query_template":
                systemPrompt = "你是SQL专家。请根据表结构生成常用的查询模板，包括：分页查询、条件搜索、统计汇总、分组排序等。使用```sql代码块格式。";
                userPrompt = "表信息：" + params.get("tableInfo")
                    + (params.get("scenario") != null ? "\n使用场景：" + params.get("scenario") : "");
                break;
            case "data_sample":
                systemPrompt = "你是测试数据专家。请根据表结构生成INSERT语句来插入合理的测试数据，数据应符合字段类型和业务逻辑。";
                userPrompt = "表结构：" + params.get("tableSchema")
                    + "\n生成数量：" + params.getOrDefault("count", "10") + "条";
                break;
            default:
                return errorResult("未知的快捷指令: " + command);
        }
        
        try {
            String response = callAiProvider(systemPrompt, userPrompt);
            return successResult(response, "command");
        } catch (Exception e) {
            return handleAiException(e, "快捷指令执行");
        }
    }
    
    /**
     * 生成ETL流程配置
     * 根据用户需求生成符合系统Pipeline结构的ETL流程JSON
     */
    public Map<String, Object> generateEtlFlow(String requirement, Map<String, Object> context) {
        if (!aiConfig.isEnabled()) {
            return errorResult("AI功能未启用");
        }
        
        String systemPrompt = """
            你是一个数据ETL专家。请根据用户需求生成ETL流程配置。
            
            系统支持的节点类型：
            1. data - 数据节点（支持read/write/sync操作）
               配置项：operation(read/write/sync), dataSourceId, tableName, sql, writeMode(insert/upsert/replace)
            2. script - 脚本节点（数据转换）
               配置项：scriptType(sql/javascript), script
            3. shell - Shell脚本节点
               配置项：script, workDir
            4. http - HTTP请求节点
               配置项：url, method, headers, body
            5. condition - 条件分支节点
               配置项：conditionType, expression
            6. sub_process - 子流程节点
               配置项：subPipelineId
            
            请返回JSON格式的流程配置，包含：
            {
              "pipelineName": "流程名称",
              "pipelineDesc": "流程描述",
              "pipelineType": 1, // 1-ETL流程, 2-数据清洗, 3-数据同步, 4-数据聚合
              "nodes": [
                {
                  "nodeCode": "node_1",
                  "nodeName": "节点名称",
                  "nodeType": "data",
                  "nodeConfig": { ... },
                  "positionX": 100,
                  "positionY": 100,
                  "preTaskCodes": [] // 前置节点编码数组
                }
              ],
              "edges": [
                { "source": "node_1", "target": "node_2" }
              ]
            }
            
            请使用中文回答，确保生成的配置可以直接导入系统使用。
            """;
        
        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("请根据以下需求生成ETL流程配置：\n\n");
        userPrompt.append(requirement);
        
        if (context != null) {
            if (context.containsKey("dataSources")) {
                userPrompt.append("\n\n可用数据源：").append(context.get("dataSources"));
            }
            if (context.containsKey("tables")) {
                userPrompt.append("\n\n可用表：").append(context.get("tables"));
            }
        }
        
        try {
            String response = callAiProvider(systemPrompt, userPrompt.toString());
            Map<String, Object> result = successResult(response, "etl");
            
            // 尝试解析JSON配置
            try {
                int jsonStart = response.indexOf("{");
                int jsonEnd = response.lastIndexOf("}");
                if (jsonStart >= 0 && jsonEnd > jsonStart) {
                    String jsonStr = response.substring(jsonStart, jsonEnd + 1);
                    Map<String, Object> flowConfig = objectMapper.readValue(jsonStr, new TypeReference<Map<String, Object>>() {});
                    result.put("flowConfig", flowConfig);
                    result.put("parseable", true);
                }
            } catch (Exception e) {
                result.put("parseable", false);
                log.warn("ETL配置解析失败，返回原始响应: {}", e.getMessage());
            }
            
            return result;
        } catch (Exception e) {
            return handleAiException(e, "ETL流程生成");
        }
    }
    
    /**
     * 分析数据血缘关系
     */
    public Map<String, Object> analyzeDataLineage(Long dataSourceId, String tableName) {
        if (!aiConfig.isEnabled()) {
            return errorResult("AI功能未启用");
        }
        
        // 收集真实的血缘上下文
        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("目标表: ").append(tableName).append(" (数据源ID: ").append(dataSourceId).append(")\n\n");
        
        // 1. 查询涉及该表的ETL任务（限制扫描数量，避免全量加载）
        List<Map<String, Object>> relatedEtlJobs = new ArrayList<>();
        if (dataxJobMapper != null) {
            try {
                List<DataxJob> allJobs = dataxJobMapper.selectList(0, 500, null, null);
                int matchLimit = 20; // 最多返回20个关联任务
                for (DataxJob job : allJobs) {
                    if (relatedEtlJobs.size() >= matchLimit) break;
                    boolean isSource = tableName.equalsIgnoreCase(job.getSourceTable()) 
                        && (dataSourceId.equals(job.getSourceDataSourceId()));
                    boolean isTarget = tableName.equalsIgnoreCase(job.getTargetTable()) 
                        && (dataSourceId.equals(job.getTargetDataSourceId()));
                    if (isSource || isTarget) {
                        Map<String, Object> etlInfo = new HashMap<>();
                        etlInfo.put("jobName", job.getJobName());
                        etlInfo.put("jobDesc", job.getJobDesc());
                        etlInfo.put("role", isSource ? "source" : "target");
                        etlInfo.put("sourceTable", job.getSourceTable());
                        etlInfo.put("targetTable", job.getTargetTable());
                        etlInfo.put("writeMode", job.getWriteMode());
                        relatedEtlJobs.add(etlInfo);
                    }
                }
            } catch (Exception e) {
                log.debug("查询ETL任务失败: {}", e.getMessage());
            }
        }
        
        if (!relatedEtlJobs.isEmpty()) {
            contextBuilder.append("### 关联ETL任务（").append(relatedEtlJobs.size()).append("个）\n");
            for (Map<String, Object> etl : relatedEtlJobs) {
                contextBuilder.append("- ").append(etl.get("jobName"))
                    .append(" [角色:").append("source".equals(etl.get("role")) ? "数据来源" : "数据目标").append("]")
                    .append(" 源表=").append(etl.get("sourceTable"))
                    .append(" → 目标表=").append(etl.get("targetTable"))
                    .append(" (").append(etl.get("writeMode")).append(")\n");
            }
            contextBuilder.append("\n");
        } else {
            contextBuilder.append("### 未找到关联ETL任务\n\n");
        }
        
        // 2. 查询引用该表的报表
        List<Map<String, Object>> relatedReports = new ArrayList<>();
        if (reportDefinitionService != null) {
            try {
                List<ReportDefinition> reports = 
                    reportDefinitionService.getReportDefinitionList(1, 200, null);
                for (ReportDefinition report : reports) {
                    if (dataSourceId.equals(report.getDataSourceId()) && report.getSqlContent() != null
                            && report.getSqlContent().toLowerCase().contains(tableName.toLowerCase())) {
                        Map<String, Object> reportInfo = new HashMap<>();
                        reportInfo.put("reportName", report.getReportName());
                        reportInfo.put("reportCode", report.getReportCode());
                        reportInfo.put("sqlSnippet", report.getSqlContent().length() > 200 
                            ? report.getSqlContent().substring(0, 200) + "..." : report.getSqlContent());
                        relatedReports.add(reportInfo);
                    }
                }
            } catch (Exception e) {
                log.debug("查询报表失败: {}", e.getMessage());
            }
        }
        
        if (!relatedReports.isEmpty()) {
            contextBuilder.append("### 关联报表（").append(relatedReports.size()).append("个）\n");
            for (Map<String, Object> rpt : relatedReports) {
                contextBuilder.append("- ").append(rpt.get("reportName"))
                    .append(" (").append(rpt.get("reportCode")).append(")")
                    .append(" SQL片段: ").append(rpt.get("sqlSnippet")).append("\n");
            }
            contextBuilder.append("\n");
        } else {
            contextBuilder.append("### 未找到引用该表的报表\n\n");
        }
        
        // 3. 获取表结构
        try {
            List<Map<String, Object>> tableStructure = tableDataService.getTableStructure(dataSourceId, tableName);
            contextBuilder.append("### 表结构（").append(tableStructure.size()).append("列）\n");
            for (Map<String, Object> col : tableStructure) {
                contextBuilder.append("- ").append(col.get("columnName"))
                    .append(" (").append(col.get("dataType")).append(")\n");
            }
        } catch (Exception e) {
            log.debug("获取表结构失败: {}", e.getMessage());
        }
        
        String systemPrompt = """
            你是数据治理专家。请基于以下真实系统数据分析该表的血缘关系。
            
            请从以下维度分析：
            1. **上游依赖**：根据ETL任务中该表作为target的记录，分析数据来源
            2. **下游影响**：根据ETL任务中该表作为source的记录和引用该表的报表，分析数据消费方
            3. **字段级血缘**：根据表结构和SQL推断字段的来源和去向
            4. **数据流图**：概述完整的数据流转路径
            
            请用结构化的Markdown格式输出分析结果。
            """;
        
        try {
            String response = callAiProvider(systemPrompt, contextBuilder.toString());
            Map<String, Object> result = successResult(response, "lineage");
            result.put("relatedEtlJobs", relatedEtlJobs);
            result.put("relatedReports", relatedReports);
            return result;
        } catch (Exception e) {
            return handleAiException(e, "数据血缘分析");
        }
    }

    /**
     * 测试AI提供商连接（委托 AiProviderClient）
     */
    public String testProviderConnection(String provider, String apiKey, String baseUrl, String model) throws Exception {
        return providerClient.testProviderConnection(provider, apiKey, baseUrl, model);
    }
    
    /**
     * 核心对话方法 - 委托 AiProviderClient
     * 保留public接口供 AiChartService 等服务调用
     * 自动记录使用量并检查配额
     */
    public String callAiProvider(String systemPrompt, String userMessage) throws Exception {
        AiUsageReservation usageReservation = reserveAiUsage();
        String response;
        try {
            response = providerClient.callAiProvider(systemPrompt, userMessage);
            recordAiResponseTokens(usageReservation, response);
        } catch (Exception e) {
            rollbackAiUsage(usageReservation);
            throw e;
        }
        return response;
    }
    
    /**
     * 检查AI调用配额是否已用尽
     */
    private void checkQuota() {
        long limit = licenseLimitService.getAiDailyQuestionLimit();
        if (limit < 0) return; // 负数表示不限制
        String today = LocalDate.now().toString();
        AtomicIntegerArray counts = dailyUsage.get(buildAiUsageKey(today));
        int currentCount = counts != null ? counts.get(0) : 0;
        if (currentCount >= limit) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "今日AI助手提问次数已达上限（" + limit + "次）。请明天再试，或放入有效密钥文件解锁限制。");
        }
    }

    /**
     * 构建SQL生成的系统提示词
     */
    private String buildSqlSystemPrompt(String tableSchema, String dbType) {
        String dialect = normalizeDbDialect(dbType);
        StringBuilder prompt = new StringBuilder();
        prompt.append("""
            你是%s数据库SQL专家。根据用户描述生成SQL，遵守以下规则：
            1. 仅输出一个最优SQL，放在```sql代码块中
            2. 必须使用%s方言，不能混用其他数据库语法
            3. 大数据量查询加目标数据库支持的分页/限制语法，聚合查询用合适的GROUP BY
            4. 不要使用SELECT *，明确列出需要的字段
            5. 简要说明SQL逻辑（1-2句话即可）
            """.formatted(dialect, dialect));
        
        if (tableSchema != null && !tableSchema.isEmpty()) {
            prompt.append("\n\n可用的表结构信息：\n").append(tableSchema);
        }
        
        return prompt.toString();
    }

    public String resolveDbType(Long dataSourceId, String fallbackDbType) {
        if (fallbackDbType != null && !fallbackDbType.isBlank()) {
            return fallbackDbType;
        }
        if (dataSourceService == null || dataSourceId == null) {
            return fallbackDbType;
        }
        try {
            DataSource ds = dataSourceService.getDataSourceById(dataSourceId);
            if (ds != null && ds.getDbType() != null && !ds.getDbType().isBlank()) {
                return ds.getDbType();
            }
        } catch (Exception e) {
            log.debug("解析AI SQL数据库类型失败: dataSourceId={}, error={}", dataSourceId, e.getMessage());
        }
        return fallbackDbType;
    }

    public String normalizeDbDialect(String dbType) {
        if (dbType == null || dbType.isBlank()) {
            return "MySQL";
        }
        String normalized = dbType.trim().toLowerCase(Locale.ROOT).replace("_", "").replace("-", "");
        return switch (normalized) {
            case "mysql", "mariadb" -> "MySQL";
            case "postgresql", "postgres", "pgsql" -> "PostgreSQL";
            case "oracle", "oracle11g", "oracle12c" -> "Oracle";
            case "sqlserver", "mssql", "microsoftsqlserver" -> "SQL Server";
            case "clickhouse" -> "ClickHouse";
            case "dm", "dameng" -> "达梦数据库";
            case "kingbase" -> "人大金仓 Kingbase";
            default -> dbType.trim();
        };
    }

    /** 获取当前AI配置状态（委托 AiConfigManager） */
    public Map<String, Object> getStatus() { return configManager.getStatus(); }
    
    /**
     * 构建AI系统提示词 - 深度集成DataTeaCup平台
     */
    private String buildSystemPrompt(String context) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("""
            你是DataTeaCup的AI助手，擅长SQL编写与优化、数据分析洞察、图表与报表设计、ETL数据同步。
            
            【关键行为准则】
            - 上下文中 ds.schema 包含用户数据库的真实表结构（表名、字段名、字段类型、注释等），你必须直接使用这些信息回答问题。
            - 当用户询问"有哪些表""字段是什么""表结构"等问题时，直接从上下文中的 schema 数据整理并回答，**绝对不要生成查询 information_schema 的 SQL**。
            - 只有当用户明确要求"帮我写一条查 information_schema 的 SQL"时才生成此类查询。
            - 生成 SQL 时必须使用上下文中提供的真实表名和字段名，不得猜测。
            - 生成 SQL 时必须读取上下文中的 ds.dbType 或 dataSources 里的数据库类型，并严格使用对应数据库方言；不要把 MySQL、PostgreSQL、Oracle、SQL Server 等语法混用。
            - 分页/限制、日期函数、字符串函数、时间截断、类型转换必须使用目标数据库支持的写法。
            
            【输出规范】
            1. 用中文简洁回答，优先给出可执行的方案
            2. SQL必须放在```sql代码块中，确保语法正确可直接运行
            3. 生成SQL时仅给出一个最优方案，不要列举多种备选
            4. 涉及DELETE/UPDATE/DROP等危险操作时，先确认再执行
            5. 上下文中没有的表或字段信息时主动追问，不要猜测
            6. 给出报表/图表建议时，说明推荐的维度、指标和图表类型
            """);
        
        // 添加用户提供的上下文
        if (context != null && !context.isEmpty()) {
            prompt.append("\n\n上下文：\n").append(context);
        }
        
        return prompt.toString();
    }

    private Map<String, Object> successResult(String content, String type) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("type", type);
        result.put("content", content);
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }

    private Map<String, Object> errorResult(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("error", message);
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }
    
    /**
     * 统一处理AI调用异常，将Spring HTTP异常分类为用户友好的错误消息
     */
    private Map<String, Object> handleAiException(Exception e, String operation) {
        if (e instanceof BusinessException be) {
            log.warn("{}业务限制: {}", operation, be.getMessage());
            return errorResult(be.getMessage());
        }
        if (e instanceof ResourceAccessException) {
            log.error("{}连接失败: {}", operation, e.getMessage());
            String msg = e.getMessage();
            if (msg != null && msg.contains("Connection refused")) {
                return errorResult("无法连接到AI服务，请检查服务地址是否正确或服务是否已启动");
            } else if (msg != null && (msg.contains("timed out") || msg.contains("timeout"))) {
                return errorResult("AI服务响应超时，请稍后重试或增加超时时间");
            } else if (msg != null && msg.contains("UnknownHostException")) {
                return errorResult("无法解析AI服务地址，请检查API地址配置");
            }
            return errorResult("AI服务连接失败");
        } else if (e instanceof HttpClientErrorException hce) {
            log.error("AI API错误: {} - {}", hce.getStatusCode(), hce.getResponseBodyAsString());
            return switch (hce.getStatusCode().value()) {
                case 401 -> errorResult("API密钥无效或已过期，请检查配置");
                case 429 -> errorResult("请求过于频繁，请稍后重试");
                case 400 -> errorResult("请求参数错误: " + hce.getResponseBodyAsString());
                default -> errorResult("AI API错误 (" + hce.getStatusCode().value() + ")");
            };
        } else if (e instanceof HttpServerErrorException hse) {
            log.error("AI服务端错误: {} - {}", hse.getStatusCode(), hse.getMessage());
            return errorResult("AI服务暂时不可用 (" + hse.getStatusCode().value() + ")，请稍后重试");
        } else {
            log.error("{}失败: {}", operation, e.getMessage(), e);
            return errorResult(operation + "失败: " + e.getMessage());
        }
    }
    
    /** 获取当前AI配置（委托 AiConfigManager） */
    public Map<String, Object> getConfig() { return configManager.getConfig(); }
    
    /** 保存AI配置（委托 AiConfigManager） */
    public void saveConfig(Map<String, Object> config) { configManager.saveConfig(config); }
    
    /** 测试AI连接（委托 AiConfigManager） */
    public Map<String, Object> testConnection(Map<String, Object> config) { return configManager.testConnection(config); }
    
    /**
     * 获取AI系统上下文（数据源、表结构信息）
     */
    public Map<String, Object> getSystemContext(Long dataSourceId) {
        Map<String, Object> context = new HashMap<>();
        context.put("timestamp", Instant.now());
        long deadline = System.currentTimeMillis() + 10_000; // 10秒时间预算
        
        try {
            // 获取所有数据源
            List<DataSource> dataSources = dataSourceService.getDataSourceList(1, 100, null);
            List<Map<String, Object>> dsInfoList = new ArrayList<>();
            
            for (DataSource ds : dataSources) {
                Map<String, Object> dsInfo = new HashMap<>();
                dsInfo.put("id", ds.getId());
                dsInfo.put("name", ds.getName());
                dsInfo.put("dbType", ds.getDbType());
                dsInfo.put("database", ds.getDatabase());
                dsInfo.put("host", ds.getHost());
                
                // 如果指定了数据源ID或者数据源数量较少，获取表信息
                if (dataSourceId != null && dataSourceId.equals(ds.getId())) {
                    try {
                        List<Map<String, Object>> tables = tableDataService.getTables(ds.getId());
                        List<Map<String, Object>> tableInfoList = new ArrayList<>();
                        
                        // 返回所有表，前20个表加载字段结构，其余只返回名称
                        int structureLoaded = 0;
                        for (int i = 0; i < tables.size(); i++) {
                            Map<String, Object> table = tables.get(i);
                            String tableName = (String) table.get("tableName");
                            
                            Map<String, Object> tableInfo = new HashMap<>();
                            tableInfo.put("tableName", tableName);
                            tableInfo.put("remarks", table.get("remarks"));
                            
                            // 前20个表获取字段结构，超时则停止加载更多结构
                            if (structureLoaded < 20 && System.currentTimeMillis() < deadline) {
                                try {
                                    List<Map<String, Object>> columns = tableDataService.getTableStructure(ds.getId(), tableName);
                                    tableInfo.put("columns", columns);
                                    structureLoaded++;
                                } catch (Exception e) {
                                    log.warn("获取表{}结构失败: {}", tableName, e.getMessage());
                                }
                            }
                            
                            tableInfoList.add(tableInfo);
                        }
                        dsInfo.put("tables", tableInfoList);
                        dsInfo.put("totalTables", tables.size());
                        dsInfo.put("structureLoaded", structureLoaded);
                    } catch (Exception e) {
                        log.warn("获取数据源{}表列表失败: {}", ds.getName(), e.getMessage());
                    }
                } else if (dataSourceId == null && dataSources.size() <= 3) {
                    // 数据源较少时，获取表列表（不获取结构）
                    try {
                        List<Map<String, Object>> tables = tableDataService.getTables(ds.getId());
                        dsInfo.put("tableNames", tables.stream()
                            .map(t -> t.get("tableName"))
                            .limit(30)
                            .toList());
                        dsInfo.put("totalTables", tables.size());
                    } catch (Exception e) {
                        log.warn("获取表列表失败: {}", e.getMessage());
                    }
                }
                
                dsInfoList.add(dsInfo);
            }
            
            context.put("dataSources", dsInfoList);
            context.put("totalDataSources", dataSources.size());
            context.put("success", true);
            
        } catch (Exception e) {
            log.error("获取系统上下文失败: {}", e.getMessage());
            context.put("success", false);
            context.put("error", e.getMessage());
        }
        
        return context;
    }
    
    /**
     * 执行SQL查询（仅支持SELECT，安全限制）
     */
    public Map<String, Object> executeSql(Long dataSourceId, String sql, Integer limit) {
        // 去掉SQL注释（单行注释和多行注释）
        String cleanSql = sql.replaceAll("--[^\r\n]*", "")  // 去掉单行注释
                             .replaceAll("/\\*[\\s\\S]*?\\*/", "")  // 去掉多行注释
                             .trim();
        
        // 安全校验
        String validationError = validateSelectSql(cleanSql);
        if (validationError != null) {
            return errorResult(validationError);
        }
        
        try {
            // 添加LIMIT限制（使用清理后的SQL）
            String safeSql = cleanSql.replaceAll(";\\s*$", "");
            
            // 🔧 移除未替换的参数占位符条件（AI生成的可选参数模式）
            safeSql = removeUnreplacedParams(safeSql);
            
            if (!cleanSql.toUpperCase().contains("LIMIT")) {
                safeSql = safeSql + " LIMIT " + limit;
            }
            
            // 记录执行开始时间
            long startTime = System.currentTimeMillis();
            
            Map<String, Object> queryResult = tableDataService.executeSql(dataSourceId, safeSql);
            
            // 计算执行时间
            long executionTime = System.currentTimeMillis() - startTime;
            
            if (Boolean.TRUE.equals(queryResult.get("success"))) {
                Map<String, Object> result = successResult(null, "query");
                result.put("data", queryResult.get("data"));
                result.put("columns", queryResult.get("columns"));
                result.put("rowCount", queryResult.get("data") != null ? ((List<?>) queryResult.get("data")).size() : 0);
                result.put("sql", safeSql);
                result.put("executionTime", executionTime);
                log.info("SQL执行成功，耗时: {}ms", executionTime);
                return result;
            } else {
                Map<String, Object> result = errorResult(String.valueOf(queryResult.get("message")));
                result.put("executionTime", executionTime);
                return result;
            }
        } catch (Exception e) {
            log.error("执行SQL失败: {}", e.getMessage());
            return errorResult("执行SQL失败: " + e.getMessage());
        }
    }
    
    /**
     * 校验SQL安全性：仅允许SELECT，禁止危险关键字和多语句
     * @return 错误消息，null表示通过
     */
    private String validateSelectSql(String cleanSql) {
        String upperSql = cleanSql.toUpperCase();
        if (!upperSql.startsWith("SELECT")) {
            return "安全限制：仅支持SELECT查询语句";
        }
        
        // 去掉字符串内容，避免字符串中的关键字被误判
        String sqlWithoutStrings = upperSql.replaceAll("'[^']*'", "''").replaceAll("\"[^\"]*\"", "\"\"");
        
        for (Map.Entry<String, Pattern> entry : DANGEROUS_SQL_PATTERNS.entrySet()) {
            if (entry.getValue().matcher(sqlWithoutStrings).find()) {
                return "安全限制：禁止执行危险操作（" + entry.getKey() + "）";
            }
        }
        
        // 检查多语句执行（防止SQL注入通过分号执行多条语句）
        String sqlForCheck = cleanSql.replaceAll(";\\s*$", "");
        String sqlForMultiCheck = sqlForCheck.replaceAll("'[^']*'", "").replaceAll("\"[^\"]*\"", "");
        if (sqlForMultiCheck.contains(";")) {
            return "安全限制：不允许执行多条SQL语句";
        }
        
        return null;
    }
    
    /**
     * 校验表名安全性：仅允许字母、数字、下划线、点号
     */
    private boolean isValidTableName(String tableName) {
        return tableName != null && VALID_TABLE_NAME.matcher(tableName).matches();
    }
    
    /**
     * 获取系统概览信息
     */
    public Map<String, Object> getSystemOverview() {
        Map<String, Object> overview = new HashMap<>();
        
        try {
            // 数据源统计
            List<DataSource> dataSources = dataSourceService.getDataSourceList(1, 100, null);
            overview.put("dataSourceCount", dataSources.size());
            
            // 按类型统计数据源
            Map<String, Long> dbTypeCount = dataSources.stream()
                .collect(Collectors.groupingBy(
                    DataSource::getDbType, 
                    Collectors.counting()
                ));
            overview.put("dataSourcesByType", dbTypeCount);
            
            // 数据源详情
            List<Map<String, Object>> dsDetails = dataSources.stream().map(ds -> {
                Map<String, Object> info = new HashMap<>();
                info.put("id", ds.getId());
                info.put("name", ds.getName());
                info.put("dbType", ds.getDbType());
                info.put("host", ds.getHost());
                info.put("database", ds.getDatabase());
                return info;
            }).collect(Collectors.toList());
            overview.put("dataSources", dsDetails);
            
            // ETL任务统计
            if (dataxJobMapper != null) {
                try {
                    long jobCount = dataxJobMapper.count(null, null);
                    overview.put("etlJobCount", jobCount);
                } catch (Exception e) {
                    overview.put("etlJobCount", 0);
                }
            }
            
            overview.put("success", true);
        } catch (Exception e) {
            log.error("获取系统概览失败: {}", e.getMessage());
            overview.put("success", false);
            overview.put("error", e.getMessage());
        }
        
        return overview;
    }
    
    /**
     * AI创建报表
     */
    public Map<String, Object> createReport(String reportName, String description, Long dataSourceId, String sql, Long userId) {
        return createReport(reportName, description, dataSourceId, sql, null, userId);
    }
    
    /**
     * AI创建报表（支持查询参数）
     */
    public Map<String, Object> createReport(String reportName, String description, Long dataSourceId, String sql,
            List<Map<String, Object>> queryParams, Long userId) {
        return createReport(reportName, description, dataSourceId, sql, queryParams, true, true, userId);
    }
    
    /**
     * AI创建报表（支持查询参数和导出选项）
     */
    public Map<String, Object> createReport(String reportName, String description, Long dataSourceId, String sql,
            List<Map<String, Object>> queryParams, Boolean allowExportExcel, Boolean allowExportPdf, Long userId) {
        return createReport(reportName, description, dataSourceId, sql, queryParams, allowExportExcel, allowExportPdf, null, userId);
    }
    
    /**
     * AI创建报表（支持查询参数、导出选项和PDF水印）
     */
    public Map<String, Object> createReport(String reportName, String description, Long dataSourceId, String sql,
            List<Map<String, Object>> queryParams, Boolean allowExportExcel, Boolean allowExportPdf, String pdfWatermark, Long userId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 生成唯一的报表编码
            String reportCode = "ai_report_" + System.currentTimeMillis();
            
            // 创建报表DTO
            ReportDefinitionCreateDTO dto = new ReportDefinitionCreateDTO();
            dto.setReportName(reportName);
            dto.setReportCode(reportCode);
            dto.setDataSourceId(dataSourceId);
            dto.setSqlContent(sql);
            dto.setDescription(description != null ? description : "由AI助手自动创建");
            dto.setStatus(1);
            dto.setAllowExportExcel(allowExportExcel != null ? allowExportExcel : true);
            dto.setAllowExportPdf(allowExportPdf != null ? allowExportPdf : true);
            dto.setPdfWatermark(pdfWatermark);
            
            // 设置查询参数配置（转换为DynamicReport兼容的格式）
            if (queryParams != null && !queryParams.isEmpty()) {
                try {
                    // 转换参数格式：type -> inputType
                    List<Map<String, Object>> convertedParams = new ArrayList<>();
                    for (Map<String, Object> param : queryParams) {
                        Map<String, Object> converted = new HashMap<>(param);
                        // 将type转换为inputType
                        String type = (String) param.get("type");
                        if (type != null) {
                            converted.put("inputType", convertParamTypeToInputType(type));
                        }
                        convertedParams.add(converted);
                    }
                    String queryParamsJson = objectMapper.writeValueAsString(convertedParams);
                    dto.setParams(queryParamsJson);
                } catch (Exception e) {
                    log.warn("序列化查询参数失败: {}", e.getMessage());
                }
            }
            
            // 创建报表
            Long reportId = reportDefinitionService.createReportDefinition(dto, userId != null ? userId : 1L);
            
            result.put("success", true);
            result.put("reportId", reportId);
            result.put("reportCode", reportCode);
            result.put("message", "报表创建成功：" + reportName);
        } catch (Exception e) {
            log.error("AI创建报表失败: {}", e.getMessage());
            return errorResult("AI创建报表失败: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * AI创建菜单
     */
    public Map<String, Object> createMenu(String menuName, Long parentId, String icon, Long reportId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 生成唯一的菜单编码
            String menuCode = "ai_menu_" + System.currentTimeMillis();
            
            // 设置参数
            Long actualParentId = parentId != null ? parentId : 0L;
            String actualIcon = icon != null ? icon : "DocumentOutline";
            String routePath = null;
            String componentPath = null;
            
            // 如果关联报表，设置路由到报表页面
            if (reportId != null) {
                routePath = "/report-view/" + reportId;
                componentPath = "views/DynamicReport";
            }
            
            // 创建菜单
            Long menuId = menuProvider.createMenu(menuName, menuCode, actualParentId, "menu",
                    actualIcon, 1, 99, routePath, componentPath, reportId,
                    null, null, null, "tab", null);
            
            result.put("success", true);
            result.put("menuId", menuId);
            result.put("menuCode", menuCode);
            result.put("message", "菜单创建成功：" + menuName);
        } catch (Exception e) {
            log.error("AI创建菜单失败: {}", e.getMessage());
            return errorResult("AI创建菜单失败: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * AI一键创建报表和菜单（支持查询参数）
     */
    public Map<String, Object> createReportWithMenu(String reportName, String description, 
            Long dataSourceId, String sql, Long parentMenuId, String icon,
            List<Map<String, Object>> queryParams, Long userId) {
        return createReportWithMenu(reportName, description, dataSourceId, sql, parentMenuId, icon, queryParams, true, true, userId);
    }
    
    /**
     * AI一键创建报表和菜单（支持查询参数和导出选项）
     */
    public Map<String, Object> createReportWithMenu(String reportName, String description, 
            Long dataSourceId, String sql, Long parentMenuId, String icon,
            List<Map<String, Object>> queryParams,
            Boolean allowExportExcel, Boolean allowExportPdf, Long userId) {
        return createReportWithMenu(reportName, description, dataSourceId, sql, parentMenuId, icon, queryParams, allowExportExcel, allowExportPdf, null, userId);
    }
    
    /**
     * AI一键创建报表和菜单（支持查询参数、导出选项和PDF水印）
     */
    public Map<String, Object> createReportWithMenu(String reportName, String description, 
            Long dataSourceId, String sql, Long parentMenuId, String icon,
            List<Map<String, Object>> queryParams,
            Boolean allowExportExcel, Boolean allowExportPdf, String pdfWatermark, Long userId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 1. 先创建报表（带查询参数、导出选项和PDF水印）
            Map<String, Object> reportResult = createReport(reportName, description, dataSourceId, sql, queryParams, allowExportExcel, allowExportPdf, pdfWatermark, userId);
            if (!Boolean.TRUE.equals(reportResult.get("success"))) {
                return errorResult("创建报表失败：" + reportResult.get("error"));
            }
            
            Long reportId = (Long) reportResult.get("reportId");
            
            // 2. 创建关联菜单
            Map<String, Object> menuResult = createMenu(reportName, parentMenuId, icon, reportId);
            if (!Boolean.TRUE.equals(menuResult.get("success"))) {
                return errorResult("创建菜单失败：" + menuResult.get("error"));
            }
            
            result.put("success", true);
            result.put("reportId", reportId);
            result.put("reportCode", reportResult.get("reportCode"));
            result.put("menuId", menuResult.get("menuId"));
            result.put("menuCode", menuResult.get("menuCode"));
            result.put("message", "报表和菜单创建成功！刷新页面后可在菜单中看到：" + reportName);
        } catch (Exception e) {
            log.error("AI创建报表和菜单失败: {}", e.getMessage());
            return errorResult("AI创建报表和菜单失败: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * AI创建ETL数据同步任务
     */
    public Map<String, Object> createEtlJob(String jobName, String jobDesc, 
            Long sourceDataSourceId, String sourceTable, 
            Long targetDataSourceId, String targetTable, String writeMode, Long userId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            DataxJob job = new DataxJob();
            job.setJobName(jobName);
            job.setJobDesc(jobDesc != null ? jobDesc : "由AI助手自动创建");
            job.setJobType(1); // 数据库同步
            job.setSourceDataSourceId(sourceDataSourceId);
            job.setSourceTable(sourceTable);
            job.setTargetDataSourceId(targetDataSourceId);
            job.setTargetTable(targetTable);
            job.setWriteMode(writeMode != null ? writeMode : "insert");
            job.setJobStatus(0); // 默认停止状态
            job.setIncrementType(0); // 默认全量
            job.setChannelCount(1);
            job.setCreateBy(userId != null ? userId : 1L);
            
            Long jobId = dataxJobService.createJob(job);
            
            result.put("success", true);
            result.put("jobId", jobId);
            result.put("message", "ETL任务创建成功：" + jobName);
        } catch (Exception e) {
            log.error("AI创建ETL任务失败: {}", e.getMessage());
            return errorResult("AI创建ETL任务失败: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 获取系统诊断信息
     */
    public Map<String, Object> getSystemDiagnosis() {
        Map<String, Object> diagnosis = new HashMap<>();
        
        try {
            // 数据源连接状态
            List<DataSource> dataSources = dataSourceService.getDataSourceList(1, 100, null);
            List<Map<String, Object>> dsStatus = new ArrayList<>();
            int healthyCount = 0;
            int unhealthyCount = 0;
            
            long diagDeadline = System.currentTimeMillis() + 30_000; // 30秒总时间预算
            for (DataSource ds : dataSources) {
                Map<String, Object> status = new HashMap<>();
                status.put("id", ds.getId());
                status.put("name", ds.getName());
                status.put("dbType", ds.getDbType());
                
                // 超时保护：超过预算则跳过剩余连接测试
                if (System.currentTimeMillis() > diagDeadline) {
                    status.put("status", "skipped");
                    status.put("message", "跳过（诊断超时）");
                    dsStatus.add(status);
                    continue;
                }
                
                // 测试连接
                try {
                    dataSourceService.testConnection(ds);
                    status.put("status", "healthy");
                    status.put("message", "连接正常");
                    healthyCount++;
                } catch (Exception e) {
                    status.put("status", "unhealthy");
                    status.put("message", e.getMessage());
                    unhealthyCount++;
                }
                dsStatus.add(status);
            }
            
            diagnosis.put("dataSources", dsStatus);
            diagnosis.put("dataSourceSummary", Map.of(
                "total", dataSources.size(),
                "healthy", healthyCount,
                "unhealthy", unhealthyCount
            ));
            
            // ETL任务状态
            if (dataxJobMapper != null) {
                try {
                    long totalJobs = dataxJobMapper.count(null, null);
                    long runningJobs = dataxJobMapper.count(null, 1);
                    diagnosis.put("etlJobSummary", Map.of(
                        "total", totalJobs,
                        "running", runningJobs,
                        "stopped", totalJobs - runningJobs
                    ));
                } catch (Exception e) {
                    log.warn("获取ETL任务统计失败: {}", e.getMessage());
                }
            }
            
            // 系统信息
            Runtime runtime = Runtime.getRuntime();
            diagnosis.put("systemInfo", Map.of(
                "javaVersion", System.getProperty("java.version"),
                "maxMemory", runtime.maxMemory() / 1024 / 1024 + "MB",
                "totalMemory", runtime.totalMemory() / 1024 / 1024 + "MB",
                "freeMemory", runtime.freeMemory() / 1024 / 1024 + "MB",
                "processors", runtime.availableProcessors()
            ));
            
            // 诊断建议
            List<String> suggestions = new ArrayList<>();
            if (unhealthyCount > 0) {
                suggestions.add("有 " + unhealthyCount + " 个数据源连接异常，请检查网络和配置");
            }
            if (dataSources.isEmpty()) {
                suggestions.add("系统尚未配置数据源，请先添加数据源");
            }
            diagnosis.put("suggestions", suggestions);
            
            diagnosis.put("success", true);
        } catch (Exception e) {
            log.error("获取系统诊断信息失败: {}", e.getMessage());
            diagnosis.put("success", false);
            diagnosis.put("error", e.getMessage());
        }
        
        return diagnosis;
    }
    
    /**
     * 获取报表列表
     */
    public Map<String, Object> getReportsList() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<ReportDefinition> reports = 
                reportDefinitionService.getReportDefinitionList(1, 50, null);
            
            List<Map<String, Object>> reportList = reports.stream().map(r -> {
                Map<String, Object> info = new HashMap<>();
                info.put("id", r.getId());
                info.put("name", r.getReportName());
                info.put("code", r.getReportCode());
                info.put("description", r.getDescription());
                info.put("dataSourceId", r.getDataSourceId());
                info.put("status", r.getStatus());
                return info;
            }).collect(Collectors.toList());
            
            result.put("success", true);
            result.put("reports", reportList);
            result.put("total", reports.size());
        } catch (Exception e) {
            log.error("获取报表列表失败: {}", e.getMessage());
            return errorResult("获取报表列表失败: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 生成数据字典
     */
    public Map<String, Object> generateDataDictionary(Long dataSourceId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<Map<String, Object>> tables = tableDataService.getTables(dataSourceId);
            List<Map<String, Object>> dictionary = new ArrayList<>();
            long deadline = System.currentTimeMillis() + 15_000; // 15秒时间预算
            int maxTables = 200; // 最多处理200张表的结构
            int structureLoaded = 0;
            
            for (Map<String, Object> table : tables) {
                String tableName = (String) table.get("tableName");
                Map<String, Object> tableDict = new HashMap<>();
                tableDict.put("tableName", tableName);
                tableDict.put("remarks", table.get("remarks"));
                
                if (structureLoaded < maxTables && System.currentTimeMillis() < deadline) {
                    try {
                        List<Map<String, Object>> columns = tableDataService.getTableStructure(dataSourceId, tableName);
                        tableDict.put("columns", columns);
                        tableDict.put("columnCount", columns.size());
                        structureLoaded++;
                    } catch (Exception e) {
                        tableDict.put("columns", new ArrayList<>());
                        tableDict.put("error", e.getMessage());
                    }
                } else {
                    tableDict.put("columns", new ArrayList<>());
                    tableDict.put("skipped", true);
                }
                
                dictionary.add(tableDict);
            }
            
            result.put("success", true);
            result.put("dataSourceId", dataSourceId);
            result.put("tables", dictionary);
            result.put("tableCount", dictionary.size());
            result.put("structureLoaded", structureLoaded);
            if (structureLoaded < tables.size()) {
                result.put("warning", "共" + tables.size() + "张表，仅加载了" + structureLoaded + "张表的结构（受时间或数量限制）");
            }
        } catch (Exception e) {
            log.error("生成数据字典失败: {}", e.getMessage());
            return errorResult("生成数据字典失败: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 推荐可视化方案
     */
    public Map<String, Object> recommendChart(Long dataSourceId, String sql) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 通过 executeSql 的安全检查执行SQL
            Map<String, Object> queryResult = executeSql(dataSourceId, sql, 10);
            
            if (Boolean.TRUE.equals(queryResult.get("success"))) {
                List<String> columns = (List<String>) queryResult.get("columns");
                List<Map<String, Object>> data = (List<Map<String, Object>>) queryResult.get("data");
                
                // 智能分析列的数据类型
                List<Map<String, Object>> recommendations = analyzeAndRecommendCharts(columns, data);
                
                result.put("success", true);
                result.put("columns", columns);
                result.put("sampleData", data);
                result.put("recommendations", recommendations);
            } else {
                return errorResult(String.valueOf(queryResult.get("error")));
            }
        } catch (Exception e) {
            log.error("推荐可视化方案失败: {}", e.getMessage());
            return errorResult("推荐可视化方案失败: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 智能分析列数据类型并推荐图表
     * 根据实际数据内容判断：时间列→折线图，分类+数值→柱状图/饼图，双数值→散点图
     */
    private List<Map<String, Object>> analyzeAndRecommendCharts(List<String> columns, List<Map<String, Object>> data) {
        List<Map<String, Object>> recommendations = new ArrayList<>();
        if (columns == null || columns.size() < 2 || data == null || data.isEmpty()) {
            return recommendations;
        }
        
        // 分析每列的数据类型
        List<String> numericCols = new ArrayList<>();
        List<String> timeCols = new ArrayList<>();
        List<String> categoryCols = new ArrayList<>();
        
        for (String col : columns) {
            boolean isNumeric = true;
            boolean isTime = false;
            
            for (Map<String, Object> row : data) {
                Object val = row.get(col);
                if (val == null) continue;
                String strVal = val.toString();
                
                // 检测时间类型
                if (val instanceof java.util.Date || val instanceof java.time.temporal.Temporal
                        || strVal.matches("\\d{4}[-/]\\d{2}[-/]\\d{2}.*")
                        || strVal.matches("\\d{4}[-/]\\d{2}")) {
                    isTime = true;
                    isNumeric = false;
                    break;
                }
                
                // 检测数值类型
                if (!(val instanceof Number)) {
                    try {
                        Double.parseDouble(strVal);
                    } catch (NumberFormatException e) {
                        isNumeric = false;
                    }
                }
            }
            
            if (isTime) {
                timeCols.add(col);
            } else if (isNumeric) {
                numericCols.add(col);
            } else {
                categoryCols.add(col);
            }
        }
        
        // 根据数据类型组合推荐图表
        String firstCategory = categoryCols.isEmpty() ? null : categoryCols.get(0);
        String firstNumeric = numericCols.isEmpty() ? null : numericCols.get(0);
        String firstTime = timeCols.isEmpty() ? null : timeCols.get(0);
        
        // 有时间列 + 数值列 → 折线图（首选）、面积图
        if (firstTime != null && firstNumeric != null) {
            Map<String, Object> line = new HashMap<>();
            line.put("type", "line");
            line.put("name", "折线图");
            line.put("description", "适合展示随时间变化的趋势");
            line.put("xAxis", firstTime);
            line.put("yAxis", firstNumeric);
            line.put("priority", 1);
            recommendations.add(line);
            
            Map<String, Object> area = new HashMap<>();
            area.put("type", "area");
            area.put("name", "面积图");
            area.put("description", "适合展示趋势和累积量");
            area.put("xAxis", firstTime);
            area.put("yAxis", firstNumeric);
            area.put("priority", 3);
            recommendations.add(area);
        }
        
        // 有分类列 + 数值列 → 柱状图、饼图
        if (firstCategory != null && firstNumeric != null) {
            int rowCount = data.size();
            
            Map<String, Object> bar = new HashMap<>();
            bar.put("type", "bar");
            bar.put("name", "柱状图");
            bar.put("description", "适合展示分类数据对比");
            bar.put("xAxis", firstCategory);
            bar.put("yAxis", firstNumeric);
            bar.put("priority", rowCount > 20 ? 3 : 1);
            recommendations.add(bar);
            
            // 饼图仅在分类数少于15时推荐
            if (rowCount <= 15) {
                Map<String, Object> pie = new HashMap<>();
                pie.put("type", "pie");
                pie.put("name", "饼图");
                pie.put("description", "适合展示占比分布（分类数≤15时效果最佳）");
                pie.put("nameField", firstCategory);
                pie.put("valueField", firstNumeric);
                pie.put("priority", 2);
                recommendations.add(pie);
            }
        }
        
        // 双数值列（无时间无分类）→ 散点图
        if (numericCols.size() >= 2 && timeCols.isEmpty()) {
            Map<String, Object> scatter = new HashMap<>();
            scatter.put("type", "scatter");
            scatter.put("name", "散点图");
            scatter.put("description", "适合展示两个数值变量之间的关系");
            scatter.put("xAxis", numericCols.get(0));
            scatter.put("yAxis", numericCols.get(1));
            scatter.put("priority", 2);
            recommendations.add(scatter);
        }
        
        // 多数值列 + 分类列 → 雷达图（3-8个数值维度时）
        if (firstCategory != null && numericCols.size() >= 3 && numericCols.size() <= 8) {
            Map<String, Object> radar = new HashMap<>();
            radar.put("type", "radar");
            radar.put("name", "雷达图");
            radar.put("description", "适合展示多维度数据对比");
            radar.put("nameField", firstCategory);
            radar.put("dimensions", numericCols);
            radar.put("priority", 4);
            recommendations.add(radar);
        }
        
        // 兜底：至少返回柱状图和折线图
        if (recommendations.isEmpty() && columns.size() >= 2) {
            recommendations.add(Map.of(
                "type", "bar", "name", "柱状图",
                "description", "通用对比展示",
                "xAxis", columns.get(0), "yAxis", columns.get(1), "priority", 1
            ));
            recommendations.add(Map.of(
                "type", "line", "name", "折线图",
                "description", "通用趋势展示",
                "xAxis", columns.get(0), "yAxis", columns.get(1), "priority", 2
            ));
        }
        
        // 按优先级排序
        recommendations.sort((a, b) -> {
            int pa = a.get("priority") instanceof Number ? ((Number) a.get("priority")).intValue() : 99;
            int pb = b.get("priority") instanceof Number ? ((Number) b.get("priority")).intValue() : 99;
            return Integer.compare(pa, pb);
        });
        
        return recommendations;
    }
    
    /**
     * 获取ETL任务列表
     */
    public Map<String, Object> getEtlJobsList() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            PageResult<DataxJob> pageResult = 
                dataxJobService.getJobList(1, 50, null, null);
            
            List<Map<String, Object>> jobList = pageResult.getList().stream().map(j -> {
                Map<String, Object> info = new HashMap<>();
                info.put("id", j.getId());
                info.put("name", j.getJobName());
                info.put("description", j.getJobDesc());
                info.put("sourceTable", j.getSourceTable());
                info.put("targetTable", j.getTargetTable());
                info.put("status", j.getJobStatus());
                info.put("writeMode", j.getWriteMode());
                return info;
            }).collect(Collectors.toList());
            
            result.put("success", true);
            result.put("jobs", jobList);
            result.put("total", pageResult.getTotal());
        } catch (Exception e) {
            log.error("获取ETL任务列表失败: {}", e.getMessage());
            return errorResult("获取ETL任务列表失败: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 保存对话历史
     */
    public void saveChatMessage(String sessionId, Long userId, String role, String content, 
                                Long dataSourceId, String messageType) {
        if (aiChatHistoryMapper == null) {
            log.warn("AI对话历史Mapper未注入，跳过保存");
            return;
        }
        
        try {
            AiChatHistory history = new AiChatHistory();
            history.setSessionId(sessionId);
            history.setUserId(userId);
            history.setRole(role);
            history.setContent(content);
            history.setDataSourceId(dataSourceId);
            history.setMessageType(messageType != null ? messageType : "text");
            history.setCreateTime(LocalDateTime.now());
            
            aiChatHistoryMapper.insert(history);
        } catch (Exception e) {
            log.error("保存对话历史失败: {}", e.getMessage());
        }
    }
    
    /**
     * 获取对话历史
     */
    public Map<String, Object> getChatHistory(String sessionId, Integer limit) {
        Map<String, Object> result = new HashMap<>();
        
        if (aiChatHistoryMapper == null) {
            return errorResult("对话历史功能未启用");
        }
        
        try {
            List<AiChatHistory> messages = 
                aiChatHistoryMapper.getSessionMessages(sessionId);
            
            List<Map<String, Object>> messageList = messages.stream().map(m -> {
                Map<String, Object> msg = new HashMap<>();
                msg.put("role", m.getRole());
                msg.put("content", m.getContent());
                msg.put("messageType", m.getMessageType());
                msg.put("createTime", m.getCreateTime());
                return msg;
            }).collect(Collectors.toList());
            
            result.put("success", true);
            result.put("messages", messageList);
            result.put("sessionId", sessionId);
        } catch (Exception e) {
            log.error("获取对话历史失败: {}", e.getMessage());
            return errorResult("获取对话历史失败: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 获取用户的会话列表
     */
    public List<Map<String, Object>> getUserSessions(Long userId, Integer limit) {
        List<Map<String, Object>> sessions = new ArrayList<>();
        
        if (aiChatHistoryMapper == null) {
            return sessions;
        }
        
        try {
            List<String> sessionIds = aiChatHistoryMapper.getUserSessions(userId, limit);
            
            // 批量获取每个会话的首条消息（单次查询，避免N+1）
            Map<String, AiChatHistory> firstMessages = new HashMap<>();
            if (!sessionIds.isEmpty()) {
                try {
                    List<AiChatHistory> batchResults = aiChatHistoryMapper.getFirstMessagesBySessionIds(sessionIds);
                    for (AiChatHistory msg : batchResults) {
                        firstMessages.put(msg.getSessionId(), msg);
                    }
                } catch (Exception e) {
                    log.warn("批量获取会话首条消息失败，降级为逐个查询: {}", e.getMessage());
                    for (String sid : sessionIds) {
                        try {
                            List<AiChatHistory> msgs = aiChatHistoryMapper.getRecentMessages(sid, 1);
                            if (!msgs.isEmpty()) firstMessages.put(sid, msgs.get(0));
                        } catch (Exception ex) {
                            log.debug("获取会话{}首条消息失败: {}", sid, ex.getMessage());
                        }
                    }
                }
            }
            
            for (String sessionId : sessionIds) {
                Map<String, Object> session = new HashMap<>();
                session.put("sessionId", sessionId);
                
                AiChatHistory firstMsg = firstMessages.get(sessionId);
                if (firstMsg != null) {
                    String title = firstMsg.getContent();
                    if (title != null && title.length() > 30) {
                        title = title.substring(0, 30) + "...";
                    }
                    session.put("title", title);
                    session.put("lastTime", firstMsg.getCreateTime());
                } else {
                    session.put("title", "新对话");
                    session.put("lastTime", null);
                }
                
                sessions.add(session);
            }
        } catch (Exception e) {
            log.error("获取用户会话列表失败: {}", e.getMessage());
        }
        
        return sessions;
    }
    
    /**
     * 数据质量分析
     */
    public Map<String, Object> analyzeDataQuality(Long dataSourceId, String tableName) {
        Map<String, Object> result = new HashMap<>();
        
        if (!isValidTableName(tableName)) {
            return errorResult("无效的表名");
        }
        
        try {
            List<Map<String, Object>> columns = tableDataService.getTableStructure(dataSourceId, tableName);
            List<Map<String, Object>> qualityIssues = new ArrayList<>();
            long qualityDeadline = System.currentTimeMillis() + 20_000; // 20秒时间预算
            int skippedColumns = 0;
            
            for (Map<String, Object> column : columns) {
                // 超时保护：宽表时跳过剩余列
                if (System.currentTimeMillis() > qualityDeadline) {
                    skippedColumns++;
                    continue;
                }
                String columnName = (String) column.get("columnName");
                String dataType = (String) column.get("dataType");
                
                Map<String, Object> issue = new HashMap<>();
                issue.put("columnName", columnName);
                issue.put("dataType", dataType);
                
                // 安全校验：列名只允许字母、数字、下划线
                if (columnName == null || !columnName.matches("^[a-zA-Z0-9_]+$")) {
                    issue.put("error", "列名包含不安全字符，跳过检查");
                    qualityIssues.add(issue);
                    continue;
                }
                
                // 判断是否为数值列（合并查询用）
                boolean isNumericType = false;
                if (dataType != null) {
                    String upperType = dataType.toUpperCase();
                    isNumericType = upperType.contains("INT") || upperType.contains("DECIMAL")
                        || upperType.contains("FLOAT") || upperType.contains("DOUBLE") || upperType.contains("NUMERIC");
                }
                
                // 单条SQL同时获取：空值率、唯一值数、数值统计（如果是数值列）
                try {
                    String qualitySql;
                    if (isNumericType) {
                        qualitySql = String.format(
                            "SELECT COUNT(*) as total, SUM(CASE WHEN `%s` IS NULL THEN 1 ELSE 0 END) as null_count, COUNT(DISTINCT `%s`) as distinct_count, MIN(`%s`) as min_val, MAX(`%s`) as max_val, AVG(`%s`) as avg_val FROM `%s`",
                            columnName, columnName, columnName, columnName, columnName, tableName
                        );
                    } else {
                        qualitySql = String.format(
                            "SELECT COUNT(*) as total, SUM(CASE WHEN `%s` IS NULL THEN 1 ELSE 0 END) as null_count, COUNT(DISTINCT `%s`) as distinct_count FROM `%s`",
                            columnName, columnName, tableName
                        );
                    }
                    Map<String, Object> qResult = tableDataService.executeSql(dataSourceId, qualitySql);
                    if (Boolean.TRUE.equals(qResult.get("success"))) {
                        List<Map<String, Object>> data = (List<Map<String, Object>>) qResult.get("data");
                        if (data != null && !data.isEmpty()) {
                            Map<String, Object> row = data.get(0);
                            long total = ((Number) row.get("total")).longValue();
                            long nullCount = ((Number) row.get("null_count")).longValue();
                            long distinctCount = ((Number) row.get("distinct_count")).longValue();
                            double nullRate = total > 0 ? (nullCount * 100.0 / total) : 0;
                            
                            issue.put("nullRate", Math.round(nullRate * 100) / 100.0);
                            issue.put("totalRows", total);
                            issue.put("nullCount", nullCount);
                            issue.put("distinctCount", distinctCount);
                            
                            // 数值统计
                            if (isNumericType) {
                                issue.put("minValue", row.get("min_val"));
                                issue.put("maxValue", row.get("max_val"));
                                issue.put("avgValue", row.get("avg_val"));
                            }
                            
                            // 唯一性分析
                            if (total > 0 && distinctCount == total - nullCount) {
                                issue.put("isUnique", true);
                                issue.put("uniqueHint", "所有非空值唯一，可能是主键或唯一索引列");
                            } else if (total > 0 && distinctCount <= 10 && total > 20) {
                                issue.put("isEnum", true);
                                issue.put("enumHint", "唯一值仅" + distinctCount + "个，可能是枚举/状态字段");
                            }
                            
                            // 严重程度判定
                            List<String> suggestions = new ArrayList<>();
                            String severity = "low";
                            if (nullRate > 50) {
                                severity = "high";
                                suggestions.add("空值率超过50%，建议检查数据采集是否正常");
                            } else if (nullRate > 20) {
                                severity = "medium";
                                suggestions.add("空值率较高，建议设置默认值或进行数据清洗");
                            }
                            if (distinctCount == 1 && total > 1) {
                                severity = severity.equals("low") ? "medium" : severity;
                                suggestions.add("所有值相同，该列可能无实际意义");
                            }
                            issue.put("severity", severity);
                            if (!suggestions.isEmpty()) {
                                issue.put("suggestion", String.join("；", suggestions));
                            }
                        }
                    }
                } catch (Exception e) {
                    issue.put("error", e.getMessage());
                }
                
                qualityIssues.add(issue);
            }
            
            // 计算整体质量分数
            double avgNullRate = qualityIssues.stream()
                .filter(i -> i.get("nullRate") != null)
                .mapToDouble(i -> ((Number) i.get("nullRate")).doubleValue())
                .average()
                .orElse(0);
            
            double qualityScore = Math.max(0, 100 - avgNullRate);
            
            result.put("success", true);
            if (skippedColumns > 0) {
                result.put("skippedColumns", skippedColumns);
                result.put("warning", "共" + columns.size() + "列，因超时跳过了" + skippedColumns + "列的质量分析");
            }
            result.put("tableName", tableName);
            result.put("columnCount", columns.size());
            result.put("qualityScore", Math.round(qualityScore * 100) / 100.0);
            result.put("columns", qualityIssues);
            result.put("summary", generateQualitySummary(qualityScore, qualityIssues));
        } catch (Exception e) {
            log.error("数据质量分析失败: {}", e.getMessage());
            return errorResult("数据质量分析失败: " + e.getMessage());
        }
        
        return result;
    }
    
    private String generateQualitySummary(double score, List<Map<String, Object>> issues) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("## 数据质量评分：%.1f/100\n\n", score));
        
        // 单次遍历统计所有指标
        long highIssues = 0, mediumIssues = 0, uniqueCols = 0, enumCols = 0;
        for (Map<String, Object> issue : issues) {
            if ("high".equals(issue.get("severity"))) highIssues++;
            else if ("medium".equals(issue.get("severity"))) mediumIssues++;
            if (Boolean.TRUE.equals(issue.get("isUnique"))) uniqueCols++;
            if (Boolean.TRUE.equals(issue.get("isEnum"))) enumCols++;
        }
        
        if (highIssues > 0) {
            sb.append(String.format("⚠️ **严重问题**：%d 个字段空值率超过50%%\n", highIssues));
        }
        if (mediumIssues > 0) {
            sb.append(String.format("⚡ **中等问题**：%d 个字段空值率超过20%%\n", mediumIssues));
        }
        if (highIssues == 0 && mediumIssues == 0) {
            sb.append("✅ 数据质量良好，未发现明显问题\n");
        }
        
        sb.append("\n### 字段特征\n");
        if (uniqueCols > 0) {
            sb.append(String.format("🔑 %d 个字段值唯一（潜在主键/唯一索引）\n", uniqueCols));
        }
        if (enumCols > 0) {
            sb.append(String.format("🏷️ %d 个字段为枚举型（唯一值≤10）\n", enumCols));
        }
        
        // 数值统计摘要
        List<String> numericSummaries = new ArrayList<>();
        for (Map<String, Object> issue : issues) {
            if (issue.get("minValue") != null && issue.get("maxValue") != null) {
                numericSummaries.add(String.format("`%s`: 范围[%s ~ %s], 均值=%s",
                    issue.get("columnName"), issue.get("minValue"), issue.get("maxValue"), issue.get("avgValue")));
            }
        }
        if (!numericSummaries.isEmpty()) {
            sb.append("\n### 数值统计\n");
            for (String s : numericSummaries) {
                sb.append("- ").append(s).append("\n");
            }
        }
        
        return sb.toString();
    }
    
    /**
     * AI智能建议
     */
    public Map<String, Object> getSmartSuggestions(Long dataSourceId, String tableName) {
        Map<String, Object> result = new HashMap<>();
        
        if (!isValidTableName(tableName)) {
            return errorResult("无效的表名");
        }
        
        try {
            List<Map<String, Object>> suggestions = new ArrayList<>();
            
            // 获取表结构
            List<Map<String, Object>> columns = tableDataService.getTableStructure(dataSourceId, tableName);
            
            // 建议1：索引优化
            Map<String, Object> indexSuggestion = new HashMap<>();
            indexSuggestion.put("type", "index");
            indexSuggestion.put("title", "索引优化建议");
            
            List<String> potentialIndexColumns = columns.stream()
                .filter(c -> {
                    String name = ((String) c.get("columnName")).toLowerCase();
                    return name.endsWith("_id") || name.equals("id") || 
                           name.contains("code") || name.contains("status");
                })
                .map(c -> (String) c.get("columnName"))
                .collect(Collectors.toList());
            
            if (!potentialIndexColumns.isEmpty()) {
                indexSuggestion.put("columns", potentialIndexColumns);
                indexSuggestion.put("description", "这些字段可能需要添加索引以提升查询性能");
                suggestions.add(indexSuggestion);
            }
            
            // 建议2：报表建议
            Map<String, Object> reportSuggestion = new HashMap<>();
            reportSuggestion.put("type", "report");
            reportSuggestion.put("title", "可生成报表");
            
            List<String> numericColumns = columns.stream()
                .filter(c -> {
                    String type = ((String) c.get("dataType")).toUpperCase();
                    return type.contains("INT") || type.contains("DECIMAL") || 
                           type.contains("FLOAT") || type.contains("DOUBLE");
                })
                .map(c -> (String) c.get("columnName"))
                .collect(Collectors.toList());
            
            if (!numericColumns.isEmpty()) {
                reportSuggestion.put("columns", numericColumns);
                reportSuggestion.put("description", "这些数值字段可用于生成统计报表");
                reportSuggestion.put("sampleSql", String.format(
                    "SELECT COUNT(*) as 总数, SUM(`%s`) as 合计, AVG(`%s`) as 平均值 FROM `%s`",
                    numericColumns.get(0), numericColumns.get(0), tableName
                ));
                suggestions.add(reportSuggestion);
            }
            
            // 建议3：ETL建议
            Map<String, Object> etlSuggestion = new HashMap<>();
            etlSuggestion.put("type", "etl");
            etlSuggestion.put("title", "数据同步建议");
            etlSuggestion.put("description", "可将此表数据同步到数据仓库进行分析");
            suggestions.add(etlSuggestion);
            
            result.put("success", true);
            result.put("tableName", tableName);
            result.put("suggestions", suggestions);
        } catch (Exception e) {
            log.error("获取智能建议失败: {}", e.getMessage());
            return errorResult("获取智能建议失败: " + e.getMessage());
        }
        
        return result;
    }
    
    // 图表相关方法已迁移到 AiChartService
    
    // ============ AI 使用量/配额管理 ============
    
    /** 每日使用量记录（内存版，生产环境建议用数据库/Redis） */
    private final Map<String, AtomicIntegerArray> dailyUsage = new ConcurrentHashMap<>(); // key=date:userId, value=[requestCount, tokenCount]
    private final Object aiUsageLock = new Object();
    
    private record AiUsageReservation(String dateKey, String usageKey, boolean counted) {}
    
    /**
     * 记录一次AI调用
     */
    public void recordAiUsage(String response) {
        consumeAiUsage(response);
    }

    private void consumeAiUsage(String response) {
        AiUsageReservation reservation = reserveAiUsage();
        recordAiResponseTokens(reservation, response);
    }

    private AiUsageReservation reserveAiUsage() {
        String dateKey = LocalDate.now().toString();
        String usageKey = buildAiUsageKey(dateKey);
        long limit = licenseLimitService.getAiDailyQuestionLimit();
        synchronized (aiUsageLock) {
            AtomicIntegerArray counts = dailyUsage.computeIfAbsent(usageKey, k -> new AtomicIntegerArray(2));
            if (limit >= 0 && counts.get(0) >= limit) {
                throw new BusinessException(ErrorCode.PARAM_ERROR,
                        "今日AI助手提问次数已达上限（" + limit + "次）。请明天再试，或放入有效密钥文件解锁限制。");
            }
            counts.incrementAndGet(0);
            cleanupAiUsage();
        }
        return new AiUsageReservation(dateKey, usageKey, true);
    }

    private void recordAiResponseTokens(AiUsageReservation reservation, String response) {
        if (reservation == null || !reservation.counted()) {
            return;
        }
        AtomicIntegerArray counts = dailyUsage.get(reservation.usageKey());
        if (counts != null) {
            counts.addAndGet(1, response != null ? response.length() / 4 : 0); // 粗略估算token
        }
    }

    private void rollbackAiUsage(AiUsageReservation reservation) {
        if (reservation == null || !reservation.counted()) {
            return;
        }
        synchronized (aiUsageLock) {
            AtomicIntegerArray counts = dailyUsage.get(reservation.usageKey());
            if (counts != null && counts.get(0) > 0) {
                counts.decrementAndGet(0);
            }
        }
    }

    private void cleanupAiUsage() {
        if (dailyUsage.size() > 30) {
            String cutoff = LocalDate.now().minusDays(30).toString();
            dailyUsage.entrySet().removeIf(e -> extractUsageDate(e.getKey()).compareTo(cutoff) < 0);
        }
    }
    
    /**
     * 查询当日使用量和配额
     */
    public Map<String, Object> getAiUsage() {
        String today = LocalDate.now().toString();
        AtomicIntegerArray counts = dailyUsage.getOrDefault(buildAiUsageKey(today), new AtomicIntegerArray(2));
        long limit = licenseLimitService.getAiDailyQuestionLimit();
        
        Map<String, Object> result = new HashMap<>();
        result.put("date", today);
        result.put("requestCount", counts.get(0));
        result.put("tokenCount", counts.get(1));
        result.put("dailyLimit", limit);
        result.put("remaining", limit >= 0 ? Math.max(0, limit - counts.get(0)) : -1);
        result.put("usagePercent", limit > 0 ? Math.round(counts.get(0) * 100.0 / limit) : 0);
        result.put("license", licenseLimitService.getStatus());
        return result;
    }
    
    /**
     * 获取最近7天使用统计
     */
    public Map<String, Object> getAiUsageStats() {
        Map<String, Object> result = new HashMap<>();
        LocalDate today = LocalDate.now();
        
        List<Map<String, Object>> daily = new ArrayList<>();
        int totalRequests = 0, totalTokens = 0;
        
        for (int i = 6; i >= 0; i--) {
            String dateKey = today.minusDays(i).toString();
            AtomicIntegerArray counts = dailyUsage.getOrDefault(buildAiUsageKey(dateKey), new AtomicIntegerArray(2));
            
            Map<String, Object> day = new HashMap<>();
            day.put("date", dateKey);
            day.put("requests", counts.get(0));
            day.put("tokens", counts.get(1));
            daily.add(day);
            
            totalRequests += counts.get(0);
            totalTokens += counts.get(1);
        }
        
        result.put("daily", daily);
        result.put("totalRequests", totalRequests);
        result.put("totalTokens", totalTokens);
        result.put("dailyLimit", licenseLimitService.getAiDailyQuestionLimit());
        return result;
    }

    private String buildAiUsageKey(String dateKey) {
        Long userId = SecurityContext.getCurrentUserId();
        return dateKey + ":" + (userId != null ? userId : "anonymous");
    }

    private String extractUsageDate(String usageKey) {
        int split = usageKey.indexOf(':');
        return split > 0 ? usageKey.substring(0, split) : usageKey;
    }
    
    /**
     * 将参数类型转换为DynamicReport兼容的inputType
     * DynamicReport支持: text, number, date, daterange, select, department
     */
    private String convertParamTypeToInputType(String type) {
        if (type == null) return "text";
        return switch (type.toLowerCase()) {
            case "input", "text", "string" -> "text";
            case "number", "int", "integer", "decimal", "float", "double" -> "number";
            case "date", "datetime" -> "date";
            case "daterange", "date_range" -> "daterange";
            case "select", "dropdown", "dynamicselect" -> "select";
            case "department", "dept" -> "department";
            default -> "text";
        };
    }
    
    /**
     * 移除SQL中未替换的参数占位符，防止SQL语法错误
     * 支持多种AI生成的可选参数模式
     */
    private String removeUnreplacedParams(String sql) {
        if (sql == null || !sql.contains("${")) {
            return sql;
        }
        
        String result = sql;
        
        // 🔧 通用方案：移除所有包含 ${...} 的括号条件（使用预编译正则）
        String prev;
        do {
            prev = result;
            result = PARAM_IN_PARENS.matcher(result).replaceAll("");
        } while (!result.equals(prev));
        
        // 移除残留的 AND/OR 后面跟着的 ${...} 条件（使用预编译正则）
        result = PARAM_AND_TAIL.matcher(result).replaceAll("");
        result = PARAM_OR_TAIL.matcher(result).replaceAll("");
        
        // 清理残留语法问题
        result = result.replaceAll("(?i)WHERE\\s+AND\\s+", "WHERE ");
        result = result.replaceAll("(?i)WHERE\\s+OR\\s+", "WHERE ");
        result = result.replaceAll("(?i)WHERE\\s+ORDER", " ORDER");
        result = result.replaceAll("(?i)WHERE\\s+LIMIT", " LIMIT");
        result = result.replaceAll("(?i)WHERE\\s+GROUP", " GROUP");
        result = result.replaceAll("(?i)WHERE\\s*$", "");
        
        // 最终检查：如果还有${...}，记录警告并尝试直接移除
        if (result.contains("${")) {
            log.warn("SQL中仍有未替换的参数占位符，尝试直接移除: {}", result);
            // 最后兜底：直接移除所有 ${...} 占位符
            result = result.replaceAll("\\$\\{[^}]+\\}", "NULL");
        }
        
        return result;
    }
}
