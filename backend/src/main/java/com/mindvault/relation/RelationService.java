package com.mindvault.relation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindvault.ai.client.AiService;
import com.mindvault.ai.prompt.PromptRegistry;
import com.mindvault.auto.AutoProcessLogMapper;
import com.mindvault.auto.entity.AutoProcessLog;
import com.mindvault.knowledge.KnowledgeMapper;
import com.mindvault.knowledge.KnowledgeService;
import com.mindvault.knowledge.entity.Knowledge;
import com.mindvault.model.ModelConfigService;
import com.mindvault.relation.entity.KnowledgeRelation;
import com.mindvault.systemconfig.SystemConfigService;
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
    private final AiService aiService;
    private final AutoProcessLogMapper logMapper;
    private final SystemConfigService config;
    private final ObjectMapper objectMapper;

    public RelationService(KnowledgeMapper knowledgeMapper,
                           KnowledgeRelationMapper relationMapper,
                           KnowledgeService knowledgeService,
                           ModelConfigService modelConfigService,
                           AiService aiService,
                           AutoProcessLogMapper logMapper,
                           SystemConfigService config) {
        this.knowledgeMapper = knowledgeMapper;
        this.relationMapper = relationMapper;
        this.knowledgeService = knowledgeService;
        this.modelConfigService = modelConfigService;
        this.aiService = aiService;
        this.logMapper = logMapper;
        this.config = config;
        this.objectMapper = new ObjectMapper();
    }

    public void processRound2() {
        int batchSize = config.getInt("threshold.relation.batch-size", 20);
        List<Knowledge> pending = knowledgeMapper.findByAutoProcessStatus("TITLE_TAG_DONE", batchSize);
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

        int candidateLimit = config.getInt("threshold.relation.candidate-limit", 50);
        List<Knowledge> candidates = knowledgeMapper.findByAutoProcessStatus("COMPLETED", candidateLimit);
        if (candidates.isEmpty()) return;

        List<Knowledge> all = new ArrayList<>(candidates);

        // 1. Semantic similarity (VECTOR)
        if (k.getEmbedding() != null && !k.getEmbedding().isBlank()) {
            int vectorTopN = config.getInt("threshold.relation.vector-top-n", 10);
            double simMin = config.getDouble("threshold.relation.similarity-min", 0.5);
            List<Map<String, Object>> similar = knowledgeMapper.findSimilarIds(k.getEmbedding(), vectorTopN);
            for (Map<String, Object> row : similar) {
                Long relatedId = ((Number) row.get("id")).longValue();
                if (relatedId.equals(k.getId())) continue;
                double similarity = ((Number) row.get("similarity")).doubleValue();
                if (similarity < simMin) continue;
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
            double scorePerTag = config.getDouble("threshold.relation.score-per-tag", 0.25);
            double tagScoreMax = config.getDouble("threshold.relation.tag-score-max", 1.0);
            for (Knowledge candidate : all) {
                if (candidate.getId().equals(k.getId())) continue;
                Set<String> candidateAiTags = parseTags(candidate.getTags());
                Set<String> candidateUserTags = parseTags(candidate.getUserTags());
                Set<String> candidateTags = new HashSet<>(candidateAiTags);
                candidateTags.addAll(candidateUserTags);

                Set<String> overlap = new HashSet<>(allTags);
                overlap.retainAll(candidateTags);
                if (!overlap.isEmpty()) {
                    double score = Math.min(tagScoreMax, overlap.size() * scorePerTag);
                    saveRelation(k.getId(), candidate.getId(), "REFERENCE",
                            BigDecimal.valueOf(score), "TAG", now);
                }
            }
        }

        // 3. LLM analysis (LLM)
        try {
            modelConfigService.getPrimaryChatModel();
        } catch (Exception e) {
            return;
        }

        try {
            int llmCandidateLimit = config.getInt("threshold.relation.llm-candidate-limit", 10);
            List<Map<String, Object>> candidateSummaries = all.stream()
                    .filter(c -> !c.getId().equals(k.getId()))
                    .limit(llmCandidateLimit)
                    .map(c -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("id", c.getId());
                        m.put("title", knowledgeService.displayTitle(c));
                        m.put("summary", c.getSummary() != null ? c.getSummary() : "");
                        return m;
                    })
                    .collect(Collectors.toList());

            if (candidateSummaries.isEmpty()) return;

            int contentTruncate = config.getInt("threshold.relation.content-truncate-length", 1500);
            double llmTemp = config.getDouble("threshold.relation.llm-temperature", 0.3);
            int llmMaxTokens = config.getInt("threshold.relation.llm-max-tokens", 500);
            double llmDefaultScore = config.getDouble("threshold.relation.llm-default-score", 0.75);

            String prompt = PromptRegistry.RELATION_LLM.resolve(config, userTitle,
                    AiService.truncate(content, contentTruncate),
                    objectMapper.writeValueAsString(candidateSummaries));

            String result = aiService.call(prompt, llmTemp, llmMaxTokens);

            if (result != null) {
                String cleaned = result.trim();
                if (cleaned.startsWith("[")) {
                    List<Map<String, Object>> relations = objectMapper.readValue(cleaned,
                            new TypeReference<List<Map<String, Object>>>() {});
                    for (Map<String, Object> rel : relations) {
                        Long relatedId = ((Number) rel.get("id")).longValue();
                        String type = (String) rel.getOrDefault("type", "REFERENCE");
                        saveRelation(k.getId(), relatedId, type, BigDecimal.valueOf(llmDefaultScore), "LLM", now);
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