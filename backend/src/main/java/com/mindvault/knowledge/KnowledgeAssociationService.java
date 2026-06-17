package com.mindvault.knowledge;

import com.mindvault.knowledge.entity.Knowledge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class KnowledgeAssociationService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeAssociationService.class);

    private final KnowledgeRepository repository;
    private final KnowledgeService knowledgeService;

    public KnowledgeAssociationService(KnowledgeRepository repository,
                                        KnowledgeService knowledgeService) {
        this.repository = repository;
        this.knowledgeService = knowledgeService;
    }

    public List<Map<String, Object>> getRelatedKnowledge(Long knowledgeId, int limit) {
        Knowledge knowledge = knowledgeService.getById(knowledgeId);
        if (knowledge.getEmbedding() == null || knowledge.getEmbedding().isBlank()) {
            log.info("知识无嵌入向量，无法进行关联推荐: id={}", knowledgeId);
            return List.of();
        }

        List<Object[]> results = repository.findSimilarIds(knowledge.getEmbedding(), limit + 1);
        Map<Long, Double> similarityMap = new LinkedHashMap<>();
        for (Object[] row : results) {
            Long id = ((Number) row[0]).longValue();
            if (id.equals(knowledgeId)) continue;
            Double similarity = ((Number) row[1]).doubleValue();
            similarityMap.put(id, similarity);
        }

        List<Knowledge> knowledgeList = repository.findAllById(similarityMap.keySet());
        Map<Long, Knowledge> knowledgeMap = knowledgeList.stream()
                .collect(Collectors.toMap(Knowledge::getId, k -> k));

        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<Long, Double> entry : similarityMap.entrySet()) {
            Knowledge k = knowledgeMap.get(entry.getKey());
            if (k == null) continue;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", k.getId());
            item.put("title", k.getTitle());
            item.put("summary", k.getSummary());
            item.put("similarity", entry.getValue());
            item.put("tags", k.getTags());
            list.add(item);
            if (list.size() >= limit) break;
        }

        log.info("知识关联推荐: knowledgeId={}, 返回 {} 条", knowledgeId, list.size());
        return list;
    }
}