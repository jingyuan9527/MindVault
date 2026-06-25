package com.mindvault.tokenusage;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mindvault.tokenusage.entity.TokenUsage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface TokenUsageMapper extends BaseMapper<TokenUsage> {

    @Select("SELECT * FROM token_usage WHERE created_at >= #{start} AND created_at < #{end} ORDER BY created_at DESC")
    List<TokenUsage> findByCreatedAtBetweenOrderByCreatedAtDesc(@Param("start") LocalDate start, @Param("end") LocalDate end);

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

    @Select(value = """
            SELECT COALESCE(SUM(total_tokens), 0) AS total_tokens, COALESCE(SUM(cost), 0) AS total_cost
            FROM token_usage
            WHERE created_at >= #{start} AND created_at < #{end}
            """)
    Map<String, Object> findTotalTokensAndCost(@Param("start") LocalDate start, @Param("end") LocalDate end);

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