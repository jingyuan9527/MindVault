package com.mindvault.knowledge.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import com.mindvault.common.config.PgVectorType;
import java.time.LocalDateTime;

/**
 * 知识条目实体
 *
 * 这是 MindVault 最核心的数据模型。
 * 每条知识包含文本内容、向量嵌入、标签和元数据。
 *
 * embedding 字段存储由嵌入模型生成的向量（1536 维），
 * 用于语义相似度搜索（余弦相似度）。
 */
@Entity
@Table(name = "knowledge")
@Data
public class Knowledge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "content_type", nullable = false, length = 20)
    private String contentType = "TEXT";

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String tags = "[]";

    /** 向量嵌入 — 由嵌入模型生成的浮点数数组，用于语义搜索 */
    @Type(PgVectorType.class)
    @Column(columnDefinition = "VECTOR(1536)")
    private String embedding;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String metadata = "{}";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}