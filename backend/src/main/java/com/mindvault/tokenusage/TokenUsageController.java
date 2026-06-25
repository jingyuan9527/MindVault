package com.mindvault.tokenusage;

import com.mindvault.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;

@Tag(name = "Token 用量", description = "Token 消耗记录查询")
@RestController
@RequestMapping("/api/v1/token-usage")
public class TokenUsageController {

    private final TokenUsageService tokenUsageService;
    private LocalDate defaultEnd;

    public TokenUsageController(TokenUsageService tokenUsageService) {
        this.tokenUsageService = tokenUsageService;
    }

    @PostConstruct
    public void init() {
        this.defaultEnd = LocalDate.now().plusDays(1);
    }

    @Operation(summary = "每日用量统计", description = "获取最近 N 天的 Token 消耗统计")
    @GetMapping("/daily")
    public ApiResponse<?> getDailySummary(@Parameter(description = "天数范围") @RequestParam(defaultValue = "30") int days) {
        return ApiResponse.success(tokenUsageService.getDailySummary(days));
    }

    @Operation(summary = "按来源统计", description = "获取最近 N 天的 Token 消耗来源分布统计")
    @GetMapping("/by-source")
    public ApiResponse<?> getBySourceSummary(@Parameter(description = "天数范围") @RequestParam(defaultValue = "30") int days) {
        return ApiResponse.success(tokenUsageService.getBySourceSummary(days));
    }

    @Operation(summary = "总计用量统计", description = "获取指定时间范围内的 Token 消耗汇总统计")
    @GetMapping("/total")
    public ApiResponse<?> getTotalStats(
            @Parameter(description = "开始日期（yyyy-MM-dd）") @RequestParam(required = false) String start,
            @Parameter(description = "结束日期（yyyy-MM-dd）") @RequestParam(required = false) String end) {
        LocalDate startDate = start != null ? LocalDate.parse(start) : LocalDate.now().minusDays(30);
        LocalDate endDate = end != null ? LocalDate.parse(end) : defaultEnd;
        return ApiResponse.success(tokenUsageService.getTotalStats(startDate, endDate));
    }
}