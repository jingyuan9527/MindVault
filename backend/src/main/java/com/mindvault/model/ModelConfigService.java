package com.mindvault.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindvault.model.entity.ModelConfig;
import com.mindvault.operationlog.OperationLogService;
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
public class ModelConfigService {

    private static final Logger log = LoggerFactory.getLogger(ModelConfigService.class);

    private final ModelConfigMapper mapper;
    private final OperationLogService operationLogService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ModelConfigService(ModelConfigMapper mapper,
                              OperationLogService operationLogService) {
        this.mapper = mapper;
        this.operationLogService = operationLogService;
    }

    @Transactional
    public ModelConfig addConfig(ModelConfig config) {
        LocalDateTime now = LocalDateTime.now();
        config.setCreatedAt(now);
        config.setUpdatedAt(now);
        mapper.insert(config);
        log.info("添加模型配置: provider={}, model={}, type={}",
                config.getProvider(), config.getModelName(), config.getModelType());
        operationLogService.log("MODEL", "ADD", config.getId(),
                "添加模型 " + config.getProvider() + "/" + config.getModelName());
        return config;
    }

    public List<ModelConfig> listAll() {
        return mapper.selectList(null);
    }

    @Transactional
    public ModelConfig setPrimary(Long id) {
        ModelConfig config = Optional.ofNullable(mapper.selectById(id))
                .orElseThrow(() -> new IllegalArgumentException("模型配置不存在: " + id));

        List<ModelConfig> sameTypePrimary = mapper.selectList(null).stream()
                .filter(mc -> mc.getIsPrimary() && config.getModelType().equals(mc.getModelType()))
                .toList();
        for (ModelConfig old : sameTypePrimary) {
            old.setIsPrimary(false);
            old.setUpdatedAt(LocalDateTime.now());
            mapper.updateById(old);
        }

        config.setIsPrimary(true);
        config.setUpdatedAt(LocalDateTime.now());
        mapper.updateById(config);

        log.info("设置主模型: id={}, provider={}, model={}", id,
                config.getProvider(), config.getModelName());
        operationLogService.log("MODEL", "SET_PRIMARY", id,
                "设置主模型 " + config.getProvider() + "/" + config.getModelName());
        return config;
    }

    public ModelConfig getPrimaryChatModel() {
        return mapper.findByModelTypeAndIsPrimaryTrue("CHAT")
                .orElseThrow(() -> new RuntimeException("未配置主模型，请在设置中添加并设置主模型"));
    }

    public List<ModelConfig> getAvailableChatModels() {
        return mapper.findByModelTypeAndIsEnabledTrueOrderByPriorityDesc("CHAT");
    }

    public List<ModelConfig> getAvailableEmbeddingModels() {
        return mapper.findByModelTypeAndIsEnabledTrueOrderByPriorityDesc("EMBEDDING");
    }

    @Transactional
    public ModelConfig updatePriority(Long id, int priority) {
        ModelConfig config = Optional.ofNullable(mapper.selectById(id))
                .orElseThrow(() -> new IllegalArgumentException("模型配置不存在: " + id));
        config.setPriority(priority);
        config.setUpdatedAt(LocalDateTime.now());
        mapper.updateById(config);
        log.info("更新模型优先级: id={}, priority={}", id, priority);
        operationLogService.log("MODEL", "UPDATE_PRIORITY", id,
                "更新模型 " + config.getProvider() + "/" + config.getModelName() + " 优先级为 " + priority);
        return config;
    }

    public List<String> fetchAvailableModels(String provider, String apiKey, String baseUrl) {
        String modelUrl = resolveListModelsUrl(provider, baseUrl);
        if (modelUrl == null) return List.of();

        try {
            RestClient.Builder builder = RestClient.builder().baseUrl(modelUrl)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            if (apiKey != null && !apiKey.isBlank()) {
                builder.defaultHeader("Authorization", "Bearer " + apiKey);
            }

            String responseJson = builder.build().get()
                    .retrieve()
                    .body(String.class);

            return parseModelList(provider, responseJson);
        } catch (Exception e) {
            log.warn("拉取模型列表失败: provider={}, error={}", provider, e.getMessage());
            return List.of();
        }
    }

    private String resolveListModelsUrl(String provider, String baseUrl) {
        return switch (provider.toUpperCase()) {
            case "ALIYUN" -> "https://dashscope.aliyuncs.com/compatible-mode/v1/models";
            case "DEEPSEEK" -> (baseUrl != null ? baseUrl : "https://api.deepseek.com") + "/v1/models";
            case "OPENAI" -> (baseUrl != null ? baseUrl : "https://api.openai.com") + "/v1/models";
            case "ANTHROPIC" -> (baseUrl != null ? baseUrl : "https://api.anthropic.com") + "/v1/models";
            case "OLLAMA" -> (baseUrl != null ? baseUrl : "http://localhost:11434") + "/api/tags";
            default -> null;
        };
    }

    @SuppressWarnings("unchecked")
    private List<String> parseModelList(String provider, String json) {
        List<String> models = new ArrayList<>();
        try {
            Map<String, Object> root = objectMapper.readValue(json, Map.class);
            if (root.containsKey("data")) {
                List<Map<String, Object>> data = (List<Map<String, Object>>) root.get("data");
                for (Map<String, Object> item : data) {
                    if (item.get("id") instanceof String id) models.add(id);
                }
            } else if (root.containsKey("models")) {
                List<Map<String, Object>> data = (List<Map<String, Object>>) root.get("models");
                for (Map<String, Object> item : data) {
                    if (item.get("name") instanceof String name) models.add(name);
                }
            }
        } catch (Exception e) {
            log.warn("解析模型列表 JSON 失败: {}", e.getMessage());
        }
        return models;
    }

    @Transactional
    public void deleteConfig(Long id) {
        ModelConfig config = Optional.ofNullable(mapper.selectById(id))
                .orElseThrow(() -> new IllegalArgumentException("模型配置不存在: " + id));
        mapper.deleteById(id);
        log.info("删除模型配置: id={}", id);
        operationLogService.log("MODEL", "DELETE", id,
                "删除模型 " + config.getProvider() + "/" + config.getModelName());
    }

    public boolean testConnection(Long id) {
        ModelConfig config = Optional.ofNullable(mapper.selectById(id))
                .orElseThrow(() -> new IllegalArgumentException("模型配置不存在: " + id));

        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            log.warn("测试模型连接失败: API Key 为空, id={}", id);
            return false;
        }

        String fullUrl = buildTestUrl(config);
        if (fullUrl == null) {
            log.warn("测试模型连接失败: 不支持的 provider={}, id={}", config.getProvider(), id);
            return false;
        }

        try {
            RestClient.Builder builder = RestClient.builder()
                    .baseUrl(fullUrl)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

            if ("ANTHROPIC".equalsIgnoreCase(config.getProvider())) {
                builder.defaultHeader("x-api-key", config.getApiKey());
                builder.defaultHeader("anthropic-version", "2023-06-01");
            } else {
                builder.defaultHeader("Authorization", "Bearer " + config.getApiKey());
            }

            Map<String, Object> requestBody = buildTestRequestBody(config);
            String jsonBody = objectMapper.writeValueAsString(requestBody);

            String responseJson = builder.build().post()
                    .body(jsonBody)
                    .retrieve()
                    .body(String.class);

            Map<?, ?> responseMap = objectMapper.readValue(responseJson, Map.class);
            boolean ok = responseMap != null && (
                    responseMap.containsKey("choices") ||
                    responseMap.containsKey("content") ||
                    responseMap.containsKey("message") ||
                    responseMap.containsKey("response"));

            log.info("测试模型连接: id={}, provider={}, model={}, result={}",
                    id, config.getProvider(), config.getModelName(), ok ? "OK" : "FAIL");
            operationLogService.log("MODEL", "TEST", id,
                    "测试模型 " + config.getProvider() + "/" + config.getModelName() + ": " + (ok ? "成功" : "失败"));
            return ok;
        } catch (Exception e) {
            log.warn("测试模型连接失败: id={}, error={}", id, e.getMessage());
            operationLogService.log("MODEL", "TEST", id,
                    "测试模型 " + config.getProvider() + "/" + config.getModelName() + ": " + e.getMessage());
            return false;
        }
    }

    private String buildTestUrl(ModelConfig config) {
        return switch (config.getProvider().toUpperCase()) {
            case "ALIYUN" -> "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";
            case "DEEPSEEK" -> (config.getBaseUrl() != null ? config.getBaseUrl() : "https://api.deepseek.com/v1") + "/chat/completions";
            case "OPENAI" -> (config.getBaseUrl() != null ? config.getBaseUrl() : "https://api.openai.com/v1") + "/chat/completions";
            case "ANTHROPIC" -> (config.getBaseUrl() != null ? config.getBaseUrl() : "https://api.anthropic.com") + "/v1/messages";
            case "OLLAMA" -> (config.getBaseUrl() != null ? config.getBaseUrl() : "http://localhost:11434") + "/api/chat";
            default -> null;
        };
    }

    private Map<String, Object> buildTestRequestBody(ModelConfig config) {
        Map<String, Object> body = new LinkedHashMap<>();
        if ("ANTHROPIC".equalsIgnoreCase(config.getProvider())) {
            body.put("model", config.getModelName());
            body.put("max_tokens", 10);
            body.put("messages", List.of(Map.of("role", "user", "content", "Hi")));
        } else if ("OLLAMA".equalsIgnoreCase(config.getProvider())) {
            body.put("model", config.getModelName());
            body.put("messages", List.of(Map.of("role", "user", "content", "Hi")));
            body.put("stream", false);
        } else {
            body.put("model", config.getModelName());
            body.put("messages", List.of(Map.of("role", "user", "content", "Hi")));
            body.put("max_tokens", 10);
        }
        return body;
    }
}