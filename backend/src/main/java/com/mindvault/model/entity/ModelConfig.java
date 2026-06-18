package com.mindvault.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@TableName("model_config")
@Data
public class ModelConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("provider")
    private String provider;

    @TableField("model_name")
    private String modelName;

    @TableField("model_type")
    private String modelType = "CHAT";

    @TableField("api_key")
    private String apiKey;

    @TableField("base_url")
    private String baseUrl;

    @TableField("is_primary")
    private Boolean isPrimary = false;

    @TableField("is_enabled")
    private Boolean isEnabled = true;

    @TableField("priority")
    private Integer priority = 0;

    @TableField("metadata")
    private String metadata = "{}";

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}