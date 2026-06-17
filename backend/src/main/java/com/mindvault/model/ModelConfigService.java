package com.mindvault.model;

import com.mindvault.model.entity.ModelConfig;
import com.mindvault.operationlog.OperationLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 模型配置服务
 *
 * 核心职责：
 * 1. 模型 CRUD 管理
 * 2. 主模型切换（事务保证一个主模型约束）
 * 3. 获取当前可用模型（供 Agent 和 Chat 模块使用）
 */
@Service
public class ModelConfigService {

    private static final Logger log = LoggerFactory.getLogger(ModelConfigService.class);

    private final ModelConfigRepository repository;
    private final OperationLogService operationLogService;

    public ModelConfigService(ModelConfigRepository repository,
                              OperationLogService operationLogService) {
        this.repository = repository;
        this.operationLogService = operationLogService;
    }

    /** 添加模型配置 */
    @Transactional
    public ModelConfig addConfig(ModelConfig config) {
        ModelConfig saved = repository.save(config);
        log.info("添加模型配置: provider={}, model={}, type={}",
                config.getProvider(), config.getModelName(), config.getModelType());
        operationLogService.log("MODEL", "ADD", saved.getId(),
                "添加模型 " + config.getProvider() + "/" + config.getModelName());
        return saved;
    }

    /** 获取全部模型列表 */
    public List<ModelConfig> listAll() {
        return repository.findAll();
    }

    /**
     * 设置为主模型（自动取消旧主模型）
     *
     * V2 迁移的全局唯一索引 idx_single_primary 要求全表只有一个 is_primary = true 的记录，
     * 因此需要先清除所有类型的旧主模型。
     */
    @Transactional
    public ModelConfig setPrimary(Long id) {
        ModelConfig config = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("模型配置不存在: " + id));

        // 清除所有类型的旧主模型（全局唯一约束）
        List<ModelConfig> allPrimary = repository.findAll().stream()
                .filter(ModelConfig::getIsPrimary)
                .toList();
        for (ModelConfig old : allPrimary) {
            old.setIsPrimary(false);
            repository.save(old);
        }

        // 设置新主模型
        config.setIsPrimary(true);
        ModelConfig saved = repository.save(config);

        log.info("设置主模型: id={}, provider={}, model={}", id,
                config.getProvider(), config.getModelName());
        operationLogService.log("MODEL", "SET_PRIMARY", id,
                "设置主模型 " + config.getProvider() + "/" + config.getModelName());
        return saved;
    }

    /** 获取当前可用的 CHAT 主模型 */
    public ModelConfig getPrimaryChatModel() {
        return repository.findByModelTypeAndIsPrimaryTrue("CHAT")
                .orElseThrow(() -> new RuntimeException("未配置主模型，请在设置中添加并设置主模型"));
    }

    /** 获取所有可用聊天模型（按优先级降序），用于故障降级 */
    public List<ModelConfig> getAvailableChatModels() {
        return repository.findByModelTypeAndIsEnabledTrueOrderByPriorityDesc("CHAT");
    }

    /** 获取所有可用嵌入模型 */
    public List<ModelConfig> getAvailableEmbeddingModels() {
        return repository.findByModelTypeAndIsEnabledTrueOrderByPriorityDesc("EMBEDDING");
    }

    /** 删除模型配置 */
    @Transactional
    public void deleteConfig(Long id) {
        ModelConfig config = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("模型配置不存在: " + id));
        repository.deleteById(id);
        log.info("删除模型配置: id={}", id);
        operationLogService.log("MODEL", "DELETE", id,
                "删除模型 " + config.getProvider() + "/" + config.getModelName());
    }

    /** 测试模型连接（v0.1 简化版本：只验证配置是否完整） */
    public boolean testConnection(Long id) {
        ModelConfig config = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("模型配置不存在: " + id));
        // v0.1: 简单校验必填字段
        // v0.3: 实际调用 LLM API 做健康检查
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            return false;
        }
        log.info("测试模型连接: id={}, result=OK", id);
        operationLogService.log("MODEL", "TEST", id,
                "测试模型 " + config.getProvider() + "/" + config.getModelName());
        return true;
    }
}