package com.mindvault.agent.config;

import com.mindvault.model.entity.ModelConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AgentConfig {

    private static final Logger log = LoggerFactory.getLogger(AgentConfig.class);

    public LlmEndpoint buildEndpoint(ModelConfig config) {
        String baseUrl;
        String apiPath;

        switch (config.getProvider().toUpperCase()) {
            case "ALIYUN":
                baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
                apiPath = "/chat/completions";
                break;
            case "DEEPSEEK":
                baseUrl = "https://api.deepseek.com/v1";
                apiPath = "/chat/completions";
                break;
            case "OPENAI":
                baseUrl = config.getBaseUrl() != null ? config.getBaseUrl() : "https://api.openai.com/v1";
                apiPath = "/chat/completions";
                break;
            case "OLLAMA":
                baseUrl = config.getBaseUrl() != null ? config.getBaseUrl() : "http://localhost:11434";
                apiPath = "/api/chat";
                break;
            default:
                baseUrl = config.getBaseUrl();
                apiPath = "/v1/chat/completions";
        }

        LlmEndpoint endpoint = new LlmEndpoint();
        endpoint.setBaseUrl(baseUrl);
        endpoint.setApiPath(apiPath);
        endpoint.setApiKey(config.getApiKey());
        endpoint.setModelName(config.getModelName());

        log.info("构建 LLM 端点: provider={}, model={}, url={}/{}",
                config.getProvider(), config.getModelName(), baseUrl, apiPath);
        return endpoint;
    }

    public static class LlmEndpoint {
        private String baseUrl;
        private String apiPath;
        private String apiKey;
        private String modelName;

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getApiPath() { return apiPath; }
        public void setApiPath(String apiPath) { this.apiPath = apiPath; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getModelName() { return modelName; }
        public void setModelName(String modelName) { this.modelName = modelName; }

        public String getFullUrl() {
            return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) + apiPath : baseUrl + apiPath;
        }
    }
}