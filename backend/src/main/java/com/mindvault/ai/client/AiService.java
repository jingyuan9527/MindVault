package com.mindvault.ai.client;

import com.mindvault.common.service.MetricsService;
import com.mindvault.model.ModelConfigService;
import com.mindvault.model.entity.ModelConfig;
import com.mindvault.tokenusage.TokenUsageService;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    private final ModelConfigService modelConfigService;
    private final AiModelFactory aiModelFactory;
    private final MetricsService metricsService;
    private final TokenUsageService tokenUsageService;

    public AiService(ModelConfigService modelConfigService,
                     AiModelFactory aiModelFactory,
                     MetricsService metricsService,
                     TokenUsageService tokenUsageService) {
        this.modelConfigService = modelConfigService;
        this.aiModelFactory = aiModelFactory;
        this.metricsService = metricsService;
        this.tokenUsageService = tokenUsageService;
    }

    public String call(String prompt) {
        return call(prompt, 0.7, 2000, "UNKNOWN");
    }

    public String call(String prompt, double temperature, int maxTokens) {
        return call(prompt, temperature, maxTokens, "UNKNOWN");
    }

    public String call(String prompt, double temperature, int maxTokens, String source) {
        try {
            ModelConfig model = modelConfigService.getPrimaryChatModel();
            ChatModel chatModel = aiModelFactory.buildChatModel(model, temperature);

            Timer.Sample sample = metricsService.startLlmCall();
            ChatResponse response = chatModel.call(new Prompt(new UserMessage(prompt)));
            metricsService.recordLlmCallSuccess(sample, model.getProvider(), model.getModelName());

            String content = response.getResult().getOutput().getText();

            var usage = response.getMetadata().getUsage();
            if (usage != null) {
                int promptTokens = usage.getPromptTokens() != null ? usage.getPromptTokens() : 0;
                int completionTokens = usage.getCompletionTokens() != null ? usage.getCompletionTokens() : 0;
                tokenUsageService.recordUsage(model, promptTokens, completionTokens, source, null);
            }

            return content;
        } catch (Exception e) {
            log.warn("AI 调用失败: {}", e.getMessage());
            return null;
        }
    }

    public static String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen);
    }
}