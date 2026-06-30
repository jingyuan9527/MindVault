package com.mindvault.knowledge.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindvault.ai.client.AiModelFactory;
import com.mindvault.ai.client.AiService;
import com.mindvault.ai.prompt.PromptRegistry;
import com.mindvault.knowledge.config.SearchProperties;
import com.mindvault.model.entity.ModelConfig;
import com.mindvault.model.service.ModelConfigService;
import com.mindvault.systemconfig.service.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchEnhanceServiceImpl implements SearchEnhanceService {

    private static final Logger log = LoggerFactory.getLogger(SearchEnhanceServiceImpl.class);

    private final KnowledgeService knowledgeService;
    private final ModelConfigService modelConfigService;
    private final AiService aiService;
    private final AiModelFactory aiModelFactory;
    private final SystemConfigService systemConfigService;
    private final SearchProperties searchProperties;
    private final ObjectMapper objectMapper;

    public SearchEnhanceServiceImpl(KnowledgeService knowledgeService,
                                    ModelConfigService modelConfigService,
                                    AiService aiService,
                                    AiModelFactory aiModelFactory,
                                    SystemConfigService systemConfigService,
                                    SearchProperties searchProperties) {
        this.knowledgeService = knowledgeService;
        this.modelConfigService = modelConfigService;
        this.aiService = aiService;
        this.aiModelFactory = aiModelFactory;
        this.systemConfigService = systemConfigService;
        this.searchProperties = searchProperties;
        this.objectMapper = new ObjectMapper();
    }

    @Override
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

    @Override
    public List<Map<String, Object>> searchWithRewrite(String query, int topN) {
        return searchWithRewrite(query, topN, 0);
    }

    @Override
    public List<Map<String, Object>> searchWithRewrite(String query, int topN, int offset) {
        int safeOffset = Math.max(0, offset);
        int poolSize = topN + safeOffset;
        String rewritten = rewriteQuery(query);
        if (rewritten == null || rewritten.isBlank()) {
            // 改写不可用：直接取候选池后 skip
            List<Map<String, Object>> pool = knowledgeService.hybridSearch(query, poolSize);
            return pool.stream().skip(safeOffset).limit(topN).toList();
        }
        log.info("查询重写: '{}' → '{}'", query, rewritten);
        List<Map<String, Object>> results = knowledgeService.hybridSearch(rewritten, poolSize);
        List<Map<String, Object>> reranked = rerankResults(rewritten, results, poolSize);
        return reranked.stream().skip(safeOffset).limit(topN).toList();
    }

    private String rewriteQuery(String query) {
        try {
            modelConfigService.getPrimaryChatModel();
        } catch (Exception e) {
            return null;
        }

        String prompt = PromptRegistry.SEARCH_QUERY_REWRITE.resolve(systemConfigService, query);
        double temperature = searchProperties.getRewriteTemperature();
        int maxTokens = searchProperties.getRewriteMaxTokens();

        return aiService.call(prompt, temperature, maxTokens);
    }

    private String generateHypotheticalDocument(String query) {
        try {
            modelConfigService.getPrimaryChatModel();
        } catch (Exception e) {
            return null;
        }

        String prompt = PromptRegistry.SEARCH_HYDE.resolve(systemConfigService, query);
        double temperature = searchProperties.getHydeTemperature();
        int maxTokens = searchProperties.getHydeMaxTokens();

        return aiService.call(prompt, temperature, maxTokens);
    }

    private String generateEmbeddingForText(String text, ModelConfig embModel) {
        int embedTruncate = searchProperties.getHydeEmbeddingTruncate();
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

    @Override
    public List<Map<String, Object>> rerankResults(String query, List<Map<String, Object>> results, int topN) {
        if (results == null || results.size() <= 1) return results;

        try {
            modelConfigService.getPrimaryChatModel();
        } catch (Exception e) {
            return results;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            Map<String, Object> item = results.get(i);
            String title = (String) item.getOrDefault("title", "");
            String content = (String) item.getOrDefault("content", "");
            String summary = (String) item.getOrDefault("summary", "");
            String display = summary != null && !summary.isBlank() ? summary : content;
            int truncateLen = searchProperties.getRerankTruncateLength();
            if (display.length() > truncateLen) display = display.substring(0, truncateLen);
            sb.append("--- 结果 ").append(i + 1).append(" ---\n");
            sb.append("标题: ").append(title).append("\n");
            sb.append("内容: ").append(display).append("\n\n");
        }

        String prompt = PromptRegistry.SEARCH_RERANK.resolve(systemConfigService, query, sb.toString());

        try {
            double rerankTemp = searchProperties.getRerankTemperature();
            int rerankMaxTokens = searchProperties.getRerankMaxTokens();
            String content = aiService.call(prompt, rerankTemp, rerankMaxTokens);

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