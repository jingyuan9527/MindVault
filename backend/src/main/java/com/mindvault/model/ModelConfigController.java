package com.mindvault.model;

import com.mindvault.common.annotation.OperationLog;
import com.mindvault.common.annotation.RateLimit;
import com.mindvault.common.dto.ApiResponse;
import com.mindvault.model.entity.ModelConfig;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * LLM 模型配置 REST 接口。
 * <p>
 * 提供模型配置的增删改查、设置主模型、更新优先级、拉取供应商模型列表和连接测试功能。
 * 模型配置支持 OpenAI 兼容 API、Ollama 和 Anthropic 等供应商。
 * </p>
 */
@Tag(name = "模型配置", description = "LLM 模型配置的增删改查（OpenAI/DeepSeek/Alibaba/Ollama）")
@RestController
@RequestMapping("/api/v1/models")
public class ModelConfigController {

    private final ModelConfigService modelConfigService;

    public ModelConfigController(ModelConfigService modelConfigService) {
        this.modelConfigService = modelConfigService;
    }

    @OperationLog(module = "模型配置", action = "新增模型", actionType = "CREATE", entityType = ModelConfig.class)
    @Operation(summary = "新增模型配置", description = "添加一个新的 LLM 模型配置")
    @PostMapping
    public ApiResponse<ModelConfig> addConfig(@Valid @RequestBody ModelConfig config) {
        return ApiResponse.success(modelConfigService.addConfig(config));
    }

    @Operation(summary = "模型配置列表", description = "获取所有 LLM 模型配置")
    @GetMapping
    public ApiResponse<List<ModelConfig>> listAll() {
        return ApiResponse.success(modelConfigService.listAll());
    }

    @OperationLog(module = "模型配置", action = "设为默认模型", actionType = "UPDATE", entityType = ModelConfig.class, recordSnapshot = true)
    @Operation(summary = "设置默认模型", description = "将指定模型设为主要使用的模型")
    @PatchMapping("/{id}/primary")
    public ApiResponse<ModelConfig> setPrimary(@Parameter(description = "模型配置 ID") @PathVariable Long id) {
        return ApiResponse.success(modelConfigService.setPrimary(id));
    }

@OperationLog(module = "模型配置", action = "更新优先级", actionType = "UPDATE", entityType = ModelConfig.class)
    @Operation(summary = "更新模型优先级", description = "更新指定模型的优先级排序")
    @PatchMapping("/{id}/priority")
    public ApiResponse<ModelConfig> updatePriority(@Parameter(description = "模型配置 ID") @PathVariable Long id,
                                                    @RequestBody Integer priority) {
        return ApiResponse.success(modelConfigService.updatePriority(id, priority));
    }

    @OperationLog(module = "模型配置", action = "删除模型", actionType = "DELETE", entityType = ModelConfig.class, recordSnapshot = true)
    @Operation(summary = "删除模型配置", description = "删除指定 ID 的模型配置")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteConfig(@Parameter(description = "模型配置 ID") @PathVariable Long id) {
        modelConfigService.deleteConfig(id);
        return ApiResponse.success(null);
    }

    @RateLimit(capacity = 3, duration = 60, key = "fetch-models")
    @Operation(summary = "获取模型列表", description = "从指定供应商拉取可用模型列表")
    @PostMapping("/fetch")
    public ApiResponse<List<String>> fetchModels(@RequestBody Map<String, String> request) {
        String provider = request.get("provider");
        String apiKey = request.get("apiKey");
        String baseUrl = request.get("baseUrl");
        List<String> models = modelConfigService.fetchAvailableModels(provider, apiKey, baseUrl);
        return ApiResponse.success(models);
    }

    @OperationLog(module = "模型配置", action = "测试连接", actionType = "OTHER")
    @Operation(summary = "测试连接", description = "测试指定模型的 API 连接是否正常")
    @PostMapping("/{id}/test")
    public ApiResponse<Boolean> testConnection(@Parameter(description = "模型配置 ID") @PathVariable Long id) {
        boolean result = modelConfigService.testConnection(id);
        return result
                ? ApiResponse.success(true)
                : ApiResponse.error(400, "连接测试失败，请检查 API Key 等配置");
    }
}