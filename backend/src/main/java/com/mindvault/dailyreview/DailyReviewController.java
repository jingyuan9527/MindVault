package com.mindvault.dailyreview;

import com.mindvault.common.annotation.OperationLog;
import com.mindvault.common.dto.ApiResponse;
import com.mindvault.dailyreview.entity.DailyReview;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "每日回顾", description = "基于 LLM 的每日回顾报告生成")
@RestController
@RequestMapping("/api/v1/daily-review")
public class DailyReviewController {

    private final DailyReviewService dailyReviewService;

    public DailyReviewController(DailyReviewService dailyReviewService) {
        this.dailyReviewService = dailyReviewService;
    }

    @Operation(summary = "最新每日回顾", description = "获取最新生成的每日回顾报告，若今日尚无则自动生成")
    @GetMapping("/latest")
    public ApiResponse<DailyReview> getLatest() {
        return ApiResponse.success(dailyReviewService.getLatestOrGenerate());
    }

    @Operation(summary = "按日期查询", description = "获取指定日期的每日回顾报告")
    @GetMapping("/date/{date}")
    public ApiResponse<DailyReview> getByDate(@Parameter(description = "日期（yyyy-MM-dd）") @PathVariable String date) {
        LocalDate localDate = LocalDate.parse(date);
        return dailyReviewService.getReportByDate(localDate)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error(404, "该日期暂无复盘报告"));
    }

    @Operation(summary = "最近回顾列表", description = "获取最近 N 天的每日回顾报告")
    @GetMapping("/recent")
    public ApiResponse<List<DailyReview>> getRecent(@Parameter(description = "返回条数") @RequestParam(defaultValue = "7") int limit) {
        return ApiResponse.success(dailyReviewService.getRecentReports(limit));
    }

    @OperationLog(module = "每日回顾", action = "生成回顾报告", actionType = "CREATE")
    @Operation(summary = "生成回顾报告", description = "为指定日期或今日生成每日回顾报告")
    @PostMapping("/generate")
    public ApiResponse<DailyReview> generate(@Parameter(description = "日期（yyyy-MM-dd），为空则使用今日") @RequestParam(required = false) String date) {
        LocalDate target = date != null ? LocalDate.parse(date) : LocalDate.now();
        return ApiResponse.success(dailyReviewService.generateReport(target));
    }
}