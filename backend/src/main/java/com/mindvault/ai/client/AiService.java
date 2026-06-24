package com.mindvault.ai.client;

import com.mindvault.common.service.MetricsService;
import com.mindvault.model.ModelConfigService;
import com.mindvault.model.entity.ModelConfig;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    private final ModelConfigService modelConfigService;
    private final AiModelFactory aiModelFactory;
    private final MetricsService metricsService;

    public AiService(ModelConfigService modelConfigService,
                     AiModelFactory aiModelFactory,
                     MetricsService metricsService) {
        this.modelConfigService = modelConfigService;
        this.aiModelFactory = aiModelFactory;
        this.metricsService = metricsService;
    }

    public String call(String prompt) {
        return call(prompt, 0.7, 2000);
    }

    public String call(String prompt, double temperature, int maxTokens) {
        try {
            ModelConfig model = modelConfigService.getPrimaryChatModel();
            ChatModel chatModel = aiModelFactory.buildChatModel(model, temperature);

            Timer.Sample sample = metricsService.startLlmCall();
            String content = chatModel.call(prompt);
            metricsService.recordLlmCallSuccess(sample, model.getProvider(), model.getModelName());
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