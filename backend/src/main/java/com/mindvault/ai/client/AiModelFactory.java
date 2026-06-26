package com.mindvault.ai.client;

import com.mindvault.model.entity.ModelConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.stereotype.Component;

@Component
public class AiModelFactory {

    private static final Logger log = LoggerFactory.getLogger(AiModelFactory.class);

    public static final String ALIYUN_DEFAULT_BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1";
    public static final String DEEPSEEK_DEFAULT_BASE_URL = "https://api.deepseek.com/v1";
    public static final String OPENAI_DEFAULT_BASE_URL = "https://api.openai.com/v1";
    public static final String SILICONFLOW_DEFAULT_BASE_URL = "https://api.siliconflow.cn/v1";

    public ChatModel buildChatModel(ModelConfig config) {
        return buildChatModel(config, null);
    }

    public ChatModel buildChatModel(ModelConfig config, Double temperature) {
        String provider = config.getProvider().toUpperCase();
        String baseUrl = config.getBaseUrl();
        String apiKey = config.getApiKey();
        String modelName = config.getModelName();

        if ("OLLAMA".equals(provider)) {
            String url = (baseUrl != null && !baseUrl.isBlank()) ? baseUrl : "http://localhost:11434";
            OllamaApi api = OllamaApi.builder().baseUrl(url).build();
            OllamaChatOptions.Builder opts = OllamaChatOptions.builder().model(modelName);
            if (temperature != null) opts.temperature(temperature);
            return OllamaChatModel.builder()
                    .ollamaApi(api)
                    .options(opts.build())
                    .build();
        }

        String endpointUrl = resolveChatBaseUrl(provider, baseUrl);
        OpenAiChatOptions.Builder opts = OpenAiChatOptions.builder()
                .baseUrl(endpointUrl)
                .apiKey(apiKey)
                .model(modelName);
        if (temperature != null) opts.temperature(temperature);
        return OpenAiChatModel.builder()
                .options(opts.build())
                .build();
    }

    public EmbeddingModel buildEmbeddingModel(ModelConfig config) {
        String provider = config.getProvider().toUpperCase();
        String baseUrl = config.getBaseUrl();
        String apiKey = config.getApiKey();
        String modelName = config.getModelName();

        if ("OLLAMA".equals(provider)) {
            String url = (baseUrl != null && !baseUrl.isBlank()) ? baseUrl : "http://localhost:11434";
            OllamaApi api = OllamaApi.builder().baseUrl(url).build();
            return OllamaEmbeddingModel.builder()
                    .ollamaApi(api)
                    .options(OllamaEmbeddingOptions.builder()
                            .model(modelName)
                            .build())
                    .build();
        }

        String endpointUrl = resolveEmbeddingBaseUrl(provider, baseUrl);
        return OpenAiEmbeddingModel.builder()
                .options(OpenAiEmbeddingOptions.builder()
                        .baseUrl(endpointUrl)
                        .apiKey(apiKey)
                        .model(modelName)
                        .build())
                .build();
    }

    private String resolveChatBaseUrl(String provider, String baseUrl) {
        String effective = (baseUrl != null && !baseUrl.isBlank()) ? baseUrl : null;
        return switch (provider) {
            case "ALIYUN" -> effective != null ? effective : ALIYUN_DEFAULT_BASE_URL;
            case "DEEPSEEK" -> effective != null ? effective : DEEPSEEK_DEFAULT_BASE_URL;
            case "OPENAI" -> effective != null ? effective : OPENAI_DEFAULT_BASE_URL;
            case "SILICONFLOW" -> effective != null ? effective : SILICONFLOW_DEFAULT_BASE_URL;
            default -> effective;
        };
    }

    private String resolveEmbeddingBaseUrl(String provider, String baseUrl) {
        String effective = (baseUrl != null && !baseUrl.isBlank()) ? baseUrl : null;
        return switch (provider) {
            case "ALIYUN" -> effective != null ? effective : ALIYUN_DEFAULT_BASE_URL;
            case "DEEPSEEK" -> effective != null ? effective : DEEPSEEK_DEFAULT_BASE_URL;
            case "OPENAI" -> effective != null ? effective : OPENAI_DEFAULT_BASE_URL;
            case "SILICONFLOW" -> effective != null ? effective : SILICONFLOW_DEFAULT_BASE_URL;
            default -> effective;
        };
    }
}