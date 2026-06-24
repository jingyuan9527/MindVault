package com.mindvault.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mindvault.common.handler.JsonbStringTypeHandler;
import lombok.Data;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@TableName("model_config")
@Data
@Schema(description = "模型配置实体")
public class ModelConfig {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键 ID")
    private Long id;

    @NotBlank(message = "提供商不能为空")
    @TableField("provider")
    @Schema(description = "提供商")
    private String provider;

    @NotBlank(message = "模型名称不能为空")
    @TableField("model_name")
    @Schema(description = "模型名称")
    private String modelName;

    @TableField("model_type")
    @Schema(description = "模型类型")
    private String modelType = "CHAT";

    @NotBlank(message = "API 密钥不能为空")
    @TableField("api_key")
    @Schema(description = "API 密钥")
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

    @TableField(value = "metadata", typeHandler = JsonbStringTypeHandler.class)
    @Schema(description = "元数据")
    private String metadata = "{}";

    @TableField("created_at")
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}