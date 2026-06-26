package com.mindvault.flashcard.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 闪卡实体。
 * <p>映射 flash_card 表，表示一个问答对形式的记忆卡片，用于快速复习。
 * 关联知识库中的某条知识（knowledge_id），包含问题（question）和答案（answer）。
 * difficulty 表示难度（EASY / MEDIUM / HARD），source_type 表示来源（AUTO 表示 LLM 自动生成、MANUAL 表示手动创建）。
 * 闪卡可通过 FlashCardService.generateCards() 基于知识内容自动生成。</p>
 */
@TableName("flash_card")
@Schema(description = "闪卡实体")
public class FlashCard {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键 ID")
    private Long id;

    @TableField("knowledge_id")
    @Schema(description = "知识 ID")
    private Long knowledgeId;

    @TableField("question")
    @Schema(description = "问题")
    private String question;

    @TableField("answer")
    @Schema(description = "答案")
    private String answer;

    @TableField("difficulty")
    @Schema(description = "难度")
    private String difficulty = "MEDIUM";

    @TableField("source_type")
    @Schema(description = "来源类型")
    private String sourceType = "AUTO";

    @TableField("created_at")
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getKnowledgeId() { return knowledgeId; }
    public void setKnowledgeId(Long knowledgeId) { this.knowledgeId = knowledgeId; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}