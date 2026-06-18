package com.mindvault.tokenusage;

import com.mindvault.model.entity.ModelConfig;
import com.mindvault.tokenusage.entity.TokenUsage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenUsageServiceTest {

    @Mock
    private TokenUsageMapper mapper;

    private TokenUsageService service;

    @Captor
    private ArgumentCaptor<TokenUsage> usageCaptor;

    @BeforeEach
    void setUp() {
        service = new TokenUsageService(mapper);
    }

    private ModelConfig createModelConfig(Long id, String provider, String modelName, String modelType) {
        ModelConfig m = new ModelConfig();
        m.setId(id);
        m.setProvider(provider);
        m.setModelName(modelName);
        m.setModelType(modelType);
        return m;
    }

    @Test
    void recordUsage_shouldInsertWithCorrectCostForKnownProvider() {
        ModelConfig model = createModelConfig(1L, "ALIYUN", "qwen-plus", "CHAT");

        service.recordUsage(model, 1000, 500, "CHAT", "req-1");

        verify(mapper).insert(usageCaptor.capture());
        TokenUsage captured = usageCaptor.getValue();

        assertEquals(1L, captured.getModelId());
        assertEquals("ALIYUN", captured.getProvider());
        assertEquals("qwen-plus", captured.getModelName());
        assertEquals("CHAT", captured.getModelType());
        assertEquals(1000, captured.getPromptTokens());
        assertEquals(500, captured.getCompletionTokens());
        assertEquals(1500, captured.getTotalTokens());
        assertEquals("CHAT", captured.getRequestSource());
        assertEquals("req-1", captured.getRequestId());
        assertNotNull(captured.getCreatedAt());

        // qwen-plus: prompt=0.0008/1K, completion=0.002/1K
        // cost = (1000/1000)*0.0008 + (500/1000)*0.002 = 0.0008 + 0.001 = 0.001800
        assertEquals(0, BigDecimal.valueOf(0.001800).compareTo(captured.getCost()));
    }

    @Test
    void recordUsage_shouldCalculateDeepseekCost() {
        ModelConfig model = createModelConfig(2L, "DEEPSEEK", "deepseek-chat", "CHAT");

        service.recordUsage(model, 2000, 1000, "CHAT", "req-2");

        verify(mapper).insert(usageCaptor.capture());
        TokenUsage captured = usageCaptor.getValue();

        // deepseek-chat: prompt=0.00014/1K, completion=0.00028/1K
        // cost = (2000/1000)*0.00014 + (1000/1000)*0.00028 = 0.00028 + 0.00028 = 0.000560
        assertEquals(0, BigDecimal.valueOf(0.000560).compareTo(captured.getCost()));
    }

    @Test
    void recordUsage_shouldCalculateOpenaiGpt4Cost() {
        ModelConfig model = createModelConfig(3L, "OPENAI", "gpt-4", "CHAT");

        service.recordUsage(model, 500, 200, "CHAT", "req-3");

        verify(mapper).insert(usageCaptor.capture());
        TokenUsage captured = usageCaptor.getValue();

        // gpt-4: prompt=0.03/1K, completion=0.06/1K
        // cost = (500/1000)*0.03 + (200/1000)*0.06 = 0.015 + 0.012 = 0.027000
        assertEquals(0, BigDecimal.valueOf(0.027000).compareTo(captured.getCost()));
    }

    @Test
    void recordUsage_shouldReturnZeroCostForUnknownProvider() {
        ModelConfig model = createModelConfig(4L, "UNKNOWN_PROVIDER", "some-model", "CHAT");

        service.recordUsage(model, 1000, 500, "CHAT", "req-4");

        verify(mapper).insert(usageCaptor.capture());
        assertEquals(0, BigDecimal.ZERO.compareTo(usageCaptor.getValue().getCost()));
    }

    @Test
    void recordUsage_shouldReturnZeroCostForUnknownModel() {
        ModelConfig model = createModelConfig(5L, "OPENAI", "unknown-model", "CHAT");

        service.recordUsage(model, 1000, 500, "CHAT", "req-5");

        verify(mapper).insert(usageCaptor.capture());
        assertEquals(0, BigDecimal.ZERO.compareTo(usageCaptor.getValue().getCost()));
    }

    @Test
    void getDailySummary_shouldMapObjectRowsToMap() {
        Object[] row1 = {LocalDate.of(2024, 1, 1), "OPENAI", "gpt-4", "CHAT", 1000L, 500L, 1500L, BigDecimal.valueOf(0.05), 3L};
        Object[] row2 = {LocalDate.of(2024, 1, 2), "DEEPSEEK", "deepseek-chat", "CHAT", 2000L, 1000L, 3000L, BigDecimal.valueOf(0.01), 5L};
        when(mapper.findDailySummary(7)).thenReturn(List.of(row1, row2));

        List<Map<String, Object>> result = service.getDailySummary(7);

        assertEquals(2, result.size());
        assertEquals(LocalDate.of(2024, 1, 1), result.get(0).get("date"));
        assertEquals("OPENAI", result.get(0).get("provider"));
        assertEquals("gpt-4", result.get(0).get("modelName"));
        assertEquals("CHAT", result.get(0).get("modelType"));
        assertEquals(1000L, result.get(0).get("promptTokens"));
        assertEquals(500L, result.get(0).get("completionTokens"));
        assertEquals(1500L, result.get(0).get("totalTokens"));
        assertEquals(BigDecimal.valueOf(0.05), result.get(0).get("cost"));
        assertEquals(3L, result.get(0).get("requestCount"));

        assertEquals(5L, result.get(1).get("requestCount"));
        verify(mapper).findDailySummary(7);
    }

    @Test
    void getTotalStats_shouldBuildCorrectResponse() {
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 31);
        Object[] stats = {50000L, BigDecimal.valueOf(2.50)};
        when(mapper.findTotalTokensAndCost(start, end)).thenReturn(stats);
        when(mapper.findByCreatedAtBetweenOrderByCreatedAtDesc(start, end)).thenReturn(List.of(
                new TokenUsage(), new TokenUsage(), new TokenUsage()
        ));

        Map<String, Object> result = service.getTotalStats(start, end);

        assertEquals(start, result.get("startDate"));
        assertEquals(end, result.get("endDate"));
        assertEquals(50000L, result.get("totalTokens"));
        assertEquals(BigDecimal.valueOf(2.50), result.get("totalCost"));
        assertEquals(3, result.get("requestCount"));
    }
}