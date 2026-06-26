package com.mindvault.model;

import com.mindvault.ai.client.AiModelFactory;
import com.mindvault.model.entity.ModelConfig;
import com.mindvault.operationlog.OperationLogService;
import com.openai.client.OpenAIClient;
import com.openai.client.OpenAIClientImpl;
import com.openai.core.ClientOptions;
import com.openai.models.models.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 模型配置服务。
 * <p>
 * 核心职责: 管理 LLM 模型配置的增删改查，包括设置主模型、更新优先级、
 * 从供应商拉取可用模型列表、测试连接可用性。提供 failover 所需的可用模型查询能力。
 * </p>
 * <p>
 * 关键设计:
 * <ul>
 *   <li>设置主模型时自动清除同类型所有现有主模型标志，确保只有一个主模型</li>
 *   <li>拉取模型列表: 支持 Ollama（本地 API）、Anthropic（REST API）和 OpenAI 兼容 API（OpenAI Java SDK）</li>
 *   <li>测试连接: 根据模型类型（CHAT/EMBEDDING）选择调用方式，结果记录操作日志</li>
 *   <li>所有变更操作通过 operationLogService 自动记录审计日志</li>
 * </ul>
 * </p>
 * <p>依赖: ModelConfigMapper, OperationLogService, AiModelFactory</p>
 */
@Service
public class ModelConfigService {

    private static final Logger log = LoggerFactory.getLogger(ModelConfigService.class);

    private final ModelConfigMapper mapper;
    private final OperationLogService operationLogService;
    private final AiModelFactory aiModelFactory;

    public ModelConfigService(ModelConfigMapper mapper,
                              OperationLogService operationLogService,
                              AiModelFactory aiModelFactory) {
        this.mapper = mapper;
        this.operationLogService = operationLogService;
        this.aiModelFactory = aiModelFactory;
    }

    /**
     * 新增模型配置。
     * @param config 模型配置实体（含提供商、模型名称、API Key 等必填字段）
     * @return 持久化后的模型配置
     */
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

    /**
     * 获取所有模型配置。
     * @return 全量模型配置列表
     */
    public List<ModelConfig> listAll() {
        return mapper.selectList(null);
    }

    /**
     * 设置主模型。
     * 自动清除同类型（CHAT/EMBEDDING）的所有现有主模型标志。
     * @param id 模型配置 ID
     * @return 更新后的模型配置
     * @throws IllegalArgumentException ID 不存在时抛出
     */
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

    /**
     * 获取主对话模型。
     * @return 主模型配置
     * @throws RuntimeException 未配置主模型时抛出
     */
    public ModelConfig getPrimaryChatModel() {
        return mapper.findByModelTypeAndIsPrimaryTrue("CHAT")
                .orElseThrow(() -> new RuntimeException("未配置主模型，请在设置中添加并设置主模型"));
    }

    /**
     * 获取所有可用的对话模型（已启用，按优先级降序）。
     * @return 可用对话模型列表
     */
    public List<ModelConfig> getAvailableChatModels() {
        return mapper.findByModelTypeAndIsEnabledTrueOrderByPriorityDesc("CHAT");
    }

    /**
     * 获取所有可用的嵌入模型（已启用，按优先级降序）。
     * @return 可用嵌入模型列表
     */
    public List<ModelConfig> getAvailableEmbeddingModels() {
        return mapper.findByModelTypeAndIsEnabledTrueOrderByPriorityDesc("EMBEDDING");
    }

    /**
     * 更新模型优先级。
     * @param id       模型配置 ID
     * @param priority 新优先级（数值越大优先级越高）
     * @return 更新后的模型配置
     * @throws IllegalArgumentException ID 不存在时抛出
     */
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

    /**
     * 从指定供应商拉取可用模型列表。
     * <p>
     * 根据供应商类型选择不同的拉取方式:
     * <ul>
     *   <li>OLLAMA: 调用本地 Ollama API 的 /api/tags 接口</li>
     *   <li>ANTHROPIC: 调用 Anthropic REST API 的 /v1/models 接口</li>
     *   <li>其他: 使用 OpenAI Java SDK 的 /v1/models 接口（兼容 OpenAI/DeepSeek/Alibaba/SiliconFlow）</li>
     * </ul>
     * </p>
     * @param provider 供应商名称（大小写不敏感）
     * @param apiKey   API 密钥
     * @param baseUrl  自定义基础 URL（可选）
     * @return 可用模型名称列表，拉取失败时返回空列表
     */
    public List<String> fetchAvailableModels(String provider, String apiKey, String baseUrl) {
        String upper = provider.toUpperCase();
        try {
            return switch (upper) {
                case "OLLAMA" -> fetchOllamaModels(baseUrl);
                case "ANTHROPIC" -> fetchAnthropicModels(apiKey, baseUrl);
                default -> fetchOpenAiCompatibleModels(upper, apiKey, baseUrl);
            };
        } catch (Exception e) {
            log.warn("拉取模型列表失败: provider={}, error={}", provider, e.getMessage());
            return List.of();
        }
    }

    private List<String> fetchOllamaModels(String baseUrl) {
        String url = baseUrl != null ? baseUrl : "http://localhost:11434";
        OllamaApi api = OllamaApi.builder().baseUrl(url).build();
        return api.listModels().models().stream()
                .map(OllamaApi.Model::name)
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private List<String> fetchAnthropicModels(String apiKey, String baseUrl) {
        String modelUrl = (baseUrl != null ? baseUrl : "https://api.anthropic.com") + "/v1/models";
        String json = RestClient.builder()
                .baseUrl(modelUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("anthropic-version", "2023-06-01")
                .build()
                .get()
                .retrieve()
                .body(String.class);
        List<String> models = new ArrayList<>();
        try {
            com.fasterxml.jackson.databind.ObjectMapper localMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> root = localMapper.readValue(json, Map.class);
            if (root.containsKey("data")) {
                List<Map<String, Object>> data = (List<Map<String, Object>>) root.get("data");
                for (Map<String, Object> item : data) {
                    if (item.get("id") instanceof String id) models.add(id);
                }
            }
        } catch (Exception e) {
            log.warn("解析 Anthropic 模型列表 JSON 失败: {}", e.getMessage());
        }
        return models;
    }

    private List<String> fetchOpenAiCompatibleModels(String provider, String apiKey, String baseUrl) {
        String url = resolveBaseUrl(provider, baseUrl);
        ClientOptions options = ClientOptions.Companion.builder()
                .baseUrl(url)
                .apiKey(apiKey)
                .build();
        OpenAIClient client = new OpenAIClientImpl(options);
        return client.models().list().data().stream()
                .map(Model::id)
                .collect(Collectors.toList());
    }

    private static String resolveBaseUrl(String provider, String baseUrl) {
        String effective = (baseUrl != null && !baseUrl.isBlank()) ? baseUrl : null;
        return switch (provider) {
            case "ALIYUN" -> effective != null ? effective : AiModelFactory.ALIYUN_DEFAULT_BASE_URL;
            case "DEEPSEEK" -> effective != null ? effective : AiModelFactory.DEEPSEEK_DEFAULT_BASE_URL;
            case "OPENAI" -> effective != null ? effective : AiModelFactory.OPENAI_DEFAULT_BASE_URL;
            case "SILICONFLOW" -> effective != null ? effective : AiModelFactory.SILICONFLOW_DEFAULT_BASE_URL;
            default -> effective;
        };
    }

    /**
     * 删除模型配置。
     * @param id 模型配置 ID
     * @throws IllegalArgumentException ID 不存在时抛出
     */
    @Transactional
    public void deleteConfig(Long id) {
        ModelConfig config = Optional.ofNullable(mapper.selectById(id))
                .orElseThrow(() -> new IllegalArgumentException("模型配置不存在: " + id));
        mapper.deleteById(id);
        log.info("删除模型配置: id={}", id);
        operationLogService.log("MODEL", "DELETE", id,
                "删除模型 " + config.getProvider() + "/" + config.getModelName());
    }

    /**
     * 测试模型连接是否正常。
     * <p>
     * 根据模型类型（CHAT/EMBEDDING）分别通过 AiModelFactory 构建对应的模型实例，
     * 发送一条简单消息验证连通性。结果记录操作日志。
     * </p>
     * @param id 模型配置 ID
     * @return true 连接成功，false 连接失败
     * @throws IllegalArgumentException ID 不存在时抛出
     */
    public boolean testConnection(Long id) {
        ModelConfig config = Optional.ofNullable(mapper.selectById(id))
                .orElseThrow(() -> new IllegalArgumentException("模型配置不存在: " + id));

        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            log.warn("测试模型连接失败: API Key 为空, id={}", id);
            return false;
        }

        try {
            if ("EMBEDDING".equals(config.getModelType())) {
                EmbeddingModel embeddingModel = aiModelFactory.buildEmbeddingModel(config);
                embeddingModel.embed("Hi");
            } else {
                ChatModel chatModel = aiModelFactory.buildChatModel(config);
                chatModel.call("Hi");
            }
            log.info("测试模型连接成功: id={}, provider={}, model={}, type={}",
                    id, config.getProvider(), config.getModelName(), config.getModelType());
            operationLogService.log("MODEL", "TEST", id,
                    "测试模型 " + config.getProvider() + "/" + config.getModelName() + ": 成功");
            return true;
        } catch (Exception e) {
            log.warn("测试模型连接失败: id={}, error={}", id, e.getMessage());
            operationLogService.log("MODEL", "TEST", id,
                    "测试模型 " + config.getProvider() + "/" + config.getModelName() + ": " + e.getMessage());
            return false;
        }
    }
}