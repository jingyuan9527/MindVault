package com.mindvault.knowledge.service.strategy;

import com.mindvault.ai.client.AiModelFactory;
import com.mindvault.knowledge.entity.Knowledge;
import com.mindvault.knowledge.mapper.KnowledgeMapper;
import com.mindvault.model.entity.ModelConfig;
import com.mindvault.model.service.ModelConfigService;
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
public class VectorSearchStrategy implements SearchStrategy {

    private static final Logger log = LoggerFactory.getLogger(VectorSearchStrategy.class);

    private final KnowledgeMapper mapper;
    private final ModelConfigService modelConfigService;
    private final AiModelFactory aiModelFactory;

    public VectorSearchStrategy(KnowledgeMapper mapper,
                                ModelConfigService modelConfigService,
                                AiModelFactory aiModelFactory) {
        this.mapper = mapper;
        this.modelConfigService = modelConfigService;
        this.aiModelFactory = aiModelFactory;
    }

    @Override
    public boolean isApplicable() {
        return hasEmbedding();
    }

    @Override
    public List<Map<String, Object>> search(String query, int limit) {
        String embedding = generateEmbedding(query);
        if (embedding == null) return List.of();
        return searchSimilar(embedding, limit);
    }

    @Override
    public String name() {
        return "vector";
    }

    private boolean hasEmbedding() {
        try {
            return mapper.selectCount(null) > 0 && mapper.findFirstWithEmbedding() != null;
        } catch (Exception e) {
            return false;
        }
    }

    private List<Map<String, Object>> searchSimilar(String embedding, int topN) {
        List<Map<String, Object>> results = mapper.findSimilarIds(embedding, topN);
        Map<Long, Double> similarityMap = new LinkedHashMap<>();
        for (Map<String, Object> row : results) {
            Long id = ((Number) row.get("id")).longValue();
            Double similarity = ((Number) row.get("similarity")).doubleValue();
            similarityMap.put(id, similarity);
        }
        List<Knowledge> knowledgeList = mapper.selectBatchIds(similarityMap.keySet());
        Map<Long, Knowledge> knowledgeMap = knowledgeList.stream().collect(Collectors.toMap(Knowledge::getId, k -> k));
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<Long, Double> entry : similarityMap.entrySet()) {
            Knowledge k = knowledgeMap.get(entry.getKey());
            if (k == null) continue;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", k.getId());
            item.put("title", k.getTitle());
            item.put("aiTitle", k.getAiTitle());
            item.put("content", k.getContent());
            item.put("similarity", entry.getValue());
            list.add(item);
        }
        log.info("语义搜索: topN={}, 返回 {} 条结果", topN, list.size());
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