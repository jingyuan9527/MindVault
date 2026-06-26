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

/**
 * AI 模型工厂
 *
 * 根据 ModelConfig 实体动态构建 Spring AI 的 ChatModel / EmbeddingModel 实例。
 * 支持的供应商：
 * - OPENAI / DEEPSEEK / ALIYUN / SILICONFLOW → OpenAiChatModel（兼容 OpenAI API 格式）
 * - OLLAMA → OllamaChatModel（本地模型）
 *
 * 对于 OpenAI 兼容供应商，默认 BaseURL 见下方常量定义；
 * 如果配置中 baseUrl 为空或空白，自动回退到对应供应商的默认地址。
 *
 * 注意：
 * - Ollama 使用 OllamaApi.builder() 构造（非 new OllamaApi(url)）
 * - 温度（temperature）可为 null（由模型自身决定默认值）
 */
@Component
public class AiModelFactory {

    private static final Logger log = LoggerFactory.getLogger(AiModelFactory.class);

    public static final String ALIYUN_DEFAULT_BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1";
    public static final String DEEPSEEK_DEFAULT_BASE_URL = "https://api.deepseek.com/v1";
    public static final String OPENAI_DEFAULT_BASE_URL = "https://api.openai.com/v1";
    public static final String SILICONFLOW_DEFAULT_BASE_URL = "https://api.siliconflow.cn/v1";

    /** 构建聊天模型（使用模型默认温度） */
    public ChatModel buildChatModel(ModelConfig config) {
        return buildChatModel(config, null);
    }

    /**
     * 构建聊天模型（可指定温度）
     * 供应商分支逻辑：
     * - OLLAMA → 使用 OllamaChatModel.Builder
     * - 其他 → 使用 OpenAiChatModel.Builder（统一兼容 OpenAI API）
     */
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

    /** 构建嵌入模型（语义搜索使用），供应商分支同 buildChatModel */
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

    /** 解析聊天模型的基础 URL，baseUrl 为空时使用供应商默认地址 */
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

    /** 解析嵌入模型的基础 URL，baseUrl 为空时使用供应商默认地址 */
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