package com.mindvault.review;

import com.mindvault.knowledge.KnowledgeService;
import com.mindvault.knowledge.entity.Knowledge;
import com.mindvault.operationlog.OperationLogService;
import com.mindvault.review.entity.ReviewSchedule;
import com.mindvault.systemconfig.SystemConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock private ReviewScheduleMapper mapper;
    @Mock private KnowledgeService knowledgeService;
    @Mock private OperationLogService operationLogService;
    @Mock private SystemConfigService config;

    private ReviewService service;

    @Captor private ArgumentCaptor<ReviewSchedule> scheduleCaptor;

    @BeforeEach
    void setUp() {
        service = new ReviewService(mapper, knowledgeService, operationLogService, config);
        lenient().when(config.getInt(anyString(), anyInt())).thenAnswer(i -> i.getArgument(1));
        lenient().when(config.getDouble(anyString(), anyDouble())).thenAnswer(i -> i.getArgument(1));
    }

    private ReviewSchedule createSchedule(Long id, Long knowledgeId, BigDecimal easeFactor,
                                           int intervalDays, int reviewCount) {
        ReviewSchedule s = new ReviewSchedule();
        s.setId(id);
        s.setKnowledgeId(knowledgeId);
        s.setEaseFactor(easeFactor);
        s.setIntervalDays(intervalDays);
        s.setReviewCount(reviewCount);
        s.setNextReviewAt(LocalDateTime.now());
        return s;
    }

    @Test
    void scheduleReview_shouldCreateWhenNotExists() {
        when(mapper.findByKnowledgeId(1L)).thenReturn(Optional.empty());

        ReviewSchedule result = service.scheduleReview(1L);

        verify(mapper).insert(scheduleCaptor.capture());
        ReviewSchedule captured = scheduleCaptor.getValue();
        assertEquals(1L, captured.getKnowledgeId());
        assertNotNull(captured.getNextReviewAt());
    }

    @Test
    void scheduleReview_shouldReturnExisting() {
        ReviewSchedule existing = createSchedule(1L, 1L, BigDecimal.valueOf(2.5), 1, 0);
        when(mapper.findByKnowledgeId(1L)).thenReturn(Optional.of(existing));

        ReviewSchedule result = service.scheduleReview(1L);

        assertSame(existing, result);
        verify(mapper, never()).insert(any(ReviewSchedule.class));
    }

    @Test
    void performReview_firstReview_qualityLow_shouldResetInterval() {
        ReviewSchedule s = createSchedule(null, 1L, BigDecimal.valueOf(2.5), 0, 0);
        when(mapper.findByKnowledgeId(1L)).thenReturn(Optional.of(s));

        ReviewSchedule result = service.performReview(1L, 2);

        assertEquals(1, result.getIntervalDays());
        assertEquals(1, result.getReviewCount());
        assertEquals(0, result.getEaseFactor().compareTo(BigDecimal.valueOf(2.30)));
        assertTrue(result.getEaseFactor().compareTo(BigDecimal.valueOf(2.50)) < 0);
        assertNotNull(result.getLastReviewAt());
        verify(mapper).insert(any(ReviewSchedule.class));
    }

    @Test
    void performReview_firstReview_qualityGood_shouldSetInterval1() {
        ReviewSchedule s = createSchedule(null, 1L, BigDecimal.valueOf(2.5), 0, 0);
        when(mapper.findByKnowledgeId(1L)).thenReturn(Optional.of(s));

        ReviewSchedule result = service.performReview(1L, 4);

        assertEquals(1, result.getIntervalDays());
        assertEquals(1, result.getReviewCount());
        assertTrue(result.getEaseFactor().compareTo(BigDecimal.valueOf(2.5)) > 0);
    }

    @Test
    void performReview_secondReview_shouldSetInterval6() {
        ReviewSchedule s = createSchedule(1L, 1L, BigDecimal.valueOf(2.5), 1, 1);
        when(mapper.findByKnowledgeId(1L)).thenReturn(Optional.of(s));

        ReviewSchedule result = service.performReview(1L, 4);

        assertEquals(6, result.getIntervalDays());
        assertEquals(2, result.getReviewCount());
        verify(mapper).updateById(any(ReviewSchedule.class));
    }

    @Test
    void performReview_thirdReview_shouldMultiplyByEaseFactor() {
        ReviewSchedule s = createSchedule(1L, 1L, BigDecimal.valueOf(2.5), 6, 2);
        when(mapper.findByKnowledgeId(1L)).thenReturn(Optional.of(s));

        ReviewSchedule result = service.performReview(1L, 4);

        int expectedInterval = BigDecimal.valueOf(6)
                .multiply(BigDecimal.valueOf(2.6))
                .setScale(0, java.math.RoundingMode.HALF_UP)
                .intValue();
        assertEquals(15, result.getIntervalDays());
        assertEquals(3, result.getReviewCount());
    }

    @Test
    void performReview_quality5_shouldIncreaseEaseFactor() {
        ReviewSchedule s = createSchedule(1L, 1L, BigDecimal.valueOf(2.5), 6, 3);
        when(mapper.findByKnowledgeId(1L)).thenReturn(Optional.of(s));

        ReviewSchedule result = service.performReview(1L, 5);

        BigDecimal expectedEF = BigDecimal.valueOf(2.5)
                .add(BigDecimal.valueOf(2).multiply(BigDecimal.valueOf(0.10)));
        assertEquals(expectedEF.setScale(2, java.math.RoundingMode.HALF_UP), result.getEaseFactor());
    }

    @Test
    void performReview_quality0_shouldDecreaseEaseFactor() {
        ReviewSchedule s = createSchedule(1L, 1L, BigDecimal.valueOf(2.5), 6, 5);
        when(mapper.findByKnowledgeId(1L)).thenReturn(Optional.of(s));

        ReviewSchedule result = service.performReview(1L, 0);

        assertEquals(1, result.getIntervalDays());
        BigDecimal expectedEF = BigDecimal.valueOf(2.5).subtract(BigDecimal.valueOf(0.20));
        assertEquals(expectedEF.setScale(2, java.math.RoundingMode.HALF_UP), result.getEaseFactor());
    }

    @Test
    void performReview_easeFactorShouldNeverGoBelow130() {
        ReviewSchedule s = createSchedule(1L, 1L, BigDecimal.valueOf(1.40), 6, 5);
        when(mapper.findByKnowledgeId(1L)).thenReturn(Optional.of(s));

        ReviewSchedule result = service.performReview(1L, 0);

        assertTrue(result.getEaseFactor().compareTo(BigDecimal.valueOf(1.30)) >= 0);
        assertEquals(1, result.getIntervalDays());
    }

    @Test
    void performReview_qualityShouldBeClamped() {
        ReviewSchedule s = createSchedule(1L, 1L, BigDecimal.valueOf(2.5), 6, 3);
        when(mapper.findByKnowledgeId(1L)).thenReturn(Optional.of(s));

        ReviewSchedule result = service.performReview(1L, 10);

        BigDecimal maxAdjustment = BigDecimal.valueOf(5 - 3).multiply(BigDecimal.valueOf(0.10));
        BigDecimal expected = BigDecimal.valueOf(2.5).add(maxAdjustment);
        assertEquals(0, expected.compareTo(result.getEaseFactor()));
    }

    @Test
    void performReview_shouldCreateNewIfNotExists() {
        when(mapper.findByKnowledgeId(1L)).thenReturn(Optional.empty());

        ReviewSchedule result = service.performReview(1L, 3);

        assertNotNull(result.getCreatedAt());
        assertEquals(1, result.getReviewCount());
        assertEquals(1, result.getIntervalDays());
        verify(mapper).insert(any(ReviewSchedule.class));
    }

    @Test
    void performReview_shouldLogOperation() {
        ReviewSchedule s = createSchedule(1L, 1L, BigDecimal.valueOf(2.5), 6, 3);
        when(mapper.findByKnowledgeId(1L)).thenReturn(Optional.of(s));

        service.performReview(1L, 4);

        verify(operationLogService).log(eq("REVIEW"), eq("PERFORM"), eq(1L), contains("质量=4"));
    }

    @Test
    void getDueReviews_shouldReturnItemsWithKnowledge() {
        LocalDateTime now = LocalDateTime.now();
        ReviewSchedule s1 = createSchedule(1L, 10L, BigDecimal.valueOf(2.5), 1, 0);
        ReviewSchedule s2 = createSchedule(2L, 20L, BigDecimal.valueOf(2.5), 1, 0);
        when(mapper.findDueReviews(any())).thenReturn(List.of(s1, s2));

        Knowledge k1 = new Knowledge();
        k1.setId(10L);
        k1.setTitle("Title1");
        k1.setContent("Content1");
        when(knowledgeService.getById(10L)).thenReturn(k1);
        when(knowledgeService.getById(20L)).thenThrow(new IllegalArgumentException("not found"));

        List<Map<String, Object>> results = service.getDueReviews(10);

        assertEquals(1, results.size());
        assertEquals("Title1", results.get(0).get("title"));
    }

    @Test
    void getDueReviews_shouldRespectLimit() {
        ReviewSchedule s1 = createSchedule(1L, 1L, BigDecimal.valueOf(2.5), 1, 0);
        ReviewSchedule s2 = createSchedule(2L, 2L, BigDecimal.valueOf(2.5), 1, 0);
        ReviewSchedule s3 = createSchedule(3L, 3L, BigDecimal.valueOf(2.5), 1, 0);
        when(mapper.findDueReviews(any())).thenReturn(List.of(s1, s2, s3));

        Knowledge k = new Knowledge();
        k.setId(1L);
        k.setTitle("T");
        k.setContent("C");
        when(knowledgeService.getById(anyLong())).thenReturn(k);

        List<Map<String, Object>> results = service.getDueReviews(2);

        assertEquals(2, results.size());
    }

    @Test
    void getDueReviewCount_shouldDelegate() {
        when(mapper.countDueReviews(any())).thenReturn(5L);

        long count = service.getDueReviewCount();

        assertEquals(5L, count);
    }
}