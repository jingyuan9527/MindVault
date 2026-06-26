package com.mindvault.dailyreview.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mindvault.daily-review")
public class DailyReviewProperties {

    private boolean taskEnabled = true;
    private String emptySummary = "当日无新增知识。";
    private String fallbackSummary = "当日知识较多，自动摘要生成失败。";
    private double temperature = 0.3;
    private int maxTokens = 1500;

    public boolean isTaskEnabled() { return taskEnabled; }
    public void setTaskEnabled(boolean taskEnabled) { this.taskEnabled = taskEnabled; }
    public String getEmptySummary() { return emptySummary; }
    public void setEmptySummary(String emptySummary) { this.emptySummary = emptySummary; }
    public String getFallbackSummary() { return fallbackSummary; }
    public void setFallbackSummary(String fallbackSummary) { this.fallbackSummary = fallbackSummary; }
    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
}