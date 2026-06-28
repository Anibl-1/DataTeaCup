package com.dataplatform.data.service;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * AI对话优化器
 * 使用HanLP中文NLP和自定义算法提供智能对话增强功能
 * 
 * 功能：
 * 1. 意图识别 - 基于关键词匹配和相似度计算
 * 2. 中文分词 - HanLP智能分词
 * 3. 关键词提取 - 自动提取核心关键词
 * 4. 上下文增强 - 补充对话上下文
 * 5. 智能建议 - 基于意图推荐后续操作
 */
@Slf4j
@Service
public class AiChatOptimizer {

    // 预定义意图及其关键词
    private static final Map<String, Set<String>> INTENT_KEYWORDS = new LinkedHashMap<>();
    
    // 意图示例句子（用于相似度匹配）
    private static final Map<String, List<String>> INTENT_EXAMPLES = new LinkedHashMap<>();
    
    // 用户会话状态
    private final Map<String, SessionState> sessionStates = new ConcurrentHashMap<>();
    
    // 中文停用词
    private static final Set<String> STOP_WORDS = new HashSet<>();
    
    // 意图权重（用于优先级排序）
    private static final Map<String, Integer> INTENT_PRIORITY = new HashMap<>();
    
    static {
        // ============ 意图关键词 ============
        INTENT_KEYWORDS.put("SQL_GENERATE", new HashSet<>(Arrays.asList(
            "sql", "查询", "select", "写", "生成", "帮我", "语句", "数据", "表", 
            "insert", "update", "delete", "创建", "建表", "from", "where"
        )));
        
        INTENT_KEYWORDS.put("SQL_OPTIMIZE", new HashSet<>(Arrays.asList(
            "优化", "性能", "慢", "索引", "执行计划", "explain", "加速", "提升"
        )));
        
        INTENT_KEYWORDS.put("SQL_EXPLAIN", new HashSet<>(Arrays.asList(
            "解释", "什么意思", "理解", "分析", "含义", "作用"
        )));
        
        INTENT_KEYWORDS.put("CHART_GENERATE", new HashSet<>(Arrays.asList(
            "图表", "柱状图", "折线图", "饼图", "散点图", "可视化", "chart",
            "曲线", "趋势图", "报表", "图形", "画图"
        )));
        
        INTENT_KEYWORDS.put("DATA_ANALYSIS", new HashSet<>(Arrays.asList(
            "分析", "统计", "趋势", "对比", "汇总", "洞察", "规律", "预测"
        )));
        
        INTENT_KEYWORDS.put("ETL_FLOW", new HashSet<>(Arrays.asList(
            "etl", "流程", "同步", "迁移", "抽取", "pipeline", "管道", "传输"
        )));
        
        // ============ 意图示例句子 ============
        INTENT_EXAMPLES.put("SQL_GENERATE", Arrays.asList(
            "帮我写一个SQL查询", "生成一个SQL语句", "我想查询数据",
            "写个select语句", "帮我从表里查数据", "如何查询用户信息"
        ));
        
        INTENT_EXAMPLES.put("SQL_OPTIMIZE", Arrays.asList(
            "优化这个SQL", "SQL性能优化", "这个查询太慢了",
            "如何加快SQL执行", "索引优化建议"
        ));
        
        INTENT_EXAMPLES.put("CHART_GENERATE", Arrays.asList(
            "生成一个图表", "创建柱状图", "做一个折线图",
            "数据可视化", "画个饼图"
        ));
        
        INTENT_EXAMPLES.put("DATA_ANALYSIS", Arrays.asList(
            "分析这些数据", "数据趋势分析", "统计分析",
            "数据洞察", "帮我分析一下"
        ));
        
        // ============ 意图优先级 ============
        INTENT_PRIORITY.put("SQL_OPTIMIZE", 10);
        INTENT_PRIORITY.put("SQL_EXPLAIN", 9);
        INTENT_PRIORITY.put("SQL_GENERATE", 8);
        INTENT_PRIORITY.put("CHART_GENERATE", 7);
        INTENT_PRIORITY.put("ETL_FLOW", 6);
        INTENT_PRIORITY.put("DATA_ANALYSIS", 5);
        INTENT_PRIORITY.put("GENERAL", 0);
        
        // ============ 停用词 ============
        STOP_WORDS.addAll(Arrays.asList(
            "的", "了", "和", "是", "就", "都", "而", "及", "与", "着",
            "或", "一个", "没有", "我们", "你们", "他们", "它们", "这个", "那个",
            "请", "帮", "帮我", "麻烦", "能", "可以", "想", "要", "需要",
            "一下", "看看", "试试", "吧", "呢", "啊", "哦", "嗯", "好"
        ));
    }
    
    @PostConstruct
    public void init() {
        log.info("AI对话优化器初始化完成，已加载 {} 个意图类别", INTENT_KEYWORDS.size());
    }
    
    /**
     * 语义意图识别
     * 使用关键词匹配 + 相似度计算
     */
    public IntentResult classifyIntentSemantic(String message) {
        if (message == null || message.trim().isEmpty()) {
            return new IntentResult("GENERAL", 0.0);
        }
        
        String processed = message.trim().toLowerCase();
        List<String> words = segment(processed);
        
        // 计算每个意图的匹配分数
        Map<String, Double> scores = new HashMap<>();
        
        for (Map.Entry<String, Set<String>> entry : INTENT_KEYWORDS.entrySet()) {
            String intent = entry.getKey();
            Set<String> keywords = entry.getValue();
            
            // 关键词匹配分数
            double keywordScore = calculateKeywordScore(words, keywords);
            
            // 示例句子相似度分数
            double exampleScore = 0.0;
            List<String> examples = INTENT_EXAMPLES.get(intent);
            if (examples != null) {
                exampleScore = calculateMaxSimilarity(processed, examples);
            }
            
            // 综合分数 = 关键词分数 * 0.6 + 示例相似度 * 0.4
            double totalScore = keywordScore * 0.6 + exampleScore * 0.4;
            scores.put(intent, totalScore);
        }
        
        // 找出最高分的意图
        String bestIntent = "GENERAL";
        double bestScore = 0.0;
        
        for (Map.Entry<String, Double> entry : scores.entrySet()) {
            if (entry.getValue() > bestScore) {
                bestScore = entry.getValue();
                bestIntent = entry.getKey();
            } else if (entry.getValue() == bestScore && entry.getValue() > 0) {
                // 相同分数时，使用优先级
                int currentPriority = INTENT_PRIORITY.getOrDefault(bestIntent, 0);
                int newPriority = INTENT_PRIORITY.getOrDefault(entry.getKey(), 0);
                if (newPriority > currentPriority) {
                    bestIntent = entry.getKey();
                }
            }
        }
        
        // 置信度阈值
        if (bestScore < 0.2) {
            return new IntentResult("GENERAL", bestScore);
        }
        
        return new IntentResult(bestIntent, bestScore);
    }
    
    /**
     * 使用HanLP进行中文分词
     */
    public List<String> segment(String text) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }
        
        try {
            List<Term> terms = HanLP.segment(text);
            return terms.stream()
                .map(term -> term.word.toLowerCase())
                .filter(word -> word.length() > 1 && !STOP_WORDS.contains(word))
                .collect(Collectors.toList());
        } catch (Exception e) {
            // 降级：简单分词
            return Arrays.stream(text.split("[\\s,，。！？!?;；:：]+"))
                .filter(s -> s.length() > 1)
                .collect(Collectors.toList());
        }
    }
    
    /**
     * 提取关键词
     */
    public List<String> extractKeywords(String message) {
        if (message == null || message.isEmpty()) {
            return Collections.emptyList();
        }
        
        try {
            // 使用HanLP提取关键词
            return HanLP.extractKeyword(message, 5);
        } catch (Exception e) {
            // 降级：使用分词结果
            return segment(message).stream()
                .limit(5)
                .collect(Collectors.toList());
        }
    }
    
    /**
     * 计算关键词匹配分数
     */
    private double calculateKeywordScore(List<String> words, Set<String> keywords) {
        if (words.isEmpty() || keywords.isEmpty()) return 0.0;
        
        int matchCount = 0;
        for (String word : words) {
            if (keywords.contains(word)) {
                matchCount++;
            }
            // 部分匹配
            for (String keyword : keywords) {
                if (word.contains(keyword) || keyword.contains(word)) {
                    matchCount++;
                    break;
                }
            }
        }
        
        return Math.min(1.0, (double) matchCount / Math.max(3, words.size()));
    }
    
    /**
     * 计算与示例句子的最大相似度
     */
    private double calculateMaxSimilarity(String text, List<String> examples) {
        double maxSim = 0.0;
        List<String> textWords = segment(text);
        
        for (String example : examples) {
            double sim = calculateJaccardSimilarity(textWords, segment(example));
            maxSim = Math.max(maxSim, sim);
        }
        
        return maxSim;
    }
    
    /**
     * Jaccard相似度
     */
    private double calculateJaccardSimilarity(List<String> words1, List<String> words2) {
        if (words1.isEmpty() || words2.isEmpty()) return 0.0;
        
        Set<String> set1 = new HashSet<>(words1);
        Set<String> set2 = new HashSet<>(words2);
        
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);
        
        return (double) intersection.size() / union.size();
    }
    
    /**
     * 消息预处理
     */
    public String preprocessMessage(String message) {
        if (message == null) return "";
        
        String result = message.trim();
        result = result.replaceAll("\\s+", " ");
        result = result.replaceAll("[，。！？,.!?;；:：]", " ");
        
        return result.trim();
    }
    
    /**
     * 增强上下文
     */
    public String enhanceContext(String message, String sessionId, String existingContext) {
        StringBuilder enhanced = new StringBuilder();
        
        if (existingContext != null && !existingContext.isEmpty()) {
            enhanced.append(existingContext).append("\n\n");
        }
        
        SessionState state = getSessionState(sessionId);
        
        if (!state.collectedInfo.isEmpty()) {
            enhanced.append("【用户已提供的信息】\n");
            for (Map.Entry<String, String> info : state.collectedInfo.entrySet()) {
                enhanced.append("- ").append(info.getKey()).append(": ").append(info.getValue()).append("\n");
            }
            enhanced.append("\n");
        }
        
        if (state.currentIntent != null) {
            enhanced.append("【识别的用户意图】").append(state.currentIntent).append("\n");
        }
        
        List<String> keywords = extractKeywords(message);
        if (!keywords.isEmpty()) {
            enhanced.append("【关键词】").append(String.join(", ", keywords)).append("\n");
        }
        
        return enhanced.toString();
    }
    
    /**
     * 获取或创建会话状态
     */
    public SessionState getSessionState(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return new SessionState();
        }
        // 清理过期会话，防止内存泄漏
        if (sessionStates.size() > 100) {
            sessionStates.entrySet().removeIf(e -> e.getValue().isExpired());
        }
        SessionState state = sessionStates.computeIfAbsent(sessionId, k -> new SessionState());
        state.lastActiveTime = System.currentTimeMillis();
        return state;
    }
    
    /**
     * 更新会话状态
     */
    public void updateSessionState(String sessionId, String intent, Map<String, String> info) {
        SessionState state = getSessionState(sessionId);
        if (intent != null) {
            state.currentIntent = intent;
        }
        if (info != null) {
            state.collectedInfo.putAll(info);
        }
        state.lastActiveTime = System.currentTimeMillis();
    }
    
    /**
     * 清除会话状态
     */
    public void clearSessionState(String sessionId) {
        sessionStates.remove(sessionId);
    }
    
    /**
     * 生成智能建议
     */
    public List<String> generateSuggestions(String intent, SessionState state) {
        List<String> suggestions = new ArrayList<>();
        
        switch (intent) {
            case "SQL_GENERATE":
                suggestions.add("查询所有用户信息");
                suggestions.add("统计今日订单数量");
                suggestions.add("查询最近7天的销售数据");
                break;
            case "SQL_OPTIMIZE":
                suggestions.add("分析SQL执行计划");
                suggestions.add("添加合适的索引");
                suggestions.add("优化JOIN查询");
                break;
            case "CHART_GENERATE":
                suggestions.add("生成销售趋势折线图");
                suggestions.add("创建用户分布饼图");
                suggestions.add("制作月度对比柱状图");
                break;
            case "DATA_ANALYSIS":
                suggestions.add("分析用户活跃度趋势");
                suggestions.add("对比各渠道转化率");
                suggestions.add("统计TOP10热销商品");
                break;
            case "ETL_FLOW":
                suggestions.add("创建数据同步任务");
                suggestions.add("配置增量抽取");
                suggestions.add("设置定时执行");
                break;
            default:
                suggestions.add("帮我写一个SQL查询");
                suggestions.add("生成一个数据图表");
                suggestions.add("分析数据趋势");
        }
        
        return suggestions;
    }
    
    // ============ 内部类 ============
    
    /**
     * 意图识别结果
     */
    public static class IntentResult {
        public final String intent;
        public final double confidence;
        
        public IntentResult(String intent, double confidence) {
            this.intent = intent;
            this.confidence = confidence;
        }
        
        public boolean isConfident() {
            return confidence >= 0.3;
        }
    }
    
    /**
     * 会话状态
     */
    public static class SessionState {
        public String currentIntent;
        public Map<String, String> collectedInfo = new HashMap<>();
        public List<String> messageHistory = new ArrayList<>();
        public long lastActiveTime = System.currentTimeMillis();
        
        public boolean hasInfo(String key) {
            return collectedInfo.containsKey(key) && !collectedInfo.get(key).isEmpty();
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() - lastActiveTime > 30 * 60 * 1000;
        }
    }
}
