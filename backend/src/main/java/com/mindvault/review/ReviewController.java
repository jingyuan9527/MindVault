package com.mindvault.review;

import com.mindvault.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/review")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/due")
    public ApiResponse<List<Map<String, Object>>> getDueReviews(
            @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.success(reviewService.getDueReviews(limit));
    }

    @GetMapping("/due-count")
    public ApiResponse<Map<String, Long>> getDueCount() {
        return ApiResponse.success(Map.of("count", reviewService.getDueReviewCount()));
    }

    @PostMapping("/{knowledgeId}/schedule")
    public ApiResponse<?> scheduleReview(@PathVariable Long knowledgeId) {
        return ApiResponse.success(Map.of("nextReviewAt",
                reviewService.scheduleReview(knowledgeId).getNextReviewAt()));
    }

    @PostMapping("/{knowledgeId}/perform")
    public ApiResponse<?> performReview(
            @PathVariable Long knowledgeId,
            @RequestBody Map<String, Integer> body) {
        int quality = body.getOrDefault("quality", 3);
        return ApiResponse.success(Map.of("nextReviewAt",
                reviewService.performReview(knowledgeId, quality).getNextReviewAt()));
    }
}