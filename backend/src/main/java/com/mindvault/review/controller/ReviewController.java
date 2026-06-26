package com.mindvault.review.controller;

import com.mindvault.common.annotation.OperationLog;
import com.mindvault.common.dto.ApiResponse;
import com.mindvault.review.service.ReviewService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "复习管理", description = "SM-2 间隔重复复习调度")
@RestController
@RequestMapping("/api/v1/review")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @Operation(summary = "待复习列表", description = "获取到期待复习的知识列表")
    @GetMapping("/due")
    public ApiResponse<List<Map<String, Object>>> getDueReviews(
            @Parameter(description = "返回条数") @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.success(reviewService.getDueReviews(limit));
    }

    @Operation(summary = "待复习数量", description = "获取待复习的知识总数")
    @GetMapping("/due-count")
    public ApiResponse<Map<String, Long>> getDueCount() {
        return ApiResponse.success(Map.of("count", reviewService.getDueReviewCount()));
    }

    @OperationLog(module = "复习", action = "安排复习", actionType = "CREATE")
    @Operation(summary = "安排复习", description = "为指定知识创建或更新复习计划")
    @PostMapping("/{knowledgeId}/schedule")
    public ApiResponse<?> scheduleReview(@Parameter(description = "知识 ID") @PathVariable Long knowledgeId) {
        return ApiResponse.success(Map.of("nextReviewAt",
                reviewService.scheduleReview(knowledgeId).getNextReviewAt()));
    }

    @OperationLog(module = "复习", action = "执行复习", actionType = "UPDATE")
    @Operation(summary = "执行复习", description = "提交复习反馈（质量评分），更新下次复习时间")
    @PostMapping("/{knowledgeId}/perform")
    public ApiResponse<?> performReview(
            @Parameter(description = "知识 ID") @PathVariable Long knowledgeId,
            @RequestBody Map<String, Integer> body) {
        int quality = body.getOrDefault("quality", 3);
        return ApiResponse.success(Map.of("nextReviewAt",
                reviewService.performReview(knowledgeId, quality).getNextReviewAt()));
    }
}