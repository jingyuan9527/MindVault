package com.mindvault.content;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindvault.auto.AutoProcessLogMapper;
import com.mindvault.auto.entity.AutoProcessLog;
import com.mindvault.common.service.LlmFailoverService;
import com.mindvault.knowledge.KnowledgeService;
import com.mindvault.model.ModelConfigService;
import com.mindvault.model.entity.ModelConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AutoProcessService {

    private static final Logger log = LoggerFactory.getLogger(AutoProcessService.class);

    private final ModelConfigService modelConfigService;
    private final LlmFailoverService llmFailoverService;
    private final KnowledgeService knowledgeService;
    private final AutoProcessLogMapper logMapper;
    private final ObjectMapper objectMapper;

    public AutoProcessService(ModelConfigService modelConfigService,
                              LlmFailoverService llmFailoverService,
                              KnowledgeService knowledgeService,
                              AutoProcessLogMapper logMapper) {
        this.modelConfigService = modelConfigService;
        this.llmFailoverService = llmFailoverService;
        this.knowledgeService = knowledgeService;
        this.logMapper = logMapper;
        this.objectMapper = new ObjectMapper();
    }

    @Async
    public void autoProcessAsync(Long knowledgeId, String userTitle, String content) {
        autoProcess(knowledgeId, userTitle, content);
    }

    public void autoProcess(Long knowledgeId, String userTitle, String content) {
        List<ModelConfig> models = modelConfigService.getAvailableChatModels();
        if (models.isEmpty()) {
            log.warn("未配置主模型，跳过自动处理: knowledgeId={}", knowledgeId);
            return;
        }

        LocalDateTime startedAt = LocalDateTime.now();
        String aiTitle = generateAiTitle(userTitle, content, models);
        String tagsJson = generateTags(userTitle, content, models);
        String summary = generateSummary(userTitle, content, models);

        if (aiTitle != null || tagsJson != null || summary != null) {
            try {
                if (aiTitle != null) {
                    knowledgeService.updateAiFields(knowledgeId, aiTitle, tagsJson);
                } else if (tagsJson != null) {
                    knowledgeService.updateAiFields(knowledgeId, null, tagsJson);
                }
                if (summary != null) {
                    var k = knowledgeService.getById(knowledgeId);
                    k.setSummary(summary);
                    knowledgeService.updateKnowledge(knowledgeId, k);
                }
                knowledgeService.updateAutoProcessStatus(knowledgeId, "TITLE_TAG_DONE");
                log.info("R1 自动处理完成: knowledgeId={}, aiTitle={}", knowledgeId, aiTitle);
                saveLog(knowledgeId, "R1_TITLE_TAG", "SUCCESS", startedAt,
                        "aiTitle=" + aiTitle + ", tags=" + tagsJson + ", summary=" + (summary != null ? "ok" : "skip"),
                        0, 0, null);
            } catch (Exception e) {
                log.error("R1 自动处理保存失败: {}", e.getMessage());
                saveLog(knowledgeId, "R1_TITLE_TAG", "FAILED", startedAt, null, 0, 0, e.getMessage());
            }
        }

        generateEmbedding(knowledgeId, userTitle, content);
    }

    private String generateAiTitle(String userTitle, String content, List<ModelConfig> models) {
        try {
            String prompt = "请根据以下内容生成一个简洁准确的中文标题（10-20字），只返回标题内容，不要额外说明。\n\n"
                    + "原始标题: " + userTitle + "\n\n内容: " + LlmFailoverService.truncate(content, 2000);
            return llmFailoverService.call(models, new LlmFailoverService.LlmCallOptions(prompt, 0.3, 100, false, null));
        } catch (Exception e) {
            log.warn("生成 AI 标题失败: {}", e.getMessage());
        }
        return null;
    }

    private String generateSummary(String userTitle, String content, List<ModelConfig> models) {
        try {
            String prompt = "请为以下内容生成一段简洁的中文摘要（50-100字），只返回摘要内容，不要额外说明。\n\n"
                    + "标题: " + userTitle + "\n\n内容: " + LlmFailoverService.truncate(content, 2000);
            return llmFailoverService.call(models, new LlmFailoverService.LlmCallOptions(prompt, 0.3, 300, false, null));
        } catch (Exception e) {
            log.warn("生成摘要失败: {}", e.getMessage());
        }
        return null;
    }

    private String generateTags(String userTitle, String content, List<ModelConfig> models) {
        try {
            String prompt = "请为以下内容生成3-5个中文标签，以JSON数组格式返回，例如 [\"标签1\", \"标签2\", \"标签3\"]。\n"
                    + "只返回JSON数组，不要额外说明。\n\n标题: " + userTitle + "\n\n内容: " + LlmFailoverService.truncate(content, 2000);
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

    private void generateEmbedding(Long knowledgeId, String userTitle, String content) {
        List<ModelConfig> embeddingModels = modelConfigService.getAvailableEmbeddingModels();
        if (embeddingModels.isEmpty()) return;

        ModelConfig embModel = embeddingModels.get(0);
        String text = (userTitle + "\n" + content);
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
                String vectorStr = "[" + vector.stream().map(String::valueOf).collect(Collectors.joining(",")) + "]";
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

    private void saveLog(Long knowledgeId, String round, String status, LocalDateTime startedAt,
                         String resultSummary, int tokens, int durationMs, String errorMessage) {
        try {
            AutoProcessLog l = new AutoProcessLog();
            l.setKnowledgeId(knowledgeId);
            l.setRound(round);
            l.setStatus(status);
            l.setResultSummary(resultSummary);
            l.setLlmTokens(tokens);
            l.setLlmDurationMs(durationMs);
            l.setErrorMessage(errorMessage);
            l.setStartedAt(startedAt);
            l.setCompletedAt(LocalDateTime.now());
            l.setCreatedAt(LocalDateTime.now());
            logMapper.insert(l);
        } catch (Exception e) {
            log.warn("保存自动处理日志失败: {}", e.getMessage());
        }
    }
}
