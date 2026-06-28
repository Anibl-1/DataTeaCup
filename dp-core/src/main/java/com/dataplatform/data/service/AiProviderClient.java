package com.dataplatform.data.service;

import com.dataplatform.infra.ai.AiConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * AI提供商客户端 - 封装所有AI API调用逻辑
 * 支持 OpenAI、Claude、Gemini、通义千问、DeepSeek、智谱GLM、DeepSeek Local、Ollama
 */
@Slf4j
@Service
public class AiProviderClient {

    private static final String DEEPSEEK_DEFAULT_BASE_URL = "https://api.deepseek.com";
    private static final String DEEPSEEK_DEFAULT_MODEL = "deepseek-v4-flash";

    @Autowired
    private AiConfig aiConfig;

    @org.springframework.beans.factory.annotation.Value("${ai.timeout.connect:30000}")
    private int connectTimeout;

    @org.springframework.beans.factory.annotation.Value("${ai.timeout.read:180000}")
    private int readTimeout;

    private RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        this.restTemplate = new RestTemplate(factory);
        log.info("AI RestTemplate初始化: connectTimeout={}ms, readTimeout={}ms", connectTimeout, readTimeout);
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    /**
     * 核心对话方法 - 根据配置选择不同的AI提供商
     */
    public String callAiProvider(String systemPrompt, String userMessage) throws Exception {
        return callAiProviderWithHistory(systemPrompt, userMessage, null);
    }

    /**
     * 简化的对话方法 - 直接传入系统提示和用户消息
     */
    public String chat(String systemPrompt, String userMessage) throws Exception {
        return callAiProvider(systemPrompt, userMessage);
    }

    /**
     * 核心对话方法（支持会话历史）
     */
    public String callAiProviderWithHistory(String systemPrompt, String userMessage, List<Map<String, String>> history) throws Exception {
        String provider = aiConfig.getProvider().toLowerCase();

        return switch (provider) {
            case "openai" -> callOpenAiWithHistory(systemPrompt, userMessage, history);
            case "claude" -> callClaudeWithHistory(systemPrompt, userMessage, history);
            case "gemini" -> callGeminiWithHistory(systemPrompt, userMessage, history);
            case "qwen" -> callQwenWithHistory(systemPrompt, userMessage, history);
            case "deepseek" -> callDeepSeekWithHistory(systemPrompt, userMessage, history);
            case "zhipu" -> callZhipuWithHistory(systemPrompt, userMessage, history);
            case "deepseek-local" -> callDeepSeekLocalWithHistory(systemPrompt, userMessage, history);
            case "ollama" -> callOllamaWithHistory(systemPrompt, userMessage, history);
            default -> throw new IllegalArgumentException("不支持的AI提供商: " + provider);
        };
    }

    /**
     * 测试AI提供商连接（使用临时配置，不修改共享状态）
     */
    public String testProviderConnection(String provider, String apiKey, String baseUrl, String model) throws Exception {
        String testMessage = "请用一句话回复：你好";
        String systemPrompt = "你是一个简洁的助手";

        // 根据提供商确定实际的URL、model、apiKey（优先使用传入参数，回退到已有配置）
        String actualUrl, actualModel, actualApiKey;
        switch (provider.toLowerCase()) {
            case "openai" -> {
                AiConfig.OpenAiConfig cfg = aiConfig.getOpenai();
                actualUrl = appendPath(baseUrl != null && !baseUrl.isEmpty() ? baseUrl : cfg.getBaseUrl(), "/chat/completions");
                actualModel = model != null && !model.isEmpty() ? model : cfg.getModel();
                actualApiKey = apiKey != null && !apiKey.isEmpty() ? apiKey : cfg.getApiKey();
            }
            case "deepseek" -> {
                AiConfig.DeepSeekConfig cfg = aiConfig.getDeepseek();
                actualUrl = appendDeepSeekChatPath(baseUrl != null && !baseUrl.isEmpty() ? baseUrl : cfg.getBaseUrl());
                actualModel = model != null && !model.isEmpty() ? model : cfg.getModel();
                actualApiKey = apiKey != null && !apiKey.isEmpty() ? apiKey : cfg.getApiKey();
            }
            case "claude" -> {
                AiConfig.ClaudeConfig cfg = aiConfig.getClaude();
                actualUrl = appendPath(baseUrl != null && !baseUrl.isEmpty() ? baseUrl : cfg.getBaseUrl(), "/v1/messages");
                actualModel = model != null && !model.isEmpty() ? model : cfg.getModel();
                actualApiKey = apiKey != null && !apiKey.isEmpty() ? apiKey : cfg.getApiKey();
            }
            case "gemini" -> {
                AiConfig.GeminiConfig cfg = aiConfig.getGemini();
                String geminiModel = model != null && !model.isEmpty() ? model : cfg.getModel();
                String geminiBaseUrl = baseUrl != null && !baseUrl.isEmpty() ? baseUrl : cfg.getBaseUrl();
                actualUrl = geminiBaseUrl + "/v1beta/models/" + geminiModel + ":generateContent";
                actualModel = geminiModel;
                actualApiKey = apiKey != null && !apiKey.isEmpty() ? apiKey : cfg.getApiKey();
            }
            case "zhipu" -> {
                AiConfig.ZhipuConfig cfg = aiConfig.getZhipu();
                actualUrl = appendPath(baseUrl != null && !baseUrl.isEmpty() ? baseUrl : cfg.getBaseUrl(), "/chat/completions");
                actualModel = model != null && !model.isEmpty() ? model : cfg.getModel();
                actualApiKey = apiKey != null && !apiKey.isEmpty() ? apiKey : cfg.getApiKey();
            }
            case "deepseek-local", "deepseeklocal" -> {
                AiConfig.DeepSeekLocalConfig cfg = aiConfig.getDeepseekLocal();
                actualUrl = appendPath(baseUrl != null && !baseUrl.isEmpty() ? baseUrl : cfg.getBaseUrl(), "/chat/completions");
                actualModel = model != null && !model.isEmpty() ? model : cfg.getModel();
                actualApiKey = apiKey != null && !apiKey.isEmpty() ? apiKey : cfg.getApiKey();
            }
            case "qwen" -> {
                AiConfig.QwenConfig cfg = aiConfig.getQwen();
                actualUrl = appendPath(baseUrl != null && !baseUrl.isEmpty() ? baseUrl : cfg.getBaseUrl(), "/services/aigc/text-generation/generation");
                actualModel = model != null && !model.isEmpty() ? model : cfg.getModel();
                actualApiKey = apiKey != null && !apiKey.isEmpty() ? apiKey : cfg.getApiKey();
            }
            case "ollama" -> {
                AiConfig.OllamaConfig cfg = aiConfig.getOllama();
                actualUrl = appendPath(baseUrl != null && !baseUrl.isEmpty() ? baseUrl : cfg.getBaseUrl(), "/api/chat");
                actualModel = model != null && !model.isEmpty() ? model : cfg.getModel();
                actualApiKey = null; // Ollama不需要apiKey
            }
            default -> throw new IllegalArgumentException("不支持的AI提供商: " + provider);
        }

        // 构建请求体（不修改共享aiConfig）
        ObjectNode requestBody = objectMapper.createObjectNode();
        HttpHeaders headers = new HttpHeaders();
        setJsonContentType(headers);

        String providerLower = provider.toLowerCase();
        if ("claude".equals(providerLower)) {
            // Anthropic Messages API 格式
            requestBody.put("model", actualModel);
            requestBody.put("max_tokens", 100);
            requestBody.putArray("messages").addObject().put("role", "user").put("content", testMessage);
            headers.set("x-api-key", actualApiKey);
            headers.set("anthropic-version", "2023-06-01");
        } else if ("gemini".equals(providerLower)) {
            // Google Gemini API 格式（apiKey 作为 query param）
            actualUrl = actualUrl + "?key=" + actualApiKey;
            ObjectNode content = requestBody.putArray("contents").addObject();
            content.put("role", "user");
            content.putArray("parts").addObject().put("text", testMessage);
        } else if ("qwen".equals(providerLower)) {
            requestBody.put("model", actualModel);
            ObjectNode input = requestBody.putObject("input");
            ArrayNode messages = input.putArray("messages");
            messages.addObject().put("role", "system").put("content", systemPrompt);
            messages.addObject().put("role", "user").put("content", testMessage);
            headers.setBearerAuth(actualApiKey);
        } else if ("ollama".equals(providerLower)) {
            requestBody.put("model", actualModel);
            requestBody.put("stream", false);
            ArrayNode messages = requestBody.putArray("messages");
            messages.addObject().put("role", "system").put("content", systemPrompt);
            messages.addObject().put("role", "user").put("content", testMessage);
        } else {
            // OpenAI-compatible: openai, deepseek, zhipu, deepseek-local
            requestBody.put("model", actualModel);
            requestBody.put("max_tokens", 100);
            requestBody.put("temperature", 0.7);
            if ("deepseek".equals(providerLower)) {
                applyDeepSeekThinkingOptions(requestBody, actualModel, aiConfig.getDeepseek());
            }
            ArrayNode messages = requestBody.putArray("messages");
            messages.addObject().put("role", "system").put("content", systemPrompt);
            messages.addObject().put("role", "user").put("content", testMessage);
            if (actualApiKey != null && !actualApiKey.isEmpty()) {
                headers.setBearerAuth(actualApiKey);
            }
        }

        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);
        ResponseEntity<String> response = restTemplate.exchange(actualUrl, HttpMethod.POST, entity, String.class);

        // 解析响应
        JsonNode responseJson = objectMapper.readTree(response.getBody());
        if ("ollama".equals(providerLower)) {
            return responseJson.path("message").path("content").asText("");
        } else if ("qwen".equals(providerLower)) {
            return responseJson.path("output").path("text").asText("");
        } else if ("claude".equals(providerLower)) {
            JsonNode content = responseJson.path("content");
            if (content.isArray() && !content.isEmpty()) {
                return content.get(0).path("text").asText("");
            }
            return "";
        } else if ("gemini".equals(providerLower)) {
            return responseJson.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText("");
        } else {
            JsonNode choices = responseJson.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                throw new RuntimeException("API返回无效响应: choices为空");
            }
            return choices.get(0).path("message").path("content").asText("");
        }
    }

    // ============ 各提供商具体实现 ============

    private String callOpenAiWithHistory(String systemPrompt, String userMessage, List<Map<String, String>> history) throws Exception {
        AiConfig.OpenAiConfig config = aiConfig.getOpenai();
        String url = appendPath(config.getBaseUrl(), "/chat/completions");

        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", config.getModel());
        requestBody.put("max_tokens", config.getMaxTokens());
        requestBody.put("temperature", config.getTemperature());

        ArrayNode messages = requestBody.putArray("messages");
        messages.addObject().put("role", "system").put("content", systemPrompt);
        addHistoryMessages(messages, history);
        messages.addObject().put("role", "user").put("content", userMessage);

        HttpHeaders headers = new HttpHeaders();
        setJsonContentType(headers);
        headers.setBearerAuth(config.getApiKey());

        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        JsonNode responseJson = objectMapper.readTree(response.getBody());
        JsonNode choices = responseJson.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            throw new RuntimeException("OpenAI API返回无效响应: choices为空");
        }
        return choices.get(0).path("message").path("content").asText("");
    }

    private String callQwenWithHistory(String systemPrompt, String userMessage, List<Map<String, String>> history) throws Exception {
        AiConfig.QwenConfig config = aiConfig.getQwen();
        String url = appendPath(config.getBaseUrl(), "/services/aigc/text-generation/generation");

        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", config.getModel());

        ObjectNode input = requestBody.putObject("input");
        ArrayNode messages = input.putArray("messages");
        messages.addObject().put("role", "system").put("content", systemPrompt);
        addHistoryMessages(messages, history);
        messages.addObject().put("role", "user").put("content", userMessage);

        HttpHeaders headers = new HttpHeaders();
        setJsonContentType(headers);
        headers.set("Authorization", "Bearer " + config.getApiKey());

        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        JsonNode responseJson = objectMapper.readTree(response.getBody());
        return responseJson.path("output").path("text").asText();
    }

    private String callDeepSeekWithHistory(String systemPrompt, String userMessage, List<Map<String, String>> history) throws Exception {
        AiConfig.DeepSeekConfig config = aiConfig.getDeepseek();
        String url = appendDeepSeekChatPath(config.getBaseUrl());

        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", config.getModel());
        requestBody.put("max_tokens", config.getMaxTokens());
        requestBody.put("temperature", config.getTemperature());
        applyDeepSeekThinkingOptions(requestBody, config.getModel(), config);

        ArrayNode messages = requestBody.putArray("messages");
        messages.addObject().put("role", "system").put("content", systemPrompt);
        addHistoryMessages(messages, history);
        messages.addObject().put("role", "user").put("content", userMessage);

        HttpHeaders headers = new HttpHeaders();
        setJsonContentType(headers);
        headers.setBearerAuth(config.getApiKey());

        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        JsonNode responseJson = objectMapper.readTree(response.getBody());
        JsonNode choices = responseJson.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            throw new RuntimeException("DeepSeek API返回无效响应: choices为空");
        }
        return choices.get(0).path("message").path("content").asText("");
    }

    private String callDeepSeekLocalWithHistory(String systemPrompt, String userMessage, List<Map<String, String>> history) throws Exception {
        AiConfig.DeepSeekLocalConfig config = aiConfig.getDeepseekLocal();
        String url = appendPath(config.getBaseUrl(), "/chat/completions");

        log.debug("调用DeepSeek Local API: url={}, model={}", url, config.getModel());

        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", config.getModel());
        requestBody.put("max_tokens", config.getMaxTokens());
        requestBody.put("temperature", config.getTemperature());

        ArrayNode messages = requestBody.putArray("messages");
        messages.addObject().put("role", "system").put("content", systemPrompt);
        addHistoryMessages(messages, history);
        messages.addObject().put("role", "user").put("content", userMessage);

        HttpHeaders headers = new HttpHeaders();
        setJsonContentType(headers);
        String apiKey = config.getApiKey();
        if (apiKey != null && !apiKey.isEmpty()) {
            headers.setBearerAuth(apiKey);
        }

        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        JsonNode responseJson = objectMapper.readTree(response.getBody());
        JsonNode choices = responseJson.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            throw new RuntimeException("DeepSeek Local API返回无效响应: choices为空");
        }
        return choices.get(0).path("message").path("content").asText("");
    }

    private String callOllamaWithHistory(String systemPrompt, String userMessage, List<Map<String, String>> history) throws Exception {
        AiConfig.OllamaConfig config = aiConfig.getOllama();
        String url = appendPath(config.getBaseUrl(), "/api/chat");

        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", config.getModel());
        requestBody.put("stream", false);

        ArrayNode messages = requestBody.putArray("messages");
        messages.addObject().put("role", "system").put("content", systemPrompt);
        addHistoryMessages(messages, history);
        messages.addObject().put("role", "user").put("content", userMessage);

        HttpHeaders headers = new HttpHeaders();
        setJsonContentType(headers);

        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        JsonNode responseJson = objectMapper.readTree(response.getBody());
        return responseJson.path("message").path("content").asText();
    }

    private String callClaudeWithHistory(String systemPrompt, String userMessage, List<Map<String, String>> history) throws Exception {
        AiConfig.ClaudeConfig config = aiConfig.getClaude();
        String url = appendPath(config.getBaseUrl(), "/v1/messages");

        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", config.getModel());
        requestBody.put("max_tokens", config.getMaxTokens());
        requestBody.put("temperature", config.getTemperature());

        // Claude uses system as a top-level field, not in messages
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            requestBody.put("system", systemPrompt);
        }

        ArrayNode messages = requestBody.putArray("messages");
        addHistoryMessages(messages, history);
        messages.addObject().put("role", "user").put("content", userMessage);

        HttpHeaders headers = new HttpHeaders();
        setJsonContentType(headers);
        headers.set("x-api-key", config.getApiKey());
        headers.set("anthropic-version", "2023-06-01");

        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        JsonNode responseJson = objectMapper.readTree(response.getBody());
        JsonNode content = responseJson.path("content");
        if (content.isArray() && !content.isEmpty()) {
            return content.get(0).path("text").asText("");
        }
        throw new RuntimeException("Claude API返回无效响应: content为空");
    }

    private String callGeminiWithHistory(String systemPrompt, String userMessage, List<Map<String, String>> history) throws Exception {
        AiConfig.GeminiConfig config = aiConfig.getGemini();
        String url = config.getBaseUrl() + "/v1beta/models/" + config.getModel() + ":generateContent?key=" + config.getApiKey();

        ObjectNode requestBody = objectMapper.createObjectNode();

        // System instruction
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            ObjectNode systemInstruction = requestBody.putObject("systemInstruction");
            systemInstruction.putArray("parts").addObject().put("text", systemPrompt);
        }

        ArrayNode contents = requestBody.putArray("contents");

        // Add history
        if (history != null) {
            for (Map<String, String> msg : history) {
                String role = "user".equals(msg.get("role")) ? "user" : "model";
                ObjectNode content = contents.addObject();
                content.put("role", role);
                content.putArray("parts").addObject().put("text", msg.get("content"));
            }
        }

        // Add current user message
        ObjectNode userContent = contents.addObject();
        userContent.put("role", "user");
        userContent.putArray("parts").addObject().put("text", userMessage);

        // Generation config
        ObjectNode generationConfig = requestBody.putObject("generationConfig");
        generationConfig.put("maxOutputTokens", config.getMaxTokens());
        generationConfig.put("temperature", config.getTemperature());

        HttpHeaders headers = new HttpHeaders();
        setJsonContentType(headers);

        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        JsonNode responseJson = objectMapper.readTree(response.getBody());
        JsonNode candidates = responseJson.path("candidates");
        if (candidates.isArray() && !candidates.isEmpty()) {
            return candidates.get(0).path("content").path("parts").get(0).path("text").asText("");
        }
        throw new RuntimeException("Gemini API返回无效响应: candidates为空");
    }

    private String callZhipuWithHistory(String systemPrompt, String userMessage, List<Map<String, String>> history) throws Exception {
        AiConfig.ZhipuConfig config = aiConfig.getZhipu();
        String url = appendPath(config.getBaseUrl(), "/chat/completions");

        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", config.getModel());
        requestBody.put("max_tokens", config.getMaxTokens());
        requestBody.put("temperature", (float) config.getTemperature());

        ArrayNode messages = requestBody.putArray("messages");
        messages.addObject().put("role", "system").put("content", systemPrompt);
        addHistoryMessages(messages, history);
        messages.addObject().put("role", "user").put("content", userMessage);

        HttpHeaders headers = new HttpHeaders();
        setJsonContentType(headers);
        headers.setBearerAuth(config.getApiKey());

        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        JsonNode responseJson = objectMapper.readTree(response.getBody());
        JsonNode choices = responseJson.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            throw new RuntimeException("智谱GLM API返回无效响应: choices为空");
        }
        return choices.get(0).path("message").path("content").asText("");
    }

    /**
     * 向消息数组中添加历史消息
     */
    private void addHistoryMessages(ArrayNode messages, List<Map<String, String>> history) {
        if (history != null) {
            for (Map<String, String> msg : history) {
                messages.addObject().put("role", msg.get("role")).put("content", msg.get("content"));
            }
        }
    }

    private String appendDeepSeekChatPath(String baseUrl) {
        String base = normalizeBaseUrl(baseUrl != null && !baseUrl.isBlank() ? baseUrl : DEEPSEEK_DEFAULT_BASE_URL);
        return appendPath(base, "/chat/completions");
    }

    private String appendPath(String baseUrl, String path) {
        String base = normalizeBaseUrl(baseUrl);
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        if (base.endsWith(normalizedPath)) {
            return base;
        }
        return base + normalizedPath;
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private void setJsonContentType(HttpHeaders headers) {
        headers.setContentType(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8));
    }

    private void applyDeepSeekThinkingOptions(ObjectNode requestBody, String model, AiConfig.DeepSeekConfig config) {
        if (model == null || model.isBlank()) {
            requestBody.put("model", DEEPSEEK_DEFAULT_MODEL);
            model = DEEPSEEK_DEFAULT_MODEL;
        }
        if ("deepseek-v4-pro".equals(model) || "deepseek-v4-flash".equals(model)) {
            ObjectNode thinking = requestBody.putObject("thinking");
            String thinkingType = config.getThinkingType();
            thinking.put("type", thinkingType != null && !thinkingType.isBlank() ? thinkingType : "disabled");
            String effort = config.getReasoningEffort();
            if (effort != null && !effort.isBlank()) {
                requestBody.put("reasoning_effort", effort);
            }
        }
    }
}
