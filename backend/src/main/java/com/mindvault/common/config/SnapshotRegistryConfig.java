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