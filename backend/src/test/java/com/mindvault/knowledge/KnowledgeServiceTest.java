package com.mindvault.knowledge;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindvault.content.AutoProcessService;
import com.mindvault.knowledge.dto.ImportPreview;
import com.mindvault.knowledge.entity.Knowledge;
import com.mindvault.operationlog.OperationLogService;
import com.mindvault.review.ReviewService;
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
    @Mock private AutoProcessService autoProcessService;
    @Mock private ReviewService reviewService;

    private KnowledgeService service;
    private ObjectMapper objectMapper;

    @Captor private ArgumentCaptor<Knowledge> knowledgeCaptor;
    @Captor private ArgumentCaptor<List<Long>> idsCaptor;

    @BeforeEach
    void setUp() {
        service = new KnowledgeService(mapper, operationLogService, autoProcessService, reviewService, null);
        objectMapper = new ObjectMapper();
    }

    private Knowledge createSampleKnowledge(Long id, String title, String content, String tags) {
        Knowledge k = new Knowledge();
        k.setId(id);
        k.setTitle(title);
        k.setContent(content);
        k.setContentType("TEXT");
        k.setTags(tags);
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
    void updateKnowledge_shouldUpdateFields() {
        Knowledge existing = createSampleKnowledge(1L, "Old Title", "Old Content", "[]");
        existing.setTags("[\"old\"]");
        when(mapper.selectById(1L)).thenReturn(existing);

        Knowledge updated = new Knowledge();
        updated.setTitle("New Title");
        updated.setContent("New Content");
        updated.setContentType("MARKDOWN");
        updated.setTags("[\"new\"]");

        Knowledge result = service.updateKnowledge(1L, updated);

        assertEquals("New Title", result.getTitle());
        assertEquals("New Content", result.getContent());
        assertEquals("MARKDOWN", result.getContentType());
        assertEquals("[\"new\"]", result.getTags());
        verify(mapper).updateById(existing);
        verify(operationLogService).log(eq("KNOWLEDGE"), eq("UPDATE"), eq(1L), anyString());
    }

    @Test
    void deleteKnowledge_shouldDeleteAndLog() {
        Knowledge existing = createSampleKnowledge(1L, "Title", "Content", "[]");
        when(mapper.selectById(1L)).thenReturn(existing);

        service.deleteKnowledge(1L);

        verify(mapper).deleteById(1L);
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
        verify(operationLogService, times(2)).log(eq("KNOWLEDGE"), eq("DELETE"), any(), anyString());
    }

    @Test
    void batchTag_shouldAddTagToAll() {
        Knowledge k1 = createSampleKnowledge(1L, "A", "a", "[]");
        k1.setTags("[\"existing\"]");
        Knowledge k2 = createSampleKnowledge(2L, "B", "b", "[]");
        when(mapper.selectById(1L)).thenReturn(k1);
        when(mapper.selectById(2L)).thenReturn(k2);

        service.batchTag(List.of(1L, 2L), "new-tag");

        verify(mapper, times(2)).updateById(any(Knowledge.class));
        assertTrue(k1.getTags().contains("new-tag"));
        assertTrue(k1.getTags().contains("existing"));
        assertTrue(k2.getTags().contains("new-tag"));
    }

    @Test
    void batchTag_shouldSkipDuplicateTags() {
        Knowledge k1 = createSampleKnowledge(1L, "A", "a", "[\"tag1\"]");
        when(mapper.selectById(1L)).thenReturn(k1);

        service.batchTag(List.of(1L), "tag1");

        verify(mapper, never()).updateById(any(Knowledge.class));
    }

    @Test
    void batchExport_shouldProduceJson() throws Exception {
        Knowledge k1 = createSampleKnowledge(1L, "Title1", "Content1", "[\"t1\"]");
        Knowledge k2 = createSampleKnowledge(2L, "Title2", "Content2", "[]");
        when(mapper.selectBatchIds(List.of(1L, 2L))).thenReturn(List.of(k1, k2));

        String json = service.batchExport(List.of(1L, 2L));

        Map<String, Object> data = objectMapper.readValue(json, Map.class);
        assertEquals(2, data.get("count"));
        assertEquals("0.2.0", data.get("version"));
    }

    @Test
    void batchExport_shouldHandleEmptyList() {
        when(mapper.selectBatchIds(List.of())).thenReturn(List.of());

        String json = service.batchExport(List.of());

        assertTrue(json.contains("\"count\" : 0") || json.contains("\"count\": 0"));
    }

    @Test
    void updateTags_shouldUpdate() {
        Knowledge existing = createSampleKnowledge(1L, "Title", "Content", "[]");
        when(mapper.selectById(1L)).thenReturn(existing);

        service.updateTags(1L, List.of("tag1", "tag2"));

        verify(mapper).updateById(knowledgeCaptor.capture());
        String tags = knowledgeCaptor.getValue().getTags();
        assertTrue(tags.contains("tag1"));
        assertTrue(tags.contains("tag2"));
        verify(operationLogService).log(eq("KNOWLEDGE"), eq("TAG"), eq(1L), anyString());
    }

    @Test
    void exportAllAsJson_shouldProduceJson() throws Exception {
        Knowledge k1 = createSampleKnowledge(1L, "Title1", "Content1", "[]");
        Knowledge k2 = createSampleKnowledge(2L, "Title2", "Content2", "[]");
        when(mapper.selectList(null)).thenReturn(List.of(k1, k2));

        String json = service.exportAllAsJson();

        Map<String, Object> data = objectMapper.readValue(json, Map.class);
        assertEquals(2, data.get("count"));
        List<Map<String, Object>> items = (List<Map<String, Object>>) data.get("items");
        assertEquals("Title1", items.get(0).get("title"));
        assertNotNull(data.get("exportedAt"));
    }

    @Test
    void exportAllAsCsv_shouldProduceValidCsv() {
        Knowledge k1 = createSampleKnowledge(1L, "Title, with comma", "Content \"quoted\"", "[\"tag1\"]");
        Knowledge k2 = createSampleKnowledge(2L, "Normal Title", "Normal content", "[]");
        when(mapper.selectList(null)).thenReturn(List.of(k1, k2));

        String csv = service.exportAllAsCsv();

        assertTrue(csv.startsWith("标题,内容,类型,摘要,标签,来源,创建时间\n"));
        assertTrue(csv.contains("Title, with comma"));
        assertTrue(csv.contains("Content \"\"quoted\"\""));
        assertTrue(csv.contains("tag1"));
        assertTrue(csv.contains("Normal Title"));
    }

    @Test
    void csv_shouldEscapeSpecialCharacters() {
        Knowledge k = createSampleKnowledge(1L, "Test", "Line1\nLine2", "[]");
        when(mapper.selectList(null)).thenReturn(List.of(k));

        String csv = service.exportAllAsCsv();

        assertTrue(csv.contains("\"Line1\nLine2\""));
    }

    @Test
    void previewImport_shouldDetectConflicts() throws Exception {
        String json = """
                {
                    "version": "0.2.0",
                    "items": [
                        {"title": "Existing Article", "content": "hello"},
                        {"title": "New Article", "content": "world"}
                    ]
                }
                """;

        Knowledge existing = createSampleKnowledge(1L, "Existing Article", "existing", "[]");
        when(mapper.findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(eq("Existing Article"), eq("")))
                .thenReturn(List.of(existing));
        when(mapper.findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(eq("New Article"), eq("")))
                .thenReturn(List.of());

        ImportPreview preview = service.previewImport(json);

        assertEquals(2, preview.totalCount());
        assertEquals(1, preview.newCount());
        assertEquals(1, preview.conflictCount());
        assertEquals(1, preview.conflicts().size());
        assertEquals("Existing Article", preview.conflicts().get(0).title());
    }

    @Test
    void previewImport_shouldHandleInvalidJson() {
        ImportPreview preview = service.previewImport("not json");

        assertEquals(0, preview.totalCount());
        assertEquals(0, preview.newCount());
        assertEquals(0, preview.conflictCount());
    }

    @Test
    void importFromJsonWithConflict_skipMode_shouldSkipConflicts() throws Exception {
        String json = """
                {
                    "items": [
                        {"title": "Existing", "content": "new content"},
                        {"title": "New", "content": "fresh"}
                    ]
                }
                """;

        Knowledge existing = createSampleKnowledge(1L, "Existing", "old content", "[]");
        when(mapper.findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(eq("Existing"), eq("")))
                .thenReturn(List.of(existing));
        when(mapper.findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(eq("New"), eq("")))
                .thenReturn(List.of());

        int count = service.importFromJsonWithConflict(json, "skip");

        assertEquals(1, count);
        verify(mapper, times(1)).insert(any(Knowledge.class));
    }

    @Test
    void importFromJsonWithConflict_overwriteMode_shouldUpdateConflicts() throws Exception {
        String json = """
                {
                    "items": [
                        {"title": "Existing", "content": "updated content", "contentType": "TEXT"},
                        {"title": "New", "content": "fresh", "contentType": "TEXT"}
                    ]
                }
                """;

        Knowledge existing = createSampleKnowledge(1L, "Existing", "old content", "[]");
        when(mapper.findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(eq("Existing"), eq("")))
                .thenReturn(List.of(existing));
        when(mapper.findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(eq("New"), eq("")))
                .thenReturn(List.of());

        int count = service.importFromJsonWithConflict(json, "overwrite");

        assertEquals(2, count);
        verify(mapper).updateById(knowledgeCaptor.capture());
        assertEquals("updated content", knowledgeCaptor.getValue().getContent());
    }

    @Test
    void importFromJsonWithConflict_shouldThrowOnError() {
        assertThrows(RuntimeException.class,
                () -> service.importFromJsonWithConflict("invalid json", "skip"));
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
    void getAllTags_shouldAggregate() {
        when(mapper.aggregateTags()).thenReturn(List.of(
                Map.of("name", "java", "count", 5L),
                Map.of("name", "spring", "count", 3L)
        ));

        List<Map<String, Object>> tags = service.getAllTags();

        assertEquals(2, tags.size());
        assertEquals("java", tags.get(0).get("name"));
        assertEquals(5L, tags.get(0).get("count"));
    }

    @Test
    void searchByKeyword_shouldDelegate() {
        service.searchByKeyword("test", 5);
        verify(mapper).keywordSearch("test", 5);
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
                new Object[]{1L, 0.95d},
                new Object[]{2L, 0.85d}
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