package com.mindvault.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mindvault.common.handler.JsonbStringTypeHandler;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 聊天消息实体。
 * <p>映射 chat_message 表，存储聊天会话中的每一条消息。
 * 支持 USER（用户）、ASSISTANT（AI 回复）、SYSTEM（系统消息）三种角色。
 * metadata 字段以 JSONB 格式存储附加元数据，sources 字段以 JSONB 数组格式存储引用的知识来源列表。
 * 消息按 created_at 升序排列以还原对话顺序。</p>
 */
@TableName("chat_message")
@Schema(description = "聊天消息实体")
public class ChatMessage {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键 ID")
    private Long id;

    @TableField("session_id")
    @Schema(description = "会话 ID")
    private Long sessionId;

    @TableField("role")
    @Schema(description = "角色")
    private String role;

    @TableField("content")
    @Schema(description = "内容")
    private String content;

    @TableField(value = "metadata", typeHandler = JsonbStringTypeHandler.class)
    @Schema(description = "元数据")
    private String metadata = "{}";

    @TableField(value = "sources", typeHandler = JsonbStringTypeHandler.class)
    @Schema(description = "来源")
    private String sources = "[]";

    @TableField("created_at")
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    public String getSources() { return sources; }
    public void setSources(String sources) { this.sources = sources; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}