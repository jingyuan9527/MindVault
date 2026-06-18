package com.mindvault.review;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindvault.review.entity.ReviewSchedule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReviewController.class)
class ReviewControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private ReviewService reviewService;

    private ReviewSchedule createSchedule() {
        ReviewSchedule s = new ReviewSchedule();
        s.setId(1L);
        s.setKnowledgeId(1L);
        s.setNextReviewAt(LocalDateTime.now().plusDays(1));
        s.setIntervalDays(1);
        return s;
    }

    @Test
    void getDueReviews_shouldReturnList() throws Exception {
        when(reviewService.getDueReviews(20)).thenReturn(List.of(
                Map.of("id", 1L, "title", "Review 1", "intervalDays", 1)
        ));

        mockMvc.perform(get("/api/v1/review/due"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("Review 1"));
    }

    @Test
    void getDueReviews_shouldRespectLimit() throws Exception {
        when(reviewService.getDueReviews(5)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/review/due?limit=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void getDueCount_shouldReturnCount() throws Exception {
        when(reviewService.getDueReviewCount()).thenReturn(5L);

        mockMvc.perform(get("/api/v1/review/due-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(5));
    }

    @Test
    void scheduleReview_shouldReturnNextReview() throws Exception {
        ReviewSchedule s = createSchedule();
        when(reviewService.scheduleReview(1L)).thenReturn(s);

        mockMvc.perform(post("/api/v1/review/1/schedule"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nextReviewAt").isNotEmpty());
    }

    @Test
    void performReview_shouldReturnNextReview() throws Exception {
        ReviewSchedule s = createSchedule();
        when(reviewService.performReview(1L, 4)).thenReturn(s);

        mockMvc.perform(post("/api/v1/review/1/perform")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quality\": 4}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nextReviewAt").isNotEmpty());
    }

    @Test
    void performReview_shouldUseDefaultQuality() throws Exception {
        ReviewSchedule s = createSchedule();
        when(reviewService.performReview(1L, 3)).thenReturn(s);

        mockMvc.perform(post("/api/v1/review/1/perform")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }
}