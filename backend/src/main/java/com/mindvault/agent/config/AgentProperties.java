package com.mindvault.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mindvault.agent")
public class AgentProperties {

    private double defaultTemperature = 0.7;
    private String errorMessage = "抱歉，处理您的消息时遇到了问题，请稍后重试。";

    public double getDefaultTemperature() { return defaultTemperature; }
    public void setDefaultTemperature(double defaultTemperature) { this.defaultTemperature = defaultTemperature; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}