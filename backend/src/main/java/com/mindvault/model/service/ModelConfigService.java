package com.mindvault.model.service;

import com.mindvault.model.entity.ModelConfig;

import java.util.List;

public interface ModelConfigService {

    ModelConfig addConfig(ModelConfig config);

    List<ModelConfig> listAll();

    ModelConfig setPrimary(Long id);

    ModelConfig getPrimaryChatModel();

    List<ModelConfig> getAvailableChatModels();

    List<ModelConfig> getAvailableEmbeddingModels();

    ModelConfig updatePriority(Long id, int priority);

    List<String> fetchAvailableModels(String provider, String apiKey, String baseUrl);

    void deleteConfig(Long id);

    boolean testConnection(Long id);
}