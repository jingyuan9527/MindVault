package com.mindvault.dailyreview.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindvault.ai.client.AiService;
import com.mindvault.ai.prompt.PromptRegistry;
import com.mindvault.dailyreview.config.DailyReviewProperties;
import com.mindvault.dailyreview.entity.DailyReview;
import com.mindvault.dailyreview.mapper.DailyReviewMapper;
import com.mindvault.knowledge.entity.Knowledge;
import com.mindvault.knowledge.mapper.KnowledgeMapper;
import com.mindvault.model.service.ModelConfigService;
import com.mindvault.systemconfig.service.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * 每日回顾报告服务。
 * <p>
 * 核心职责: 每天凌晨 2:30 通过定时任务自动生成前一天的回顾报告（依赖 LLM），
 * 同时提供按日期查询、获取最新报告、手动触发生成等接口。
 * </p>
 * <p>
 * 关键设计:
 * <ul>
 *   <li>报告生成使用 PromptRegistry.DAILY_REVIEW_REPORT 模板调用 LLM，结果解析为结构化 JSON</li>
 *   <li>当天无新增知识时直接返回空模板，不调用 LLM</li>
 *   <li>LLM 调用失败时使用 fallback 摘要文案</li>
 * </ul>
 * </p>
 * <p>依赖: AiService, KnowledgeMapper, DailyReviewMapper, SystemConfigService, ModelConfigService</p>
 */
@Service
public class DailyReviewServiceImpl implements DailyReviewService {

    private static final Logger log = LoggerFactory.getLogger(DailyReviewServiceImpl.class);

    private final ModelConfigService modelConfigService;
    private final AiService aiService;
    private final KnowledgeMapper knowledgeMapper;
    private final DailyReviewMapper mapper;
    private final SystemConfigService systemConfigService;
    private final DailyReviewProperties dailyReviewProperties;
    private final ObjectMapper objectMapper;

    public DailyReviewServiceImpl(ModelConfigService modelConfigService,
                                  AiService aiService,
                                  KnowledgeMapper knowledgeMapper,
                                  DailyReviewMapper mapper,
                                  SystemConfigService systemConfigService,
                                  DailyReviewProperties dailyReviewProperties) {
        this.modelConfigService = modelConfigService;
        this.aiService = aiService;
        this.knowledgeMapper = knowledgeMapper;
        this.mapper = mapper;
        this.systemConfigService = systemConfigService;
        this.dailyReviewProperties = dailyReviewProperties;
        this.objectMapper = new ObjectMapper();
    }

    @Scheduled(cron = "0 30 2 * * ?")
    @Transactional
    public void scheduledDailyReview() {
        if (!dailyReviewProperties.isTaskEnabled()) return;
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
            report.setSummary(dailyReviewProperties.getEmptySummary());
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
            report.setSummary(dailyReviewProperties.getFallbackSummary());
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

        double temperature = dailyReviewProperties.getTemperature();
        int maxTokens = dailyReviewProperties.getMaxTokens();
        String prompt = PromptRegistry.DAILY_REVIEW_REPORT.resolve(systemConfigService, knowledgeSummary);

        return aiService.call(prompt, temperature, maxTokens, "DAILY_REVIEW");
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
