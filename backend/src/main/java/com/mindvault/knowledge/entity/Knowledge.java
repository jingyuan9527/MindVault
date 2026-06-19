package com.mindvault.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mindvault.common.handler.JsonbStringTypeHandler;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@TableName("knowledge")
@Schema(description = "知识实体")
public class Knowledge {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键 ID")
    private Long id;

    @Schema(description = "用户标题（AI 永不修改）")
    private String title;

    @TableField("ai_title")
    @Schema(description = "AI 生成标题")
    private String aiTitle;

    @Schema(description = "内容")
    private String content;

    @TableField("content_type")
    @Schema(description = "内容类型")
    private String contentType = "TEXT";

    @TableField("source_url")
    @Schema(description = "来源 URL")
    private String sourceUrl;

    @Schema(description = "摘要")
    private String summary;

    @TableField(typeHandler = JsonbStringTypeHandler.class)
    @Schema(description = "AI 生成标签")
    private String tags = "[]";

    @TableField(typeHandler = JsonbStringTypeHandler.class)
    @Schema(description = "用户标签（AI 永不修改）")
    private String userTags = "[]";

    @Schema(description = "向量嵌入")
    private String embedding;

    @TableField(typeHandler = JsonbStringTypeHandler.class)
    @Schema(description = "元数据")
    private String metadata = "{}";

    @TableField("auto_process_status")
    @Schema(description = "自动处理状态")
    private String autoProcessStatus = "PENDING";

    @TableField("created_at")
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAiTitle() { return aiTitle; }
    public void setAiTitle(String aiTitle) { this.aiTitle = aiTitle; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getUserTags() { return userTags; }
    public void setUserTags(String userTags) { this.userTags = userTags; }
    public String getEmbedding() { return embedding; }
    public void setEmbedding(String embedding) { this.embedding = embedding; }
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    public String getAutoProcessStatus() { return autoProcessStatus; }
    public void setAutoProcessStatus(String autoProcessStatus) { this.autoProcessStatus = autoProcessStatus; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
