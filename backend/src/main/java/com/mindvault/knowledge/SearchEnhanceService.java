package com.mindvault.knowledge;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindvault.common.service.LlmFailoverService;
import com.mindvault.knowledge.entity.Knowledge;
import com.mindvault.model.ModelConfigService;
import com.mindvault.model.entity.ModelConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchEnhanceService {

    private static final Logger log = LoggerFactory.getLogger(SearchEnhanceService.class);

    private final KnowledgeService knowledgeService;
    private final ModelConfigService modelConfigService;
    private final LlmFailoverService llmFailoverService;
    private final ObjectMapper objectMapper;

    public SearchEnhanceService(KnowledgeService knowledgeService,
                                ModelConfigService modelConfigService,
                                LlmFailoverService llmFailoverService) {
        this.knowledgeService = knowledgeService;
        this.modelConfigService = modelConfigService;
        this.llmFailoverService = llmFailoverService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * HyDE 搜索：先用 LLM 生成假设文档，再用其向量做相似搜索
     */
    public List<Map<String, Object>> hydeSearch(String query, int topN) {
        String hypotheticalDoc = generateHypotheticalDocument(query);
        if (hypotheticalDoc == null) {
            return knowledgeService.hybridSearch(query, topN);
        }

        List<ModelConfig> embeddingModels = modelConfigService.getAvailableEmbeddingModels();
        if (embeddingModels.isEmpty()) {
            return knowledgeService.hybridSearch(query, topN);
        }

        String embedding = generateEmbeddingForText(hypotheticalDoc, embeddingModels.get(0));
        if (embedding == null) {
            return knowledgeService.hybridSearch(query, topN);
        }

        List<Map<String, Object>> results = knowledgeService.searchSimilar(embedding, topN);
        log.info("HyDE 搜索完成: query={}, 返回 {} 条", query, results.size());
        return results;
    }

    /**
     * 查询重写：让 LLM 将用户原始问题改写为更利于检索的形式
     */
    public List<Map<String, Object>> searchWithRewrite(String query, int topN) {
        String rewritten = rewriteQuery(query);
        if (rewritten == null || rewritten.isBlank()) {
            return knowledgeService.hybridSearch(query, topN);
        }
        log.info("查询重写: '{}' → '{}'", query, rewritten);
        List<Map<String, Object>> results = knowledgeService.hybridSearch(rewritten, topN);
        List<Map<String, Object>> reranked = rerankResults(rewritten, results, topN);
        return reranked;
    }

    private String rewriteQuery(String query) {
        List<ModelConfig> chatModels = modelConfigService.getAvailableChatModels();
        if (chatModels.isEmpty()) return null;

        String prompt = """
                你是一个搜索查询优化助手。请将用户的原始问题改写成更适合向量检索和关键词检索的形式。
                要求：
                1. 提取核心关键词和实体
                2. 补充同义词或相关术语
                3. 保持简洁，长度不超过50字
                4. 只返回改写后的查询文本，不要额外说明

                原始问题: %s
                """.formatted(query);

        return llmFailoverService.call(chatModels, new LlmFailoverService.LlmCallOptions(prompt, 0.2, 100, false, null));
    }

    private String generateHypotheticalDocument(String query) {
        List<ModelConfig> chatModels = modelConfigService.getAvailableChatModels();
        if (chatModels.isEmpty()) return null;

        String prompt = """
                你是一个知识库检索助手。用户提出了一个问题，请生成一段假设性的文档内容，
                这段内容应当包含回答该问题所需的关键信息。只返回文档内容本身，不要额外说明。

                用户问题: %s
                """.formatted(query);

        return llmFailoverService.call(chatModels, new LlmFailoverService.LlmCallOptions(prompt, 0.3, 500, false, null));
    }

    private String generateEmbeddingForText(String text, ModelConfig embModel) {
        if (text.length() > 8000) text = text.substring(0, 8000);
        try {
            String embedUrl = switch (embModel.getProvider().toUpperCase()) {
                case "ALIYUN" -> "https://dashscope.aliyuncs.com/compatible-mode/v1/embeddings";
                case "DEEPSEEK" -> (embModel.getBaseUrl() != null ? embModel.getBaseUrl() : "https://api.deepseek.com/v1") + "/embeddings";
                case "OPENAI" -> (embModel.getBaseUrl() != null ? embModel.getBaseUrl() : "https://api.openai.com/v1") + "/embeddings";
                case "OLLAMA" -> (embModel.getBaseUrl() != null ? embModel.getBaseUrl() : "http://localhost:11434") + "/api/embeddings";
                default -> null;
            };
            if (embedUrl == null) return null;

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

            Map<String, Object> root = objectMapper.readValue(responseJson,
                    new TypeReference<Map<String, Object>>() {});
            List<Double> vector;
            if ("OLLAMA".equalsIgnoreCase(embModel.getProvider())) {
                vector = (List<Double>) root.get("embedding");
            } else {
                List<Map<String, Object>> data = (List<Map<String, Object>>) root.get("data");
                if (data != null && !data.isEmpty()) {
                    vector = (List<Double>) data.get(0).get("embedding");
                } else {
                    return null;
                }
            }
            if (vector != null && !vector.isEmpty()) {
                return "[" + vector.stream().map(String::valueOf).collect(Collectors.joining(",")) + "]";
            }
        } catch (Exception e) {
            log.warn("HyDE 向量生成失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * LLM 重排序：对搜索结果用 LLM 进行相关性评分并重新排序
     */
    public List<Map<String, Object>> rerankResults(String query, List<Map<String, Object>> results, int topN) {
        if (results == null || results.size() <= 1) return results;

        List<ModelConfig> chatModels = modelConfigService.getAvailableChatModels();
        if (chatModels.isEmpty()) return results;

        StringBuilder sb = new StringBuilder();
        sb.append("请评估以下搜索结果与用户查询的相关性。对每条结果给出 0-10 的分数（10 最相关）。\n\n");
        sb.append("用户查询: ").append(query).append("\n\n搜索结果:\n");

        for (int i = 0; i < results.size(); i++) {
            Map<String, Object> item = results.get(i);
            String title = (String) item.getOrDefault("title", "");
            String content = (String) item.getOrDefault("content", "");
            String summary = (String) item.getOrDefault("summary", "");
            String display = summary != null && !summary.isBlank() ? summary : content;
            if (display.length() > 300) display = display.substring(0, 300);
            sb.append("--- 结果 ").append(i + 1).append(" ---\n");
            sb.append("标题: ").append(title).append("\n");
            sb.append("内容: ").append(display).append("\n\n");
        }

        sb.append("请只返回 JSON 数组格式的评分，例如 [9, 5, 7]，不要额外说明。");

        try {
            String content = llmFailoverService.call(chatModels, new LlmFailoverService.LlmCallOptions(sb.toString(), 0.1, 200, false, null));

            if (content != null) {
                String cleaned = content.trim();
                int start = cleaned.indexOf('[');
                int end = cleaned.lastIndexOf(']');
                if (start >= 0 && end > start) {
                    cleaned = cleaned.substring(start, end + 1);
                    List<Integer> scores = objectMapper.readValue(cleaned,
                            new TypeReference<List<Integer>>() {});
                    if (scores.size() == results.size()) {
                        List<Map.Entry<Map<String, Object>, Integer>> scored = new ArrayList<>();
                        for (int i = 0; i < results.size(); i++) {
                            scored.add(Map.entry(results.get(i), scores.get(i)));
                        }
                        scored.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
                        List<Map<String, Object>> reranked = scored.stream()
                                .limit(topN)
                                .map(Map.Entry::getKey)
                                .toList();
                        log.info("LLM 重排序完成: {} 条 → {} 条", results.size(), reranked.size());
                        return reranked;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("LLM 重排序失败: {}", e.getMessage());
        }
        return results.stream().limit(topN).toList();
    }
}