package com.mindvault.content;

import com.mindvault.agent.config.AgentConfig;
import com.mindvault.knowledge.KnowledgeService;
import com.mindvault.model.ModelConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutoProcessServiceTest {

    @Mock private ModelConfigService modelConfigService;
    @Mock private AgentConfig agentConfig;
    @Mock private KnowledgeService knowledgeService;

    private AutoProcessService service;

    @BeforeEach
    void setUp() {
        service = new AutoProcessService(modelConfigService, agentConfig, knowledgeService);
    }

    @Test
    void autoProcess_noModels_shouldSkip() {
        service.autoProcess(1L, "Test Title", "Test content");

        verifyNoInteractions(knowledgeService);
    }

    @Test
    void refreshModels_whenException_shouldHandleGracefully() {
        when(modelConfigService.getAvailableChatModels()).thenThrow(new RuntimeException("DB error"));

        service.refreshModels();

        verifyNoInteractions(agentConfig);
        verifyNoInteractions(knowledgeService);
    }
}