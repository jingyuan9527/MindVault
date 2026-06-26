package com.mindvault.knowledge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mindvault.search")
public class SearchProperties {

    private double rewriteTemperature = 0.2;
    private int rewriteMaxTokens = 100;
    private double hydeTemperature = 0.3;
    private int hydeMaxTokens = 500;
    private int hydeEmbeddingTruncate = 8000;
    private int rerankTruncateLength = 300;
    private double rerankTemperature = 0.1;
    private int rerankMaxTokens = 200;
    private int fetchLimitMultiplier = 3;
    private int minFetchLimit = 20;
    private double rrfK = 60.0;

    public double getRewriteTemperature() { return rewriteTemperature; }
    public void setRewriteTemperature(double rewriteTemperature) { this.rewriteTemperature = rewriteTemperature; }
    public int getRewriteMaxTokens() { return rewriteMaxTokens; }
    public void setRewriteMaxTokens(int rewriteMaxTokens) { this.rewriteMaxTokens = rewriteMaxTokens; }
    public double getHydeTemperature() { return hydeTemperature; }
    public void setHydeTemperature(double hydeTemperature) { this.hydeTemperature = hydeTemperature; }
    public int getHydeMaxTokens() { return hydeMaxTokens; }
    public void setHydeMaxTokens(int hydeMaxTokens) { this.hydeMaxTokens = hydeMaxTokens; }
    public int getHydeEmbeddingTruncate() { return hydeEmbeddingTruncate; }
    public void setHydeEmbeddingTruncate(int hydeEmbeddingTruncate) { this.hydeEmbeddingTruncate = hydeEmbeddingTruncate; }
    public int getRerankTruncateLength() { return rerankTruncateLength; }
    public void setRerankTruncateLength(int rerankTruncateLength) { this.rerankTruncateLength = rerankTruncateLength; }
    public double getRerankTemperature() { return rerankTemperature; }
    public void setRerankTemperature(double rerankTemperature) { this.rerankTemperature = rerankTemperature; }
    public int getRerankMaxTokens() { return rerankMaxTokens; }
    public void setRerankMaxTokens(int rerankMaxTokens) { this.rerankMaxTokens = rerankMaxTokens; }
    public int getFetchLimitMultiplier() { return fetchLimitMultiplier; }
    public void setFetchLimitMultiplier(int fetchLimitMultiplier) { this.fetchLimitMultiplier = fetchLimitMultiplier; }
    public int getMinFetchLimit() { return minFetchLimit; }
    public void setMinFetchLimit(int minFetchLimit) { this.minFetchLimit = minFetchLimit; }
    public double getRrfK() { return rrfK; }
    public void setRrfK(double rrfK) { this.rrfK = rrfK; }
}