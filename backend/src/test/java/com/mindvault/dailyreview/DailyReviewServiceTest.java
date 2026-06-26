package com.mindvault.dailyreview;

import com.mindvault.ai.client.AiService;
import com.mindvault.dailyreview.config.DailyReviewProperties;
import com.mindvault.dailyreview.entity.DailyReview;
import com.mindvault.dailyreview.mapper.DailyReviewMapper;
import com.mindvault.dailyreview.service.DailyReviewService;
import com.mindvault.dailyreview.service.DailyReviewServiceImpl;
import com.mindvault.knowledge.mapper.KnowledgeMapper;
import com.mindvault.knowledge.entity.Knowledge;
import com.mindvault.model.service.ModelConfigService;
import com.mindvault.systemconfig.service.SystemConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyReviewServiceTest {

    @Mock private ModelConfigService modelConfigService;
    @Mock private AiService aiService;
    @Mock private KnowledgeMapper knowledgeMapper;
    @Mock private DailyReviewMapper mapper;
    private DailyReviewProperties dailyReviewProperties;
    @Mock private SystemConfigService config;

    private DailyReviewService service;

    @Captor private ArgumentCaptor<DailyReview> reportCaptor;

    @BeforeEach
    void setUp() {
        dailyReviewProperties = new DailyReviewProperties();
        service = new DailyReviewServiceImpl(modelConfigService, aiService, knowledgeMapper, mapper, config, dailyReviewProperties);
        lenient().when(config.getInt(anyString(), anyInt())).thenAnswer(i -> i.getArgument(1));
        lenient().when(config.getLong(anyString(), anyLong())).thenAnswer(i -> i.getArgument(1));
        lenient().when(config.getDouble(anyString(), anyDouble())).thenAnswer(i -> i.getArgument(1));
        lenient().when(config.getString(anyString(), anyString())).thenAnswer(i -> i.getArgument(1));
        lenient().when(config.getBool(anyString(), anyBoolean())).thenAnswer(i -> i.getArgument(1));
        lenient().when(config.getPrompt(anyString(), anyString())).thenAnswer(i -> i.getArgument(1));
    }

    private DailyReview createReport(Long id, LocalDate date, int count, String summary) {
        DailyReview report = new DailyReview();
        report.setId(id);
        report.setReportDate(date);
        report.setTotalCount(count);
        report.setSummary(summary);
        report.setKeyInsights("[]");
        report.setRecommendations("[]");
        report.setCategoryBreakdown("{}");
        report.setCreatedAt(LocalDateTime.now());
        return report;
    }

    private Knowledge createKnowledge(Long id, String title, String summary) {
        Knowledge k = new Knowledge();
        k.setId(id);
        k.setTitle(title);
        k.setContent("content " + id);
        k.setSummary(summary);
        k.setCreatedAt(LocalDateTime.now());
        return k;
    }

    @Test
    void getReportByDate_shouldDelegate() {
        LocalDate date = LocalDate.of(2025, 6, 1);
        DailyReview expected = createReport(1L, date, 5, "A good day");
        when(mapper.findByReportDate(date)).thenReturn(Optional.of(expected));

        Optional<DailyReview> result = service.getReportByDate(date);

        assertTrue(result.isPresent());
        assertEquals("A good day", result.get().getSummary());
        verify(mapper).findByReportDate(date);
    }

    @Test
    void getRecentReports_shouldDelegate() {
        int limit = 5;
        List<DailyReview> expected = List.of(
                createReport(2L, LocalDate.of(2025, 6, 2), 3, "Day 2"),
                createReport(1L, LocalDate.of(2025, 6, 1), 5, "Day 1")
        );
        when(mapper.findTopByOrderByReportDateDesc(limit)).thenReturn(expected);

        List<DailyReview> result = service.getRecentReports(limit);

        assertEquals(2, result.size());
        assertEquals("Day 2", result.get(0).getSummary());
        verify(mapper).findTopByOrderByReportDateDesc(limit);
    }

    @Test
    void generateReport_shouldReturnCachedWhenExists() {
        LocalDate date = LocalDate.of(2025, 6, 1);
        DailyReview existing = createReport(1L, date, 3, "Existing report");
        when(mapper.findByReportDate(date)).thenReturn(Optional.of(existing));

        DailyReview result = service.generateReport(date);

        assertSame(existing, result);
        verify(mapper).findByReportDate(date);
        verifyNoMoreInteractions(knowledgeMapper, mapper);
    }

    @Test
    void generateReport_withNoKnowledge_shouldCreateEmptyReport() {
        LocalDate date = LocalDate.of(2025, 6, 1);
        when(mapper.findByReportDate(date)).thenReturn(Optional.empty());
        when(knowledgeMapper.findByCreatedAtBetween(any(), any())).thenReturn(List.of());

        DailyReview result = service.generateReport(date);

        assertNotNull(result);
        assertEquals(date, result.getReportDate());
        assertEquals(0, result.getTotalCount());
        assertEquals("当日无新增知识。", result.getSummary());
        verify(mapper).insert(reportCaptor.capture());
        DailyReview inserted = reportCaptor.getValue();
        assertEquals(date, inserted.getReportDate());
        assertEquals(0, inserted.getTotalCount());
        assertEquals("当日无新增知识。", inserted.getSummary());
    }

    @Test
    void getLatestOrGenerate_shouldReturnExisting() {
        LocalDate today = LocalDate.now();
        DailyReview latest = createReport(1L, today, 2, "Latest report");
        when(mapper.findTopByOrderByReportDateDesc(1)).thenReturn(List.of(latest));

        DailyReview result = service.getLatestOrGenerate();

        assertSame(latest, result);
        verify(mapper).findTopByOrderByReportDateDesc(1);
        verify(mapper, never()).findByReportDate(any());
    }

    @Test
    void getLatestOrGenerate_shouldGenerateWhenNone() {
        when(mapper.findTopByOrderByReportDateDesc(1)).thenReturn(List.of());
        LocalDate today = LocalDate.now();
        when(mapper.findByReportDate(today)).thenReturn(Optional.empty());
        when(knowledgeMapper.findByCreatedAtBetween(any(), any())).thenReturn(List.of());

        DailyReview result = service.getLatestOrGenerate();

        assertNotNull(result);
        assertEquals(today, result.getReportDate());
        assertEquals("当日无新增知识。", result.getSummary());
        verify(mapper).findTopByOrderByReportDateDesc(1);
        verify(mapper).insert(any(DailyReview.class));
    }
}
