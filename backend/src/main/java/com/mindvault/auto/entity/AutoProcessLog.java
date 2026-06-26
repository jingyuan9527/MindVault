package com.mindvault.auto.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 自动处理日志实体
 *
 * 记录每条知识在 R1/R2/R3 各阶段的处理结果。
 * 每条知识在每个阶段可能有多个日志条目（如首次处理 + 重新处理）。
 *
 * 轮次说明：
 * - R1_TITLE_TAG:   AI 标题 + 标签 + 摘要 + 嵌入向量
 * - R2_RELATION:    关联发现（语义 + 标签 + LLM）
 * - R3_AGGREGATION: 聚合统计 + 标签云重建
 */
@TableName("auto_process_log")
@Schema(description = "自动处理日志")
public class AutoProcessLog {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键 ID")
    private Long id;

    @TableField("knowledge_id")
    @Schema(description = "知识 ID")
    private Long knowledgeId;

    @TableField("round")
    @Schema(description = "处理轮次：R1_TITLE_TAG/R2_RELATION/R3_AGGREGATION")
    private String round;

    @Schema(description = "状态：SUCCESS/FAILED")
    private String status;

    @TableField("result_summary")
    @Schema(description = "处理结果摘要")
    private String resultSummary;

    @TableField("llm_tokens")
    @Schema(description = "LLM 调用 token 数")
    private Integer llmTokens = 0;

    @TableField("llm_duration_ms")
    @Schema(description = "LLM 调用耗时 ms")
    private Integer llmDurationMs = 0;

    @TableField("error_message")
    @Schema(description = "错误信息")
    private String errorMessage;

    @TableField("started_at")
    private LocalDateTime startedAt;

    @TableField("completed_at")
    private LocalDateTime completedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getKnowledgeId() { return knowledgeId; }
    public void setKnowledgeId(Long knowledgeId) { this.knowledgeId = knowledgeId; }
    public String getRound() { return round; }
    public void setRound(String round) { this.round = round; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getResultSummary() { return resultSummary; }
    public void setResultSummary(String resultSummary) { this.resultSummary = resultSummary; }
    public Integer getLlmTokens() { return llmTokens; }
    public void setLlmTokens(Integer llmTokens) { this.llmTokens = llmTokens; }
    public Integer getLlmDurationMs() { return llmDurationMs; }
    public void setLlmDurationMs(Integer llmDurationMs) { this.llmDurationMs = llmDurationMs; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
