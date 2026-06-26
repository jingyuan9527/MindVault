package com.mindvault.tokenusage.service;

import com.mindvault.model.entity.ModelConfig;
import com.mindvault.tokenusage.entity.TokenUsage;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface TokenUsageService {

    TokenUsage recordUsage(ModelConfig model, int promptTokens, int completionTokens,
                            String requestSource, String requestId);

    List<Map<String, Object>> getDailySummary(int days);

    List<Map<String, Object>> getBySourceSummary(int days);

    Map<String, Object> getTotalStats(LocalDate start, LocalDate end);
}