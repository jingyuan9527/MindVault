package com.mindvault.relation;

import com.mindvault.auto.mapper.AutoProcessLogMapper;
import com.mindvault.auto.r3.AggregationService;
import com.mindvault.auto.r3.AggregationServiceImpl;
import com.mindvault.auto.config.AutoThresholdProperties;
import com.mindvault.knowledge.mapper.KnowledgeMapper;
import com.mindvault.knowledge.service.KnowledgeService;
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
    private AutoThresholdProperties autoThresholdProperties;

    private AggregationService service;

    @BeforeEach
    void setUp() {
        autoThresholdProperties = new AutoThresholdProperties();
        service = new AggregationServiceImpl(knowledgeMapper, knowledgeService, logMapper, autoThresholdProperties);
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
        when(knowledgeMapper.selectList(null)).thenReturn(List.of());

        service.processRound3();

        verify(knowledgeService).updateAutoProcessStatus(1L, "COMPLETED");
    }

    @Test
    void rebuildTagCloud_shouldCountTags() {
        Knowledge k1 = new Knowledge();
        k1.setTags("[\"java\", \"spring\"]");
        k1.setUserTags("[\"backend\"]");

        Knowledge k2 = new Knowledge();
        k2.setTags("[\"java\", \"kotlin\"]");
        k2.setUserTags("[]");

        Knowledge k3 = new Knowledge();
        k3.setTags("[]");
        k3.setUserTags("[]");

        when(knowledgeMapper.selectList(null)).thenReturn(List.of(k1, k2, k3));

        service.rebuildTagCloud();

        verify(knowledgeMapper).selectList(null);
    }

    @Test
    void rebuildTagCloud_shouldHandleNullTagsGracefully() {
        Knowledge k = new Knowledge();
        k.setTags(null);
        k.setUserTags(null);

        when(knowledgeMapper.selectList(null)).thenReturn(List.of(k));

        service.rebuildTagCloud();

        verify(knowledgeMapper).selectList(null);
    }

    @Test
    void processRound3_shouldSaveLogOnSuccess() {
        Knowledge k = new Knowledge();
        k.setId(1L);
        k.setTags("[]");
        k.setUserTags("[]");

        when(knowledgeMapper.findByAutoProcessStatus("RELATION_DONE", 50)).thenReturn(List.of(k));
        doNothing().when(knowledgeService).updateAutoProcessStatus(1L, "COMPLETED");
        when(knowledgeMapper.selectList(null)).thenReturn(List.of());

        service.processRound3();

        verify(logMapper, atLeastOnce()).insert(any(com.mindvault.auto.entity.AutoProcessLog.class));
    }

    @Test
    void processRound3_shouldHandleUpdateFailure() {
        Knowledge k = new Knowledge();
        k.setId(1L);
        k.setTags("[]");
        k.setUserTags("[]");

        when(knowledgeMapper.findByAutoProcessStatus("RELATION_DONE", 50)).thenReturn(List.of(k));
        doThrow(new RuntimeException("DB error")).when(knowledgeService).updateAutoProcessStatus(1L, "COMPLETED");
        when(knowledgeMapper.selectList(null)).thenReturn(List.of());

        service.processRound3();

        verify(knowledgeService).updateAutoProcessStatus(1L, "COMPLETED");
        verify(logMapper, atLeastOnce()).insert(any(com.mindvault.auto.entity.AutoProcessLog.class));
    }
}