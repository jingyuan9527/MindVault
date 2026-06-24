package com.mindvault.knowledge;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindvault.ai.client.AiModelFactory;
import com.mindvault.ai.prompt.PromptRegistry;
import com.mindvault.common.service.LlmFailoverService;
import com.mindvault.knowledge.entity.Knowledge;
import com.mindvault.model.ModelConfigService;
import com.mindvault.model.entity.ModelConfig;
import com.mindvault.systemconfig.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchEnhanceService {

    private static final Logger log = LoggerFactory.getLogger(SearchEnhanceService.class);

    private final KnowledgeService knowledgeService;
    private final ModelConfigService modelConfigService;
    private final LlmFailoverService llmFailoverService;
    private final AiModelFactory aiModelFactory;
    private final SystemConfigService config;
    private final ObjectMapper objectMapper;

    public SearchEnhanceService(KnowledgeService knowledgeService,
                                ModelConfigService modelConfigService,
                                LlmFailoverService llmFailoverService,
                                AiModelFactory aiModelFactory,
                                SystemConfigService config) {
        this.knowledgeService = knowledgeService;
        this.modelConfigService = modelConfigService;
        this.llmFailoverService = llmFailoverService;
        this.aiModelFactory = aiModelFactory;
        this.config = config;
        this.objectMapper = new ObjectMapper();
    }

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

        String prompt = PromptRegistry.SEARCH_QUERY_REWRITE.resolve(config, query);
        double temperature = config.getDouble("threshold.search.rewrite-temperature", 0.2);
        int maxTokens = config.getInt("threshold.search.rewrite-max-tokens", 100);

        return llmFailoverService.call(chatModels, new LlmFailoverService.LlmCallOptions(prompt, temperature, maxTokens, false, null));
    }

    private String generateHypotheticalDocument(String query) {
        List<ModelConfig> chatModels = modelConfigService.getAvailableChatModels();
        if (chatModels.isEmpty()) return null;

        String prompt = PromptRegistry.SEARCH_HYDE.resolve(config, query);
        double temperature = config.getDouble("threshold.search.hyde-temperature", 0.3);
        int maxTokens = config.getInt("threshold.search.hyde-max-tokens", 500);

        return llmFailoverService.call(chatModels, new LlmFailoverService.LlmCallOptions(prompt, temperature, maxTokens, false, null));
    }

    private String generateEmbeddingForText(String text, ModelConfig embModel) {
        int embedTruncate = config.getInt("threshold.search.hyde-embedding-truncate", 8000);
        if (text.length() > embedTruncate) text = text.substring(0, embedTruncate);
        try {
            EmbeddingModel springAiModel = aiModelFactory.buildEmbeddingModel(embModel);
            float[] vector = springAiModel.embed(text);
            if (vector != null && vector.length > 0) {
                StringJoiner sj = new StringJoiner(",");
                        for (float v : vector) sj.add(String.valueOf(v));
                        return "[" + sj + "]";
            }
        } catch (Exception e) {
            log.warn("HyDE 向量生成失败: {}", e.getMessage());
        }
        return null;
    }

    public List<Map<String, Object>> rerankResults(String query, List<Map<String, Object>> results, int topN) {
        if (results == null || results.size() <= 1) return results;

        List<ModelConfig> chatModels = modelConfigService.getAvailableChatModels();
        if (chatModels.isEmpty()) return results;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            Map<String, Object> item = results.get(i);
            String title = (String) item.getOrDefault("title", "");
            String content = (String) item.getOrDefault("content", "");
            String summary = (String) item.getOrDefault("summary", "");
            String display = summary != null && !summary.isBlank() ? summary : content;
            int truncateLen = config.getInt("threshold.search.rerank-truncate-length", 300);
            if (display.length() > truncateLen) display = display.substring(0, truncateLen);
            sb.append("--- 结果 ").append(i + 1).append(" ---\n");
            sb.append("标题: ").append(title).append("\n");
            sb.append("内容: ").append(display).append("\n\n");
        }

        String prompt = PromptRegistry.SEARCH_RERANK.resolve(config, query, sb.toString());

        try {
            double rerankTemp = config.getDouble("threshold.search.rerank-temperature", 0.1);
            int rerankMaxTokens = config.getInt("threshold.search.rerank-max-tokens", 200);
            String content = llmFailoverService.call(chatModels, new LlmFailoverService.LlmCallOptions(prompt, rerankTemp, rerankMaxTokens, false, null));

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