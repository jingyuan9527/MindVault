package com.mindvault.knowledge;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindvault.agent.config.AgentConfig;
import com.mindvault.agent.config.AgentConfig.LlmEndpoint;
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
    private final AgentConfig agentConfig;
    private final ObjectMapper objectMapper;

    public SearchEnhanceService(KnowledgeService knowledgeService,
                                ModelConfigService modelConfigService,
                                AgentConfig agentConfig) {
        this.knowledgeService = knowledgeService;
        this.modelConfigService = modelConfigService;
        this.agentConfig = agentConfig;
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

        List<String> errors = new ArrayList<>();
        for (ModelConfig mc : chatModels) {
            try {
                LlmEndpoint endpoint = agentConfig.buildEndpoint(mc);
                RestClient client = RestClient.builder()
                        .baseUrl(endpoint.getFullUrl())
                        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .defaultHeader("Authorization", "Bearer " + endpoint.getApiKey())
                        .build();

                Map<String, Object> body = new LinkedHashMap<>();
                body.put("model", endpoint.getModelName());
                body.put("messages", List.of(Map.of("role", "user", "content", prompt)));
                body.put("temperature", 0.2);
                body.put("max_tokens", 100);

                String json = objectMapper.writeValueAsString(body);
                String response = client.post().body(json).retrieve().body(String.class);
                Map<?, ?> map = objectMapper.readValue(response, Map.class);
                String content = extractContent(map);
                if (content != null && !content.isBlank()) return content.trim();
            } catch (Exception e) {
                log.warn("查询重写失败 ({}): {}", mc.getModelName(), e.getMessage());
                errors.add(e.getMessage());
            }
        }
        log.warn("所有模型查询重写均失败: {}", String.join("; ", errors));
        return null;
    }

    private String generateHypotheticalDocument(String query) {
        List<ModelConfig> chatModels = modelConfigService.getAvailableChatModels();
        if (chatModels.isEmpty()) return null;

        String prompt = """
                你是一个知识库检索助手。用户提出了一个问题，请生成一段假设性的文档内容，
                这段内容应当包含回答该问题所需的关键信息。只返回文档内容本身，不要额外说明。

                用户问题: %s
                """.formatted(query);

        List<String> errors = new ArrayList<>();
        for (ModelConfig mc : chatModels) {
            try {
                LlmEndpoint endpoint = agentConfig.buildEndpoint(mc);
                RestClient client = RestClient.builder()
                        .baseUrl(endpoint.getFullUrl())
                        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .defaultHeader("Authorization", "Bearer " + endpoint.getApiKey())
                        .build();

                Map<String, Object> body = new LinkedHashMap<>();
                body.put("model", endpoint.getModelName());
                body.put("messages", List.of(Map.of("role", "user", "content", prompt)));
                body.put("temperature", 0.3);
                body.put("max_tokens", 500);

                String json = objectMapper.writeValueAsString(body);
                String response = client.post().body(json).retrieve().body(String.class);
                Map<?, ?> map = objectMapper.readValue(response, Map.class);
                String content = extractContent(map);
                if (content != null && !content.isBlank()) return content.trim();
            } catch (Exception e) {
                log.warn("HyDE 文档生成失败 ({}): {}", mc.getModelName(), e.getMessage());
                errors.add(e.getMessage());
            }
        }
        log.warn("所有模型 HyDE 生成均失败: {}", String.join("; ", errors));
        return null;
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
            ModelConfig mc = chatModels.get(0);
            LlmEndpoint endpoint = agentConfig.buildEndpoint(mc);
            RestClient client = RestClient.builder()
                    .baseUrl(endpoint.getFullUrl())
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .defaultHeader("Authorization", "Bearer " + endpoint.getApiKey())
                    .build();

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", endpoint.getModelName());
            body.put("messages", List.of(Map.of("role", "user", "content", sb.toString())));
            body.put("temperature", 0.1);
            body.put("max_tokens", 200);

            String json = objectMapper.writeValueAsString(body);
            String response = client.post().body(json).retrieve().body(String.class);
            Map<?, ?> map = objectMapper.readValue(response, Map.class);
            String content = extractContent(map);

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
}