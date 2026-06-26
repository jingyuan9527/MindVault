package com.mindvault.knowledge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mindvault.association")
public class AssociationProperties {

    private boolean enabled = true;
    private int topN = 6;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public int getTopN() { return topN; }
    public void setTopN(int topN) { this.topN = topN; }
}