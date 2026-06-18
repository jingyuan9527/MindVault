package com.mindvault.tokenusage;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TokenUsageController.class)
class TokenUsageControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private TokenUsageService tokenUsageService;

    @Test
    void getDailySummary_shouldReturnList() throws Exception {
        when(tokenUsageService.getDailySummary(30)).thenReturn(List.of(
                Map.of("date", LocalDate.now(), "provider", "OPENAI", "totalTokens", 500)
        ));

        mockMvc.perform(get("/api/v1/token-usage/daily"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].provider").value("OPENAI"))
                .andExpect(jsonPath("$.data[0].totalTokens").value(500));
    }

    @Test
    void getDailySummary_shouldRespectDaysParam() throws Exception {
        when(tokenUsageService.getDailySummary(7)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/token-usage/daily?days=7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void getTotalStats_shouldReturnStats() throws Exception {
        when(tokenUsageService.getTotalStats(any(), any())).thenReturn(
                Map.of("totalTokens", 1000L, "totalCost", 0.05, "requestCount", 10)
        );

        mockMvc.perform(get("/api/v1/token-usage/total")
                        .param("start", "2024-01-01")
                        .param("end", "2024-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalTokens").value(1000))
                .andExpect(jsonPath("$.data.requestCount").value(10));
    }
}