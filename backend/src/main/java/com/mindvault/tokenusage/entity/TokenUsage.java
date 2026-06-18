package com.mindvault.tokenusage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@TableName("token_usage")
@Schema(description = "Token 用量记录实体")
public class TokenUsage {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键 ID")
    private Long id;

    @TableField("model_id")
    @Schema(description = "模型 ID")
    private Long modelId;

    @TableField("provider")
    @Schema(description = "提供商")
    private String provider;

    @TableField("model_name")
    @Schema(description = "模型名称")
    private String modelName;

    @TableField("model_type")
    @Schema(description = "模型类型")
    private String modelType = "CHAT";

    @TableField("prompt_tokens")
    @Schema(description = "提示词 token 数")
    private int promptTokens;

    @TableField("completion_tokens")
    @Schema(description = "补全 token 数")
    private int completionTokens;

    @TableField("total_tokens")
    @Schema(description = "总 token 数")
    private int totalTokens;

    @TableField("cost")
    @Schema(description = "费用")
    private BigDecimal cost = BigDecimal.ZERO;

    @TableField("request_source")
    @Schema(description = "请求来源")
    private String requestSource = "CHAT";

    @TableField("request_id")
    @Schema(description = "请求 ID")
    private String requestId;

    @TableField("created_at")
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getModelId() { return modelId; }
    public void setModelId(Long modelId) { this.modelId = modelId; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getModelType() { return modelType; }
    public void setModelType(String modelType) { this.modelType = modelType; }
    public int getPromptTokens() { return promptTokens; }
    public void setPromptTokens(int promptTokens) { this.promptTokens = promptTokens; }
    public int getCompletionTokens() { return completionTokens; }
    public void setCompletionTokens(int completionTokens) { this.completionTokens = completionTokens; }
    public int getTotalTokens() { return totalTokens; }
    public void setTotalTokens(int totalTokens) { this.totalTokens = totalTokens; }
    public BigDecimal getCost() { return cost; }
    public void setCost(BigDecimal cost) { this.cost = cost; }
    public String getRequestSource() { return requestSource; }
    public void setRequestSource(String requestSource) { this.requestSource = requestSource; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}