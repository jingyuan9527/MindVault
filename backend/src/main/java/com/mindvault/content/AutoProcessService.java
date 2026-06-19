package com.mindvault.content;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindvault.common.service.LlmFailoverService;
import com.mindvault.knowledge.KnowledgeService;
import com.mindvault.knowledge.entity.Knowledge;
import com.mindvault.model.ModelConfigService;
import com.mindvault.model.entity.ModelConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map;

@Service
public class AutoProcessService {

    private static final Logger log = LoggerFactory.getLogger(AutoProcessService.class);

    private final ModelConfigService modelConfigService;
    private final LlmFailoverService llmFailoverService;
    private final KnowledgeService knowledgeService;
    private final ObjectMapper objectMapper;

    public AutoProcessService(ModelConfigService modelConfigService,
                              LlmFailoverService llmFailoverService,
                              @Lazy KnowledgeService knowledgeService) {
        this.modelConfigService = modelConfigService;
        this.llmFailoverService = llmFailoverService;
        this.knowledgeService = knowledgeService;
        this.objectMapper = new ObjectMapper();
    }

    public void autoProcess(Long knowledgeId, String title, String content) {
        List<ModelConfig> models = modelConfigService.getAvailableChatModels();
        if (models.isEmpty()) {
            log.warn("未配置主模型，跳过自动处理: knowledgeId={}", knowledgeId);
            return;
        }

        String summary = generateSummary(title, content, models);
        String tagsJson = generateTags(title, content, models);

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

    private String generateSummary(String title, String content, List<ModelConfig> models) {
        try {
            String prompt = "请为以下内容生成一段简洁的中文摘要（50-100字），只返回摘要内容，不要额外说明。\n\n"
                    + "标题: " + title + "\n\n内容: " + LlmFailoverService.truncate(content, 2000);
            return llmFailoverService.call(models, new LlmFailoverService.LlmCallOptions(prompt, 0.3, 300, false, null));
        } catch (Exception e) {
            log.warn("生成摘要失败: {}", e.getMessage());
        }
        return null;
    }

    private String generateTags(String title, String content, List<ModelConfig> models) {
        try {
            String prompt = "请为以下内容生成3-5个中文标签，以JSON数组格式返回，例如 [\"标签1\", \"标签2\", \"标签3\"]。\n"
                    + "只返回JSON数组，不要额外说明。\n\n标题: " + title + "\n\n内容: " + LlmFailoverService.truncate(content, 2000);
            String result = llmFailoverService.call(models, new LlmFailoverService.LlmCallOptions(prompt, 0.3, 300, false, null));
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
        if (embeddingModels.isEmpty()) return;

        ModelConfig embModel = embeddingModels.get(0);
        String text = (title + "\n" + content);
        if (text.length() > 8000) text = text.substring(0, 8000);

        try {
            String embedUrl = buildEmbeddingUrl(embModel);
            if (embedUrl == null) return;

            var builder = RestClient.builder()
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
}