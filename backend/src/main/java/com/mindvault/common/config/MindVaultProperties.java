package com.mindvault.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 应用自定义配置映射
 *
 * 将 application.yml 中 mindvault.* 配置映射为类型安全的 Java Bean。
 * 包含三个配置组：
 * - session:   Session 过期时间配置
 * - jsoup:     网页解析超时配置
 * - pricing:   各模型供应商的输入/输出 Token 单价（用于 Token 用量计费）
 */
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

    /** Session 会话配置 */
    public static class Session {
        private int ttlHours = 24;
        public int getTtlHours() { return ttlHours; }
        public void setTtlHours(int ttlHours) { this.ttlHours = ttlHours; }
    }

    /** Jsoup 网页解析配置 */
    public static class Jsoup {
        private int timeoutMs = 15000;
        public int getTimeoutMs() { return timeoutMs; }
        public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
    }

    /** Token 定价配置：map[供应商][模型名] = [输入单价, 输出单价] */
    public static class Pricing {
        private Map<String, Map<String, BigDecimal[]>> models = new LinkedHashMap<>();
        public Map<String, Map<String, BigDecimal[]>> getModels() { return models; }
        public void setModels(Map<String, Map<String, BigDecimal[]>> models) { this.models = models; }
    }
}
