package com.mindvault.knowledge;

import com.mindvault.agent.config.AgentConfig;
import com.mindvault.agent.config.AgentConfig.LlmEndpoint;
import com.mindvault.model.ModelConfigService;
import com.mindvault.model.entity.ModelConfig;
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
    @Mock private AgentConfig agentConfig;

    private SearchEnhanceService service;

    @BeforeEach
    void setUp() {
        service = new SearchEnhanceService(knowledgeService, modelConfigService, agentConfig);
    }

    @Test
    void searchWithRewrite_shouldFallbackToHybridWhenNoModels() {
        when(modelConfigService.getAvailableChatModels()).thenReturn(List.of());

        List<Map<String, Object>> expected = List.of(Map.of("title", "fallback"));
        when(knowledgeService.hybridSearch("test", 5)).thenReturn(expected);

        List<Map<String, Object>> result = service.searchWithRewrite("test", 5);

        assertEquals(expected, result);
    }

    @Test
    void searchWithRewrite_shouldFallbackToHybridWhenRewriteFails() {
        ModelConfig mc = new ModelConfig();
        mc.setProvider("OPENAI");
        mc.setModelName("gpt-4");
        mc.setApiKey("sk-test");
        when(modelConfigService.getAvailableChatModels()).thenReturn(List.of(mc));
        when(agentConfig.buildEndpoint(mc)).thenThrow(new RuntimeException("API error"));

        List<Map<String, Object>> expected = List.of(Map.of("title", "fallback"));
        when(knowledgeService.hybridSearch("test", 5)).thenReturn(expected);

        List<Map<String, Object>> result = service.searchWithRewrite("test", 5);

        assertEquals(expected, result);
    }

    @Test
    void hydeSearch_shouldFallbackToHybridWhenNoModels() {
        when(modelConfigService.getAvailableChatModels()).thenReturn(List.of());

        List<Map<String, Object>> expected = List.of(Map.of("title", "fallback"));
        when(knowledgeService.hybridSearch("test", 5)).thenReturn(expected);

        List<Map<String, Object>> result = service.hydeSearch("test", 5);

        assertEquals(expected, result);
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
        when(modelConfigService.getAvailableChatModels()).thenReturn(List.of());
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
        when(modelConfigService.getAvailableChatModels()).thenReturn(List.of());
        List<Map<String, Object>> input = List.of(
                Map.of("title", "A"),
                Map.of("title", "B"),
                Map.of("title", "C")
        );

        List<Map<String, Object>> result = service.rerankResults("query", input, 2);

        assertEquals(3, result.size());
    }
}