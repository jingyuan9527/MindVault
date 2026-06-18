package com.mindvault.tokenusage;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mindvault.tokenusage.entity.TokenUsage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface TokenUsageMapper extends BaseMapper<TokenUsage> {

    @Select("SELECT * FROM token_usage WHERE created_at >= #{start} AND created_at < #{end} ORDER BY created_at DESC")
    List<TokenUsage> findByCreatedAtBetweenOrderByCreatedAtDesc(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Select(value = """
            SELECT usage_date, provider, model_name, model_type,
                   total_prompt_tokens, total_completion_tokens, total_tokens,
                   total_cost, request_count
            FROM token_usage_daily
            ORDER BY usage_date DESC
            LIMIT #{limit}
            """)
    List<Object[]> findDailySummary(@Param("limit") int limit);

    @Select(value = """
            SELECT COALESCE(SUM(total_tokens), 0), COALESCE(SUM(cost), 0)
            FROM token_usage
            WHERE created_at >= #{start} AND created_at < #{end}
            """)
    Object[] findTotalTokensAndCost(@Param("start") LocalDate start, @Param("end") LocalDate end);
}