package com.mindvault.tokenusage;

import com.mindvault.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/token-usage")
public class TokenUsageController {

    private final TokenUsageService tokenUsageService;

    public TokenUsageController(TokenUsageService tokenUsageService) {
        this.tokenUsageService = tokenUsageService;
    }

    @GetMapping("/daily")
    public ApiResponse<?> getDailySummary(@RequestParam(defaultValue = "30") int days) {
        return ApiResponse.success(tokenUsageService.getDailySummary(days));
    }

    @GetMapping("/total")
    public ApiResponse<?> getTotalStats(
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().minusDays(30).toString()}") String start,
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().plusDays(1).toString()}") String end) {
        return ApiResponse.success(tokenUsageService.getTotalStats(
                LocalDate.parse(start), LocalDate.parse(end)));
    }
}