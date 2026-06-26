package com.mindvault.review.service;

import com.mindvault.review.entity.ReviewSchedule;

import java.util.List;
import java.util.Map;

public interface ReviewService {

    ReviewSchedule scheduleReview(Long knowledgeId);

    ReviewSchedule performReview(Long knowledgeId, int quality);

    List<Map<String, Object>> getDueReviews(int limit);

    long getDueReviewCount();
}