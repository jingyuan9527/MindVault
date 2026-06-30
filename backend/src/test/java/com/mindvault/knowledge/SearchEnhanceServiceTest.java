package com.mindvault.knowledge;

import com.mindvault.ai.client.AiModelFactory;
import com.mindvault.knowledge.service.KnowledgeService;
import com.mindvault.knowledge.service.SearchEnhanceService;
import com.mindvault.knowledge.service.SearchEnhanceServiceImpl;
import com.mindvault.ai.client.AiService;
import com.mindvault.model.service.ModelConfigService;
import com.mindvault.knowledge.config.SearchProperties;
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
class SearchEnhanceServiceTest {

    @Mock private KnowledgeService knowledgeService;
    @Mock private ModelConfigService modelConfigService;
    @Mock private AiService aiService;
    @Mock private AiModelFactory aiModelFactory;
    @Mock private SystemConfigService config;
    @Mock private SearchProperties searchProperties;

    private SearchEnhanceService service;

    @BeforeEach
    void setUp() {
        service = new SearchEnhanceServiceImpl(knowledgeService, modelConfigService, aiService, aiModelFactory, config, searchProperties);
        lenient().when(config.getInt(anyString(), anyInt())).thenAnswer(i -> i.getArgument(1));
        lenient().when(config.getDouble(anyString(), anyDouble())).thenAnswer(i -> i.getArgument(1));
        lenient().when(config.getString(anyString(), anyString())).thenAnswer(i -> i.getArgument(1));
        lenient().when(config.getPrompt(anyString(), anyString())).thenAnswer(i -> i.getArgument(1));
    }

    @Test
    void searchWithRewrite_shouldFallbackToHybridWhenNoModels() {
        when(modelConfigService.getPrimaryChatModel()).thenThrow(new RuntimeException("no model"));

        List<Map<String, Object>> expected = List.of(Map.of("title", "fallback"));
        when(knowledgeService.hybridSearch("test", 5)).thenReturn(expected);

        List<Map<String, Object>> result = service.searchWithRewrite("test", 5);

        assertEquals(expected, result);
    }

    @Test
    void searchWithRewrite_shouldFallbackToHybridWhenRewriteFails() {
        when(modelConfigService.getPrimaryChatModel()).thenReturn(new com.mindvault.model.entity.ModelConfig());
        when(aiService.call(anyString(), anyDouble(), anyInt())).thenReturn(null);

        List<Map<String, Object>> expected = List.of(Map.of("title", "fallback"));
        when(knowledgeService.hybridSearch("test", 5)).thenReturn(expected);

        List<Map<String, Object>> result = service.searchWithRewrite("test", 5);

        assertEquals(expected, result);
    }

    @Test
    void hydeSearch_shouldFallbackToHybridWhenNoChatModel() {
        when(modelConfigService.getPrimaryChatModel()).thenThrow(new RuntimeException("no model"));

        List<Map<String, Object>> expected = List.of(Map.of("title", "fallback"));
        when(knowledgeService.hybridSearch("test", 5)).thenReturn(expected);

        List<Map<String, Object>> result = service.hydeSearch("test", 5);

        assertEquals(expected, result);
    }

    @Test
    void hydeSearch_shouldFallbackToHybridWhenNoEmbeddingModel() {
        when(modelConfigService.getPrimaryChatModel()).thenReturn(new com.mindvault.model.entity.ModelConfig());
        when(aiService.call(anyString(), anyDouble(), anyInt())).thenReturn("hypothetical doc");
        when(modelConfigService.getAvailableEmbeddingModels()).thenReturn(List.of());

        List<Map<String, Object>> expected = List.of(Map.of("title", "fallback"));
        when(knowledgeService.hybridSearch("test", 5)).thenReturn(expected);

        List<Map<String, Object>> result = service.hydeSearch("test", 5);

        assertEquals(expected, result);
    }

    @Test
    void searchWithRewrite_withOffset_shouldFallbackAndSkipSecondTier() {
        // rewrite 不可用 → fallback 走 hybridSearch(query, topN+offset=4)，再 skip offset=2 → 返回 [C,D]
        when(modelConfigService.getPrimaryChatModel()).thenThrow(new RuntimeException("no model"));
        when(knowledgeService.hybridSearch("test", 4)).thenReturn(List.of(
                Map.of("id", 1L, "title", "A"),
                Map.of("id", 2L, "title", "B"),
                Map.of("id", 3L, "title", "C"),
                Map.of("id", 4L, "title", "D")
        ));

        List<Map<String, Object>> result = service.searchWithRewrite("test", 2, 2);

        assertEquals(2, result.size());
        assertEquals("C", result.get(0).get("title"));
        assertEquals("D", result.get(1).get("title"));
        // 第二 tier 与第一 tier (offset=0 → [A,B]) 无重叠
        verify(knowledgeService).hybridSearch("test", 4);
    }

    @Test
    void rerankResults_shouldReturnOriginalWhenNull() {
        List<Map<String, Object>> result = service.rerankResults("query", null, 5);
        assertNull(result);
    }

    @Test
    void rerankResults_shouldReturnOriginalWhenSingle() {
        List<Map<String, Object>> input = List.of(Map.of("title", "only"));
        List<Map<String, Object>> result = service.rerankResults("query", input, 5);
        assertEquals(input, result);
    }

    @Test
    void rerankResults_shouldReturnOriginalWhenNoModels() {
        when(modelConfigService.getPrimaryChatModel()).thenThrow(new RuntimeException("no model"));
        List<Map<String, Object>> input = List.of(
                Map.of("title", "A"),
                Map.of("title", "B")
        );

        List<Map<String, Object>> result = service.rerankResults("query", input, 5);

        assertEquals(2, result.size());
        assertEquals(input, result);
    }

    @Test
    void rerankResults_shouldLimitToTopN() {
        when(modelConfigService.getPrimaryChatModel()).thenThrow(new RuntimeException("no model"));
        List<Map<String, Object>> input = List.of(
                Map.of("title", "A"),
                Map.of("title", "B"),
                Map.of("title", "C")
        );

        List<Map<String, Object>> result = service.rerankResults("query", input, 2);

        assertEquals(3, result.size());
    }
}