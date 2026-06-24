package com.mindvault.dailyreview;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindvault.ai.client.AiService;
import com.mindvault.ai.prompt.PromptRegistry;
import com.mindvault.dailyreview.entity.DailyReview;
import com.mindvault.knowledge.KnowledgeMapper;
import com.mindvault.knowledge.entity.Knowledge;
import com.mindvault.model.ModelConfigService;
import com.mindvault.systemconfig.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
public class DailyReviewService {

    private static final Logger log = LoggerFactory.getLogger(DailyReviewService.class);

    private final ModelConfigService modelConfigService;
    private final AiService aiService;
    private final KnowledgeMapper knowledgeMapper;
    private final DailyReviewMapper mapper;
    private final SystemConfigService config;
    private final ObjectMapper objectMapper;

    public DailyReviewService(ModelConfigService modelConfigService,
                              AiService aiService,
                              KnowledgeMapper knowledgeMapper,
                              DailyReviewMapper mapper,
                              SystemConfigService config) {
        this.modelConfigService = modelConfigService;
        this.aiService = aiService;
        this.knowledgeMapper = knowledgeMapper;
        this.mapper = mapper;
        this.config = config;
        this.objectMapper = new ObjectMapper();
    }

    @Scheduled(cron = "0 30 2 * * ?")
    @Transactional
    public void scheduledDailyReview() {
        if (!config.getBool("task.daily-review.enabled", true)) return;
        log.info("开始执行定时每日复盘...");
        LocalDate yesterday = LocalDate.now().minusDays(1);
        generateReport(yesterday);
    }

    @Transactional
    public DailyReview generateReport(LocalDate date) {
        Optional<DailyReview> existing = mapper.findByReportDate(date);
        if (existing.isPresent()) {
            return existing.get();
        }

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);
        List<Knowledge> dayKnowledge = knowledgeMapper.findByCreatedAtBetween(start, end);

        DailyReview report = new DailyReview();
        report.setReportDate(date);
        report.setTotalCount(dayKnowledge.size());

        if (dayKnowledge.isEmpty()) {
            report.setSummary(config.getString("default.daily-review.empty-summary", "当日无新增知识。"));
            report.setKeyInsights("[]");
            report.setRecommendations("[]");
            report.setCategoryBreakdown("{}");
            report.setCreatedAt(LocalDateTime.now());
            mapper.insert(report);
            return report;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("日期: ").append(date).append("\n\n");
        sb.append("新增知识 (").append(dayKnowledge.size()).append(" 条):\n");
        for (Knowledge k : dayKnowledge) {
            sb.append("- ").append(k.getTitle());
            if (k.getSummary() != null) sb.append(": ").append(k.getSummary());
            sb.append("\n");
        }

        String llmResult = callLlmForReport(sb.toString());

        if (llmResult != null) {
            try {
                Map<String, Object> parsed = parseReportJson(llmResult);
                report.setSummary((String) parsed.getOrDefault("summary", ""));
                report.setKeyInsights(objectMapper.writeValueAsString(
                        parsed.getOrDefault("keyInsights", List.of())));
                report.setRecommendations(objectMapper.writeValueAsString(
                        parsed.getOrDefault("recommendations", List.of())));
                report.setCategoryBreakdown(objectMapper.writeValueAsString(
                        parsed.getOrDefault("categoryBreakdown", Map.of())));
            } catch (Exception e) {
                log.warn("解析 LLM 复盘结果失败: {}", e.getMessage());
                report.setSummary(llmResult);
                report.setKeyInsights("[]");
                report.setRecommendations("[]");
                report.setCategoryBreakdown("{}");
            }
        } else {
            report.setSummary(config.getString("default.daily-review.fallback-summary", "当日知识较多，自动摘要生成失败。"));
            report.setKeyInsights("[]");
            report.setRecommendations("[]");
            report.setCategoryBreakdown("{}");
        }

        report.setCreatedAt(LocalDateTime.now());
        mapper.insert(report);
        return report;
    }

    private String callLlmForReport(String knowledgeSummary) {
        try {
            modelConfigService.getPrimaryChatModel();
        } catch (Exception e) {
            return null;
        }

        double temperature = config.getDouble("threshold.daily-review.temperature", 0.3);
        int maxTokens = config.getInt("threshold.daily-review.max-tokens", 1500);
        String prompt = PromptRegistry.DAILY_REVIEW_REPORT.resolve(config, knowledgeSummary);

        return aiService.call(prompt, temperature, maxTokens);
    }

    private Map<String, Object> parseReportJson(String json) {
        try {
            String cleaned = json.trim();
            if (cleaned.startsWith("```json")) cleaned = cleaned.substring(7);
            if (cleaned.endsWith("```")) cleaned = cleaned.substring(0, cleaned.length() - 3);
            cleaned = cleaned.trim();
            return objectMapper.readValue(cleaned, Map.class);
        } catch (Exception e) {
            Map<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("summary", json);
            fallback.put("keyInsights", List.of());
            fallback.put("recommendations", List.of());
            fallback.put("categoryBreakdown", Map.of());
            return fallback;
        }
    }

    public Optional<DailyReview> getReportByDate(LocalDate date) {
        return mapper.findByReportDate(date);
    }

    public List<DailyReview> getRecentReports(int limit) {
        return mapper.findTopByOrderByReportDateDesc(limit);
    }

    public DailyReview getLatestOrGenerate() {
        return mapper.findTopByOrderByReportDateDesc(1).stream()
                .findFirst()
                .orElseGet(() -> generateReport(LocalDate.now()));
    }
}