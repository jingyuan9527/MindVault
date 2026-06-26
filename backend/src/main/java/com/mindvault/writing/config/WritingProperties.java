package com.mindvault.writing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mindvault.writing")
public class WritingProperties {

    private String noModelMessage = "系统未配置可用模型，请先在设置中添加并启用模型。";
    private int contentTruncateLength = 500;
    private String style = "写作风格: 清晰、条理、专业";
    private double temperature = 0.7;
    private int maxTokens = 4096;
    private String fallbackMessage = "文章生成失败，请稍后重试。";
    private int minTermLength = 1;
    private int maxRelatedResults = 5;

    public String getNoModelMessage() { return noModelMessage; }
    public void setNoModelMessage(String noModelMessage) { this.noModelMessage = noModelMessage; }
    public int getContentTruncateLength() { return contentTruncateLength; }
    public void setContentTruncateLength(int contentTruncateLength) { this.contentTruncateLength = contentTruncateLength; }
    public String getStyle() { return style; }
    public void setStyle(String style) { this.style = style; }
    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
    public String getFallbackMessage() { return fallbackMessage; }
    public void setFallbackMessage(String fallbackMessage) { this.fallbackMessage = fallbackMessage; }
    public int getMinTermLength() { return minTermLength; }
    public void setMinTermLength(int minTermLength) { this.minTermLength = minTermLength; }
    public int getMaxRelatedResults() { return maxRelatedResults; }
    public void setMaxRelatedResults(int maxRelatedResults) { this.maxRelatedResults = maxRelatedResults; }
}