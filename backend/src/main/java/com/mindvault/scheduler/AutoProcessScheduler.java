package com.mindvault.scheduler;

import com.mindvault.relation.AggregationService;
import com.mindvault.relation.RelationService;
import com.mindvault.systemconfig.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * AI 自动处理调度器。
 * <p>
 * 核心职责: 以固定频率（30 秒）轮询并触发 R2（关联发现）和 R3（聚合统计）自动处理阶段。
 * R2 和 R3 的轮询间隔分别由 system_config 中的 poll-ms 参数控制，可通过开关启用/禁用。
 * </p>
 * <p>
 * 关键设计:
 * <ul>
 *   <li>使用两个带状态标记的定时方法（runRound2/runRound3），每 30 秒检查一次是否需要执行</li>
 *   <li>实际执行间隔由各自 poll-ms 配置决定（R2 默认 5 分钟，R3 默认 30 分钟）</li>
 *   <li>通过 volatile 变量记录上次执行时间，实现简单的节流控制，避免多个实例重复执行</li>
 * </ul>
 * </p>
 * <p>依赖: RelationService (R2), AggregationService (R3), SystemConfigService</p>
 */
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

    /**
     * 定时任务: 每 30 秒检查一次是否需要执行 R2 关联发现。
     * 实际执行间隔由 task.auto-process.round2.poll-ms 控制（默认 300000ms = 5 分钟）。
     */
    @Scheduled(fixedDelay = 30000)
    public void runRound2() {
        if (!config.getBool("task.auto-process.round2.enabled", true)) return;
        long pollMs = config.getLong("task.auto-process.round2.poll-ms", 300000);
        long now = System.currentTimeMillis();
        if (now - lastRound2Run < pollMs) return;
        lastRound2Run = now;
        relationService.processRound2();
    }

    /**
     * 定时任务: 每 30 秒检查一次是否需要执行 R3 聚合统计。
     * 实际执行间隔由 task.auto-process.round3.poll-ms 控制（默认 1800000ms = 30 分钟）。
     */
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
