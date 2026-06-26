package com.mindvault.tokenusage;

import com.mindvault.common.config.MindVaultProperties;
import com.mindvault.model.entity.ModelConfig;
import com.mindvault.systemconfig.SystemConfigService;
import com.mindvault.tokenusage.entity.TokenUsage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Token 用量统计服务。
 * <p>
 * 核心职责: 记录每次 LLM 调用的 Token 消耗、查询每日/按来源的汇总统计、
 * 以及定时汇总统计任务。费用根据 MindVaultProperties 中配置的定价表按提供商+模型匹配计算。
 * </p>
 * <p>
 * 关键设计:
 * <ul>
 *   <li>计费模型: 通过 MindVaultProperties.pricing 配置各提供商的输入/输出单价（每千 Token）</li>
 *   <li>模型匹配: 按名称前缀匹配 pricing 配置（如 modelName 以 "gpt-4" 开头则匹配 gpt-4 的价格）</li>
 *   <li>每天凌晨 3:30 执行一次定时汇总统计，可通过 task.token-usage.enabled 开关控制</li>
 * </ul>
 * </p>
 * <p>依赖: TokenUsageMapper, MindVaultProperties, SystemConfigService</p>
 */
@Service
public class TokenUsageService {

    private static final Logger log = LoggerFactory.getLogger(TokenUsageService.class);

    private final TokenUsageMapper mapper;
    private final MindVaultProperties properties;
    private final SystemConfigService config;

    public TokenUsageService(TokenUsageMapper mapper, MindVaultProperties properties, SystemConfigService config) {
        this.mapper = mapper;
        this.properties = properties;
        this.config = config;
    }

    /**
     * 记录一次 LLM 调用的 Token 消耗。
     * <p>
     * 自动计算总 Token 数、根据定价表估算费用，并写入 DB。
     * </p>
     * @param model            使用的模型配置（用于获取提供商、模型名称等）
     * @param promptTokens     提示词 Token 数
     * @param completionTokens 补全 Token 数
     * @param requestSource    请求来源（如 CHAT / DAILY_REVIEW / WRITING 等）
     * @param requestId        请求 ID
     * @return 持久化后的 Token 用量记录
     */
    public TokenUsage recordUsage(ModelConfig model, int promptTokens, int completionTokens,
                                   String requestSource, String requestId) {
        int totalTokens = promptTokens + completionTokens;
        BigDecimal cost = calculateCost(model.getProvider(), model.getModelName(), promptTokens, completionTokens);

        TokenUsage usage = new TokenUsage();
        usage.setModelId(model.getId());
        usage.setProvider(model.getProvider());
        usage.setModelName(model.getModelName());
        usage.setModelType(model.getModelType());
        usage.setPromptTokens(promptTokens);
        usage.setCompletionTokens(completionTokens);
        usage.setTotalTokens(totalTokens);
        usage.setCost(cost);
        usage.setRequestSource(requestSource);
        usage.setRequestId(requestId);
        usage.setCreatedAt(LocalDateTime.now());

        mapper.insert(usage);
        log.debug("记录 Token 用量: model={}, prompt={}, completion={}, cost={}",
                model.getModelName(), promptTokens, completionTokens, cost);
        return usage;
    }

    /**
     * 获取最近 N 天每日用量汇总。
     * @param days 天数范围
     * @return 每日汇总列表，每条包含 date/provider/modelName/totalTokens/cost/requestCount 等字段
     */
    public List<Map<String, Object>> getDailySummary(int days) {
        List<Map<String, Object>> rows = mapper.findDailySummary(days);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", row.get("date"));
            item.put("provider", row.get("provider"));
            item.put("modelName", row.get("model_name"));
            item.put("modelType", row.get("model_type"));
            item.put("promptTokens", row.get("prompt_tokens"));
            item.put("completionTokens", row.get("completion_tokens"));
            item.put("totalTokens", row.get("total_tokens"));
            item.put("cost", row.get("cost"));
            item.put("requestCount", row.get("request_count"));
            result.add(item);
        }
        return result;
    }

    /**
     * 获取最近 N 天按请求来源分组的用量统计。
     * @param days 天数范围
     * @return 来源统计列表，按 total_tokens 降序排列
     */
    public List<Map<String, Object>> getBySourceSummary(int days) {
        return mapper.findBySourceSummary(days);
    }

    /**
     * 获取指定时间范围内的总计用量统计。
     * @param start 开始日期（含）
     * @param end   结束日期（不含）
     * @return 包含 startDate/endDate/totalTokens/totalCost/requestCount 的 Map
     */
    public Map<String, Object> getTotalStats(LocalDate start, LocalDate end) {
        Map<String, Object> stats = mapper.findTotalTokensAndCost(start, end);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("startDate", start);
        result.put("endDate", end);
        result.put("totalTokens", stats.get("total_tokens"));
        result.put("totalCost", stats.get("total_cost"));
        result.put("requestCount", mapper.findByCreatedAtBetweenOrderByCreatedAtDesc(start, end).size());
        return result;
    }

    /**
     * 定时任务: 每天凌晨 3:30 汇总前一天的 Token 用量并记录日志。
     * 可通过 task.token-usage.enabled 开关控制。
     */
    @Scheduled(cron = "0 30 3 * * ?")
    public void scheduledTokenAggregation() {
        if (!config.getBool("task.token-usage.enabled", true)) return;
        log.info("开始执行定时 Token 用量统计...");
        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            Map<String, Object> stats = mapper.findTotalTokensAndCost(yesterday, LocalDate.now());
            long totalTokens = stats.get("total_tokens") instanceof Number n ? n.longValue() : 0;
            log.info("Token 用量统计完成: 日期={}, 总Token={}, 总费用={}",
                    yesterday, totalTokens, stats.get("total_cost"));
        } catch (Exception e) {
            log.warn("Token 用量统计失败: {}", e.getMessage());
        }
    }

    /**
     * 按定价表计算本次调用的费用。
     * <p>
     * 匹配策略: 按 modelName 的前缀匹配 pricing 配置中的模型键（如 modelName 以 "gpt-4" 开头则匹配 gpt-4 的定价）。
     * 计算公式: promptTokens/除数 × 输入单价 + completionTokens/除数 × 输出单价。
     * 默认除数为 1000（即按每千 Token 计价），可通过 threshold.tokenusage.calc-divisor 配置。
     * </p>
     * @param provider         提供商名称（大写）
     * @param modelName        模型名称
     * @param promptTokens     提示词 Token 数
     * @param completionTokens 补全 Token 数
     * @return 估算费用（保留 6 位小数），无法匹配定价时返回 0
     */
    private BigDecimal calculateCost(String provider, String modelName, int promptTokens, int completionTokens) {
        Map<String, Map<String, BigDecimal[]>> pricing = properties.getPricing().getModels();
        if (pricing == null) return BigDecimal.ZERO;
        Map<String, BigDecimal[]> modelPrices = pricing.get(provider.toUpperCase());
        if (modelPrices == null) return BigDecimal.ZERO;

        BigDecimal[] prices = null;
        for (Map.Entry<String, BigDecimal[]> entry : modelPrices.entrySet()) {
            if (modelName.startsWith(entry.getKey()) || modelName.equals(entry.getKey())) {
                prices = entry.getValue();
                break;
            }
        }
        if (prices == null) return BigDecimal.ZERO;

        double divisor = config.getDouble("threshold.tokenusage.calc-divisor", 1000.0);
        BigDecimal promptCost = BigDecimal.valueOf(promptTokens / divisor).multiply(prices[0]);
        BigDecimal completionCost = BigDecimal.valueOf(completionTokens / divisor).multiply(prices[1]);
        return promptCost.add(completionCost).setScale(6, RoundingMode.HALF_UP);
    }
}