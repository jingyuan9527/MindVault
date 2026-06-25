package com.mindvault.tokenusage;

import com.mindvault.common.config.MindVaultProperties;
import com.mindvault.systemconfig.SystemConfigService;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenUsageServiceTest {

    @Mock
    private TokenUsageMapper mapper;

    @Mock private SystemConfigService config;

    private TokenUsageService service;

    @Captor
    private ArgumentCaptor<TokenUsage> usageCaptor;

    @BeforeEach
    void setUp() {
        MindVaultProperties props = new MindVaultProperties();
        Map<String, Map<String, BigDecimal[]>> pricing = new LinkedHashMap<>();
        Map<String, BigDecimal[]> aliyun = new LinkedHashMap<>();
        aliyun.put("qwen-turbo", new BigDecimal[]{BigDecimal.valueOf(0.0003), BigDecimal.valueOf(0.0006)});
        aliyun.put("qwen-plus", new BigDecimal[]{BigDecimal.valueOf(0.0008), BigDecimal.valueOf(0.002)});
        aliyun.put("qwen-max", new BigDecimal[]{BigDecimal.valueOf(0.002), BigDecimal.valueOf(0.006)});
        pricing.put("ALIYUN", aliyun);
        Map<String, BigDecimal[]> deepseek = new LinkedHashMap<>();
        deepseek.put("deepseek-chat", new BigDecimal[]{BigDecimal.valueOf(0.00014), BigDecimal.valueOf(0.00028)});
        deepseek.put("deepseek-coder", new BigDecimal[]{BigDecimal.valueOf(0.00014), BigDecimal.valueOf(0.00028)});
        pricing.put("DEEPSEEK", deepseek);
        Map<String, BigDecimal[]> openai = new LinkedHashMap<>();
        openai.put("gpt-3.5-turbo", new BigDecimal[]{BigDecimal.valueOf(0.0015), BigDecimal.valueOf(0.002)});
        openai.put("gpt-4", new BigDecimal[]{BigDecimal.valueOf(0.03), BigDecimal.valueOf(0.06)});
        openai.put("gpt-4o", new BigDecimal[]{BigDecimal.valueOf(0.005), BigDecimal.valueOf(0.015)});
        pricing.put("OPENAI", openai);
        props.getPricing().setModels(pricing);
        service = new TokenUsageService(mapper, props, config);
        lenient().when(config.getInt(anyString(), anyInt())).thenAnswer(i -> i.getArgument(1));
        lenient().when(config.getLong(anyString(), anyLong())).thenAnswer(i -> i.getArgument(1));
        lenient().when(config.getDouble(anyString(), anyDouble())).thenAnswer(i -> i.getArgument(1));
        lenient().when(config.getString(anyString(), anyString())).thenAnswer(i -> i.getArgument(1));
        lenient().when(config.getBool(anyString(), anyBoolean())).thenAnswer(i -> i.getArgument(1));
        lenient().when(config.getPrompt(anyString(), anyString())).thenAnswer(i -> i.getArgument(1));
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
        when(mapper.findDailySummary(7)).thenReturn(List.of(
                Map.of("date", LocalDate.of(2024, 1, 1), "provider", "OPENAI", "model_name", "gpt-4", "model_type", "CHAT",
                        "prompt_tokens", 1000L, "completion_tokens", 500L, "total_tokens", 1500L, "cost", BigDecimal.valueOf(0.05), "request_count", 3L),
                Map.of("date", LocalDate.of(2024, 1, 2), "provider", "DEEPSEEK", "model_name", "deepseek-chat", "model_type", "CHAT",
                        "prompt_tokens", 2000L, "completion_tokens", 1000L, "total_tokens", 3000L, "cost", BigDecimal.valueOf(0.01), "request_count", 5L)
        ));

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
    void getBySourceSummary_shouldDelegate() {
        List<Map<String, Object>> expected = List.of(
                Map.of("request_source", "CHAT", "total_tokens", 1000L, "cost", 0.01, "request_count", 5),
                Map.of("request_source", "AUTO_PROCESS", "total_tokens", 500L, "cost", 0.005, "request_count", 3)
        );
        when(mapper.findBySourceSummary(7)).thenReturn(expected);

        List<Map<String, Object>> result = service.getBySourceSummary(7);

        assertEquals(expected, result);
        verify(mapper).findBySourceSummary(7);
    }

    @Test
    void getTotalStats_shouldBuildCorrectResponse() {
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 31);
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total_tokens", 50000L);
        stats.put("total_cost", BigDecimal.valueOf(2.50));
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