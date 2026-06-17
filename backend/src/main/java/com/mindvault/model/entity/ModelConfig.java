package com.mindvault.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.JdbcTypeCode;
import java.time.LocalDateTime;

/**
 * 模型配置实体
 *
 * 对应 model_config 表，存储 LLM 模型的连接配置。
 * AgentScope 通过这里配置的 API Key 和 Base URL 调用大模型。
 *
 * model_type 字段：
 * - CHAT: 对话模型（如 qwen-turbo, gpt-4o），用于 Agent 问答
 * - EMBEDDING: 嵌入模型（如 text-embedding-v2），用于向量化
 * - SUMMARIZE: 摘要模型（v0.2+ 使用），用于自动摘要和标签生成
 */
@Entity
@Table(name = "model_config")
@Data
public class ModelConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String provider;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "model_type", nullable = false, length = 20)
    private String modelType = "CHAT";

    @Column(name = "api_key")
    private String apiKey;

    @Column(name = "base_url", length = 500)
    private String baseUrl;

    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary = false;

    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled = true;

    @Column(nullable = false)
    private Integer priority = 0;

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