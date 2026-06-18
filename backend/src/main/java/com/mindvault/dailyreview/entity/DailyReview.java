package com.mindvault.dailyreview.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_review")
public class DailyReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_date", nullable = false, unique = true)
    private LocalDate reportDate;

    @Column(name = "total_count", nullable = false)
    private Integer totalCount = 0;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "key_insights", columnDefinition = "TEXT")
    private String keyInsights = "[]";

    @Column(columnDefinition = "TEXT")
    private String recommendations = "[]";

    @Column(name = "category_breakdown", columnDefinition = "TEXT")
    private String categoryBreakdown = "{}";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

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
}