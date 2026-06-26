package com.mindvault.knowledge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mindvault.knowledge")
public class KnowledgeProperties {

    private String autoProcessStatus = "PENDING";
    private String tagsEmptyJson = "[]";

    public String getAutoProcessStatus() { return autoProcessStatus; }
    public void setAutoProcessStatus(String autoProcessStatus) { this.autoProcessStatus = autoProcessStatus; }
    public String getTagsEmptyJson() { return tagsEmptyJson; }
    public void setTagsEmptyJson(String tagsEmptyJson) { this.tagsEmptyJson = tagsEmptyJson; }
}