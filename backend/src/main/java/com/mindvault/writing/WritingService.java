package com.mindvault.writing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindvault.agent.config.AgentConfig;
import com.mindvault.common.service.MetricsService;
import com.mindvault.knowledge.KnowledgeMapper;
import com.mindvault.knowledge.entity.Knowledge;
import com.mindvault.model.ModelConfigService;
import com.mindvault.model.entity.ModelConfig;
import com.mindvault.tokenusage.TokenUsageService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;

@Service
public class WritingService {

    private static final Logger log = LoggerFactory.getLogger(WritingService.class);

    private final ModelConfigService modelConfigService;
    private final AgentConfig agentConfig;
    private final KnowledgeMapper knowledgeMapper;
    private final TokenUsageService tokenUsageService;
    private final MetricsService metricsService;
    private final ObjectMapper objectMapper;

    private volatile List<LlmEndpoint> modelEndpoints = List.of();

    public WritingService(ModelConfigService modelConfigService,
                          AgentConfig agentConfig,
                          KnowledgeMapper knowledgeMapper,
                          TokenUsageService tokenUsageService,
                          MetricsService metricsService) {
        this.modelConfigService = modelConfigService;
        this.agentConfig = agentConfig;
        this.knowledgeMapper = knowledgeMapper;
        this.tokenUsageService = tokenUsageService;
        this.metricsService = metricsService;
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
                    .map(mc -> new LlmEndpoint(mc, agentConfig.buildEndpoint(mc)))
                    .toList();
            log.info("WritingService 初始化完成，可用模型数: {}", modelEndpoints.size());
        } catch (Exception e) {
            log.warn("WritingService 初始化失败: {}", e.getMessage());
        }
    }

    public String generateArticle(String topic, String style, String keywords) {
        if (modelEndpoints.isEmpty()) {
            return "系统未配置可用模型，请先在设置中添加并启用模型。";
        }

        List<Knowledge> related = findRelatedKnowledge(topic, keywords);
        StringBuilder knowledgeContext = new StringBuilder();
        if (!related.isEmpty()) {
            knowledgeContext.append("以下是从知识库中检索到的相关参考内容：\n\n");
            for (Knowledge k : related) {
                knowledgeContext.append("[标题]: ").append(k.getTitle()).append("\n");
                if (k.getSummary() != null) {
                    knowledgeContext.append("[摘要]: ").append(k.getSummary()).append("\n");
                }
                knowledgeContext.append("[内容]: ").append(truncate(k.getContent(), 500)).append("\n\n");
            }
        }

        String styleGuide = style != null && !style.isBlank()
                ? "写作风格: " + style : "写作风格: 清晰、条理、专业";
        String kwGuide = keywords != null && !keywords.isBlank()
                ? "关键词: " + keywords : "";

        String prompt = "你是一个基于个人知识库的写作助手。请根据参考内容撰写一篇文章。\n\n"
                + styleGuide + "\n" + kwGuide + "\n\n"
                + "主题: " + topic + "\n\n"
                + (!related.isEmpty() ? knowledgeContext.toString() : "（知识库中没有直接相关的参考内容，请基于你的知识进行创作）\n\n")
                + "请撰写一篇结构完整、内容详实的文章。使用中文。";

        String result = callLlmWithFailover(prompt);
        return result != null ? result : "文章生成失败，请稍后重试。";
    }

    private List<Knowledge> findRelatedKnowledge(String topic, String keywords) {
        Set<String> searchTerms = new LinkedHashSet<>();
        if (topic != null) {
            Arrays.stream(topic.split("[\\s,，、]+"))
                    .filter(s -> s.length() > 1)
                    .forEach(searchTerms::add);
        }
        if (keywords != null) {
            Arrays.stream(keywords.split("[\\s,，、]+"))
                    .filter(s -> s.length() > 1)
                    .forEach(searchTerms::add);
        }

        if (searchTerms.isEmpty()) return List.of();

        List<Knowledge> results = new ArrayList<>();
        for (String term : searchTerms) {
            List<Knowledge> matches = knowledgeMapper.findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(term, term);
            for (Knowledge k : matches) {
                if (results.stream().noneMatch(r -> r.getId().equals(k.getId()))) {
                    results.add(k);
                }
            }
        }

        return results.subList(0, Math.min(results.size(), 5));
    }

    @SuppressWarnings("unchecked")
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
                requestBody.put("temperature", 0.7);
                requestBody.put("max_tokens", 4096);

                String responseJson = client.post()
                        .body(objectMapper.writeValueAsString(requestBody))
                        .retrieve()
                        .body(String.class);

                Map<?, ?> responseMap = objectMapper.readValue(responseJson, Map.class);
                String content = extractContent(responseMap);
                if (content != null) {
                    recordUsage(me, responseMap);
                    return content.trim();
                }
            } catch (Exception e) {
                log.warn("模型调用失败: {}", e.getMessage());
                errors.add(e.getMessage());
            }
        }
        log.warn("所有模型均调用失败: {}", String.join("; ", errors));
        return null;
    }

    @SuppressWarnings("unchecked")
    private void recordUsage(LlmEndpoint me, Map<?, ?> responseMap) {
        try {
            Map<String, Object> usage = (Map<String, Object>) responseMap.get("usage");
            if (usage != null) {
                int promptTokens = ((Number) usage.getOrDefault("prompt_tokens", 0)).intValue();
                int completionTokens = ((Number) usage.getOrDefault("completion_tokens", 0)).intValue();
                metricsService.recordTokens(promptTokens, completionTokens);
                tokenUsageService.recordUsage(me.modelConfig, promptTokens, completionTokens, "WRITING", null);
            }
        } catch (Exception e) {
            log.warn("记录 Writing Token 用量失败: {}", e.getMessage());
        }
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

    private record LlmEndpoint(ModelConfig modelConfig, AgentConfig.LlmEndpoint endpoint) {}
}