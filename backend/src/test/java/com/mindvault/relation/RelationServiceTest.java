package com.mindvault.relation;

import com.mindvault.ai.client.AiService;
import com.mindvault.auto.AutoProcessLogMapper;
import com.mindvault.knowledge.KnowledgeMapper;
import com.mindvault.knowledge.KnowledgeService;
import com.mindvault.knowledge.entity.Knowledge;
import com.mindvault.model.ModelConfigService;
import com.mindvault.relation.entity.KnowledgeRelation;
import com.mindvault.systemconfig.SystemConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RelationServiceTest {

    @Mock private KnowledgeMapper knowledgeMapper;
    @Mock private KnowledgeRelationMapper relationMapper;
    @Mock private KnowledgeService knowledgeService;
    @Mock private ModelConfigService modelConfigService;
    @Mock private AiService aiService;
    @Mock private AutoProcessLogMapper logMapper;
    @Mock private SystemConfigService config;

    private RelationService service;

    @BeforeEach
    void setUp() {
        service = new RelationService(knowledgeMapper, relationMapper, knowledgeService,
                modelConfigService, aiService, logMapper, config);
        lenient().when(config.getInt(anyString(), anyInt())).thenAnswer(i -> i.getArgument(1));
        lenient().when(config.getLong(anyString(), anyLong())).thenAnswer(i -> i.getArgument(1));
        lenient().when(config.getDouble(anyString(), anyDouble())).thenAnswer(i -> i.getArgument(1));
        lenient().when(config.getString(anyString(), anyString())).thenAnswer(i -> i.getArgument(1));
        lenient().when(config.getBool(anyString(), anyBoolean())).thenAnswer(i -> i.getArgument(1));
        lenient().when(config.getPrompt(anyString(), anyString())).thenAnswer(i -> i.getArgument(1));
    }

    @Test
    void processRound2_noPending_shouldSkip() {
        when(knowledgeMapper.findByAutoProcessStatus("TITLE_TAG_DONE", 20)).thenReturn(Collections.emptyList());

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

        when(knowledgeMapper.findByAutoProcessStatus("TITLE_TAG_DONE", 20)).thenReturn(Collections.singletonList(k));
        when(knowledgeMapper.findByAutoProcessStatus("COMPLETED", 50)).thenReturn(Collections.emptyList());
        doNothing().when(knowledgeService).updateAutoProcessStatus(1L, "RELATION_DONE");

        service.processRound2();

        verify(knowledgeService).updateAutoProcessStatus(1L, "RELATION_DONE");
    }

    @Test
    void processKnowledgeRelations_vectorSimilarity_shouldSaveRelations() {
        Knowledge k = new Knowledge();
        k.setId(1L);
        k.setTitle("Test");
        k.setContent("Content");
        k.setTags("[\"ai\"]");
        k.setUserTags("[]");
        k.setEmbedding("[0.1,0.2,0.3]");

        Knowledge candidate = new Knowledge();
        candidate.setId(2L);
        candidate.setTitle("Candidate");
        candidate.setTags("[]");
        candidate.setUserTags("[]");

        List<Map<String, Object>> similar = new ArrayList<>();
        similar.add(Map.of("id", 2L, "similarity", 0.85d));

        when(knowledgeMapper.findByAutoProcessStatus("COMPLETED", 50)).thenReturn(Collections.singletonList(candidate));
        when(knowledgeMapper.findSimilarIds("[0.1,0.2,0.3]", 10)).thenReturn(similar);

        service.processKnowledgeRelations(k);

        ArgumentCaptor<KnowledgeRelation> captor = ArgumentCaptor.forClass(KnowledgeRelation.class);
        verify(relationMapper).insert(captor.capture());
        KnowledgeRelation saved = captor.getValue();
        assertEquals(1L, saved.getKnowledgeId());
        assertEquals(2L, saved.getRelatedId());
        assertEquals("COMPLEMENT", saved.getRelationType());
        assertEquals("VECTOR", saved.getSource());
    }

    @Test
    void processKnowledgeRelations_vectorSimilarity_belowThreshold_shouldSkip() {
        Knowledge k = new Knowledge();
        k.setId(1L);
        k.setTitle("Test");
        k.setContent("Content");
        k.setTags("[]");
        k.setUserTags("[]");
        k.setEmbedding("[0.1,0.2,0.3]");

        Knowledge candidate = new Knowledge();
        candidate.setId(2L);
        candidate.setTags("[]");
        candidate.setUserTags("[]");

        List<Map<String, Object>> similar = new ArrayList<>();
        similar.add(Map.of("id", 2L, "similarity", 0.3d));

        when(knowledgeMapper.findByAutoProcessStatus("COMPLETED", 50)).thenReturn(Collections.singletonList(candidate));
        when(knowledgeMapper.findSimilarIds("[0.1,0.2,0.3]", 10)).thenReturn(similar);

        service.processKnowledgeRelations(k);

        verify(relationMapper, never()).insert(any(KnowledgeRelation.class));
    }

    @Test
    void processKnowledgeRelations_tagOverlap_shouldSaveRelations() {
        Knowledge k = new Knowledge();
        k.setId(1L);
        k.setTitle("Test");
        k.setContent("Content");
        k.setTags("[\"java\", \"spring\"]");
        k.setUserTags("[]");
        k.setEmbedding("");

        Knowledge candidate = new Knowledge();
        candidate.setId(2L);
        candidate.setTitle("Candidate");
        candidate.setTags("[\"java\"]");
        candidate.setUserTags("[]");

        when(knowledgeMapper.findByAutoProcessStatus("COMPLETED", 50)).thenReturn(Collections.singletonList(candidate));

        service.processKnowledgeRelations(k);

        ArgumentCaptor<KnowledgeRelation> captor = ArgumentCaptor.forClass(KnowledgeRelation.class);
        verify(relationMapper).insert(captor.capture());
        KnowledgeRelation saved = captor.getValue();
        assertEquals(1L, saved.getKnowledgeId());
        assertEquals(2L, saved.getRelatedId());
        assertEquals("REFERENCE", saved.getRelationType());
        assertEquals("TAG", saved.getSource());
    }

    @Test
    void processKnowledgeRelations_llmAnalysis_shouldSaveRelations() {
        Knowledge k = new Knowledge();
        k.setId(1L);
        k.setTitle("Test Note");
        k.setContent("Content about AI");
        k.setTags("[]");
        k.setUserTags("[]");
        k.setEmbedding("");

        Knowledge candidate = new Knowledge();
        candidate.setId(2L);
        candidate.setTitle("Candidate");
        candidate.setContent("Related content");

        when(knowledgeMapper.findByAutoProcessStatus("COMPLETED", 50)).thenReturn(Collections.singletonList(candidate));
        when(modelConfigService.getPrimaryChatModel()).thenReturn(new com.mindvault.model.entity.ModelConfig());
        when(knowledgeService.displayTitle(candidate)).thenReturn("Candidate");
        when(aiService.call(anyString(), anyDouble(), anyInt()))
                .thenReturn("[{\"id\": 2, \"type\": \"EXTENSION\", \"reason\": \"related\"}]");

        service.processKnowledgeRelations(k);

        ArgumentCaptor<KnowledgeRelation> captor = ArgumentCaptor.forClass(KnowledgeRelation.class);
        verify(relationMapper).insert(captor.capture());
        KnowledgeRelation saved = captor.getValue();
        assertEquals(1L, saved.getKnowledgeId());
        assertEquals(2L, saved.getRelatedId());
        assertEquals("EXTENSION", saved.getRelationType());
        assertEquals("LLM", saved.getSource());
    }

    @Test
    void processKnowledgeRelations_llmFailure_shouldSkipGracefully() {
        Knowledge k = new Knowledge();
        k.setId(1L);
        k.setTitle("Test");
        k.setContent("Content");
        k.setTags("[]");
        k.setUserTags("[]");
        k.setEmbedding("");

        Knowledge candidate = new Knowledge();
        candidate.setId(2L);
        candidate.setTitle("Candidate");
        candidate.setContent("Content");

        when(knowledgeMapper.findByAutoProcessStatus("COMPLETED", 50)).thenReturn(Collections.singletonList(candidate));
        when(modelConfigService.getPrimaryChatModel()).thenReturn(new com.mindvault.model.entity.ModelConfig());
        when(knowledgeService.displayTitle(candidate)).thenReturn("Candidate");
        when(aiService.call(anyString(), anyDouble(), anyInt())).thenReturn(null);

        service.processKnowledgeRelations(k);

        verify(relationMapper, never()).insert(any(KnowledgeRelation.class));
        verify(logMapper, atLeastOnce()).insert(any(com.mindvault.auto.entity.AutoProcessLog.class));
    }

    @Test
    void processKnowledgeRelations_noEmbedding_shouldSkipVectorPath() {
        Knowledge k = new Knowledge();
        k.setId(1L);
        k.setTitle("Test");
        k.setContent("Content");
        k.setTags("[]");
        k.setUserTags("[]");
        k.setEmbedding("");

        when(knowledgeMapper.findByAutoProcessStatus("COMPLETED", 50)).thenReturn(Collections.emptyList());

        service.processKnowledgeRelations(k);

        verify(knowledgeMapper, never()).findSimilarIds(anyString(), anyInt());
    }

    @Test
    void processKnowledgeRelations_noCandidates_shouldReturnEarly() {
        Knowledge k = new Knowledge();
        k.setId(1L);
        k.setTitle("Test");
        k.setContent("Content");
        k.setTags("[]");
        k.setUserTags("[]");

        when(knowledgeMapper.findByAutoProcessStatus("COMPLETED", 50)).thenReturn(Collections.emptyList());

        service.processKnowledgeRelations(k);

        verifyNoInteractions(relationMapper);
    }
}