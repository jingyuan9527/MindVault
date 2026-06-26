package com.mindvault.agent;

import com.mindvault.agent.service.AgentService;
import com.mindvault.agent.service.AgentServiceImpl;
import com.mindvault.agent.tool.AddKnowledgeTool;
import com.mindvault.agent.tool.SearchKnowledgeTool;
import com.mindvault.ai.client.AiModelFactory;
import com.mindvault.common.service.MetricsService;
import com.mindvault.knowledge.service.KnowledgeService;
import com.mindvault.knowledge.service.SearchEnhanceService;
import com.mindvault.model.service.ModelConfigService;
import com.mindvault.model.entity.ModelConfig;
import com.mindvault.systemconfig.service.SystemConfigService;
import com.mindvault.tokenusage.service.TokenUsageService;
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
    @Mock private TokenUsageService tokenUsageService;

    private AgentService service;

    @BeforeEach
    void setUp() {
        SearchKnowledgeTool searchTool = new SearchKnowledgeTool(knowledgeService, searchEnhanceService, config);
        AddKnowledgeTool addTool = new AddKnowledgeTool(knowledgeService);
        service = new AgentServiceImpl(modelConfigService, aiModelFactory, searchTool, addTool,
                metricsService, config, tokenUsageService);
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