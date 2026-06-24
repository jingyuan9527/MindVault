package com.mindvault.operationlog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

import com.mindvault.common.handler.JsonbStringTypeHandler;
import io.swagger.v3.oas.annotations.media.Schema;

@TableName("operation_log")
@Data
@Schema(description = "操作日志实体")
public class OperationLog {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键 ID")
    private Long id;

    @TableField("module")
    @Schema(description = "模块")
    private String module;

    @TableField("action")
    @Schema(description = "操作")
    private String action;

    @TableField("entity_id")
    @Schema(description = "实体 ID")
    private Long entityId;

    @TableField("summary")
    @Schema(description = "摘要")
    private String summary;

    @TableField(value = "detail", typeHandler = JsonbStringTypeHandler.class)
    @Schema(description = "详情")
    private String detail;

    @TableField("operator")
    @Schema(description = "操作人")
    private String operator = "system";

    @TableField("created_at")
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}