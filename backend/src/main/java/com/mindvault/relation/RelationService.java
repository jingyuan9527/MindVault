package com.mindvault.relation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindvault.auto.AutoProcessLogMapper;
import com.mindvault.auto.entity.AutoProcessLog;
import com.mindvault.common.service.LlmFailoverService;
import com.mindvault.knowledge.KnowledgeMapper;
import com.mindvault.knowledge.KnowledgeService;
import com.mindvault.knowledge.entity.Knowledge;
import com.mindvault.model.ModelConfigService;
import com.mindvault.model.entity.ModelConfig;
import com.mindvault.relation.entity.KnowledgeRelation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RelationService {

    private static final Logger log = LoggerFactory.getLogger(RelationService.class);

    private final KnowledgeMapper knowledgeMapper;
    private final KnowledgeRelationMapper relationMapper;
    private final KnowledgeService knowledgeService;
    private final ModelConfigService modelConfigService;
    private final LlmFailoverService llmFailoverService;
    private final AutoProcessLogMapper logMapper;
    private final ObjectMapper objectMapper;

    public RelationService(KnowledgeMapper knowledgeMapper,
                           KnowledgeRelationMapper relationMapper,
                           KnowledgeService knowledgeService,
                           ModelConfigService modelConfigService,
                           LlmFailoverService llmFailoverService,
                           AutoProcessLogMapper logMapper) {
        this.knowledgeMapper = knowledgeMapper;
        this.relationMapper = relationMapper;
        this.knowledgeService = knowledgeService;
        this.modelConfigService = modelConfigService;
        this.llmFailoverService = llmFailoverService;
        this.logMapper = logMapper;
        this.objectMapper = new ObjectMapper();
    }

    public void processRound2() {
        List<Knowledge> pending = knowledgeMapper.findByAutoProcessStatus("TITLE_TAG_DONE", 20);
        if (pending.isEmpty()) return;
        log.info("R2 关联发现: 待处理 {} 条", pending.size());

        for (Knowledge k : pending) {
            try {
                processKnowledgeRelations(k);
                knowledgeService.updateAutoProcessStatus(k.getId(), "RELATION_DONE");
            } catch (Exception e) {
                log.warn("R2 关联失败: knowledgeId={}, error={}", k.getId(), e.getMessage());
                saveLog(k.getId(), "R2_RELATION", "FAILED", e.getMessage());
            }
        }
    }

    @Transactional
    public void processKnowledgeRelations(Knowledge k) {
        LocalDateTime now = LocalDateTime.now();
        String userTitle = k.getTitle();
        String content = k.getContent();

        List<Knowledge> candidates = knowledgeMapper.findByAutoProcessStatus("COMPLETED", 50);
        if (candidates.isEmpty()) return;

        List<Knowledge> all = new ArrayList<>(candidates);

        // 1. Semantic similarity (VECTOR)
        if (k.getEmbedding() != null && !k.getEmbedding().isBlank()) {
            List<Object[]> similar = knowledgeMapper.findSimilarIds(k.getEmbedding(), 10);
            for (Object[] row : similar) {
                Long relatedId = ((Number) row[0]).longValue();
                if (relatedId.equals(k.getId())) continue;
                double similarity = ((Number) row[1]).doubleValue();
                if (similarity < 0.5) continue;
                saveRelation(k.getId(), relatedId, "COMPLEMENT",
                        BigDecimal.valueOf(similarity), "VECTOR", now);
            }
        }

        // 2. Tag overlap (TAG)
        Set<String> aiTagSet = parseTags(k.getTags());
        Set<String> userTagSet = parseTags(k.getUserTags());
        Set<String> allTags = new HashSet<>(aiTagSet);
        allTags.addAll(userTagSet);

        if (!allTags.isEmpty()) {
            for (Knowledge candidate : all) {
                if (candidate.getId().equals(k.getId())) continue;
                Set<String> candidateAiTags = parseTags(candidate.getTags());
                Set<String> candidateUserTags = parseTags(candidate.getUserTags());
                Set<String> candidateTags = new HashSet<>(candidateAiTags);
                candidateTags.addAll(candidateUserTags);

                Set<String> overlap = new HashSet<>(allTags);
                overlap.retainAll(candidateTags);
                if (!overlap.isEmpty()) {
                    double score = Math.min(1.0, overlap.size() * 0.25);
                    saveRelation(k.getId(), candidate.getId(), "REFERENCE",
                            BigDecimal.valueOf(score), "TAG", now);
                }
            }
        }

        // 3. LLM analysis (LLM)
        List<ModelConfig> models = modelConfigService.getAvailableChatModels();
        if (models.isEmpty()) return;

        try {
            List<Map<String, Object>> candidateSummaries = all.stream()
                    .filter(c -> !c.getId().equals(k.getId()))
                    .limit(10)
                    .map(c -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("id", c.getId());
                        m.put("title", knowledgeService.displayTitle(c));
                        m.put("summary", c.getSummary() != null ? c.getSummary() : "");
                        return m;
                    })
                    .collect(Collectors.toList());

            if (candidateSummaries.isEmpty()) return;

            String prompt = "你是一个知识关联分析助手。新笔记内容如下：\n\n"
                    + "标题: " + userTitle + "\n内容: " + LlmFailoverService.truncate(content, 1500)
                    + "\n\n以下是已有笔记列表（id + 标题 + 摘要）：\n"
                    + objectMapper.writeValueAsString(candidateSummaries)
                    + "\n\n请分析新笔记与哪些已有笔记相关，返回JSON数组，每一项包含："
                    + "{\"id\": 已有笔记ID, \"type\": \"COMPLEMENT|CONTRAST|EXTENSION|REFERENCE\", \"reason\": \"简短原因\"}。"
                    + "如果都不相关则返回 []. 只返回JSON数组。";

            String result = llmFailoverService.call(models,
                    new LlmFailoverService.LlmCallOptions(prompt, 0.3, 500, false, null));

            if (result != null) {
                String cleaned = result.trim();
                if (cleaned.startsWith("[")) {
                    List<Map<String, Object>> relations = objectMapper.readValue(cleaned,
                            new TypeReference<List<Map<String, Object>>>() {});
                    for (Map<String, Object> rel : relations) {
                        Long relatedId = ((Number) rel.get("id")).longValue();
                        String type = (String) rel.getOrDefault("type", "REFERENCE");
                        saveRelation(k.getId(), relatedId, type, BigDecimal.valueOf(0.75), "LLM", now);
                    }
                }
            }

            saveLog(k.getId(), "R2_RELATION", "SUCCESS", null);
        } catch (Exception e) {
            log.warn("LLM 关联分析失败: knowledgeId={}, error={}", k.getId(), e.getMessage());
            saveLog(k.getId(), "R2_RELATION", "SUCCESS", null);
        }
    }

    private void saveRelation(Long knowledgeId, Long relatedId, String type, BigDecimal score, String source, LocalDateTime now) {
        try {
            KnowledgeRelation rel = new KnowledgeRelation();
            rel.setKnowledgeId(knowledgeId);
            rel.setRelatedId(relatedId);
            rel.setRelationType(type);
            rel.setScore(score);
            rel.setSource(source);
            rel.setCreatedAt(now);
            relationMapper.insert(rel);
        } catch (Exception e) {
            log.warn("保存关联失败 (可能已存在): knowledgeId={}, relatedId={}, source={}",
                    knowledgeId, relatedId, source);
        }
    }

    private Set<String> parseTags(String tagsJson) {
        if (tagsJson == null || tagsJson.equals("[]")) return new HashSet<>();
        try {
            List<String> list = objectMapper.readValue(tagsJson, new TypeReference<List<String>>() {});
            return new HashSet<>(list);
        } catch (Exception e) {
            return new HashSet<>();
        }
    }

    private void saveLog(Long knowledgeId, String round, String status, String errorMessage) {
        try {
            AutoProcessLog l = new AutoProcessLog();
            l.setKnowledgeId(knowledgeId);
            l.setRound(round);
            l.setStatus(status);
            l.setErrorMessage(errorMessage);
            l.setStartedAt(LocalDateTime.now());
            l.setCompletedAt(LocalDateTime.now());
            l.setCreatedAt(LocalDateTime.now());
            logMapper.insert(l);
        } catch (Exception e) {
            log.warn("保存处理日志失败: {}", e.getMessage());
        }
    }
}
