package com.mindvault.content;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindvault.agent.config.AgentConfig;
import com.mindvault.agent.config.AgentConfig.LlmEndpoint;
import com.mindvault.knowledge.KnowledgeService;
import com.mindvault.knowledge.entity.Knowledge;
import com.mindvault.model.ModelConfigService;
import com.mindvault.model.entity.ModelConfig;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AutoProcessService {

    private static final Logger log = LoggerFactory.getLogger(AutoProcessService.class);

    private final ModelConfigService modelConfigService;
    private final AgentConfig agentConfig;
    private final KnowledgeService knowledgeService;
    private final ObjectMapper objectMapper;

    private volatile List<ModelEndpoint> modelEndpoints = List.of();

    public AutoProcessService(ModelConfigService modelConfigService,
                              AgentConfig agentConfig,
                              @Lazy KnowledgeService knowledgeService) {
        this.modelConfigService = modelConfigService;
        this.agentConfig = agentConfig;
        this.knowledgeService = knowledgeService;
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void init() {
        refreshModels();
    }

    public void refreshModels() {
        try {
            List<ModelConfig> models = modelConfigService.getAvailableChatModels();
            modelEndpoints = models.stream()
                    .map(mc -> new ModelEndpoint(agentConfig.buildEndpoint(mc)))
                    .toList();
            log.info("AutoProcessService 初始化完成，可用模型数: {}", modelEndpoints.size());
        } catch (Exception e) {
            log.warn("AutoProcessService 初始化失败: {}", e.getMessage());
        }
    }

    public void autoProcess(Long knowledgeId, String title, String content) {
        if (modelEndpoints.isEmpty()) {
            log.warn("未配置主模型，跳过自动处理: knowledgeId={}", knowledgeId);
            return;
        }

        String summary = generateSummary(title, content);
        String tagsJson = generateTags(title, content);

        if (summary != null || tagsJson != null) {
            try {
                Knowledge k = knowledgeService.getById(knowledgeId);
                if (summary != null) k.setSummary(summary);
                if (tagsJson != null) k.setTags(tagsJson);
                knowledgeService.updateKnowledge(knowledgeId, k);
                log.info("自动处理完成: knowledgeId={}", knowledgeId);
            } catch (Exception e) {
                log.error("自动处理保存失败: {}", e.getMessage());
            }
        }

        generateEmbedding(knowledgeId, title, content);
    }

    private String generateSummary(String title, String content) {
        try {
            String prompt = "请为以下内容生成一段简洁的中文摘要（50-100字），只返回摘要内容，不要额外说明。\n\n"
                    + "标题: " + title + "\n\n内容: " + truncate(content, 2000);
            return callLlmWithFailover(prompt);
        } catch (Exception e) {
            log.warn("生成摘要失败: {}", e.getMessage());
        }
        return null;
    }

    private String generateTags(String title, String content) {
        try {
            String prompt = "请为以下内容生成3-5个中文标签，以JSON数组格式返回，例如 [\"标签1\", \"标签2\", \"标签3\"]。\n"
                    + "只返回JSON数组，不要额外说明。\n\n标题: " + title + "\n\n内容: " + truncate(content, 2000);
            String result = callLlmWithFailover(prompt);
            if (result != null) {
                String cleaned = result.trim();
                if (cleaned.startsWith("[") && cleaned.endsWith("]")) {
                    return cleaned;
                }
            }
        } catch (Exception e) {
            log.warn("生成标签失败: {}", e.getMessage());
        }
        return null;
    }

    private void generateEmbedding(Long knowledgeId, String title, String content) {
        List<ModelConfig> embeddingModels = modelConfigService.getAvailableEmbeddingModels();
        if (embeddingModels.isEmpty()) {
            return;
        }

        ModelConfig embModel = embeddingModels.get(0);
        String text = (title + "\n" + content);
        if (text.length() > 8000) text = text.substring(0, 8000);

        try {
            String embedUrl = buildEmbeddingUrl(embModel);
            if (embedUrl == null) return;

            RestClient.Builder builder = RestClient.builder()
                    .baseUrl(embedUrl)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .defaultHeader("Authorization", "Bearer " + embModel.getApiKey());

            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", embModel.getModelName());

            if ("OLLAMA".equalsIgnoreCase(embModel.getProvider())) {
                requestBody.put("prompt", text);
            } else {
                requestBody.put("input", text);
            }

            String responseJson = builder.build().post()
                    .body(objectMapper.writeValueAsString(requestBody))
                    .retrieve()
                    .body(String.class);

            List<Double> vector = parseEmbeddingResponse(embModel.getProvider(), responseJson);
            if (vector != null && !vector.isEmpty()) {
                String vectorStr = "[" + vector.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(",")) + "]";
                knowledgeService.updateEmbedding(knowledgeId, vectorStr);
                log.info("嵌入向量生成完成: knowledgeId={}, dim={}", knowledgeId, vector.size());
            }
        } catch (Exception e) {
            log.warn("嵌入向量生成失败: knowledgeId={}, error={}", knowledgeId, e.getMessage());
        }
    }

    private String buildEmbeddingUrl(ModelConfig config) {
        return switch (config.getProvider().toUpperCase()) {
            case "ALIYUN" -> "https://dashscope.aliyuncs.com/compatible-mode/v1/embeddings";
            case "DEEPSEEK" -> (config.getBaseUrl() != null ? config.getBaseUrl() : "https://api.deepseek.com/v1") + "/embeddings";
            case "OPENAI" -> (config.getBaseUrl() != null ? config.getBaseUrl() : "https://api.openai.com/v1") + "/embeddings";
            case "OLLAMA" -> (config.getBaseUrl() != null ? config.getBaseUrl() : "http://localhost:11434") + "/api/embeddings";
            default -> null;
        };
    }

    @SuppressWarnings("unchecked")
    private List<Double> parseEmbeddingResponse(String provider, String json) {
        try {
            Map<String, Object> root = objectMapper.readValue(json, Map.class);
            if ("OLLAMA".equalsIgnoreCase(provider)) {
                if (root.containsKey("embedding")) {
                    return (List<Double>) root.get("embedding");
                }
            } else {
                List<Map<String, Object>> data = (List<Map<String, Object>>) root.get("data");
                if (data != null && !data.isEmpty()) {
                    return (List<Double>) data.get(0).get("embedding");
                }
            }
        } catch (Exception e) {
            log.warn("解析嵌入向量响应失败: {}", e.getMessage());
        }
        return null;
    }

    private String callLlmWithFailover(String prompt) {
        List<String> errors = new ArrayList<>();
        for (ModelEndpoint me : modelEndpoints) {
            try {
                RestClient client = RestClient.builder()
                        .baseUrl(me.endpoint.getFullUrl())
                        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .defaultHeader("Authorization", "Bearer " + me.endpoint.getApiKey())
                        .build();

                Map<String, Object> requestBody = new LinkedHashMap<>();
                requestBody.put("model", me.endpoint.getModelName());
                requestBody.put("messages", List.of(Map.of("role", "user", "content", prompt)));
                requestBody.put("temperature", 0.3);
                requestBody.put("max_tokens", 300);

                String responseJson = client.post()
                        .body(objectMapper.writeValueAsString(requestBody))
                        .retrieve()
                        .body(String.class);

                Map<?, ?> responseMap = objectMapper.readValue(responseJson, Map.class);
                String content = extractContent(responseMap);
                if (content != null) return content.trim();
            } catch (Exception e) {
                log.warn("模型调用失败: {}", e.getMessage());
                errors.add(e.getMessage());
            }
        }
        log.warn("所有模型均调用失败: {}", String.join("; ", errors));
        return null;
    }

    private String extractContent(Map<?, ?> responseMap) {
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

    private static String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen);
    }

    private record ModelEndpoint(LlmEndpoint endpoint) {}
}