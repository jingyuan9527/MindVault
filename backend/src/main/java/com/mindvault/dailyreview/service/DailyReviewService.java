package com.mindvault.dailyreview.service;

import com.mindvault.dailyreview.entity.DailyReview;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 每日回顾报告服务接口。
 * <p>提供每日回顾报告的定时生成、按日期查询、获取最新报告等功能。</p>
 */
public interface DailyReviewService {

    void scheduledDailyReview();

    DailyReview generateReport(LocalDate date);

    Optional<DailyReview> getReportByDate(LocalDate date);

    List<DailyReview> getRecentReports(int limit);

    DailyReview getLatestOrGenerate();
}