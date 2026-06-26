package com.mindvault.auto.scheduler;

import com.mindvault.auto.config.AutoProcessProperties;
import com.mindvault.auto.r2.RelationService;
import com.mindvault.auto.r3.AggregationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * AI 自动处理调度器。
 * <p>
 * 核心职责：以固定频率触发 R2（关联发现）和 R3（聚合统计）自动处理阶段。
 * R2 默认每 5 分钟运行一次，R3 默认每 30 分钟运行一次。
 * 调度频率和开关由 {@link AutoProcessProperties} 强类型配置控制，变更需重启生效。
 * </p>
 * <p>
 * 关键设计：
 * <ul>
 *   <li>使用 {@code @Scheduled(fixedRateString)} 直接控制调度间隔，无手动节流</li>
 *   <li>通过 {@code @ConditionalOnProperty} 或属性 enabled 开关控制启用/禁用</li>
 *   <li>无 volatile 字段，无 SystemConfigService 依赖</li>
 * </ul>
 * </p>
 */
@Component
public class AutoProcessScheduler {

    private static final Logger log = LoggerFactory.getLogger(AutoProcessScheduler.class);

    private final RelationService relationService;
    private final AggregationService aggregationService;
    private final AutoProcessProperties properties;

    public AutoProcessScheduler(RelationService relationService,
                                AggregationService aggregationService,
                                AutoProcessProperties properties) {
        this.relationService = relationService;
        this.aggregationService = aggregationService;
        this.properties = properties;
    }

    /**
     * 定时任务：R2 关联发现。
     * 调度间隔由 {@code mindvault.auto-process.round2.fixed-delay-ms} 控制（默认 300000ms = 5 分钟）。
     */
    @Scheduled(fixedDelayString = "${mindvault.auto-process.round2.fixed-delay-ms:300000}")
    public void runRound2() {
        if (!properties.getRound2().isEnabled()) return;
        relationService.processRound2();
    }

    /**
     * 定时任务：R3 聚合统计。
     * 调度间隔由 {@code mindvault.auto-process.round3.fixed-delay-ms} 控制（默认 1800000ms = 30 分钟）。
     */
    @Scheduled(fixedDelayString = "${mindvault.auto-process.round3.fixed-delay-ms:1800000}")
    public void runRound3() {
        if (!properties.getRound3().isEnabled()) return;
        aggregationService.processRound3();
    }
}