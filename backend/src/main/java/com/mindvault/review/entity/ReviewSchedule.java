package com.mindvault.review.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@TableName("review_schedule")
@Schema(description = "复习调度实体（SM-2 算法）")
public class ReviewSchedule {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键 ID")
    private Long id;

    @TableField("knowledge_id")
    @Schema(description = "知识 ID")
    private Long knowledgeId;

    @TableField("ease_factor")
    @Schema(description = "简易系数")
    private BigDecimal easeFactor = new BigDecimal("2.50");

    @TableField("interval_days")
    @Schema(description = "间隔天数")
    private Integer intervalDays = 0;

    @TableField("review_count")
    @Schema(description = "复习次数")
    private Integer reviewCount = 0;

    @TableField("next_review_at")
    @Schema(description = "下次复习时间")
    private LocalDateTime nextReviewAt = LocalDateTime.now();

    @TableField("last_review_at")
    @Schema(description = "上次复习时间")
    private LocalDateTime lastReviewAt;

    @TableField("created_at")
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getKnowledgeId() { return knowledgeId; }
    public void setKnowledgeId(Long knowledgeId) { this.knowledgeId = knowledgeId; }
    public BigDecimal getEaseFactor() { return easeFactor; }
    public void setEaseFactor(BigDecimal easeFactor) { this.easeFactor = easeFactor; }
    public Integer getIntervalDays() { return intervalDays; }
    public void setIntervalDays(Integer intervalDays) { this.intervalDays = intervalDays; }
    public Integer getReviewCount() { return reviewCount; }
    public void setReviewCount(Integer reviewCount) { this.reviewCount = reviewCount; }
    public LocalDateTime getNextReviewAt() { return nextReviewAt; }
    public void setNextReviewAt(LocalDateTime nextReviewAt) { this.nextReviewAt = nextReviewAt; }
    public LocalDateTime getLastReviewAt() { return lastReviewAt; }
    public void setLastReviewAt(LocalDateTime lastReviewAt) { this.lastReviewAt = lastReviewAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}