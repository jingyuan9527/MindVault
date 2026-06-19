package com.mindvault.common.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindvault.agent.config.AgentConfig;
import com.mindvault.model.entity.ModelConfig;
import com.mindvault.tokenusage.TokenUsageService;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LlmFailoverService {

    private static final Logger log = LoggerFactory.getLogger(LlmFailoverService.class);

    private final AgentConfig agentConfig;
    private final ObjectMapper objectMapper;
    private final MetricsService metricsService;
    private final TokenUsageService tokenUsageService;

    public LlmFailoverService(AgentConfig agentConfig, ObjectMapper objectMapper,
                              MetricsService metricsService, TokenUsageService tokenUsageService) {
        this.agentConfig = agentConfig;
        this.objectMapper = objectMapper;
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
                AgentConfig.LlmEndpoint ep = agentConfig.buildEndpoint(mc);
                RestClient client = RestClient.builder()
                        .baseUrl(ep.getFullUrl())
                        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .defaultHeader("Authorization", "Bearer " + ep.getApiKey())
                        .build();

                Map<String, Object> requestBody = new LinkedHashMap<>();
                requestBody.put("model", ep.getModelName());
                requestBody.put("messages", List.of(Map.of("role", "user", "content", opts.prompt)));
                requestBody.put("temperature", opts.temperature);
                requestBody.put("max_tokens", opts.maxTokens);

                String responseJson = client.post()
                        .body(objectMapper.writeValueAsString(requestBody))
                        .retrieve()
                        .body(String.class);

                Map<?, ?> responseMap = objectMapper.readValue(responseJson, Map.class);
                String content = extractContent(responseMap);
                if (content != null) {
                    if (opts.recordMetrics && sample != null) {
                        metricsService.recordLlmCallSuccess(sample, mc.getProvider(), ep.getModelName());
                    }
                    if (opts.source != null) {
                        recordUsage(mc, responseMap, opts.source);
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

    @SuppressWarnings("unchecked")
    private void recordUsage(ModelConfig mc, Map<?, ?> responseMap, String source) {
        try {
            Map<String, Object> usage = (Map<String, Object>) responseMap.get("usage");
            if (usage != null) {
                int promptTokens = ((Number) usage.getOrDefault("prompt_tokens", 0)).intValue();
                int completionTokens = ((Number) usage.getOrDefault("completion_tokens", 0)).intValue();
                metricsService.recordTokens(promptTokens, completionTokens);
                tokenUsageService.recordUsage(mc, promptTokens, completionTokens, source, null);
            }
        } catch (Exception e) {
            log.warn("记录 Token 用量失败: {}", e.getMessage());
        }
    }

    public static String extractContent(Map<?, ?> responseMap) {
        if (responseMap.containsKey("choices")) {
            List<?> choices = (List<?>) responseMap.get("choices");
            if (!choices.isEmpty()) {
                Map<?, ?> choice = (Map<?, ?>) choices.get(0);
                Map<?, ?> message = (Map<?, ?>) choice.get("message");
                if (message != null && message.get("content") instanceof String s) return s;
                if (choice.get("text") instanceof String s) return s;
            }
        }
        if (responseMap.containsKey("message")) {
            Map<?, ?> message = (Map<?, ?>) responseMap.get("message");
            if (message.get("content") instanceof String s) return s;
        }
        if (responseMap.containsKey("response")) {
            Object resp = responseMap.get("response");
            if (resp instanceof String s) return s;
        }
        return null;
    }

    public static String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen);
    }
}
