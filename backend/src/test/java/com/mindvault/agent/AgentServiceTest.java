package com.mindvault.agent;

import com.mindvault.agent.tool.Tool;
import com.mindvault.ai.client.AiModelFactory;
import com.mindvault.common.config.CircuitBreakerConfig;
import com.mindvault.common.service.MetricsService;
import com.mindvault.model.ModelConfigService;
import com.mindvault.systemconfig.SystemConfigService;
import com.mindvault.tokenusage.TokenUsageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentServiceTest {

    @Mock private ModelConfigService modelConfigService;
    @Mock private AiModelFactory aiModelFactory;
    @Mock private TokenUsageService tokenUsageService;
    @Mock private CircuitBreakerConfig circuitBreaker;
    @Mock private Tool tool1;
    @Mock private Tool tool2;
    @Mock private MetricsService metricsService;
    @Mock private SystemConfigService config;

    private AgentService service;

    @BeforeEach
    void setUp() {
        List<Tool> tools = List.of(tool1, tool2);
        service = new AgentService(modelConfigService, aiModelFactory, tools,
                tokenUsageService, circuitBreaker, metricsService, config);
    }

    @Test
    void processMessage_noModels_shouldReturnErrorMessage() {
        lenient().when(config.getString(anyString(), anyString())).thenAnswer(i -> i.getArgument(1));

        String result = service.processMessage("hello");

        assertEquals("系统未配置主模型，请先在设置中添加并设置主模型。", result);
        verify(modelConfigService).getAvailableChatModels();
    }

    @Test
    void getTools_shouldReturnInjectedTools() {
        when(tool1.getName()).thenReturn("search");
        when(tool2.getName()).thenReturn("summary");

        List<Tool> result = service.getTools();

        assertEquals(2, result.size());
        assertEquals("search", result.get(0).getName());
        assertEquals("summary", result.get(1).getName());
    }
}