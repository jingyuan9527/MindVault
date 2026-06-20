package com.mindvault.relation;

import com.mindvault.auto.AutoProcessLogMapper;
import com.mindvault.knowledge.KnowledgeMapper;
import com.mindvault.knowledge.KnowledgeService;
import com.mindvault.systemconfig.SystemConfigService;
import com.mindvault.knowledge.entity.Knowledge;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AggregationServiceTest {

    @Mock private KnowledgeMapper knowledgeMapper;
    @Mock private KnowledgeService knowledgeService;
    @Mock private AutoProcessLogMapper logMapper;
    @Mock private SystemConfigService config;

    private AggregationService service;

    @BeforeEach
    void setUp() {
        service = new AggregationService(knowledgeMapper, knowledgeService, logMapper, config);
        lenient().when(config.getInt(anyString(), anyInt())).thenAnswer(i -> i.getArgument(1));
        lenient().when(config.getLong(anyString(), anyLong())).thenAnswer(i -> i.getArgument(1));
        lenient().when(config.getDouble(anyString(), anyDouble())).thenAnswer(i -> i.getArgument(1));
        lenient().when(config.getString(anyString(), anyString())).thenAnswer(i -> i.getArgument(1));
        lenient().when(config.getBool(anyString(), anyBoolean())).thenAnswer(i -> i.getArgument(1));
        lenient().when(config.getPrompt(anyString(), anyString())).thenAnswer(i -> i.getArgument(1));
    }

    @Test
    void processRound3_noPending_shouldSkip() {
        when(knowledgeMapper.findByAutoProcessStatus("RELATION_DONE", 50)).thenReturn(List.of());

        service.processRound3();

        verifyNoInteractions(knowledgeService);
    }

    @Test
    void processRound3_shouldCompletePending() {
        Knowledge k = new Knowledge();
        k.setId(1L);
        k.setTags("[]");
        k.setUserTags("[]");

        when(knowledgeMapper.findByAutoProcessStatus("RELATION_DONE", 50)).thenReturn(List.of(k));
        doNothing().when(knowledgeService).updateAutoProcessStatus(1L, "COMPLETED");

        service.processRound3();

        verify(knowledgeService).updateAutoProcessStatus(1L, "COMPLETED");
    }
}
