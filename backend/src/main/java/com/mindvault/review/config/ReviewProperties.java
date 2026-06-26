package com.mindvault.review.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mindvault.review")
public class ReviewProperties {

    private int initialIntervalDays = 1;
    private int qualityMin = 0;
    private int qualityMax = 5;
    private int failedIntervalDays = 1;
    private double easeFactorPenalty = 0.20;
    private int firstSuccessInterval = 1;
    private int secondSuccessInterval = 6;
    private double easeFactorAdjustment = 0.10;
    private double minEaseFactor = 1.30;

    public int getInitialIntervalDays() { return initialIntervalDays; }
    public void setInitialIntervalDays(int initialIntervalDays) { this.initialIntervalDays = initialIntervalDays; }
    public int getQualityMin() { return qualityMin; }
    public void setQualityMin(int qualityMin) { this.qualityMin = qualityMin; }
    public int getQualityMax() { return qualityMax; }
    public void setQualityMax(int qualityMax) { this.qualityMax = qualityMax; }
    public int getFailedIntervalDays() { return failedIntervalDays; }
    public void setFailedIntervalDays(int failedIntervalDays) { this.failedIntervalDays = failedIntervalDays; }
    public double getEaseFactorPenalty() { return easeFactorPenalty; }
    public void setEaseFactorPenalty(double easeFactorPenalty) { this.easeFactorPenalty = easeFactorPenalty; }
    public int getFirstSuccessInterval() { return firstSuccessInterval; }
    public void setFirstSuccessInterval(int firstSuccessInterval) { this.firstSuccessInterval = firstSuccessInterval; }
    public int getSecondSuccessInterval() { return secondSuccessInterval; }
    public void setSecondSuccessInterval(int secondSuccessInterval) { this.secondSuccessInterval = secondSuccessInterval; }
    public double getEaseFactorAdjustment() { return easeFactorAdjustment; }
    public void setEaseFactorAdjustment(double easeFactorAdjustment) { this.easeFactorAdjustment = easeFactorAdjustment; }
    public double getMinEaseFactor() { return minEaseFactor; }
    public void setMinEaseFactor(double minEaseFactor) { this.minEaseFactor = minEaseFactor; }
}