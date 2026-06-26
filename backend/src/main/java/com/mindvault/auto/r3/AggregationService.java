package com.mindvault.auto.r3;

/**
 * 聚合统计服务（R3 阶段）
 *
 * 对完成 R2 处理（RELATION_DONE 状态）的知识条目执行最终处理：
 * 1. 标记状态为 COMPLETED（自动处理流水线结束）
 * 2. 重建标签云（统计所有标签使用频率）
 *
 * 调度方式：由 AutoProcessScheduler 每 30 分钟触发。
 */
public interface AggregationService {

    /** R3 入口：批量完成 RELATION_DONE → COMPLETED，重建标签云 */
    void processRound3();

    /** 重建标签云：聚合所有知识的 ai_tags + user_tags，统计使用频率 */
    void rebuildTagCloud();
}