package com.mindvault.content;

import com.mindvault.auto.AutoProcessLogMapper;
import com.mindvault.common.service.LlmFailoverService;
import com.mindvault.knowledge.KnowledgeService;
import com.mindvault.knowledge.entity.Knowledge;
import com.mindvault.model.ModelConfigService;
import com.mindvault.model.entity.ModelConfig;
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
    @Mock private AutoProcessLogMapper logMapper;

    private AutoProcessService service;

    @BeforeEach
    void setUp() {
        service = new AutoProcessService(modelConfigService, llmFailoverService, knowledgeService, logMapper);
    }

    @Test
    void autoProcess_noModels_shouldSkip() {
        when(modelConfigService.getAvailableChatModels()).thenReturn(List.of());

        service.autoProcess(1L, "Test Title", "Test content");

        verifyNoInteractions(knowledgeService);
    }

    @Test
    void autoProcess_shouldGenerateAiTitleAndTags() {
        ModelConfig model = new ModelConfig();
        model.setModelName("gpt-4o");
        when(modelConfigService.getAvailableChatModels()).thenReturn(List.of(model));
        when(llmFailoverService.call(anyList(), any()))
                .thenReturn("AI Title")
                .thenReturn("[\"tag1\", \"tag2\"]")
                .thenReturn("A summary here");

        Knowledge k = new Knowledge();
        k.setId(1L);
        when(knowledgeService.getById(1L)).thenReturn(k);

        service.autoProcess(1L, "User Title", "Content");

        verify(knowledgeService).updateAiFields(1L, "AI Title", "[\"tag1\", \"tag2\"]");
        verify(knowledgeService).updateAutoProcessStatus(1L, "TITLE_TAG_DONE");
    }

    @Test
    void autoProcess_shouldHandleLlmFailure() {
        ModelConfig model = new ModelConfig();
        model.setModelName("gpt-4o");
        when(modelConfigService.getAvailableChatModels()).thenReturn(List.of(model));
        when(llmFailoverService.call(anyList(), any())).thenReturn(null);

        service.autoProcess(1L, "User Title", "Content");

        verify(knowledgeService, never()).updateAiFields(any(), any(), any());
    }
}
