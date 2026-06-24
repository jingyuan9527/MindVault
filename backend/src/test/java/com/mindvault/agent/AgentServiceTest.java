package com.mindvault.agent;

import com.mindvault.agent.tool.AddKnowledgeTool;
import com.mindvault.agent.tool.SearchKnowledgeTool;
import com.mindvault.ai.client.AiModelFactory;
import com.mindvault.common.service.MetricsService;
import com.mindvault.knowledge.KnowledgeService;
import com.mindvault.knowledge.SearchEnhanceService;
import com.mindvault.model.ModelConfigService;
import com.mindvault.model.entity.ModelConfig;
import com.mindvault.systemconfig.SystemConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentServiceTest {

    @Mock private ModelConfigService modelConfigService;
    @Mock private AiModelFactory aiModelFactory;
    @Mock private MetricsService metricsService;
    @Mock private SystemConfigService config;
    @Mock private KnowledgeService knowledgeService;
    @Mock private SearchEnhanceService searchEnhanceService;

    private AgentService service;

    @BeforeEach
    void setUp() {
        SearchKnowledgeTool searchTool = new SearchKnowledgeTool(knowledgeService, searchEnhanceService, config);
        AddKnowledgeTool addTool = new AddKnowledgeTool(knowledgeService);
        service = new AgentService(modelConfigService, aiModelFactory, searchTool, addTool,
                metricsService, config);
        lenient().when(config.getInt(anyString(), anyInt())).thenAnswer(i -> i.getArgument(1));
        lenient().when(config.getDouble(anyString(), anyDouble())).thenAnswer(i -> i.getArgument(1));
        lenient().when(config.getString(anyString(), anyString())).thenAnswer(i -> i.getArgument(1));
        lenient().when(config.getPrompt(anyString(), anyString())).thenAnswer(i -> i.getArgument(1));
    }

    @Test
    void processMessage_noModels_shouldReturnErrorMessage() {
        when(modelConfigService.getPrimaryChatModel()).thenThrow(new RuntimeException("未配置主模型"));

        String result = service.processMessage("hello");

        assertTrue(result.contains("抱歉"));
    }
}