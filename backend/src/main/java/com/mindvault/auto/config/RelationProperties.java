package com.mindvault.auto.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mindvault.relation")
public class RelationProperties {

    private int batchSize = 20;
    private int candidateLimit = 50;
    private int vectorTopN = 10;
    private double similarityMin = 0.5;
    private double scorePerTag = 0.25;
    private double tagScoreMax = 1.0;
    private int llmCandidateLimit = 10;
    private int contentTruncateLength = 1500;
    private double llmTemperature = 0.3;
    private int llmMaxTokens = 500;
    private double llmDefaultScore = 0.75;

    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
    public int getCandidateLimit() { return candidateLimit; }
    public void setCandidateLimit(int candidateLimit) { this.candidateLimit = candidateLimit; }
    public int getVectorTopN() { return vectorTopN; }
    public void setVectorTopN(int vectorTopN) { this.vectorTopN = vectorTopN; }
    public double getSimilarityMin() { return similarityMin; }
    public void setSimilarityMin(double similarityMin) { this.similarityMin = similarityMin; }
    public double getScorePerTag() { return scorePerTag; }
    public void setScorePerTag(double scorePerTag) { this.scorePerTag = scorePerTag; }
    public double getTagScoreMax() { return tagScoreMax; }
    public void setTagScoreMax(double tagScoreMax) { this.tagScoreMax = tagScoreMax; }
    public int getLlmCandidateLimit() { return llmCandidateLimit; }
    public void setLlmCandidateLimit(int llmCandidateLimit) { this.llmCandidateLimit = llmCandidateLimit; }
    public int getContentTruncateLength() { return contentTruncateLength; }
    public void setContentTruncateLength(int contentTruncateLength) { this.contentTruncateLength = contentTruncateLength; }
    public double getLlmTemperature() { return llmTemperature; }
    public void setLlmTemperature(double llmTemperature) { this.llmTemperature = llmTemperature; }
    public int getLlmMaxTokens() { return llmMaxTokens; }
    public void setLlmMaxTokens(int llmMaxTokens) { this.llmMaxTokens = llmMaxTokens; }
    public double getLlmDefaultScore() { return llmDefaultScore; }
    public void setLlmDefaultScore(double llmDefaultScore) { this.llmDefaultScore = llmDefaultScore; }
}