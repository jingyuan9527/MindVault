package com.mindvault.knowledge.service;

import com.mindvault.auto.mapper.KnowledgeRelationMapper;
import com.mindvault.auto.service.AutoProcessOrchestrator;
import com.mindvault.common.dto.PageResult;
import com.mindvault.knowledge.entity.Knowledge;
import com.mindvault.knowledge.mapper.KnowledgeMapper;
import com.mindvault.knowledge.service.strategy.SearchStrategy;
import com.mindvault.operationlog.service.OperationLogService;
import com.mindvault.review.service.ReviewService;
import com.mindvault.knowledge.config.KnowledgeProperties;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class KnowledgeServiceImpl implements KnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeServiceImpl.class);

    private final KnowledgeMapper mapper;
    private final OperationLogService operationLogService;
    private final AutoProcessOrchestrator autoProcessOrchestrator;
    private final ReviewService reviewService;
    private final KnowledgeRelationMapper relationMapper;
    private final KnowledgeProperties knowledgeProperties;
    private final List<SearchStrategy> searchStrategies;

    public KnowledgeServiceImpl(KnowledgeMapper mapper,
                                OperationLogService operationLogService,
                                AutoProcessOrchestrator autoProcessOrchestrator,
                                ReviewService reviewService,
                                KnowledgeRelationMapper relationMapper,
KnowledgeProperties knowledgeProperties,
        List<SearchStrategy> searchStrategies) {
        this.mapper = mapper;
        this.operationLogService = operationLogService;
        this.autoProcessOrchestrator = autoProcessOrchestrator;
        this.reviewService = reviewService;
        this.relationMapper = relationMapper;
        this.knowledgeProperties = knowledgeProperties;
        this.searchStrategies = searchStrategies;
    }

    @Transactional
    @Override
    public Knowledge addKnowledge(Knowledge knowledge) {
        LocalDateTime now = LocalDateTime.now();
        knowledge.setCreatedAt(now);
        knowledge.setUpdatedAt(now);
        knowledge.setAutoProcessStatus(knowledgeProperties.getAutoProcessStatus());
        mapper.insert(knowledge);
        log.info("添加知识: id={}, userTitle={}, type={}", knowledge.getId(), knowledge.getTitle(), knowledge.getContentType());
        operationLogService.log("KNOWLEDGE", "ADD", knowledge.getId(),
                "添加知识\u00AB" + knowledge.getTitle() + "\u00BB");

        if (knowledge.getContent() != null && !knowledge.getContent().isBlank()) {
            String aiTitle = autoProcessOrchestrator.generateAiTitle(knowledge.getId(), knowledge.getTitle(), knowledge.getContent());
            if (aiTitle != null) {
                knowledge.setAiTitle(aiTitle);
            }
            triggerAutoProcess(knowledge);
        }
        try {
            reviewService.scheduleReview(knowledge.getId());
        } catch (Exception e) {
            log.warn("自动安排复习失败: {}", e.getMessage());
        }
        return knowledge;
    }

    @Override
    public PageResult<Knowledge> listAll(int page, int size, String keyword, List<String> tags, String sortBy, String sortOrder) {
        QueryWrapper<Knowledge> qw = new QueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like("title", keyword).or().like("ai_title", keyword).or().like("content", keyword));
        }
        if (tags != null && !tags.isEmpty()) {
            for (String tag : tags) {
                String tagJson = "[\"" + tag + "\"]";
                qw.and(w -> w.apply("tags::jsonb @> {0}::jsonb", tagJson).or().apply("user_tags::jsonb @> {0}::jsonb", tagJson));
            }
        }
        long total = mapper.selectCount(qw);
        String column = switch (sortBy != null ? sortBy : "createdAt") {
            case "updatedAt" -> "updated_at";
            case "title" -> "title";
            default -> "created_at";
        };
        qw.orderBy(true, "asc".equalsIgnoreCase(sortOrder), column);
        int offset = page * size;
        qw.last("LIMIT " + size + " OFFSET " + offset);
        List<Knowledge> records = mapper.selectList(qw);
        int totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
        return new PageResult<>(records, total, page, size, totalPages);
    }

    @Override
    public List<Knowledge> listAllSimple(int page, int size) {
        int offset = page * size;
        QueryWrapper<Knowledge> qw = new QueryWrapper<>();
        qw.orderBy(true, false, "created_at");
        qw.last("LIMIT " + size + " OFFSET " + offset);
        return mapper.selectList(qw);
    }

    @Override
    public Knowledge getById(Long id) {
        Knowledge k = mapper.selectById(id);
        if (k == null) throw new IllegalArgumentException("知识不存在: " + id);
        return k;
    }

    @Override
    public List<Map<String, Object>> searchSimilar(String embedding, int topN) {
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
            item.put("id", k.getId()); item.put("title", k.getTitle()); item.put("aiTitle", k.getAiTitle());
            item.put("content", k.getContent()); item.put("similarity", entry.getValue());
            list.add(item);
        }
        log.info("语义搜索: topN={}, 返回 {} 条结果", topN, list.size());
        return list;
    }

    @Override
    public List<Map<String, Object>> hybridSearch(String query, int limit) {
        for (SearchStrategy strategy : searchStrategies) {
            if (strategy.isApplicable()) {
                log.info("搜索策略: {} (query={})", strategy.name(), query);
                return strategy.search(query, limit);
            }
        }
        return List.of();
    }

    @Override
    public List<Map<String, Object>> keywordSearchWithRank(String query, int limit) {
        List<Knowledge> results = mapper.keywordSearch(query, limit);
        List<Map<String, Object>> list = new java.util.ArrayList<>();
        for (Knowledge k : results) {
            Map<String, Object> item = new java.util.LinkedHashMap<>();
            item.put("id", k.getId()); item.put("title", k.getTitle()); item.put("aiTitle", k.getAiTitle());
            item.put("content", k.getContent()); item.put("summary", k.getSummary());
            item.put("tags", k.getTags()); item.put("userTags", k.getUserTags());
            item.put("contentType", k.getContentType()); item.put("sourceUrl", k.getSourceUrl());
            item.put("createdAt", k.getCreatedAt());
            list.add(item);
        }
        return list;
    }

    @Override
    public String displayTitle(Knowledge k) {
        return k.getAiTitle() != null && !k.getAiTitle().isBlank() ? k.getAiTitle() : k.getTitle();
    }

    @Override
    public List<Knowledge> searchByKeyword(String query, int limit) {
        return mapper.keywordSearch(query, limit);
    }

    @Override
    public List<Knowledge> searchByKeywordWithTag(String query, int limit, String tag) {
        return mapper.searchByTag("[\"" + tag + "\"]", limit);
    }

    @Transactional
    @Override
    public Knowledge updateKnowledge(Long id, Knowledge updated) {
        Knowledge existing = getById(id);
        existing.setTitle(updated.getTitle()); existing.setContent(updated.getContent());
        existing.setContentType(updated.getContentType()); existing.setSourceUrl(updated.getSourceUrl());
        existing.setSummary(updated.getSummary()); existing.setUserTags(updated.getUserTags());
        existing.setMetadata(updated.getMetadata()); existing.setUpdatedAt(LocalDateTime.now());
        mapper.updateById(existing);
        log.info("更新知识: id={}, userTitle={}", id, existing.getTitle());
        operationLogService.log("KNOWLEDGE", "UPDATE", id, "更新知识\u00AB" + existing.getTitle() + "\u00BB");
        return existing;
    }

    @Transactional
    @Override
    public Knowledge updateAiFields(Long id, String aiTitle, String aiTags) {
        Knowledge k = getById(id);
        if (aiTitle != null) k.setAiTitle(aiTitle);
        if (aiTags != null) k.setTags(aiTags);
        k.setUpdatedAt(LocalDateTime.now());
        mapper.updateById(k);
        return k;
    }

    @Transactional
    @Override
    public void updateEmbedding(Long id, String embedding) {
        Knowledge k = mapper.selectById(id);
        if (k != null) {
            k.setEmbedding(embedding);
            mapper.updateById(k);
        }
    }

    @Transactional
    @Override
    public void deleteKnowledge(Long id) {
        Knowledge k = getById(id);
        mapper.deleteById(id);
        relationMapper.deleteByKnowledgeId(id);
        log.info("删除知识: id={}, title={}", id, k.getTitle());
        operationLogService.log("KNOWLEDGE", "DELETE", id, "删除知识\u00AB" + k.getTitle() + "\u00BB");
    }

    private void triggerAutoProcess(Knowledge knowledge) {
        if (knowledge.getContent() == null || knowledge.getContent().isBlank()) return;
        autoProcessOrchestrator.processAsync(knowledge.getId(), knowledge.getTitle(), knowledge.getContent());
    }

    @Transactional
    @Override
    public void updateAutoProcessStatus(Long id, String status) {
        Knowledge k = mapper.selectById(id);
        if (k != null) {
            k.setAutoProcessStatus(status);
            k.setUpdatedAt(LocalDateTime.now());
            mapper.updateById(k);
        }
    }

    @Transactional
    @Override
    public void reprocessKnowledge(Long id) {
        Knowledge k = getById(id);
        k.setAutoProcessStatus(knowledgeProperties.getAutoProcessStatus());
        k.setAiTitle(null);
        k.setTags(knowledgeProperties.getTagsEmptyJson());
        k.setSummary(null);
        mapper.updateById(k);
        autoProcessOrchestrator.processAsync(k.getId(), k.getTitle(), k.getContent());
        log.info("重新处理知识: id={}", id);
        operationLogService.log("KNOWLEDGE", "REPROCESS", id, "重新处理知识\u00AB" + k.getTitle() + "\u00BB");
    }

    @Transactional
    @Override
    public void batchDelete(List<Long> ids) {
        for (Long id : ids) {
            Knowledge k = mapper.selectById(id);
            if (k != null) {
                mapper.deleteById(id);
                relationMapper.deleteByKnowledgeId(id);
                operationLogService.log("KNOWLEDGE", "DELETE", id, "批量删除知识\u00AB" + k.getTitle() + "\u00BB");
            }
        }
    }
}