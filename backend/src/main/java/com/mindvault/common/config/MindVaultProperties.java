package com.mindvault.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "mindvault")
public class MindVaultProperties {

    private Session session = new Session();
    private Jsoup jsoup = new Jsoup();
    private Pricing pricing = new Pricing();

    public Session getSession() { return session; }
    public void setSession(Session session) { this.session = session; }
    public Jsoup getJsoup() { return jsoup; }
    public void setJsoup(Jsoup jsoup) { this.jsoup = jsoup; }
    public Pricing getPricing() { return pricing; }
    public void setPricing(Pricing pricing) { this.pricing = pricing; }

    public static class Session {
        private int ttlHours = 24;
        public int getTtlHours() { return ttlHours; }
        public void setTtlHours(int ttlHours) { this.ttlHours = ttlHours; }
    }

    public static class Jsoup {
        private int timeoutMs = 15000;
        public int getTimeoutMs() { return timeoutMs; }
        public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
    }

    public static class Pricing {
        private Map<String, Map<String, BigDecimal[]>> models = new LinkedHashMap<>();
        public Map<String, Map<String, BigDecimal[]>> getModels() { return models; }
        public void setModels(Map<String, Map<String, BigDecimal[]>> models) { this.models = models; }
    }
}
