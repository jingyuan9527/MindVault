package com.mindvault.review;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mindvault.review.entity.ReviewSchedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 复习调度数据访问层。
 * <p>继承 MyBatis-Plus BaseMapper 提供标准 CRUD。
 * 提供根据知识 ID 查询、到期复习列表和计数等自定义查询。
 * 到期条件使用 next_review_at <= 当前时间判定。</p>
 */
@Mapper
public interface ReviewScheduleMapper extends BaseMapper<ReviewSchedule> {

    /**
     * 根据知识 ID 查询复习计划。
     * @param knowledgeId 知识 ID
     * @return 复习计划（Optional）
     */
    @Select("SELECT * FROM review_schedule WHERE knowledge_id = #{knowledgeId} LIMIT 1")
    Optional<ReviewSchedule> findByKnowledgeId(@Param("knowledgeId") Long knowledgeId);

    /**
     * 查询指定时间之前到期未复习的计划。
     * @param time 截止时间
     * @return 到期复习计划列表
     */
    @Select("SELECT * FROM review_schedule WHERE next_review_at < #{time} ORDER BY next_review_at ASC")
    List<ReviewSchedule> findByNextReviewAtBeforeOrderByNextReviewAtAsc(@Param("time") LocalDateTime time);

    /**
     * 统计当前到期未复习的知识数量。
     * @param time 当前时间
     * @return 到期数量
     */
    @Select("SELECT COUNT(*) FROM review_schedule WHERE next_review_at <= #{time}")
    long countDueReviews(@Param("time") LocalDateTime time);

    /**
     * 查询当前到期未复习的计划列表。
     * @param time 当前时间
     * @return 到期复习计划列表
     */
    @Select("SELECT * FROM review_schedule WHERE next_review_at <= #{time} ORDER BY next_review_at ASC")
    List<ReviewSchedule> findDueReviews(@Param("time") LocalDateTime time);
}