package com.mindvault.knowledge;

import com.mindvault.knowledge.entity.Knowledge;
import com.mindvault.systemconfig.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class KnowledgeAssociationService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeAssociationService.class);

    private final KnowledgeMapper mapper;
    private final KnowledgeService knowledgeService;
    private final SystemConfigService config;

    public KnowledgeAssociationService(KnowledgeMapper mapper,
                                        KnowledgeService knowledgeService,
                                        SystemConfigService config) {
        this.mapper = mapper;
        this.knowledgeService = knowledgeService;
        this.config = config;
    }

    public List<Map<String, Object>> getRelatedKnowledge(Long knowledgeId, int limit) {
        Knowledge knowledge = knowledgeService.getById(knowledgeId);
        if (knowledge.getEmbedding() == null || knowledge.getEmbedding().isBlank()) {
            log.info("知识无嵌入向量，无法进行关联推荐: id={}", knowledgeId);
            return List.of();
        }

        List<Map<String, Object>> results = mapper.findSimilarIds(knowledge.getEmbedding(), limit + 1);
        Map<Long, Double> similarityMap = new LinkedHashMap<>();
        for (Map<String, Object> row : results) {
            Long id = ((Number) row.get("id")).longValue();
            if (id.equals(knowledgeId)) continue;
            Double similarity = ((Number) row.get("similarity")).doubleValue();
            similarityMap.put(id, similarity);
        }

        List<Knowledge> knowledgeList = mapper.selectBatchIds(similarityMap.keySet());
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

    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduledAssociationDiscovery() {
        if (!config.getBool("task.association.enabled", true)) return;
        log.info("开始执行定时知识关联发现...");
        List<Knowledge> all = mapper.selectList(null);
        int totalWithEmbedding = 0;
        int totalAssociations = 0;

        int topN = config.getInt("threshold.association.top-n", 6);
        for (Knowledge k : all) {
            if (k.getEmbedding() == null || k.getEmbedding().isBlank()) continue;
            totalWithEmbedding++;
            List<Map<String, Object>> similar = mapper.findSimilarIds(k.getEmbedding(), topN);
            int count = 0;
            for (Map<String, Object> row : similar) {
                Long id = ((Number) row.get("id")).longValue();
                if (id.equals(k.getId())) continue;
                count++;
            }
            totalAssociations += count;
        }

        log.info("知识关联发现完成: 有嵌入的知识 {} 条, 共发现关联 {} 条",
                totalWithEmbedding, totalAssociations);
    }
}