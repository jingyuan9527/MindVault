package com.mindvault.content;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindvault.agent.config.AgentConfig;
import com.mindvault.agent.config.AgentConfig.LlmEndpoint;
import com.mindvault.knowledge.KnowledgeService;
import com.mindvault.model.ModelConfigService;
import com.mindvault.model.entity.ModelConfig;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AutoProcessService {

    private static final Logger log = LoggerFactory.getLogger(AutoProcessService.class);

    private final ModelConfigService modelConfigService;
    private final AgentConfig agentConfig;
    private final KnowledgeService knowledgeService;
    private final ObjectMapper objectMapper;

    private RestClient restClient;
    private LlmEndpoint endpoint;

    public AutoProcessService(ModelConfigService modelConfigService,
                              AgentConfig agentConfig,
                              KnowledgeService knowledgeService) {
        this.modelConfigService = modelConfigService;
        this.agentConfig = agentConfig;
        this.knowledgeService = knowledgeService;
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void init() {
        try {
            ModelConfig primaryConfig = modelConfigService.getPrimaryChatModel();
            this.endpoint = agentConfig.buildEndpoint(primaryConfig);
            this.restClient = RestClient.builder()
                    .baseUrl(endpoint.getFullUrl())
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .defaultHeader("Authorization", "Bearer " + endpoint.getApiKey())
                    .build();
            log.info("AutoProcessService 初始化完成");
        } catch (Exception e) {
            log.warn("AutoProcessService 初始化失败（未配置主模型）: {}", e.getMessage());
        }
    }

    public void autoProcess(Long knowledgeId, String title, String content) {
        if (restClient == null) {
            log.warn("未配置主模型，跳过自动处理: knowledgeId={}", knowledgeId);
            return;
        }

        String summary = generateSummary(title, content);
        String tagsJson = generateTags(title, content);

        if (summary != null || tagsJson != null) {
            try {
                com.mindvault.knowledge.entity.Knowledge k = knowledgeService.getById(knowledgeId);
                if (summary != null) {
                    k.setSummary(summary);
                }
                if (tagsJson != null) {
                    k.setTags(tagsJson);
                }
                knowledgeService.updateKnowledge(knowledgeId, k);
                log.info("自动处理完成: knowledgeId={}", knowledgeId);
            } catch (Exception e) {
                log.error("自动处理保存失败: {}", e.getMessage());
            }
        }
    }

    private String generateSummary(String title, String content) {
        try {
            String prompt = "请为以下内容生成一段简洁的中文摘要（50-100字），只返回摘要内容，不要额外说明。\n\n标题: "
                    + title + "\n\n内容: " + truncate(content, 2000);
            String result = callLlm(prompt);
            if (result != null && !result.isBlank()) {
                return result.trim();
            }
        } catch (Exception e) {
            log.warn("生成摘要失败: {}", e.getMessage());
        }
        return null;
    }

    private String generateTags(String title, String content) {
        try {
            String prompt = "请为以下内容生成3-5个中文标签，以JSON数组格式返回，例如 [\"标签1\", \"标签2\", \"标签3\"]。只返回JSON数组，不要额外说明。\n\n标题: "
                    + title + "\n\n内容: " + truncate(content, 2000);
            String result = callLlm(prompt);
            if (result != null && !result.isBlank()) {
                String cleaned = result.trim();
                if (cleaned.startsWith("[") && cleaned.endsWith("]")) {
                    return cleaned;
                }
            }
        } catch (Exception e) {
            log.warn("生成标签失败: {}", e.getMessage());
        }
        return null;
    }

    private String callLlm(String prompt) {
        try {
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", endpoint.getModelName());
            requestBody.put("messages", List.of(Map.of("role", "user", "content", prompt)));
            requestBody.put("temperature", 0.3);
            requestBody.put("max_tokens", 300);

            String responseJson = restClient.post()
                    .body(objectMapper.writeValueAsString(requestBody))
                    .retrieve()
                    .body(String.class);

            Map<?, ?> responseMap = objectMapper.readValue(responseJson, Map.class);
            if (responseMap.containsKey("choices")) {
                List<?> choices = (List<?>) responseMap.get("choices");
                if (!choices.isEmpty()) {
                    Map<?, ?> choice = (Map<?, ?>) choices.get(0);
                    Map<?, ?> message = (Map<?, ?>) choice.get("message");
                    return (String) message.get("content");
                }
            }
            if (responseMap.containsKey("message")) {
                Map<?, ?> message = (Map<?, ?>) responseMap.get("message");
                return (String) message.get("content");
            }
        } catch (Exception e) {
            log.warn("LLM调用失败: {}", e.getMessage());
        }
        return null;
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen);
    }
}