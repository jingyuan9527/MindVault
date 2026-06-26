package com.mindvault.common.config;

import com.mindvault.auth.entity.User;
import com.mindvault.auth.mapper.UserMapper;
import com.mindvault.common.service.SnapshotProvider;
import com.mindvault.knowledge.KnowledgeMapper;
import com.mindvault.knowledge.entity.Knowledge;
import com.mindvault.model.ModelConfigMapper;
import com.mindvault.model.entity.ModelConfig;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * 快照 Mapper 注册配置
 *
 * 在应用启动时（@PostConstruct）将需要支持数据快照的实体类及其 Mapper
 * 注册到 SnapshotProvider 中，以便 OperationLogAspect 在操作日志中
 * 自动录制操作前后的数据快照。
 *
 * 当前支持的快照实体：
 * - Knowledge（知识条目）
 * - ModelConfig（模型配置）
 * - User（用户信息）
 */
@Component
public class SnapshotRegistryConfig {

    private final SnapshotProvider snapshotProvider;
    private final KnowledgeMapper knowledgeMapper;
    private final ModelConfigMapper modelConfigMapper;
    private final UserMapper userMapper;

    public SnapshotRegistryConfig(SnapshotProvider snapshotProvider,
                                  KnowledgeMapper knowledgeMapper,
                                  ModelConfigMapper modelConfigMapper,
                                  UserMapper userMapper) {
        this.snapshotProvider = snapshotProvider;
        this.knowledgeMapper = knowledgeMapper;
        this.modelConfigMapper = modelConfigMapper;
        this.userMapper = userMapper;
    }

    @PostConstruct
    public void registerMappers() {
        snapshotProvider.register(Knowledge.class, knowledgeMapper);
        snapshotProvider.register(ModelConfig.class, modelConfigMapper);
        snapshotProvider.register(User.class, userMapper);
    }
}