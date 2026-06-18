package com.mindvault.model;

import com.mindvault.common.dto.ApiResponse;
import com.mindvault.model.entity.ModelConfig;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模型配置 REST API
 *
 * 提供 Web 端模型管理的后端接口
 */
@RestController
@RequestMapping("/api/v1/models")
public class ModelConfigController {

    private final ModelConfigService modelConfigService;

    public ModelConfigController(ModelConfigService modelConfigService) {
        this.modelConfigService = modelConfigService;
    }

    @PostMapping
    public ApiResponse<ModelConfig> addConfig(@Valid @RequestBody ModelConfig config) {
        return ApiResponse.success(modelConfigService.addConfig(config));
    }

    @GetMapping
    public ApiResponse<List<ModelConfig>> listAll() {
        return ApiResponse.success(modelConfigService.listAll());
    }

    @PatchMapping("/{id}/primary")
    public ApiResponse<ModelConfig> setPrimary(@PathVariable Long id) {
        return ApiResponse.success(modelConfigService.setPrimary(id));
    }

    @PatchMapping("/{id}/priority")
    public ApiResponse<ModelConfig> updatePriority(@PathVariable Long id,
                                                   @RequestBody Integer priority) {
        return ApiResponse.success(modelConfigService.updatePriority(id, priority));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteConfig(@PathVariable Long id) {
        modelConfigService.deleteConfig(id);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/test")
    public ApiResponse<Boolean> testConnection(@PathVariable Long id) {
        boolean result = modelConfigService.testConnection(id);
        return result
                ? ApiResponse.success(true)
                : ApiResponse.error(400, "连接测试失败，请检查 API Key 等配置");
    }
}