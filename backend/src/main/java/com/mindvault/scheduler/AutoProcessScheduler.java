package com.mindvault.scheduler;

import com.mindvault.relation.AggregationService;
import com.mindvault.relation.RelationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AutoProcessScheduler {

    private static final Logger log = LoggerFactory.getLogger(AutoProcessScheduler.class);

    private final RelationService relationService;
    private final AggregationService aggregationService;

    public AutoProcessScheduler(RelationService relationService,
                                AggregationService aggregationService) {
        this.relationService = relationService;
        this.aggregationService = aggregationService;
    }

    @Scheduled(fixedDelay = 300000)
    public void runRound2() {
        log.debug("R2 定时任务触发");
        relationService.processRound2();
    }

    @Scheduled(fixedDelay = 1800000)
    public void runRound3() {
        log.debug("R3 定时任务触发");
        aggregationService.processRound3();
    }
}
