package com.mindvault.flashcard;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindvault.common.service.LlmFailoverService;
import com.mindvault.flashcard.entity.FlashCard;
import com.mindvault.knowledge.KnowledgeService;
import com.mindvault.knowledge.entity.Knowledge;
import com.mindvault.model.ModelConfigService;
import com.mindvault.model.entity.ModelConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class FlashCardService {

    private static final Logger log = LoggerFactory.getLogger(FlashCardService.class);

    private final ModelConfigService modelConfigService;
    private final LlmFailoverService llmFailoverService;
    private final KnowledgeService knowledgeService;
    private final FlashCardMapper mapper;
    private final ObjectMapper objectMapper;

    public FlashCardService(ModelConfigService modelConfigService,
                            LlmFailoverService llmFailoverService,
                            KnowledgeService knowledgeService,
                            FlashCardMapper mapper) {
        this.modelConfigService = modelConfigService;
        this.llmFailoverService = llmFailoverService;
        this.knowledgeService = knowledgeService;
        this.mapper = mapper;
        this.objectMapper = new ObjectMapper();
    }

    @Transactional
    public List<FlashCard> generateCards(Long knowledgeId) {
        Knowledge knowledge = knowledgeService.getById(knowledgeId);
        if (knowledge == null) throw new IllegalArgumentException("知识不存在: " + knowledgeId);

        mapper.findByKnowledgeId(knowledgeId).forEach(c -> mapper.deleteById(c.getId()));

        List<Map<String, String>> qaPairs = callLlmForCards(knowledge.getTitle(), knowledge.getContent());
        List<FlashCard> cards = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (Map<String, String> pair : qaPairs) {
            FlashCard card = new FlashCard();
            card.setKnowledgeId(knowledgeId);
            card.setQuestion(pair.get("question"));
            card.setAnswer(pair.get("answer"));
            card.setDifficulty(pair.getOrDefault("difficulty", "MEDIUM"));
            card.setSourceType("AUTO");
            card.setCreatedAt(now);
            mapper.insert(card);
            cards.add(card);
        }
        log.info("生成知识卡片: knowledgeId={}, count={}", knowledgeId, cards.size());
        return cards;
    }

    private List<Map<String, String>> callLlmForCards(String title, String content) {
        List<ModelConfig> models = modelConfigService.getAvailableChatModels();
        if (models.isEmpty()) return List.of();

        String prompt = "你是一个知识卡片生成助手。请根据以下内容生成3-5个问答式知识卡片。" +
                "返回JSON数组格式，每个元素包含 question、answer、difficulty 字段。" +
                "difficulty 取值为 EASY / MEDIUM / HARD。" +
                "只返回JSON数组，不要额外说明。\n\n标题: " + title + "\n\n内容: " + LlmFailoverService.truncate(content, 3000);

        String result = llmFailoverService.call(models, new LlmFailoverService.LlmCallOptions(prompt, 0.3, 1000, false, null));
        if (result == null) return List.of();

        try {
            String cleaned = result.trim();
            if (cleaned.startsWith("```json")) cleaned = cleaned.substring(7);
            if (cleaned.endsWith("```")) cleaned = cleaned.substring(0, cleaned.length() - 3);
            cleaned = cleaned.trim();
            return objectMapper.readValue(cleaned, new TypeReference<List<Map<String, String>>>() {});
        } catch (Exception e) {
            log.warn("解析知识卡片 JSON 失败: {}", e.getMessage());
            return List.of();
        }
    }

    /* CRUD methods unchanged */

    public List<FlashCard> listByKnowledge(Long knowledgeId) {
        return mapper.findByKnowledgeId(knowledgeId);
    }

    public List<FlashCard> listAll() {
        return mapper.selectList(null);
    }

    public List<FlashCard> listBySourceType(String sourceType) {
        return mapper.findBySourceType(sourceType);
    }

    public long countByKnowledge(Long knowledgeId) {
        return mapper.countByKnowledgeId(knowledgeId);
    }

    @Transactional
    public void deleteCard(Long id) {
        mapper.deleteById(id);
    }
}