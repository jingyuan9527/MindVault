package com.mindvault.agent.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindvault.knowledge.service.KnowledgeService;
import com.mindvault.knowledge.service.SearchEnhanceService;
import com.mindvault.systemconfig.service.SystemConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchKnowledgeToolTest {

    @Mock private KnowledgeService knowledgeService;
    @Mock private SearchEnhanceService searchEnhanceService;
    @Mock private SystemConfigService config;

    private SearchKnowledgeTool tool;

    @BeforeEach
    void setUp() {
        lenient().when(config.getString(anyString(), anyString())).thenAnswer(i -> i.getArgument(1));
        lenient().when(config.getInt(anyString(), anyInt())).thenAnswer(i -> i.getArgument(1));
        tool = new SearchKnowledgeTool(knowledgeService, searchEnhanceService, config);
    }

    @Test
    void searchKnowledge_withRewriteMethod_shouldCallSearchWithRewrite() throws Exception {
        when(config.getString("tool.search-knowledge.search-method", "rewrite")).thenReturn("rewrite");
        when(searchEnhanceService.searchWithRewrite("test query", 3)).thenReturn(List.of(
                Map.of("id", 1L, "title", "result1", "summary", "summary1", "similarity", 0.95)
        ));

        String result = tool.searchKnowledge("test query", null);

        ObjectMapper mapper = new ObjectMapper();
        List<?> parsed = mapper.readValue(result, List.class);
        assertEquals(1, parsed.size());
        verify(searchEnhanceService).searchWithRewrite("test query", 3);
    }

    @Test
    void searchKnowledge_withHydeMethod_shouldCallHydeSearch() throws Exception {
        when(config.getString("tool.search-knowledge.search-method", "rewrite")).thenReturn("hyde");
        when(searchEnhanceService.hydeSearch("test query", 3)).thenReturn(List.of(
                Map.of("id", 1L, "title", "result1", "summary", "summary1", "similarity", 0.9)
        ));

        String result = tool.searchKnowledge("test query", null);

        ObjectMapper mapper = new ObjectMapper();
        List<?> parsed = mapper.readValue(result, List.class);
        assertEquals(1, parsed.size());
        verify(searchEnhanceService).hydeSearch("test query", 3);
    }

    @Test
    void searchKnowledge_withHybridMethod_shouldCallHybridSearch() throws Exception {
        when(config.getString("tool.search-knowledge.search-method", "rewrite")).thenReturn("hybrid");
        when(knowledgeService.hybridSearch("test query", 3)).thenReturn(List.of(
                Map.of("id", 1L, "title", "result1", "content", "content1", "similarity", 0.85)
        ));

        String result = tool.searchKnowledge("test query", null);

        ObjectMapper mapper = new ObjectMapper();
        List<?> parsed = mapper.readValue(result, List.class);
        assertEquals(1, parsed.size());
        verify(knowledgeService).hybridSearch("test query", 3);
    }

    @Test
    void searchKnowledge_withCustomDefaultLimit_shouldUseDefaultLimitFromConfig() throws Exception {
        when(config.getString("tool.search-knowledge.search-method", "rewrite")).thenReturn("rewrite");
        when(config.getInt("tool.search-knowledge.default-limit", 3)).thenReturn(5);
        when(searchEnhanceService.searchWithRewrite("test", 5)).thenReturn(List.of(
                Map.of("id", 1L, "title", "t", "summary", "s", "similarity", 0.5)
        ));

        String result = tool.searchKnowledge("test", null);

        ObjectMapper mapper = new ObjectMapper();
        List<?> parsed = mapper.readValue(result, List.class);
        assertEquals(1, parsed.size());
        verify(searchEnhanceService).searchWithRewrite("test", 5);
    }

    @Test
    void searchKnowledge_withCustomLimitArg_shouldUseSpecifiedLimit() throws Exception {
        when(config.getString("tool.search-knowledge.search-method", "rewrite")).thenReturn("rewrite");
        when(config.getInt("tool.search-knowledge.max-limit", 10)).thenReturn(10);
        when(searchEnhanceService.searchWithRewrite("test", 7)).thenReturn(List.of(
                Map.of("id", 1L, "title", "t", "summary", "s", "similarity", 0.5)
        ));

        tool.searchKnowledge("test", 7);

        verify(searchEnhanceService).searchWithRewrite("test", 7);
    }

    @Test
    void searchKnowledge_shouldNotExceedMaxLimit() throws Exception {
        when(config.getString("tool.search-knowledge.search-method", "rewrite")).thenReturn("rewrite");
        when(config.getInt("tool.search-knowledge.max-limit", 10)).thenReturn(5);
        when(searchEnhanceService.searchWithRewrite("test", 5)).thenReturn(List.of(
                Map.of("id", 1L, "title", "t", "summary", "s", "similarity", 0.5)
        ));

        tool.searchKnowledge("test", 10);

        verify(searchEnhanceService).searchWithRewrite("test", 5);
    }

    @Test
    void searchKnowledge_withEmptyResults_shouldReturnEmptyJsonArray() {
        when(config.getString("tool.search-knowledge.search-method", "rewrite")).thenReturn("rewrite");
        when(searchEnhanceService.searchWithRewrite("empty query", 3)).thenReturn(List.of());

        String result = tool.searchKnowledge("empty query", null);

        assertEquals("[]", result);
    }

    @Test
    void searchKnowledge_whenMethodThrows_shouldFallbackToHybrid() throws Exception {
        when(config.getString("tool.search-knowledge.search-method", "rewrite")).thenReturn("rewrite");
        when(searchEnhanceService.searchWithRewrite("query", 3)).thenThrow(new RuntimeException("API error"));
        when(knowledgeService.hybridSearch("query", 3)).thenReturn(List.of(
                Map.of("id", 1L, "title", "fallback", "content", "fallback content", "similarity", 0.5)
        ));

        String result = tool.searchKnowledge("query", null);

        ObjectMapper mapper = new ObjectMapper();
        List<?> parsed = mapper.readValue(result, List.class);
        assertEquals(1, parsed.size());
        verify(knowledgeService).hybridSearch("query", 3);
    }

    @Test
    void searchKnowledge_whenMethodAndFallbackBothFail_shouldReturnEmptyArray() {
        when(config.getString("tool.search-knowledge.search-method", "rewrite")).thenReturn("rewrite");
        when(searchEnhanceService.searchWithRewrite("query", 3)).thenThrow(new RuntimeException("API error"));
        when(knowledgeService.hybridSearch("query", 3)).thenThrow(new RuntimeException("DB error"));

        String result = tool.searchKnowledge("query", null);

        assertEquals("[]", result);
    }

    @Test
    void searchKnowledge_shouldFormatOutputFields() throws Exception {
        when(config.getString("tool.search-knowledge.search-method", "rewrite")).thenReturn("rewrite");
        when(searchEnhanceService.searchWithRewrite("query", 3)).thenReturn(List.of(
                Map.of("id", 42L, "title", "My Title", "summary", "Short summary", "similarity", 0.88)
        ));

        String result = tool.searchKnowledge("query", null);

        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> parsed = mapper.readValue(result, List.class);
        Map<String, Object> item = parsed.get(0);
        assertEquals(42, item.get("id"));
        assertEquals("My Title", item.get("title"));
        assertEquals("Short summary", item.get("summary"));
        assertEquals(0.88, (Double) item.get("score"), 0.001);
    }

    @Test
    void searchKnowledge_shouldTruncateLongContent() throws Exception {
        when(config.getString("tool.search-knowledge.search-method", "rewrite")).thenReturn("rewrite");
        String longContent = "x".repeat(500);
        when(searchEnhanceService.searchWithRewrite("query", 3)).thenReturn(List.of(
                Map.of("id", 1L, "title", "t", "content", longContent, "similarity", 0.5)
        ));

        String result = tool.searchKnowledge("query", null);

        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> parsed = mapper.readValue(result, List.class);
        String summary = (String) parsed.get(0).get("summary");
        assertTrue(summary.length() <= 300);
    }
}