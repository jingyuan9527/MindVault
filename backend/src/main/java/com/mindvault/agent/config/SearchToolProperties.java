package com.mindvault.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mindvault.search-tool")
public class SearchToolProperties {

    private int defaultLimit = 3;
    private int maxLimit = 10;
    private String searchMethod = "rewrite";

    public int getDefaultLimit() { return defaultLimit; }
    public void setDefaultLimit(int defaultLimit) { this.defaultLimit = defaultLimit; }
    public int getMaxLimit() { return maxLimit; }
    public void setMaxLimit(int maxLimit) { this.maxLimit = maxLimit; }
    public String getSearchMethod() { return searchMethod; }
    public void setSearchMethod(String searchMethod) { this.searchMethod = searchMethod; }
}