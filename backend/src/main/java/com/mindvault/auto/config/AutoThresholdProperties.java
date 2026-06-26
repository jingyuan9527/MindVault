package com.mindvault.auto.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mindvault.auto-threshold")
public class AutoThresholdProperties {

    private int truncateLength = 2000;
    private double llmTemperature = 0.3;
    private int titleMaxTokens = 100;
    private int summaryMaxTokens = 300;
    private int tagsMaxTokens = 300;
    private int embeddingTruncateLength = 8000;
    private int aggregationBatchSize = 50;
    private int tagCloudTopN = 50;

    public int getTruncateLength() { return truncateLength; }
    public void setTruncateLength(int truncateLength) { this.truncateLength = truncateLength; }
    public double getLlmTemperature() { return llmTemperature; }
    public void setLlmTemperature(double llmTemperature) { this.llmTemperature = llmTemperature; }
    public int getTitleMaxTokens() { return titleMaxTokens; }
    public void setTitleMaxTokens(int titleMaxTokens) { this.titleMaxTokens = titleMaxTokens; }
    public int getSummaryMaxTokens() { return summaryMaxTokens; }
    public void setSummaryMaxTokens(int summaryMaxTokens) { this.summaryMaxTokens = summaryMaxTokens; }
    public int getTagsMaxTokens() { return tagsMaxTokens; }
    public void setTagsMaxTokens(int tagsMaxTokens) { this.tagsMaxTokens = tagsMaxTokens; }
    public int getEmbeddingTruncateLength() { return embeddingTruncateLength; }
    public void setEmbeddingTruncateLength(int embeddingTruncateLength) { this.embeddingTruncateLength = embeddingTruncateLength; }
    public int getAggregationBatchSize() { return aggregationBatchSize; }
    public void setAggregationBatchSize(int aggregationBatchSize) { this.aggregationBatchSize = aggregationBatchSize; }
    public int getTagCloudTopN() { return tagCloudTopN; }
    public void setTagCloudTopN(int tagCloudTopN) { this.tagCloudTopN = tagCloudTopN; }
}