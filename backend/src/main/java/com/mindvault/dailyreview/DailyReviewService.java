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

    /**
     * 定时任务: 每天凌晨 2:30 自动生成前一天的回顾报告。
     * 可通过 system_config 中的 task.daily-review.enabled 开关控制。
     */
    @Scheduled(cron = "0 30 2 * * ?")
    @Transactional
    public void scheduledDailyReview() {
        if (!config.getBool("task.daily-review.enabled", true)) return;
        log.info("开始执行定时每日复盘...");
        LocalDate yesterday = LocalDate.now().minusDays(1);
        generateReport(yesterday);
    }

    /**
     * 生成指定日期的回顾报告。
     * <p>
     * 流程: 查询当日新增知识 → 如果为空则返回空模板 → 否则调用 LLM 生成摘要/洞察/建议/分类统计 → 持久化。
     * 如果已有当日的报告则直接返回已有结果（幂等）。
     * </p>
     * @param date 回顾报告日期
     * @return 生成的每日回顾报告
     */
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

    /**
     * 调用 LLM 生成回顾报告文本。
     * 使用 modelConfigService 检查主模型可用性，通过 SystemConfig 读取温度和 maxTokens 参数。
     * @param knowledgeSummary 当日新增知识的汇总文本
     * @return LLM 返回的原始 JSON 字符串，失败时返回 null
     */
    private String callLlmForReport(String knowledgeSummary) {
        try {
            modelConfigService.getPrimaryChatModel();
        } catch (Exception e) {
            return null;
        }

        double temperature = config.getDouble("threshold.daily-review.temperature", 0.3);
        int maxTokens = config.getInt("threshold.daily-review.max-tokens", 1500);
        String prompt = PromptRegistry.DAILY_REVIEW_REPORT.resolve(config, knowledgeSummary);

        return aiService.call(prompt, temperature, maxTokens, "DAILY_REVIEW");
    }

    /**
     * 解析 LLM 返回的 JSON 字符串为结构化 Map。
     * 处理常见的 markdown 代码块包裹 (```json ... ```)，解析失败时以原始文本作为 summary 兜底。
     * @param json LLM 返回的原始文本
     * @return 解析后的 Map，始终包含 summary/keyInsights/recommendations/categoryBreakdown 四个键
     */
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

    /**
     * 按日期查询回顾报告。
     * @param date 目标日期
     * @return 该日期的报告，不存在则返回空
     */
    public Optional<DailyReview> getReportByDate(LocalDate date) {
        return mapper.findByReportDate(date);
    }

    /**
     * 获取最近 N 条回顾报告。
     * @param limit 返回条数上限
     * @return 最近报告列表
     */
    public List<DailyReview> getRecentReports(int limit) {
        return mapper.findTopByOrderByReportDateDesc(limit);
    }

    /**
     * 获取最新报告，若不存在则自动为今天生成一份。
     * @return 最新或今日生成的报告
     */
    public DailyReview getLatestOrGenerate() {
        return mapper.findTopByOrderByReportDateDesc(1).stream()
                .findFirst()
                .orElseGet(() -> generateReport(LocalDate.now()));
    }
}