package com.mindvault.auto.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 知识关联实体
 *
 * 记录两条知识间的关联关系，每条关联有方向性（knowledge_id → related_id）。
 * 关联来源（source）标识是哪种策略发现的，关联类型（relationType）描述关系语义。
 *
 * 关联类型：
 * - COMPLEMENT: 内容互补
 * - CONTRAST:   观点对比
 * - EXTENSION:  延伸讨论
 * - REFERENCE:  相互引用
 *
 * 来源（source）：
 * - VECTOR: 向量语义相似
 * - TAG: 共享标签
 * - LLM: LLM 分析判定
 */
@TableName("knowledge_relation")
@Schema(description = "知识关联")
public class KnowledgeRelation {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键 ID")
    private Long id;

    @TableField("knowledge_id")
    @Schema(description = "知识 ID")
    private Long knowledgeId;

    @TableField("related_id")
    @Schema(description = "关联知识 ID")
    private Long relatedId;

    @TableField("relation_type")
    @Schema(description = "关联类型：COMPLEMENT/CONTRAST/EXTENSION/REFERENCE")
    private String relationType;

    @Schema(description = "置信度 0.00~1.00")
    private BigDecimal score;

    @TableField("source")
    @Schema(description = "来源：VECTOR/TAG/LLM")
    private String source;

    @TableField("created_at")
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getKnowledgeId() { return knowledgeId; }
    public void setKnowledgeId(Long knowledgeId) { this.knowledgeId = knowledgeId; }
    public Long getRelatedId() { return relatedId; }
    public void setRelatedId(Long relatedId) { this.relatedId = relatedId; }
    public String getRelationType() { return relationType; }
    public void setRelationType(String relationType) { this.relationType = relationType; }
    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}