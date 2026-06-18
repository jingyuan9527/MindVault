package com.mindvault.dailyreview;

import com.mindvault.common.dto.ApiResponse;
import com.mindvault.dailyreview.entity.DailyReview;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/daily-review")
public class DailyReviewController {

    private final DailyReviewService dailyReviewService;

    public DailyReviewController(DailyReviewService dailyReviewService) {
        this.dailyReviewService = dailyReviewService;
    }

    @GetMapping("/latest")
    public ApiResponse<DailyReview> getLatest() {
        return ApiResponse.success(dailyReviewService.getLatestOrGenerate());
    }

    @GetMapping("/date/{date}")
    public ApiResponse<DailyReview> getByDate(@PathVariable String date) {
        LocalDate localDate = LocalDate.parse(date);
        return dailyReviewService.getReportByDate(localDate)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error(404, "该日期暂无复盘报告"));
    }

    @GetMapping("/recent")
    public ApiResponse<List<DailyReview>> getRecent(@RequestParam(defaultValue = "7") int limit) {
        return ApiResponse.success(dailyReviewService.getRecentReports(limit));
    }

    @PostMapping("/generate")
    public ApiResponse<DailyReview> generate(@RequestParam(required = false) String date) {
        LocalDate target = date != null ? LocalDate.parse(date) : LocalDate.now();
        return ApiResponse.success(dailyReviewService.generateReport(target));
    }
}