package com.mindvault.flashcard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mindvault.flashcard")
public class FlashCardProperties {

    private int truncateLength = 3000;
    private double temperature = 0.3;
    private int maxTokens = 1000;

    public int getTruncateLength() { return truncateLength; }
    public void setTruncateLength(int truncateLength) { this.truncateLength = truncateLength; }
    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
}