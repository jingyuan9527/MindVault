package com.mindvault.dailyreview;

import com.mindvault.dailyreview.entity.DailyReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyReviewRepository extends JpaRepository<DailyReview, Long> {
    Optional<DailyReview> findByReportDate(LocalDate reportDate);

    @Query(value = "SELECT * FROM daily_review ORDER BY report_date DESC LIMIT :limit", nativeQuery = true)
    List<DailyReview> findTopByOrderByReportDateDesc(int limit);
}