package com.mindvault.dailyreview;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindvault.dailyreview.controller.DailyReviewController;
import com.mindvault.dailyreview.entity.DailyReview;
import com.mindvault.dailyreview.service.DailyReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DailyReviewController.class)
class DailyReviewControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private DailyReviewService dailyReviewService;

    private DailyReview createReview() {
        DailyReview r = new DailyReview();
        r.setId(1L);
        r.setReportDate(LocalDate.now());
        r.setTotalCount(3);
        r.setSummary("今日新增3条知识");
        r.setKeyInsights("[]");
        r.setRecommendations("[]");
        r.setCategoryBreakdown("{}");
        r.setCreatedAt(LocalDateTime.now());
        return r;
    }

    @Test
    void getLatest_shouldReturnReview() throws Exception {
        when(dailyReviewService.getLatestOrGenerate()).thenReturn(createReview());

        mockMvc.perform(get("/api/v1/daily-review/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.summary").value("今日新增3条知识"));
    }

    @Test
    void getByDate_shouldReturnReview() throws Exception {
        DailyReview r = createReview();
        when(dailyReviewService.getReportByDate(LocalDate.parse("2024-01-15")))
                .thenReturn(Optional.of(r));

        mockMvc.perform(get("/api/v1/daily-review/date/2024-01-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.summary").value("今日新增3条知识"));
    }

    @Test
    void getByDate_shouldReturn404WhenNotFound() throws Exception {
        when(dailyReviewService.getReportByDate(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/daily-review/date/2024-01-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void getRecent_shouldReturnList() throws Exception {
        when(dailyReviewService.getRecentReports(7)).thenReturn(List.of(createReview()));

        mockMvc.perform(get("/api/v1/daily-review/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].summary").value("今日新增3条知识"));
    }

    @Test
    void getRecent_shouldRespectLimit() throws Exception {
        when(dailyReviewService.getRecentReports(3)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/daily-review/recent?limit=3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void generate_shouldReturnReview() throws Exception {
        DailyReview r = createReview();
        when(dailyReviewService.generateReport(LocalDate.parse("2024-01-15"))).thenReturn(r);

        mockMvc.perform(post("/api/v1/daily-review/generate?date=2024-01-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.summary").value("今日新增3条知识"));
    }

    @Test
    void generate_shouldUseTodayWhenNoDate() throws Exception {
        DailyReview r = createReview();
        when(dailyReviewService.generateReport(LocalDate.now())).thenReturn(r);

        mockMvc.perform(post("/api/v1/daily-review/generate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(3));
    }
}