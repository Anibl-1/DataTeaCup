package com.dataplatform.infra.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * AI服务配置类
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ai")
public class AiConfig {

    private String provider = "openai";
    private boolean enabled = true;
    private OpenAiConfig openai = new OpenAiConfig();
    private ClaudeConfig claude = new ClaudeConfig();
    private GeminiConfig gemini = new GeminiConfig();
    private QwenConfig qwen = new QwenConfig();
    private DeepSeekConfig deepseek = new DeepSeekConfig();
    private ZhipuConfig zhipu = new ZhipuConfig();
    private OllamaConfig ollama = new OllamaConfig();
    private DeepSeekLocalConfig deepseekLocal = new DeepSeekLocalConfig();
    private FeaturesConfig features = new FeaturesConfig();

    public void updateProviderConfig(String providerName, String apiKey, String baseUrl, String model) {
        switch (providerName) {
            case "openai":
                applyConfig(openai.getApiKey(), openai::setApiKey, apiKey);
                applyConfig(openai.getBaseUrl(), openai::setBaseUrl, baseUrl);
                applyConfig(openai.getModel(), openai::setModel, model);
                break;
            case "deepseek":
                applyConfig(deepseek.getApiKey(), deepseek::setApiKey, apiKey);
                applyConfig(deepseek.getBaseUrl(), deepseek::setBaseUrl, baseUrl);
                applyConfig(deepseek.getModel(), deepseek::setModel, model);
                break;
            case "claude":
                applyConfig(claude.getApiKey(), claude::setApiKey, apiKey);
                applyConfig(claude.getBaseUrl(), claude::setBaseUrl, baseUrl);
                applyConfig(claude.getModel(), claude::setModel, model);
                break;
            case "gemini":
                applyConfig(gemini.getApiKey(), gemini::setApiKey, apiKey);
                applyConfig(gemini.getBaseUrl(), gemini::setBaseUrl, baseUrl);
                applyConfig(gemini.getModel(), gemini::setModel, model);
                break;
            case "qwen":
                applyConfig(qwen.getApiKey(), qwen::setApiKey, apiKey);
                applyConfig(qwen.getModel(), qwen::setModel, model);
                break;
            case "zhipu":
                applyConfig(zhipu.getApiKey(), zhipu::setApiKey, apiKey);
                applyConfig(zhipu.getBaseUrl(), zhipu::setBaseUrl, baseUrl);
                applyConfig(zhipu.getModel(), zhipu::setModel, model);
                break;
            case "ollama":
                applyConfig(ollama.getBaseUrl(), ollama::setBaseUrl, baseUrl);
                applyConfig(ollama.getModel(), ollama::setModel, model);
                break;
            case "deepseekLocal":
                applyConfig(deepseekLocal.getApiKey(), deepseekLocal::setApiKey, apiKey);
                applyConfig(deepseekLocal.getBaseUrl(), deepseekLocal::setBaseUrl, baseUrl);
                applyConfig(deepseekLocal.getModel(), deepseekLocal::setModel, model);
                break;
        }
    }

    private void applyConfig(String current, java.util.function.Consumer<String> setter, String value) {
        if (value != null && !value.isEmpty()) {
            setter.accept(value);
        }
    }

    @Data
    public static class OpenAiConfig {
        private String apiKey;
        private String baseUrl = "https://api.openai.com/v1";
        private String model = "gpt-3.5-turbo";
        private int maxTokens = 4096;
        private double temperature = 0.7;
    }

    @Data
    public static class QwenConfig {
        private String apiKey;
        private String baseUrl = "https://dashscope.aliyuncs.com/api/v1";
        private String model = "qwen-turbo";
    }

    @Data
    public static class DeepSeekConfig {
        private String apiKey;
        private String baseUrl = "https://api.deepseek.com";
        private String model = "deepseek-v4-flash";
        private int maxTokens = 4096;
        private double temperature = 0.7;
        private String thinkingType = "disabled";
        private String reasoningEffort = "high";
    }

    @Data
    public static class OllamaConfig {
        private String baseUrl = "http://localhost:11434";
        private String model = "llama3";
    }

    @Data
    public static class DeepSeekLocalConfig {
        private String baseUrl = "http://localhost:8080/v1";
        private String model = "deepseek-v4-flash";
        private String apiKey = "";
        private int maxTokens = 8192;
        private double temperature = 0.7;
    }

    @Data
    public static class ClaudeConfig {
        private String apiKey;
        private String baseUrl = "https://api.anthropic.com";
        private String model = "claude-sonnet-4-20250514";
        private int maxTokens = 4096;
        private double temperature = 0.7;
    }

    @Data
    public static class GeminiConfig {
        private String apiKey;
        private String baseUrl = "https://generativelanguage.googleapis.com";
        private String model = "gemini-2.5-flash";
        private int maxTokens = 4096;
        private double temperature = 0.7;
    }

    @Data
    public static class ZhipuConfig {
        private String apiKey;
        private String baseUrl = "https://open.bigmodel.cn/api/paas/v4";
        private String model = "glm-4-plus";
        private int maxTokens = 4096;
        private double temperature = 0.7;
    }

    @Data
    public static class FeaturesConfig {
        private boolean sqlGeneration = true;
        private boolean dataAnalysis = true;
        private boolean codeOptimization = true;
    }
}
