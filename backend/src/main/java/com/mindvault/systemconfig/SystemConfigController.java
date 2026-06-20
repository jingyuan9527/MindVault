package com.mindvault.systemconfig;

import com.mindvault.common.dto.ApiResponse;
import com.mindvault.systemconfig.entity.SystemConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "系统配置", description = "动态 KV 配置管理，支持运行时修改不重启")
@RestController
@RequestMapping("/api/v1/system-config")
public class SystemConfigController {

    private final SystemConfigService configService;

    public SystemConfigController(SystemConfigService configService) {
        this.configService = configService;
    }

    @Operation(summary = "配置列表", description = "获取所有系统配置项")
    @GetMapping
    public ApiResponse<List<SystemConfig>> listAll() {
        return ApiResponse.success(configService.listAll());
    }

    @Operation(summary = "获取配置", description = "按 key 获取配置值")
    @GetMapping("/{key}")
    public ApiResponse<String> getByKey(@PathVariable String key) {
        return ApiResponse.success(configService.getString(key, null));
    }

    @Operation(summary = "更新配置", description = "创建或更新配置项")
    @PutMapping("/{key}")
    public ApiResponse<Void> setConfig(@PathVariable String key, @RequestBody SystemConfig config) {
        configService.set(key, config.getConfigValue(), config.getDescription(), config.getValueType());
        return ApiResponse.success(null);
    }

    @Operation(summary = "删除配置", description = "删除指定配置项")
    @DeleteMapping("/{key}")
    public ApiResponse<Void> deleteConfig(@PathVariable String key) {
        configService.delete(key);
        return ApiResponse.success(null);
    }

    @Operation(summary = "刷新缓存", description = "手动刷新配置缓存（从 DB 重新加载）")
    @PostMapping("/refresh")
    public ApiResponse<Void> refresh() {
        configService.refreshCache();
        return ApiResponse.success(null);
    }
}
