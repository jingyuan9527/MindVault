package com.mindvault.flashcard;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindvault.ai.client.AiService;
import com.mindvault.ai.prompt.PromptRegistry;
import com.mindvault.flashcard.entity.FlashCard;
import com.mindvault.knowledge.KnowledgeService;
import com.mindvault.knowledge.entity.Knowledge;
import com.mindvault.model.ModelConfigService;
import com.mindvault.systemconfig.SystemConfigService;
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
public class FlashCardService {

    private static final Logger log = LoggerFactory.getLogger(FlashCardService.class);

    private final ModelConfigService modelConfigService;
    private final AiService aiService;
    private final KnowledgeService knowledgeService;
    private final FlashCardMapper mapper;
    private final SystemConfigService config;
    private final ObjectMapper objectMapper;

    public FlashCardService(ModelConfigService modelConfigService,
                            AiService aiService,
                            KnowledgeService knowledgeService,
                            FlashCardMapper mapper,
                            SystemConfigService config) {
        this.modelConfigService = modelConfigService;
        this.aiService = aiService;
        this.knowledgeService = knowledgeService;
        this.mapper = mapper;
        this.config = config;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 基于知识内容自动生成闪卡。
     * 先清空该知识已有的自动闪卡，调用 LLM 提取问答对，解析 JSON 并批量入库。
     * @param knowledgeId 知识 ID
     * @return 生成的闪卡列表
     * @throws IllegalArgumentException 当知识不存在时
     */
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

        int truncateLen = config.getInt("threshold.flashcard.truncate-length", 3000);
        double temperature = config.getDouble("threshold.flashcard.temperature", 0.3);
        int maxTokens = config.getInt("threshold.flashcard.max-tokens", 1000);
        String prompt = PromptRegistry.FLASHCARD_GENERATION.resolve(config, title,
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

    /* CRUD methods unchanged */

    /**
     * 查询指定知识关联的所有闪卡。
     * @param knowledgeId 知识 ID
     * @return 闪卡列表
     */
    public List<FlashCard> listByKnowledge(Long knowledgeId) {
        return mapper.findByKnowledgeId(knowledgeId);
    }

    /**
     * 查询所有闪卡。
     * @return 全部闪卡列表
     */
    public List<FlashCard> listAll() {
        return mapper.selectList(null);
    }

    /**
     * 按来源类型查询闪卡。
     * @param sourceType 来源类型（AUTO / MANUAL）
     * @return 闪卡列表
     */
    public List<FlashCard> listBySourceType(String sourceType) {
        return mapper.findBySourceType(sourceType);
    }

    /**
     * 统计指定知识的闪卡数量。
     * @param knowledgeId 知识 ID
     * @return 闪卡数量
     */
    public long countByKnowledge(Long knowledgeId) {
        return mapper.countByKnowledgeId(knowledgeId);
    }

    /**
     * 删除指定闪卡。
     * @param id 闪卡 ID
     */
    @Transactional
    public void deleteCard(Long id) {
        mapper.deleteById(id);
    }
}