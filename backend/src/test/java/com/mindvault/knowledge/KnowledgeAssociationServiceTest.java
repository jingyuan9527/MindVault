package com.mindvault.knowledge;

import com.mindvault.knowledge.entity.Knowledge;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class KnowledgeAssociationServiceTest {

    @Mock
    private KnowledgeMapper mapper;

    @Mock
    private KnowledgeService knowledgeService;

    private KnowledgeAssociationService service;

    @BeforeEach
    void setUp() {
        service = new KnowledgeAssociationService(mapper, knowledgeService);
    }

    private Knowledge createKnowledge(Long id, String title, String embedding) {
        Knowledge k = new Knowledge();
        k.setId(id);
        k.setTitle(title);
        k.setContent("Content of " + title);
        k.setContentType("TEXT");
        k.setTags("[\"tag1\"]");
        k.setSummary("Summary of " + title);
        k.setEmbedding(embedding);
        k.setCreatedAt(LocalDateTime.now());
        k.setUpdatedAt(LocalDateTime.now());
        return k;
    }

    @Test
    void getRelatedKnowledge_shouldReturnEmptyWhenNoEmbedding() {
        Knowledge source = createKnowledge(1L, "Source", null);
        when(knowledgeService.getById(1L)).thenReturn(source);

        List<Map<String, Object>> result = service.getRelatedKnowledge(1L, 5);

        assertTrue(result.isEmpty());
        verify(mapper, never()).findSimilarIds(anyString(), anyInt());
    }

    @Test
    void getRelatedKnowledge_shouldReturnEmptyWhenEmbeddingBlank() {
        Knowledge source = createKnowledge(1L, "Source", "");
        when(knowledgeService.getById(1L)).thenReturn(source);

        List<Map<String, Object>> result = service.getRelatedKnowledge(1L, 5);

        assertTrue(result.isEmpty());
        verify(mapper, never()).findSimilarIds(anyString(), anyInt());
    }

    @Test
    void getRelatedKnowledge_shouldReturnRelatedItemsSortedBySimilarity() {
        String embedding = "[0.1,0.2,0.3]";
        Knowledge source = createKnowledge(1L, "Source", embedding);
        when(knowledgeService.getById(1L)).thenReturn(source);

        Object[] row1 = new Object[]{2L, 0.95d};
        Object[] row2 = new Object[]{3L, 0.85d};
        Object[] row3 = new Object[]{1L, 1.0d}; // source itself, should be excluded
        when(mapper.findSimilarIds(embedding, 6)).thenReturn(List.<Object[]>of(row1, row2, row3));

        Knowledge related1 = createKnowledge(2L, "Related A", null);
        Knowledge related2 = createKnowledge(3L, "Related B", null);
        when(mapper.selectBatchIds(Set.of(2L, 3L))).thenReturn(List.of(related1, related2));

        List<Map<String, Object>> result = service.getRelatedKnowledge(1L, 5);

        assertEquals(2, result.size());
        assertEquals(2L, result.get(0).get("id"));
        assertEquals("Related A", result.get(0).get("title"));
        assertEquals("Summary of Related A", result.get(0).get("summary"));
        assertEquals(0.95d, (double) result.get(0).get("similarity"), 0.001);
        assertEquals("[\"tag1\"]", result.get(0).get("tags"));

        assertEquals(3L, result.get(1).get("id"));
        assertEquals(0.85d, (double) result.get(1).get("similarity"), 0.001);
    }

    @Test
    void getRelatedKnowledge_shouldExcludeSourceKnowledge() {
        String embedding = "[0.1,0.2]";
        Knowledge source = createKnowledge(1L, "Source", embedding);
        when(knowledgeService.getById(1L)).thenReturn(source);

        Object[] row = new Object[]{1L, 1.0d};
        when(mapper.findSimilarIds(embedding, 6)).thenReturn(List.<Object[]>of(row));

        List<Map<String, Object>> result = service.getRelatedKnowledge(1L, 5);

        assertTrue(result.isEmpty());
    }

    @Test
    void getRelatedKnowledge_shouldLimitResults() {
        String embedding = "[0.1,0.2]";
        Knowledge source = createKnowledge(1L, "Source", embedding);
        when(knowledgeService.getById(1L)).thenReturn(source);

        Object[] row1 = new Object[]{2L, 0.95d};
        Object[] row2 = new Object[]{3L, 0.85d};
        Object[] row3 = new Object[]{4L, 0.75d};
        when(mapper.findSimilarIds(embedding, 3)).thenReturn(List.<Object[]>of(row1, row2, row3));

        Knowledge related1 = createKnowledge(2L, "A", null);
        Knowledge related2 = createKnowledge(3L, "B", null);
        Knowledge related3 = createKnowledge(4L, "C", null);
        when(mapper.selectBatchIds(Set.of(2L, 3L, 4L))).thenReturn(List.of(related1, related2, related3));

        List<Map<String, Object>> result = service.getRelatedKnowledge(1L, 2);

        assertEquals(2, result.size());
        assertEquals(2L, result.get(0).get("id"));
        assertEquals(3L, result.get(1).get("id"));
    }

    @Test
    void getRelatedKnowledge_shouldSkipNullKnowledgeInBatchLookup() {
        String embedding = "[0.1,0.2]";
        Knowledge source = createKnowledge(1L, "Source", embedding);
        when(knowledgeService.getById(1L)).thenReturn(source);

        // request limit+1 = 3 from mapper
        Object[] row1 = new Object[]{2L, 0.95d};
        Object[] row2 = new Object[]{3L, 0.85d};
        when(mapper.findSimilarIds(embedding, 3)).thenReturn(List.<Object[]>of(row1, row2));

        // only 2L is returned by selectBatchIds (3L may have been deleted)
        Knowledge related1 = createKnowledge(2L, "Existing", null);
        when(mapper.selectBatchIds(Set.of(2L, 3L))).thenReturn(List.of(related1));

        List<Map<String, Object>> result = service.getRelatedKnowledge(1L, 2);

        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).get("id"));
    }
}