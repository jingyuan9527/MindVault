package com.mindvault.systemconfig.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 系统配置实体。
 * <p>
 * 以 KV 键值对形式存储系统的动态配置参数，支持运行时修改无需重启。
 * 配置项涵盖提示词模板（prompt.*）、阈值参数（threshold.*）、默认值（default.*）、
 * 定时任务控制（task.*）等，按模块分组管理。
 * </p>
 * <p>
 * 关键设计:
 * <ul>
 *   <li>configKey 按命名空间分层: prompt.模块名.* / threshold.模块名.* / task.模块名.* 等</li>
 *   <li>valueType 字段标注值类型（string/int/bool/cron/prompt），前端自动切换输入控件</li>
 *   <li>默认值和校验规则由 SystemConfigDefaults 类集中管理</li>
 * </ul>
 * </p>
 * <p>表: system_config</p>
 */
@TableName("system_config")
@Schema(description = "系统配置")
public class SystemConfig {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键 ID")
    private Long id;

    @TableField("config_key")
    @Schema(description = "配置键")
    private String configKey;

    @TableField("config_value")
    @Schema(description = "配置值")
    private String configValue;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "值类型: string/int/bool/cron/prompt")
    private String valueType = "string";

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getConfigKey() { return configKey; }
    public void setConfigKey(String configKey) { this.configKey = configKey; }
    public String getConfigValue() { return configValue; }
    public void setConfigValue(String configValue) { this.configValue = configValue; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getValueType() { return valueType; }
    public void setValueType(String valueType) { this.valueType = valueType; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
