package com.mindvault.review;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mindvault.review.entity.ReviewSchedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Mapper
public interface ReviewScheduleMapper extends BaseMapper<ReviewSchedule> {

    @Select("SELECT * FROM review_schedule WHERE knowledge_id = #{knowledgeId} LIMIT 1")
    Optional<ReviewSchedule> findByKnowledgeId(@Param("knowledgeId") Long knowledgeId);

    @Select("SELECT * FROM review_schedule WHERE next_review_at < #{time} ORDER BY next_review_at ASC")
    List<ReviewSchedule> findByNextReviewAtBeforeOrderByNextReviewAtAsc(@Param("time") LocalDateTime time);

    @Select("SELECT COUNT(*) FROM review_schedule WHERE next_review_at <= #{time}")
    long countDueReviews(@Param("time") LocalDateTime time);

    @Select("SELECT * FROM review_schedule WHERE next_review_at <= #{time} ORDER BY next_review_at ASC")
    List<ReviewSchedule> findDueReviews(@Param("time") LocalDateTime time);
}