package com.mindvault.relation;

import com.mindvault.auto.AutoProcessLogMapper;
import com.mindvault.common.service.LlmFailoverService;
import com.mindvault.knowledge.KnowledgeMapper;
import com.mindvault.knowledge.KnowledgeService;
import com.mindvault.knowledge.entity.Knowledge;
import com.mindvault.model.ModelConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RelationServiceTest {

    @Mock private KnowledgeMapper knowledgeMapper;
    @Mock private KnowledgeRelationMapper relationMapper;
    @Mock private KnowledgeService knowledgeService;
    @Mock private ModelConfigService modelConfigService;
    @Mock private LlmFailoverService llmFailoverService;
    @Mock private AutoProcessLogMapper logMapper;

    private RelationService service;

    @BeforeEach
    void setUp() {
        service = new RelationService(knowledgeMapper, relationMapper, knowledgeService,
                modelConfigService, llmFailoverService, logMapper);
    }

    @Test
    void processRound2_noPending_shouldSkip() {
        when(knowledgeMapper.findByAutoProcessStatus("TITLE_TAG_DONE", 20)).thenReturn(List.of());

        service.processRound2();

        verifyNoInteractions(relationMapper);
    }

    @Test
    void processRound2_shouldProcessPending() {
        Knowledge k = new Knowledge();
        k.setId(1L);
        k.setTitle("Test");
        k.setContent("Content");
        k.setTags("[]");
        k.setUserTags("[]");
        k.setEmbedding("[0.1,0.2]");

        when(knowledgeMapper.findByAutoProcessStatus("TITLE_TAG_DONE", 20)).thenReturn(List.of(k));
        when(knowledgeMapper.findByAutoProcessStatus("COMPLETED", 50)).thenReturn(List.of());
        doNothing().when(knowledgeService).updateAutoProcessStatus(1L, "RELATION_DONE");

        service.processRound2();

        verify(knowledgeService).updateAutoProcessStatus(1L, "RELATION_DONE");
    }
}
