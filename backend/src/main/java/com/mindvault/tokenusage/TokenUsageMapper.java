package com.mindvault.tokenusage;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mindvault.tokenusage.entity.TokenUsage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Token 用量 Mapper 接口。
 * <p>
 * 提供时间范围查询、每日汇总统计（按提供商/模型分组）、来源分布统计和总计统计。
 * 所有统计查询使用聚合 SQL 在数据库层完成，减少内存计算。
 * </p>
 */
@Mapper
public interface TokenUsageMapper extends BaseMapper<TokenUsage> {

    /**
     * 查询指定时间范围内的 Token 用量记录。
     * @param start 开始日期（含）
     * @param end   结束日期（不含）
     * @return 用量记录列表，按创建时间降序
     */
    @Select("SELECT * FROM token_usage WHERE created_at >= #{start} AND created_at < #{end} ORDER BY created_at DESC")
    List<TokenUsage> findByCreatedAtBetweenOrderByCreatedAtDesc(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /**
     * 获取最近 N 天的每日用量汇总。
     * 按日期、提供商、模型名称分组汇总，含请求次数统计。
     * @param limit 天数范围
     * @return 每日汇总行，每行包含 date/provider/modelName/totalTokens/cost/requestCount 等
     */
    @Select(value = """
            SELECT DATE(created_at) AS "date", provider, model_name, model_type,
                   COALESCE(SUM(prompt_tokens), 0) AS prompt_tokens,
                   COALESCE(SUM(completion_tokens), 0) AS completion_tokens,
                   COALESCE(SUM(total_tokens), 0) AS total_tokens,
                   COALESCE(SUM(cost), 0) AS cost,
                   COUNT(*) AS request_count
            FROM token_usage
            WHERE created_at >= CURRENT_DATE - CAST(#{limit} AS INTEGER)
            GROUP BY DATE(created_at), provider, model_name, model_type
            ORDER BY "date" DESC
            """)
    List<Map<String, Object>> findDailySummary(@Param("limit") int limit);

    /**
     * 获取指定时间范围内的总计 Token 和费用。
     * @param start 开始日期（含）
     * @param end   结束日期（不含）
     * @return 包含 total_tokens 和 total_cost 的 Map
     */
    @Select(value = """
            SELECT COALESCE(SUM(total_tokens), 0) AS total_tokens, COALESCE(SUM(cost), 0) AS total_cost
            FROM token_usage
            WHERE created_at >= #{start} AND created_at < #{end}
            """)
    Map<String, Object> findTotalTokensAndCost(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /**
     * 获取最近 N 天按请求来源分组的用量统计。
     * @param limit 天数范围
     * @return 来源统计列表，按 total_tokens 降序排列
     */
    @Select(value = """
            SELECT request_source,
                   COALESCE(SUM(prompt_tokens), 0) AS prompt_tokens,
                   COALESCE(SUM(completion_tokens), 0) AS completion_tokens,
                   COALESCE(SUM(total_tokens), 0) AS total_tokens,
                   COALESCE(SUM(cost), 0) AS cost,
                   COUNT(*) AS request_count
            FROM token_usage
            WHERE created_at >= CURRENT_DATE - CAST(#{limit} AS INTEGER)
            GROUP BY request_source
            ORDER BY total_tokens DESC
            """)
    List<Map<String, Object>> findBySourceSummary(@Param("limit") int limit);
}