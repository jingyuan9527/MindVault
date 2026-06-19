package com.mindvault.dailyreview;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindvault.agent.config.AgentConfig;
import com.mindvault.common.service.MetricsService;
import com.mindvault.dailyreview.entity.DailyReview;
import com.mindvault.knowledge.KnowledgeMapper;
import com.mindvault.knowledge.entity.Knowledge;
import com.mindvault.model.ModelConfigService;
import com.mindvault.model.entity.ModelConfig;
import com.mindvault.tokenusage.TokenUsageService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
public class DailyReviewService {

    private static final Logger log = LoggerFactory.getLogger(DailyReviewService.class);

    private final ModelConfigService modelConfigService;
    private final AgentConfig agentConfig;
    private final KnowledgeMapper knowledgeMapper;
    private final DailyReviewMapper mapper;
    private final TokenUsageService tokenUsageService;
    private final MetricsService metricsService;
    private final ObjectMapper objectMapper;

    private volatile List<LlmEndpoint> modelEndpoints = List.of();

    public DailyReviewService(ModelConfigService modelConfigService,
                              AgentConfig agentConfig,
                              KnowledgeMapper knowledgeMapper,
                              DailyReviewMapper mapper,
                              TokenUsageService tokenUsageService,
                              MetricsService metricsService) {
        this.modelConfigService = modelConfigService;
        this.agentConfig = agentConfig;
        this.knowledgeMapper = knowledgeMapper;
        this.mapper = mapper;
        this.tokenUsageService = tokenUsageService;
        this.metricsService = metricsService;
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void init() {
        refreshModels();
    }

    public void refreshModels() {
        try {
            List<ModelConfig> models = modelConfigService.getAvailableChatModels();
            modelEndpoints = models.stream()
                    .map(mc -> new LlmEndpoint(mc, agentConfig.buildEndpoint(mc)))
                    .toList();
            log.info("DailyReviewService 初始化完成，可用模型数: {}", modelEndpoints.size());
        } catch (Exception e) {
            log.warn("DailyReviewService 初始化失败: {}", e.getMessage());
        }
    }

    @Scheduled(cron = "0 30 2 * * ?")
    @Transactional
    public void scheduledDailyReview() {
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
            report.setSummary("当日无新增知识。");
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
            report.setSummary("当日知识较多，自动摘要生成失败。");
            report.setKeyInsights("[]");
            report.setRecommendations("[]");
            report.setCategoryBreakdown("{}");
        }

        report.setCreatedAt(LocalDateTime.now());
        mapper.insert(report);
        return report;
    }

    private String callLlmForReport(String knowledgeSummary) {
        String prompt = "你是一个每日知识复盘助手。请根据以下今日新增知识，生成一份复盘报告。" +
                "返回JSON格式，包含以下字段：\n" +
                "1. summary: 一段概括性总结（50-100字）\n" +
                "2. keyInsights: 关键洞见数组（3-5条）\n" +
                "3. recommendations: 后续建议数组（2-3条）\n" +
                "4. categoryBreakdown: 知识分类统计对象\n\n" +
                "只返回JSON，不要额外说明。\n\n" + knowledgeSummary;

        String result = callLlmWithFailover(prompt);
        return result;
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

    @SuppressWarnings("unchecked")
    private String callLlmWithFailover(String prompt) {
        List<String> errors = new ArrayList<>();
        for (LlmEndpoint me : modelEndpoints) {
            try {
                RestClient client = RestClient.builder()
                        .baseUrl(me.endpoint.getFullUrl())
                        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .defaultHeader("Authorization", "Bearer " + me.endpoint.getApiKey())
                        .build();

                Map<String, Object> requestBody = new LinkedHashMap<>();
                requestBody.put("model", me.endpoint.getModelName());
                requestBody.put("messages", List.of(Map.of("role", "user", "content", prompt)));
                requestBody.put("temperature", 0.3);
                requestBody.put("max_tokens", 1500);

                String responseJson = client.post()
                        .body(objectMapper.writeValueAsString(requestBody))
                        .retrieve()
                        .body(String.class);

                Map<?, ?> responseMap = objectMapper.readValue(responseJson, Map.class);
                String content = extractContent(responseMap);
                if (content != null) {
                    recordUsage(me, responseMap);
                    return content.trim();
                }
            } catch (Exception e) {
                log.warn("模型调用失败: {}", e.getMessage());
                errors.add(e.getMessage());
            }
        }
        log.warn("所有模型均调用失败: {}", String.join("; ", errors));
        return null;
    }

    @SuppressWarnings("unchecked")
    private void recordUsage(LlmEndpoint me, Map<?, ?> responseMap) {
        try {
            Map<String, Object> usage = (Map<String, Object>) responseMap.get("usage");
            if (usage != null) {
                int promptTokens = ((Number) usage.getOrDefault("prompt_tokens", 0)).intValue();
                int completionTokens = ((Number) usage.getOrDefault("completion_tokens", 0)).intValue();
                metricsService.recordTokens(promptTokens, completionTokens);
                tokenUsageService.recordUsage(me.modelConfig, promptTokens, completionTokens, "DAILY_REVIEW", null);
            }
        } catch (Exception e) {
            log.warn("记录 DailyReview Token 用量失败: {}", e.getMessage());
        }
    }

    private String extractContent(Map<?, ?> responseMap) {
        if (responseMap.containsKey("choices")) {
            List<?> choices = (List<?>) responseMap.get("choices");
            if (!choices.isEmpty()) {
                Map<?, ?> choice = (Map<?, ?>) choices.get(0);
                Map<?, ?> message = (Map<?, ?>) choice.get("message");
                if (message != null && message.get("content") instanceof String s) return s;
                if (choice.get("text") instanceof String s) return s;
            }
        }
        if (responseMap.containsKey("message")) {
            Map<?, ?> message = (Map<?, ?>) responseMap.get("message");
            if (message.get("content") instanceof String s) return s;
        }
        if (responseMap.containsKey("response")) {
            Object resp = responseMap.get("response");
            if (resp instanceof String s) return s;
        }
        return null;
    }

    private record LlmEndpoint(ModelConfig modelConfig, AgentConfig.LlmEndpoint endpoint) {}
}