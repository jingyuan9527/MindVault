package com.mindvault.systemconfig.controller;

import com.mindvault.common.annotation.OperationLog;
import com.mindvault.common.dto.ApiResponse;
import com.mindvault.operationlog.service.OperationLogService;
import com.mindvault.systemconfig.entity.SystemConfig;
import com.mindvault.systemconfig.service.SystemConfigDefaults;
import com.mindvault.systemconfig.service.SystemConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "系统配置", description = "动态 KV 配置管理，支持运行时修改不重启")
@RestController
@RequestMapping("/api/v1/system-config")
public class SystemConfigController {

    private final SystemConfigService configService;
    private final OperationLogService operationLogService;

    public SystemConfigController(SystemConfigService configService, OperationLogService operationLogService) {
        this.configService = configService;
        this.operationLogService = operationLogService;
    }

    @Operation(summary = "配置列表", description = "获取所有系统配置项")
    @GetMapping
    public ApiResponse<List<SystemConfig>> listAll() {
        return ApiResponse.success(configService.listAll());
    }

    @Operation(summary = "按模块分组", description = "获取按模块分组的结构化配置数据（新页面使用）")
    @GetMapping("/modules")
    public ApiResponse<Map<String, Object>> getModules() {
        return ApiResponse.success(configService.getModules());
    }

    @Operation(summary = "模块详情", description = "获取指定模块的所有配置项")
    @GetMapping("/modules/{moduleId}")
    public ApiResponse<Map<String, Object>> getModuleDetail(@PathVariable String moduleId) {
        return ApiResponse.success(configService.getModuleDetail(moduleId));
    }

    @Operation(summary = "配置详情", description = "获取配置项详情（含默认值、校验规则、模块归属）")
    @GetMapping("/items/{key}")
    public ApiResponse<Map<String, Object>> getItemDetail(@PathVariable String key) {
        return ApiResponse.success(configService.getItemDetail(key));
    }

    @Operation(summary = "默认值", description = "获取配置项默认值")
    @GetMapping("/items/{key}/default")
    public ApiResponse<String> getDefault(@PathVariable String key) {
        return ApiResponse.success(configService.getDefault(key));
    }

    @Operation(summary = "校验规则", description = "获取配置项校验规则")
    @GetMapping("/items/{key}/validation")
    public ApiResponse<SystemConfigDefaults.ValidationRule> getValidation(@PathVariable String key) {
        return ApiResponse.success(configService.getValidation(key));
    }

    @Operation(summary = "变更记录", description = "获取配置项最近修改记录（复用操作日志）")
    @GetMapping("/items/{key}/audit")
    public ApiResponse<List<com.mindvault.operationlog.entity.OperationLog>> getAudit(@PathVariable String key) {
        var logs = operationLogService.listByModule("系统配置").stream()
                .filter(l -> key.equals(l.getEntityId())
                        || ("更新配置".equals(l.getAction()) && l.getSummary() != null && l.getSummary().contains(key)))
                .limit(10)
                .toList();
        return ApiResponse.success(logs);
    }

    @Operation(summary = "定时任务列表", description = "获取所有定时任务的元数据")
    @GetMapping("/tasks")
    public ApiResponse<List<SystemConfigDefaults.TaskMeta>> getScheduledTasks() {
        return ApiResponse.success(configService.getScheduledTasks());
    }

    @Operation(summary = "获取配置", description = "按 key 获取配置值")
    @GetMapping("/{key}")
    public ApiResponse<String> getByKey(@PathVariable String key) {
        return ApiResponse.success(configService.getString(key, null));
    }

    @OperationLog(module = "系统配置", action = "更新配置", actionType = "UPDATE")
    @Operation(summary = "更新配置", description = "创建或更新配置项")
    @PutMapping("/{key}")
    public ApiResponse<Void> setConfig(@PathVariable String key, @RequestBody SystemConfig config) {
        configService.set(key, config.getConfigValue(), config.getDescription(), config.getValueType());
        return ApiResponse.success(null);
    }

    @OperationLog(module = "系统配置", action = "删除配置", actionType = "DELETE")
    @Operation(summary = "删除配置", description = "删除指定配置项")
    @DeleteMapping("/{key}")
    public ApiResponse<Void> deleteConfig(@PathVariable String key) {
        configService.delete(key);
        return ApiResponse.success(null);
    }

    @OperationLog(module = "系统配置", action = "刷新缓存", actionType = "OTHER")
    @Operation(summary = "刷新缓存", description = "手动刷新配置缓存（从 DB 重新加载）")
    @PostMapping("/refresh")
    public ApiResponse<Void> refresh() {
        configService.refreshCache();
        return ApiResponse.success(null);
    }
}