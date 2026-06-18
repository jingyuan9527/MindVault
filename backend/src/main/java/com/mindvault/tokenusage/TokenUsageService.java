package com.mindvault.tokenusage;

import com.mindvault.model.entity.ModelConfig;
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

@Service
public class TokenUsageService {

    private static final Logger log = LoggerFactory.getLogger(TokenUsageService.class);

    private final TokenUsageMapper mapper;

    private static final Map<String, Map<String, BigDecimal[]>> PRICING = Map.of(
            "ALIYUN", Map.of(
                    "qwen-turbo", new BigDecimal[]{BigDecimal.valueOf(0.0003), BigDecimal.valueOf(0.0006)},
                    "qwen-plus", new BigDecimal[]{BigDecimal.valueOf(0.0008), BigDecimal.valueOf(0.002)},
                    "qwen-max", new BigDecimal[]{BigDecimal.valueOf(0.002), BigDecimal.valueOf(0.006)}
            ),
            "DEEPSEEK", Map.of(
                    "deepseek-chat", new BigDecimal[]{BigDecimal.valueOf(0.00014), BigDecimal.valueOf(0.00028)},
                    "deepseek-coder", new BigDecimal[]{BigDecimal.valueOf(0.00014), BigDecimal.valueOf(0.00028)}
            ),
            "OPENAI", Map.of(
                    "gpt-3.5-turbo", new BigDecimal[]{BigDecimal.valueOf(0.0015), BigDecimal.valueOf(0.002)},
                    "gpt-4", new BigDecimal[]{BigDecimal.valueOf(0.03), BigDecimal.valueOf(0.06)},
                    "gpt-4o", new BigDecimal[]{BigDecimal.valueOf(0.005), BigDecimal.valueOf(0.015)}
            )
    );

    public TokenUsageService(TokenUsageMapper mapper) {
        this.mapper = mapper;
    }

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

    public List<Map<String, Object>> getDailySummary(int days) {
        List<Object[]> rows = mapper.findDailySummary(days);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", row[0]);
            item.put("provider", row[1]);
            item.put("modelName", row[2]);
            item.put("modelType", row[3]);
            item.put("promptTokens", row[4]);
            item.put("completionTokens", row[5]);
            item.put("totalTokens", row[6]);
            item.put("cost", row[7]);
            item.put("requestCount", row[8]);
            result.add(item);
        }
        return result;
    }

    public Map<String, Object> getTotalStats(LocalDate start, LocalDate end) {
        Object[] stats = mapper.findTotalTokensAndCost(start, end);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("startDate", start);
        result.put("endDate", end);
        result.put("totalTokens", stats[0]);
        result.put("totalCost", stats[1]);
        result.put("requestCount", mapper.findByCreatedAtBetweenOrderByCreatedAtDesc(start, end).size());
        return result;
    }

    @Scheduled(cron = "0 30 3 * * ?")
    public void scheduledTokenAggregation() {
        log.info("开始执行定时 Token 用量统计...");
        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            Object[] stats = mapper.findTotalTokensAndCost(yesterday, LocalDate.now());
            long totalTokens = stats[0] instanceof Number n ? n.longValue() : 0;
            log.info("Token 用量统计完成: 日期={}, 总Token={}, 总费用={}",
                    yesterday, totalTokens, stats[1]);
        } catch (Exception e) {
            log.warn("Token 用量统计失败: {}", e.getMessage());
        }
    }

    private BigDecimal calculateCost(String provider, String modelName, int promptTokens, int completionTokens) {
        Map<String, BigDecimal[]> modelPrices = PRICING.get(provider.toUpperCase());
        if (modelPrices == null) return BigDecimal.ZERO;

        BigDecimal[] prices = null;
        for (Map.Entry<String, BigDecimal[]> entry : modelPrices.entrySet()) {
            if (modelName.startsWith(entry.getKey()) || modelName.equals(entry.getKey())) {
                prices = entry.getValue();
                break;
            }
        }
        if (prices == null) return BigDecimal.ZERO;

        BigDecimal promptCost = BigDecimal.valueOf(promptTokens / 1000.0).multiply(prices[0]);
        BigDecimal completionCost = BigDecimal.valueOf(completionTokens / 1000.0).multiply(prices[1]);
        return promptCost.add(completionCost).setScale(6, RoundingMode.HALF_UP);
    }
}