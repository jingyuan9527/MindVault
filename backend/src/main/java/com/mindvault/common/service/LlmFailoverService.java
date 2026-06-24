package com.mindvault.common.service;

import com.mindvault.ai.client.AiModelFactory;
import com.mindvault.model.entity.ModelConfig;
import com.mindvault.tokenusage.TokenUsageService;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class LlmFailoverService {

    private static final Logger log = LoggerFactory.getLogger(LlmFailoverService.class);

    private final AiModelFactory aiModelFactory;
    private final MetricsService metricsService;
    private final TokenUsageService tokenUsageService;

    public LlmFailoverService(AiModelFactory aiModelFactory,
                              MetricsService metricsService,
                              TokenUsageService tokenUsageService) {
        this.aiModelFactory = aiModelFactory;
        this.metricsService = metricsService;
        this.tokenUsageService = tokenUsageService;
    }

    public record LlmCallOptions(
            String prompt,
            double temperature,
            int maxTokens,
            boolean recordMetrics,
            String source
    ) {
        public LlmCallOptions {
            if (source == null) source = "CHAT";
        }
    }

    public String call(List<ModelConfig> models, LlmCallOptions opts) {
        List<String> errors = new ArrayList<>();
        for (ModelConfig mc : models) {
            Timer.Sample sample = opts.recordMetrics ? metricsService.startLlmCall() : null;
            try {
                ChatModel chatModel = aiModelFactory.buildChatModel(mc);

                ChatOptions options = ChatOptions.builder()
                        .temperature(opts.temperature)
                        .maxTokens(opts.maxTokens)
                        .build();

                Prompt prompt = new Prompt(new UserMessage(opts.prompt()), options);
                ChatResponse response = chatModel.call(prompt);

                String content = response.getResult().getOutput().getText();
                if (content != null) {
                    if (opts.recordMetrics && sample != null) {
                        metricsService.recordLlmCallSuccess(sample, mc.getProvider(), mc.getModelName());
                    }
                    if (opts.source != null) {
                        recordUsage(mc, response.getMetadata(), opts.source);
                    }
                    return content.trim();
                }
            } catch (Exception e) {
                if (opts.recordMetrics) {
                    metricsService.recordLlmCallError(mc.getProvider(), mc.getModelName());
                }
                log.warn("模型 {} 调用失败: {}", mc.getModelName(), e.getMessage());
                errors.add(mc.getModelName() + ": " + e.getMessage());
            }
        }
        log.warn("所有模型均调用失败: {}", String.join("; ", errors));
        return null;
    }

    private void recordUsage(ModelConfig mc, ChatResponseMetadata metadata, String source) {
        try {
            var usage = metadata.getUsage();
            if (usage != null) {
                int promptTokens = usage.getPromptTokens() != null ? usage.getPromptTokens() : 0;
                int completionTokens = usage.getCompletionTokens() != null ? usage.getCompletionTokens() : 0;
                if (promptTokens > 0 || completionTokens > 0) {
                    metricsService.recordTokens(promptTokens, completionTokens);
                    tokenUsageService.recordUsage(mc, promptTokens, completionTokens, source, null);
                }
            }
        } catch (Exception e) {
            log.warn("记录 Token 用量失败: {}", e.getMessage());
        }
    }

    public static String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen);
    }
}