package com.dataplatform.data.service;

import com.dataplatform.infra.ai.AiConfig;
import com.dataplatform.data.mapper.AiConfigMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * AI配置管理器 - 负责AI配置的加载、保存、测试
 */
@Slf4j
@Service
public class AiConfigManager {

    @Autowired
    private AiConfig aiConfig;

    @Autowired(required = false)
    private AiConfigMapper aiConfigMapper;

    @Autowired
    private AiProviderClient providerClient;

    private int maxHistorySize = 5;

    public int getMaxHistorySize() {
        return maxHistorySize;
    }

    /**
     * 从数据库加载配置到内存
     */
    public void loadConfigFromDb() {
        if (aiConfigMapper == null) {
            log.warn("AiConfigMapper未注入，使用默认配置");
            return;
        }
        try {
            Map<String, Map<String, Object>> configs = aiConfigMapper.getAllConfigs();
            if (configs == null || configs.isEmpty()) {
                log.info("数据库中无AI配置，使用默认配置");
                return;
            }

            String provider = getDbConfigValue(configs, "provider");
            if (provider != null) aiConfig.setProvider(provider);

            loadOpenAiConfig(configs);
            loadClaudeConfig(configs);
            loadGeminiConfig(configs);
            loadQwenConfig(configs);
            loadDeepSeekConfig(configs);
            loadZhipuConfig(configs);

            String ollamaUrl = getDbConfigValue(configs, "ollama_base_url");
            String ollamaModel = getDbConfigValue(configs, "ollama_model");
            if (ollamaUrl != null) aiConfig.getOllama().setBaseUrl(ollamaUrl);
            if (ollamaModel != null) aiConfig.getOllama().setModel(ollamaModel);

            String maxHistory = getDbConfigValue(configs, "max_history");
            if (maxHistory != null) {
                try { maxHistorySize = Integer.parseInt(maxHistory); } catch (NumberFormatException ignored) {}
            }

            // 打印加载后的关键信息，便于排查"密钥丢失"问题
            String prov = aiConfig.getProvider();
            boolean keyOk = isCurrentApiKeyConfigured();
            log.info("AI配置已从数据库加载: provider={}, apiKeyConfigured={}", prov, keyOk);
            if (!keyOk) {
                log.warn("当前provider={}的API密钥未配置或无效，请在前端AI设置中配置", prov);
            }
        } catch (Exception e) {
            log.error("从数据库加载AI配置失败，将使用yml默认配置！请检查sys_ai_config表: {}", e.getMessage(), e);
        }
    }

    private String getDbConfigValue(Map<String, Map<String, Object>> configs, String key) {
        Map<String, Object> item = configs.get(key);
        if (item != null) {
            Object value = item.get("config_value");
            return value != null ? value.toString() : null;
        }
        return null;
    }

    private void loadOpenAiConfig(Map<String, Map<String, Object>> configs) {
        String apiKey = getDbConfigValue(configs, "openai_api_key");
        String baseUrl = getDbConfigValue(configs, "openai_base_url");
        String model = getDbConfigValue(configs, "openai_model");
        if (apiKey != null && !apiKey.isEmpty()) aiConfig.getOpenai().setApiKey(apiKey);
        if (baseUrl != null && !baseUrl.isEmpty()) aiConfig.getOpenai().setBaseUrl(baseUrl);
        if (model != null && !model.isEmpty()) aiConfig.getOpenai().setModel(model);
    }

    private void loadQwenConfig(Map<String, Map<String, Object>> configs) {
        String apiKey = getDbConfigValue(configs, "qwen_api_key");
        String baseUrl = getDbConfigValue(configs, "qwen_base_url");
        String model = getDbConfigValue(configs, "qwen_model");
        if (apiKey != null && !apiKey.isEmpty()) aiConfig.getQwen().setApiKey(apiKey);
        if (baseUrl != null && !baseUrl.isEmpty()) aiConfig.getQwen().setBaseUrl(baseUrl);
        if (model != null && !model.isEmpty()) aiConfig.getQwen().setModel(model);
    }

    private void loadClaudeConfig(Map<String, Map<String, Object>> configs) {
        String apiKey = getDbConfigValue(configs, "claude_api_key");
        String baseUrl = getDbConfigValue(configs, "claude_base_url");
        String model = getDbConfigValue(configs, "claude_model");
        if (apiKey != null && !apiKey.isEmpty()) aiConfig.getClaude().setApiKey(apiKey);
        if (baseUrl != null && !baseUrl.isEmpty()) aiConfig.getClaude().setBaseUrl(baseUrl);
        if (model != null && !model.isEmpty()) aiConfig.getClaude().setModel(model);
    }

    private void loadGeminiConfig(Map<String, Map<String, Object>> configs) {
        String apiKey = getDbConfigValue(configs, "gemini_api_key");
        String baseUrl = getDbConfigValue(configs, "gemini_base_url");
        String model = getDbConfigValue(configs, "gemini_model");
        if (apiKey != null && !apiKey.isEmpty()) aiConfig.getGemini().setApiKey(apiKey);
        if (baseUrl != null && !baseUrl.isEmpty()) aiConfig.getGemini().setBaseUrl(baseUrl);
        if (model != null && !model.isEmpty()) aiConfig.getGemini().setModel(model);
    }

    private void loadDeepSeekConfig(Map<String, Map<String, Object>> configs) {
        String apiKey = getDbConfigValue(configs, "deepseek_api_key");
        String baseUrl = getDbConfigValue(configs, "deepseek_base_url");
        String model = getDbConfigValue(configs, "deepseek_model");
        if (apiKey != null && !apiKey.isEmpty()) aiConfig.getDeepseek().setApiKey(apiKey);
        if (baseUrl != null && !baseUrl.isEmpty()) aiConfig.getDeepseek().setBaseUrl(baseUrl);
        if (model != null && !model.isEmpty()) aiConfig.getDeepseek().setModel(model);
    }

    private void loadZhipuConfig(Map<String, Map<String, Object>> configs) {
        String apiKey = getDbConfigValue(configs, "zhipu_api_key");
        String baseUrl = getDbConfigValue(configs, "zhipu_base_url");
        String model = getDbConfigValue(configs, "zhipu_model");
        if (apiKey != null && !apiKey.isEmpty()) aiConfig.getZhipu().setApiKey(apiKey);
        if (baseUrl != null && !baseUrl.isEmpty()) aiConfig.getZhipu().setBaseUrl(baseUrl);
        if (model != null && !model.isEmpty()) aiConfig.getZhipu().setModel(model);
    }

    /**
     * 获取当前AI配置状态
     */
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("enabled", aiConfig.isEnabled());
        status.put("provider", aiConfig.getProvider());
        status.put("features", Map.of(
            "sqlGeneration", aiConfig.getFeatures().isSqlGeneration(),
            "dataAnalysis", aiConfig.getFeatures().isDataAnalysis(),
            "codeOptimization", aiConfig.getFeatures().isCodeOptimization()
        ));

        String provider = aiConfig.getProvider().toLowerCase();
        Map<String, Object> providerInfo = switch (provider) {
            case "openai" -> buildProviderInfo(aiConfig.getOpenai().getApiKey(), aiConfig.getOpenai().getBaseUrl(), aiConfig.getOpenai().getModel(), false);
            case "claude" -> buildProviderInfo(aiConfig.getClaude().getApiKey(), aiConfig.getClaude().getBaseUrl(), aiConfig.getClaude().getModel(), false);
            case "gemini" -> buildProviderInfo(aiConfig.getGemini().getApiKey(), aiConfig.getGemini().getBaseUrl(), aiConfig.getGemini().getModel(), false);
            case "qwen" -> buildProviderInfo(aiConfig.getQwen().getApiKey(), aiConfig.getQwen().getBaseUrl(), aiConfig.getQwen().getModel(), false);
            case "deepseek" -> buildProviderInfo(aiConfig.getDeepseek().getApiKey(), aiConfig.getDeepseek().getBaseUrl(), aiConfig.getDeepseek().getModel(), false);
            case "zhipu" -> buildProviderInfo(aiConfig.getZhipu().getApiKey(), aiConfig.getZhipu().getBaseUrl(), aiConfig.getZhipu().getModel(), false);
            case "ollama" -> buildProviderInfo(null, aiConfig.getOllama().getBaseUrl(), aiConfig.getOllama().getModel(), true);
            case "deepseek-local" -> buildProviderInfo(aiConfig.getDeepseekLocal().getApiKey(), aiConfig.getDeepseekLocal().getBaseUrl(), aiConfig.getDeepseekLocal().getModel(), true);
            default -> Map.of("configured", false, "baseUrl", "", "model", "");
        };
        boolean apiKeyConfigured = switch (provider) {
            case "openai" -> isValidApiKey(aiConfig.getOpenai().getApiKey());
            case "claude" -> isValidApiKey(aiConfig.getClaude().getApiKey());
            case "gemini" -> isValidApiKey(aiConfig.getGemini().getApiKey());
            case "qwen" -> isValidApiKey(aiConfig.getQwen().getApiKey());
            case "deepseek" -> isValidApiKey(aiConfig.getDeepseek().getApiKey());
            case "zhipu" -> isValidApiKey(aiConfig.getZhipu().getApiKey());
            case "ollama", "deepseek-local" -> true;
            default -> false;
        };
        status.put("apiKeyConfigured", apiKeyConfigured);
        status.put("providerConfigured", providerInfo.get("configured"));
        status.put("baseUrl", providerInfo.get("baseUrl"));
        status.put("model", providerInfo.get("model"));

        return status;
    }

    /**
     * 获取所有AI配置（多服务商独立配置，apiKey脱敏返回）
     */
    public Map<String, Object> getConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("activeProvider", aiConfig.getProvider());
        config.put("maxHistory", maxHistorySize);

        Map<String, Object> providers = new HashMap<>();
        providers.put("openai", buildProviderInfo(
            aiConfig.getOpenai().getApiKey(), aiConfig.getOpenai().getBaseUrl(), aiConfig.getOpenai().getModel(), false));
        providers.put("claude", buildProviderInfo(
            aiConfig.getClaude().getApiKey(), aiConfig.getClaude().getBaseUrl(), aiConfig.getClaude().getModel(), false));
        providers.put("gemini", buildProviderInfo(
            aiConfig.getGemini().getApiKey(), aiConfig.getGemini().getBaseUrl(), aiConfig.getGemini().getModel(), false));
        providers.put("qwen", buildProviderInfo(
            aiConfig.getQwen().getApiKey(), aiConfig.getQwen().getBaseUrl(), aiConfig.getQwen().getModel(), false));
        providers.put("deepseek", buildProviderInfo(
            aiConfig.getDeepseek().getApiKey(), aiConfig.getDeepseek().getBaseUrl(), aiConfig.getDeepseek().getModel(), false));
        providers.put("zhipu", buildProviderInfo(
            aiConfig.getZhipu().getApiKey(), aiConfig.getZhipu().getBaseUrl(), aiConfig.getZhipu().getModel(), false));
        providers.put("deepseek-local", buildProviderInfo(
            aiConfig.getDeepseekLocal().getApiKey(), aiConfig.getDeepseekLocal().getBaseUrl(), aiConfig.getDeepseekLocal().getModel(), true));
        providers.put("ollama", buildProviderInfo(
            null, aiConfig.getOllama().getBaseUrl(), aiConfig.getOllama().getModel(), true));
        config.put("providers", providers);
        return config;
    }

    private Map<String, Object> buildProviderInfo(String apiKey, String baseUrl, String model, boolean isLocal) {
        Map<String, Object> info = new HashMap<>();
        info.put("apiKey", maskApiKey(apiKey));
        info.put("baseUrl", baseUrl != null ? baseUrl : "");
        info.put("model", model != null ? model : "");
        info.put("configured", isLocal || isValidApiKey(apiKey));
        return info;
    }

    /**
     * 保存AI配置（持久化到数据库）
     */
    public void saveConfig(Map<String, Object> config) {
        String provider = (String) config.get("provider");
        String apiKey = (String) config.get("apiKey");
        String baseUrl = (String) config.get("baseUrl");
        String model = (String) config.get("model");
        Object maxHistoryObj = config.get("maxHistory");

        if (provider != null && !provider.isEmpty()) {
            aiConfig.setProvider(provider);
            saveToDb("provider", provider);
        }

        String providerLower = aiConfig.getProvider().toLowerCase();

        if (apiKey != null && !apiKey.isEmpty()) {
            switch (providerLower) {
                case "openai" -> { aiConfig.getOpenai().setApiKey(apiKey); saveToDb("openai_api_key", apiKey); }
                case "claude" -> { aiConfig.getClaude().setApiKey(apiKey); saveToDb("claude_api_key", apiKey); }
                case "gemini" -> { aiConfig.getGemini().setApiKey(apiKey); saveToDb("gemini_api_key", apiKey); }
                case "qwen" -> { aiConfig.getQwen().setApiKey(apiKey); saveToDb("qwen_api_key", apiKey); }
                case "deepseek" -> { aiConfig.getDeepseek().setApiKey(apiKey); saveToDb("deepseek_api_key", apiKey); }
                case "zhipu" -> { aiConfig.getZhipu().setApiKey(apiKey); saveToDb("zhipu_api_key", apiKey); }
                case "deepseek-local" -> { aiConfig.getDeepseekLocal().setApiKey(apiKey); saveToDb("deepseek_local_api_key", apiKey); }
            }
        }

        if (baseUrl != null && !baseUrl.isEmpty()) {
            switch (providerLower) {
                case "openai" -> { aiConfig.getOpenai().setBaseUrl(baseUrl); saveToDb("openai_base_url", baseUrl); }
                case "claude" -> { aiConfig.getClaude().setBaseUrl(baseUrl); saveToDb("claude_base_url", baseUrl); }
                case "gemini" -> { aiConfig.getGemini().setBaseUrl(baseUrl); saveToDb("gemini_base_url", baseUrl); }
                case "qwen" -> { aiConfig.getQwen().setBaseUrl(baseUrl); saveToDb("qwen_base_url", baseUrl); }
                case "deepseek" -> { aiConfig.getDeepseek().setBaseUrl(baseUrl); saveToDb("deepseek_base_url", baseUrl); }
                case "zhipu" -> { aiConfig.getZhipu().setBaseUrl(baseUrl); saveToDb("zhipu_base_url", baseUrl); }
                case "ollama" -> { aiConfig.getOllama().setBaseUrl(baseUrl); saveToDb("ollama_base_url", baseUrl); }
                case "deepseek-local" -> { aiConfig.getDeepseekLocal().setBaseUrl(baseUrl); saveToDb("deepseek_local_base_url", baseUrl); }
            }
        }

        if (model != null && !model.isEmpty()) {
            switch (providerLower) {
                case "openai" -> { aiConfig.getOpenai().setModel(model); saveToDb("openai_model", model); }
                case "claude" -> { aiConfig.getClaude().setModel(model); saveToDb("claude_model", model); }
                case "gemini" -> { aiConfig.getGemini().setModel(model); saveToDb("gemini_model", model); }
                case "qwen" -> { aiConfig.getQwen().setModel(model); saveToDb("qwen_model", model); }
                case "deepseek" -> { aiConfig.getDeepseek().setModel(model); saveToDb("deepseek_model", model); }
                case "zhipu" -> { aiConfig.getZhipu().setModel(model); saveToDb("zhipu_model", model); }
                case "ollama" -> { aiConfig.getOllama().setModel(model); saveToDb("ollama_model", model); }
                case "deepseek-local" -> { aiConfig.getDeepseekLocal().setModel(model); saveToDb("deepseek_local_model", model); }
            }
        }

        if (maxHistoryObj != null) {
            int newMaxHistory = maxHistoryObj instanceof Integer ? (Integer) maxHistoryObj : Integer.parseInt(maxHistoryObj.toString());
            maxHistorySize = newMaxHistory;
            saveToDb("max_history", String.valueOf(newMaxHistory));
        }

        log.info("AI配置已更新并持久化: provider={}", aiConfig.getProvider());
    }

    /**
     * 测试AI连接
     */
    public Map<String, Object> testConnection(Map<String, Object> config) {
        String provider = (String) config.get("provider");
        String apiKey = (String) config.get("apiKey");
        String baseUrl = (String) config.get("baseUrl");
        String model = (String) config.get("model");

        try {
            String testMessage = "Hello, please respond with 'OK' to confirm the connection is working.";
            String url;
            ObjectMapper objectMapper = providerClient.getObjectMapper();
            ObjectNode requestBody = objectMapper.createObjectNode();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            switch (provider.toLowerCase()) {
                case "openai" -> {
                    url = (baseUrl != null && !baseUrl.isEmpty() ? baseUrl : "https://api.openai.com/v1") + "/chat/completions";
                    requestBody.put("model", model != null ? model : "gpt-3.5-turbo");
                    requestBody.put("max_tokens", 10);
                    requestBody.putArray("messages").addObject().put("role", "user").put("content", testMessage);
                    headers.setBearerAuth(apiKey);
                }
                case "qwen" -> {
                    url = (baseUrl != null && !baseUrl.isEmpty() ? baseUrl : "https://dashscope.aliyuncs.com/api/v1") + "/services/aigc/text-generation/generation";
                    requestBody.put("model", model != null ? model : "qwen-turbo");
                    requestBody.putObject("input").putArray("messages").addObject().put("role", "user").put("content", testMessage);
                    headers.set("Authorization", "Bearer " + apiKey);
                }
                case "claude" -> {
                    url = (baseUrl != null && !baseUrl.isEmpty() ? baseUrl : "https://api.anthropic.com") + "/v1/messages";
                    requestBody.put("model", model != null ? model : "claude-sonnet-4-20250514");
                    requestBody.put("max_tokens", 10);
                    requestBody.putArray("messages").addObject().put("role", "user").put("content", testMessage);
                    headers.set("x-api-key", apiKey);
                    headers.set("anthropic-version", "2023-06-01");
                }
                case "gemini" -> {
                    String geminiModel = model != null ? model : "gemini-2.5-flash";
                    String geminiBase = baseUrl != null && !baseUrl.isEmpty() ? baseUrl : "https://generativelanguage.googleapis.com";
                    url = geminiBase + "/v1beta/models/" + geminiModel + ":generateContent?key=" + apiKey;
                    ObjectNode content = requestBody.putArray("contents").addObject();
                    content.put("role", "user");
                    content.putArray("parts").addObject().put("text", testMessage);
                }
                case "deepseek", "deepseek-local", "zhipu" -> {
                    String defaultUrl;
                    String defaultModel;
                    if ("deepseek-local".equals(provider)) {
                        defaultUrl = "http://localhost:8080/v1";
                        defaultModel = "deepseek-v4-flash";
                    } else if ("zhipu".equals(provider)) {
                        defaultUrl = "https://open.bigmodel.cn/api/paas/v4";
                        defaultModel = "glm-4-plus";
                    } else {
                        defaultUrl = "https://api.deepseek.com";
                        defaultModel = "deepseek-v4-flash";
                    }
                    url = (baseUrl != null && !baseUrl.isEmpty() ? baseUrl : defaultUrl) + "/chat/completions";
                    requestBody.put("model", model != null ? model : defaultModel);
                    requestBody.put("max_tokens", 10);
                    if ("deepseek".equals(provider)) {
                        requestBody.putObject("thinking").put("type", "disabled");
                        requestBody.put("reasoning_effort", "high");
                    }
                    requestBody.putArray("messages").addObject().put("role", "user").put("content", testMessage);
                    if (apiKey != null && !apiKey.isEmpty()) headers.setBearerAuth(apiKey);
                }
                case "ollama" -> {
                    url = (baseUrl != null && !baseUrl.isEmpty() ? baseUrl : "http://localhost:11434") + "/api/chat";
                    requestBody.put("model", model != null ? model : "llama3");
                    requestBody.put("stream", false);
                    requestBody.putArray("messages").addObject().put("role", "user").put("content", testMessage);
                }
                default -> { return Map.of("success", false, "error", "不支持的服务商: " + provider); }
            }

            RestTemplate restTemplate = new RestTemplate(); // 测试连接用临时实例，避免污染全局超时配置
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return Map.of("success", true, "message", "AI服务响应正常");
            } else {
                return Map.of("success", false, "error", "服务返回错误: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("测试AI连接失败: {}", e.getMessage());
            String errorMsg = e.getMessage();
            if (errorMsg.contains("Connection refused")) {
                errorMsg = "连接被拒绝，请检查服务地址和端口是否正确";
            } else if (errorMsg.contains("UnknownHostException")) {
                errorMsg = "无法解析主机名，请检查服务地址";
            } else if (errorMsg.contains("401") || errorMsg.contains("Unauthorized")) {
                errorMsg = "认证失败，请检查API密钥";
            }
            return Map.of("success", false, "error", errorMsg);
        }
    }

    // ============ 内部工具方法 ============

    private void saveToDb(String key, String value) {
        if (aiConfigMapper != null) {
            try {
                aiConfigMapper.updateConfig(key, value);
            } catch (Exception e) {
                log.warn("保存配置到数据库失败: key={}, error={}", key, e.getMessage());
            }
        }
    }

    private String getCurrentModel() {
        return switch (aiConfig.getProvider().toLowerCase()) {
            case "openai" -> aiConfig.getOpenai().getModel();
            case "claude" -> aiConfig.getClaude().getModel();
            case "gemini" -> aiConfig.getGemini().getModel();
            case "qwen" -> aiConfig.getQwen().getModel();
            case "deepseek" -> aiConfig.getDeepseek().getModel();
            case "zhipu" -> aiConfig.getZhipu().getModel();
            case "deepseek-local" -> aiConfig.getDeepseekLocal().getModel();
            case "ollama" -> aiConfig.getOllama().getModel();
            default -> "unknown";
        };
    }

    private String getCurrentBaseUrl() {
        return switch (aiConfig.getProvider().toLowerCase()) {
            case "openai" -> aiConfig.getOpenai().getBaseUrl();
            case "claude" -> aiConfig.getClaude().getBaseUrl();
            case "gemini" -> aiConfig.getGemini().getBaseUrl();
            case "qwen" -> aiConfig.getQwen().getBaseUrl();
            case "deepseek" -> aiConfig.getDeepseek().getBaseUrl();
            case "zhipu" -> aiConfig.getZhipu().getBaseUrl();
            case "deepseek-local" -> aiConfig.getDeepseekLocal().getBaseUrl();
            case "ollama" -> aiConfig.getOllama().getBaseUrl();
            default -> "";
        };
    }

    private String getCurrentApiKey() {
        return switch (aiConfig.getProvider().toLowerCase()) {
            case "openai" -> aiConfig.getOpenai().getApiKey();
            case "claude" -> aiConfig.getClaude().getApiKey();
            case "gemini" -> aiConfig.getGemini().getApiKey();
            case "qwen" -> aiConfig.getQwen().getApiKey();
            case "deepseek" -> aiConfig.getDeepseek().getApiKey();
            case "zhipu" -> aiConfig.getZhipu().getApiKey();
            case "deepseek-local" -> aiConfig.getDeepseekLocal().getApiKey();
            default -> null;
        };
    }

    /**
     * API密钥脱敏：显示前3位 + **** + 后4位
     */
    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) return "";
        int len = apiKey.length();
        if (len <= 8) return "****" + apiKey.substring(Math.max(0, len - 4));
        return apiKey.substring(0, 3) + "****" + apiKey.substring(len - 4);
    }

    private boolean isCurrentApiKeyConfigured() {
        return switch (aiConfig.getProvider().toLowerCase()) {
            case "openai" -> isValidApiKey(aiConfig.getOpenai().getApiKey());
            case "claude" -> isValidApiKey(aiConfig.getClaude().getApiKey());
            case "gemini" -> isValidApiKey(aiConfig.getGemini().getApiKey());
            case "qwen" -> isValidApiKey(aiConfig.getQwen().getApiKey());
            case "deepseek" -> isValidApiKey(aiConfig.getDeepseek().getApiKey());
            case "zhipu" -> isValidApiKey(aiConfig.getZhipu().getApiKey());
            case "deepseek-local", "ollama" -> true;
            default -> false;
        };
    }

    private boolean isValidApiKey(String apiKey) {
        return apiKey != null && !apiKey.isEmpty() && !apiKey.startsWith("your-");
    }
}
