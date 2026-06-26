package com.mindvault.dailyreview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 每日回顾报告实体。
 * <p>
 * 由 DailyReviewService 在每日凌晨自动生成，通过 LLM 对当日新增知识进行摘要、
 * 关键洞察提取、后续建议生成和分类统计。报告内容以 JSON 格式存储在 summary、
 * keyInsights、recommendations、categoryBreakdown 四个字段中，由前端解析展示。
 * </p>
 * <p>依赖: MyBatis-Plus 映射到 daily_review 表</p>
 */
@TableName("daily_review")
@Schema(description = "每日回顾实体")
public class DailyReview {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键 ID")
    private Long id;

    @TableField("report_date")
    @Schema(description = "报告日期")
    private LocalDate reportDate;

    @TableField("total_count")
    @Schema(description = "总计数")
    private Integer totalCount = 0;

    @TableField("summary")
    @Schema(description = "摘要")
    private String summary;

    @TableField("key_insights")
    @Schema(description = "关键洞察")
    private String keyInsights = "[]";

    @TableField("recommendations")
    @Schema(description = "建议")
    private String recommendations = "[]";

    @TableField("category_breakdown")
    @Schema(description = "分类统计")
    private String categoryBreakdown = "{}";

    @TableField("created_at")
    @Schema(description = "创建时间")
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