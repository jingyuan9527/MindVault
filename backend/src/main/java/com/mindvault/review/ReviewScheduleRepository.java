package com.mindvault.review;

import com.mindvault.review.entity.ReviewSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewScheduleRepository extends JpaRepository<ReviewSchedule, Long> {

    Optional<ReviewSchedule> findByKnowledgeId(Long knowledgeId);

    List<ReviewSchedule> findByNextReviewAtBeforeOrderByNextReviewAtAsc(LocalDateTime time);

    @Query("SELECT COUNT(r) FROM ReviewSchedule r WHERE r.nextReviewAt <= :time")
    long countDueReviews(@org.springframework.data.repository.query.Param("time") LocalDateTime time);

    @Query("SELECT r FROM ReviewSchedule r WHERE r.nextReviewAt <= :time ORDER BY r.nextReviewAt ASC")
    List<ReviewSchedule> findDueReviews(@org.springframework.data.repository.query.Param("time") LocalDateTime time);
}