package com.mindvault.auto.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 自动处理流水线配置属性。
 *
 * 将 application.yml 中 mindvault.auto-process.* 配置映射为类型安全的 Java Bean。
 * 控制 R1/R2/R3 的调度频率、开关和阈值参数。
 * 频率变更需重启生效（非运行时热更新）。
 */
@Component
@ConfigurationProperties(prefix = "mindvault.auto-process")
public class AutoProcessProperties {

    private Round2 round2 = new Round2();
    private Round3 round3 = new Round3();

    public Round2 getRound2() { return round2; }
    public void setRound2(Round2 round2) { this.round2 = round2; }
    public Round3 getRound3() { return round3; }
    public void setRound3(Round3 round3) { this.round3 = round3; }

    /** R2 关联发现调度配置 */
    public static class Round2 {
        private boolean enabled = true;
        private long fixedDelayMs = 300000;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public long getFixedDelayMs() { return fixedDelayMs; }
        public void setFixedDelayMs(long fixedDelayMs) { this.fixedDelayMs = fixedDelayMs; }
    }

    /** R3 聚合统计调度配置 */
    public static class Round3 {
        private boolean enabled = true;
        private long fixedDelayMs = 1800000;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public long getFixedDelayMs() { return fixedDelayMs; }
        public void setFixedDelayMs(long fixedDelayMs) { this.fixedDelayMs = fixedDelayMs; }
    }
}