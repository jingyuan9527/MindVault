package com.mindvault.tokenusage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mindvault.token-usage")
public class TokenUsageProperties {

    private boolean taskEnabled = true;
    private double calcDivisor = 1000.0;

    public boolean isTaskEnabled() { return taskEnabled; }
    public void setTaskEnabled(boolean taskEnabled) { this.taskEnabled = taskEnabled; }
    public double getCalcDivisor() { return calcDivisor; }
    public void setCalcDivisor(double calcDivisor) { this.calcDivisor = calcDivisor; }
}