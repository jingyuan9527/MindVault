package com.mindvault.knowledge;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindvault.auto.service.AutoProcessOrchestrator;
import com.mindvault.knowledge.entity.Knowledge;
import com.mindvault.knowledge.mapper.KnowledgeMapper;
import com.mindvault.knowledge.service.KnowledgeService;
import com.mindvault.knowledge.service.KnowledgeServiceImpl;
import com.mindvault.knowledge.service.strategy.SearchStrategy;
import com.mindvault.operationlog.service.OperationLogService;
import com.mindvault.auto.mapper.KnowledgeRelationMapper;
import com.mindvault.review.service.ReviewService;
import com.mindvault.systemconfig.service.SystemConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KnowledgeServiceTest {

    @Mock private KnowledgeMapper mapper;
    @Mock private OperationLogService operationLogService;
    @Mock private AutoProcessOrchestrator autoProcessOrchestrator;
    @Mock private ReviewService reviewService;
    @Mock private KnowledgeRelationMapper relationMapper;
    @Mock private SystemConfigService config;

    private KnowledgeService service;
    private ObjectMapper objectMapper;

    @Captor private ArgumentCaptor<Knowledge> knowledgeCaptor;
    @Captor private ArgumentCaptor<List<Long>> idsCaptor;

    @BeforeEach
    void setUp() {
        service = new KnowledgeServiceImpl(mapper, operationLogService, autoProcessOrchestrator, reviewService, relationMapper, config, List.of());
        objectMapper = new ObjectMapper();
        lenient().when(config.getInt(anyString(), anyInt())).thenAnswer(i -> i.getArgument(1));
        lenient().when(config.getLong(anyString(), anyLong())).thenAnswer(i -> i.getArgument(1));
        lenient().when(config.getDouble(anyString(), anyDouble())).thenAnswer(i -> i.getArgument(1));
        lenient().when(config.getString(anyString(), anyString())).thenAnswer(i -> i.getArgument(1));
        lenient().when(config.getBool(anyString(), anyBoolean())).thenAnswer(i -> i.getArgument(1));
        lenient().when(config.getPrompt(anyString(), anyString())).thenAnswer(i -> i.getArgument(1));
    }

    private Knowledge createSampleKnowledge(Long id, String title, String content, String tags) {
        Knowledge k = new Knowledge();
        k.setId(id);
        k.setTitle(title);
        k.setContent(content);
        k.setContentType("TEXT");
        k.setTags(tags);
        k.setUserTags("[]");
        k.setAutoProcessStatus("PENDING");
        k.setCreatedAt(LocalDateTime.now());
        k.setUpdatedAt(LocalDateTime.now());
        return k;
    }

    @Test
    void addKnowledge_shouldInsertAndLog() {
        when(mapper.insert(any(Knowledge.class))).thenReturn(1);

        Knowledge input = createSampleKnowledge(null, "Test Title", "Test Content", "[]");
        Knowledge result = service.addKnowledge(input);

        verify(mapper).insert(knowledgeCaptor.capture());
        assertEquals("Test Title", knowledgeCaptor.getValue().getTitle());
        assertEquals("PENDING", knowledgeCaptor.getValue().getAutoProcessStatus());
        assertNotNull(knowledgeCaptor.getValue().getCreatedAt());
        verify(operationLogService).log(eq("KNOWLEDGE"), eq("ADD"), any(), contains("Test Title"));
        verify(reviewService).scheduleReview(any());
    }

    @Test
    void addKnowledge_shouldHandleReviewFailureGracefully() {
        when(mapper.insert(any(Knowledge.class))).thenReturn(1);
        doThrow(new RuntimeException("DB error")).when(reviewService).scheduleReview(any());

        Knowledge input = createSampleKnowledge(null, "Test", "Content", "[]");
        Knowledge result = service.addKnowledge(input);

        assertNotNull(result);
        verify(operationLogService).log(eq("KNOWLEDGE"), eq("ADD"), any(), anyString());
    }

    @Test
    void getById_shouldReturnKnowledge() {
        Knowledge expected = createSampleKnowledge(1L, "Title", "Content", "[]");
        when(mapper.selectById(1L)).thenReturn(expected);

        Knowledge result = service.getById(1L);

        assertEquals("Title", result.getTitle());
        verify(mapper).selectById(1L);
    }

    @Test
    void getById_shouldThrowWhenNotFound() {
        when(mapper.selectById(99L)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> service.getById(99L));
    }

    @Test
    void updateKnowledge_shouldOnlyUpdateUserFields() {
        Knowledge existing = createSampleKnowledge(1L, "Old Title", "Old Content", "[\"old\"]");
        existing.setAiTitle("AI Title");
        when(mapper.selectById(1L)).thenReturn(existing);

        Knowledge updated = new Knowledge();
        updated.setTitle("New Title");
        updated.setContent("New Content");
        updated.setContentType("MARKDOWN");
        updated.setUserTags("[\"new\"]");

        Knowledge result = service.updateKnowledge(1L, updated);

        assertEquals("New Title", result.getTitle());
        assertEquals("New Content", result.getContent());
        assertEquals("MARKDOWN", result.getContentType());
        assertEquals("[\"new\"]", result.getUserTags());
        assertEquals("AI Title", result.getAiTitle());
        verify(mapper).updateById(existing);
        verify(operationLogService).log(eq("KNOWLEDGE"), eq("UPDATE"), eq(1L), anyString());
    }

    @Test
    void updateAiFields_shouldOnlyUpdateAiFields() {
        Knowledge existing = createSampleKnowledge(1L, "User Title", "Content", "[]");
        when(mapper.selectById(1L)).thenReturn(existing);

        Knowledge result = service.updateAiFields(1L, "AI Generated Title", "[\"ai-tag\"]");

        assertEquals("AI Generated Title", result.getAiTitle());
        assertEquals("[\"ai-tag\"]", result.getTags());
        assertEquals("User Title", result.getTitle());
        verify(mapper).updateById(existing);
    }

    @Test
    void deleteKnowledge_shouldDeleteAndRemoveRelations() {
        Knowledge existing = createSampleKnowledge(1L, "Title", "Content", "[]");
        when(mapper.selectById(1L)).thenReturn(existing);

        service.deleteKnowledge(1L);

        verify(mapper).deleteById(1L);
        verify(relationMapper).deleteByKnowledgeId(1L);
        verify(operationLogService).log(eq("KNOWLEDGE"), eq("DELETE"), eq(1L), anyString());
    }

    @Test
    void batchDelete_shouldDeleteAll() {
        Knowledge k1 = createSampleKnowledge(1L, "A", "a", "[]");
        Knowledge k2 = createSampleKnowledge(2L, "B", "b", "[]");
        when(mapper.selectById(1L)).thenReturn(k1);
        when(mapper.selectById(2L)).thenReturn(k2);

        service.batchDelete(List.of(1L, 2L));

        verify(mapper).deleteById(1L);
        verify(mapper).deleteById(2L);
        verify(relationMapper, times(2)).deleteByKnowledgeId(any());
        verify(operationLogService, times(2)).log(eq("KNOWLEDGE"), eq("DELETE"), any(), anyString());
    }

    @Test
    void displayTitle_shouldUseAiTitleWhenAvailable() {
        Knowledge k = createSampleKnowledge(1L, "User Title", "Content", "[]");
        k.setAiTitle("AI Title");
        assertEquals("AI Title", service.displayTitle(k));
    }

    @Test
    void displayTitle_shouldFallbackToUserTitle() {
        Knowledge k = createSampleKnowledge(1L, "User Title", "Content", "[]");
        assertEquals("User Title", service.displayTitle(k));
    }

    @Test
    void keywordSearchWithRank_shouldConvertResults() {
        Knowledge k = createSampleKnowledge(1L, "Result", "content", "[\"tag1\"]");
        when(mapper.keywordSearch("test", 10)).thenReturn(List.of(k));

        List<Map<String, Object>> results = service.keywordSearchWithRank("test", 10);

        assertEquals(1, results.size());
        assertEquals("Result", results.get(0).get("title"));
        assertEquals("content", results.get(0).get("content"));
        assertEquals("[\"tag1\"]", results.get(0).get("tags"));
    }

    @Test
    void updateEmbedding_shouldUpdateWhenExists() {
        Knowledge k = createSampleKnowledge(1L, "T", "C", "[]");
        when(mapper.selectById(1L)).thenReturn(k);

        service.updateEmbedding(1L, "[0.1,0.2,0.3]");

        assertEquals("[0.1,0.2,0.3]", k.getEmbedding());
        verify(mapper).updateById(k);
    }

    @Test
    void updateEmbedding_shouldSkipWhenNotExists() {
        when(mapper.selectById(99L)).thenReturn(null);

        service.updateEmbedding(99L, "[0.1,0.2]");

        verify(mapper, never()).updateById(any(Knowledge.class));
    }

    @Test
    void searchSimilar_shouldMapResults() {
        when(mapper.findSimilarIds("[0.1,0.2]", 5)).thenReturn(List.of(
                Map.of("id", 1L, "similarity", 0.95d),
                Map.of("id", 2L, "similarity", 0.85d)
        ));
        Knowledge k1 = createSampleKnowledge(1L, "Match1", "c1", "[]");
        Knowledge k2 = createSampleKnowledge(2L, "Match2", "c2", "[]");
        when(mapper.selectBatchIds(Set.of(1L, 2L))).thenReturn(List.of(k1, k2));

        List<Map<String, Object>> results = service.searchSimilar("[0.1,0.2]", 5);

        assertEquals(2, results.size());
        assertEquals(0.95d, (double) results.get(0).get("similarity"), 0.001);
        assertEquals("Match1", results.get(0).get("title"));
    }
}
