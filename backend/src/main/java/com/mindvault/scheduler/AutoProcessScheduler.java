package com.mindvault.scheduler;

import com.mindvault.relation.AggregationService;
import com.mindvault.relation.RelationService;
import com.mindvault.systemconfig.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AutoProcessScheduler {

    private static final Logger log = LoggerFactory.getLogger(AutoProcessScheduler.class);

    private final RelationService relationService;
    private final AggregationService aggregationService;
    private final SystemConfigService config;

    private volatile long lastRound2Run = 0;
    private volatile long lastRound3Run = 0;

    public AutoProcessScheduler(RelationService relationService,
                                AggregationService aggregationService,
                                SystemConfigService config) {
        this.relationService = relationService;
        this.aggregationService = aggregationService;
        this.config = config;
    }

    @Scheduled(fixedDelay = 30000)
    public void runRound2() {
        if (!config.getBool("task.auto-process.round2.enabled", true)) return;
        long pollMs = config.getLong("task.auto-process.round2.poll-ms", 300000);
        long now = System.currentTimeMillis();
        if (now - lastRound2Run < pollMs) return;
        lastRound2Run = now;
        relationService.processRound2();
    }

    @Scheduled(fixedDelay = 30000)
    public void runRound3() {
        if (!config.getBool("task.auto-process.round3.enabled", true)) return;
        long pollMs = config.getLong("task.auto-process.round3.poll-ms", 1800000);
        long now = System.currentTimeMillis();
        if (now - lastRound3Run < pollMs) return;
        lastRound3Run = now;
        aggregationService.processRound3();
    }
}
