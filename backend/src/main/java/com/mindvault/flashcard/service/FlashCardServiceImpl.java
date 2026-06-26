package com.mindvault.flashcard.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindvault.ai.client.AiService;
import com.mindvault.ai.prompt.PromptRegistry;
import com.mindvault.flashcard.entity.FlashCard;
import com.mindvault.flashcard.mapper.FlashCardMapper;
import com.mindvault.knowledge.service.KnowledgeService;
import com.mindvault.knowledge.entity.Knowledge;
import com.mindvault.model.service.ModelConfigService;
import com.mindvault.flashcard.config.FlashCardProperties;
import com.mindvault.systemconfig.service.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 闪卡服务。
 * <p>提供闪卡的自动生成（基于 LLM）、查询和删除功能。
 * 自动生成流程：调用 LLM 根据知识标题和内容提取问答对，解析 JSON 后批量入库。
 * 生成前会清空该知识已有的自动闪卡，避免重复。LLM 调用参数（截断长度、温度、最大 token 数）通过 SystemConfigService 配置。
 * 输入为 knowledgeId，输出为生成的 FlashCard 列表。</p>
 */
@Service
public class FlashCardServiceImpl implements FlashCardService {

    private static final Logger log = LoggerFactory.getLogger(FlashCardServiceImpl.class);

    private final ModelConfigService modelConfigService;
    private final AiService aiService;
    private final KnowledgeService knowledgeService;
    private final FlashCardMapper mapper;
    private final FlashCardProperties flashCardProperties;
    private final SystemConfigService systemConfigService;
    private final ObjectMapper objectMapper;

    public FlashCardServiceImpl(ModelConfigService modelConfigService,
                                AiService aiService,
                                KnowledgeService knowledgeService,
                                FlashCardMapper mapper,
                                FlashCardProperties flashCardProperties,
                                SystemConfigService systemConfigService) {
        this.modelConfigService = modelConfigService;
        this.aiService = aiService;
        this.knowledgeService = knowledgeService;
        this.mapper = mapper;
        this.flashCardProperties = flashCardProperties;
        this.systemConfigService = systemConfigService;
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
        try {
            modelConfigService.getPrimaryChatModel();
        } catch (Exception e) {
            return List.of();
        }

        int truncateLen = flashCardProperties.getTruncateLength();
        double temperature = flashCardProperties.getTemperature();
        int maxTokens = flashCardProperties.getMaxTokens();
        String prompt = PromptRegistry.FLASHCARD_GENERATION.resolve(systemConfigService, title,
                AiService.truncate(content, truncateLen));

        String result = aiService.call(prompt, temperature, maxTokens);
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
