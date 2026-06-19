package com.mindvault.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@TableName("model_config")
@Data
@Schema(description = "模型配置实体")
public class ModelConfig {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键 ID")
    private Long id;

    @TableField("provider")
    @Schema(description = "提供商")
    private String provider;

    @TableField("model_name")
    @Schema(description = "模型名称")
    private String modelName;

    @TableField("model_type")
    @Schema(description = "模型类型")
    private String modelType = "CHAT";

    @TableField("api_key")
    @Schema(description = "API 密钥")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String apiKey;

    @TableField("base_url")
    @Schema(description = "基础 URL")
    private String baseUrl;

    @TableField("is_primary")
    @Schema(description = "是否为主模型")
    private Boolean isPrimary = false;

    @TableField("is_enabled")
    @Schema(description = "是否启用")
    private Boolean isEnabled = true;

    @TableField("priority")
    @Schema(description = "优先级")
    private Integer priority = 0;

    @TableField("metadata")
    @Schema(description = "元数据")
    private String metadata = "{}";

    @TableField("created_at")
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}