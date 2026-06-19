package com.mindvault.content;

import com.mindvault.common.service.LlmFailoverService;
import com.mindvault.knowledge.KnowledgeService;
import com.mindvault.model.ModelConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class AutoProcessServiceTest {

    @Mock private ModelConfigService modelConfigService;
    @Mock private LlmFailoverService llmFailoverService;
    @Mock private KnowledgeService knowledgeService;

    private AutoProcessService service;

    @BeforeEach
    void setUp() {
        service = new AutoProcessService(modelConfigService, llmFailoverService, knowledgeService);
    }

    @Test
    void autoProcess_noModels_shouldSkip() {
        when(modelConfigService.getAvailableChatModels()).thenReturn(List.of());

        service.autoProcess(1L, "Test Title", "Test content");

        verifyNoInteractions(knowledgeService);
    }
}