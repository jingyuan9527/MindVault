package com.mindvault.dailyreview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("daily_review")
public class DailyReview {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("report_date")
    private LocalDate reportDate;

    @TableField("total_count")
    private Integer totalCount = 0;

    @TableField("summary")
    private String summary;

    @TableField("key_insights")
    private String keyInsights = "[]";

    @TableField("recommendations")
    private String recommendations = "[]";

    @TableField("category_breakdown")
    private String categoryBreakdown = "{}";

    @TableField("created_at")
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getReportDate() { return reportDate; }
    public void setReportDate(LocalDate reportDate) { this.reportDate = reportDate; }
    public Integer getTotalCount() { return totalCount; }
    public void setTotalCount(Integer totalCount) { this.totalCount = totalCount; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getKeyInsights() { return keyInsights; }
    public void setKeyInsights(String keyInsights) { this.keyInsights = keyInsights; }
    public String getRecommendations() { return recommendations; }
    public void setRecommendations(String recommendations) { this.recommendations = recommendations; }
    public String getCategoryBreakdown() { return categoryBreakdown; }
    public void setCategoryBreakdown(String categoryBreakdown) { this.categoryBreakdown = categoryBreakdown; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}