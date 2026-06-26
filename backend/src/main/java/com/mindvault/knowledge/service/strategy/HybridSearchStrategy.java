package com.mindvault.knowledge.service.strategy;

import com.mindvault.ai.client.AiModelFactory;
import com.mindvault.knowledge.mapper.KnowledgeMapper;
import com.mindvault.model.entity.ModelConfig;
import com.mindvault.model.service.ModelConfigService;
import com.mindvault.systemconfig.service.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

@Component
public class HybridSearchStrategy implements SearchStrategy {

    private static final Logger log = LoggerFactory.getLogger(HybridSearchStrategy.class);

    private final KnowledgeMapper mapper;
    private final ModelConfigService modelConfigService;
    private final AiModelFactory aiModelFactory;
    private final SystemConfigService config;

    public HybridSearchStrategy(KnowledgeMapper mapper,
                                ModelConfigService modelConfigService,
                                AiModelFactory aiModelFactory,
                                SystemConfigService config) {
        this.mapper = mapper;
        this.modelConfigService = modelConfigService;
        this.aiModelFactory = aiModelFactory;
        this.config = config;
    }

    @Override
    public boolean isApplicable() {
        return hasEmbedding();
    }

    @Override
    public List<Map<String, Object>> search(String query, int limit) {
        return hybridSearchWithRerank(query, limit);
    }

    @Override
    public String name() {
        return "hybrid";
    }

    private boolean hasEmbedding() {
        try {
            return mapper.selectCount(null) > 0 && mapper.findFirstWithEmbedding() != null;
        } catch (Exception e) {
            return false;
        }
    }

    private List<Map<String, Object>> hybridSearchWithRerank(String query, int limit) {
        String embedding = generateEmbedding(query);
        if (embedding == null) return keywordSearchWithRank(query, limit);
        int multiplier = config.getInt("threshold.search.fetch-limit-multiplier", 3);
        int minFetch = config.getInt("threshold.search.min-fetch-limit", 20);
        int fetchLimit = Math.max(limit * multiplier, minFetch);
        List<Map<String, Object>> keywordResults = mapper.keywordSearchWithRank(query, fetchLimit);
        List<Map<String, Object>> vectorResults = mapper.findSimilarIds(embedding, fetchLimit);
        double k = config.getDouble("threshold.search.rrf-k", 60.0);
        Map<Long, Double> rrfScores = new LinkedHashMap<>();
        int rank = 1;
        for (Map<String, Object> row : keywordResults) {
            Long id = ((Number) row.get("id")).longValue();
            rrfScores.merge(id, 1.0 / (k + rank), Double::sum);
            rank++;
        }
        rank = 1;
        for (Map<String, Object> row : vectorResults) {
            Long id = ((Number) row.get("id")).longValue();
            rrfScores.merge(id, 1.0 / (k + rank), Double::sum);
            rank++;
        }
        List<Map.Entry<Long, Double>> sorted = rrfScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed()).limit(limit).toList();
        List<Long> ids = sorted.stream().map(Map.Entry::getKey).toList();
        Map<Long, com.mindvault.knowledge.entity.Knowledge> knowledgeMap = mapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(com.mindvault.knowledge.entity.Knowledge::getId, entry -> entry));
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<Long, Double> entry : sorted) {
            com.mindvault.knowledge.entity.Knowledge kn = knowledgeMap.get(entry.getKey());
            if (kn == null) continue;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", kn.getId()); item.put("title", kn.getTitle()); item.put("aiTitle", kn.getAiTitle());
            item.put("content", kn.getContent()); item.put("summary", kn.getSummary());
            item.put("tags", kn.getTags()); item.put("userTags", kn.getUserTags());
            item.put("contentType", kn.getContentType()); item.put("sourceUrl", kn.getSourceUrl());
            item.put("similarity", entry.getValue());
            item.put("createdAt", kn.getCreatedAt() != null ? kn.getCreatedAt().toString() : null);
            list.add(item);
        }
        log.info("混合搜索: query={}, 返回 {} 条结果 (RRF)", query, list.size());
        return list;
    }

    private List<Map<String, Object>> keywordSearchWithRank(String query, int limit) {
        List<com.mindvault.knowledge.entity.Knowledge> results = mapper.keywordSearch(query, limit);
        List<Map<String, Object>> list = new ArrayList<>();
        for (com.mindvault.knowledge.entity.Knowledge k : results) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", k.getId()); item.put("title", k.getTitle()); item.put("aiTitle", k.getAiTitle());
            item.put("content", k.getContent()); item.put("summary", k.getSummary());
            item.put("tags", k.getTags()); item.put("userTags", k.getUserTags());
            item.put("contentType", k.getContentType()); item.put("sourceUrl", k.getSourceUrl());
            item.put("createdAt", k.getCreatedAt());
            list.add(item);
        }
        return list;
    }

    private String generateEmbedding(String text) {
        List<ModelConfig> embeddingModels = modelConfigService.getAvailableEmbeddingModels();
        if (embeddingModels.isEmpty()) return null;
        ModelConfig embModel = embeddingModels.get(0);
        if (text.length() > 8000) text = text.substring(0, 8000);
        try {
            EmbeddingModel model = aiModelFactory.buildEmbeddingModel(embModel);
            float[] vector = model.embed(text);
            if (vector != null && vector.length > 0) {
                StringJoiner sj = new StringJoiner(",");
                for (float v : vector) sj.add(String.valueOf(v));
                return "[" + sj + "]";
            }
        } catch (Exception e) {
            log.warn("生成查询向量失败: {}", e.getMessage());
        }
        return null;
    }
}