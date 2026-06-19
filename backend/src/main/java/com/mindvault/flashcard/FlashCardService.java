package com.mindvault.flashcard;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindvault.agent.config.AgentConfig;
import com.mindvault.flashcard.entity.FlashCard;
import com.mindvault.knowledge.KnowledgeService;
import com.mindvault.knowledge.entity.Knowledge;
import com.mindvault.model.ModelConfigService;
import com.mindvault.model.entity.ModelConfig;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class FlashCardService {

    private static final Logger log = LoggerFactory.getLogger(FlashCardService.class);

    private final ModelConfigService modelConfigService;
    private final AgentConfig agentConfig;
    private final KnowledgeService knowledgeService;
    private final FlashCardMapper mapper;
    private final ObjectMapper objectMapper;

    private volatile List<LlmEndpoint> modelEndpoints = List.of();

    public FlashCardService(ModelConfigService modelConfigService,
                            AgentConfig agentConfig,
                            KnowledgeService knowledgeService,
                            FlashCardMapper mapper) {
        this.modelConfigService = modelConfigService;
        this.agentConfig = agentConfig;
        this.knowledgeService = knowledgeService;
        this.mapper = mapper;
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void init() {
        refreshModels();
    }

    public void refreshModels() {
        try {
            List<ModelConfig> models = modelConfigService.getAvailableChatModels();
            modelEndpoints = models.stream()
                    .map(mc -> new LlmEndpoint(agentConfig.buildEndpoint(mc)))
                    .toList();
            log.info("FlashCardService 初始化完成，可用模型数: {}", modelEndpoints.size());
        } catch (Exception e) {
            log.warn("FlashCardService 初始化失败: {}", e.getMessage());
        }
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
        String prompt = "你是一个知识卡片生成助手。请根据以下内容生成3-5个问答式知识卡片。" +
                "返回JSON数组格式，每个元素包含 question、answer、difficulty 字段。" +
                "difficulty 取值为 EASY / MEDIUM / HARD。" +
                "只返回JSON数组，不要额外说明。\n\n标题: " + title + "\n\n内容: " + truncate(content, 3000);

        String result = callLlmWithFailover(prompt);
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

    private String callLlmWithFailover(String prompt) {
        List<String> errors = new ArrayList<>();
        for (LlmEndpoint me : modelEndpoints) {
            try {
                RestClient client = RestClient.builder()
                        .baseUrl(me.endpoint.getFullUrl())
                        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .defaultHeader("Authorization", "Bearer " + me.endpoint.getApiKey())
                        .build();

                Map<String, Object> requestBody = new LinkedHashMap<>();
                requestBody.put("model", me.endpoint.getModelName());
                requestBody.put("messages", List.of(Map.of("role", "user", "content", prompt)));
                requestBody.put("temperature", 0.3);
                requestBody.put("max_tokens", 1000);

                String responseJson = client.post()
                        .body(objectMapper.writeValueAsString(requestBody))
                        .retrieve()
                        .body(String.class);

                Map<?, ?> responseMap = objectMapper.readValue(responseJson, Map.class);
                String content = extractContent(responseMap);
                if (content != null) return content.trim();
            } catch (Exception e) {
                log.warn("模型调用失败: {}", e.getMessage());
                errors.add(e.getMessage());
            }
        }
        log.warn("所有模型均调用失败: {}", String.join("; ", errors));
        return null;
    }

    private String extractContent(Map<?, ?> responseMap) {
        if (responseMap.containsKey("choices")) {
            List<?> choices = (List<?>) responseMap.get("choices");
            if (!choices.isEmpty()) {
                Map<?, ?> choice = (Map<?, ?>) choices.get(0);
                Map<?, ?> message = (Map<?, ?>) choice.get("message");
                if (message != null && message.get("content") instanceof String s) return s;
                if (choice.get("text") instanceof String s) return s;
            }
        }
        if (responseMap.containsKey("message")) {
            Map<?, ?> message = (Map<?, ?>) responseMap.get("message");
            if (message.get("content") instanceof String s) return s;
        }
        if (responseMap.containsKey("response")) {
            Object resp = responseMap.get("response");
            if (resp instanceof String s) return s;
        }
        return null;
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen);
    }

    private record LlmEndpoint(AgentConfig.LlmEndpoint endpoint) {}
}